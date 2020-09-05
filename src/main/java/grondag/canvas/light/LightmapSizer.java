/*
 * Copyright 2019, 2020 grondag
 *
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
 */

package grondag.canvas.light;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;

import grondag.canvas.Configurator;

public final class LightmapSizer {
	public static final int texSize = Configurator.moreLightmap ? 8192 : 4096;
	public static final int paddedSize = 6;
	public static final int centerToCenterPixelDistance = paddedSize - 1;
	public static final int radius = paddedSize / 2;
	public static final int lightmapPixels = paddedSize * paddedSize;
	public static final int mapsPerAxis = texSize / paddedSize;
	public static final int maxCount = mapsPerAxis * mapsPerAxis;
	public static final float pixelUnitFraction = 1f / centerToCenterPixelDistance;
	public static final float centralPixelDistance = pixelUnitFraction / 2;
	// UGLY - consider making this a full unsigned short
	// for initial pass didn't want to worry about signed value mistakes
	/** Scale of texture units sent to shader. Shader should divide by this. */
	public static final int bufferScale = 0x8000;
	public static final float textureToBuffer = (float) bufferScale / texSize;

	/** converts zero-based distance from center to u/v index - use for top/left */
	public static final Int2IntFunction NEG = i -> radius - 1 - i;
	/** converts zero-based distance from center to u/v index - use for bottom/right */
	public static final Int2IntFunction POS = i -> radius + i;
}
