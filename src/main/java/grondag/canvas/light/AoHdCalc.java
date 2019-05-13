package grondag.canvas.light;

import static grondag.canvas.light.LightmapHd.*;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.util.math.MathHelper;

final class AoHdCalc {
    static float input(int b) {
        return b / 240f;
    }
    
    static void computeAo(int[] light, long key, int index) {
        // PERF: use integer math
        final float center = input(LightKey.center(key));
        final float top = input(LightKey.top(key));
        final float bottom = input(LightKey.bottom(key));
        final float right = input(LightKey.right(key));
        final float left = input(LightKey.left(key));
        final float topLeft = input(LightKey.topLeft(key));
        final float topRight = input(LightKey.topRight(key));
        final float bottomRight = input(LightKey.bottomRight(key));
        final float bottomLeft = input(LightKey.bottomLeft(key));

        // Note: won't work for other than 4x4 interior, 6x6 padded
        computeQuadrant(center, left, top, topLeft, light, NEG, NEG);
        computeQuadrant(center, right, top, topRight, light, POS, NEG);
        computeQuadrant(center, left, bottom, bottomLeft, light, NEG, POS);
        computeQuadrant(center, right, bottom, bottomRight, light, POS, POS);        
    }
    
    private static void computeQuadrant(float center, float uSide, float vSide, float corner, int light[], Int2IntFunction uFunc, Int2IntFunction vFunc) {
        for(int u = 0; u <= RADIUS; u++) {
            for(int v = 0; v <= RADIUS; v++) {
                final float uLinear = 1f - (u + 0.5f) / LIGHTMAP_SIZE;
                final float vLinear = 1f - (v + 0.5f) / LIGHTMAP_SIZE;
                
                assert uLinear >= 0 && uLinear <= 1f;
                assert vLinear >= 0 && vLinear <= 1f;
                
                float linear = center * (uLinear * vLinear) 
                        + corner * (1 - uLinear) * (1 - vLinear)
                        + uSide * ((1 - uLinear) * (vLinear))
                        + vSide * ((uLinear) * (1 - vLinear));
                
                light[lightIndex(uFunc.applyAsInt(u), vFunc.applyAsInt(v))] = output(linear);
            }
        }
    }
    
    static int output(float in) {
        return MathHelper.clamp(Math.round(in * 255), 0, 255);
    }
}
