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
	final AtomicInteger regionCount = new AtomicInteger();
	public final RenderRegionPruner regionPruner;
	final CanvasWorldRenderer cwr;
	private int lastCameraChunkX = Integer.MAX_VALUE;
	private int lastCameraChunkZ = Integer.MAX_VALUE;

	static final int CHUNK_COUNT = 128 * 128;
	private final RenderRegionChunk[] chunks = new RenderRegionChunk[CHUNK_COUNT];

	// WIP: so big?
	private final ArrayBlockingQueue<RenderRegionChunk> closeQueue = new ArrayBlockingQueue<>(RenderRegionStorage.CHUNK_COUNT);

	public RenderRegionStorage(CanvasWorldRenderer canvasWorldRenderer, RenderRegionPruner pruner) {
		cwr = canvasWorldRenderer;
		regionPruner = pruner;

		for (int i = 0; i < CHUNK_COUNT; ++i) {
			chunks[i] = new RenderRegionChunk(this);
		}
	}

	public synchronized void clear() {
		for (final RenderRegionChunk chunk : chunks) {
			chunk.close();
		}
	}

	private static int chunkIndex(int x, int z) {
		x = ((x + 30000000) >> 4) & 127;
		z = ((z + 30000000) >> 4) & 127;

		return x | (z << 7);
	}

	public void scheduleRebuild(int x, int y, int z, boolean urgent) {
		if ((y & 0xFFFFFF00) == 0) {
			final BuiltRenderRegion region = getRegionIfExists(x, y, z);

			if (region != null) {
				region.markForBuild(urgent);
			}
		}
	}

	int chunkDistVersion = 1;

	public void updateCameraDistanceAndVisibilityInfo(long cameraChunkOrigin) {
		regionPruner.prepare(cameraChunkOrigin);

		final long cameraChunkPos = regionPruner.cameraChunkPos();
		final int cameraChunkX = BlockPos.unpackLongX(cameraChunkPos);
		final int cameraChunkZ = BlockPos.unpackLongZ(cameraChunkPos);

		if (!(cameraChunkX == lastCameraChunkX && cameraChunkZ == lastCameraChunkZ)) {
			lastCameraChunkX = cameraChunkX;
			lastCameraChunkZ = cameraChunkZ;
			++chunkDistVersion;
		}

		for (int i = 0; i < CHUNK_COUNT; ++i) {
			chunks[i].updateCameraDistanceAndVisibilityInfo();
		}

		if (regionPruner.didInvalidateOccluder()) {
			regionPruner.occluder.invalidate();
		}
	}

	public int regionCount() {
		return regionCount.get();
	}

	public BuiltRenderRegion getOrCreateRegion(int x, int y, int z) {
		return chunks[chunkIndex(x, z)].getOrCreateRegion(x, y, z);
	}

	public BuiltRenderRegion getOrCreateRegion(BlockPos pos) {
		return getOrCreateRegion(pos.getX(), pos.getY(), pos.getZ());
	}

	public BuiltRenderRegion getRegionIfExists(BlockPos pos) {
		return getRegionIfExists(pos.getX(), pos.getY(), pos.getZ());
	}

	public BuiltRenderRegion getRegionIfExists(int x, int y, int z) {
		return chunks[chunkIndex(x, z)].getRegionIfExists(x, y, z);
	}

	public boolean wasSeen(int x, int y, int z) {
		final BuiltRenderRegion r = getRegionIfExists(x, y, z);
		return r != null && r.wasRecentlySeen();
	}

	public void scheduleClose(RenderRegionChunk chunk) {
		closeQueue.offer(chunk);
	}

	public void closeRegionsOnRenderThread() {
		final RenderRegionChunk chunk = closeQueue.poll();

		if (chunk != null) {
			chunk.close();
		}
	}
}
