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

import net.minecraft.util.math.BlockPos;

import grondag.canvas.shader.data.ShaderDataManager;
import grondag.canvas.terrain.occlusion.CameraPotentiallyVisibleRegionSet;
import grondag.canvas.terrain.occlusion.ShadowPotentiallyVisibleRegionSet;

public class ViewTracker {
	/**
	 * Tracks which regions within render distance are potentially visible from the camera
	 * and sorts them from near to far relative to the camera.  Supports terrain iteration
	 * for the camera view.
	 */
	public final CameraPotentiallyVisibleRegionSet cameraPVS = new CameraPotentiallyVisibleRegionSet();
	public final ShadowPotentiallyVisibleRegionSet<RenderRegion> shadowPVS = new ShadowPotentiallyVisibleRegionSet<>(new RenderRegion[RenderRegionIndexer.PADDED_REGION_INDEX_COUNT]);

	private int lastCameraChunkX = Integer.MAX_VALUE;
	private int lastCameraChunkY = Integer.MAX_VALUE;
	private int lastCameraChunkZ = Integer.MAX_VALUE;

	private int cameraRegionOriginVersion = 1;

	public synchronized void clear() {
		cameraPVS.clear();
		shadowPVS.clear();
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

	/**
	 * Increments every time the camera moves to a different region.
	 * Non-loadable regions (outside world boundaries) trigger changes
	 * the same as loadable regions.
	 *
	 * <p>Chunk and Region instances track this value to trigger refresh of
	 * computations that depend on which region contains the camera.
	 */
	public int cameraRegionOriginVersion() {
		return cameraRegionOriginVersion;
	}

	public void update(long cameraRegionOrigin) {
		final int cameraChunkX = BlockPos.unpackLongX(cameraRegionOrigin) >> 4;
		final int cameraChunkY = BlockPos.unpackLongY(cameraRegionOrigin) >> 4;
		final int cameraChunkZ = BlockPos.unpackLongZ(cameraRegionOrigin) >> 4;

		if (!(cameraChunkX == lastCameraChunkX && cameraChunkY == lastCameraChunkY && cameraChunkZ == lastCameraChunkZ)) {
			lastCameraChunkX = cameraChunkX;
			lastCameraChunkY = cameraChunkY;
			lastCameraChunkZ = cameraChunkZ;
			++cameraRegionOriginVersion;
			cameraPVS.clear();
			shadowPVS.setCameraChunkOriginAndClear(cameraChunkX, cameraChunkZ);
		} else {
			cameraPVS.returnToStart();
		}

		shadowPVS.setLightVectorAndRestart(ShaderDataManager.skyLightVector);
	}
}
