package grondag.canvas.varia;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import grondag.canvas.Canvas;
import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.canvas.apiimpl.util.AoFaceData;
import grondag.fermion.varia.Useful;

public class LightmapHD {
    public static final int TEX_SIZE = 4096;
    private static final int LIGHTMAP_SIZE = 6;
    public static final int PADDED_SIZE = LIGHTMAP_SIZE + 2;
    public static final int PADDED_MARGIN = LIGHTMAP_SIZE / 2;
    private static final int RADIUS = LIGHTMAP_SIZE / 2;
    private static final int LIGHTMAP_PIXELS = PADDED_SIZE * PADDED_SIZE;
    private static final int IDX_SIZE = TEX_SIZE / PADDED_SIZE;
    private static final int MAX_COUNT = IDX_SIZE * IDX_SIZE;
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
            long l0 = center | (top << 8) | (left << 16) | (right << 24) | (bottom << 32);
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
    
    private static LightmapHD find(AoFaceData faceData, BiConsumer<AoFaceData, Key> mapper) {
        Key key = TEMPLATES.get();
        
        mapper.accept(faceData, key);

        key.computeHash();
        
        LightmapHD result = MAP.get(key);
        
        if(result == null) {
            // create new key object to avoid putting threadlocal into map
            key = new Key(key);
            result = MAP.computeIfAbsent(key, k -> new LightmapHD(k));
        }
        
        return result;
    }
    
    private static float input(int b) {
        b &= 0xFF;
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
        final int s = index % IDX_SIZE;
        final int t = index / IDX_SIZE;
        uMinImg = s * PADDED_SIZE;
        vMinImg = t * PADDED_SIZE;
        this.light = new int[LIGHTMAP_PIXELS];
        
        if(index >= MAX_COUNT) {
            //TODO: put back
            //assert false : "Out of lightmap space.";
            Canvas.LOG.info("Out of lightmap space for index = " + index);
            return;
        }
        
        compute(light, key);
        
        LightmapHdTexture.instance().setDirty(this);
    }
    
    private static void compute(int[] light, Key key) {
        final float center = input(key.center);
        final float top = input(key.top);
        final float bottom = input(key.bottom);
        final float right = input(key.right);
        final float left = input(key.left);
        final float topLeft = input(key.topLeft);
        final float topRight = input(key.topRight);
        final float bottomRight = input(key.bottomRight);
        final float bottomLeft = input(key.bottomLeft);

        for(int i = -1; i < RADIUS; i++) {
            for(int j = -1; j < RADIUS; j++) {
                light[lightIndex(i, j)] = output(inside(center, left, top, topLeft, i, j));
                light[lightIndex(RADIUS + 1 + i, j)] = output(inside(center, right, top, topRight, RADIUS + 1 + i, j));
                light[lightIndex(RADIUS + 1 + i, RADIUS + 1 + j)] = output(inside(center, right, bottom, bottomRight, RADIUS + 1 + i, RADIUS + 1 + j));
                light[lightIndex(i, RADIUS + 1 + j)] = output(inside(center, left, bottom, bottomLeft, i, RADIUS + 1 + j));
            }
        }
    }        
    
    
    private static int lightIndex(int u, int v) {
        return (v + 1) * PADDED_SIZE + u + 1;
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
        int u = Math.round((uMinImg + 1) * TEXTURE_TO_BUFFER  + q.u[i] * (LIGHTMAP_SIZE * TEXTURE_TO_BUFFER));
        int v = Math.round((vMinImg + 1) * TEXTURE_TO_BUFFER  + q.v[i] * (LIGHTMAP_SIZE * TEXTURE_TO_BUFFER));
        
        
        return u | (v << 16);
    }
    
    
    private static float inside(float self, float uVal, float vVal, float cornerVal, int u, int v) {
        if(self == uVal && self == vVal && self == cornerVal) {
            return cornerVal;
        }
        int du = pixelDist(u);
        int dv = pixelDist(v);
        
        float nz = nonZeroMin(self, uVal, vVal, cornerVal);
        if(self == 0) self = nz;
        if(cornerVal == 0) cornerVal = nz;
        if(uVal == 0) uVal = nz;
        if(vVal == 0) vVal = nz;
        
        float uLinear = 1f - (du + 0.5f) / LIGHTMAP_SIZE;
        float vLinear = 1f - (dv + 0.5f) / LIGHTMAP_SIZE;
        assert uLinear >= 0 && uLinear <= 1f;
        assert vLinear >= 0 && vLinear <= 1f;
        float linear = self * (uLinear * vLinear) 
                + cornerVal * (1 - uLinear) * (1 - vLinear)
                + uVal * ((1 - uLinear) * (vLinear))
                + vVal * ((uLinear) * (1 - vLinear));
        
        return linear;
    }
    
    static final int CELL_DISTANCE = RADIUS * 2 - 1;
    static final float INVERSE_CELL_DISTANCE = 1f / CELL_DISTANCE;
    
    private static int pixelDist(int c) {
        return c >= RADIUS ? c - RADIUS : RADIUS - 1 - c;
    }
    
    private static float nonZeroMin(float a, float b) {
        if(a == 0) return b;
        if(b == 0) return a;
        return Math.min(a, b);
    }
    
    private static float nonZeroMin(float a, float b, float c, float d) {
        return nonZeroMin(nonZeroMin(a, b), nonZeroMin(c, d));
    }
}
