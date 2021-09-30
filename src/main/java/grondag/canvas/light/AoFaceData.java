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

/**
 * Holds per-corner results for a single block face. Handles caching and
 * provides various utility methods to simplify code elsewhere.
 */
public class AoFaceData {
	public static final int OPAQUE = -1;
	public final AoFaceCalc calc = new AoFaceCalc();
	// packed values gathered during compute
	public int bottom;
	public int top;
	public int left;
	public int right;
	public int bottomLeft;
	public int bottomRight;
	public int topLeft;
	public int topRight;
	public int center;
	// these values are fully computed at gather time
	public int aoBottom;
	public int aoTop;
	public int aoLeft;
	public int aoRight;
	public int aoBottomLeft;
	public int aoBottomRight;
	public int aoTopLeft;
	public int aoTopRight;
	public int aoCenter;
	private int hashCode;

	public static void blendTo(AoFaceData in0, float w0, AoFaceData in1, float w1, AoFaceData out) {
		out.top = lightBlend(in0.top, w0, in1.top, w1);
		out.left = lightBlend(in0.left, w0, in1.left, w1);
		out.right = lightBlend(in0.right, w0, in1.right, w1);
		out.bottom = lightBlend(in0.bottom, w0, in1.bottom, w1);

		out.topLeft = lightBlend(in0.topLeft, w0, in1.topLeft, w1);
		out.topRight = lightBlend(in0.topRight, w0, in1.topRight, w1);
		out.bottomLeft = lightBlend(in0.bottomLeft, w0, in1.bottomLeft, w1);
		out.bottomRight = lightBlend(in0.bottomRight, w0, in1.bottomRight, w1);

		out.center = lightBlend(in0.center, w0, in1.center, w1);

		out.aoTopLeft = Math.round(in0.aoTopLeft * w0 + in1.aoTopLeft * w1);
		out.aoTopRight = Math.round(in0.aoTopRight * w0 + in1.aoTopRight * w1);
		out.aoBottomLeft = Math.round(in0.aoBottomLeft * w0 + in1.aoBottomLeft * w1);
		out.aoBottomRight = Math.round(in0.aoBottomRight * w0 + in1.aoBottomRight * w1);
		out.aoTop = Math.round(in0.aoTop * w0 + in1.aoTop * w1);
		out.aoRight = Math.round(in0.aoRight * w0 + in1.aoRight * w1);
		out.aoLeft = Math.round(in0.aoLeft * w0 + in1.aoLeft * w1);
		out.aoBottom = Math.round(in0.aoBottom * w0 + in1.aoBottom * w1);
		out.aoCenter = Math.round(in0.aoCenter * w0 + in1.aoCenter * w1);

		out.updateHash();
	}

	private static int lightBlend(int l0, float w0, int l1, float w1) {
		if (l0 == OPAQUE) {
			if (l1 == OPAQUE) {
				return OPAQUE;
			} else {
				return reduce(l1);
			}
		} else {
			if (l1 == OPAQUE) {
				return reduce(l0);
			} else {
				return lightBlendInner(l0, w0, l1, w1);
			}
		}
	}

	private static int lightBlendInner(int l0, float w0, int l1, float w1) {
		final int b0 = (l0 & 0xFF);
		final int k0 = ((l0 >> 16) & 0xFF);
		final int b1 = (l1 & 0xFF);
		final int k1 = ((l1 >> 16) & 0xFF);
		final float b = b0 * w0 + b1 * w1;
		final float k = k0 * w0 + k1 * w1;
		return Math.round(b) | (Math.round(k) << 16);
	}

	private static int reduce(int light) {
		final int block = (light & 0xFF) - 16;
		final int sky = ((light >> 16) & 0xFF) - 16;
		return Math.max(0, block) | (Math.max(0, sky) << 16);
	}

	public void setFlat(int flatBrightness) {
		bottom = flatBrightness;
		top = flatBrightness;
		left = flatBrightness;
		right = flatBrightness;
		bottomLeft = flatBrightness;
		bottomRight = flatBrightness;
		topLeft = flatBrightness;
		topRight = flatBrightness;
		center = flatBrightness;

		aoBottomLeft = 255;
		aoBottomRight = 255;
		aoTopLeft = 255;
		aoTopRight = 255;
		aoBottom = 255;
		aoRight = 255;
		aoLeft = 255;
		aoTop = 255;
		aoCenter = 255;

		updateHash();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof AoFaceData)) {
			return false;
		}

		final AoFaceData o = (AoFaceData) other;

		return o.bottom == bottom
				&& o.top == top
				&& o.left == left
				&& o.right == right
				&& o.bottomLeft == bottomLeft
				&& o.bottomRight == bottomRight
				&& o.topLeft == topLeft
				&& o.topRight == topRight
				&& o.center == center

				&& o.aoBottomLeft == aoBottomLeft
				&& o.aoBottomRight == aoBottomRight
				&& o.aoTopLeft == aoTopLeft
				&& o.aoTopRight == aoTopRight
				&& o.aoLeft == aoLeft
				&& o.aoBottom == aoBottom
				&& o.aoTop == aoTop
				&& o.aoRight == aoRight
				&& o.aoCenter == aoCenter;
	}

	@Override
	public AoFaceData clone() {
		final AoFaceData result = new AoFaceData();

		result.bottom = bottom;
		result.top = top;
		result.left = left;
		result.right = right;
		result.bottomLeft = bottomLeft;
		result.bottomRight = bottomRight;
		result.topLeft = topLeft;
		result.topRight = topRight;
		result.center = center;

		result.aoBottomLeft = aoBottomLeft;
		result.aoBottomRight = aoBottomRight;
		result.aoTopLeft = aoTopLeft;
		result.aoTopRight = aoTopRight;
		result.aoBottom = aoBottom;
		result.aoRight = aoRight;
		result.aoLeft = aoLeft;
		result.aoTop = aoTop;
		result.aoCenter = aoCenter;

		result.hashCode = hashCode;

		return result;
	}

	public void updateHash() {
		int h = bottom;
		h = 31 * h + top;
		h = 31 * h + left;
		h = 31 * h + right;
		h = 31 * h + bottomLeft;
		h = 31 * h + bottomRight;
		h = 31 * h + topLeft;
		h = 31 * h + topRight;
		h = 31 * h + center;
		h = 31 * h + aoBottomLeft;
		h = 31 * h + aoBottomRight;
		h = 31 * h + aoTopLeft;
		h = 31 * h + aoTopRight;
		h = 31 * h + aoBottom;
		h = 31 * h + aoRight;
		h = 31 * h + aoLeft;
		h = 31 * h + aoTop;
		h = 31 * h + aoCenter;
		hashCode = h;
	}
}
