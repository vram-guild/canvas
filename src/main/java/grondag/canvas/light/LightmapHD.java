package grondag.canvas.light;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import grondag.canvas.Canvas;
import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.canvas.apiimpl.util.AoFaceData;
import grondag.fermion.varia.Useful;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;

public class LightmapHD {
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
    
    private static class Key {
        private int top;
        private int bottom;
        private int right;
        private int left;
        private int topLeft;
        private int topRight;
        private int bottomRight;
        private int bottomLeft;
        private int center;
        
        private int hashCode;
        
        Key() {
        }

        Key(Key k) {
            this.center = k.center;
            this.top = k.top;
            this.left = k.left;
            this.right = k.right;
            this.bottom = k.bottom;
            this.topLeft = k.topLeft;
            this.topRight = k.topRight;
            this.bottomLeft = k.bottomLeft;
            this.bottomRight = k.bottomRight;
            computeHash();
        }

        // PERF - if last bit always zero for light values can fit this in a single long
        // and use a long hash table directly
        
        /**
         * Call after mutating {@link #light}
         */
        void computeHash() {
            long l0 = center | (top << 8) | (left << 16) | (right << 24) | ((long)bottom << 32);
            long l1 = topLeft | (topRight << 8) | (bottomLeft << 16) | (bottomRight << 24);
            this.hashCode = Long.hashCode(Useful.longHash(l0) ^ Useful.longHash(l1));
        }
        
        @Override
        public boolean equals(Object other) {
            if(other == null || !(other instanceof Key)) {
                return false;
            }
            Key k = (Key)other;
            return this.center == k.center
                    && this.top == k.top
                    && this.left == k.left
                    && this.right == k.right
                    && this.bottom == k.bottom
                    && this.topLeft == k.topLeft
                    && this.topRight == k.topRight
                    && this.bottomLeft == k.bottomLeft
                    && this.bottomRight == k.bottomRight;
        }
        
        @Override
        public int hashCode() {
            return hashCode;
        }
    }
    
    static final ThreadLocal<Key> TEMPLATES = ThreadLocal.withInitial(Key::new);
    
    static final ConcurrentHashMap<Key, LightmapHD> MAP = new ConcurrentHashMap<>();
    
    public static LightmapHD findBlock(AoFaceData faceData) {
        return find(faceData, LightmapHD::mapBlock);
    }
    
    public static LightmapHD findSky(AoFaceData faceData) {
        return find(faceData, LightmapHD::mapSky);
    }
    
    //PERF: quantize values to reduce consumption
    private static void mapBlock(AoFaceData faceData, Key key) {
        key.top = faceData.top & 0xFF;
        key.bottom = faceData.bottom & 0xFF;
        key.right = faceData.right & 0xFF;
        key.left = faceData.left & 0xFF;
        key.topLeft = faceData.topLeft & 0xFF;
        key.topRight = faceData.topRight & 0xFF;
        key.bottomRight = faceData.bottomRight & 0xFF;
        key.bottomLeft = faceData.bottomLeft & 0xFF;
        key.center = faceData.center & 0xFF;
    }
    
  //PERF: quantize values to reduce consumption
    private static void mapSky(AoFaceData faceData, Key key) {
        key.top = (faceData.top >>> 16) & 0xFF;
        key.bottom = (faceData.bottom >>> 16) & 0xFF;
        key.right = (faceData.right >>> 16) & 0xFF;
        key.left = (faceData.left >>> 16) & 0xFF;
        key.topLeft = (faceData.topLeft >>> 16) & 0xFF;
        key.topRight = (faceData.topRight >>> 16) & 0xFF;
        key.bottomRight = (faceData.bottomRight >>> 16) & 0xFF;
        key.bottomLeft = (faceData.bottomLeft >>> 16) & 0xFF;
        key.center = (faceData.center >>> 16) & 0xFF;
    }
    
    // PERF: can reduce texture consumption 8X by reusing rotations/inversions 
    private static LightmapHD find(AoFaceData faceData, BiConsumer<AoFaceData, Key> mapper) {
        Key key = TEMPLATES.get();
        mapper.accept(faceData, key);

        key.computeHash();
        
        LightmapHD result = MAP.get(key);
        
        if(result == null) {
            // create new key object to avoid putting threadlocal into map
            Key key2 = new Key(key);
            result = MAP.computeIfAbsent(key2, k -> new LightmapHD(k));
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
    
    private LightmapHD(Key key) {
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
    
    private static void compute(int[] light, Key key, int index) {
        // PERF: use integer math
        final float center = input(key.center);
        final float top = input(key.top);
        final float bottom = input(key.bottom);
        final float right = input(key.right);
        final float left = input(key.left);
        final float topLeft = input(key.topLeft);
        final float topRight = input(key.topRight);
        final float bottomRight = input(key.bottomRight);
        final float bottomLeft = input(key.bottomLeft);

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
//                computeOpaqueAll(center, light, uFunc, vFunc);
            } else if (corner == AoFaceData.OPAQUE) {
                // U + corner enclosing
//                computeOpenV(center, vSide, light, uFunc, vFunc);
                computeOpen(center, center - 0.5f, vSide, vSide - 0.5f, light, uFunc, vFunc);
            } else {
                // U side enclosing
                computeOpaqueU(center, vSide, corner, light, uFunc, vFunc);
            }
        } else if(vSide == AoFaceData.OPAQUE) {
            if(corner == AoFaceData.OPAQUE) {
                // V + corner enclosing
//                computeOpenU(center, uSide, light, uFunc, vFunc);
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
        //PERF could compress coordinates sent to shader by 
        // sending lightmap/shademap index with same uv for each
        // would probably save 2 bytes - send 2 + 3 shorts instead of 6
        // or put each block/sky/ao combination in a texture and send 4 shorts...
        // 2 for uv and 2 to lookup the combinations
//        float u = uMinImg + 0.5f + q.u[i] * (LIGHTMAP_SIZE - 1);
//        float v = vMinImg + 0.5f + q.v[i] * (LIGHTMAP_SIZE - 1);
        int u = Math.round((uMinImg + 1  + q.u[i] * LIGHTMAP_SIZE) * TEXTURE_TO_BUFFER);
        int v = Math.round((vMinImg + 1  + q.v[i] * LIGHTMAP_SIZE) * TEXTURE_TO_BUFFER);
        
        return u | (v << 16);
    }
}
