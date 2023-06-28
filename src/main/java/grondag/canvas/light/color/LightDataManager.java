/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.light.color;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterable;
import it.unimi.dsi.fastutil.longs.LongIterator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

import grondag.canvas.CanvasMod;

public class LightDataManager {
	// NB: must be even
	private static final int REGION_COUNT_LENGTH_WISE = 32;
	private static final boolean debugRedrawEveryFrame = false;
	// private static final int INITIAL_LIMIT = REGION_COUNT_LENGTH_WISE * REGION_COUNT_LENGTH_WISE * REGION_COUNT_LENGTH_WISE;

	public static final LightDataManager INSTANCE = new LightDataManager();

	private final Long2ObjectMap<LightRegion> allocated = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

	private int extentStartBlockX = 0;
	private int extentStartBlockY = 0;
	private int extentStartBlockZ = 0;
	private boolean cameraUninitialized = true;

	// NB: must be even
	private int extentSizeInRegions = REGION_COUNT_LENGTH_WISE;
	private LightDataTexture texture;
	ExtentIterable extentIterable = new ExtentIterable();

	{
		allocated.defaultReturnValue(null);
	}

	private class ExtentIterable implements LongIterable, LongIterator  {
		@Override
		public LongIterator iterator() {
			x = y = z = 0;
			return this;
		}

		int x, y, z, startX, startY, startZ;

		void set(int startX, int startY, int startZ) {
			this.startX = startX;
			this.startY = startY;
			this.startZ = startZ;
		}

		@Override
		public long nextLong() {
			final int extent = extentSizeInBlocks(1);
			final long value = BlockPos.asLong(startX + x * extent, startY + y * extent, startZ + z * extent);
			if (++z >= extentSizeInRegions) {
				z = 0;
				y++;
			}

			if (y >= extentSizeInRegions) {
				y = 0;
				x++;
			}

			return value;
		}

		@Override
		public boolean hasNext() {
			return x < extentSizeInRegions;
		}
	}

	// TODO: stuff
	public static void initialize() {

	}

	public void update(BlockAndTintGetter blockView, int cameraX, int cameraY, int cameraZ) {
		if (texture == null) {
			initializeTexture();
		}

		final int regionSnapMask = ~LightRegionData.Const.WIDTH_MASK;
		final int halfRadius = extentSizeInBlocks(extentSizeInRegions / 2);

		final int prevExtentX = extentStartBlockX;
		final int prevExtentY = extentStartBlockY;
		final int prevExtentZ = extentStartBlockZ;

		// snap camera position to the nearest region (chunk)
		extentStartBlockX = (cameraX & regionSnapMask) - halfRadius;
		extentStartBlockY = (cameraY & regionSnapMask) - halfRadius;
		extentStartBlockZ = (cameraZ & regionSnapMask) - halfRadius;

		if (!cameraUninitialized
				&& (extentStartBlockX != prevExtentX || extentStartBlockY != prevExtentY || extentStartBlockZ != prevExtentZ)) {
			//TODO: IMPORTANT: re-draw newly entered regions
			//TODO: if newly entered region is null, clear using dummy (empty) lightDataRegion
			//TODO: cleanup dummy lightDataRegion in close()
			CanvasMod.LOG.info("Extent have changed");
		}

		cameraUninitialized = false;

		extentIterable.set(extentStartBlockX, extentStartBlockY, extentStartBlockZ);

		boolean needUpdate = true;

		// TODO: account for extent-edge chunks?
		// process all active regions' decrease queue until none is left
		while (needUpdate) {
			needUpdate = false;

			for (long index : extentIterable) {
				final LightRegion lightRegion = allocated.get(index);

				if (lightRegion != null && !lightRegion.isClosed()) {
					needUpdate = needUpdate || lightRegion.updateDecrease(blockView);
				}
			}
		}

		// update all regions within extent
		for (long index:extentIterable) {
			final LightRegion lightRegion = allocated.get(index);

			if (lightRegion == null || lightRegion.isClosed()) {
				continue;
			}

			lightRegion.updateIncrease(blockView);

			if (lightRegion.lightData.isDirty() || debugRedrawEveryFrame) {
				final int extentGridMask = extentSizeMask();
				final int x = lightRegion.lightData.regionOriginBlockX;
				final int y = lightRegion.lightData.regionOriginBlockY;
				final int z = lightRegion.lightData.regionOriginBlockZ;
				// modulo into extent-grid
				texture.upload(x & extentGridMask, y & extentGridMask, z & extentGridMask, lightRegion.lightData.getBuffer());
				lightRegion.lightData.clearDirty();
			}
		}
	}

	public LightRegion getFromBlock(BlockPos blockPos) {
		final long key = BlockPos.asLong(
				blockPos.getX() & ~LightRegionData.Const.WIDTH_MASK,
				blockPos.getY() & ~LightRegionData.Const.WIDTH_MASK,
				blockPos.getZ() & ~LightRegionData.Const.WIDTH_MASK);
		return allocated.get(key);
	}

	public void deallocate(BlockPos regionOrigin) {
		final LightRegion lightRegion = allocated.get(regionOrigin.asLong());

		if (lightRegion != null && !lightRegion.isClosed()) {
			lightRegion.close();
		}

		allocated.remove(regionOrigin.asLong());
	}

	public LightRegion allocate(BlockPos regionOrigin) {
		if (allocated.containsKey(regionOrigin.asLong())) {
			deallocate(regionOrigin);
		}

		final LightRegion lightRegion = new LightRegion(regionOrigin);
		allocated.put(regionOrigin.asLong(), lightRegion);

		return lightRegion;
	}

	private int extentSizeInBlocks(int extentSize) {
		return extentSize * LightRegionData.Const.WIDTH;
	}

	private int extentSizeInBlocks() {
		return extentSizeInBlocks(extentSizeInRegions);
	}

	private int extentSizeMask() {
		return extentSizeInBlocks() - 1;
	}

	private void initializeTexture() {
		texture = new LightDataTexture(extentSizeInBlocks());
	}

	public void close() {
		texture.close();

		synchronized (allocated) {
			for (var lightRegion : allocated.values()) {
				if (!lightRegion.isClosed()) {
					lightRegion.close();
				}
			}

			allocated.clear();
		}
	}

	public int getTexture(String imageName) {
		if (imageName.equals("canvas:alpha/light_data")) {
			if (texture == null) {
				initializeTexture();
			}

			return texture.getTexId();
		}

		return -1;
	}

	// public void queueUpdate(LightRegion lightRegion) {
	// 	updateQueue.enqueue(lightRegion.origin);
	// }
}
