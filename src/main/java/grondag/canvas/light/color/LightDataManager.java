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
import org.joml.Vector3i;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

import grondag.canvas.pipeline.Image;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.shader.data.ShaderDataManager;

public class LightDataManager {
	private static final boolean debugRedrawEveryFrame = false;

	static LightDataManager INSTANCE;

	public static LightRegionAccess allocate(BlockPos regionOrigin) {
		if (INSTANCE == null) {
			return LightRegionAccess.EMPTY;
		}

		return INSTANCE.allocateInner(regionOrigin);
	}

	public static void reload() {
		if (Pipeline.coloredLightsEnabled()) {
			assert Pipeline.config().coloredLights != null;
			final var image = Pipeline.getImage(Pipeline.config().coloredLights.lightImage.name);

			if (INSTANCE == null) {
				INSTANCE = new LightDataManager(image);
			} else {
				INSTANCE.resize(image);
			}
		} else {
			if (INSTANCE != null) {
				INSTANCE.close();
				INSTANCE = null;
			}
		}
	}

	public static void free(BlockPos regionOrigin) {
		if (INSTANCE != null) {
			INSTANCE.freeInner(regionOrigin);
		}
	}

	public static void update(BlockAndTintGetter blockView, int cameraX, int cameraY, int cameraZ) {
		if (INSTANCE != null) {
			INSTANCE.updateInner(blockView, cameraX, cameraY, cameraZ);
		}
	}

	private final Long2ObjectMap<LightRegion> allocated = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

	private final Vector3i extentOrigin = new Vector3i();

	private int extentGridMaskX;
	private int extentGridMaskY;
	private int extentGridMaskZ;

	private int extentSizeX;
	private int extentSizeY;
	private int extentSizeZ;

	private boolean cameraUninitialized = true;
	private boolean extentWasResized = false;

	private int extentSizeXInRegions = 0;
	private int extentSizeYInRegions = 0;
	private int extentSizeZInRegions = 0;
	private LightDataTexture texture;
	ExtentIterable extentIterable = new ExtentIterable();

	public LightDataManager(Image image) {
		allocated.defaultReturnValue(null);
		init(image);
	}

	private void resize(Image image) {
		init(image);
		extentWasResized = true;
	}

	private void init(Image image) {
		// TODO: for some reason it's not working properly when width =/= depth (height is fine)
		extentSizeXInRegions = image.config.width / 16;
		extentSizeYInRegions = image.config.height / 16;
		extentSizeZInRegions = image.config.depth / 16;

		extentGridMaskX = extentSizeMask(extentSizeXInRegions);
		extentGridMaskY = extentSizeMask(extentSizeYInRegions);
		extentGridMaskZ = extentSizeMask(extentSizeZInRegions);

		extentSizeX = extentSizeInBlocks(extentSizeXInRegions);
		extentSizeY = extentSizeInBlocks(extentSizeYInRegions);
		extentSizeZ = extentSizeInBlocks(extentSizeZInRegions);

		texture = new LightDataTexture(image);
	}

