/*
 * Copyright 2019, 2020 grondag
 *
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
 */

package grondag.canvas.terrain.occlusion.region;

public abstract class PackedBox {
	private PackedBox() { }

	private static final int SHIFT_X0 = 0;
	private static final int SHIFT_Y0 = SHIFT_X0 + 5;
	private static final int SHIFT_Z0 = SHIFT_Y0 + 5;
	private static final int SHIFT_X1 = SHIFT_Z0 + 5;
	private static final int SHIFT_Y1 = SHIFT_X1 + 5;
	private static final int SHIFT_Z1 = SHIFT_Y1 + 5;
	private static final int SHIFT_RANGE = SHIFT_Z1 + 5;

	public static final int RANGE_NEAR = 0;
	public static final int RANGE_MID = 1;
	public static final int RANGE_FAR = 2;
	public static final int RANGE_EXTREME = 3;

	public static final int CHUNK_DIST_NEAR = 1;
	public static final int CHUNK_DIST_MID = 2;
	public static final int CHUNK_DIST_FAR = 8;

	public static final int SQUARE_BLOCK_DIST_NEAR = CHUNK_DIST_NEAR * CHUNK_DIST_NEAR * 16 * 16 * 3;
	public static final int SQUARE_BLOCK_DIST_MID = CHUNK_DIST_MID * CHUNK_DIST_MID * 16 * 16 * 3;
	public static final int SQUARE_BLOCK_DIST_FAR = CHUNK_DIST_FAR * CHUNK_DIST_FAR * 16 * 16 * 3;

	public static int rangeFromSquareBlockDist(int squareBlockDist) {
		if (squareBlockDist <= SQUARE_BLOCK_DIST_MID) {
			return squareBlockDist > SQUARE_BLOCK_DIST_NEAR ? RANGE_MID : RANGE_NEAR;
		} else {
			return squareBlockDist > SQUARE_BLOCK_DIST_FAR ? RANGE_EXTREME: RANGE_FAR;
		}
	}

	public static int pack(int x0, int y0, int z0, int x1, int y1, int z1, int range) {
		return x0 | (y0 << SHIFT_Y0) | (z0 << SHIFT_Z0)
				| ((x1) << SHIFT_X1) | ((y1) << SHIFT_Y1) | ((z1) << SHIFT_Z1)
				| (range << SHIFT_RANGE);
	}

	public static int range(int packed) {
		return (packed >>> SHIFT_RANGE) & 3;
	}

	public static int x0(int packed) {
		return (packed & 31);
	}

	public static int y0(int packed) {
		return (packed >>> SHIFT_Y0) & 31;
	}

	public static int z0(int packed) {
		return (packed >>> SHIFT_Z0) & 31;
	}

	public static int x1(int packed) {
		return ((packed >>> SHIFT_X1) & 31);
	}

	public static int y1(int packed) {
		return ((packed >>> SHIFT_Y1) & 31);
	}

	public static int z1(int packed) {
		return ((packed >>> SHIFT_Z1) & 31);
	}

	public static String toString(int packed) {
		return "(" + x0(packed) + ", " + y0(packed) + ", " + z0(packed) + "), ("
				+ x1(packed) + ", " + y1(packed) + ", " + z1(packed) + ")";
	}

	public static final int FULL_BOX = pack(0, 0, 0, 16, 16, 16, RANGE_EXTREME);

	public static final int EMPTY_BOX = 0;
}
