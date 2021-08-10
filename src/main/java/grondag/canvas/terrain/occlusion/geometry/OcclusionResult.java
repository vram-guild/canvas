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

	public static long mask(int fromFaceIndex, int toFaceIndex) {
		return 1L << ((fromFaceIndex << 3) | toFaceIndex);
	}

	public static long mutualMask(int faceIndexA, int faceIndexB) {
		return mask(faceIndexA, faceIndexB) | mask(faceIndexB, faceIndexA);
	}

	public static long buildMutualFaceMask(int mutualFaces) {
		assert (mutualFaces & 0b111111) == mutualFaces : "More than six face bits provided to mutual mask input";
		long result = 0L;

		for (int i = 0; i < 5; ++i) {
			if (((1 << i) & mutualFaces) != 0) {
				for (int j = i + 1; j < 6; ++j) {
					if (((1 << j) & mutualFaces) != 0) {
						result |= mutualMask(i, j);
					}
				}
			}
		}

		return result;
	}
}
