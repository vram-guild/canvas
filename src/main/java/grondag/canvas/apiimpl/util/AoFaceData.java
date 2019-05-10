/*******************************************************************************
 * Copyright 2019 grondag
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.apiimpl.util;

import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;

/**
 * Holds per-corner results for a single block face. Handles caching and
 * provides various utility methods to simplify code elsewhere.
 */
public class AoFaceData {
    public static int OPAQUE = -1;
    
    // interpolated corner results
    float outAoBottomRight;
    float outAoBottomLeft;
    float outAoTopLeft;
    float outAoTopRight;
    int outBlockBottomRight;
    int outBlockBottomLeft;
    int outBlockTopLeft;
    int outBlockTopRight;
    int outSkyBottomRight;
    int outSkyBottomLeft;
    int outSkyTopLeft;
    int outSkyTopRight;
    
    // packed values gathered during compute
    public int bottom;
    public int top;
    public int left;
    public int right;
    public int bottomLeft;
    public int bottomRight;
    public int topLeft;
    public int topRight;
    public int center;

    float aoBottom;
    float aoTop;
    float aoLeft;
    float aoRight;
    float aoBottomLeft;
    float aoBottomRight;
    float aoTopLeft;
    float aoTopRight;
    float aoCenter;
    
    // PERF - need to call if extra smooth enabled?  Maybe only the AO part?
    void compute() {
        outAoBottomRight = (aoRight + aoBottom + aoBottomRight + aoCenter) * 0.25F;
        outAoBottomLeft = (aoLeft + aoBottom + aoBottomLeft + aoCenter) * 0.25F;
        outAoTopLeft = (aoLeft + aoTop + aoTopLeft + aoCenter) * 0.25F;
        outAoTopRight = (aoRight + aoTop + aoTopRight + aoCenter) * 0.25F;

        int l = meanBrightness(right, bottom, bottomRight, center);
        outBlockBottomRight = l & 0xFFFF;
        outSkyBottomRight = (l >>> 16) & 0xFFFF;
        
        l = meanBrightness(left, bottom, bottomLeft, center);
        this.outBlockBottomLeft = l & 0xFFFF;
        this.outSkyBottomLeft = (l >>> 16) & 0xFFFF;
        
        l = meanBrightness(left, top, topLeft, center);
        this.outBlockTopLeft = l & 0xFFFF;
        this.outSkyTopLeft = (l >>> 16) & 0xFFFF;
        
        l = meanBrightness(right, top, topRight, center);
        this.outBlockTopRight = l & 0xFFFF;
        this.outSkyTopRight = (l >>> 16) & 0xFFFF;
    }

    int weigtedBlockLight(float[] w) {
        return (int) (outBlockBottomRight * w[0] + outBlockBottomLeft * w[1] + outBlockTopLeft * w[2] + outBlockTopRight * w[3]) & 0xFF;
    }

    int maxBlockLight(int oldMax) {
        final int i = outBlockBottomRight > outBlockBottomLeft ? outBlockBottomRight : outBlockBottomLeft;
        final int j = outBlockTopLeft > outBlockTopRight ? outBlockTopLeft : outBlockTopRight;
        return Math.max(oldMax, i > j ? i : j);
    }

    int weigtedSkyLight(float[] w) {
        return (int) (outSkyBottomRight * w[0] + outSkyBottomLeft * w[1] + outSkyTopLeft * w[2] + outSkyTopRight * w[3]) & 0xFF;
    }

    int maxSkyLight(int oldMax) {
        final int i = outSkyBottomRight > outSkyBottomLeft ? outSkyBottomRight : outSkyBottomLeft;
        final int j = outSkyTopLeft > outSkyTopRight ? outSkyTopLeft : outSkyTopRight;
        return Math.max(oldMax, i > j ? i : j);
    }

    int weightedCombinedLight(float[] w) {
        return weigtedSkyLight(w) << 16 | weigtedBlockLight(w);
    }

    float weigtedAo(float[] w) {
        return outAoBottomRight * w[0] + outAoBottomLeft * w[1] + outAoTopLeft * w[2] + outAoTopRight * w[3];
    }

    float maxAo(float oldMax) {
        final float x = outAoBottomRight > outAoBottomLeft ? outAoBottomRight : outAoBottomLeft;
        final float y = outAoTopLeft > outAoTopRight ? outAoTopLeft : outAoTopRight;
        final float z = x > y ? x : y;
        return oldMax > z ? oldMax : z;
    }

