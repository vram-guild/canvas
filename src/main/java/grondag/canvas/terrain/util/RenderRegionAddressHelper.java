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

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

public abstract class RenderRegionAddressHelper {
	/** How many block/fluid states we capture along edges of region to support neighbor queries on edges. Vanilla is 2. */
	private static final int RENDER_REGION_PADDING = 2;
	private static final int RENDER_REGION_PADDED_MAX = 15 + RENDER_REGION_PADDING * 2;
	/** Number of block/fluid states in each axis of a render region including padding on each face. */
	private static final int RENDER_REGION_DIAMETER = 16 + RENDER_REGION_PADDING * 2;
	private static final int RENDER_REGION_DIAMETER_SQ = RENDER_REGION_DIAMETER * RENDER_REGION_DIAMETER;

	/** How many block/fluid states catured for a render region, including padding. */
	public static final int RENDER_REGION_STATE_COUNT = RENDER_REGION_DIAMETER * RENDER_REGION_DIAMETER * RENDER_REGION_DIAMETER;

	/** Index of state from coordinates relative to region origin. Returns -1 if outside padded range. */
	public static final int regionStateCacheIndex(int x, int y, int z) {
		final int xPad = x + RENDER_REGION_PADDING;
		final int yPad = y + RENDER_REGION_PADDING;
		final int zPad = z + RENDER_REGION_PADDING;

		final int test = xPad | (RENDER_REGION_PADDED_MAX - xPad)
				| yPad | (RENDER_REGION_PADDED_MAX - yPad)
				| zPad | (RENDER_REGION_PADDED_MAX - zPad);

		return test < 0 ? -1 : (xPad + yPad * RENDER_REGION_DIAMETER + zPad * RENDER_REGION_DIAMETER_SQ);
	}

	/**
	 * Accepts an index from {@link #regionStateCacheIndex(int, int, int)} and derives
	 * the region-relative x, y, z values - putting the into the provided mutable position.
	 */
	public static void indexToBlockPos(int index, BlockPos.Mutable pos) {
		final int z = index / RENDER_REGION_DIAMETER_SQ;
		index -= z * RENDER_REGION_DIAMETER_SQ;
		final int y = index / RENDER_REGION_DIAMETER;
		final int x = index - y * RENDER_REGION_DIAMETER;
		pos.set(x - RENDER_REGION_PADDING, y - RENDER_REGION_PADDING, z - RENDER_REGION_PADDING);
	}

	/**
	 * Accepts an index from {@link #regionStateCacheIndex(int, int, int)} and derives
	 * the index offset by the given face.  Assumes and does not check the input
	 * position is not on the exterior of the region size addressable by this indexing scheme.
	 */
	public static int offsetIndex(int index, Direction face) {
		switch (face) {
			case DOWN:
				return index - RENDER_REGION_DIAMETER;
			case UP:
				return index + RENDER_REGION_DIAMETER;
			case WEST:
				return index - 1;
			case EAST:
				return index + 1;
			case NORTH:
				return index - RENDER_REGION_DIAMETER_SQ;
			case SOUTH:
				return index + RENDER_REGION_DIAMETER_SQ;
			default:
				return index;
		}
	}

	/**
	 * Accepts an index from {@link #regionStateCacheIndex(int, int, int)} and derives
	 * the index offset by the given vector.  Assumes and does not check the input
	 * position is not on the exterior of the region size addressable by this indexing scheme.
	 */
	public static int offsetIndex(int index, Vec3i offset) {
		return index + offset.getX() + offset.getY() * RENDER_REGION_DIAMETER + offset.getZ() * RENDER_REGION_DIAMETER_SQ;
	}

	// WIP: remove when occlusion region switches to simple addressing
	public static int newIndexToOldIndex(int index) {
		final int z = index / RENDER_REGION_DIAMETER_SQ;
		index -= z * RENDER_REGION_DIAMETER_SQ;
		final int y = index / RENDER_REGION_DIAMETER;
		final int x = index - y * RENDER_REGION_DIAMETER;
		return computeRelativeBlockIndex(x - RENDER_REGION_PADDING, y - RENDER_REGION_PADDING, z - RENDER_REGION_PADDING);
	}

	/////

	public static final int RENDER_REGION_INTERIOR_COUNT = 4096;
	private static final int FACE_CACHE_START = RENDER_REGION_INTERIOR_COUNT;
	private static final int FACE_CACHE_SIZE = 256 * 6;
	private static final int EDGE_CACHE_START = FACE_CACHE_START + FACE_CACHE_SIZE;
	private static final int EDGE_CACHE_SIZE = 16 * 12;
	private static final int CORNER_CACHE_START = EDGE_CACHE_START + EDGE_CACHE_SIZE;
	private static final int CORNER_CACHE_SIZE = 8;
	public static final int RENDER_REGION_TOTAL_COUNT = RENDER_REGION_INTERIOR_COUNT + FACE_CACHE_SIZE + EDGE_CACHE_SIZE + CORNER_CACHE_SIZE;
	public static final int EXTERIOR_CACHE_SIZE = RENDER_REGION_TOTAL_COUNT - RENDER_REGION_INTERIOR_COUNT;
	/**
	 * number of long words per dimensional unit.  Default orientation sliced on z axis
	 */
	public static final int SLICE_WORD_COUNT = 4;
	public static final int INTERIOR_CACHE_WORDS = RENDER_REGION_INTERIOR_COUNT / 64;
	private static final int EXTERIOR_CACHE_WORDS = (EXTERIOR_CACHE_SIZE + 63) / 64;

