package grondag.canvas.varia;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import grondag.canvas.apiimpl.util.LightFaceData;

public class Lightmap3 {
    static final int TEX_SIZE = 512;
    static final int IDX_SIZE = 512 / 3;
    static final int MAX_COUNT = IDX_SIZE * IDX_SIZE;
    // UGLY - consider making this a full unsigned short
    // for initial pass didn't want to worry about signed value mistakes
    /** Scale of texture units sent to shader. Shader should divide by this after adding 0.5 */
    static final int BUFFER_SCALE = 0x8000;
    static final int UNITS_PER_PIXEL = BUFFER_SCALE / TEX_SIZE;
    static final float TEXTURE_TO_BUFFER = (float) BUFFER_SCALE / TEX_SIZE;
    
    private static final Lightmap3[] maps = new Lightmap3[IDX_SIZE * IDX_SIZE];
    
    private static final AtomicInteger nextIndex = new AtomicInteger();
    
    public static void forceReload() {
        nextIndex.set(0);
    }
    
    public static void forEach(Consumer<Lightmap3> consumer) {
        final int limit = Math.min(MAX_COUNT, nextIndex.get());
        for(int i = 0; i < limit; i++) {
            consumer.accept(maps[i]);
        }
    }
    
    public final int uMinImg;
    public final int vMinImg;
    
    // PERF: too heavy
    public final int[][] sky = new int[3][3];
    public final int[][] block = new int[3][3];
    final float u0, u1, u2, u3, v0, v1, v2, v3;

    public Lightmap3(LightFaceData data) {
        final int index = nextIndex.getAndIncrement();
        final int s = index % IDX_SIZE;
        final int t = index / IDX_SIZE;
        uMinImg = s * 3;
        vMinImg = t * 3;
        
        u0 = (uMinImg + 1f);
        v0 = (vMinImg + 1f);
        u1 = (uMinImg + 2f);
        v1 = (vMinImg + 1f);
        u2 = (uMinImg + 2f);
        v2 = (vMinImg + 2f);
        u3 = (uMinImg + 1f);
        v3 = (vMinImg + 2f);
        
        if(index >= MAX_COUNT) {
            assert false : "Out of lightmap space.";
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
        sky[1][1] = data.kCenter;
        sky[0][0] = data.kc1;
        sky[1][0] = data.ks0;
        sky[2][0] = data.kc0;
        sky[2][1] = data.ks2;
        sky[2][2] = data.kc2;
        sky[1][2] = data.ks1;
        sky[0][2] = data.kc3;
        sky[0][1] = data.ks3;
        
        block[1][1] = data.bCenter;
        block[0][0] = data.bc1;
        block[1][0] = data.bs0;
        block[2][0] = data.bc0;
        block[2][1] = data.bs2;
        block[2][2] = data.bc2;
        block[1][2] = data.bs1;
        block[0][2] = data.bc3;
        block[0][1] = data.bs3;
    }
    
    
    public final int coord(float[] w) {
        float u = u0 * w[0] + u1 * w[1] + u2 * w[2] + u3 * w[3];
        float v = v0 * w[0] + v1 * w[1] + v2 * w[2] + v3 * w[3];
        return Math.round(u * TEXTURE_TO_BUFFER) | (Math.round(v * TEXTURE_TO_BUFFER) << 16);
    }
}
