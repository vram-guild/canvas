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

package grondag.canvas.terrain.region;

import net.minecraft.util.math.BlockPos;

public final class RenderRegionIndexer {
	RenderRegionIndexer() { }

	/** Max per-axis chunk distance + 2 padding to allow for neighbor regions at edge. */
	public static final int MAX_LOADED_CHUNK_RADIUS = 34;

	/** Max per-axis width of loaded chunks, inclusive. */
	public static final int MAX_LOADED_CHUNK_DIAMETER = 1 + MAX_LOADED_CHUNK_RADIUS * 2;

	/** Smallest power of two that can hold MAX_CHUNK_DIAMETER. Relevant for efficient (bit-wise) addressing. */
	public static final int PADDED_CHUNK_DIAMETER = 128;

	/** Number of bits needed to represent PADDED_CHUNK_DIAMETER. */
	public static final int CHUNK_DIAMETER_BITS = 7;

	/** Size of the address space for all horizontal chunk positions within the padded chunk diameter. */
	public static final int PADDED_CHUNK_INDEX_COUNT = PADDED_CHUNK_DIAMETER * PADDED_CHUNK_DIAMETER;

	/** Value that must be added to region orgin Y component to ensure it is non-negative. */
	public static final int Y_BLOCKPOS_OFFSET = 64;

	/** Max number of Y regions in a chunk.  May be fewer of them present - this is meant for addressing. */
	public static final int MAX_Y_REGIONS = 24;

	/** Number of bits needed to represent MAX_Y_REGIONS as a positive number. (Positive because offset by Y_BLOCKPOS_OFFSET.) */
	public static final int Y_REGION_BITS = 5;

	/** Largest possible number of loaded regions within the un-padded chunk diameter. */
	public static final int LOADED_REGION_INDEX_COUNT = MAX_LOADED_CHUNK_DIAMETER * MAX_LOADED_CHUNK_DIAMETER * MAX_Y_REGIONS;

	/** Size of the padded address space for regions within the padded chunk diameter. Used for sparse open addressing. */
	public static final int PADDED_REGION_INDEX_COUNT = PADDED_CHUNK_INDEX_COUNT * MAX_Y_REGIONS;

	/** Size of the address space in an X- or Z-axis cross-section of the loaded region volume. */
	public static final int SIDE_SLICE_LOADED_REGION_INDEX_COUNT = MAX_Y_REGIONS * MAX_LOADED_CHUNK_DIAMETER;

	/**
	 * Returns an index within an array of CHUNK_INDEX_COUNT size that
	 * uniquely maps to the x and z coordinates of a region/chunk origin.
	 *
	 * <p>The maps makes no guarantee regarding spatial relationship to other regions -
	 * the mapping "wraps" across the array, but because the array address space is larger
	 * than the max loaded chunk diameter and assuming chunks outside that radius
	 * are unloaded, only one loaded chunk will be present at a given index at any time.
	 *
	 * @param x x-axis coordinate of chunk/region block position (not a chunk pos)
	 * @param z z-axis coordinate of chunk/region block position (not a chunk pos)
	 * @return index to chunk array that uniquely maps to a chunk within max loading radius
	 */
	public static int chunkIndex(int x, int z) {
		x = ((x + 30000000) >> 4) & 127;
		z = ((z + 30000000) >> 4) & 127;

		return x | (z << 7);
	}

	public static long blockPosToRegionOrigin(BlockPos pos) {
		return blockPosToRegionOrigin(pos.getX(), pos.getY(), pos.getZ());
	}

	public static long blockPosToRegionOrigin(int x, int y, int z) {
		return BlockPos.asLong(x & 0xFFFFFFF0, y & 0xFFFFFFF0, z & 0xFFFFFFF0);
	}
}
