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
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongPriorityQueue;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

import io.vram.frex.api.config.FlawlessFrames;

import grondag.canvas.CanvasMod;
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
		if (INSTANCE != null) {
			INSTANCE.close();
			INSTANCE = null;
		}

		if (Pipeline.coloredLightsEnabled()) {
			assert Pipeline.config().coloredLights != null;

			INSTANCE = new LightDataManager();
			INSTANCE.useOcclusionData = Pipeline.config().coloredLights.useOcclusionData;
		}
	}

	public static void free(BlockPos regionOrigin) {
		if (INSTANCE != null) {
			INSTANCE.freeInner(regionOrigin);
		}
	}

	public static void update(BlockAndTintGetter blockView, long deadlineNanos, Runnable profilerTask) {
		if (INSTANCE != null) {
			profilerTask.run();
			INSTANCE.profiler.start();
			INSTANCE.updateInner(blockView, FlawlessFrames.isActive() ? Long.MAX_VALUE : deadlineNanos);
			INSTANCE.profiler.end();
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
	private final LongPriorityQueue decreaseQueue = new LongArrayFIFOQueue();
	private final LongPriorityQueue increaseQueue = new LongArrayFIFOQueue();

	final LongSet publicUrgentUpdateQueue = LongSets.synchronize(new LongOpenHashSet());
	private final LongPriorityQueue urgentDecreaseQueue = new LongArrayFIFOQueue();
	private final LongPriorityQueue urgentIncreaseQueue = new LongArrayFIFOQueue();

	private final LightDataAllocator texAllocator;

	boolean useOcclusionData = false;
	private LightDataTexture texture;

	private DebugProfiler profiler = new ActiveProfiler();
	public LightDataManager() {
		texAllocator = new LightDataAllocator();
		allocated.defaultReturnValue(null);
	}

	private void executeRegularUpdates(BlockAndTintGetter blockView, int minimumUpdates, long deadlineNanos) {
		synchronized (publicUpdateQueue) {
			for (long index : publicUpdateQueue) {
				decreaseQueue.enqueue(index);
				increaseQueue.enqueue(index);
			}

			publicUpdateQueue.clear();
		}

		int count = 0;

		while (!decreaseQueue.isEmpty()) {
			final long index = decreaseQueue.dequeueLong();
			final LightRegion lightRegion = allocated.get(index);

			if (lightRegion != null && !lightRegion.isClosed()) {
				lightRegion.updateDecrease(blockView, decreaseQueue, increaseQueue);
			}

			if (++count > minimumUpdates && System.nanoTime() > deadlineNanos) {
				break;
			}
		}

		count = 0;

		while (!increaseQueue.isEmpty()) {
			final long index = increaseQueue.dequeueLong();
			final LightRegion lightRegion = allocated.get(index);

			if (lightRegion != null && !lightRegion.isClosed()) {
				lightRegion.updateIncrease(blockView, increaseQueue);
			}

			if (++count > minimumUpdates && System.nanoTime() > deadlineNanos) {
				break;
			}
		}
	}

	private void updateInner(BlockAndTintGetter blockView, long deadlineNanos) {
		synchronized (publicUrgentUpdateQueue) {
			for (long index : publicUrgentUpdateQueue) {
				urgentDecreaseQueue.enqueue(index);
				urgentIncreaseQueue.enqueue(index);
				publicUpdateQueue.remove(index);
			}

			publicUrgentUpdateQueue.clear();
		}

		while (!urgentDecreaseQueue.isEmpty()) {
			final long index = urgentDecreaseQueue.dequeueLong();
			final LightRegion lightRegion = allocated.get(index);

			if (lightRegion != null && !lightRegion.isClosed()) {
				lightRegion.updateDecrease(blockView, urgentDecreaseQueue, urgentIncreaseQueue);
			}
		}

		executeRegularUpdates(blockView, 7, deadlineNanos);

		while (!urgentIncreaseQueue.isEmpty()) {
			final long index = urgentIncreaseQueue.dequeueLong();
			final LightRegion lightRegion = allocated.get(index);

			if (lightRegion != null && !lightRegion.isClosed()) {
				lightRegion.updateIncrease(blockView, urgentIncreaseQueue);
			}
		}

		if (texAllocator.checkInvalid()) {
			if (texture != null) {
				texture.close();
			}

			texture = new LightDataTexture(texAllocator.textureWidth(), texAllocator.textureHeight());
			texAllocator.textureRemade();

			publicDrawQueue.clear();

			// redraw
			synchronized (allocated) {
				for (var lightRegion : allocated.values()) {
					if (!lightRegion.isClosed()) {
						drawInner(lightRegion, true);
					}
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

	private interface DebugProfiler {
		void start();
		void end();
	}

	private static class EmptyProfiler implements DebugProfiler {
		@Override
		public void start() {
		}

		@Override
		public void end() {
		}
	}

	private final class ActiveProfiler implements DebugProfiler {
		private long minUpdateTimeNanos = Long.MAX_VALUE;
		private long maxUpdateTimeNanos = 0;
		private long totalUpdateTimeNanos = 0;
		private long totalUpdatePerformed = 0;
		private long startTimeNanos = 0;
		private long startTimeOverall = 0;
		private boolean init = false;

		@Override
		public void start() {
			startTimeNanos = System.nanoTime();

			if (!init) {
				startTimeOverall = startTimeNanos;
				init = true;
			}
		}

		@Override
		public void end() {
			final long elapsedNanos = System.nanoTime() - startTimeNanos;
			minUpdateTimeNanos = Math.min(elapsedNanos, minUpdateTimeNanos);
			maxUpdateTimeNanos = Math.max(elapsedNanos, maxUpdateTimeNanos);
			totalUpdateTimeNanos += elapsedNanos;
			totalUpdatePerformed ++;

			if (System.nanoTime() - startTimeOverall > 10000000000L) {
				CanvasMod.LOG.info("Colored Lights profiler output:");
				CanvasMod.LOG.info("min update time:  " + ((float) minUpdateTimeNanos / 1000000.0f) + "ms");
				CanvasMod.LOG.info("max update time:  " + ((float) maxUpdateTimeNanos / 1000000.0f) + "ms");
				CanvasMod.LOG.info("avg. update time: " + (((float) totalUpdateTimeNanos / (float) totalUpdatePerformed) / 1000000.0f) + "ms (over 10 seconds)");
				LightDataManager.this.profiler = new EmptyProfiler();
			}
		}
	}
}
