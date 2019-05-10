package grondag.canvas.apiimpl.util;

import java.util.concurrent.ConcurrentHashMap;

import grondag.fermion.varia.Useful;

public class ShadeFaceData {
    
    // interpolated corner results
    float a0;
    float a1;
    float a2;
    float a3;
    
    public float ao0;
    public float ao1;
    public float ao2;
    public float ao3;
    public float cAo0;
    public float cAo1;
    public float cAo2;
    public float cAo3;
    public float aoCenter;
    
    @Override
    public ShadeFaceData clone() {
        ShadeFaceData result = new ShadeFaceData();
        result.ao0 = this.ao0;
        result.ao1 = this.ao1;
        result.ao2 = this.ao2;
        result.ao3 = this.ao3;
        result.cAo0 = this.cAo0;
        result.cAo1 = this.cAo1;
        result.cAo2 = this.cAo2;
        result.cAo3 = this.cAo3;
        result.aoCenter = this.aoCenter;
        return result;
    }

    @Override
    public int hashCode() {
        long h = ((long)(ao0 * 0xFF)) | ((long)(ao1 * 0xFF) << 8) | ((long)(ao2 * 0xFF) << 16);
        h |= ((long)(ao3 * 0xFF) << 24) | ((long)(cAo0 * 0xFF) << 32) | ((long)(cAo1 * 0xFF) << 40);
        h |= ((long)(cAo2 * 0xFF) << 48) | ((long)(cAo3 * 0xFF) << 56);
        
        return Long.hashCode(Useful.longHash(h) ^ (long)(aoCenter * 0xFF));
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof ShadeFaceData)) {
            return false;
        }
        ShadeFaceData d = (ShadeFaceData)o;
        return this.ao0 == d.ao0
                && this.ao1 == d.ao1
                && this.ao2 == d.ao2
                && this.ao3 == d.ao3
                && this.cAo0 == d.cAo0
                && this.cAo1 == d.cAo1
                && this.cAo2 == d.cAo2
                && this.cAo3 == d.cAo3
                && this.aoCenter == d.aoCenter;
    }
    
    public void compute() {
        a0 = (ao3 + ao0 + cAo1 + aoCenter) * 0.25F;
        a1 = (ao2 + ao0 + cAo0 + aoCenter) * 0.25F;
        a2 = (ao2 + ao1 + cAo2 + aoCenter) * 0.25F;
        a3 = (ao3 + ao1 + cAo3 + aoCenter) * 0.25F;
    }

    float weigtedAo(float[] w) {
        return a0 * w[0] + a1 * w[1] + a2 * w[2] + a3 * w[3];
    }

    float maxAo(float oldMax) {
        final float x = a0 > a1 ? a0 : a1;
        final float y = a2 > a3 ? a2 : a3;
        final float z = x > y ? x : y;
        return oldMax > z ? oldMax : z;
    }

    static final ThreadLocal<ShadeFaceData> THREADLOCAL = ThreadLocal.withInitial(ShadeFaceData::new);
    
    static ShadeFaceData weightedMean(ShadeFaceData in0, float w0, ShadeFaceData in1, float w1) {
        ShadeFaceData out = THREADLOCAL.get();
        
        out.a0 = in0.a0 * w0 + in1.a0 * w1;
        out.a1 = in0.a1 * w0 + in1.a1 * w1;
        out.a2 = in0.a2 * w0 + in1.a2 * w1;
        out.a3 = in0.a3 * w0 + in1.a3 * w1;
        
        out.ao0 = in0.ao0 * w0 + in1.ao0 * w1;
        out.ao1 = in0.ao1 * w0 + in1.ao1 * w1;
        out.ao2 = in0.ao2 * w0 + in1.ao2 * w1;
        out.ao3 = in0.ao3 * w0 + in1.ao3 * w1;
        
        out.cAo0 = in0.cAo0 * w0 + in1.cAo0 * w1;
        out.cAo1 = in0.cAo1 * w0 + in1.cAo1 * w1;
        out.cAo2 = in0.cAo2 * w0 + in1.cAo2 * w1;
        out.cAo3 = in0.cAo3 * w0 + in1.cAo3 * w1;
        
        out.aoCenter = in0.aoCenter * w0 + in1.aoCenter * w1;
        
        return out;
    }
    
    public static void forceReload() {
        MAP.clear();
    }
    
    static final ConcurrentHashMap<ShadeFaceData, ShadeFaceData> MAP = new ConcurrentHashMap<>();

    public static ShadeFaceData intern(ShadeFaceData searchFace) {
        return MAP.computeIfAbsent(searchFace, s -> s.clone());
    }

    public static ShadeFaceData find(AoFaceData faceData) {
        ShadeFaceData search = THREADLOCAL.get();
        search.a0 = faceData.outAoBottomRight;
        search.a1 = faceData.outAoBottomLeft;
        search.a2 = faceData.outAoTopLeft;
        search.a3 = faceData.outAoTopRight;
        
        search.ao0 = faceData.aoBottom;
        search.ao1 = faceData.aoTop;
        search.ao2 = faceData.aoLeft;
        search.ao3 = faceData.aoRight;
        
        search.cAo0 = faceData.aoBottomLeft;
        search.cAo1 = faceData.aoBottomRight;
        search.cAo2 = faceData.aoTopLeft;
        search.cAo3 = faceData.aoTopRight;
        
        search.aoCenter = faceData.aoCenter;
        
        return intern(search);
    }
}
