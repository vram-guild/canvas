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

import static java.lang.Math.max;

/**
 * Holds per-corner results for a single block face. Handles caching and
 * provides various utility methods to simplify code elsewhere.
 */
class AoFaceData {
    // interpolated corner results
    float a0;
    float a1;
    float a2;
    float a3;
    int b0;
    int b1;
    int b2;
    int b3;
    int s0;
    int s1;
    int s2;
    int s3;
    
    // packed values gathered during compute
    int light0;
    int light1;
    int light2;
    int light3;
    int cLight0;
    int cLight1;
    int cLight2;
    int cLight3;
    int lightCenter;

    float ao0;
    float ao1;
    float ao2;
    float ao3;
    float cAo0;
    float cAo1;
    float cAo2;
    float cAo3;
    float aoCenter;
    
    void compute() {
        a0 = (ao3 + ao0 + cAo1 + aoCenter) * 0.25F;
        a1 = (ao2 + ao0 + cAo0 + aoCenter) * 0.25F;
        a2 = (ao2 + ao1 + cAo2 + aoCenter) * 0.25F;
        a3 = (ao3 + ao1 + cAo3 + aoCenter) * 0.25F;

        l0(meanBrightness(light3, light0, cLight1, lightCenter));
        l1(meanBrightness(light2, light0, cLight0, lightCenter));
        l2(meanBrightness(light2, light1, cLight2, lightCenter));
        l3(meanBrightness(light3, light1, cLight3, lightCenter));
    }
    
    void l0(int l0) {
        this.b0 = l0 & 0xFFFF;
        this.s0 = (l0 >>> 16) & 0xFFFF;
    }

    void l1(int l1) {
        this.b1 = l1 & 0xFFFF;
        this.s1 = (l1 >>> 16) & 0xFFFF;
    }

    void l2(int l2) {
        this.b2 = l2 & 0xFFFF;
        this.s2 = (l2 >>> 16) & 0xFFFF;
    }

    void l3(int l3) {
        this.b3 = l3 & 0xFFFF;
        this.s3 = (l3 >>> 16) & 0xFFFF;
    }

    int weigtedBlockLight(float[] w) {
        return (int) (b0 * w[0] + b1 * w[1] + b2 * w[2] + b3 * w[3]) & 0xFF;
    }

    int maxBlockLight(int oldMax) {
        final int i = b0 > b1 ? b0 : b1;
        final int j = b2 > b3 ? b2 : b3;
        return Math.max(oldMax, i > j ? i : j);
    }

    int weigtedSkyLight(float[] w) {
        return (int) (s0 * w[0] + s1 * w[1] + s2 * w[2] + s3 * w[3]) & 0xFF;
    }

    int maxSkyLight(int oldMax) {
        final int i = s0 > s1 ? s0 : s1;
        final int j = s2 > s3 ? s2 : s3;
        return Math.max(oldMax, i > j ? i : j);
    }

    int weightedCombinedLight(float[] w) {
        return weigtedSkyLight(w) << 16 | weigtedBlockLight(w);
    }

    float weigtedAo(float[] w) {
        return a0 * w[0] + a1 * w[1] + a2 * w[2] + a3 * w[3];
    }

    float maxAo(float oldMax) {
        final float x = a0 > a1 ? a0 : a1;
        final float y = a2 > a3 ? a2 : a3;
        final float z = x > y ? x : y;
        return oldMax > z ? oldMax : z;
    }

    void toArray(float[] aOut, int[] bOut, int[] vertexMap) {
        aOut[vertexMap[0]] = a0;
        aOut[vertexMap[1]] = a1;
        aOut[vertexMap[2]] = a2;
        aOut[vertexMap[3]] = a3;
        bOut[vertexMap[0]] = s0 << 16 | b0;
        bOut[vertexMap[1]] = s1 << 16 | b1;
        bOut[vertexMap[2]] = s2 << 16 | b2;
        bOut[vertexMap[3]] = s3 << 16 | b3;
    }

    // PERF - given that we need the out samples
    // should average those and then derive the block corner values if needed
    static AoFaceData weightedMean(AoFaceData in0, float w0, AoFaceData in1, float w1, AoFaceData out) {
        out.a0 = in0.a0 * w0 + in1.a0 * w1;
        out.a1 = in0.a1 * w0 + in1.a1 * w1;
        out.a2 = in0.a2 * w0 + in1.a2 * w1;
        out.a3 = in0.a3 * w0 + in1.a3 * w1;

        out.ao0 = in0.ao0 * w0 + in1.ao0 * w1;
        out.ao1 = in0.ao1 * w0 + in1.ao1 * w1;
        out.ao2 = in0.ao2 * w0 + in1.ao2 * w1;
        out.ao3 = in0.ao3 * w0 + in1.ao3 * w1;
        
        out.cAo0 = in0.cAo0 * w0 + in1.cAo0 * w1;
        out.cAo1 = in0.cAo1 * w0 + in1.cAo1 * w1;
        out.cAo2 = in0.cAo2 * w0 + in1.cAo2 * w1;
        out.cAo3 = in0.cAo3 * w0 + in1.cAo3 * w1;
        
        out.aoCenter = in0.aoCenter * w0 + in1.aoCenter * w1;
        
        out.b0 = (int) (in0.b0 * w0 + in1.b0 * w1);
        out.b1 = (int) (in0.b1 * w0 + in1.b1 * w1);
        out.b2 = (int) (in0.b2 * w0 + in1.b2 * w1);
        out.b3 = (int) (in0.b3 * w0 + in1.b3 * w1);

        out.s0 = (int) (in0.s0 * w0 + in1.s0 * w1);
        out.s1 = (int) (in0.s1 * w0 + in1.s1 * w1);
        out.s2 = (int) (in0.s2 * w0 + in1.s2 * w1);
        out.s3 = (int) (in0.s3 * w0 + in1.s3 * w1);

        // TODO - need for light
        out.b0 = (int) (in0.b0 * w0 + in1.b0 * w1);
        out.b1 = (int) (in0.b1 * w0 + in1.b1 * w1);
        out.b2 = (int) (in0.b2 * w0 + in1.b2 * w1);
        out.b3 = (int) (in0.b3 * w0 + in1.b3 * w1);

        out.s0 = (int) (in0.s0 * w0 + in1.s0 * w1);
        out.s1 = (int) (in0.s1 * w0 + in1.s1 * w1);
        out.s2 = (int) (in0.s2 * w0 + in1.s2 * w1);
        out.s3 = (int) (in0.s3 * w0 + in1.s3 * w1);
        
        return out;
    }

    /** 
     * Vanilla code excluded missing light values from mean but was not isotropic.
     * Still need to substitute or edges are too dark but consistently use the min 
     * value from all four samples.
     */
    private static int meanBrightness(int a, int b, int c, int d) {
        return a == 0 || b == 0 || c == 0 || d == 0 ? meanEdgeBrightness(a, b, c, d) : meanInnerBrightness(a, b, c, d);
    }
    
    private static int meanEdgeBrightness(int a, int b, int c, int d) {
        final int min = nonZeroMin(nonZeroMin(a, b), nonZeroMin(c, d));
        return meanInnerBrightness(max(a, min), max(b, min), max(c, min), max(d, min));
    }
    
    private static int meanInnerBrightness(int a, int b, int c, int d) {
        // bitwise divide by 4, clamp to expected (positive) range
        return a + b + c + d >> 2 & 16711935;
    }

    private static int nonZeroMin(int a, int b) {
        if(a == 0) return b;
        if(b == 0) return a;
        return Math.min(a, b);
    }
}
