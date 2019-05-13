package grondag.canvas.light;

final class LightmapCornerHelper {
    private static final ThreadLocal<LightmapCornerHelper> THREADLOCAL = ThreadLocal.withInitial(LightmapCornerHelper::new);
    
    public static LightmapCornerHelper prepareThreadlocal(float u, float v, float x) {
        return THREADLOCAL.get().prepare(u, v, x);
    }
    
    private float a = 0;
    private float b = 0;
    private float c = 0;
    private float f = 0;
    private float h = 0;
    private float i = 0;
    private float k = 0;
    private float l = 0;
    private float m = 0;
    private float o = 0;
    
    private float x;
    
    /**
     * 
     * Computations assumes the following layout/naming conventions
     * 
     * u               x
     *   A   B   C   D
     *    
     *   E   F   G   H             where I, J, K are obscured pixels in the corner
     *   ------|                   and u,x,v are brightness values
     *   I   J | K   L             and other capital letters are pixels/results
     *         |                   to be computed
     *       M | N   O
     *                 v
     *                 
     *  A = .25x + .75u         B = .5x + .5u       C = .75x + .25u
     *  D = x
     *  H = .75x + .25v         L = .5x _ .5v       O = .25x + .75v
     *  I = A - .3215   M = O - .3215
     *  F = .5u + .25x + .25v - .15625     K = .5v + .25x + .25u - .15625
     *  J = mean(IFKM)  E = mean(AFI)   N = mean(MKO)  G = mean(FCHK)
     *  
     *  The above started out as barycentric interpolation but was then hand-tweaked to look good.
     *  There's no other logic to it.
     */
    private LightmapCornerHelper prepare(float u, float v, float x) {
        this.x = x;
        
        // PERF: could make this lazier
        
        a = 0.25f * x + 0.75f * u;
        b = 0.5f * x + 0.5f * u;
        c = 0.75f * x + 0.25f * u;
        h = 0.75f * x + 0.25f * v;
        l = 0.5f * x + 0.5f * v;
        o = 0.25f * x + 0.75f * v;
        i = a - 0.3215f;
        m = o - 0.3215f;
        f = 0.5f * u + 0.25f * x + 0.25f * v;
        k = 0.5f * v + 0.25f * x + 0.25f * u;
        return this;
    }
    
    public float a() { return a; } 
    public float b() { return b; } 
    public float c() { return c; } 
    public float d() { return x; } 
    public float e() { return (a + f + i) * 0.3333333f; }
    public float f() { return f; } 
    public float g() { return (f + c + h + k) * 0.25f; }
    public float h() { return h; } 
    public float i() { return i; } 
    public float j() { return (i + k + f + m) * 0.25f; } 
    public float k() { return k; } 
    public float l() { return l; } 
    public float m() { return m; } 
    public float n() { return (m + k + o) * 0.3333333f; }
    public float o() { return o; } 
}
