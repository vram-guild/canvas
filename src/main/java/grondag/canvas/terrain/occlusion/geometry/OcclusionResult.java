/*
 * Copyright © Original Authors
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
