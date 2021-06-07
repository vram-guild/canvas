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

package grondag.canvas.terrain.util;

import java.util.Arrays;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

public abstract class RenderRegionAddressHelper {
	public static final int INTERIOR_STATE_COUNT = 4096;

	private static final int REGION_PADDING = 2;
	private static final int FACE_STATE_COUNT = 16 * 16 * REGION_PADDING;
	private static final int EDGE_STATE_COUNT = 16 * REGION_PADDING * REGION_PADDING;
	private static final int CORNER_STATE_COUNT = REGION_PADDING * REGION_PADDING * REGION_PADDING;
	private static final int SIDE_INDEX_X0 = INTERIOR_STATE_COUNT;
	private static final int SIDE_INDEX_X2 = SIDE_INDEX_X0 + FACE_STATE_COUNT;
	private static final int SIDE_INDEX_Y0 = SIDE_INDEX_X2 + FACE_STATE_COUNT;
	private static final int SIDE_INDEX_Y2 = SIDE_INDEX_Y0 + FACE_STATE_COUNT;
	private static final int SIDE_INDEX_Z0 = SIDE_INDEX_Y2 + FACE_STATE_COUNT;
	private static final int SIDE_INDEX_Z2 = SIDE_INDEX_Z0 + FACE_STATE_COUNT;

	private static final int EDGE_INDEX_Y0X0 = SIDE_INDEX_Z2 + FACE_STATE_COUNT;
	private static final int EDGE_INDEX_Y0X2 = EDGE_INDEX_Y0X0 + EDGE_STATE_COUNT;
	private static final int EDGE_INDEX_Y2X0 = EDGE_INDEX_Y0X2 + EDGE_STATE_COUNT;
	private static final int EDGE_INDEX_Y2X2 = EDGE_INDEX_Y2X0 + EDGE_STATE_COUNT;

	private static final int EDGE_INDEX_Z0X0 = EDGE_INDEX_Y2X2 + EDGE_STATE_COUNT;
	private static final int EDGE_INDEX_Z0X2 = EDGE_INDEX_Z0X0 + EDGE_STATE_COUNT;
	private static final int EDGE_INDEX_Z2X0 = EDGE_INDEX_Z0X2 + EDGE_STATE_COUNT;
	private static final int EDGE_INDEX_Z2X2 = EDGE_INDEX_Z2X0 + EDGE_STATE_COUNT;

	private static final int EDGE_INDEX_Z0Y0 = EDGE_INDEX_Z2X2 + EDGE_STATE_COUNT;
	private static final int EDGE_INDEX_Z0Y2 = EDGE_INDEX_Z0Y0 + EDGE_STATE_COUNT;
	private static final int EDGE_INDEX_Z2Y0 = EDGE_INDEX_Z0Y2 + EDGE_STATE_COUNT;
	private static final int EDGE_INDEX_Z2Y2 = EDGE_INDEX_Z2Y0 + EDGE_STATE_COUNT;

	private static final int CORNER_INDEX_000 = EDGE_INDEX_Z2Y2 + EDGE_STATE_COUNT;
	private static final int CORNER_INDEX_002 = CORNER_INDEX_000 + CORNER_STATE_COUNT;
	private static final int CORNER_INDEX_020 = CORNER_INDEX_002 + CORNER_STATE_COUNT;
	private static final int CORNER_INDEX_022 = CORNER_INDEX_020 + CORNER_STATE_COUNT;
	private static final int CORNER_INDEX_200 = CORNER_INDEX_022 + CORNER_STATE_COUNT;
	private static final int CORNER_INDEX_202 = CORNER_INDEX_200 + CORNER_STATE_COUNT;
	private static final int CORNER_INDEX_220 = CORNER_INDEX_202 + CORNER_STATE_COUNT;
	private static final int CORNER_INDEX_222 = CORNER_INDEX_220 + CORNER_STATE_COUNT;

	public static final int TOTAL_STATE_COUNT = CORNER_INDEX_222 + CORNER_STATE_COUNT;

	private static final int X0 = 0;
	private static final int X1 = 1;
	private static final int X2 = 2;
	private static final int Y0 = 0;
	private static final int Y1 = 4;
	private static final int Y2 = 8;
	private static final int Z0 = 0;
	private static final int Z1 = 16;
	private static final int Z2 = 32;

	public static final int EXTERIOR_STATE_COUNT = TOTAL_STATE_COUNT - INTERIOR_STATE_COUNT;

	/**
	 * number of long words per dimensional unit.  Default orientation sliced on z axis
	 */
	public static final int SLICE_WORD_COUNT = 4;
	public static final int INTERIOR_CACHE_WORDS = INTERIOR_STATE_COUNT / 64;
	public static final int EXTERIOR_CACHE_WORDS = (EXTERIOR_STATE_COUNT + 63) / 64;
	public static final int TOTAL_CACHE_WORDS = INTERIOR_CACHE_WORDS + EXTERIOR_CACHE_WORDS;
	public static final BlockState AIR = Blocks.AIR.getDefaultState();

