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

import java.util.concurrent.atomic.AtomicInteger;

import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.render.CanvasWorldRenderer;

public class OcclusionInputManager {
	public static final int CURRENT = 0;
	public static final int CAMERA_INVALID = 1;
	public static final int SHADOW_INVALID = 2;
	public static final int BOTH_INVALID = CAMERA_INVALID | SHADOW_INVALID;

	private final CanvasWorldRenderer cwr;

	/**
	 * Incremented whenever regions are built so visibility search can progress or to indicate visibility might be changed.
	 * Distinct from occluder state, which indicates if/when occluder must be reset or redrawn.
	 */
	private final AtomicInteger cameraInputVersion = new AtomicInteger();
	private int lastCameraInputVersion = -1;

	private final AtomicInteger shadowInputVersion = new AtomicInteger();
	private int lastShadowInputVersion = -1;

	private int lastViewVersion = -1;

	public OcclusionInputManager(CanvasWorldRenderer cwr) {
		this.cwr = cwr;
	}

	public void invalidateOcclusionInputs(int flags) {
		if ((flags & CAMERA_INVALID) == CAMERA_INVALID) {
			cameraInputVersion.incrementAndGet();
		}

		if ((flags & SHADOW_INVALID) == SHADOW_INVALID) {
			shadowInputVersion.incrementAndGet();
		}
	}

	public boolean isCurrent() {
		return cameraInputVersion.get() == lastCameraInputVersion && (!Pipeline.shadowsEnabled() || shadowInputVersion.get() == lastShadowInputVersion);
	}

	public int getAndClearStatus() {
		int result = CURRENT;

		final int newRegionDataVersion = cameraInputVersion.get();

		if (lastCameraInputVersion != newRegionDataVersion) {
			lastCameraInputVersion = newRegionDataVersion;
			result |= CAMERA_INVALID;
		}

		if (Pipeline.shadowsEnabled()) {
			final int newShadowDataVersion = shadowInputVersion.get();

			if (lastShadowInputVersion != newShadowDataVersion) {
				lastShadowInputVersion = newShadowDataVersion;
				result |= SHADOW_INVALID;
			}
		}

		final int newViewVersion = cwr.terrainFrustum.viewVersion();

		if (lastViewVersion != newViewVersion) {
			lastViewVersion = newViewVersion;
			result = BOTH_INVALID;
		}

		return result;
	}
}
