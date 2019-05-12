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

package grondag.canvas.light;

/**
 * Holds per-corner results for a single block face. Handles caching and
 * provides various utility methods to simplify code elsewhere.
 */
public class AoFaceData {
    public static int OPAQUE = -1;
    
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
    
    private AoFaceCalc calc = null;
    
    public AoFaceCalc calc() {
        AoFaceCalc result = calc;
        if(result == null) {
            result = AoFaceCalc.claim().compute(this);
            this.calc = result;
        }
        return result;
    }
    
    public AoFaceData resetCalc() {
        if(calc != null) {
            calc.release();
            calc = null;
        }
        return this;
    }
    
    public static AoFaceData weightedBlend(AoFaceData in0, float w0, AoFaceData in1, float w1, AoFaceData out) {
        out.top = lightBlend(in0.top, w0, in1.top, w1);
        out.left = lightBlend(in0.left, w0, in1.left, w1);
        out.right = lightBlend(in0.right, w0, in1.right, w1);
        out.bottom = lightBlend(in0.bottom, w0, in1.bottom, w1);
        
        out.topLeft = lightBlend(in0.topLeft, w0, in1.topLeft, w1);
        out.topRight = lightBlend(in0.topRight, w0, in1.topRight, w1);
        out.bottomLeft = lightBlend(in0.bottomLeft, w0, in1.bottomLeft, w1);
        out.bottomRight = lightBlend(in0.bottomRight, w0, in1.bottomRight, w1);
        
        out.center = lightBlend(in0.center, w0, in1.center, w1);
        
        out.aoTop = in0.aoTop * w0 + in1.aoTop * w1;
        out.aoLeft = in0.aoLeft * w0 + in1.aoLeft * w1;
        out.aoRight = in0.aoRight * w0 + in1.aoRight * w1;
        out.aoBottom = in0.aoBottom * w0 + in1.aoBottom * w1;
        
        out.aoTopLeft = in0.aoTopLeft * w0 + in1.aoTopLeft * w1;
        out.aoTopRight = in0.aoTopRight * w0 + in1.aoTopRight * w1;
        out.aoBottomLeft = in0.aoBottomLeft * w0 + in1.aoBottomLeft * w1;
        out.aoBottomRight = in0.aoBottomRight * w0 + in1.aoBottomRight * w1;
        
        out.aoCenter = in0.aoCenter * w0 + in1.aoCenter * w1;
        
        return out;
    }
    
    private static int lightBlend(int l0, float w0, int l1, float w1) {
        if(l0 == OPAQUE) {
            if(l1 == OPAQUE) { // both opaque
                return OPAQUE;
            } else { // l0 opaque
                return reduce(l1);
            }
        } else { 
            if(l1 == OPAQUE) { //l1 opaque
                return reduce(l0);
            } else { // neither opaque
                return lightBlendInner(l0, w0, l1, w1);
            }
        }
    }
    
    private static int lightBlendInner(int l0, float w0, int l1, float w1) {
        int b0 = (l0 & 0xFF);
        int k0 = ((l0 >> 16) & 0xFF);
        int b1 = (l1 & 0xFF);
        int k1 = ((l1 >> 16) & 0xFF);
        float b = b0 * w0 + b1 * w1;
        float k = k0 * w0 + k1 * w1;
        return Math.round(b) | (Math.round(k) << 16);
    }
    
    private static int reduce(int light) {
        int block = (light & 0xFF) - 16;
        int sky = ((light >> 16) & 0xFF) - 16;
        return Math.max(0, block) | (Math.max(0, sky) << 16);
    }
}
