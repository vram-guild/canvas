package grondag.canvas.light;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToLongFunction;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.QuadViewImpl;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.math.MathHelper;

public class LightmapHd {
    static final int TEX_SIZE = 4096;
    static final int LIGHTMAP_SIZE = 4;
    static final int AO_SIZE = LIGHTMAP_SIZE + 1;
    public static final int PADDED_SIZE = LIGHTMAP_SIZE + 2;
    static final int RADIUS = LIGHTMAP_SIZE / 2;
    static final int LIGHTMAP_PIXELS = PADDED_SIZE * PADDED_SIZE;
    static final int MAPS_PER_AXIS = TEX_SIZE / PADDED_SIZE;
    static final int MAX_COUNT = MAPS_PER_AXIS * MAPS_PER_AXIS;
    // UGLY - consider making this a full unsigned short
    // for initial pass didn't want to worry about signed value mistakes
    /** Scale of texture units sent to shader. Shader should divide by this. */
    static final int BUFFER_SCALE = 0x8000;
    static final float TEXTURE_TO_BUFFER = (float) BUFFER_SCALE / TEX_SIZE;
    
    
    /** converts zero-based distance from center to u/v index - use for top/left */
    static final Int2IntFunction NEG = i -> RADIUS - i;
    /** converts zero-based distance from center to u/v index - use for bottom/right */
    static final Int2IntFunction POS = i -> RADIUS + 1 + i;
    
    private static boolean errorNoticeNeeded = true;
    
    private static final AtomicInteger nextIndex = new AtomicInteger();
    
    public static void forceReload() {
        nextIndex.set(0);
        MAP.clear();
        errorNoticeNeeded = true;
    }
    
    // PERF: use Fermion cache
    static final Long2ObjectOpenHashMap<LightmapHd> MAP = new Long2ObjectOpenHashMap<>(MathHelper.smallestEncompassingPowerOfTwo(MAX_COUNT), MAX_COUNT / (float)MathHelper.smallestEncompassingPowerOfTwo(MAX_COUNT));
    
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
        return LightKey.toLightmapKey(
            faceData.top & 0xFF,
            faceData.left & 0xFF,
            faceData.right & 0xFF,
            faceData.bottom & 0xFF,
            faceData.topLeft & 0xFF,
            faceData.topRight & 0xFF,
            faceData.bottomLeft & 0xFF,
            faceData.bottomRight & 0xFF,
            faceData.center & 0xFF        
        );
    }
    
    private static long mapSky(AoFaceData faceData) {
        return LightKey.toLightmapKey(
            (faceData.top >>> 16) & 0xFF,
            (faceData.left >>> 16) & 0xFF,
            (faceData.right >>> 16) & 0xFF,
            (faceData.bottom >>> 16) & 0xFF,
            (faceData.topLeft >>> 16) & 0xFF,
            (faceData.topRight >>> 16) & 0xFF,
            (faceData.bottomLeft >>> 16) & 0xFF,
            (faceData.bottomRight >>> 16) & 0xFF,
            (faceData.center >>> 16) & 0xFF
        );
    }
    
    private static long mapAo(AoFaceData faceData) {
        return LightKey.toAoKey(
            faceData.aoTopLeft,
            faceData.aoTopRight,
            faceData.aoBottomLeft,
            faceData.aoBottomRight
        );
    }
    
    static int lightIndex(int u, int v) {
        return v * PADDED_SIZE + u;
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
    
    public final int uMinImg;
    public final int vMinImg;
    private final int[] light;
    public final boolean isAo;
    
    private LightmapHd(long key) {
        final int index = nextIndex.getAndIncrement();
        final int s = index % MAPS_PER_AXIS;
        final int t = index / MAPS_PER_AXIS;
        uMinImg = s * PADDED_SIZE;
        vMinImg = t * PADDED_SIZE;
        // PERF: light data could be repooled once uploaded - not needed after
        // or simply output to the texture directly
        this.light = new int[LIGHTMAP_PIXELS];
        isAo = LightKey.isAo(key);
        
        if(index >= MAX_COUNT) {
            if(errorNoticeNeeded) {
                CanvasMod.LOG.warn(I18n.translate("error.canvas.fail_create_lightmap"));
                errorNoticeNeeded = false;
            }
        } else {
            if(isAo) {
                AoMapHd.computeAo(light, key, index);
            } else {
                LightmapHdCalc.computeLight(light, key, index);
            }
            
            LightmapHdTexture.instance().enque(this);
        }
    }
    
    /**
     * Handles padding
     */
    public int pixel(int u, int v) {
        return light[v * PADDED_SIZE + u];
    }
    
    public int coord(QuadViewImpl q, int i) {
        final int u, v;
        
        if(isAo) {
            u = Math.round((uMinImg + 0.5f  + q.u[i] * AO_SIZE ) * TEXTURE_TO_BUFFER);
            v = Math.round((vMinImg + 0.5f  + q.v[i] * AO_SIZE) * TEXTURE_TO_BUFFER);
        } else {
            u = Math.round((uMinImg + 1  + q.u[i] * LIGHTMAP_SIZE) * TEXTURE_TO_BUFFER);
            v = Math.round((vMinImg + 1  + q.v[i] * LIGHTMAP_SIZE) * TEXTURE_TO_BUFFER);
        }
        
        return u | (v << 16);
    }
}
