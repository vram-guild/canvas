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
import grondag.canvas.terrain.occlusion.CameraPotentiallyVisibleRegionSet;
import grondag.canvas.terrain.occlusion.TerrainOccluder;

public class RenderRegionStorage {
	/**
	 * Tracks which regions within render distance are potentially visible from the camera
	 * and sorts them from near to far relative to the camera.  Supports terrain iteration
	 * for the camera view.
	 */
	public final CameraPotentiallyVisibleRegionSet cameraPVS = new CameraPotentiallyVisibleRegionSet();

	private final AtomicInteger loadedRegionCount = new AtomicInteger();

	private int lastCameraChunkX = Integer.MAX_VALUE;
	private int lastCameraChunkY = Integer.MAX_VALUE;
	private int lastCameraChunkZ = Integer.MAX_VALUE;

	final CanvasWorldRenderer cwr;
	final TerrainOccluder cameraOccluder;

	private boolean didInvalidateCameraOccluder = false;
	private int cameraOccluderVersion = 0;
	private int maxSquaredCameraChunkDistance;

	private final RenderRegionChunk[] chunks = new RenderRegionChunk[RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT];
	private final ArrayBlockingQueue<RenderRegionChunk> closeQueue = new ArrayBlockingQueue<>(RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT);

	public RenderRegionStorage(CanvasWorldRenderer canvasWorldRenderer) {
		cwr = canvasWorldRenderer;
		cameraOccluder = cwr.terrainIterator.cameraOccluder;

		for (int i = 0; i < RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT; ++i) {
			chunks[i] = new RenderRegionChunk(this);
		}
	}

	public int cameraOccluderVersion() {
		return cameraOccluderVersion;
	}

	public void invalidateCameraOccluder() {
		didInvalidateCameraOccluder = true;
	}

	public int maxSquaredCameraChunkDistance() {
		return maxSquaredCameraChunkDistance;
	}

	public synchronized void clear() {
		cameraPVS.clear();

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

	public void scheduleRebuild(int x, int y, int z, boolean urgent) {
		final BuiltRenderRegion region = getRegionIfExists(x, y, z);

		if (region != null) {
			region.markForBuild(urgent);
		}
	}

	int cameraChunkDistVersion = 1;

	public void updateCameraDistanceAndVisibility(long cameraChunkOrigin) {
		final int cameraChunkX = BlockPos.unpackLongX(cameraChunkOrigin) >> 4;
		final int cameraChunkY = BlockPos.unpackLongY(cameraChunkOrigin) >> 4;
		final int cameraChunkZ = BlockPos.unpackLongZ(cameraChunkOrigin) >> 4;

		if (!(cameraChunkX == lastCameraChunkX && cameraChunkY == lastCameraChunkY && cameraChunkZ == lastCameraChunkZ)) {
			lastCameraChunkX = cameraChunkX;
			lastCameraChunkY = cameraChunkY;
			lastCameraChunkZ = cameraChunkZ;
			++cameraChunkDistVersion;
			cameraPVS.clear();
		} else {
			cameraPVS.returnToStart();
		}

		cameraOccluderVersion = cameraOccluder.version();
		maxSquaredCameraChunkDistance = cameraOccluder.maxSquaredChunkDistance();
		didInvalidateCameraOccluder = false;

		for (int i = 0; i < RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT; ++i) {
			chunks[i].updateCameraDistanceAndVisibilityInfo();
		}

		if (didInvalidateCameraOccluder) {
			cameraOccluder.invalidate();
		}
	}

	public int loadedRegionCount() {
		return loadedRegionCount.get();
	}

	public BuiltRenderRegion getOrCreateRegion(int x, int y, int z) {
		return chunks[RenderRegionIndexer.chunkIndex(x, z)].getOrCreateRegion(x, y, z);
	}

	public BuiltRenderRegion getOrCreateRegion(BlockPos pos) {
		return getOrCreateRegion(pos.getX(), pos.getY(), pos.getZ());
	}

	public BuiltRenderRegion getRegionIfExists(BlockPos pos) {
		return getRegionIfExists(pos.getX(), pos.getY(), pos.getZ());
	}

	public BuiltRenderRegion getRegionIfExists(int x, int y, int z) {
		return chunks[RenderRegionIndexer.chunkIndex(x, z)].getRegionIfExists(x, y, z);
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

	void trackRegionClosed() {
		loadedRegionCount.decrementAndGet();
	}

	void trackRegionLoaded() {
		loadedRegionCount.incrementAndGet();
	}
}
