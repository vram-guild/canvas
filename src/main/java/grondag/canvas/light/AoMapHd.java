/*******************************************************************************
 * Copyright 2019, 2020 grondag
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package grondag.canvas.light;

import static grondag.canvas.light.LightmapHd.lightIndex;

import net.minecraft.util.math.MathHelper;

final class AoMapHd {

	static void computeAo(int[] light, AoFaceData faceData) {
		final float topLeft = faceData.aoTopLeft / 255f;
		final float topRight = faceData.aoTopRight / 255f;
		final float bottomRight = faceData.aoBottomRight / 255f;
		final float bottomLeft = faceData.aoBottomLeft / 255f;

		for(int u = 0; u < LightmapSizer.paddedSize; u++) {
			for(int v = 0; v < LightmapSizer.paddedSize; v++) {
				final float uDist = (float)u / LightmapSizer.centerToCenterPixelDistance;
				final float vDist = (float)v / LightmapSizer.centerToCenterPixelDistance;

				final float tl = (1 - uDist) * (1 - vDist) * topLeft;
				final float tr = uDist * (1 - vDist) * topRight;
				final float br = uDist * vDist * bottomRight;
				final float bl = (1 - uDist) * vDist * bottomLeft;
				light[lightIndex(u, v)] = output(tl + tr + br + bl);
			}
		}
	}

	static int output(float in) {
		return MathHelper.clamp(Math.round(in * 255), 0, 255);
	}
}
