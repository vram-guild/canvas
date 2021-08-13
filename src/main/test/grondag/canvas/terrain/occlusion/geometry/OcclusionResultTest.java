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

import org.junit.jupiter.api.Test;

class OcclusionResultTest {
	private static final int BIT_A = 1;
	private static final int BIT_B = 2;
	private static final int BIT_C = 4;
	private static final int BIT_D = 8;
	private static final int BIT_E = 16;
	private static final int BIT_F = 32;

	@Test
	void test() {
		long mutualMask = 0L;

		assert OcclusionResult.openFacesFlag(mutualMask, 63) == 0;

		mutualMask |= OcclusionResult.buildMutualFaceMask(BIT_A | BIT_B);

		int openMask = OcclusionResult.openFacesFlag(mutualMask, BIT_A);
		assert openMask == (BIT_A | BIT_B);

		openMask = OcclusionResult.openFacesFlag(mutualMask, BIT_B);
		assert openMask == (BIT_A | BIT_B);

		openMask = OcclusionResult.openFacesFlag(mutualMask, BIT_A | BIT_B);
		assert openMask == (BIT_A | BIT_B);

		openMask = OcclusionResult.openFacesFlag(mutualMask, 63);
		assert openMask == (BIT_A | BIT_B);

		openMask = OcclusionResult.openFacesFlag(mutualMask, 0);
		assert openMask == 0;

		openMask = OcclusionResult.openFacesFlag(mutualMask, BIT_C);
		assert openMask == 0;

		openMask = OcclusionResult.openFacesFlag(mutualMask, BIT_C | BIT_D | BIT_E | BIT_F);
		assert openMask == 0;

		mutualMask |= OcclusionResult.buildMutualFaceMask(BIT_B | BIT_D | BIT_E);

		openMask = OcclusionResult.openFacesFlag(mutualMask, BIT_A);
		assert openMask == (BIT_A | BIT_B);

		openMask = OcclusionResult.openFacesFlag(mutualMask, BIT_B);
		assert openMask == (BIT_A | BIT_B | BIT_D | BIT_E);

		openMask = OcclusionResult.openFacesFlag(mutualMask, BIT_D);
		assert openMask == (BIT_B | BIT_D | BIT_E);

		openMask = OcclusionResult.openFacesFlag(mutualMask, BIT_C);
		assert openMask == 0;
	}
}
