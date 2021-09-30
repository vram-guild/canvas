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

import it.unimi.dsi.fastutil.ints.Int2IntFunction;

public final class LightmapSizer {
	public static final int texSize = 8192; //Configurator.moreLightmap ? 8192 : 4096;
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
	/**
	 * Scale of texture units sent to shader. Shader should divide by this.
	 */
	public static final int bufferScale = 0x8000;
	public static final float textureToBuffer = (float) bufferScale / texSize;

	/**
	 * Converts zero-based distance from center to u/v index - use for top/left.
	 */
	public static final Int2IntFunction NEG = i -> radius - 1 - i;
	/**
	 * Converts zero-based distance from center to u/v index - use for bottom/right.
	 */
	public static final Int2IntFunction POS = i -> radius + i;
}
