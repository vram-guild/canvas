package grondag.canvas.apiimpl.util;

import static java.lang.Math.max;

import java.util.concurrent.ConcurrentHashMap;

import grondag.canvas.varia.Lightmap3;
import grondag.fermion.varia.Useful;

public class LightFaceData {
    // interpolated corner results
    public int b0;
    public int b1;
    public int b2;
    public int b3;
    public int s0;
    public int s1;
    public int s2;
    public int s3;
    
    public int bs0;
    public int bs1;
    public int bs2;
    public int bs3;
    public int bc0;
    public int bc1;
    public int bc2;
    public int bc3;
    public int bCenter;
    
    public int ks0;
    public int ks1;
    public int ks2;
    public int ks3;
    public int kc0;
    public int kc1;
    public int kc2;
    public int kc3;
    public int kCenter;
    
    public Lightmap3 lightmap = null;
    
    @Override
    public LightFaceData clone() {
        LightFaceData result = new LightFaceData();
        result.bs0 = this.bs0;
        result.bs1 = this.bs1;
        result.bs2 = this.bs2;
        result.bs3 = this.bs3;
        result.bc0 = this.bc0;
        result.bc1 = this.bc1;
        result.bc2 = this.bc2;
        result.bc3 = this.bc3;
        result.bCenter = this.bCenter;
        
        result.ks0 = this.ks0;
        result.ks1 = this.ks1;
        result.ks2 = this.ks2;
        result.ks3 = this.ks3;
        result.kc0 = this.kc0;
        result.kc1 = this.kc1;
        result.kc2 = this.kc2;
        result.kc3 = this.kc3;
        result.kCenter = this.kCenter;
        
        result.b0 = this.b0;
        result.b1 = this.b1;
        result.b2 = this.b2;
        result.b3 = this.b3;
        result.s0 = this.s0;
        result.s1 = this.s1;
        result.s2 = this.s2;
        result.s3 = this.s3;
        
        return result;
    }
    
