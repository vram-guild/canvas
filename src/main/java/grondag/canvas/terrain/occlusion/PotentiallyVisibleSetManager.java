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

package grondag.canvas.terrain.occlusion;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.shader.data.ShaderDataManager;
import grondag.canvas.terrain.region.RenderRegion;
import grondag.canvas.terrain.region.RenderRegionIndexer;

public class PotentiallyVisibleSetManager {
	/**
	 * Tracks which regions within render distance are potentially visible from the camera
	 * and sorts them from near to far relative to the camera.  Supports terrain iteration
	 * for the camera view.
	 */
	public final CameraPotentiallyVisibleRegionSet cameraPVS = new CameraPotentiallyVisibleRegionSet();
	public final ShadowPotentiallyVisibleRegionSet<RenderRegion> shadowPVS = new ShadowPotentiallyVisibleRegionSet<>(new RenderRegion[RenderRegionIndexer.PADDED_REGION_INDEX_COUNT]);

	private long lastCameraRegionOrigin;

	public synchronized void clear() {
		cameraPVS.clear();
		shadowPVS.clear();
	}

	public void update(long cameraRegionOrigin) {
		if (lastCameraRegionOrigin != cameraRegionOrigin) {
			lastCameraRegionOrigin = cameraRegionOrigin;
			cameraPVS.clear();
			shadowPVS.setCameraChunkOriginAndClear(BlockPos.unpackLongX(cameraRegionOrigin) >> 4, BlockPos.unpackLongZ(cameraRegionOrigin) >> 4);
		} else {
			cameraPVS.returnToStart();
		}

		shadowPVS.setLightVectorAndRestart(ShaderDataManager.skyLightVector);
	}
}
