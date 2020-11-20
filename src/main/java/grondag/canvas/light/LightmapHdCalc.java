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

import static grondag.canvas.light.LightmapHd.lightIndex;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;

final class LightmapHdCalc {
	static float input(int b, boolean isSky) {
		return b == AoFaceData.OPAQUE ? AoFaceData.OPAQUE : (isSky ? (b >> 16) & 0xFF : b & 0xFF);
	}

	static void computeLight(int[] light, AoFaceData faceData, boolean isSky) {
		// PERF: use integer math
		final float center = input(faceData.center, isSky);
		final float top = input(faceData.top, isSky);
		final float bottom = input(faceData.bottom, isSky);
		final float right = input(faceData.right, isSky);
		final float left = input(faceData.left, isSky);
		final float topLeft = input(faceData.topLeft, isSky);
		final float topRight = input(faceData.topRight, isSky);
		final float bottomRight = input(faceData.bottomRight, isSky);
		final float bottomLeft = input(faceData.bottomLeft, isSky);

		// Note: won't work for other than 4x4 interior, 6x6 padded
		computeQuadrant(center, left, top, topLeft, light, LightmapSizer.NEG, LightmapSizer.NEG);
		computeQuadrant(center, right, top, topRight, light, LightmapSizer.POS, LightmapSizer.NEG);
		computeQuadrant(center, left, bottom, bottomLeft, light, LightmapSizer.NEG, LightmapSizer.POS);
		computeQuadrant(center, right, bottom, bottomRight, light, LightmapSizer.POS, LightmapSizer.POS);
	}

	private static void computeQuadrant(float center, float uSide, float vSide, float corner, int[] light, Int2IntFunction uFunc, Int2IntFunction vFunc) {
		//FIX: handle error case when center is missing
		if (uSide == AoFaceData.OPAQUE) {
			if (vSide == AoFaceData.OPAQUE) {
				// fully enclosed
				computeOpen(center, center - 8f, center - 8f, center - 8f, light, uFunc, vFunc);
			} else if (corner == AoFaceData.OPAQUE) {
				// U + corner enclosing
				uSide = center - 4f;
				computeClamped(center, uSide, (vSide + center) * 0.5f, (uSide + vSide - 4f) * 0.5f, light, uFunc, vFunc);
			} else {
				// U side enclosing
				final float join = (center + vSide + corner) / 3f;
				computeClamped(center, center - 4f, (vSide + center) * 0.5f, join, light, uFunc, vFunc);
			}
		} else if (vSide == AoFaceData.OPAQUE) {
			if (corner == AoFaceData.OPAQUE) {
				// V + corner enclosing
				vSide = center - 4f;
				computeClamped(center, (uSide + center) * 0.5f, vSide, (uSide + vSide - 4f) * 0.5f, light, uFunc, vFunc);
			} else {
				// V side enclosing
				final float join = (center + uSide + corner) / 3f;
				computeClamped(center, (uSide + center) * 0.5f, center - 4f, join, light, uFunc, vFunc);
			}
		} else if (corner == AoFaceData.OPAQUE) {
			// opaque corner
			final float join = (center + uSide + vSide) / 3f;
			computeClamped(center, (uSide + center) * 0.5f, (vSide + center) * 0.5f, join, light, uFunc, vFunc);
		} else {
			// all open
			computeOpen(center, uSide, vSide, corner, light, uFunc, vFunc);
		}
	}

	/* interpolates center-to-center */
	static void computeOpen(float center, float uSide, float vSide, float corner, int[] light, Int2IntFunction uFunc, Int2IntFunction vFunc) {
		for (int u = 0; u < LightmapSizer.radius; u++) {
			for (int v = 0; v < LightmapSizer.radius; v++) {
				final float uLinear = 1f - LightmapSizer.centralPixelDistance - u * LightmapSizer.pixelUnitFraction;
				final float vLinear = 1f - LightmapSizer.centralPixelDistance - v * LightmapSizer.pixelUnitFraction;

				assert uLinear >= 0 && uLinear <= 1f;
				assert vLinear >= 0 && vLinear <= 1f;

				final float linear = center * (uLinear * vLinear)
						+ corner * (1 - uLinear) * (1 - vLinear)
						+ uSide * ((1 - uLinear) * (vLinear))
						+ vSide * ((uLinear) * (1 - vLinear));

				light[lightIndex(uFunc.applyAsInt(u), vFunc.applyAsInt(v))] = output(linear);
			}
		}
	}

