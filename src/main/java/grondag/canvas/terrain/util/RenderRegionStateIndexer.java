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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

/*
 * Static methods and definitions to efficiently
 * address positions within a render region.
 * A render region hold a 16x16x16 chunk section
 * plus positions ("padding") from adjacent sections.
 */
public abstract class RenderRegionStateIndexer {
	/**
	 * Number of positions within a 16x16x16 render region, not including exterior padding.
	 */
	public static final int INTERIOR_STATE_COUNT = 4096;

	/** Render region padding depth, inclusive.  Same as vanilla. */
	public static final int REGION_PADDING = 2;

	// Constants below define where each section of padding maps in the total index space.
	// Exterior index values begin after index values for interior positions, so that
	// addressing of interior can be done with simple bitwise math.
	public static final int FACE_STATE_COUNT = 16 * 16 * REGION_PADDING;
	public static final int EDGE_STATE_COUNT = 16 * REGION_PADDING * REGION_PADDING;
	public static final int CORNER_STATE_COUNT = REGION_PADDING * REGION_PADDING * REGION_PADDING;
	public static final int SIDE_INDEX_X0 = INTERIOR_STATE_COUNT;
	public static final int SIDE_INDEX_X2 = SIDE_INDEX_X0 + FACE_STATE_COUNT;
	public static final int SIDE_INDEX_Y0 = SIDE_INDEX_X2 + FACE_STATE_COUNT;
	public static final int SIDE_INDEX_Y2 = SIDE_INDEX_Y0 + FACE_STATE_COUNT;
	public static final int SIDE_INDEX_Z0 = SIDE_INDEX_Y2 + FACE_STATE_COUNT;
	public static final int SIDE_INDEX_Z2 = SIDE_INDEX_Z0 + FACE_STATE_COUNT;

	public static final int EDGE_INDEX_Y0X0 = SIDE_INDEX_Z2 + FACE_STATE_COUNT;
	public static final int EDGE_INDEX_Y0X2 = EDGE_INDEX_Y0X0 + EDGE_STATE_COUNT;
	public static final int EDGE_INDEX_Y2X0 = EDGE_INDEX_Y0X2 + EDGE_STATE_COUNT;
	public static final int EDGE_INDEX_Y2X2 = EDGE_INDEX_Y2X0 + EDGE_STATE_COUNT;

	public static final int EDGE_INDEX_Z0X0 = EDGE_INDEX_Y2X2 + EDGE_STATE_COUNT;
	public static final int EDGE_INDEX_Z0X2 = EDGE_INDEX_Z0X0 + EDGE_STATE_COUNT;
	public static final int EDGE_INDEX_Z2X0 = EDGE_INDEX_Z0X2 + EDGE_STATE_COUNT;
	public static final int EDGE_INDEX_Z2X2 = EDGE_INDEX_Z2X0 + EDGE_STATE_COUNT;

	public static final int EDGE_INDEX_Z0Y0 = EDGE_INDEX_Z2X2 + EDGE_STATE_COUNT;
	public static final int EDGE_INDEX_Z0Y2 = EDGE_INDEX_Z0Y0 + EDGE_STATE_COUNT;
	public static final int EDGE_INDEX_Z2Y0 = EDGE_INDEX_Z0Y2 + EDGE_STATE_COUNT;
	public static final int EDGE_INDEX_Z2Y2 = EDGE_INDEX_Z2Y0 + EDGE_STATE_COUNT;

	public static final int CORNER_INDEX_000 = EDGE_INDEX_Z2Y2 + EDGE_STATE_COUNT;
	public static final int CORNER_INDEX_002 = CORNER_INDEX_000 + CORNER_STATE_COUNT;
	public static final int CORNER_INDEX_020 = CORNER_INDEX_002 + CORNER_STATE_COUNT;
	public static final int CORNER_INDEX_022 = CORNER_INDEX_020 + CORNER_STATE_COUNT;
	public static final int CORNER_INDEX_200 = CORNER_INDEX_022 + CORNER_STATE_COUNT;
	public static final int CORNER_INDEX_202 = CORNER_INDEX_200 + CORNER_STATE_COUNT;
	public static final int CORNER_INDEX_220 = CORNER_INDEX_202 + CORNER_STATE_COUNT;
	public static final int CORNER_INDEX_222 = CORNER_INDEX_220 + CORNER_STATE_COUNT;

