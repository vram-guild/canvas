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

package grondag.canvas.terrain.occlusion.geometry;

public record OcclusionResult(int[] occlusionData, long mutalFaceMask) {
	public boolean areFacesOpen(int fromFaceIndex, int toFaceIndex) {
		return (mutalFaceMask & mask(fromFaceIndex, toFaceIndex)) != 0L;
	}

	// packing is inefficient (only 30 bits really needed) but avoids multiply
	private static long mask(int highIndex, int lowIndex) {
		return 1L << ((highIndex << 3) | lowIndex);
	}

	private static long mutualMask(int faceIndexA, int faceIndexB) {
		return faceIndexA > faceIndexB ? mask(faceIndexA, faceIndexB) : mask(faceIndexB, faceIndexA);
	}

	public static long buildMutualFaceMask(int mutualFaceMasks) {
		assert (mutualFaceMasks & 0b111111) == mutualFaceMasks : "More than six face bits provided to mutual mask input";
		long result = 0L;

		for (int i = 0; i < 5; ++i) {
			if (((1 << i) & mutualFaceMasks) != 0) {
				for (int j = i + 1; j < 6; ++j) {
					if (((1 << j) & mutualFaceMasks) != 0) {
						result |= mutualMask(i, j);
					}
				}
			}
		}

		return result;
	}

	/**
	 * For terrain iteration face-based culling.
	 * @param mutualFaceMask indicates which faces are connected, built with {@link #buildMutualFaceMask(int)}
	 * @param fromFaceFlags BIT FLAGS for faces visible from outside the section from which it was entered
	 * @param toFaceFlag INDEX (0-5) of exit face being tested
	 * @return true if the exit face is open to any of the entry faces
	 */
	public static boolean canVisitFace(long mutualFaceMask, int fromFaceFlags, int toFaceIndex) {
		assert toFaceIndex >= 0;
		assert toFaceIndex < 6;

		if (fromFaceFlags == 0) {
			return false;
		}

		if ((fromFaceFlags & 1) != 0 && (mutualMask(toFaceIndex, 0) & mutualFaceMask) != 0) {
			return true;
		}

		if ((fromFaceFlags & 2) != 0 && (mutualMask(toFaceIndex, 1) & mutualFaceMask) != 0) {
			return true;
		}

		if ((fromFaceFlags & 4) != 0 && (mutualMask(toFaceIndex, 2) & mutualFaceMask) != 0) {
			return true;
		}

		if ((fromFaceFlags & 8) != 0 && (mutualMask(toFaceIndex, 3) & mutualFaceMask) != 0) {
			return true;
		}

		if ((fromFaceFlags & 16) != 0 && (mutualMask(toFaceIndex, 4) & mutualFaceMask) != 0) {
			return true;
		}

		if ((fromFaceFlags & 32) != 0 && (mutualMask(toFaceIndex, 5) & mutualFaceMask) != 0) {
			return true;
		}

		return false;
	}
}
