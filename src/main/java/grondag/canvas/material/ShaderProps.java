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
package grondag.canvas.material;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl;

/**
 * Identifies and encodes shader properties needed for a given material and quad.
 */
public abstract class ShaderProps {
    ShaderProps() {}
    
    public static final int WHITE_0 = 1;
    public static final int WHITE_1 = 2;
    public static final int WHITE_2 = 4;
    public static final int FACENORMAL = 8;
    public static final int PIXEL_ALIGNED = 16;
    public static final int NO_AO = 32;
    public static final int SIMPLE_SKYLIGHT = 64;
    public static final int SIMPLE_BLOCKLIGHT = 128;
    public static final int CUTOUT_SPLIT = 256;
    public static final int SMOOTH_LIGHTMAPS = 512;
    
    private static final int FLAGS_LENGTH = 10;
    
    public static final int BITLENGTH = FLAGS_LENGTH + 2;
    
    public static int classify(RenderMaterialImpl.Value material, QuadViewImpl quad, ShaderContext context) {
        int flags = 0;
        
        if(Configurator.enableCompactGPUFormats && context.isBlock) {
            boolean white0 = true;
            for(int i = 0; i < 4; i++) {
                if(white0 && quad.spriteColor(i, 0) != -1) {
                    white0 = false;
                    break;
                }
            }
            if(white0) {
                flags |= WHITE_0;
            }
        }
        
        // PERF - sucks
        if(context.isBlock && quad.blockLight != null && quad.skyLight != null && quad.aoShade != null) {
            flags |= SMOOTH_LIGHTMAPS;
        }
        
        return (material.spriteDepth() << FLAGS_LENGTH) | flags;
    }
    
    public static int spriteDepth(int props) {
        assert ((props >> FLAGS_LENGTH) & 3) > 0;
        return (props >> FLAGS_LENGTH) & 3;
    }
    
    public static int waterProps() {
        return 1 << FLAGS_LENGTH;
    }
}
