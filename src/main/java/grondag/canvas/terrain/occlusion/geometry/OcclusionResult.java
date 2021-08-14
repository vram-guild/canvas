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
	public static long buildMutualFaceMask(long mutualFaceMasks) {
		assert (mutualFaceMasks & 0b111111) == mutualFaceMasks : "More than six face bits provided to mutual mask input";
		long result = 0L;

		for (int i = 0; i < 6; ++i) {
			if (((1 << i) & mutualFaceMasks) != 0) {
				result |= (mutualFaceMasks << (i << 3));
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
	public static int openFacesFlag(long mutualFaceMask, int fromFaceFlags) {
		if (fromFaceFlags == 0 || mutualFaceMask == 0) {
			return 0;
		}

		int result = 0;

		if ((fromFaceFlags & 1) != 0) {
			result |= (mutualFaceMask & 63);
		}

		if ((fromFaceFlags & 2) != 00) {
			result |= ((mutualFaceMask >> 8) & 63);
		}

		if ((fromFaceFlags & 4) != 0) {
			result |= ((mutualFaceMask >> 16) & 63);
		}

		if ((fromFaceFlags & 8) != 0) {
			result |= ((mutualFaceMask >> 24) & 63);
		}

		if ((fromFaceFlags & 16) != 0) {
			result |= ((mutualFaceMask >> 32) & 63);
		}

		if ((fromFaceFlags & 32) != 0) {
			result |= ((mutualFaceMask >> 40) & 63);
		}

		return result;
	}
}