	private void updateInner(BlockAndTintGetter blockView, int cameraX, int cameraY, int cameraZ) {
		final int regionSnapMask = ~LightRegionData.Const.WIDTH_MASK;

		final int prevExtentBlockX = extentOrigin.x;
		final int prevExtentBlockY = extentOrigin.y;
		final int prevExtentBlockZ = extentOrigin.z;

		// snap camera position to the nearest region (chunk)
		extentOrigin.x = (cameraX & regionSnapMask) - extentSizeInBlocks(extentSizeXInRegions / 2);
		extentOrigin.y = (cameraY & regionSnapMask) - extentSizeInBlocks(extentSizeYInRegions / 2);
		extentOrigin.z = (cameraZ & regionSnapMask) - extentSizeInBlocks(extentSizeZInRegions / 2);

		ShaderDataManager.updateLightVolumeOrigin(extentOrigin);

		boolean extentMoved = !extentOrigin.equals(prevExtentBlockX, prevExtentBlockY, prevExtentBlockZ);

		boolean needUpdate = true;

		// TODO: account for extent-edge chunks?
		// process all active regions' decrease queue until none is left
		while (needUpdate) {
			needUpdate = false;

			for (long index : extentIterable) {
				final LightRegion lightRegion = allocated.get(index);

				if (lightRegion != null && !lightRegion.isClosed()) {
					needUpdate |= lightRegion.updateDecrease(blockView);
				}
			}
		}

		boolean shouldRedraw = !cameraUninitialized && extentMoved;

		needUpdate = true;

		// TODO: optimize active region traversal with queue
		while (needUpdate) {
			needUpdate = false;

			for (long index : extentIterable) {
				final LightRegion lightRegion = allocated.get(index);

				if (lightRegion != null && !lightRegion.isClosed()) {
					needUpdate |= lightRegion.updateIncrease(blockView);
				}
			}
		}

		// TODO: swap texture in case of sparse, perhaps
		for (long index:extentIterable) {
			final LightRegion lightRegion = allocated.get(index);

			if (lightRegion == null || lightRegion.isClosed()) {
				continue;
			}

			boolean outsidePrev = false;

			final int x = lightRegion.lightData.regionOriginBlockX;
			final int y = lightRegion.lightData.regionOriginBlockY;
			final int z = lightRegion.lightData.regionOriginBlockZ;

			if (shouldRedraw) {
				// Redraw regions that just entered the current-frame extent
				outsidePrev |= x < prevExtentBlockX || x >= (prevExtentBlockX + extentSizeX);
				outsidePrev |= y < prevExtentBlockY || y >= (prevExtentBlockY + extentSizeY);
				outsidePrev |= z < prevExtentBlockZ || z >= (prevExtentBlockZ + extentSizeZ);
			}

			if (lightRegion.lightData.isDirty() || outsidePrev || extentWasResized || debugRedrawEveryFrame) {
				// modulo into extent-grid
				texture.upload(x & extentGridMaskX, y & extentGridMaskY, z & extentGridMaskZ, lightRegion.lightData.getBuffer());
				lightRegion.lightData.clearDirty();
			}
		}

		extentWasResized = false;
		cameraUninitialized = false;
	}

	LightRegion getFromBlock(BlockPos blockPos) {
		final long key = BlockPos.asLong(
				blockPos.getX() & ~LightRegionData.Const.WIDTH_MASK,
				blockPos.getY() & ~LightRegionData.Const.WIDTH_MASK,
				blockPos.getZ() & ~LightRegionData.Const.WIDTH_MASK);
		return allocated.get(key);
	}

	private void freeInner(BlockPos regionOrigin) {
		final LightRegion lightRegion = allocated.get(regionOrigin.asLong());

		if (lightRegion != null && !lightRegion.isClosed()) {
			lightRegion.close();
		}

		allocated.remove(regionOrigin.asLong());
	}

	private LightRegion allocateInner(BlockPos regionOrigin) {
		if (allocated.containsKey(regionOrigin.asLong())) {
			freeInner(regionOrigin);
		}

		final LightRegion lightRegion = new LightRegion(regionOrigin);
		allocated.put(regionOrigin.asLong(), lightRegion);

		return lightRegion;
	}

	private int extentSizeInBlocks(int extentSize) {
		return extentSize * LightRegionData.Const.WIDTH;
	}

	private int extentSizeMask(int extentSize) {
		return extentSizeInBlocks(extentSize) - 1;
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

	private class ExtentIterable implements LongIterable, LongIterator {
		final int extent = extentSizeInBlocks(1);

		@Override
		public LongIterator iterator() {
			x = y = z = 0;
			return this;
		}

		int x, y, z;

		@Override
		public long nextLong() {
			final long value = BlockPos.asLong(extentOrigin.x + x * extent, extentOrigin.y + y * extent, extentOrigin.z + z * extent);

			if (++z >= extentSizeZInRegions) {
				z = 0;
				y++;
			}

			if (y >= extentSizeYInRegions) {
				y = 0;
				x++;
			}

			return value;
		}

		@Override
		public boolean hasNext() {
			return x < extentSizeXInRegions;
		}
	}
}
