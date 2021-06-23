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

import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.terrain.occlusion.CameraPotentiallyVisibleRegionSet;
import grondag.canvas.terrain.occlusion.ShadowPotentiallyVisibleRegionSet;

public class RegionVisibility {
	/** Region for which we track visibility. Provides access to world render state. */
	private final RenderRegion owner;

	public RegionVisibility(RenderRegion owner) {
		this.owner = owner;
	}

	private int cameraOcclusionVersion;
	private int lastSeenCameraPvsVersion;
	private boolean cameraOccluderResult;

	/** Build version that was in effect last time drawn to occluder. */
	private int cameraOcclusionBuildVersion;

	private int shadowOccluderVersion;
	private int lastSeenShadowPvsVersion;
	private boolean shadowOccluderResult;
	private int shadowOcclusionBuildVersion;

	/** Concatenated bit flags marking the shadow cascades that include this region. */
	private int shadowCascadeFlags;

	public void setCameraOccluderResult(boolean occluderResult, int occluderVersion) {
		if (cameraOcclusionVersion == occluderVersion) {
			assert occluderResult == cameraOccluderResult;
		} else {
			cameraOccluderResult = occluderResult;
			cameraOcclusionVersion = occluderVersion;
			cameraOcclusionBuildVersion = owner.buildVersion();
		}
	}

	public boolean cameraOccluderResult() {
		return cameraOccluderResult;
	}

	public boolean matchesCameraOccluderVersion(int occluderVersion) {
		return cameraOcclusionVersion == occluderVersion;
	}

	public void addToCameraPvsIfValid() {
		// Previously checked for r.squaredChunkDistance > squaredChunkDistance
		// but some progression patterns seem to require it or chunks are missed.
		// This is probably because a nearer path has an occlude chunk and so it
		// has to be found reaching around. This will cause some backtracking and
		// thus redraw of the occluder, but that already happens and is handled.

		final CameraPotentiallyVisibleRegionSet cameraPVS = owner.storage.cameraPVS;
		final int pvsVersion = cameraPVS.version();

		// The frustum version check is necessary to skip regions without valid info.
		// WIP: is frustum version check still correct/needed?
		if (lastSeenCameraPvsVersion != pvsVersion && owner.origin.hasValidFrustumVersion()) {
			lastSeenCameraPvsVersion = pvsVersion;
			cameraPVS.add(owner);
		}
	}

	public void setShadowOccluderResult(boolean occluderResult, int occluderVersion) {
		if (shadowOccluderVersion == occluderVersion) {
			assert occluderResult == shadowOccluderResult;
		} else {
			shadowOccluderResult = occluderResult;
			shadowOccluderVersion = occluderVersion;
			shadowOcclusionBuildVersion = owner.buildVersion();
		}
	}

	public boolean shadowOccluderResult() {
		return shadowOccluderResult;
	}

	public boolean matchesShadowOccluderVersion(int occluderVersion) {
		return shadowOccluderVersion == occluderVersion;
	}

	public int shadowCascadeFlags() {
		return shadowCascadeFlags;
	}

	public boolean isPotentiallyVisibleFromSkylight() {
		return owner.origin.isInsideRenderDistance() & shadowCascadeFlags != 0;
	}

	public void addToShadowPvsIfValid() {
		final ShadowPotentiallyVisibleRegionSet<RenderRegion> shadowPVS = owner.storage.shadowPVS;
		final int pvsVersion = shadowPVS.version();

		// The frustum version check is necessary to skip regions without valid info.
		// WIP: is frustum version check still correct/needed?
		if (lastSeenShadowPvsVersion != pvsVersion && owner.origin.hasValidFrustumVersion()) {
			lastSeenShadowPvsVersion = pvsVersion;
			shadowPVS.add(owner);
		}
	}

	/**
	 * We check here to know if the occlusion raster must be redrawn.
	 *
	 * <p>The check depends on classifying this region as one of:<ul>
	 *   <li>new - has not been drawn in raster - occluder version doesn't match
	 *   <li>existing - has been drawn in rater - occluder version matches</ul>
	 *
	 * <p>The raster must be redrawn if either is true:<ul>
	 *   <li>A new chunk has a chunk distance less than the current max drawn (we somehow went backwards towards the camera)
	 *   <li>An existing chunk has been reloaded - the buildCounter doesn't match the buildCounter when it was marked existing</ul>
	 */
	private void invalidateCameraOccluderIfNeeded() {
		// WIP track shadow invalidation separately - may change without camera movement
		final RegionPosition origin = owner.origin;
		final RenderRegionStorage storage = owner.storage;

		// WIP: second part of this check seems incorect - if could occlude before and now it can't then a redraw may be needed
		if (origin.isPotentiallyVisibleFromCamera() && owner.getBuildState().canOcclude()) {
			if (cameraOcclusionVersion == storage.cameraOcclusionVersion()) {
				// Existing - has been drawn in occlusion raster
				if (owner.buildVersion() != cameraOcclusionBuildVersion) {
					storage.invalidateCameraOccluder();
					storage.invalidateShadowOccluder();
				}
			} else if (origin.squaredCameraChunkDistance() < storage.maxSquaredCameraChunkDistance()) {
				// Not yet drawn in current occlusion raster and could be nearer than a chunk that has been
				// Need to invalidate the occlusion raster if both things are true:
				//   1) This region isn't empty (empty regions don't matter for culling)
				//   2) This region is in the view frustum
				storage.invalidateCameraOccluder();
			}
		}
	}

	public boolean wasRecentlySeenFromCamera() {
		return owner.storage.cameraPVS.version() - lastSeenCameraPvsVersion < 4 && cameraOccluderResult;
	}

	public void update() {
		invalidateCameraOccluderIfNeeded();
		shadowCascadeFlags = Pipeline.shadowsEnabled() ? owner.cwr.terrainIterator.shadowOccluder.cascadeFlags(owner.origin) : 0;
	}

	/**
	 * Called when this region's occlusion data has changed (or is newly available)
	 * and marks camera iteration and/or shadow iteration for refresh if this region
	 * was part of the most recent visibility iteration.
	 */
	void notifyOfOcclusionChange() {
		// WIP: actually check version and track camera and shadow visibility rebuilt separately
		// use pvs versions for this test, not occluder versions
		owner.cwr.forceVisibilityUpdate();
	}
}
