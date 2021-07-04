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

import java.util.Arrays;

import grondag.canvas.terrain.region.RenderRegion;

public class VisibleRegionList {
	protected RenderRegion[] visibleRegions = new RenderRegion[4096];
	protected volatile int visibleRegionCount = 0;

	public void clear() {
		visibleRegionCount = 0;
		Arrays.fill(visibleRegions, null);
	}

	public final void add(RenderRegion builtRegion) {
		int index = visibleRegionCount++;
		RenderRegion[] visibleRegions = this.visibleRegions;

		if (index >= visibleRegions.length) {
			RenderRegion[] newRegions = new RenderRegion[visibleRegions.length * 2];
			System.arraycopy(visibleRegions, 0, newRegions, 0, visibleRegions.length);
			this.visibleRegions = newRegions;
			visibleRegions = newRegions;
		}

		visibleRegions[index] = builtRegion;
	}

	public final void copyFrom(VisibleRegionList source) {
		final int count = source.visibleRegionCount;
		visibleRegionCount = count;

		if (count > visibleRegions.length) {
			visibleRegions = new RenderRegion[source.visibleRegions.length];
		}

		System.arraycopy(source.visibleRegions, 0, visibleRegions, 0, count);
	}

	public final int size() {
		return visibleRegionCount;
	}

	public final RenderRegion get(int index) {
		return visibleRegions[index];
	}

	public final int getActiveCount() {
		int result = 0;
		final int limit = visibleRegionCount;

		for (int i = 0; i < limit; i++) {
			final RenderRegion region = visibleRegions[i];

			if (!region.solidDrawable().isClosed() || !region.translucentDrawable().isClosed()) {
				++result;
			}
		}

		return result;
	}
}