	/**
	 * Number of positions captured for a 16x16x16 render region, including exterior padding.
	 */
	public static final int TOTAL_STATE_COUNT = CORNER_INDEX_222 + CORNER_STATE_COUNT;

	// These constants address the low/interior/high sub-regions (chunk sections) that are
	// ultimately the source of data for a padded region.  Each sub-region represents a sub-addressing scheme.
	private static final int X0 = 0;
	private static final int X1 = 1;
	private static final int X2 = 2;
	private static final int Y0 = 0;
	private static final int Y1 = 4;
	private static final int Y2 = 8;
	private static final int Z0 = 0;
	private static final int Z1 = 16;
	private static final int Z2 = 32;

	/**
	 * Number of positions in the padded exterior area of a 16x16x16 render region - excludes the interior positions.
	 */
	public static final int EXTERIOR_STATE_COUNT = TOTAL_STATE_COUNT - INTERIOR_STATE_COUNT;

	/**
	 * Number of long words per dimensional unit for bit fields that represent interior sections.
	 * Interior sections are 16x16, which can be represented with 4 long words.
	 * Default orientation slices on z axis.
	 */
	public static final int SLICE_WORD_COUNT = 4;

	/**
	 * Number of long words needed to represent a full region interior as a bit field.
	 */
	public static final int INTERIOR_CACHE_WORDS = INTERIOR_STATE_COUNT / 64;

	/**
	 * Number of long words needed to represent the padded areas of a region.
	 * This does not map cleanly to a word boundary and it assumes indexing of
	 * bits in padded areas is done using values from {@link #regionIndex(int, int, int)}.
	 */
	public static final int EXTERIOR_CACHE_WORDS = (EXTERIOR_STATE_COUNT + 63) / 64;

	/**
	 * Number of long words needed to represent a bit field for region with both interior and padded areas.
	 */
	public static final int TOTAL_CACHE_WORDS = INTERIOR_CACHE_WORDS + EXTERIOR_CACHE_WORDS;

	/**
	 * Pre-computed, compact region index values that can be looked up using a larger but computationally less
	 * expensive bit-wise derivation.  (The larger space is simply 32^3, or five bits per dimension.)
	 */
	private static final int[] INDEX_LOOKUP = new int[32768];

	/**
	 * Our compact region index values don't offer an inexpensive way to extract the original x, y, z components
	 * so we use a lookup from compact index to the five-bits-per axis index that offers easy bit-wise extraction
	 * of each component.
	 */
	private static final int[] REVERSE_INDEX_LOOKUP = new int[TOTAL_STATE_COUNT];

	static {
		Arrays.fill(INDEX_LOOKUP, -1);

		for (int x = -2; x <= 17; x++) {
			for (int y = -2; y <= 17; y++) {
				for (int z = -2; z <= 17; z++) {
					final int fastIndex = (x + 2) | ((y + 2) << 5) | ((z + 2) << 10);
					final int cacheIndex = computeRegionIndex(x, y, z);
					INDEX_LOOKUP[fastIndex] = cacheIndex;
					REVERSE_INDEX_LOOKUP[cacheIndex] = fastIndex;
				}
			}
		}
	}

	private RenderRegionStateIndexer() {
	}

	// These are exposed so addressing in intensive region lookups can be directly computed
	public static final int CORNER_I_MASK = 1;
	public static final int CORNER_J_SHIFT = 1;
	public static final int CORNER_J_MASK = 1;
	public static final int CORNER_K_SHIFT = 2;

	private static int cornerSubAddress(int i, int j, int k) {
		return i | (j << CORNER_J_SHIFT) | (k << CORNER_K_SHIFT);
	}

	// These are exposed so addressing in intensive region lookups can be directly computed
	public static final int FACE_I_MASK = 0xF;
	public static final int FACE_J_SHIFT = 4;
	public static final int FACE_J_MASK = 0xF;
	public static final int FACE_K_SHIFT = 8;

	/**
	 * Compute sub index for face state.
	 *
	 * @param i 0-15 - source depends on axis
	 * @param j 0-15 - source depends on axis
	 * @param k 0 to padding size - 1 (currently 0-2)
	 * @return
	 */
	private static int faceSubAddress(int i, int j, int k) {
		return i | (j << FACE_J_SHIFT) | (k << FACE_K_SHIFT);
	}