	public static final int TOTAL_CACHE_WORDS = INTERIOR_CACHE_WORDS + EXTERIOR_CACHE_WORDS;
	public static final BlockState AIR = Blocks.AIR.getDefaultState();

	//	private static final int[] REVERSE_INDEX_LOOKUP = new int[RENDER_REGION_TOTAL_COUNT];
	//	private static final int[] INDEX_LOOKUP = new int[32768];
	//
	//	static {
	//		Arrays.fill(INDEX_LOOKUP, -1);
	//
	//		for (int x = -1; x <= 16; x++) {
	//			for (int y = -1; y <= 16; y++) {
	//				for (int z = -1; z <= 16; z++) {
	//					final int fastIndex = (x + 1) | ((y + 1) << 5) | ((z + 1) << 10);
	//					final int cacheIndex = computeRelativeBlockIndex(x, y, z);
	//					INDEX_LOOKUP[fastIndex] = cacheIndex;
	//					REVERSE_INDEX_LOOKUP[cacheIndex] = fastIndex;
	//				}
	//			}
	//		}
	//	}

	/**
	 * Handles values < 0 or > 15 by masking to LSB.
	 */
	private static int clampedInteriorIndex(int x, int y, int z) {
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

	public static int localXfaceIndex(boolean high, int y, int z) {
		return FACE_CACHE_START + (high ? 256 : 0) | y | (z << 4);
	}

	public static int localYfaceIndex(int x, boolean high, int z) {
		return FACE_CACHE_START + (high ? 768 : 512) | x | (z << 4);
	}

	public static int localZfaceIndex(int x, int y, boolean high) {
		return FACE_CACHE_START + (high ? 1280 : 1024) | x | (y << 4);
	}

	public static int localXEdgeIndex(int x, boolean highY, boolean highZ) {
		final int subindex = highY ? highZ ? 48 : 32 : highZ ? 16 : 0;
		return EDGE_CACHE_START + subindex + x;
	}

	public static int localYEdgeIndex(boolean highX, int y, boolean highZ) {
		final int subindex = highX ? highZ ? 48 : 32 : highZ ? 16 : 0;
		return EDGE_CACHE_START + 64 + subindex + y;
	}

	public static int localZEdgeIndex(boolean highX, boolean highY, int z) {
		final int subindex = highX ? highY ? 48 : 32 : highY ? 16 : 0;
		return EDGE_CACHE_START + 128 + subindex + z;
	}

	public static int localCornerIndex(boolean highX, boolean highY, boolean highZ) {
		int subindex = highX ? 0 : 1;

		if (highY) {
			subindex |= 2;
		}

		if (highZ) {
			subindex |= 4;
		}

		return CORNER_CACHE_START + subindex;
	}

	//	/**
	//	 * Accepts a main section index (0-4096) and returns
	//	 * a full-region index offset by the given face.
	//	 */
	//	public static int offsetMainChunkBlockIndex(int index, Direction face) {
	//		final Vec3i vec = face.getVector();
	//
	//		final int x = (index & 0xF) + vec.getX();
	//		final int y = ((index >> 4) & 0xF) + vec.getY();
	//		final int z = ((index >> 8) & 0xF) + vec.getZ();
	//
	//		return fastRelativeCacheIndex(x, y, z);
	//	}
	//
	//		/**
	//		 * Checks for values outside -1 to 16, returns -1 if outside.
	//		 */
	//		public static int relativeCacheIndex(int x, int y, int z) {
	//			final int ix = (x + 1);
	//
	//			if ((ix & 31) != ix) return -1;
	//
	//			final int iy = (y + 1);
	//
	//			if ((iy & 31) != iy) return -1;
	//
	//			final int iz = (z + 1);
	//
	//			if ((iz & 31) != iz) return -1;
	//
	//			return INDEX_LOOKUP[ix | (iy << 5) | (iz << 10)];
	//		}
	//
	//	/**
	//	 * Values must be -1 to 16.
	//	 */
	//	public static int fastRelativeCacheIndex(int x, int y, int z) {
	//		final int lookupIndex = (x + 1) | ((y + 1) << 5) | ((z + 1) << 10);
	//		return INDEX_LOOKUP[lookupIndex];
	//	}
	//
	//	/**
	//	 * Inputs must ensure result of addition is in  -1 to 16 range.
	//	 *
	//	 * @param packedXyz5       must be in  -1 to 16 range (packed values 0-17)
	//	 * @param signedXyzOffset5 must be in -1 to 1 (packed values 0-2)
	//	 * @return equivalent to {@link #fastRelativeCacheIndex(int, int, int)} with added values
	//	 */
	//	public static int fastOffsetRelativeCacheIndex(int packedXyz5, int signedXyzOffset5) {
	//		return INDEX_LOOKUP[packedXyz5 + signedXyzOffset5 - 0b000010000100001];
	//	}
	//

	private static int computeRelativeBlockIndex(int x, int y, int z) {
		final int scenario = ((x & 0xF) == x ? 1 : 0) | ((y & 0xF) == y ? 2 : 0) | ((z & 0xF) == z ? 4 : 0);

		switch (scenario) {
			case 0b000:
				// not contained in any - may be a corner
				if (x == -1) {
					if (y == -1) {
						if (z == -1) {
							return localCornerIndex(false, false, false);
						} else if (z == 16) {
							return localCornerIndex(false, false, true);
						}
					} else if (y == 16) {
						if (z == -1) {
							return localCornerIndex(false, true, false);
						} else if (z == 16) {
							return localCornerIndex(false, true, true);
						}
					}
				} else if (x == 16) {
					if (y == -1) {
						if (z == -1) {
							return localCornerIndex(true, false, false);
						} else if (z == 16) {
							return localCornerIndex(true, false, true);
						}
					} else if (y == 16) {
						if (z == -1) {
							return localCornerIndex(true, true, false);
						} else if (z == 16) {
							return localCornerIndex(true, true, true);
						}
					}
				}

				break;
			case 0b001:
				// contained in X - may be an edge
				if (y == -1) {
					if (z == -1) {
						return localXEdgeIndex(x & 0xF, false, false);
					} else if (z == 16) {
						return localXEdgeIndex(x & 0xF, false, true);
					}
				} else if (y == 16) {
					if (z == -1) {
						return localXEdgeIndex(x & 0xF, true, false);
					} else if (z == 16) {
						return localXEdgeIndex(x & 0xF, true, true);
					}
				}

				break;

			case 0b010:
				// contained in Y - may be an edge
				if (x == -1) {
					if (z == -1) {
						return localYEdgeIndex(false, y & 0xF, false);
					} else if (z == 16) {
						return localYEdgeIndex(false, y & 0xF, true);
					}
				} else if (x == 16) {
					if (z == -1) {
						return localYEdgeIndex(true, y & 0xF, false);
					} else if (z == +16) {
						return localYEdgeIndex(true, y & 0xF, true);
					}
				}

				break;

			case 0b100:
				// contained in Z - may be an edge
				if (x == -1) {
					if (y == -1) {
						return localZEdgeIndex(false, false, z & 0xF);
					} else if (y == 16) {
						return localZEdgeIndex(false, true, z & 0xF);
					}
				} else if (x == 16) {
					if (y == -1) {
						return localZEdgeIndex(true, false, z & 0xF);
					} else if (y == 16) {
						return localZEdgeIndex(true, true, z & 0xF);
					}
				}

				break;

			case 0b011:
				// contained in XY - may be a Z face
				if (z == -1) {
					return localZfaceIndex(x & 0xF, y & 0xF, false);
				} else if (z == 16) {
					return localZfaceIndex(x & 0xF, y & 0xF, true);
				}

				break;

			case 0b110:
				// contained in YZ - may be an X face
				if (x == -1) {
					return localXfaceIndex(false, y & 0xF, z & 0xF);
				} else if (x == 16) {
					return localXfaceIndex(true, y & 0xF, z & 0xF);
				}

				break;

			case 0b101:
				// contained in XZ - may be a Y face
				if (y == -1) {
					return localYfaceIndex(x & 0xF, false, z & 0xF);
				} else if (y == 16) {
					return localYfaceIndex(x & 0xF, true, z & 0xF);
				}

				break;

			case 0b111:
				// contained in XYZ - is main section
				return clampedInteriorIndex(x, y, z);
		}

		// use world directly
		return -1;
	}

	//	/**
	//	 * Return packed x, y, z relative coordinates for the given cache position.
	//	 * Values are +1 actual. (0-17 instead of -1 to 16).
	//	 */
	//	public static int cacheIndexToXyz5(int cacheIndex) {
	//		return REVERSE_INDEX_LOOKUP[cacheIndex];
	//	}

	//	/**
	//	 * Packs values in -1 to 1 range with 5 bit encoding
	//	 * Reduces call overhead by passing xyz5 and packed
	//	 * offset vs adding component-wise and passing each component.
	//	 *
	//	 * @param x -1 to 1
	//	 * @param y -1 to 1
	//	 * @param z -1 to 1
	//	 * @return
	//	 */
	//	public static int signedXyzOffset5(int x, int y, int z) {
	//		return (x + 1) | ((y + 1) << 5) | ((z + 1) << 10);
	//	}
	//
	//	public static int signedXyzOffset5(Vec3i vec) {
	//		return signedXyzOffset5(vec.getX(), vec.getY(), vec.getZ());
	//	}
}