    @Override
    public int hashCode() {
        long h0 = bs0 | (bs1 << 4) | (bs2 << 8) | (bs3 << 12) | (bCenter << 16);
        h0 |= (bc0 << 20) | (bc1 << 24) | (bc2 << 28) | ((long)bc3 << 32);
        
        long h1 = ks0 | (ks1 << 4) | (ks2 << 8) | (ks3 << 12) | (kCenter << 16);
        h0 |= (kc0 << 20) | (kc1 << 24) | (kc2 << 28) | ((long)kc3 << 32);
        return Long.hashCode(Useful.longHash(h0) ^ Useful.longHash(h1));
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof LightFaceData)) {
            return false;
        }
        LightFaceData d = (LightFaceData)o;
        return this.bs0 == d.bs0
                && this.bs0 == d.bs0
                && this.bs1 == d.bs1
                && this.bs2 == d.bs2
                && this.bs3 == d.bs3
                && this.bCenter == d.bCenter
                && this.bc0 == d.bc0
                && this.bc1 == d.bc1
                && this.bc2 == d.bc2
                && this.bc3 == d.bc3
                
                && this.ks0 == d.ks0
                && this.ks0 == d.ks0
                && this.ks1 == d.ks1
                && this.ks2 == d.ks2
                && this.ks3 == d.ks3
                && this.kCenter == d.kCenter
                && this.kc0 == d.kc0
                && this.kc1 == d.kc1
                && this.kc2 == d.kc2
                && this.kc3 == d.kc3;
    }
    
    void simplify() {
        int nbs0 = Math.max(bs0, nonZeroMin(bs0, bCenter, bc0, bc1));
        int nbs1 = Math.max(bs1, nonZeroMin(bs1, bCenter, bc2, bc3));
        int nbs2 = Math.max(bs2, nonZeroMin(bs2, bCenter, bc0, bc2));
        int nbs3 = Math.max(bs3, nonZeroMin(bs3, bCenter, bc1, bc3));
        
        // note the grouping here is different than in computing
        // because computing maps to vertex order. Here we just want 
        // to eliminate too-dark samples
        int nbc0 = Math.max(bc0, nonZeroMin(bs2, bs0, bc0, bCenter));
        int nbc1 = Math.max(bc1, nonZeroMin(bs3, bs0, bc1, bCenter));
        int nbc2 = Math.max(bc2, nonZeroMin(bs2, bs1, bc2, bCenter));
        int nbc3 = Math.max(bc3, nonZeroMin(bs3, bs1, bc3, bCenter));
        
        int nks0 = Math.max(ks0, nonZeroMin(ks0, kCenter, kc0, kc1));
        int nks1 = Math.max(ks1, nonZeroMin(ks1, kCenter, kc2, kc3));
        int nks2 = Math.max(ks2, nonZeroMin(ks2, kCenter, kc0, kc2));
        int nks3 = Math.max(ks3, nonZeroMin(ks3, kCenter, kc1, kc3));
        
        // note the grouping here is different than in computing
        // because computing maps to vertex order. Here we just want 
        // to eliminate too-dark samples
        int nkc0 = Math.max(kc0, nonZeroMin(ks2, ks0, kc0, kCenter));
        int nkc1 = Math.max(kc1, nonZeroMin(ks3, ks0, kc1, kCenter));
        int nkc2 = Math.max(kc2, nonZeroMin(ks2, ks1, kc2, kCenter));
        int nkc3 = Math.max(kc3, nonZeroMin(ks3, ks1, kc3, kCenter));
        
        bs0 = nbs0;
        bs1 = nbs1;
        bs2 = nbs2;
        bs3 = nbs3;
        bc0 = nbc0;
        bc1 = nbc1;
        bc2 = nbc2;
        bc3 = nbc3;
        
        ks0 = nks0;
        ks1 = nks1;
        ks2 = nks2;
        ks3 = nks3;
        kc0 = nkc0;
        kc1 = nkc1;
        kc2 = nkc2;
        kc3 = nkc3;
    }
    
    LightFaceData compute() {
        b0 = meanBrightness(bs3, bs0, bc1, bCenter);
        b1 = meanBrightness(bs2, bs0, bc0, bCenter);
        b2 = meanBrightness(bs2, bs1, bc2, bCenter);
        b3 = meanBrightness(bs3, bs1, bc3, bCenter);
        
        s0 = meanBrightness(ks3, ks0, kc1, kCenter);
        s1 = meanBrightness(ks2, ks0, kc0, kCenter);
        s2 = meanBrightness(ks2, ks1, kc2, kCenter);
        s3 = meanBrightness(ks3, ks1, kc3, kCenter);
        
        return this;
    }
    
    LightFaceData upload() {
        this.lightmap = new Lightmap3(this);
        return this;
    }

    public int weigtedBlockLight(float[] w) {
        return (int) (b0 * w[0] + b1 * w[1] + b2 * w[2] + b3 * w[3]) & 0xFF;
    }

    public int weigtedSkyLight(float[] w) {
        return (int) (s0 * w[0] + s1 * w[1] + s2 * w[2] + s3 * w[3]) & 0xFF;
    }

    int weightedCombinedLight(float[] w) {
        return weigtedSkyLight(w) << 16 | weigtedBlockLight(w);
    }

    static final ThreadLocal<LightFaceData> TEMPLATES = ThreadLocal.withInitial(LightFaceData::new);
    
    static LightFaceData weightedMean(LightFaceData in0, float w0, LightFaceData in1, float w1) {
        LightFaceData out = TEMPLATES.get();

        out.ks0 = (int) (in0.ks0 * w0 + in1.ks0 * w1);
        out.ks1 = (int) (in0.ks1 * w0 + in1.ks1 * w1);
        out.ks2 = (int) (in0.ks2 * w0 + in1.ks2 * w1);
        out.ks3 = (int) (in0.ks3 * w0 + in1.ks3 * w1);
        
        out.bc0 = (int) (in0.bc0 * w0 + in1.bc0 * w1);
        out.bc1 = (int) (in0.bc1 * w0 + in1.bc1 * w1);
        out.bc2 = (int) (in0.bc2 * w0 + in1.bc2 * w1);
        out.bc3 = (int) (in0.bc3 * w0 + in1.bc3 * w1);
        
        out.bCenter = (int) (in0.bCenter * w0 + in1.bCenter * w1);
        
        out.bs0 = (int) (in0.bs0 * w0 + in1.bs0 * w1);
        out.bs1 = (int) (in0.bs1 * w0 + in1.bs1 * w1);
        out.bs2 = (int) (in0.bs2 * w0 + in1.bs2 * w1);
        out.bs3 = (int) (in0.bs3 * w0 + in1.bs3 * w1);
        
        out.kc0 = (int) (in0.kc0 * w0 + in1.kc0 * w1);
        out.kc1 = (int) (in0.kc1 * w0 + in1.kc1 * w1);
        out.kc2 = (int) (in0.kc2 * w0 + in1.kc2 * w1);
        out.kc3 = (int) (in0.kc3 * w0 + in1.kc3 * w1);
        
        out.kCenter = (int) (in0.kCenter * w0 + in1.kCenter * w1);
        
        out.b0 = (int) (in0.b0 * w0 + in1.b0 * w1);
        out.b1 = (int) (in0.b1 * w0 + in1.b1 * w1);
        out.b2 = (int) (in0.b2 * w0 + in1.b2 * w1);
        out.b3 = (int) (in0.b3 * w0 + in1.b3 * w1);

        out.s0 = (int) (in0.s0 * w0 + in1.s0 * w1);
        out.s1 = (int) (in0.s1 * w0 + in1.s1 * w1);
        out.s2 = (int) (in0.s2 * w0 + in1.s2 * w1);
        out.s3 = (int) (in0.s3 * w0 + in1.s3 * w1);

        return out;
    }

    public static void forceReload() {
        MAP.clear();
    }
    
    static final ConcurrentHashMap<LightFaceData, LightFaceData> MAP = new ConcurrentHashMap<>();
    
    public static LightFaceData intern(LightFaceData searchFace) {
//        searchFace.simplify();
        return MAP.computeIfAbsent(searchFace, s -> s.clone().compute().upload());
    }
    
    public static LightFaceData find(AoFaceData faceData) {
        LightFaceData search = TEMPLATES.get();
        
        search.bs0 = faceData.light0 & 0xFFFF;
        search.bs1 = faceData.light1 & 0xFFFF;
        search.bs2 = faceData.light2 & 0xFFFF;
        search.bs3 = faceData.light3 & 0xFFFF;
        
        search.bc0 = faceData.cLight0 & 0xFFFF;
        search.bc1 = faceData.cLight1 & 0xFFFF;
        search.bc2 = faceData.cLight2 & 0xFFFF;
        search.bc3 = faceData.cLight3 & 0xFFFF;
        
        search.bCenter = faceData.lightCenter & 0xFFFF;
        
        search.ks0 = (faceData.light0 >>> 16) & 0xFFFF;
        search.ks1 = (faceData.light1 >>> 16) & 0xFFFF;
        search.ks2 = (faceData.light2 >>> 16) & 0xFFFF;
        search.ks3 = (faceData.light3 >>> 16) & 0xFFFF;
        
        search.kc0 = (faceData.cLight0 >>> 16) & 0xFFFF;
        search.kc1 = (faceData.cLight1 >>> 16) & 0xFFFF;
        search.kc2 = (faceData.cLight2 >>> 16) & 0xFFFF;
        search.kc3 = (faceData.cLight3 >>> 16) & 0xFFFF;
        
        search.kCenter = (faceData.lightCenter >>> 16) & 0xFFFF;
        
        search.b0 = faceData.b0;
        search.b1 = faceData.b1;
        search.b2 = faceData.b2;
        search.b3 = faceData.b3;
        search.s0 = faceData.s0;
        search.s1 = faceData.s1;
        search.s2 = faceData.s2;
        search.s3 = faceData.s3;
        
        return intern(search);
    }
    
    /** 
     * Vanilla code excluded missing light values from mean but was not isotropic.
     * Still need to substitute or edges are too dark but consistently use the min 
     * value from all four samples.
     */
    private static int meanBrightness(int a, int b, int c, int d) {
        return a == 0 || b == 0 || c == 0 || d == 0 ? meanEdgeBrightness(a, b, c, d) : meanInnerBrightness(a, b, c, d);
    }
    
    private static int meanEdgeBrightness(int a, int b, int c, int d) {
        final int min = nonZeroMin(nonZeroMin(a, b), nonZeroMin(c, d));
        return meanInnerBrightness(max(a, min), max(b, min), max(c, min), max(d, min));
    }
    
    private static int meanInnerBrightness(int a, int b, int c, int d) {
        // bitwise divide by 4, clamp to expected (positive) range
        return a + b + c + d >> 2 & 16711935;
    }

    private static int nonZeroMin(int a, int b) {
        if(a == 0) return b;
        if(b == 0) return a;
        return Math.min(a, b);
    }
    
    private static int nonZeroMin(int a, int b, int c, int d) {
        return nonZeroMin(nonZeroMin(a, b), nonZeroMin(c, d));
    }
}
