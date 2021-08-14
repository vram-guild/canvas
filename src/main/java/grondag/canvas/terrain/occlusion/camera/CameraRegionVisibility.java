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

package grondag.canvas.terrain.occlusion.camera;

import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.terrain.occlusion.base.AbstractRegionVisibility;
import grondag.canvas.terrain.region.RenderRegion;

public class CameraRegionVisibility extends AbstractRegionVisibility<CameraVisibility, CameraRegionVisibility> {
	public CameraRegionVisibility(CameraVisibility visibility, RenderRegion region) {
		super(visibility, region);
	}

	public void addIfFrontFacing(int fromSquaredDistance) {
		assert Pipeline.advancedTerrainCulling();

		if (region.origin.squaredCameraChunkDistance() >= fromSquaredDistance || region.origin.isNear()) {
			addIfValid();
		}
	}

	@Override
	public void addIfValid(int entryFaceFlags) {
		if (region.origin.isPotentiallyVisibleFromCamera() && !region.isClosed() && region.isNearOrHasLoadedNeighbors()) {
			addVisitedIfNotPresent(entryFaceFlags);
		}
	}

	@Override
	public void addIfValid() {
		if (region.origin.isPotentiallyVisibleFromCamera() && !region.isClosed() && region.isNearOrHasLoadedNeighbors()) {
			addVisitedIfNotPresent();
		}
	}
}
