/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.light;

import java.util.concurrent.atomic.AtomicInteger;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.math.MathHelper;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;

class LightmapHd {
	// PERF: use Fermion cache
	static final Object2ObjectOpenHashMap<AoFaceData, LightmapHd> MAP = new Object2ObjectOpenHashMap<>(MathHelper.smallestEncompassingPowerOfTwo(LightmapSizer.maxCount), LightmapSizer.maxCount / (float) MathHelper.smallestEncompassingPowerOfTwo(LightmapSizer.maxCount));
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
				CanvasMod.LOG.warn(I18n.translate("error.canvas.fail_create_lightmap"));
				errorNoticeNeeded = false;
			}
		} else {
			// PERF: pool these and the main array - not needed after upload

			final int[] aoLight = new int[LightmapSizer.lightmapPixels];
			final int[] skyLight = new int[LightmapSizer.lightmapPixels];
			final int[] blockLight = new int[LightmapSizer.lightmapPixels];

			// TODO: make this an option for AO debugging
			//			Arrays.fill(skyLight, 255);
			//			Arrays.fill(blockLight, 255);

			// PERF: skips steps when all unit value or same  value
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

	// PERF: can reduce texture consumption 8X by reusing rotations/inversions
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

	public int coord(MutableQuadViewImpl q, int i) {
		final int u, v;

		u = Math.round((uMinImg + 0.5f + q.u[i] * LightmapSizer.centerToCenterPixelDistance) * LightmapSizer.textureToBuffer);
		v = Math.round((vMinImg + 0.5f + q.v[i] * LightmapSizer.centerToCenterPixelDistance) * LightmapSizer.textureToBuffer);

		return u | (v << 16);
	}
}
