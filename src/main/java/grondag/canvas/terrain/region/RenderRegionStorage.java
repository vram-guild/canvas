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
import grondag.canvas.shader.data.ShaderDataManager;
import grondag.canvas.terrain.occlusion.CameraPotentiallyVisibleRegionSet;
import grondag.canvas.terrain.occlusion.ShadowOccluder;
import grondag.canvas.terrain.occlusion.ShadowPotentiallyVisibleRegionSet;
import grondag.canvas.terrain.occlusion.TerrainOccluder;

public class RenderRegionStorage {
	/**
	 * Tracks which regions within render distance are potentially visible from the camera
	 * and sorts them from near to far relative to the camera.  Supports terrain iteration
	 * for the camera view.
	 */
	public final CameraPotentiallyVisibleRegionSet cameraPVS = new CameraPotentiallyVisibleRegionSet();

	public final ShadowPotentiallyVisibleRegionSet<RenderRegion> shadowPVS = new ShadowPotentiallyVisibleRegionSet<>(new RenderRegion[RenderRegionIndexer.PADDED_REGION_INDEX_COUNT]);

	private final AtomicInteger loadedRegionCount = new AtomicInteger();

	private int lastCameraChunkX = Integer.MAX_VALUE;
	private int lastCameraChunkY = Integer.MAX_VALUE;
	private int lastCameraChunkZ = Integer.MAX_VALUE;

	public final CanvasWorldRenderer cwr;

	private boolean didInvalidateCameraOccluder = false;
	private boolean didInvalidateShadowOccluder = false;
	private int cameraOcclusionVersion = 0;
	private int maxSquaredCameraChunkDistance;

	private int cameraRegionVersion = 1;

	private final RenderChunk[] chunks = new RenderChunk[RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT];
	private final ArrayBlockingQueue<RenderChunk> closeQueue = new ArrayBlockingQueue<>(RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT);

	public RenderRegionStorage(CanvasWorldRenderer canvasWorldRenderer) {
		cwr = canvasWorldRenderer;

		for (int i = 0; i < RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT; ++i) {
			chunks[i] = new RenderChunk(this);
		}
	}

	public int cameraOcclusionVersion() {
		return cameraOcclusionVersion;
	}

	public void invalidateCameraOccluder() {
		didInvalidateCameraOccluder = true;
	}

	public void invalidateShadowOccluder() {
		didInvalidateShadowOccluder = true;
	}

	public int maxSquaredCameraChunkDistance() {
		return maxSquaredCameraChunkDistance;
	}

	public synchronized void clear() {
		cameraPVS.clear();
		shadowPVS.clear();

		for (final RenderChunk chunk : chunks) {
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
		final RenderRegion region = getRegionIfExists(x, y, z);

		if (region != null) {
			region.markForBuild(urgent);
		}
	}

	public void updateCameraDistanceAndVisibility(long cameraRegionOrigin) {
		final int cameraChunkX = BlockPos.unpackLongX(cameraRegionOrigin) >> 4;
		final int cameraChunkY = BlockPos.unpackLongY(cameraRegionOrigin) >> 4;
		final int cameraChunkZ = BlockPos.unpackLongZ(cameraRegionOrigin) >> 4;

		if (!(cameraChunkX == lastCameraChunkX && cameraChunkY == lastCameraChunkY && cameraChunkZ == lastCameraChunkZ)) {
			lastCameraChunkX = cameraChunkX;
			lastCameraChunkY = cameraChunkY;
			lastCameraChunkZ = cameraChunkZ;
			++cameraRegionVersion;
			cameraPVS.clear();
			cwr.renderRegionStorage.shadowPVS.setCameraChunkOriginAndClear(cameraChunkX, cameraChunkZ);
		} else {
			cameraPVS.returnToStart();
		}

		final TerrainOccluder cameraOccluder = cwr.terrainIterator.cameraOccluder;
		final ShadowOccluder shadowOccluder = cwr.terrainIterator.shadowOccluder;
		shadowOccluder.setLightVector(ShaderDataManager.skyLightVector);
		shadowPVS.setLightVectorAndRestart(ShaderDataManager.skyLightVector);

		cameraOcclusionVersion = cameraOccluder.occlusionVersion();
		maxSquaredCameraChunkDistance = cameraOccluder.maxSquaredChunkDistance();

		for (int i = 0; i < RenderRegionIndexer.PADDED_CHUNK_INDEX_COUNT; ++i) {
			chunks[i].updatePositionAndVisibility();
		}

		if (didInvalidateCameraOccluder) {
			cameraOccluder.invalidate();
			didInvalidateCameraOccluder = false;
		}

		if (didInvalidateShadowOccluder) {
			shadowOccluder.invalidate();
			didInvalidateShadowOccluder = false;
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
		return r != null && r.visibility.wasRecentlySeenFromCamera();
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

	/**
	 * Increments every time the camera moves to a different region.
	 * Non-loadable regions (outside world boundaries) trigger changes
	 * the same as loadable regions.
	 *
	 * <p>Chunk and Region instances track this value to trigger refresh of
	 * computations that depend on which region contains the camera.
	 */
	public int cameraRegionVersion() {
		return cameraRegionVersion;
	}
}
