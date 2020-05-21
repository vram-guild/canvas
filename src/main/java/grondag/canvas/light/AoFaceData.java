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

/**
 * Holds per-corner results for a single block face. Handles caching and
 * provides various utility methods to simplify code elsewhere.
 */
public class AoFaceData {

	public static final int OPAQUE = -1;

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
	int aoBottomLeft;
	int aoBottomRight;
	int aoTopLeft;
	int aoTopRight;

	public final AoFaceCalc calc = new AoFaceCalc();

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
	}

	private static int lightBlend(int l0, float w0, int l1, float w1) {
		if(l0 == OPAQUE) {
			if(l1 == OPAQUE) {
				return OPAQUE;
			} else {
				return reduce(l1);
			}
		} else {
			if(l1 == OPAQUE) {
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
}
