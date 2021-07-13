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

package grondag.canvas.render.region.vs;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.terrain.region.RegionPosition;
import grondag.canvas.vf.lookup.FixedCapacityIndexAllocator;

public class RegionSectorMap {
	private static final int SECTOR_AXIS_LENGTH_BLOCKS = 128;
	private static final int SECTOR_BLOCK_MASK = SECTOR_AXIS_LENGTH_BLOCKS - 1;
	private static final int SECTOR_COORDINATE_MASK = ~SECTOR_BLOCK_MASK;
	private static final int BLOCK_TO_SECTOR_SHIFT_BITS = 7;

	private static final int SECTOR_AXIS_LENGTH_REGIONS = SECTOR_AXIS_LENGTH_BLOCKS / 16;
	private static final int SECTOR_XZ_RADIUS = (32 + 2 + SECTOR_AXIS_LENGTH_REGIONS - 1) / SECTOR_AXIS_LENGTH_REGIONS;
	private static final int SECTOR_XZ_DIAMETER = 1 + SECTOR_XZ_RADIUS * 2;
	private static final int MIN_WORLD_HEIGHT_BLOCKS_INCLUSIVE = -64;
	private static final int MAX_WORLD_HEIGHT_BLOCKS_EXCLUSIVE = 320;
	private static final int SECTOR_Y_DIAMETER_BLOCKS = MAX_WORLD_HEIGHT_BLOCKS_EXCLUSIVE - MIN_WORLD_HEIGHT_BLOCKS_INCLUSIVE;
	private static final int SECTOR_Y_DIAMETER = (SECTOR_Y_DIAMETER_BLOCKS + SECTOR_AXIS_LENGTH_BLOCKS - 1) / SECTOR_AXIS_LENGTH_BLOCKS;
	private static final int MAX_SECTORS_LOADED = (SECTOR_XZ_DIAMETER * SECTOR_XZ_DIAMETER * SECTOR_Y_DIAMETER + 1) / 2 * 2;
	// Two extra ints at end for
	public static final int UNIFORM_ARRAY_LENGTH = MAX_SECTORS_LOADED / 2 + 2;
	public static final int UNIFORM_X_ORIGIN_INDEX = UNIFORM_ARRAY_LENGTH - 2;
	public static final int UNIFORM_Z_ORIGIN_INDEX = UNIFORM_ARRAY_LENGTH - 1;

	private static long sectorKey(int blockX, int blockY, int blockZ) {
		// shift Y up so we only have 3 sectors vertically
		blockY -= MIN_WORLD_HEIGHT_BLOCKS_INCLUSIVE;
		return BlockPos.asLong(blockX & SECTOR_COORDINATE_MASK, blockY & SECTOR_COORDINATE_MASK, blockZ & SECTOR_COORDINATE_MASK);
	}

	private static int sectorXorZ(int blockXorZ) {
		return blockXorZ >> BLOCK_TO_SECTOR_SHIFT_BITS;
	}

	private static int sectorY(int blockY) {
		return (blockY - MIN_WORLD_HEIGHT_BLOCKS_INCLUSIVE) >> BLOCK_TO_SECTOR_SHIFT_BITS;
	}

	//////////////////////////////////////////////////////////////////////////////////////////

	private final Long2ObjectOpenHashMap<RenderSector> map = new Long2ObjectOpenHashMap<>();
	private final FixedCapacityIndexAllocator allocator = new FixedCapacityIndexAllocator(MAX_SECTORS_LOADED);
	private int originBlockX, originBlockZ;
	private int originSectorX, originSectorZ;
	private final int[] sectorOffsets = new int[UNIFORM_ARRAY_LENGTH];

	public void clear() {
		synchronized (map) {
			map.clear();
			allocator.clear();
		}
	}

	public int[] uniformData() {
		return sectorOffsets;
	}

	public RenderSector findSector(RegionPosition origin) {
		final long key = sectorKey(origin.getX(), origin.getY(), origin.getZ());

		synchronized (map) {
			RenderSector result = map.get(key);

			if (result == null) {
				result = new RenderSector(origin);
				map.put(key, result);
				result.retain(origin);
				result.updateCameraDependentValues();
			} else {
				result.retain(origin);
			}

			return result;
		}
	}

