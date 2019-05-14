package grondag.canvas.light;

import static grondag.canvas.light.LightmapHd.*;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.util.math.MathHelper;

final class AoMapHd {
    
    private static float AO_OPAQUE = AoFaceData.OPAQUE / 240f;
    
    static float OPAQUE_PROXY = 0.2f;
    
    static float input(int b) {
        return b < 0 ? OPAQUE_PROXY : b / 240f;
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

        final float TL = (center + top + left + topLeft) * 0.25f;
        final float TR = (center + top + right + topRight) * 0.25f;
        final float BL = (center + bottom + left + bottomLeft) * 0.25f;
        final float BR = (center + bottom + right + bottomRight) * 0.25f;
        
        for(int u = 0; u < PADDED_SIZE; u++) {
            for(int v = 0; v < PADDED_SIZE; v++) {
                float uDist = (float)u / AO_SIZE;
                float vDist = (float)v / AO_SIZE;
                
                float tl = (1 - uDist) * (1 - vDist) * TL;
                float tr = uDist * (1 - vDist) * TR;
                float br = uDist * vDist * BR;
                float bl = (1 - uDist) * vDist * BL;
                light[lightIndex(u, v)] = output(tl + tr + br + bl);
            }
        }
//        
//        // Note: won't work for other than 4x4 interior, 6x6 padded
//        computeQuadrant(center, left, top, topLeft, light, NEG, NEG);
//        computeQuadrant(center, right, top, topRight, light, POS, NEG);
//        computeQuadrant(center, left, bottom, bottomLeft, light, NEG, POS);
//        computeQuadrant(center, right, bottom, bottomRight, light, POS, POS);        
    }
    
    
    private static void computeQuadrant(float center, float uSide, float vSide, float corner, int light[], Int2IntFunction uFunc, Int2IntFunction vFunc) {
        //FIX: handle error case when center is missing
        if(uSide == AO_OPAQUE) {
            if(vSide == AO_OPAQUE) {
                // fully enclosed
                computeOpen(center, OPAQUE_PROXY, OPAQUE_PROXY, OPAQUE_PROXY, light, uFunc, vFunc);
            } else if (corner == AO_OPAQUE) {
                // U + corner enclosing
                computeOpen(center, OPAQUE_PROXY, vSide, OPAQUE_PROXY, light, uFunc, vFunc);
            } else {
                // U side enclosing
                computeOpaqueU(center, vSide, corner, light, uFunc, vFunc);
            }
        } else if(vSide == AO_OPAQUE) {
            if(corner == AO_OPAQUE) {
                // V + corner enclosing
                computeOpen(center, uSide, OPAQUE_PROXY, OPAQUE_PROXY, light, uFunc, vFunc);
            } else {
                // V side enclosing
                computeOpaqueV(center, uSide, corner, light, uFunc, vFunc);
            }
            
        } else if(corner == AO_OPAQUE) {
            // opaque corner
            computeOpaqueCorner(center, uSide, vSide, light, uFunc, vFunc);
            
        } else {
            // all open
            computeOpen(center, uSide, vSide, corner, light, uFunc, vFunc);
        }
    }
    
