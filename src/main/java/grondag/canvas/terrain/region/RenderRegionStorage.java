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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.render.CanvasWorldRenderer;

public class RenderRegionStorage {
	private final AtomicInteger loadedRegionCount = new AtomicInteger();

	public final CanvasWorldRenderer cwr;

	private final RenderChunk[] chunks = new RenderChunk[RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT];
	private final ArrayBlockingQueue<RenderChunk> closeQueue = new ArrayBlockingQueue<>(RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT);

	public RenderRegionStorage(CanvasWorldRenderer canvasWorldRenderer) {
		cwr = canvasWorldRenderer;

		for (int i = 0; i < RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT; ++i) {
			chunks[i] = new RenderChunk(this);
		}
	}

	public synchronized void clear() {
		for (final RenderChunk chunk : chunks) {
			chunk.close();
		}
	}

	public void scheduleRebuild(int x, int y, int z, boolean urgent) {
		final RenderRegion region = getRegionIfExists(x, y, z);

		if (region != null) {
			region.markForBuild(urgent);
			// Marking the region for rebuild doesn't cause iteration to be rerun.
			// We don't know if the change would have affected occlusion so we
			// have to assume that it did and if it was within the potential visible
			// set we need to rerun iteration.

			if (region.occlusionState.isInCurrentPVS()) {
				cwr.regionRebuildManager.acceptExternalBuildRequest(region);
			}
		}
	}

	public void updateRegionPositionAndVisibility() {
		for (int i = 0; i < RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT; ++i) {
			chunks[i].updatePositionAndVisibility();
		}
	}

	public int loadedRegionCount() {
		return loadedRegionCount.get();
	}

	public RenderRegion getOrCreateRegion(int x, int y, int z) {
		return chunks[RenderRegionIndexer.chunkIndex(x, z)].getOrCreateRegion(x, y, z);
	}

	public RenderRegion getOrCreateRegion(BlockPos pos) {
		return getOrCreateRegion(pos.getX(), pos.getY(), pos.getZ());
	}

	public RenderRegion getRegionIfExists(BlockPos pos) {
		return getRegionIfExists(pos.getX(), pos.getY(), pos.getZ());
	}

	public RenderRegion getRegionIfExists(int x, int y, int z) {
		return chunks[RenderRegionIndexer.chunkIndex(x, z)].getRegionIfExists(x, y, z);
	}

	public boolean wasSeenFromCamera(int x, int y, int z) {
		final RenderRegion r = getRegionIfExists(x, y, z);
		return r != null && r.occlusionState.wasRecentlySeenFromCamera();
	}

	public void scheduleClose(RenderChunk chunk) {
		closeQueue.offer(chunk);
	}

	public void closeRegionsOnRenderThread() {
		RenderChunk chunk = closeQueue.poll();

		while (chunk != null) {
			chunk.close();
			chunk = closeQueue.poll();
		}
	}

	void trackRegionClosed() {
		loadedRegionCount.decrementAndGet();
	}

	void trackRegionLoaded() {
		loadedRegionCount.incrementAndGet();
	}
}
