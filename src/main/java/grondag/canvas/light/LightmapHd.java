package grondag.canvas.light;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToLongFunction;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.math.MathHelper;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.mesh.QuadViewImpl;

public class LightmapHd {
	private static boolean errorNoticeNeeded = true;

	private static final AtomicInteger nextIndex = new AtomicInteger();

	public static String occupancyReport() {
		final int i = nextIndex.get();
		return String.format("%d of %d ( %d percent )", i, LightmapSizer.maxCount, i * 100 / LightmapSizer.maxCount);
	}

	public static void forceReload() {
		nextIndex.set(0);
		MAP.clear();
		errorNoticeNeeded = true;
	}

	// PERF: use Fermion cache
	static final Long2ObjectOpenHashMap<LightmapHd> MAP = new Long2ObjectOpenHashMap<>(MathHelper.smallestEncompassingPowerOfTwo(LightmapSizer.maxCount), LightmapSizer.maxCount / (float)MathHelper.smallestEncompassingPowerOfTwo(LightmapSizer.maxCount));

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
		return v * LightmapSizer.paddedSize + u;
	}

	// PERF: can reduce texture consumption 8X by reusing rotations/inversions
	private static LightmapHd find(AoFaceData faceData, ToLongFunction<AoFaceData> mapper) {
		final long key = mapper.applyAsLong(faceData);

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
		final int s = index % LightmapSizer.mapsPerAxis;
		final int t = index / LightmapSizer.mapsPerAxis;
		uMinImg = s * LightmapSizer.paddedSize;
		vMinImg = t * LightmapSizer.paddedSize;
		// PERF: light data could be repooled once uploaded - not needed after
		// or simply output to the texture directly
		light = new int[LightmapSizer.lightmapPixels];
		isAo = LightKey.isAo(key);

		if(index >= LightmapSizer.maxCount) {
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
		return light[v * LightmapSizer.paddedSize + u];
	}

	public int coord(QuadViewImpl q, int i) {
		//TODO: restore
		//		final int u, v;
		//
		//		if(isAo) {
		//			u = Math.round((uMinImg + 0.5f  + q.u[i] * LightmapSizer.aoSize) * LightmapSizer.textureToBuffer);
		//			v = Math.round((vMinImg + 0.5f  + q.v[i] * LightmapSizer.aoSize) * LightmapSizer.textureToBuffer);
		//		} else {
		//			u = Math.round((uMinImg + 1  + q.u[i] * LightmapSizer.lightmapSize) * LightmapSizer.textureToBuffer);
		//			v = Math.round((vMinImg + 1  + q.v[i] * LightmapSizer.lightmapSize) * LightmapSizer.textureToBuffer);
		//		}
		//
		//		return u | (v << 16);
		return 0;
	}
}
