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

package grondag.canvas.terrain.region;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;

import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.terrain.occlusion.OcclusionStatus;

public class RenderRegionStorage {
	private final AtomicInteger loadedRegionCount = new AtomicInteger();

	private final WorldRenderState worldRenderState;

	private final RenderChunk[] chunks = new RenderChunk[RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT];
	private final ArrayBlockingQueue<RenderChunk> closeQueue = new ArrayBlockingQueue<>(RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT);

	public RenderRegionStorage(WorldRenderState worldRenderState) {
		this.worldRenderState = worldRenderState;

		for (int i = 0; i < RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT; ++i) {
			chunks[i] = new RenderChunk(worldRenderState);
		}
	}

	public synchronized void clear(boolean resetWorld) {
		for (final RenderChunk chunk : chunks) {
			chunk.close(resetWorld);
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

			if (region.cameraVisibility.getOcclusionStatus() != OcclusionStatus.UNDETERMINED || ((worldRenderState.shadowsEnabled() && region.shadowVisibility.getOcclusionStatus() != OcclusionStatus.UNDETERMINED))) {
				worldRenderState.regionRebuildManager.acceptExternalBuildRequest(region);
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

	public @Nullable RenderRegion getOrCreateRegion(int x, int y, int z) {
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

	public boolean isPotentiallyVisible(int x, int y, int z) {
		final RenderRegion r = getRegionIfExists(x, y, z);
		return r == null || r.cameraVisibility.isPotentiallyVisible();
	}

	public void scheduleClose(RenderChunk chunk) {
		closeQueue.offer(chunk);
	}

	public void closeRegionsOnRenderThread() {
		RenderChunk chunk = closeQueue.poll();

		while (chunk != null) {
			chunk.close(false);
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
