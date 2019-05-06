//package grondag.canvas.apiimpl.util;
//
//import static grondag.canvas.apiimpl.util.AoFaceData.meanBrightness;
//
//import java.util.concurrent.ConcurrentHashMap;
//
//import grondag.canvas.varia.LightmapHD;
//import grondag.fermion.varia.Useful;
//
////TODO: remove
//public class LightFaceData {
//    // interpolated corner results
//    public int b0;
//    public int b1;
//    public int b2;
//    public int b3;
//    
//    public int bs0;
//    public int bs1;
//    public int bs2;
//    public int bs3;
//    public int bc0;
//    public int bc1;
//    public int bc2;
//    public int bc3;
//    public int bCenter;
//    
//    
//    public float bs0() {return bs0 / 17f; }
//    public float bs1() {return bs1 / 17f; }
//    public float bs2() {return bs2 / 17f; }
//    public float bs3() {return bs3 / 17f; }
//    public float bc0() {return bc0 / 17f; }
//    public float bc1() {return bc1 / 17f; }
//    public float bc2() {return bc2 / 17f; }
//    public float bc3() {return bc3 / 17f; }
//    public float bCenter() {return bCenter / 17f; }
//    
//    public LightmapHD lightmap = null;
//    
//    @Override
//    public LightFaceData clone() {
//        LightFaceData result = new LightFaceData();
//        result.bs0 = this.bs0;
//        result.bs1 = this.bs1;
//        result.bs2 = this.bs2;
//        result.bs3 = this.bs3;
//        result.bc0 = this.bc0;
//        result.bc1 = this.bc1;
//        result.bc2 = this.bc2;
//        result.bc3 = this.bc3;
//        result.bCenter = this.bCenter;
//        
//        result.b0 = this.b0;
//        result.b1 = this.b1;
//        result.b2 = this.b2;
//        result.b3 = this.b3;
//        
//        return result;
//    }
//    
//    @Override
//    public int hashCode() {
//        final long h0 = bs0 | (bs1 << 4) | (bs2 << 8) | (bs3 << 12) | (bCenter << 16)
//                | (bc0 << 20) | (bc1 << 24) | (bc2 << 28) | ((long)bc3 << 32);
//        
//        return Long.hashCode(h0);
//    }
//    
//    @Override
//    public boolean equals(Object o) {
//        if(!(o instanceof LightFaceData)) {
//            return false;
//        }
//        LightFaceData d = (LightFaceData)o;
//        return this.bs0 == d.bs0
//                && this.bs0 == d.bs0
//                && this.bs1 == d.bs1
//                && this.bs2 == d.bs2
//                && this.bs3 == d.bs3
//                && this.bCenter == d.bCenter
//                && this.bc0 == d.bc0
//                && this.bc1 == d.bc1
//                && this.bc2 == d.bc2
//                && this.bc3 == d.bc3;
//                
//    }
//    
//    LightFaceData compute() {
//        b0 = meanBrightness(bs3, bs0, bc1, bCenter);
//        b1 = meanBrightness(bs2, bs0, bc0, bCenter);
//        b2 = meanBrightness(bs2, bs1, bc2, bCenter);
//        b3 = meanBrightness(bs3, bs1, bc3, bCenter);
//        
//        return this;
//    }
//    
//    LightFaceData upload() {
////        this.lightmap = new LightmapHD(this);
//        return this;
//    }
//
//    public int weigtedBlockLight(float[] w) {
//        return (int) (b0 * w[0] + b1 * w[1] + b2 * w[2] + b3 * w[3]) & 0xFF;
//    }
//
//    static final ThreadLocal<LightFaceData> TEMPLATES = ThreadLocal.withInitial(LightFaceData::new);
//    
//    static LightFaceData weightedMean(LightFaceData in0, float w0, LightFaceData in1, float w1) {
//        LightFaceData out = TEMPLATES.get();
//
//        out.bc0 = (int) (in0.bc0 * w0 + in1.bc0 * w1);
//        out.bc1 = (int) (in0.bc1 * w0 + in1.bc1 * w1);
//        out.bc2 = (int) (in0.bc2 * w0 + in1.bc2 * w1);
//        out.bc3 = (int) (in0.bc3 * w0 + in1.bc3 * w1);
//        
//        out.bCenter = (int) (in0.bCenter * w0 + in1.bCenter * w1);
//        
//        out.bs0 = (int) (in0.bs0 * w0 + in1.bs0 * w1);
//        out.bs1 = (int) (in0.bs1 * w0 + in1.bs1 * w1);
//        out.bs2 = (int) (in0.bs2 * w0 + in1.bs2 * w1);
//        out.bs3 = (int) (in0.bs3 * w0 + in1.bs3 * w1);
//        
//        out.b0 = (int) (in0.b0 * w0 + in1.b0 * w1);
//        out.b1 = (int) (in0.b1 * w0 + in1.b1 * w1);
//        out.b2 = (int) (in0.b2 * w0 + in1.b2 * w1);
//        out.b3 = (int) (in0.b3 * w0 + in1.b3 * w1);
//
//        return out;
//    }
//
//    public static void forceReload() {
//        MAP.clear();
//    }
//    
//    static final ConcurrentHashMap<LightFaceData, LightFaceData> MAP = new ConcurrentHashMap<>();
//    
//    public static LightFaceData intern(LightFaceData searchFace) {
//        searchFace.compute();
//        return MAP.computeIfAbsent(searchFace, s -> s.clone().upload());
//    }
//    
//    public static LightFaceData find(AoFaceData faceData) {
//        LightFaceData search = TEMPLATES.get();
//        
//        search.bs0 = faceData.light0 & 0xFFFF;
//        search.bs1 = faceData.light1 & 0xFFFF;
//        search.bs2 = faceData.light2 & 0xFFFF;
//        search.bs3 = faceData.light3 & 0xFFFF;
//        
//        search.bc0 = faceData.cLight0 & 0xFFFF;
//        search.bc1 = faceData.cLight1 & 0xFFFF;
//        search.bc2 = faceData.cLight2 & 0xFFFF;
//        search.bc3 = faceData.cLight3 & 0xFFFF;
//        
//        search.bCenter = faceData.lightCenter & 0xFFFF;
//        
//        search.b0 = faceData.b0;
//        search.b1 = faceData.b1;
//        search.b2 = faceData.b2;
//        search.b3 = faceData.b3;
//        
//        LightFaceData result = intern(search);
//        
////        if(result.b0 == faceData.b0 && result.b1 == faceData.b1 && result.b2 == faceData.b2 && result.b3 == faceData.b3) {
//////            if(result.b0 != 0 || result.b1 != 0 || result.b2 != 0 || result.b3 != 0) {
//////                Canvas.LOG.info(String.format("MATCH %d, %d, %d, %d", faceData.b0, faceData.b1, faceData.b2, faceData.b3));
//////            }
////        } else {
////            Canvas.LOG.info(String.format("OLD %d, %d, %d, %d", faceData.b0, faceData.b1, faceData.b2, faceData.b3));
////            Canvas.LOG.info(String.format("NEW %d, %d, %d, %d", result.b0, result.b1, result.b2, result.b3));
////        }
//        return result;
//    }
//}