    static void computeOpen(float center, float uSide, float vSide, float corner, int light[], Int2IntFunction uFunc, Int2IntFunction vFunc) {
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
    
    static void computeOpaqueU(float center, float vSide, float corner, int light[], Int2IntFunction uFunc, Int2IntFunction vFunc) {
        //  Layout  S = self, C = corner
        //  V C V
        //  S x S
        //  V C V
        
        AoCornerHelper help = AoCornerHelper.prepareThreadlocal(corner, center, vSide);
        
        //  F G H
        //  J K L
        //  M N O
        light[lightIndex(uFunc.applyAsInt(0), vFunc.applyAsInt(0))] = output(help.o());
        light[lightIndex(uFunc.applyAsInt(1), vFunc.applyAsInt(0))] = output(help.n());
        light[lightIndex(uFunc.applyAsInt(2), vFunc.applyAsInt(0))] = output(help.m());
        
        light[lightIndex(uFunc.applyAsInt(0), vFunc.applyAsInt(1))] = output(help.l());
        light[lightIndex(uFunc.applyAsInt(1), vFunc.applyAsInt(1))] = output(help.k());
        light[lightIndex(uFunc.applyAsInt(2), vFunc.applyAsInt(1))] = output(help.j());
        
        light[lightIndex(uFunc.applyAsInt(0), vFunc.applyAsInt(2))] = output(help.h());
        light[lightIndex(uFunc.applyAsInt(1), vFunc.applyAsInt(2))] = output(help.g());
        light[lightIndex(uFunc.applyAsInt(2), vFunc.applyAsInt(2))] = output(help.f());
        
    }
    
    static void computeOpaqueV(float center, float uSide, float corner, int light[], Int2IntFunction uFunc, Int2IntFunction vFunc) {
        //  Layout  S = self, C = corner
        //  U S U
        //  C x C
        //  U S U
        //  
        AoCornerHelper help = AoCornerHelper.prepareThreadlocal(center, corner, uSide);
        
        //  A B C
        //  E F G
        //  I J K
        light[lightIndex(uFunc.applyAsInt(0), vFunc.applyAsInt(0))] = output(help.a());
        light[lightIndex(uFunc.applyAsInt(1), vFunc.applyAsInt(0))] = output(help.b());
        light[lightIndex(uFunc.applyAsInt(2), vFunc.applyAsInt(0))] = output(help.c());
        
        light[lightIndex(uFunc.applyAsInt(0), vFunc.applyAsInt(1))] = output(help.e());
        light[lightIndex(uFunc.applyAsInt(1), vFunc.applyAsInt(1))] = output(help.f());
        light[lightIndex(uFunc.applyAsInt(2), vFunc.applyAsInt(1))] = output(help.g());
        
        light[lightIndex(uFunc.applyAsInt(0), vFunc.applyAsInt(2))] = output(help.i());
        light[lightIndex(uFunc.applyAsInt(1), vFunc.applyAsInt(2))] = output(help.j());
        light[lightIndex(uFunc.applyAsInt(2), vFunc.applyAsInt(2))] = output(help.k());
    }
    
    static void computeOpaqueCorner(float center, float uSide, float vSide, int light[], Int2IntFunction uFunc, Int2IntFunction vFunc) {
        AoCornerHelper help = AoCornerHelper.prepareThreadlocal(uSide, vSide, center);
        
        //  Layout
        //  U C
        //  x V
        
        //  B C D
        //  F G H
        //  J K L
        light[lightIndex(uFunc.applyAsInt(0), vFunc.applyAsInt(0))] = output(help.d());
        light[lightIndex(uFunc.applyAsInt(1), vFunc.applyAsInt(0))] = output(help.c());
        light[lightIndex(uFunc.applyAsInt(2), vFunc.applyAsInt(0))] = output(help.b());
        
        light[lightIndex(uFunc.applyAsInt(0), vFunc.applyAsInt(1))] = output(help.h());
        light[lightIndex(uFunc.applyAsInt(1), vFunc.applyAsInt(1))] = output(help.g());
        light[lightIndex(uFunc.applyAsInt(2), vFunc.applyAsInt(1))] = output(help.f());
        
        light[lightIndex(uFunc.applyAsInt(0), vFunc.applyAsInt(2))] = output(help.l());
        light[lightIndex(uFunc.applyAsInt(1), vFunc.applyAsInt(2))] = output(help.k());
        light[lightIndex(uFunc.applyAsInt(2), vFunc.applyAsInt(2))] = output(help.j());
    }
    
    static int output(float in) {
        in = 1 - in;
//        in  = in * in;
        
        in = in * in * in * (in * (in * 6 - 15) + 10);
        in = 1 - in;
        return MathHelper.clamp(Math.round(in * 255), 0, 255);
    }
}
