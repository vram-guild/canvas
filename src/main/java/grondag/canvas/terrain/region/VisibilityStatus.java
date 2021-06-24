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

import java.util.concurrent.atomic.AtomicInteger;

import grondag.canvas.pipeline.Pipeline;

public class VisibilityStatus {
	public static final int CURRENT = 0;
	public static final int CAMERA_INVALID = 1;
	public static final int SHADOW_INVALID = 2;
	public static final int BOTH_INVALID = CAMERA_INVALID | SHADOW_INVALID;

	final RenderRegionStorage storage;

	/**
	 * Incremented whenever regions are built so visibility search can progress or to indicate visibility might be changed.
	 * Distinct from occluder state, which indicates if/when occluder must be reset or redrawn.
	 */
	private final AtomicInteger cameraDataVersion = new AtomicInteger();
	private int lastCameraDataVersion = -1;

	private final AtomicInteger shadowDataVersion = new AtomicInteger();
	private int lastShadowDataVersion = -1;

	private int lastViewVersion = -1;

	public VisibilityStatus(RenderRegionStorage renderRegionStorage) {
		storage = renderRegionStorage;
	}

	public void invalidateOcclusionData(int flags) {
		if ((flags & CAMERA_INVALID) == CAMERA_INVALID) {
			cameraDataVersion.incrementAndGet();
		}

		if ((flags & SHADOW_INVALID) == SHADOW_INVALID) {
			shadowDataVersion.incrementAndGet();
		}
	}

	public boolean isCurrent() {
		return cameraDataVersion.get() == lastCameraDataVersion;
	}

	public int getAndClearStatus() {
		int result = CURRENT;

		final int newRegionDataVersion = cameraDataVersion.get();

		if (lastCameraDataVersion != newRegionDataVersion) {
			lastCameraDataVersion = newRegionDataVersion;
			result |= CAMERA_INVALID;
		}

		if (Pipeline.shadowsEnabled()) {
			final int newShadowDataVersion = shadowDataVersion.get();

			if (lastShadowDataVersion != newShadowDataVersion) {
				lastShadowDataVersion = newShadowDataVersion;
				result |= SHADOW_INVALID;
			}
		}

		final int newViewVersion = storage.cwr.terrainFrustum.viewVersion();

		if (lastViewVersion != newViewVersion) {
			lastViewVersion = newViewVersion;
			result = BOTH_INVALID;
		}

		return result;
	}
}
