/*
 * Copyright © Original Authors
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

package grondag.canvas.terrain.occlusion.camera;

import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.terrain.occlusion.OcclusionStatus;
import grondag.canvas.terrain.occlusion.base.AbstractRegionVisibility;
import grondag.canvas.terrain.region.RenderRegion;

public class CameraRegionVisibility extends AbstractRegionVisibility<CameraVisibility, CameraRegionVisibility> {
	private int entryFaceFlags;

	public CameraRegionVisibility(CameraVisibility visibility, RenderRegion region) {
		super(visibility, region);
	}

	public void addIfFrontFacing(int fromSquaredDistance) {
		assert Pipeline.advancedTerrainCulling();

		if (region.origin.squaredCameraChunkDistance() >= fromSquaredDistance || region.origin.isNear()) {
			addIfValid();
		}
	}

	public void addIfValid(int entryFaceFlags) {
		if (region.origin.isPotentiallyVisibleFromCamera() && !region.isClosed() && region.isNearOrHasLoadedNeighbors()) {
			addVisitedIfNotPresent(entryFaceFlags);
		}
	}

	public final int entryFaceFlags() {
		assert !Pipeline.advancedTerrainCulling();
		return entryFaceFlags;
	}

	@Override
	public void addIfValid() {
		if (region.origin.isPotentiallyVisibleFromCamera() && !region.isClosed() && region.isNearOrHasLoadedNeighbors()) {
			addVisitedIfNotPresent();
		}
	}

	/**
	 * Adds region to set in sorted position according to implementation.
	 * Requires but does NOT check that region is not already in the set.
	 * Will mark region with result {@link OcclusionStatus#VISITED}.
	 */
	public void addVisitedIfNotPresent(int entryFaceFlags) {
		assert !Pipeline.advancedTerrainCulling();

		final int v = visibility.version();

		if (visibilityVersion != v) {
			visibilityVersion = v;
			occlusionStatus = OcclusionStatus.VISITED;
			visibility.add(this);
			this.entryFaceFlags = entryFaceFlags;
		} else {
			this.entryFaceFlags |= entryFaceFlags;
		}
	}
}
