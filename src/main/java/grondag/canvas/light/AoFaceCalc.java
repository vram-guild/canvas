package grondag.canvas.light;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.IntBinaryOperator;


/**
 * Handles vanilla-style calculations for ao and light blending.
 */
public class AoFaceCalc {
    private static final ArrayBlockingQueue<AoFaceCalc> POOL = new ArrayBlockingQueue<>(512);
    
    public static AoFaceCalc claim() {
        AoFaceCalc result = POOL.poll();
        if (result == null) {
            result = new AoFaceCalc();
        }
        return result;
    }
    
    private AoFaceCalc() {}
    
    public void release() {
        POOL.offer(this);
    }
    
    int aoBottomRight;
    int aoBottomLeft;
    int aoTopLeft;
    int aoTopRight;
    
    int blockBottomRight;
    int blockBottomLeft;
    int blockTopLeft;
    int blockTopRight;
    
    int skyBottomRight;
    int skyBottomLeft;
    int skyTopLeft;
    int skyTopRight;
    
    public AoFaceCalc compute(AoFaceData input) {
        aoTopLeft = input.aoTopLeft;
        aoTopRight = input.aoTopRight;
        aoBottomLeft = input.aoBottomLeft;
        aoBottomRight = input.aoBottomRight;
        
        int l = meanBrightness(input.right, input.bottom, input.bottomRight, input.center);
        blockBottomRight = l & 0xFFFF;
        skyBottomRight = (l >>> 16) & 0xFFFF;
        
        l = meanBrightness(input.left, input.bottom, input.bottomLeft, input.center);
        this.blockBottomLeft = l & 0xFFFF;
        this.skyBottomLeft = (l >>> 16) & 0xFFFF;
        
        l = meanBrightness(input.left, input.top, input.topLeft, input.center);
        this.blockTopLeft = l & 0xFFFF;
        this.skyTopLeft = (l >>> 16) & 0xFFFF;
        
        l = meanBrightness(input.right, input.top, input.topRight, input.center);
        this.blockTopRight = l & 0xFFFF;
        this.skyTopRight = (l >>> 16) & 0xFFFF;
        
        return this;
    }

    int weigtedBlockLight(float[] w) {
        return (int) (blockBottomRight * w[0] + blockBottomLeft * w[1] + blockTopLeft * w[2] + blockTopRight * w[3]) & 0xFF;
    }

    int maxBlockLight(int oldMax) {
        final int i = blockBottomRight > blockBottomLeft ? blockBottomRight : blockBottomLeft;
        final int j = blockTopLeft > blockTopRight ? blockTopLeft : blockTopRight;
        return Math.max(oldMax, i > j ? i : j);
    }

    int weigtedSkyLight(float[] w) {
        return (int) (skyBottomRight * w[0] + skyBottomLeft * w[1] + skyTopLeft * w[2] + skyTopRight * w[3]) & 0xFF;
    }

    int maxSkyLight(int oldMax) {
        final int i = skyBottomRight > skyBottomLeft ? skyBottomRight : skyBottomLeft;
        final int j = skyTopLeft > skyTopRight ? skyTopLeft : skyTopRight;
        return Math.max(oldMax, i > j ? i : j);
    }

    int weightedCombinedLight(float[] w) {
        return weigtedSkyLight(w) << 16 | weigtedBlockLight(w);
    }

    float weigtedAo(float[] w) {
        // PERF: pass ints directly to vertex encoder
        return (aoBottomRight * w[0] + aoBottomLeft * w[1] + aoTopLeft * w[2] + aoTopRight * w[3]);
    }

    float maxAo(float oldMax) {
        final int x = aoBottomRight > aoBottomLeft ? aoBottomRight : aoBottomLeft;
        final int y = aoTopLeft > aoTopRight ? aoTopLeft : aoTopRight;
        final int z = x > y ? x : y;
        return oldMax > z ? oldMax : z;
    }

