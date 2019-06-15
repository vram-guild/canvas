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

import java.nio.ByteOrder;

import grondag.canvas.apiimpl.MutableQuadViewImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;

/**
 * Static routines of general utility for renderer implementations. Renderers
 * are not required to use these helpers, but they were designed to be usable
 * without the default renderer.
 */
public abstract class ColorHelper {
    private ColorHelper() {
    }

    private static final Int2IntFunction colorSwapper = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN
            ? color -> ((color & 0xFF00FF00) | ((color & 0x00FF0000) >> 16) | ((color & 0xFF) << 16))
            : color -> color;

    /**
     * Swaps red blue order if needed to match GPU expectations for color component
     * order.
     */
    public static int swapRedBlueIfNeeded(int color) {
        return colorSwapper.applyAsInt(color);
    }

    /** arguments are assumed to be ARGB - does not modify alpha */
    public static int multiplyRGB(int color, float shade) {
        int red = (int) (((color >> 16) & 0xFF) * shade);
        int green = (int) (((color >> 8) & 0xFF) * shade);
        int blue = (int) ((color & 0xFF) * shade);
        int alpha = ((color >> 24) & 0xFF);

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
    
    /**
     * Component-wise multiply. Components need to be in same order in both inputs!
     */
    public static int multiplyColor(int color1, int color2) {
        if (color1 == -1) {
            return color2;
        } else if (color2 == -1) {
            return color1;
        }

        int alpha = ((color1 >> 24) & 0xFF) * ((color2 >> 24) & 0xFF) / 0xFF;
        int red = ((color1 >> 16) & 0xFF) * ((color2 >> 16) & 0xFF) / 0xFF;
        int green = ((color1 >> 8) & 0xFF) * ((color2 >> 8) & 0xFF) / 0xFF;
        int blue = (color1 & 0xFF) * (color2 & 0xFF) / 0xFF;

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    @FunctionalInterface
    private static interface Colorizer {
        void shade(MutableQuadViewImpl quad, int vertexIndex, int color);
    }

    private static Colorizer[][] COLORIZERS = new Colorizer[3][8];

    static {
        COLORIZERS[0][0b000] = (q, i, s) -> q.spriteColor(i, 0, swapRedBlueIfNeeded(q.spriteColor(i, 0)));
  
        COLORIZERS[0][0b001] = (q, i, s) -> q.spriteColor(i, 0, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 0), s)));
        
        COLORIZERS[1][0b000] = (q, i, s) -> 
            q.spriteColor(i, 0, swapRedBlueIfNeeded(q.spriteColor(i, 0)))
             .spriteColor(i, 1, swapRedBlueIfNeeded(q.spriteColor(i, 1)));
        
        COLORIZERS[1][0b001] = (q, i, s) -> 
            q.spriteColor(i, 0, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 0), s)))
             .spriteColor(i, 1, swapRedBlueIfNeeded(q.spriteColor(i, 1)));
        
        COLORIZERS[1][0b010] = (q, i, s) -> 
            q.spriteColor(i, 0, swapRedBlueIfNeeded(q.spriteColor(i, 0)))
             .spriteColor(i, 1, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 1), s)));
        COLORIZERS[1][0b011] = (q, i, s) -> 
            q.spriteColor(i, 0, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 0), s)))
             .spriteColor(i, 1, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 1), s)));
        
        COLORIZERS[2][0b000] = (q, i, s) -> 
            q.spriteColor(i, 0, swapRedBlueIfNeeded(q.spriteColor(i, 0)))
             .spriteColor(i, 1, swapRedBlueIfNeeded(q.spriteColor(i, 1)))
             .spriteColor(i, 2, swapRedBlueIfNeeded(q.spriteColor(i, 2)));
        
        COLORIZERS[2][0b001] = (q, i, s) -> 
            q.spriteColor(i, 0, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 0), s)))
             .spriteColor(i, 1, swapRedBlueIfNeeded(q.spriteColor(i, 1)))
             .spriteColor(i, 2, swapRedBlueIfNeeded(q.spriteColor(i, 2)));
        
        COLORIZERS[2][0b010] = (q, i, s) -> 
            q.spriteColor(i, 0, swapRedBlueIfNeeded(q.spriteColor(i, 0)))
             .spriteColor(i, 1, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 1), s)))
             .spriteColor(i, 2, swapRedBlueIfNeeded(q.spriteColor(i, 2)));
        
        COLORIZERS[2][0b011] = (q, i, s) -> 
            q.spriteColor(i, 0, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 0), s)))
             .spriteColor(i, 1, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 1), s)))
             .spriteColor(i, 2, swapRedBlueIfNeeded(q.spriteColor(i, 2)));
        
        COLORIZERS[2][0b100] = (q, i, s) -> 
            q.spriteColor(i, 0, swapRedBlueIfNeeded(q.spriteColor(i, 0)))
             .spriteColor(i, 1, swapRedBlueIfNeeded(q.spriteColor(i, 1)))
             .spriteColor(i, 2, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 2), s)));
        
        COLORIZERS[2][0b101] = (q, i, s) -> 
            q.spriteColor(i, 0, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 0), s)))
             .spriteColor(i, 1, swapRedBlueIfNeeded(q.spriteColor(i, 1)))
             .spriteColor(i, 2, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 2), s)));
        
        COLORIZERS[2][0b110] = (q, i, s) -> 
            q.spriteColor(i, 0, swapRedBlueIfNeeded(q.spriteColor(i, 0)))
             .spriteColor(i, 1, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 1), s)))
             .spriteColor(i, 2, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 2), s)));
            
        COLORIZERS[2][0b111] = (q, i, s) -> 
            q.spriteColor(i, 0, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 0), s)))
             .spriteColor(i, 1, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 1), s)))
             .spriteColor(i, 2, swapRedBlueIfNeeded(multiplyColor(q.spriteColor(i, 2), s)));
    }

    public static void colorizeQuad(MutableQuadViewImpl quad, int color) {
        final RenderMaterialImpl mat = quad.material();
        final int depth = mat.spriteDepth();
        int flags = 0;

        if(quad.colorIndex() != -1) {
            if(!mat.disableColorIndex(0)) {
                flags = 1;
            }
            
            if(depth > 1) {
                if(!mat.disableColorIndex(1)) {
                    flags |= 2;
                }
                if(depth == 3 && !mat.disableColorIndex(2)) {
                    flags |= 4;
                }
            }
        }
        
        final Colorizer colorizer = COLORIZERS[depth - 1][flags];
        for (int i = 0; i < 4; i++) {
            colorizer.shade(quad, i, color);
        }
    }

    /**
     * Component-wise max
     */
    public static int maxBrightness(int b0, int b1) {
        if (b0 == 0)
            return b1;
        else if (b1 == 0)
            return b0;
        return Math.max(b0 & 0xFFFF, b1 & 0xFFFF) | Math.max(b0 & 0xFFFF0000, b1 & 0xFFFF0000);
    }
}
