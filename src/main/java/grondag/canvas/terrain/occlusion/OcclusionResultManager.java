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

import grondag.canvas.render.CanvasWorldRenderer;
import grondag.canvas.shader.data.ShaderDataManager;

public class OcclusionResultManager {
	private final CanvasWorldRenderer cwr;

	private boolean didInvalidateCameraOcclusionResult = false;
	private boolean didInvalidateShadowOcclusionResult = false;
	private int cameraOcclusionResultVersion = 0;
	private int shadowOcclusionResultVersion = 0;
	private int maxSquaredCameraChunkDistance;

	public OcclusionResultManager(CanvasWorldRenderer canvasWorldRenderer) {
		cwr = canvasWorldRenderer;
	}

	/**
	 * The version of the camera occluder in effect when region visibility is being updated.
	 * This is NOT necessarily the version in effect while iteration is being run.
	 * Indeed, if occlusion state is invalidated the version during iteration will not
	 * match, causing regions to be re-tested and redrawn, which is the main point of this process.
	 */
	public int cameraOcclusionResultVersion() {
		return cameraOcclusionResultVersion;
	}

	public void invalidateCameraOcclusionResult() {
		didInvalidateCameraOcclusionResult = true;
	}

	/**
	 * Like {@link #cameraOcclusionResultVersion} but for shadow occluder.
	 */
	public int shadowOcclusionResultVersion() {
		return shadowOcclusionResultVersion;
	}

	public void invalidateShadowOcclusionResult() {
		didInvalidateShadowOcclusionResult = true;
	}

	public int maxSquaredCameraChunkDistance() {
		return maxSquaredCameraChunkDistance;
	}

	public void beforeRegionUpdate() {
		final TerrainOccluder cameraOccluder = cwr.terrainIterator.cameraOccluder;
		final ShadowOccluder shadowOccluder = cwr.terrainIterator.shadowOccluder;
		shadowOccluder.setLightVector(ShaderDataManager.skyLightVector);

		cameraOcclusionResultVersion = cameraOccluder.occlusionVersion();
		shadowOcclusionResultVersion = shadowOccluder.occlusionVersion();
		maxSquaredCameraChunkDistance = cameraOccluder.maxSquaredChunkDistance();
	}

	public void afterRegionUpdate() {
		if (didInvalidateCameraOcclusionResult) {
			cwr.terrainIterator.cameraOccluder.invalidate();
			didInvalidateCameraOcclusionResult = false;
		}

		if (didInvalidateShadowOcclusionResult) {
			cwr.terrainIterator.shadowOccluder.invalidate();
			didInvalidateShadowOcclusionResult = false;
		}
	}
}