	/* interpolates center-to-corner */
	private static void computeClamped(float center, float uSide, float vSide, float corner, int[] light, Int2IntFunction uFunc, Int2IntFunction vFunc) {
		for (int u = 0; u < LightmapSizer.radius; u++) {
			for (int v = 0; v < LightmapSizer.radius; v++) {
				final float uLinear = 1f - LightmapSizer.pixelUnitFraction - u * LightmapSizer.pixelUnitFraction * 2f;
				final float vLinear = 1f - LightmapSizer.pixelUnitFraction - v * LightmapSizer.pixelUnitFraction * 2f;

				assert uLinear >= 0 && uLinear <= 1f;
				assert vLinear >= 0 && vLinear <= 1f;

				final float linear = center * (uLinear * vLinear)
						+ corner * (1 - uLinear) * (1 - vLinear)
						+ uSide * ((1 - uLinear) * (vLinear))
						+ vSide * ((uLinear) * (1 - vLinear));

				light[lightIndex(uFunc.applyAsInt(u), vFunc.applyAsInt(v))] = output(linear);
			}
		}
	}

	static int output(float in) {
		int result = Math.round(in);

		if (result < 0) {
			result = 0;
		} else if (result > 255) {
			result = 255;
		}

		return result;
	}

	/* interpolates center-to-corner */
	private static void computeClampedAo(int center, int uSide, int vSide, int corner, int[] light, Int2IntFunction uFunc, Int2IntFunction vFunc) {
		for (int u = 0; u < LightmapSizer.radius; u++) {
			for (int v = 0; v < LightmapSizer.radius; v++) {
				final float uLinear = 1f - LightmapSizer.pixelUnitFraction - u * LightmapSizer.pixelUnitFraction * 2f;
				final float vLinear = 1f - LightmapSizer.pixelUnitFraction - v * LightmapSizer.pixelUnitFraction * 2f;

				assert uLinear >= 0 && uLinear <= 1f;
				assert vLinear >= 0 && vLinear <= 1f;

				final float linear = center * (uLinear * vLinear)
						+ corner * (1 - uLinear) * (1 - vLinear)
						+ uSide * ((1 - uLinear) * (vLinear))
						+ vSide * ((uLinear) * (1 - vLinear));

				light[lightIndex(uFunc.applyAsInt(u), vFunc.applyAsInt(v))] = outputAo(Math.round(linear));
			}
		}
	}

	static int outputAo(int in) {
		// non-linear curve towards dark
		//		in = (in + ((in * in) >> 8) >> 1);

		if (in < 0) {
			in = 0;
		} else if (in > 255) {
			in = 255;
		}

		return in;
	}

	static int aoCorner(int a, int b, int c, int d) {
		if (a < 0xFF) {
			if (b < 0xFF || c < 0xFF || d < 0xFF) {
				return (a + b + c + d + 1) >> 2;
			} else {
				return (a + a + b + c + d) / 5;
			}
		} else if (b < 0xFF) {
			if (c < 0xFF || d < 0xFF) {
				return (a + b + c + d + 1) >> 2;
			} else {
				return (a + b + b + c + d) / 5;
			}
		} else if (c < 0xFF) {
			if (d < 0xFF) {
				return (a + b + c + d + 1) >> 2;
			} else {
				return (a + b + c + c + d) / 5;
			}
		} else if (d < 0xFF) {
			return (a + b + c + d + d) / 5;
		} else {
			return 0xFF;
		}
	}