    void toArray(float[] aoOut, int[] lightOut, int[] vertexMap) {
        aoOut[vertexMap[0]] = outAoBottomRight;
        aoOut[vertexMap[1]] = outAoBottomLeft;
        aoOut[vertexMap[2]] = outAoTopLeft;
        aoOut[vertexMap[3]] = outAoTopRight;
        lightOut[vertexMap[0]] = outSkyBottomRight << 16 | outBlockBottomRight;
        lightOut[vertexMap[1]] = outSkyBottomLeft << 16 | outBlockBottomLeft;
        lightOut[vertexMap[2]] = outSkyTopLeft << 16 | outBlockTopLeft;
        lightOut[vertexMap[3]] = outSkyTopRight << 16 | outBlockTopRight;
    }

    // PERF - given that we need the out samples
    // should average those and then derive the block corner values if needed
    static AoFaceData weightedMean(AoFaceData in0, float w0, AoFaceData in1, float w1, AoFaceData out) {
        out.outAoBottomRight = in0.outAoBottomRight * w0 + in1.outAoBottomRight * w1;
        out.outAoBottomLeft = in0.outAoBottomLeft * w0 + in1.outAoBottomLeft * w1;
        out.outAoTopLeft = in0.outAoTopLeft * w0 + in1.outAoTopLeft * w1;
        out.outAoTopRight = in0.outAoTopRight * w0 + in1.outAoTopRight * w1;

        out.aoBottom = in0.aoBottom * w0 + in1.aoBottom * w1;
        out.aoTop = in0.aoTop * w0 + in1.aoTop * w1;
        out.aoLeft = in0.aoLeft * w0 + in1.aoLeft * w1;
        out.aoRight = in0.aoRight * w0 + in1.aoRight * w1;
        
        out.aoBottomLeft = in0.aoBottomLeft * w0 + in1.aoBottomLeft * w1;
        out.aoBottomRight = in0.aoBottomRight * w0 + in1.aoBottomRight * w1;
        out.aoTopLeft = in0.aoTopLeft * w0 + in1.aoTopLeft * w1;
        out.aoTopRight = in0.aoTopRight * w0 + in1.aoTopRight * w1;
        
        out.aoCenter = in0.aoCenter * w0 + in1.aoCenter * w1;
        
        out.outBlockBottomRight = (int) (in0.outBlockBottomRight * w0 + in1.outBlockBottomRight * w1);
        out.outBlockBottomLeft = (int) (in0.outBlockBottomLeft * w0 + in1.outBlockBottomLeft * w1);
        out.outBlockTopLeft = (int) (in0.outBlockTopLeft * w0 + in1.outBlockTopLeft * w1);
        out.outBlockTopRight = (int) (in0.outBlockTopRight * w0 + in1.outBlockTopRight * w1);

        out.outSkyBottomRight = (int) (in0.outSkyBottomRight * w0 + in1.outSkyBottomRight * w1);
        out.outSkyBottomLeft = (int) (in0.outSkyBottomLeft * w0 + in1.outSkyBottomLeft * w1);
        out.outSkyTopLeft = (int) (in0.outSkyTopLeft * w0 + in1.outSkyTopLeft * w1);
        out.outSkyTopRight = (int) (in0.outSkyTopRight * w0 + in1.outSkyTopRight * w1);

        // TODO - need for light
        out.outBlockBottomRight = (int) (in0.outBlockBottomRight * w0 + in1.outBlockBottomRight * w1);
        out.outBlockBottomLeft = (int) (in0.outBlockBottomLeft * w0 + in1.outBlockBottomLeft * w1);
        out.outBlockTopLeft = (int) (in0.outBlockTopLeft * w0 + in1.outBlockTopLeft * w1);
        out.outBlockTopRight = (int) (in0.outBlockTopRight * w0 + in1.outBlockTopRight * w1);

        out.outSkyBottomRight = (int) (in0.outSkyBottomRight * w0 + in1.outSkyBottomRight * w1);
        out.outSkyBottomLeft = (int) (in0.outSkyBottomLeft * w0 + in1.outSkyBottomLeft * w1);
        out.outSkyTopLeft = (int) (in0.outSkyTopLeft * w0 + in1.outSkyTopLeft * w1);
        out.outSkyTopRight = (int) (in0.outSkyTopRight * w0 + in1.outSkyTopRight * w1);
        
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
        IntBinaryOperator func = useMax ? AoFaceData::max : AoFaceData::min;
        int missingCount = 0;
        int total = 0;
        
        if(a == OPAQUE) {
            missingCount++;
        } else {
            total += a;
            
            missingVal = func.applyAsInt(missingVal, a);
        }
        
        if(b == OPAQUE) {
            missingCount++;
        } else {
            total += b;
            missingVal = func.applyAsInt(missingVal, b);
        }
        
        if(c == OPAQUE) {
            missingCount++;
        } else {
            total += c;
            missingVal = func.applyAsInt(missingVal, c);
        }
        
        if(d == OPAQUE) {
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