	private static final int[] REVERSE_INDEX_LOOKUP = new int[TOTAL_STATE_COUNT];
	private static final int[] INDEX_LOOKUP = new int[32768];

	static {
		Arrays.fill(INDEX_LOOKUP, -1);

		for (int x = -2; x <= 17; x++) {
			for (int y = -2; y <= 17; y++) {
				for (int z = -2; z <= 17; z++) {
					final int fastIndex = (x + 2) | ((y + 2) << 5) | ((z + 2) << 10);
					final int cacheIndex = address(x, y, z);
					INDEX_LOOKUP[fastIndex] = cacheIndex;
					REVERSE_INDEX_LOOKUP[cacheIndex] = fastIndex;
				}
			}
		}
	}

	private RenderRegionAddressHelper() {
	}

	public static int cornerSubAddress(int x, int y, int z) {
		return x | (y << 1) | (z << 2);
	}

	/**
	 * Compute sub index for face state.
	 *
	 * @param i 0-15 - source depends on axis
	 * @param j 0-15 - source depends on axis
	 * @param depth 0 to padding size - 1 (currently 0-2)
	 * @return
	 */
	public static int faceSubAddress(int i, int j, int depth) {
		return i | (j << 4) | (depth << 8);
	}

	/**
	 * Compute sub index for edge state.
	 *
	 * @param i 0 to padding size - 1 (currently 0-2)
	 * @param j 0 to padding size - 1 (currently 0-2)
	 * @param depth 0-15 - source depends on axis
	 * @return
	 */
	public static int edgeSubAddress(int i, int j, int depth) {
		return i | (j << 1) | (depth << 2);
	}

	public static int address(int x, int y, int z) {
		// translate each component to a 0-2 value that indicates low, interior or high
		final int rx = (x + 16) >> 4;
		final int ry = (y + 16) >> 4;
		final int rz = (z + 16) >> 4;

		// switch based on word that indicates which quadrant we are in, or if we are in interior
		// bitwise address space not fully handled because meaningful inputs are 0-2, not 0-3

		switch (rx | (ry << 2) | (rz << 4)) {
			case Z1 | Y1 | X1:
				return interiorIndex(x, y, z);

			//faces
			case Z1 | Y1 | X0:
				return SIDE_INDEX_X0 + faceSubAddress(y, z, x + REGION_PADDING);
			case Z1 | Y1 | X2:
				return SIDE_INDEX_X2 + faceSubAddress(y, z, x - 16);
			case Z1 | Y0 | X1:
				return SIDE_INDEX_Y0 + faceSubAddress(x, z, y + REGION_PADDING);
			case Z1 | Y2 | X1:
				return SIDE_INDEX_Y2 + faceSubAddress(x, z, y - 16);
			case Z0 | Y1 | X1:
				return SIDE_INDEX_Z0 + faceSubAddress(x, y, z + REGION_PADDING);
			case Z2 | Y1 | X1:
				return SIDE_INDEX_Z2 + faceSubAddress(x, y, z - 16);

			// edges
			case Z1 | Y0 | X0:
				return EDGE_INDEX_Y0X0 + edgeSubAddress(x + REGION_PADDING, y + REGION_PADDING, z);
			case Z1 | Y0 | X2:
				return EDGE_INDEX_Y0X2 + edgeSubAddress(x - 16, y + REGION_PADDING, z);
			case Z1 | Y2 | X0:
				return EDGE_INDEX_Y2X0 + edgeSubAddress(x + REGION_PADDING, y - 16, z);
			case Z1 | Y2 | X2:
				return EDGE_INDEX_Y2X2 + edgeSubAddress(x - 16, y - 16, z);

			case Z0 | Y1 | X0:
				return EDGE_INDEX_Z0X0 + edgeSubAddress(x + REGION_PADDING, z + REGION_PADDING, y);
			case Z0 | Y1 | X2:
				return EDGE_INDEX_Z0X2 + edgeSubAddress(x - 16, z + REGION_PADDING, y);
			case Z2 | Y1 | X0:
				return EDGE_INDEX_Z2X0 + edgeSubAddress(x + REGION_PADDING, z - 16, y);
			case Z2 | Y1 | X2:
				return EDGE_INDEX_Z2X2 + edgeSubAddress(x - 16, z - 16, y);

			case Z0 | Y0 | X1:
				return EDGE_INDEX_Z0Y0 + edgeSubAddress(y + REGION_PADDING, z + REGION_PADDING, x);
			case Z0 | Y2 | X1:
				return EDGE_INDEX_Z0Y2 + edgeSubAddress(y - 16, z + REGION_PADDING, x);
			case Z2 | Y0 | X1:
				return EDGE_INDEX_Z2Y0 + edgeSubAddress(y + REGION_PADDING, z - 16, x);
			case Z2 | Y2 | X1:
				return EDGE_INDEX_Z2Y2 + edgeSubAddress(y - 16, z - 16, x);

			// corners
			case Z0 | Y0 | X0:
				return CORNER_INDEX_000 + cornerSubAddress(x + REGION_PADDING, y + REGION_PADDING, z + REGION_PADDING);
			case Z0 | Y0 | X2:
				return CORNER_INDEX_002 + cornerSubAddress(x - 16, y + REGION_PADDING, z + REGION_PADDING);
			case Z0 | Y2 | X0:
				return CORNER_INDEX_020 + cornerSubAddress(x + REGION_PADDING, y - 16, z + REGION_PADDING);
			case Z0 | Y2 | X2:
				return CORNER_INDEX_022 + cornerSubAddress(x - 16, y - 16, z + REGION_PADDING);
			case Z2 | Y0 | X0:
				return CORNER_INDEX_200 + cornerSubAddress(x + REGION_PADDING, y + REGION_PADDING, z - 16);
			case Z2 | Y0 | X2:
				return CORNER_INDEX_202 + cornerSubAddress(x - 16, y + REGION_PADDING, z - 16);
			case Z2 | Y2 | X0:
				return CORNER_INDEX_220 + cornerSubAddress(x + REGION_PADDING, y - 16, z - 16);
			case Z2 | Y2 | X2:
				return CORNER_INDEX_222 + cornerSubAddress(x - 16, y - 16, z - 16);
			default:
				assert false;
				return 0;
		}
	}