	static void computeAo(int[] light, AoFaceData faceData) {
		// final float FACTOR = 0.6f;
		//		final float topLeft = faceData.aoTopLeft; //FACTOR * (255f - faceData.aoTopLeft);
		//		final float topRight = faceData.aoTopRight; //FACTOR * (255f - faceData.aoTopRight);
		//		final float bottomRight = faceData.aoBottomRight; //FACTOR * (255f - faceData.aoBottomRight);
		//		final float bottomLeft = faceData.aoBottomLeft; //FACTOR * (255f - faceData.aoBottomLeft);
		//
		//		final float top = faceData.aoTop; //FACTOR * (255f - faceData.aoTop);
		//		final float right = faceData.aoRight; //FACTOR * (255f - faceData.aoRight);
		//		final float bottom = faceData.aoBottom; //FACTOR * (255f - faceData.aoBottom);
		//		final float left = faceData.aoLeft; //FACTOR * (255f - faceData.aoLeft);
		//
		//		final float center = faceData.aoCenter;

		final int topLeft = aoCorner(faceData.aoTop, faceData.aoTopLeft, faceData.aoLeft, faceData.aoCenter);
		final int topRight = aoCorner(faceData.aoTop, faceData.aoTopRight, faceData.aoRight, faceData.aoCenter);
		final int bottomRight = aoCorner(faceData.aoBottom, faceData.aoBottomRight, faceData.aoRight, faceData.aoCenter);
		final int bottomLeft = aoCorner(faceData.aoBottom, faceData.aoBottomLeft, faceData.aoLeft, faceData.aoCenter);

		final int center = faceData.aoCenter; //(topLeft + topRight + bottomRight + bottomLeft) * 0.25f;

		final int top = ((faceData.aoTop + center + 1) >> 1); //FACTOR * (255f - faceData.aoTop);
		final int right = ((faceData.aoRight + center + 1) >> 1); //FACTOR * (255f - faceData.aoRight);
		final int bottom = ((faceData.aoBottom + center + 1) >> 1); //FACTOR * (255f - faceData.aoBottom);
		final int left = ((faceData.aoLeft + center + 1) >> 1); //FACTOR * (255f - faceData.aoLeft);

		computeClampedAo(center, left, top, topLeft, light, LightmapSizer.NEG, LightmapSizer.NEG);
		computeClampedAo(center, right, top, topRight, light, LightmapSizer.POS, LightmapSizer.NEG);
		computeClampedAo(center, left, bottom, bottomLeft, light, LightmapSizer.NEG, LightmapSizer.POS);
		computeClampedAo(center, right, bottom, bottomRight, light, LightmapSizer.POS, LightmapSizer.POS);

		//		for(int u = 0; u < LightmapSizer.paddedSize; u++) {
		//			for(int v = 0; v < LightmapSizer.paddedSize; v++) {
		//				final float uDist = (float)u / LightmapSizer.centerToCenterPixelDistance;
		//				final float vDist = (float)v / LightmapSizer.centerToCenterPixelDistance;
		//				final float uInvDist = 1 - uDist;
		//				final float uInvSq  = uInvDist * uInvDist;
		//
		//				final float uSq = uDist * uDist;
		//				final float vSq = vDist * vDist;
		//
		//
		//				final float vInvDist = 1 - vDist;
		//				final float vInvSq  = vInvDist;// * vInvDist;
		//
		//				float ao = 1;
		//
		//				//				ao -= topRight * (uSq + vInvSq);
		//				//				ao -= bottomRight * (uSq + vSq);
		//				//				ao -= bottomLeft * (uInvSq + vSq);
		//				ao -= top * vInvSq;
		//				//				ao -= bottom * vSq;
		//				//				ao -= left * uSq;
		//				//				ao -= right * uInvSq;
		//
		//				//				final float tl = (1 - uDist) * (1 - vDist) * topLeft;
		//				//				final float tr = uDist * (1 - vDist) * topRight;
		//				//				final float br = uDist * vDist * bottomRight;
		//				//				final float bl = (1 - uDist) * vDist * bottomLeft;
		//				light[lightIndex(u, v)] = outputAo(ao < 0 ? 0 : ao);
		//			}
		//		}
	}

	//	private static void computeQuadrantAo(float center, float uSide, float vSide, float corner, int light[], Int2IntFunction uFunc, Int2IntFunction vFunc) {
	//		//FIX: handle error case when center is missing
	//		if(uSide == AoFaceData.OPAQUE) {
	//			if(vSide == AoFaceData.OPAQUE) {
	//				// fully enclosed
	//				computeOpen(center, center - 0.5f, center - 0.5f, center - 0.5f, light, uFunc, vFunc);
	//			} else if (corner == AoFaceData.OPAQUE) {
	//				// U + corner enclosing
	//				uSide = center - 0.25f;
	//				computeClamped(center, uSide, (vSide + center) * 0.5f, (uSide + vSide - 0.25f) * 0.5f, light, uFunc, vFunc);
	//			} else {
	//				// U side enclosing
	//				final float join = (center + vSide + corner) / 3f;
	//				computeClamped(center, center - 0.25f, (vSide + center) * 0.5f, join, light, uFunc, vFunc);
	//			}
	//		} else if(vSide == AoFaceData.OPAQUE) {
	//			if(corner == AoFaceData.OPAQUE) {
	//				// V + corner enclosing
	//				vSide = center - 0.25f;
	//				computeClamped(center, (uSide + center) * 0.5f, vSide, (uSide + vSide - 0.25f) * 0.5f, light, uFunc, vFunc);
	//			} else {
	//				// V side enclosing
	//				final float join = (center + uSide + corner) / 3f;
	//				computeClamped(center, (uSide + center) * 0.5f, center - 0.25f, join, light, uFunc, vFunc);
	//			}
	//
	//		} else if(corner == AoFaceData.OPAQUE) {
	//			// opaque corner
	//			final float join = (center + uSide + vSide) / 3f;
	//			computeClamped(center, (uSide + center) * 0.5f, (vSide + center) * 0.5f, join, light, uFunc, vFunc);
	//		} else {
	//			// all open
	//			computeOpen(center, uSide, vSide, corner, light, uFunc, vFunc);
	//		}
	//	}
}
