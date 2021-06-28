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

import grondag.canvas.render.world.WorldRenderState;

public class OcclusionResultManager {
	private final WorldRenderState worldRenderState;

	private boolean didInvalidateCameraOcclusionResult = false;
	private boolean didInvalidateShadowOcclusionResult = false;

	public OcclusionResultManager(WorldRenderState worldRenderState) {
		this.worldRenderState = worldRenderState;
	}

	public void invalidateCameraOcclusionResult() {
		didInvalidateCameraOcclusionResult = true;
	}

	public void invalidateShadowOcclusionResult() {
		didInvalidateShadowOcclusionResult = true;
	}

	public void afterRegionUpdate() {
		if (didInvalidateCameraOcclusionResult) {
			worldRenderState.terrainIterator.cameraOccluder.invalidate();
			worldRenderState.terrainIterator.targetOccluder.invalidate();
			didInvalidateCameraOcclusionResult = false;
			didInvalidateShadowOcclusionResult = true;
		}

		if (didInvalidateShadowOcclusionResult) {
			worldRenderState.terrainIterator.shadowOccluder.invalidate();
			didInvalidateShadowOcclusionResult = false;
		}
	}
}