	/**
	 * Handles values < 0 or > 15 by masking to LSB.
	 */
	public static int clampedInteriorIndex(int x, int y, int z) {
		return interiorIndex(x & 0xF, y & 0xF, z & 0xF);
	}

	/**
	 * Assumes values 0-15.
	 */
	public static int interiorIndex(int x, int y, int z) {
		return x | (y << 4) | (z << 8);
	}

	public static int interiorIndex(BlockPos pos) {
		return clampedInteriorIndex(pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	 * Accepts a main section index (0-4096) and returns
	 * a full-region index offset by the given face.
	 */
	public static int offsetMainChunkBlockIndex(int index, Direction face) {
		final Vec3i vec = face.getVector();

		final int x = (index & 0xF) + vec.getX();
		final int y = ((index >> 4) & 0xF) + vec.getY();
		final int z = ((index >> 8) & 0xF) + vec.getZ();

		return fastRelativeCacheIndex(x, y, z);
	}

	/**
	 * Checks for values outside -2 to 17, returns -1 if outside.
	 */
	public static int relativeCacheIndex(int x, int y, int z) {
		final int ix = (x + 2);

		if ((ix & 31) != ix) return -1;

		final int iy = (y + 2);

		if ((iy & 31) != iy) return -1;

		final int iz = (z + 2);

		if ((iz & 31) != iz) return -1;

		return INDEX_LOOKUP[ix | (iy << 5) | (iz << 10)];
	}

	/**
	 * Values must be -2 to 17.
	 */
	public static int fastRelativeCacheIndex(int x, int y, int z) {
		final int lookupIndex = (x + 2) | ((y + 2) << 5) | ((z + 2) << 10);
		return INDEX_LOOKUP[lookupIndex];
	}

	/**
	 * Inputs must ensure result of addition is in  -2 to 17 range.
	 *
	 * @param packedXyz5       must be in  -2 to 17 range (packed values 0-19)
	 * @param signedXyzOffset5 must be in -1 to 1 (packed values 0-2)
	 * @return equivalent to {@link #fastRelativeCacheIndex(int, int, int)} with added values
	 */
	public static int fastOffsetRelativeCacheIndex(int packedXyz5, int signedXyzOffset5) {
		return INDEX_LOOKUP[packedXyz5 + signedXyzOffset5 - 0b000010000100001];
	}

	/**
	 * Return packed x, y, z relative coordinates for the given cache position.
	 * Values are +2 actual. (0-19 instead of -2 to 17).
	 */
	public static int cacheIndexToXyz5(int cacheIndex) {
		return REVERSE_INDEX_LOOKUP[cacheIndex];
	}

	/**
	 * Packs values in -1 to 1 range with 5 bit encoding
	 * Reduces call overhead by passing xyz5 and packed
	 * offset vs adding component-wise and passing each component.
	 *
	 * @param x -1 to 1
	 * @param y -1 to 1
	 * @param z -1 to 1
	 * @return
	 */
	public static int signedXyzOffset5(int x, int y, int z) {
		return (x + 1) | ((y + 1) << 5) | ((z + 1) << 10);
	}

	public static int signedXyzOffset5(Vec3i vec) {
		return signedXyzOffset5(vec.getX(), vec.getY(), vec.getZ());
	}
}
