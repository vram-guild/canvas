package grondag.canvas.varia;

import static grondag.canvas.apiimpl.util.AoFaceData.nonZeroMin;
import static grondag.canvas.apiimpl.util.AoFaceData.zif;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.canvas.apiimpl.util.LightFaceData;

public class LightmapHD {
    static final int TEX_SIZE = 512;
    static final int IDX_SIZE = 512 / 4;
    static final int MAX_COUNT = IDX_SIZE * IDX_SIZE;
    // UGLY - consider making this a full unsigned short
    // for initial pass didn't want to worry about signed value mistakes
    /** Scale of texture units sent to shader. Shader should divide by this. */
    static final int BUFFER_SCALE = 0x8000;
    static final int UNITS_PER_PIXEL = BUFFER_SCALE / TEX_SIZE;
    static final float TEXTURE_TO_BUFFER = (float) BUFFER_SCALE / TEX_SIZE;
    
    private static final LightmapHD[] maps = new LightmapHD[IDX_SIZE * IDX_SIZE];
    
    private static final AtomicInteger nextIndex = new AtomicInteger();
    
    public static void forceReload() {
        nextIndex.set(0);
    }
    
    public static void forEach(Consumer<LightmapHD> consumer) {
        final int limit = Math.min(MAX_COUNT, nextIndex.get());
        for(int i = 0; i < limit; i++) {
            consumer.accept(maps[i]);
        }
    }
    
    public final int uMinImg;
    public final int vMinImg;
    
    // PERF: too heavy
    public final float[][] sky = new float[4][4];
    public final float[][] block = new float[4][4];
    final float u0, u1, u2, u3, v0, v1, v2, v3;

