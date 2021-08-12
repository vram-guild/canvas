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
	private static final int IDX_A = 0;
	private static final int IDX_B = 1;
	private static final int IDX_C = 2;
	private static final int IDX_D = 3;
	private static final int IDX_E = 4;
	private static final int IDX_F = 5;

	private static final int BIT_A = 1 << IDX_A;
	private static final int BIT_B = 1 << IDX_B;
	private static final int BIT_C = 1 << IDX_C;
	private static final int BIT_D = 1 << IDX_D;
	private static final int BIT_E = 1 << IDX_E;
	private static final int BIT_F = 1 << IDX_F;

	@Test
	void test() {
		long mutualMask = 0L;

		for (int i = 0; i < 6; ++i) {
			for (int j = i; j < 6; ++j) {
				assert !OcclusionResult.canVisitFace(mutualMask, 1 << i, j);
			}
		}

		mutualMask |= OcclusionResult.buildMutualFaceMask(BIT_A | BIT_B);
		assert OcclusionResult.canVisitFace(mutualMask, BIT_A, IDX_B);
		assert OcclusionResult.canVisitFace(mutualMask, BIT_B, IDX_A);
		assert !OcclusionResult.canVisitFace(mutualMask, BIT_B, IDX_C);
		assert !OcclusionResult.canVisitFace(mutualMask, BIT_C, IDX_B);
		assert !OcclusionResult.canVisitFace(mutualMask, BIT_D, IDX_E);

		mutualMask |= OcclusionResult.buildMutualFaceMask(BIT_B | BIT_D | BIT_E);

		assert OcclusionResult.canVisitFace(mutualMask, BIT_A, IDX_B);
		assert OcclusionResult.canVisitFace(mutualMask, BIT_B, IDX_A);
		assert OcclusionResult.canVisitFace(mutualMask, BIT_D, IDX_B);
		assert OcclusionResult.canVisitFace(mutualMask, BIT_D, IDX_E);
		assert OcclusionResult.canVisitFace(mutualMask, BIT_B, IDX_E);
		assert !OcclusionResult.canVisitFace(mutualMask, BIT_B, IDX_C);
		assert !OcclusionResult.canVisitFace(mutualMask, BIT_A, IDX_C);
		assert !OcclusionResult.canVisitFace(mutualMask, BIT_E, IDX_F);
		assert !OcclusionResult.canVisitFace(mutualMask, BIT_F, IDX_A);
	}
}
