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

package grondag.canvas.terrain.occlusion.base;

import grondag.canvas.terrain.occlusion.OcclusionResult;
import grondag.canvas.terrain.region.RenderRegion;

public abstract class AbstractRegionVisibility<T extends AbstractVisbility<T, U, ?, ?>, U extends AbstractRegionVisibility<T, U>> {
	public final RenderRegion region;
	private final T visibility;
	private int visibilityVersion;
	private OcclusionResult result = OcclusionResult.UNDETERMINED;

	public AbstractRegionVisibility(T visbility, RenderRegion region) {
		this.visibility = visbility;
		this.region = region;
	}

	public OcclusionResult getResult() {
		return visibilityVersion == visibility.version() ? result : OcclusionResult.UNDETERMINED;
	}

	/**
	 * Should not be used to mark visited or undetermined.
	 */
	public void setResult(OcclusionResult result) {
		assert result != OcclusionResult.VISITED;
		assert result != OcclusionResult.UNDETERMINED;

		this.result = result;
		visibilityVersion = visibility.version();
	}

	/**
	 * Adds region to set in sorted position according to implementation.
	 * Requires but does NOT check that region is not already in the set.
	 * Will mark region with result {@link OcclusionResult#VISITED}.
	 */
	@SuppressWarnings("unchecked")
	public void addVisitedIfNotPresent() {
		final int v = visibility.version();

		if (visibilityVersion != v) {
			visibilityVersion = v;
			result = OcclusionResult.VISITED;
			visibility.add((U) this);
		}
	}

	public boolean wasRecentlySeen() {
		return visibility.version() - visibilityVersion < 4 && result != OcclusionResult.REGION_NOT_VISIBLE;
	}

	/**
	 * Called when this region's occlusion data has changed (or is newly available)
	 * and marks camera iteration and/or shadow iteration for refresh if this region
	 * was part of the most recent visibility iteration.
	 *
	 * <p>Will also trigger invalidation of camera or shadow occlusion raster if
	 * this region was drawn in the current version(s) AND iteration has progressed
	 * past this region's distance/tier.
	 *
	 * <p>Not thread-safe, but can be called from threads in a pool so long as none of
	 * them try to call for the same region.
	 */
	public void notifyOfOcclusionChange() {
		if (getResult() != OcclusionResult.UNDETERMINED) {
			visibility.invalidate();
		}
	}

	public abstract void addIfValid();
}