    AoFaceCalc toArray(float[] aoOut, int[] lightOut, int[] vertexMap) {
        // PERF: pass ints directly to vertex encoder
        aoOut[vertexMap[0]] = aoBottomRight * AoCalculator.DIVIDE_BY_255;
        aoOut[vertexMap[1]] = aoBottomLeft * AoCalculator.DIVIDE_BY_255;
        aoOut[vertexMap[2]] = aoTopLeft * AoCalculator.DIVIDE_BY_255;
        aoOut[vertexMap[3]] = aoTopRight * AoCalculator.DIVIDE_BY_255;
        
        lightOut[vertexMap[0]] = skyBottomRight << 16 | blockBottomRight;
        lightOut[vertexMap[1]] = skyBottomLeft << 16 | blockBottomLeft;
        lightOut[vertexMap[2]] = skyTopLeft << 16 | blockTopLeft;
        lightOut[vertexMap[3]] = skyTopRight << 16 | blockTopRight;
        return this;
    }

    // PERF: use integer weights
    static AoFaceCalc weightedMean(AoFaceCalc in0, float w0, AoFaceCalc in1, float w1) {
        AoFaceCalc out = claim();
        out.aoBottomRight = Math.round(in0.aoBottomRight * w0 + in1.aoBottomRight * w1);
        out.aoBottomLeft = Math.round(in0.aoBottomLeft * w0 + in1.aoBottomLeft * w1);
        out.aoTopLeft = Math.round(in0.aoTopLeft * w0 + in1.aoTopLeft * w1);
        out.aoTopRight = Math.round(in0.aoTopRight * w0 + in1.aoTopRight * w1);

        out.blockBottomRight = Math.round(in0.blockBottomRight * w0 + in1.blockBottomRight * w1);
        out.blockBottomLeft = Math.round(in0.blockBottomLeft * w0 + in1.blockBottomLeft * w1);
        out.blockTopLeft = Math.round(in0.blockTopLeft * w0 + in1.blockTopLeft * w1);
        out.blockTopRight = Math.round(in0.blockTopRight * w0 + in1.blockTopRight * w1);

        out.skyBottomRight = Math.round(in0.skyBottomRight * w0 + in1.skyBottomRight * w1);
        out.skyBottomLeft = Math.round(in0.skyBottomLeft * w0 + in1.skyBottomLeft * w1);
        out.skyTopLeft = Math.round(in0.skyTopLeft * w0 + in1.skyTopLeft * w1);
        out.skyTopRight = Math.round(in0.skyTopRight * w0 + in1.skyTopRight * w1);
        
        return out;
    }

    /**
     * Independent minimum of packed components
     */
    private static int min(int x, int y) {
        final int s = Math.min(x & 0x00FF0000, y & 0x00FF0000);
        final int b = Math.min(x & 0xFF, y & 0xFF);
        return s | b;
    }
    
    /** 
     * Vanilla code excluded missing light values from mean but was not isotropic.
     * Still need to substitute or edges are too dark but consistently use the min 
     * value from all four samples.
     */
    private static int meanBrightness(int a, int b, int c, int d) {
        int missingVal = 0x0FFFFFFF;
        IntBinaryOperator func = AoFaceCalc::min;
        int missingCount = 0;
        int total = 0;
        
        if(a == AoFaceData.OPAQUE) {
            missingCount++;
        } else {
            total += a;
            
            missingVal = func.applyAsInt(missingVal, a);
        }
        
        if(b == AoFaceData.OPAQUE) {
            missingCount++;
        } else {
            total += b;
            missingVal = func.applyAsInt(missingVal, b);
        }
        
        if(c == AoFaceData.OPAQUE) {
            missingCount++;
        } else {
            total += c;
            missingVal = func.applyAsInt(missingVal, c);
        }
        
        if(d == AoFaceData.OPAQUE) {
            missingCount++;
        } else {
            total += d;
            missingVal = func.applyAsInt(missingVal, d);
        }
        
        assert missingCount < 4 : "Computing light for four occluding neighbors?";
        
        // bitwise divide by 4, clamp to expected (positive) range, round up
        return (total + missingVal * missingCount + 2) >> 2 & 16711935;
    }
}