    public LightmapHD(LightFaceData data) {
        final int index = nextIndex.getAndIncrement();
        final int s = index % IDX_SIZE;
        final int t = index / IDX_SIZE;
        uMinImg = s * 4;
        vMinImg = t * 4;
        
        u0 = (uMinImg + 1f);
        v0 = (vMinImg + 1f);
        u1 = (uMinImg + 3f);
        v1 = (vMinImg + 1f);
        u2 = (uMinImg + 3f);
        v2 = (vMinImg + 3f);
        u3 = (uMinImg + 1f);
        v3 = (vMinImg + 3f);
        
        if(index >= MAX_COUNT) {
            //TODO: put back
            //assert false : "Out of lightmap space.";
            return;
        }
        
        maps[index] = this;
        
        /**
         * Note that Ao data order is different from vertex order.
         * We will need to remap that here unless/until Ao data is simplified.
         * wc0 = s0 s3 c1
         * wc1 = s0 s2 c0
         * wc2 = s1 s2 c2
         * wc3 = s1 s3 c3
         * 
         * c1 s0 c0 s2 c2 s1 c3 s3
         * w0    w1    w2    w3
         * u0v0  u1v0  u1v1  u0v1
         */
        // quadrant 0, 0
        sky[1][1] = max(data.kCenter(), data.ks0() - 1f, data.ks3() - 1f, data.kc1() - 1.41f);
        sky[0][0] = corner(data.kCenter(), data.ks0(), data.ks3(), data.kc1());
        sky[0][1] = max(data.kCenter() - .33f, data.ks0() - 1.05f, data.ks3() - .67f, data.kc1() - 1.2f);
        sky[1][0] = max(data.kCenter() - .33f, data.ks3() - 1.05f, data.ks0() - .67f, data.kc1() - 1.2f);
        
        // quadrant 1, 0
//        min = nonZeroMin(data.kCenter, data.kc0, data.ks0, data.ks2);
        sky[2][1] = max(data.kCenter(), data.ks0() - 1f, data.ks2() - 1f, data.kc0() - 1.41f);
        sky[3][0] = corner(data.kCenter(), data.ks0(), data.ks2(), data.kc0());
        sky[2][0] = max(data.kCenter() - .33f, data.ks2() - 1.05f, data.ks0() - .67f, data.kc0() - 1.2f);
        sky[3][1] = max(data.kCenter() - .33f, data.ks0() - 1.05f, data.ks2() - .67f, data.kc0() - 1.2f);
        
//        sky[2][1] = zif(data.kCenter, min);
//        sky[3][0] = zif(data.kc0, min);
//        sky[2][0] = zif(data.ks0, min);
//        sky[3][1] = zif(data.ks2, min);
        
        // quadrant 1, 1
//        min = nonZeroMin(data.kCenter, data.kc2, data.ks1, data.ks2);
        
        sky[2][2] = max(data.kCenter(), data.ks1() - 1f, data.ks2() - 1f, data.kc2() - 1.41f);
        sky[3][3] = corner(data.kCenter(), data.ks1(), data.ks2(), data.kc2());
        sky[3][2] = max(data.kCenter() - .33f, data.ks1() - 1.05f, data.ks2() - .67f, data.kc2() - 1.2f);
        sky[2][3] = max(data.kCenter() - .33f, data.ks2() - 1.05f, data.ks1() - .67f, data.kc2() - 1.2f);
        
//        sky[2][2] = zif(data.kCenter, min);
//        sky[3][3] = zif(data.kc2, min);
//        sky[3][2] = zif(data.ks2, min);
//        sky[2][3] = zif(data.ks1, min);
        
        // quadrant 0, 1
//        min = nonZeroMin(data.kCenter, data.kc3, data.ks1, data.ks3);
        
        sky[1][2] = max(data.kCenter(), data.ks1() - 1f, data.ks3() - 1f, data.kc3() - 1.41f);
        sky[0][3] = corner(data.kCenter(), data.ks1(), data.ks3(), data.kc3());
        sky[1][3] = max(data.kCenter() - .33f, data.ks3() - 1.05f, data.ks1() - .67f, data.kc3() - 1.2f);
        sky[0][2] = max(data.kCenter() - .33f, data.ks1() - 1.05f, data.ks3() - .67f, data.kc3() - 1.2f);
        
//        sky[1][2] = zif(data.kCenter, min);
//        sky[0][3] = zif(data.kc3, min);
//        sky[1][3] = zif(data.ks1, min);
//        sky[0][2] = zif(data.ks3, min);
        
        // quadrant 0, 0
//        min = nonZeroMin(data.bCenter, data.bc1, data.bs0, data.bs3);
//        block[1][1] = zif(data.bCenter, min);
//        block[0][1] = zif(data.bs3, min);
//        block[0][0] = zif(data.bc1, min);
//        block[1][0] = zif(data.bs0, min);
        
        block[1][1] = max(data.bCenter(), data.bs0() - 1f, data.bs3() - 1f, data.bc1() - 1.41f);
        block[0][0] = corner(data.bCenter(), data.bs0(), data.bs3(), data.bc1());
        block[0][1] = max(data.bCenter() - .33f, data.bs0() - 1.05f, data.bs3() - .67f, data.bc1() - 1.2f);
        block[1][0] = max(data.bCenter() - .33f, data.bs3() - 1.05f, data.bs0() - .67f, data.bc1() - 1.2f);
        
        // quadrant 1, 0
//        min = nonZeroMin(data.bCenter, data.bc0, data.bs0, data.bs2);
//        block[2][1] = zif(data.bCenter, min);
//        block[2][0] = zif(data.bs0, min);
//        block[3][0] = zif(data.bc0, min);
//        block[3][1] = zif(data.bs2, min);
        
        block[2][1] = max(data.bCenter(), data.bs0() - 1f, data.bs2() - 1f, data.bc0() - 1.41f);
        block[3][0] = corner(data.bCenter(), data.bs0(), data.bs2(), data.bc0());
        block[2][0] = max(data.bCenter() - .33f, data.bs2() - 1.05f, data.bs0() - .67f, data.bc0() - 1.2f);
        block[3][1] = max(data.bCenter() - .33f, data.bs0() - 1.05f, data.bs2() - .67f, data.bc0() - 1.2f);
        
        // quadrant 1, 1
//        min = nonZeroMin(data.bCenter, data.bc2, data.bs1, data.bs2);
//        block[2][2] = zif(data.bCenter, min);
//        block[3][2] = zif(data.bs2, min);
//        block[3][3] = zif(data.bc2, min);
//        block[2][3] = zif(data.bs1, min);
        
        block[2][2] = max(data.bCenter(), data.bs1() - 1f, data.bs2() - 1f, data.bc2() - 1.41f);
        block[3][3] = corner(data.bCenter(), data.bs1(), data.bs2(), data.bc2());
        block[3][2] = max(data.bCenter() - .33f, data.bs1() - 1.05f, data.bs2() - .67f, data.bc2() - 1.2f);
        block[2][3] = max(data.bCenter() - .33f, data.bs2() - 1.05f, data.bs1() - .67f, data.bc2() - 1.2f);
        
        // quadrant 0, 1
//        min = nonZeroMin(data.bCenter, data.bc3, data.bs1, data.bs3);
//        block[1][2] = zif(data.bCenter, min);
//        block[0][3] = zif(data.bc3, min);
//        block[1][3] = zif(data.bs1, min);
//        block[0][2] = zif(data.bs3, min);
        
        block[1][2] = max(data.bCenter(), data.bs1() - 1f, data.bs3() - 1f, data.bc3() - 1.41f);
        block[0][3] = corner(data.bCenter(), data.bs1(), data.bs3(), data.bc3());
        block[1][3] = max(data.bCenter() - .33f, data.bs3() - 1.05f, data.bs1() - .67f, data.bc3() - 1.2f);
        block[0][2] = max(data.bCenter() - .33f, data.bs1() - 1.05f, data.bs3() - .67f, data.bc3() - 1.2f);
    }
    
    
    public final int coord(float[] w) {
        float u = u0 * w[0] + u1 * w[1] + u2 * w[2] + u3 * w[3];
        float v = v0 * w[0] + v1 * w[1] + v2 * w[2] + v3 * w[3];
        return Math.round(u * TEXTURE_TO_BUFFER) | (Math.round(v * TEXTURE_TO_BUFFER) << 16);
    }

    public int coord(QuadViewImpl q, int i) {
        float u = uMinImg + 0.5f + q.u[i] * 3f;
        float v = vMinImg + 0.5f + q.v[i] * 3f;
        return Math.round(u * TEXTURE_TO_BUFFER) | (Math.round(v * TEXTURE_TO_BUFFER) << 16);
    }
    
    private static float max(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }
    
    private static float sideInner(float self, float far, float near, float corner) {
        return max(self - .33f, far - 1.05f, near - .67f, corner - 1.2f);
    }
    
    private static float side(float self, float far, float near, float corner) {
        float s = sideInner(self, far, near, corner);
        float t = sideInner(self, far, near, corner);
        float u = sideInner(self, far, near, corner);
        float v = sideInner(self, far, near, corner);
        return mean (s, t, u, v);
    }
    
    
    private static float cornerInner(float near, float far, float a, float b) {
        return max(near - .47f, a - .75f, b - .75f, far - .94f);
    }
    
    private static float corner(float near, float far, float a, float b) {
        float s = cornerInner(near, far, a, b);
        float t = cornerInner(far, near, a, b);
        float u = cornerInner(a, b, near, far);
        float v = cornerInner(b, a, near, far);
        return mean (s, t, u, v);
    }
    
    private static float mean(float a, float b, float c, float d) {
        return (a + b + c + d) * 0.25f;
    }
}
