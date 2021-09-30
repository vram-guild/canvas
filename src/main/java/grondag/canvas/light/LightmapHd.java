/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.light;

import java.util.concurrent.atomic.AtomicInteger;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.mesh.QuadEditorImpl;

class LightmapHd {
	// MAYBE: use Fermion cache
	static final Object2ObjectOpenHashMap<AoFaceData, LightmapHd> MAP = new Object2ObjectOpenHashMap<>(Mth.smallestEncompassingPowerOfTwo(LightmapSizer.maxCount), LightmapSizer.maxCount / (float) Mth.smallestEncompassingPowerOfTwo(LightmapSizer.maxCount));
	private static final AtomicInteger nextIndex = new AtomicInteger();
	private static boolean errorNoticeNeeded = true;
	public final int uMinImg;
	public final int vMinImg;
	private final int[] light;

	private LightmapHd(AoFaceData faceData) {
		final int index = nextIndex.getAndIncrement();
		final int s = index % LightmapSizer.mapsPerAxis;
		final int t = index / LightmapSizer.mapsPerAxis;
		uMinImg = s * LightmapSizer.paddedSize;
		vMinImg = t * LightmapSizer.paddedSize;
		light = new int[LightmapSizer.lightmapPixels];

		if (index >= LightmapSizer.maxCount) {
			if (errorNoticeNeeded) {
				CanvasMod.LOG.warn(I18n.get("error.canvas.fail_create_lightmap"));
				errorNoticeNeeded = false;
			}
		} else {
			// MAYBE: pool these and the main array - not needed after upload

			final int[] aoLight = new int[LightmapSizer.lightmapPixels];
			final int[] skyLight = new int[LightmapSizer.lightmapPixels];
			final int[] blockLight = new int[LightmapSizer.lightmapPixels];

			// MAYBE: make this an option for AO debugging
			//			Arrays.fill(skyLight, 255);
			//			Arrays.fill(blockLight, 255);

			// MAYBE: skips steps when all unit value or same  value
			LightmapHdCalc.computeAo(aoLight, faceData);
			LightmapHdCalc.computeLight(blockLight, faceData, false);
			LightmapHdCalc.computeLight(skyLight, faceData, true);

			for (int i = 0; i < LightmapSizer.lightmapPixels; ++i) {
				final int ao = aoLight[i];
				final int sky = skyLight[i]; // * ao / 255;
				final int block = blockLight[i]; // * ao / 255;
				light[i] = (sky << 24) | (ao << 16) | (block << 8) | ao;
			}

			LightmapHdTexture.instance().enque(this);
		}
	}

	public static String occupancyReport() {
		final int i = nextIndex.get();
		return String.format("%d of %d ( %d percent )", i, LightmapSizer.maxCount, i * 100 / LightmapSizer.maxCount);
	}

	public static void reload() {
		nextIndex.set(0);
		MAP.clear();
		errorNoticeNeeded = true;
	}

	static int lightIndex(int u, int v) {
		return v * LightmapSizer.paddedSize + u;
	}

	// MAYBE: can reduce texture consumption 8X by reusing rotations/inversions
	public static LightmapHd find(AoFaceData faceData) {
		LightmapHd result = MAP.get(faceData);

		if (result == null) {
			synchronized (MAP) {
				result = MAP.get(faceData);

				if (result == null) {
					result = new LightmapHd(faceData);
					MAP.put(faceData.clone(), result);
				}
			}
		}

		return result;
	}

	/**
	 * Handles padding.
	 */
	public int pixel(int u, int v) {
		return light[v * LightmapSizer.paddedSize + u];
	}

	public int coord(QuadEditorImpl q, int i) {
		final int u, v;

		u = Math.round((uMinImg + 0.5f + q.u[i] * LightmapSizer.centerToCenterPixelDistance) * LightmapSizer.textureToBuffer);
		v = Math.round((vMinImg + 0.5f + q.v[i] * LightmapSizer.centerToCenterPixelDistance) * LightmapSizer.textureToBuffer);

		return u | (v << 16);
	}
}
