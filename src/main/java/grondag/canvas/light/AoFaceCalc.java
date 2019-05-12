package grondag.canvas.light;

import java.util.function.IntBinaryOperator;

/**
 * Handles vanilla-style calculations for ao and light blending.
 */
public class AoFaceCalc {
    float aoBottomRight;
    float aoBottomLeft;
    float aoTopLeft;
    float aoTopRight;
    
    int blockBottomRight;
    int blockBottomLeft;
    int blockTopLeft;
    int blockTopRight;
    
    int skyBottomRight;
    int skyBottomLeft;
    int skyTopLeft;
    int skyTopRight;
    
    public AoFaceCalc compute(AoFaceData input) {
        aoBottomRight = (input.aoRight + input.aoBottom + input.aoBottomRight + input.aoCenter) * 0.25F;
        aoBottomLeft = (input.aoLeft + input.aoBottom + input.aoBottomLeft + input.aoCenter) * 0.25F;
        aoTopLeft = (input.aoLeft + input.aoTop + input.aoTopLeft + input.aoCenter) * 0.25F;
        aoTopRight = (input.aoRight + input.aoTop + input.aoTopRight + input.aoCenter) * 0.25F;

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
        return aoBottomRight * w[0] + aoBottomLeft * w[1] + aoTopLeft * w[2] + aoTopRight * w[3];
    }

    float maxAo(float oldMax) {
        final float x = aoBottomRight > aoBottomLeft ? aoBottomRight : aoBottomLeft;
        final float y = aoTopLeft > aoTopRight ? aoTopLeft : aoTopRight;
        final float z = x > y ? x : y;
        return oldMax > z ? oldMax : z;
    }

    void toArray(float[] aoOut, int[] lightOut, int[] vertexMap) {
        aoOut[vertexMap[0]] = aoBottomRight;
        aoOut[vertexMap[1]] = aoBottomLeft;
        aoOut[vertexMap[2]] = aoTopLeft;
        aoOut[vertexMap[3]] = aoTopRight;
        lightOut[vertexMap[0]] = skyBottomRight << 16 | blockBottomRight;
        lightOut[vertexMap[1]] = skyBottomLeft << 16 | blockBottomLeft;
        lightOut[vertexMap[2]] = skyTopLeft << 16 | blockTopLeft;
        lightOut[vertexMap[3]] = skyTopRight << 16 | blockTopRight;
    }

    // PERF - given that we need the out samples
    // should average those and then derive the block corner values if needed
    static AoFaceCalc weightedMean(AoFaceCalc in0, float w0, AoFaceCalc in1, float w1, AoFaceCalc out) {
        out.aoBottomRight = in0.aoBottomRight * w0 + in1.aoBottomRight * w1;
        out.aoBottomLeft = in0.aoBottomLeft * w0 + in1.aoBottomLeft * w1;
        out.aoTopLeft = in0.aoTopLeft * w0 + in1.aoTopLeft * w1;
        out.aoTopRight = in0.aoTopRight * w0 + in1.aoTopRight * w1;

        // was here when was part of input class
//        out.aoBottom = in0.aoBottom * w0 + in1.aoBottom * w1;
//        out.aoTop = in0.aoTop * w0 + in1.aoTop * w1;
//        out.aoLeft = in0.aoLeft * w0 + in1.aoLeft * w1;
//        out.aoRight = in0.aoRight * w0 + in1.aoRight * w1;
//        
//        out.aoBottomLeft = in0.aoBottomLeft * w0 + in1.aoBottomLeft * w1;
//        out.aoBottomRight = in0.aoBottomRight * w0 + in1.aoBottomRight * w1;
//        out.aoTopLeft = in0.aoTopLeft * w0 + in1.aoTopLeft * w1;
//        out.aoTopRight = in0.aoTopRight * w0 + in1.aoTopRight * w1;
//        
//        out.aoCenter = in0.aoCenter * w0 + in1.aoCenter * w1;
        
        out.blockBottomRight = (int) (in0.blockBottomRight * w0 + in1.blockBottomRight * w1);
        out.blockBottomLeft = (int) (in0.blockBottomLeft * w0 + in1.blockBottomLeft * w1);
        out.blockTopLeft = (int) (in0.blockTopLeft * w0 + in1.blockTopLeft * w1);
        out.blockTopRight = (int) (in0.blockTopRight * w0 + in1.blockTopRight * w1);

        out.skyBottomRight = (int) (in0.skyBottomRight * w0 + in1.skyBottomRight * w1);
        out.skyBottomLeft = (int) (in0.skyBottomLeft * w0 + in1.skyBottomLeft * w1);
        out.skyTopLeft = (int) (in0.skyTopLeft * w0 + in1.skyTopLeft * w1);
        out.skyTopRight = (int) (in0.skyTopRight * w0 + in1.skyTopRight * w1);

        out.blockBottomRight = (int) (in0.blockBottomRight * w0 + in1.blockBottomRight * w1);
        out.blockBottomLeft = (int) (in0.blockBottomLeft * w0 + in1.blockBottomLeft * w1);
        out.blockTopLeft = (int) (in0.blockTopLeft * w0 + in1.blockTopLeft * w1);
        out.blockTopRight = (int) (in0.blockTopRight * w0 + in1.blockTopRight * w1);

        out.skyBottomRight = (int) (in0.skyBottomRight * w0 + in1.skyBottomRight * w1);
        out.skyBottomLeft = (int) (in0.skyBottomLeft * w0 + in1.skyBottomLeft * w1);
        out.skyTopLeft = (int) (in0.skyTopLeft * w0 + in1.skyTopLeft * w1);
        out.skyTopRight = (int) (in0.skyTopRight * w0 + in1.skyTopRight * w1);
        
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
    
    private static int max(int x, int y) {
        final int s = Math.max(x & 0x00FF0000, y & 0x00FF0000);
        final int b = Math.max(x & 0xFF, y & 0xFF);
        return s | b;
    }

    
    /** 
     * Vanilla code excluded missing light values from mean but was not isotropic.
     * Still need to substitute or edges are too dark but consistently use the min 
     * value from all four samples.
     */
    private static int meanBrightness(int a, int b, int c, int d) {
        //TODO: configure min vs max - vanilla is min
        final boolean useMax = false;
        int missingVal = useMax ? 0 : 0x0FFFFFFF;
        IntBinaryOperator func = useMax ? AoFaceCalc::max : AoFaceCalc::min;
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
        
        // bitwise divide by 4, clamp to expected (positive) range
        return (total + missingVal * missingCount) >> 2 & 16711935;
    }
}
