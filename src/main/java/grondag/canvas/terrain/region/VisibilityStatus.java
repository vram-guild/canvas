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

public class VisibilityStatus {
	final RenderRegionStorage storage;

	/**
	 * Incremented whenever regions are built so visibility search can progress or to indicate visibility might be changed.
	 * Distinct from occluder state, which indicates if/when occluder must be reset or redrawn.
	 */
	private final AtomicInteger regionDataVersion = new AtomicInteger();
	private int lastRegionDataVersion = -1;

	public VisibilityStatus(RenderRegionStorage renderRegionStorage) {
		storage = renderRegionStorage;
	}

	public void forceVisibilityUpdate() {
		regionDataVersion.incrementAndGet();
	}

	public boolean isCurrent() {
		return regionDataVersion.get() == lastRegionDataVersion;
	}

	public boolean checkForIterationNeededAndReset() {
		final int newRegionDataVersion = regionDataVersion.get();

		if (lastRegionDataVersion != newRegionDataVersion) {
			lastRegionDataVersion = newRegionDataVersion;
			return true;
		} else {
			return false;
		}
	}
}
