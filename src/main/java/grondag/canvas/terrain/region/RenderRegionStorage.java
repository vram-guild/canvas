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
import grondag.canvas.terrain.occlusion.PotentiallyVisibleRegionSorter;

public class RenderRegionStorage {
	/**
	 * Sorts regions within render distance from near to far relative to the camera.
	 */
	public final PotentiallyVisibleRegionSorter cameraDistanceSorter = new PotentiallyVisibleRegionSorter();

	final AtomicInteger regionCount = new AtomicInteger();
	final CanvasWorldRenderer cwr;
	private int lastCameraChunkX = Integer.MAX_VALUE;
	private int lastCameraChunkY = Integer.MAX_VALUE;
	private int lastCameraChunkZ = Integer.MAX_VALUE;

	private boolean didInvalidateOccluder = false;
	private int occluderVersion = 0;
	private int maxSquaredChunkDistance;

	static final int CHUNK_COUNT = 128 * 128;
	private final RenderRegionChunk[] chunks = new RenderRegionChunk[CHUNK_COUNT];
	private final ArrayBlockingQueue<RenderRegionChunk> closeQueue = new ArrayBlockingQueue<>(RenderRegionStorage.CHUNK_COUNT);

	public RenderRegionStorage(CanvasWorldRenderer canvasWorldRenderer) {
		cwr = canvasWorldRenderer;

		for (int i = 0; i < CHUNK_COUNT; ++i) {
			chunks[i] = new RenderRegionChunk(this);
		}
	}

	public int occluderVersion() {
		return occluderVersion;
	}

	public void invalidateOccluder() {
		didInvalidateOccluder = true;
	}

	public int maxSquaredChunkDistance() {
		return maxSquaredChunkDistance;
	}

	public synchronized void clear() {
		cameraDistanceSorter.clear();

		for (final RenderRegionChunk chunk : chunks) {
			chunk.close();
		}
	}

	public int cameraChunkX() {
		return lastCameraChunkX;
	}

	public int cameraChunkY() {
		return lastCameraChunkY;
	}

	public int cameraChunkZ() {
		return lastCameraChunkZ;
	}

	private static int chunkIndex(int x, int z) {
		x = ((x + 30000000) >> 4) & 127;
		z = ((z + 30000000) >> 4) & 127;

		return x | (z << 7);
	}

	public void scheduleRebuild(int x, int y, int z, boolean urgent) {
		final BuiltRenderRegion region = getRegionIfExists(x, y, z);

		if (region != null) {
			region.markForBuild(urgent);
		}
	}

	int chunkDistVersion = 1;

	public void updateCameraDistanceAndVisibilityInfo(long cameraChunkOrigin) {
		final int cameraChunkX = BlockPos.unpackLongX(cameraChunkOrigin) >> 4;
		final int cameraChunkY = BlockPos.unpackLongY(cameraChunkOrigin) >> 4;
		final int cameraChunkZ = BlockPos.unpackLongZ(cameraChunkOrigin) >> 4;

		if (!(cameraChunkX == lastCameraChunkX && cameraChunkY == lastCameraChunkY && cameraChunkZ == lastCameraChunkZ)) {
			lastCameraChunkX = cameraChunkX;
			lastCameraChunkY = cameraChunkY;
			lastCameraChunkZ = cameraChunkZ;
			++chunkDistVersion;
			cameraDistanceSorter.clear();
		} else {
			cameraDistanceSorter.returnToStart();
		}

		occluderVersion = cwr.occluder.version();
		maxSquaredChunkDistance = cwr.occluder.maxSquaredChunkDistance();
		didInvalidateOccluder = false;

		for (int i = 0; i < CHUNK_COUNT; ++i) {
			chunks[i].updateCameraDistanceAndVisibilityInfo();
		}

		if (didInvalidateOccluder) {
			cwr.occluder.invalidate();
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

	public boolean wasSeenFromCamera(int x, int y, int z) {
		final BuiltRenderRegion r = getRegionIfExists(x, y, z);
		return r != null && r.wasRecentlySeenFromCamera();
	}

	public void scheduleClose(RenderRegionChunk chunk) {
		closeQueue.offer(chunk);
	}

	public void closeRegionsOnRenderThread() {
		RenderRegionChunk chunk = closeQueue.poll();

		while (chunk != null) {
			chunk.close();
			chunk = closeQueue.poll();
		}
	}
}
