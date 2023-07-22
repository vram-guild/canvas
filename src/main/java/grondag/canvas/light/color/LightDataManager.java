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
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

import grondag.canvas.pipeline.Pipeline;

public class LightDataManager {
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

			if (INSTANCE == null) {
				INSTANCE = new LightDataManager();
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

	public static void update(BlockAndTintGetter blockView) {
		if (INSTANCE != null) {
			INSTANCE.updateInner(blockView);
		}
	}

	public static int texId() {
		if (INSTANCE != null && INSTANCE.texture != null) {
			return INSTANCE.texture.texId();
		}

		return 0;
	}

	public static String debugString() {
		if (INSTANCE != null) {
			return INSTANCE.texAllocator.debugString();
		}

		return "Colored lights DISABLED";
	}

	private final Long2ObjectMap<LightRegion> allocated = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>(1024));

	final LongSet publicUpdateQueue = LongSets.synchronize(new LongOpenHashSet());
	final LongSet publicDrawQueue = LongSets.synchronize(new LongOpenHashSet());
	private final LongSet decreaseQueue = new LongOpenHashSet();
	private final LongSet increaseQueue = new LongOpenHashSet();

	private final LightDataAllocator texAllocator;

	boolean useOcclusionData = false;
	private LightDataTexture texture;

	public LightDataManager() {
		texAllocator = new LightDataAllocator();
		allocated.defaultReturnValue(null);
	}

	private void updateInner(BlockAndTintGetter blockView) {
		synchronized (publicUpdateQueue) {
			for (long index : publicUpdateQueue) {
				decreaseQueue.add(index);
				increaseQueue.add(index);
			}

			publicUpdateQueue.clear();
		}

		while (!decreaseQueue.isEmpty()) {
			// hopefully faster with native op?
			final int queueLen = decreaseQueue.size();
			final long queueIterator = MemoryUtil.nmemAlloc(8L * queueLen);

			long i = 0L;

			for (long index : decreaseQueue) {
				MemoryUtil.memPutLong(queueIterator + i * 8L, index);
				i++;
			}

			decreaseQueue.clear();

			for (i = 0; i < queueLen; i++) {
				final long index = MemoryUtil.memGetLong(queueIterator + i * 8L);
				final LightRegion lightRegion = allocated.get(index);

				if (lightRegion != null && !lightRegion.isClosed()) {
					lightRegion.updateDecrease(blockView, decreaseQueue, increaseQueue);
				}
			}

			MemoryUtil.nmemFree(queueIterator);
		}

		while (!increaseQueue.isEmpty()) {
			// hopefully faster with native op?
			final int queueLen = increaseQueue.size();
			final long queueIterator = MemoryUtil.nmemAlloc(8L * queueLen);

			long i = 0L;

			for (long index : increaseQueue) {
				MemoryUtil.memPutLong(queueIterator + i * 8L, index);
				i++;
			}

			increaseQueue.clear();

			for (i = 0; i < queueLen; i++) {
				final long index = MemoryUtil.memGetLong(queueIterator + i * 8L);
				final LightRegion lightRegion = allocated.get(index);

				if (lightRegion != null && !lightRegion.isClosed()) {
					lightRegion.updateIncrease(blockView, increaseQueue);
				}
			}

			MemoryUtil.nmemFree(queueIterator);
		}

		if (texAllocator.checkInvalid()) {
			if (texture != null) {
				texture.close();
			}

			texture = new LightDataTexture(texAllocator.textureWidth(), texAllocator.textureHeight());
			texAllocator.textureRemade();

			publicDrawQueue.clear();

			// redraw
			for (var lightRegion : allocated.values()) {
				if (!lightRegion.isClosed()) {
					drawInner(lightRegion, true);
				}
			}
		} else if (!publicDrawQueue.isEmpty()) {
			final int queueLen;
			final long queueIterator;

			long i = 0L;

			synchronized (publicDrawQueue) {
				// hopefully faster with native op?
				queueLen = publicDrawQueue.size();
				queueIterator = MemoryUtil.nmemAlloc(8L * queueLen);

				for (long index : publicDrawQueue) {
					MemoryUtil.memPutLong(queueIterator + i * 8L, index);
					i++;
				}

				publicDrawQueue.clear();
			}

			for (i = 0; i < queueLen; i++) {
				final long index = MemoryUtil.memGetLong(queueIterator + i * 8L);
				final LightRegion lightRegion = allocated.get(index);

				if (lightRegion != null && !lightRegion.isClosed()) {
					drawInner(lightRegion, false);
				}
			}

			MemoryUtil.nmemFree(queueIterator);
		}

		texAllocator.uploadPointersIfNeeded(texture);
	}

	private void drawInner(LightRegion lightRegion, boolean redraw) {
		if (lightRegion.lightData.hasBuffer() && (lightRegion.lightData.isDirty() || redraw)) {
			final int targetAddress = texAllocator.allocateAddress(lightRegion);

			if (targetAddress != LightDataAllocator.EMPTY_ADDRESS) {
				final int targetRow = texAllocator.dataRowStart() + targetAddress;
				texture.upload(targetRow, lightRegion.lightData.getBuffer());
			}

			lightRegion.lightData.clearDirty();
		}
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
}
