/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.terrain.occlusion.base;

import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.terrain.occlusion.OcclusionStatus;
import grondag.canvas.terrain.region.RenderRegion;

public abstract class AbstractRegionVisibility<T extends AbstractVisbility<T, U, ?, ?>, U extends AbstractRegionVisibility<T, U>> {
	public final RenderRegion region;
	protected final T visibility;
	protected int visibilityVersion;
	protected OcclusionStatus occlusionStatus = OcclusionStatus.UNDETERMINED;

	public AbstractRegionVisibility(T visbility, RenderRegion region) {
		this.visibility = visbility;
		this.region = region;
	}

	public OcclusionStatus getOcclusionStatus() {
		return visibilityVersion == visibility.version() ? occlusionStatus : OcclusionStatus.UNDETERMINED;
	}

	/**
	 * Should not be used to mark visited or undetermined.
	 */
	public void setOcclusionStatus(OcclusionStatus occlusionStatus) {
		assert occlusionStatus != OcclusionStatus.VISITED;
		assert occlusionStatus != OcclusionStatus.UNDETERMINED;

		this.occlusionStatus = occlusionStatus;
		visibilityVersion = visibility.version();
	}

	@SuppressWarnings("unchecked")
	public void addVisitedIfNotPresent() {
		assert Pipeline.advancedTerrainCulling();

		final int v = visibility.version();

		if (visibilityVersion != v) {
			visibilityVersion = v;
			occlusionStatus = OcclusionStatus.VISITED;
			visibility.add((U) this);
		}
	}

	/** Used for entity culling so needs to error on the side of caution. */
	public boolean isPotentiallyVisible() {
		return visibility.version() != visibilityVersion || occlusionStatus != OcclusionStatus.REGION_NOT_VISIBLE;
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
		if (getOcclusionStatus() != OcclusionStatus.UNDETERMINED) {
			visibility.invalidate();
		}
	}

	public abstract void addIfValid();
}