	// These are exposed so addressing in intensive region lookups can be directly computed
	public static final int EDGE_I_MASK = 1;
	public static final int EDGE_J_SHIFT = 1;
	public static final int EDGE_J_MASK = 1;
	public static final int EDGE_K_SHIFT = 2;

	/**
	 * Compute sub index for edge state.
	 *
	 * @param i 0 to padding size - 1 (currently 0-1)
	 * @param j 0 to padding size - 1 (currently 0-1)
	 * @param k 0-15 - source depends on axis
	 * @return
	 */
	private static int edgeSubAddress(int i, int j, int k) {
		return i | (j << EDGE_J_SHIFT) | (k << EDGE_K_SHIFT);
	}

	/**
	 * Computes indexes that match the contract of {@link #regionIndex(int, int, int)}.
	 */
	static int computeRegionIndex(int x, int y, int z) {
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
				assert false : "Encountered input values out of range in render region addressing.";
				return -1;
		}
	}

	/**
	 * Handles values < 0 or > 15 by masking to LSB.
	 */
	public static int clampedInteriorIndex(int x, int y, int z) {
		return interiorIndex(x & 0xF, y & 0xF, z & 0xF);
	}

	/**
	 * Same as {@link #clampedInteriorIndex(int, int, int)} but accepts a position instance.
	 */
	public static int interiorIndex(BlockPos pos) {
		return clampedInteriorIndex(pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	 * Assumes input values 0-15.
	 */
	public static int interiorIndex(int x, int y, int z) {
		return x | (y << 4) | (z << 8);
	}

	/**
	 * Accepts an interior index (0-4095) and returns
	 * a full-region index offset in the direction of the given face.
	 */
	public static int offsetInteriorIndex(int interiorIndex, Direction face) {
		final Vec3i vec = face.getNormal();

		final int x = (interiorIndex & 0xF) + vec.getX();
		final int y = ((interiorIndex >> 4) & 0xF) + vec.getY();
		final int z = ((interiorIndex >> 8) & 0xF) + vec.getZ();

		return regionIndex(x, y, z);
	}

	/**
	 * Add to make coordinates zero-based instead of starting at -2.
	 */
	private static final int ADDEND = 2 | (2 << 5) | (2 << 10);

	/**
	 * Returns an index of block position relative to the region origin.
	 * Handles values that are within the padding distance, currently
	 * -2 to 17, vs the 0-15 within a normal region.
	 *
	 * <p>The index space is arranged so that interior positions
	 * map to index values 0-4095 using simple bitwise math that
	 * is codified in {@link #interiorIndex(int, int, int)}.
	 *
	 * @param x -2 to 17
	 * @param y -2 to 17
	 * @param z -2 to 17
	 * @return 0 to 7999, >= 4096 if position is outside the interior, -1 if inputs are invalid.
	 */
	public static int regionIndex(int x, int y, int z) {
		final int index = x + (y << 5) + (z << 10) + ADDEND;
		return (index & 32767) == index ? INDEX_LOOKUP[index] : -1;
	}

	/**
	 * Takes a 5-bit per axis region index and a five-bit-per axis offset,
	 * adds them and returns the compact index value consistent with
	 * {@link #regionIndex(int, int, int)}.
	 *
	 * <p>Caller must ensure result of addition is in  -2 to 17 range.
	 *
	 * @param packedXyz5       must be in  -2 to 17 range (packed values 0-19)
	 * @param signedXyzOffset5 must be in -1 to 1 (packed values 0-2)
	 * @return equivalent to {@link #uncheckedRegionIndex(int, int, int)} with added values
	 */
	public static int fastOffsetRegionIndex(int packedXyz5, int signedXyzOffset5) {
		return INDEX_LOOKUP[packedXyz5 + signedXyzOffset5 - 0b000010000100001];
	}

	/**
	 * Returns a five-bit-per axis translation of the given compact region index.
	 * Values are +2 actual. (0-19 instead of -2 to 17).
	 * Useful for extracting x, y, z values of a region index.
	 */
	public static int regionIndexToXyz5(int cacheIndex) {
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
