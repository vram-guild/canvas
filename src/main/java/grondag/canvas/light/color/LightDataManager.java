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
			final var extentSizeRegions = Pipeline.config().coloredLights.maxRadiusChunks * 2;

			if (INSTANCE == null) {
				INSTANCE = new LightDataManager(extentSizeRegions);
			} else {
				INSTANCE.resize(extentSizeRegions);
			}

			INSTANCE.useOcclusionData = Pipeline.config().coloredLights.useOcclusionData;
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

	public static int texId() {
		if (INSTANCE != null && INSTANCE.texture != null) {
			return INSTANCE.texture.texId();
		}

		return 0;
	}

	private final Long2ObjectMap<LightRegion> allocated = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

	private final Vector3i extentOrigin = new Vector3i();
	private final LightDataAllocator texAllocator;

	boolean useOcclusionData = false;

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

	public LightDataManager(int newExtentSize) {
		texAllocator = new LightDataAllocator();
		allocated.defaultReturnValue(null);
		init(newExtentSize);
	}

	private void resize(int newExtentSize) {
		init(newExtentSize);
		extentWasResized = true;
	}

	private void init(int newExtentSize) {
		extentSizeXInRegions = newExtentSize;
		extentSizeYInRegions = newExtentSize;
		extentSizeZInRegions = newExtentSize;

		extentSizeX = extentSizeInBlocks(extentSizeXInRegions);
		extentSizeY = extentSizeInBlocks(extentSizeYInRegions);
		extentSizeZ = extentSizeInBlocks(extentSizeZInRegions);
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

		boolean textureOrExtentChanged = extentWasResized;

		if (texAllocator.checkInvalid()) {
			if (texture != null) {
				texture.close();
			}

			texture = new LightDataTexture(texAllocator.textureWidth(), texAllocator.textureHeight());
			texAllocator.textureRemade();
			textureOrExtentChanged = true;
		}

		// TODO: swap texture in case of sparse, perhaps
		for (long index : extentIterable) {
			final LightRegion lightRegion = allocated.get(index);

			if (lightRegion == null || lightRegion.isClosed()) {
				continue;
			}

			// debug
			texAllocator.allocateAddress(lightRegion);

			boolean outsidePrev = false;

			if (shouldRedraw && !textureOrExtentChanged) {
				// Redraw regions that just entered the current-frame extent
				outsidePrev |= lightRegion.lightData.regionOriginBlockX < prevExtentBlockX || lightRegion.lightData.regionOriginBlockX >= (prevExtentBlockX + extentSizeX);
				outsidePrev |= lightRegion.lightData.regionOriginBlockY < prevExtentBlockY || lightRegion.lightData.regionOriginBlockY >= (prevExtentBlockY + extentSizeY);
				outsidePrev |= lightRegion.lightData.regionOriginBlockZ < prevExtentBlockZ || lightRegion.lightData.regionOriginBlockZ >= (prevExtentBlockZ + extentSizeZ);
			}

			if (lightRegion.lightData.hasBuffer() && (lightRegion.lightData.isDirty() || outsidePrev || textureOrExtentChanged || debugRedrawEveryFrame)) {
				final int targetAddress = texAllocator.allocateAddress(lightRegion);

				if (targetAddress != LightDataAllocator.EMPTY_ADDRESS) {
					final int targetRow = texAllocator.dataRowStart() + targetAddress;
					texture.upload(targetRow, lightRegion.lightData.getBuffer());
				}

				lightRegion.lightData.clearDirty();
			}
		}

		texAllocator.uploadPointersIfNeeded(texture);

		extentWasResized = false;
		cameraUninitialized = false;
		texAllocator.debug_PrintAddressCount();
	}

	LightRegion getFromBlock(BlockPos blockPos) {
		final long key = BlockPos.asLong(
				blockPos.getX() & ~LightRegionData.Const.WIDTH_MASK,
				blockPos.getY() & ~LightRegionData.Const.WIDTH_MASK,
				blockPos.getZ() & ~LightRegionData.Const.WIDTH_MASK);
		return allocated.get(key);
	}

	LightRegion get(long originKey) {
		return allocated.get(originKey);
	}

	private void freeInner(BlockPos regionOrigin) {
		final LightRegion lightRegion = allocated.get(regionOrigin.asLong());

		if (lightRegion != null && !lightRegion.isClosed()) {
			texAllocator.freeAddress(lightRegion);
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
