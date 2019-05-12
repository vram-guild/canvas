package grondag.canvas.light;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToLongFunction;

import grondag.canvas.Canvas;
import grondag.canvas.apiimpl.QuadViewImpl;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class LightmapHd {
    public static final int TEX_SIZE = 4096;
    private static final int LIGHTMAP_SIZE = 4;
    public static final int PADDED_SIZE = LIGHTMAP_SIZE + 2;
    private static final int RADIUS = LIGHTMAP_SIZE / 2;
    private static final int LIGHTMAP_PIXELS = PADDED_SIZE * PADDED_SIZE;
    private static final int MAPS_PER_AXIS = TEX_SIZE / PADDED_SIZE;
    private static final int MAX_COUNT = MAPS_PER_AXIS * MAPS_PER_AXIS;
    // UGLY - consider making this a full unsigned short
    // for initial pass didn't want to worry about signed value mistakes
    /** Scale of texture units sent to shader. Shader should divide by this. */
    private static final int BUFFER_SCALE = 0x8000;
    private static final float TEXTURE_TO_BUFFER = (float) BUFFER_SCALE / TEX_SIZE;
    
    private static final AtomicInteger nextIndex = new AtomicInteger();
    
    public static void forceReload() {
        nextIndex.set(0);
        MAP.clear();
    }
    
    static final Long2ObjectOpenHashMap<LightmapHd> MAP = new Long2ObjectOpenHashMap<>();
    
    public static LightmapHd findBlock(AoFaceData faceData) {
        return find(faceData, LightmapHd::mapBlock);
    }
    
    public static LightmapHd findSky(AoFaceData faceData) {
        return find(faceData, LightmapHd::mapSky);
    }
    
    public static LightmapHd findAo(AoFaceData faceData) {
        return find(faceData, LightmapHd::mapAo);
    }
    
    private static long mapBlock(AoFaceData faceData) {
        return LightKey.toKey(
            faceData.top & 0xFF,
            faceData.left & 0xFF,
            faceData.right & 0xFF,
            faceData.bottom & 0xFF,
            faceData.topLeft & 0xFF,
            faceData.topRight & 0xFF,
            faceData.bottomLeft & 0xFF,
            faceData.bottomRight & 0xFF,
            faceData.center & 0xFF,
            false
        );
    }
    
    private static long mapSky(AoFaceData faceData) {
        return LightKey.toKey(
            (faceData.top >>> 16) & 0xFF,
            (faceData.left >>> 16) & 0xFF,
            (faceData.right >>> 16) & 0xFF,
            (faceData.bottom >>> 16) & 0xFF,
            (faceData.topLeft >>> 16) & 0xFF,
            (faceData.topRight >>> 16) & 0xFF,
            (faceData.bottomLeft >>> 16) & 0xFF,
            (faceData.bottomRight >>> 16) & 0xFF,
            (faceData.center >>> 16) & 0xFF,
            false
        );
    }
    
    private static long mapAo(AoFaceData faceData) {
        return LightKey.toKey(
            faceData.top & 0xFF,
            faceData.left & 0xFF,
            faceData.right & 0xFF,
            faceData.bottom & 0xFF,
            faceData.topLeft & 0xFF,
            faceData.topRight & 0xFF,
            faceData.bottomLeft & 0xFF,
            faceData.bottomRight & 0xFF,
            faceData.center & 0xFF,
            true
        );
    }
    
    // PERF: can reduce texture consumption 8X by reusing rotations/inversions 
    private static LightmapHd find(AoFaceData faceData, ToLongFunction<AoFaceData> mapper) {
        long key = mapper.applyAsLong(faceData);
        
        LightmapHd result = MAP.get(key);
        
        if(result == null) {
            synchronized(MAP) {
                result = MAP.get(key);
                if(result == null) {
                    result = new LightmapHd(key);
                    MAP.put(key, result);
                }
            }
        }
        
        return result;
    }
    
    private static float input(int b) {
        b &= 0xFF;
        if(b == 0xFF) {
            return AoFaceData.OPAQUE;
        }
        if(b > 240) {
            b = 240;
        }
        return b / 16f;
    }
    
    public final int uMinImg;
    public final int vMinImg;
    private final int[] light;
    
    private LightmapHd(long key) {
        final int index = nextIndex.getAndIncrement();
        final int s = index % MAPS_PER_AXIS;
        final int t = index / MAPS_PER_AXIS;
        uMinImg = s * PADDED_SIZE;
        vMinImg = t * PADDED_SIZE;
        // PERF: light data could be repooled once uploaded - not needed after
        // or simply output to the texture directly
        this.light = new int[LIGHTMAP_PIXELS];
        
        if(index >= MAX_COUNT) {
            //TODO: put back and/or handle better
            //assert false : "Out of lightmap space.";
            Canvas.LOG.info("Out of lightmap space for index = " + index);
            return;
        }
        
        compute(light, key, index);
        
        LightmapHdTexture.instance().setDirty(this);
    }
    
    /** converts zero-based distance from center to u/v index - use for top/left */
    private static final Int2IntFunction NEG = i -> RADIUS - i;
    /** converts zero-based distance from center to u/v index - use for bottom/right */
    private static final Int2IntFunction POS = i -> RADIUS + 1 + i;
    
    private static void compute(int[] light, long key, int index) {
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
        //FIX: handle error case when center is missing
        if(uSide == AoFaceData.OPAQUE) {
            if(vSide == AoFaceData.OPAQUE) {
                // fully enclosed
                computeOpen(center, center - 0.5f, center - 0.5f, center - 0.5f, light, uFunc, vFunc);
            } else if (corner == AoFaceData.OPAQUE) {
                // U + corner enclosing
                computeOpen(center, center - 0.5f, vSide, vSide - 0.5f, light, uFunc, vFunc);
            } else {
                // U side enclosing
                computeOpaqueU(center, vSide, corner, light, uFunc, vFunc);
            }
        } else if(vSide == AoFaceData.OPAQUE) {
            if(corner == AoFaceData.OPAQUE) {
                // V + corner enclosing
                computeOpen(center, uSide, center - 0.5f, uSide - 0.5f, light, uFunc, vFunc);
            } else {
                // V side enclosing
                computeOpaqueV(center, uSide, corner, light, uFunc, vFunc);
            }
            
        } else if(corner == AoFaceData.OPAQUE) {
            // opaque corner
            computeOpaqueCorner(center, uSide, vSide, light, uFunc, vFunc);
            
        } else {
            // all open
            computeOpen(center, uSide, vSide, corner, light, uFunc, vFunc);
        }
    }
    
    private static void computeOpen(float center, float uSide, float vSide, float corner, int light[], Int2IntFunction uFunc, Int2IntFunction vFunc) {
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
    
    private static void computeOpaqueU(float center, float vSide, float corner, int light[], Int2IntFunction uFunc, Int2IntFunction vFunc) {
        //  Layout  S = self, C = corner
        //  V C V
        //  S x S
        //  V C V
        
        LightmapCornerHelper help = LightmapCornerHelper.prepareThreadlocal(corner, center, vSide);
        
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
    
    private static void computeOpaqueV(float center, float uSide, float corner, int light[], Int2IntFunction uFunc, Int2IntFunction vFunc) {
        //  Layout  S = self, C = corner
        //  U S U
        //  C x C
        //  U S U
        //  
        LightmapCornerHelper help = LightmapCornerHelper.prepareThreadlocal(center, corner, uSide);
        
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
    
    private static void computeOpaqueCorner(float center, float uSide, float vSide, int light[], Int2IntFunction uFunc, Int2IntFunction vFunc) {
        LightmapCornerHelper help = LightmapCornerHelper.prepareThreadlocal(uSide, vSide, center);
        
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
    
    
    private static int lightIndex(int u, int v) {
        return v * PADDED_SIZE + u;
    }
    
    private static int output(float in) {
        int result = Math.round(in * 17f);
        
        if(result < 0) {
            result = 0;
        } else if(result > 255) {
            result = 255;
        }
        return result;
    }
    
    /**
     * Handles padding
     */
    public int pixel(int u, int v) {
        return light[v * PADDED_SIZE + u];
    }
    
    public int coord(QuadViewImpl q, int i) {
        int u = Math.round((uMinImg + 1  + q.u[i] * LIGHTMAP_SIZE) * TEXTURE_TO_BUFFER);
        int v = Math.round((vMinImg + 1  + q.v[i] * LIGHTMAP_SIZE) * TEXTURE_TO_BUFFER);
        
        return u | (v << 16);
    }
}