	public void setCameraXZ(int blockX, int blockZ) {
		final int newBlockX = blockX & SECTOR_COORDINATE_MASK;
		final int newBlockZ = blockZ & SECTOR_COORDINATE_MASK;

		if (newBlockX != originBlockX || newBlockZ != originBlockZ) {
			originBlockX = newBlockX;
			originBlockZ = newBlockZ;

			sectorOffsets[UNIFORM_X_ORIGIN_INDEX] = originBlockX;
			sectorOffsets[UNIFORM_Z_ORIGIN_INDEX] = originBlockZ;
			originSectorX = sectorXorZ(blockX);
			originSectorZ = sectorXorZ(blockZ);

			synchronized (map) {
				for (var sector : map.values()) {
					sector.updateCameraDependentValues();
				}
			}
		}
	}

	public class RenderSector {
		// only used in dev environment
		final ReferenceOpenHashSet<RegionPosition> holders = new ReferenceOpenHashSet<>();

		int retainCount = 0;

		final long sectorKey;
		final int paddedBlockOriginX, paddedBlockOriginY, paddedBlockOriginZ;
		final int sectorOriginX, sectorOriginY, sectorOriginZ;
		int sectorId = -1;

		private RenderSector(RegionPosition origin) {
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			sectorKey = sectorKey(x, y, z);

			// Shifted negative to give positive packed coordinates with negative vertex coordinates
			paddedBlockOriginX = (x & SECTOR_COORDINATE_MASK) - 63;
			paddedBlockOriginY = (y & SECTOR_COORDINATE_MASK) - 63;
			paddedBlockOriginZ = (z & SECTOR_COORDINATE_MASK) - 63;

			sectorOriginX = sectorXorZ(x);
			sectorOriginY = sectorY(y);
			sectorOriginZ = sectorXorZ(z);

			sectorId = allocator.claimIndex();
		}

		public int sectorId() {
			assert sectorId != -1 : "using unallocated sector";
			return sectorId;
		}

		private void retain(RegionPosition origin) {
			assert sectorId != -1 : "retaining unallocated sector";
			assert holders.add(origin);
			++retainCount;
		}

		public RenderSector release(RegionPosition origin) {
			synchronized (map) {
				assert holders.remove(origin);

				if (--retainCount == 0) {
					allocator.releaseIndex(sectorId);
					map.remove(sectorKey);
				}

				return null;
			}
		}

		private void updateCameraDependentValues() {
			assert sectorId != -1 : "unallocated sector not removed from map";
			assert retainCount > 0 : "unused sector not removed from map";

			final int xOffset = SECTOR_XZ_RADIUS + sectorOriginX - originSectorX;
			final int zOffset = SECTOR_XZ_RADIUS + sectorOriginZ - originSectorZ;
			assert (xOffset & 0xF) == xOffset;
			assert (zOffset & 0xF) == zOffset;
			assert (sectorOriginY & 3) == sectorOriginY;

			final int packedOffset = (xOffset | (sectorOriginY << 4) | (zOffset << 8));
			final int wordIndex = sectorId >> 1;
			int wordValue = sectorOffsets[wordIndex];

			if ((sectorId & 1) == 1) {
				// odd
				wordValue = (wordValue & 0x0000FFFF) | (packedOffset << 16);
			} else {
				// even
				wordValue = (wordValue & 0xFFFF0000) | packedOffset;
			}

			sectorOffsets[wordIndex] = wordValue;
		}

		/**
		 * Returns packed 24-bit unsigned XYZ offset that should be
		 * added to region-relative vertex integer component.
		 *
		 * <p>The region origin is shifted -63 on all axis for this purpose
		 * (meaning the smallest value per axis is +63) so that we can
		 * handle odd vertex coordinates that extend outside the sector.
		 *
		 * <p>The sector is only 128 blocks wide on each axis, so this gives
		 * us ample margin.
		 */
		public int sectorRelativeRegionOrigin(RegionPosition position) {
			final int x = position.getX() - paddedBlockOriginX;
			final int y = position.getY() - paddedBlockOriginY;
			final int z = position.getZ() - paddedBlockOriginZ;

			return x | (y << 8) | (z << 16);
		}
	}
}
