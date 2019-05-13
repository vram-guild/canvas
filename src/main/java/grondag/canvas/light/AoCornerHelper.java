package grondag.canvas.light;

final class AoCornerHelper {
    private static final ThreadLocal<AoCornerHelper> THREADLOCAL = ThreadLocal.withInitial(AoCornerHelper::new);

    public static AoCornerHelper prepareThreadlocal(float u, float v, float x) {
        return THREADLOCAL.get().prepare(u, v, x);
    }

    private float a = 0;
    private float b = 0;
    private float c = 0;
    private float e = 0;
    private float f = 0;
    private float g = 0;
    private float h = 0;
    private float i = 0;
    private float j = 0;
    private float k = 0;
    private float l = 0;
    private float m = 0;
    private float n = 0;
    private float o = 0;

    private float x;

    /**
     * 
     * Computations assume the following layout/naming conventions.  Similar to Lightmap version.
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
     */
    private AoCornerHelper prepare(float u, float v, float x) {
        this.x = x;
        // PERF: could make this lazier
        final float COVERED = AoMapHd.OPAQUE_PROXY * 2.5f / 4f + 1.5f / 4f;

        final float gRaw = 0.25f * u + 0.25f * v + 0.5f * x;
        g = 0.9f * gRaw + 0.1f * AoMapHd.OPAQUE_PROXY;

        a = 0.65625f * u + 0.21875f * x + 0.125f * AoMapHd.OPAQUE_PROXY;
        final float bRaw = (u + x) * 0.5f;
        b = 0.885f * bRaw + 0.115f * AoMapHd.OPAQUE_PROXY;
        final float cRaw = 0.25f * u + 0.75f * x;
        c = 0.9f * cRaw + 0.1f * AoMapHd.OPAQUE_PROXY;

        o = 0.65625f * v + 0.21875f * x + 0.125f * AoMapHd.OPAQUE_PROXY;
        final float lRaw = (v + x) * 0.5f;
        l = 0.885f * lRaw + 0.115f * AoMapHd.OPAQUE_PROXY;
        final float hRaw = 0.25f * v + 0.75f * x;
        h = 0.9f * hRaw + 0.1f * AoMapHd.OPAQUE_PROXY;

        e = 0.625f * u + 0.375f * AoMapHd.OPAQUE_PROXY;
        f = 0.7f * bRaw + 0.300f * AoMapHd.OPAQUE_PROXY;

        k = 0.7f * lRaw + 0.300f * AoMapHd.OPAQUE_PROXY;
        n = 0.625f * v + 0.375f * AoMapHd.OPAQUE_PROXY;

        m = COVERED;//0.625f * v + 0.375f * AoMapHd.OPAQUE_PROXY;
        i = COVERED;//0.625f * u + 0.375f * AoMapHd.OPAQUE_PROXY;
        j = COVERED;//0.625f * x + 0.375f * AoMapHd.OPAQUE_PROXY;

        return this;
    }

    public float a() { return a; } 
    public float b() { return b; } 
    public float c() { return c; } 
    public float d() { return x; } 
    public float e() { return e; }
    public float f() { return f; } 
    public float g() { return g; }
    public float h() { return h; } 
    public float i() { return i; } 
    public float j() { return j; } 
    public float k() { return k; } 
    public float l() { return l; } 
    public float m() { return m; } 
    public float n() { return n; }
    public float o() { return o; } 
}
