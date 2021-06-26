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
import grondag.canvas.terrain.occlusion.OcclusionInputManager;
import grondag.canvas.terrain.occlusion.OcclusionResultManager;
import grondag.canvas.terrain.occlusion.ShadowPotentiallyVisibleRegionSet;

public class RegionOcclusionState {
	/** Region for which we track visibility information. Provides access to world render state. */
	private final RenderRegion owner;

	// Here to save some pointer chases
	private final CameraPotentiallyVisibleRegionSet cameraPVS;
	private final ShadowPotentiallyVisibleRegionSet<RenderRegion> shadowPVS;
	private final OcclusionInputManager occlusionInputStatus;

	/** Incremented when occlusion data changes (including first time built). */
	private int regionOcclusionInputVersion = -1;

	/**
	 * Version of the camera occluder when this region was last drawn.
	 * If it matches the current camera occluder version, then the
	 * occluder state depends on this region's occlusion data.
	 */
	private int cameraOcclusionResultVersion;

	private boolean cameraOccluderResult;

	private int lastSeenCameraPvsVersion;

	/**
	 * Occlusion data version that was in effect last time drawn to camera occluder.
	 * If this does not match the current data version and the camera occluder has not
	 * been reset since it was last drawn (meaning this region is still in it)
	 * then the camera occluder state is invalid.
	 */
	private int cameraOcclusionInputVersion;

	/**
	 * Version of the shadow occluder when this region was last drawn.
	 * If it matches the current shadow occluder version, then the
	 * occluder state depends on this region's occlusion data.
	 */
	private int shadowOccluderResultVersion;

	private boolean shadowOccluderResult;

	private int lastSeenShadowPvsVersion;

	/**
	 * Occlusion data version that was in effect last time drawn to shadow occluder.
	 * If this does not match the current data version and the shadow occluder has not
	 * been reset since it was last drawn (meaning this region is still in it)
	 * then the shadow occluder state is invalid.
	 */
	private int shadowOcclusionInputVersion;

	public RegionOcclusionState(RenderRegion owner) {
		this.owner = owner;
		cameraPVS = owner.cwr.potentiallyVisibleSetManager.cameraPVS;
		shadowPVS = owner.cwr.potentiallyVisibleSetManager.shadowPVS;
		occlusionInputStatus = owner.cwr.occlusionInputStatus;
	}

	public void setCameraOccluderResult(boolean occluderResult, int occluderResultVersion) {
		if (cameraOcclusionResultVersion == occluderResultVersion) {
			assert occluderResult == cameraOccluderResult;
		} else {
			cameraOccluderResult = occluderResult;
			cameraOcclusionResultVersion = occluderResultVersion;
			cameraOcclusionInputVersion = regionOcclusionInputVersion;
		}
	}

	/**
	 * Cached result of last camera occlusion test for this region.
	 * Only valid if the camera occluder has not been reset since,
	 * which must be confirmed using {@link #isCameraOcclusionResultCurrent(int)}.
	 */
	public boolean cameraOccluderResult() {
		return cameraOccluderResult;
	}

	/**
	 * True if the given occluder version matches the last call to {@link #setCameraOccluderResult(boolean, int)}
	 * which means {@link #cameraOccluderResult} is still valid.
	 */
	public boolean isCameraOcclusionResultCurrent(int occluderResultVersion) {
		return cameraOcclusionResultVersion == occluderResultVersion;
	}

	public void addToCameraPvsIfValid() {
		// Previously checked for r.squaredChunkDistance > squaredChunkDistance
		// but some progression patterns seem to require it or chunks are missed.
		// This is probably because a nearer path has an occlude chunk and so it
		// has to be found reaching around. This will cause some backtracking and
		// thus redraw of the occluder, but that already happens and is handled.

		final int pvsVersion = cameraPVS.version();

		if (lastSeenCameraPvsVersion != pvsVersion) {
			lastSeenCameraPvsVersion = pvsVersion;
			cameraPVS.add(owner);
		}
	}

	/**
	 * Same as {@link #setCameraOccluderResult(boolean, int)} except for shadow occlusion.
	 */
	public void setShadowOccluderResult(boolean occluderResult, int occluderResultVersion) {
		if (shadowOccluderResultVersion == occluderResultVersion) {
			assert occluderResult == shadowOccluderResult;
		} else {
			shadowOccluderResult = occluderResult;
			shadowOccluderResultVersion = occluderResultVersion;
			shadowOcclusionInputVersion = regionOcclusionInputVersion;
		}
	}

	/**
	 * Same as {@link #cameraOccluderResult} except for shadow occlusion.
	 */
	public boolean shadowOccluderResult() {
		return shadowOccluderResult;
	}

	/**
	 * Same as {@link #isCameraOcclusionResultCurrent(int)} except for shadow occlusion.
	 */
	public boolean isShadowOcclusionResultCurrent(int occluderResultVersion) {
		return shadowOccluderResultVersion == occluderResultVersion;
	}

	public void addToShadowPvsIfValid() {
		final int pvsVersion = shadowPVS.version();

		if (lastSeenShadowPvsVersion != pvsVersion) {
			lastSeenShadowPvsVersion = pvsVersion;
			shadowPVS.add(owner);
		}
	}

	/**
	 * We check here to know if any inputs affecting occlusion raster(s)
	 * have changed and invalidate them as needed.
	 *
	 * <p>This runs outside of and before terrain iteration so
	 * the occluder versions used for comparison are "live."
	 */
	private void invalidateOcclusionResultIfNeeded() {
		final RegionPosition origin = owner.origin;
		final OcclusionResultManager occlusionResultManager = owner.cwr.occlusionStateManager;

		// Check camera occluder
		if (cameraOcclusionResultVersion == occlusionResultManager.cameraOcclusionResultVersion()) {
			// Existing - has been drawn in occlusion raster

			// We don't need to check if region is still in view here because
			// a view change will invalidate the occluder on its own.

			// We also don't check if the region can occlude here because it
			// may have been an occluder previously and empty now.

			// The only check needed here is for a change in the region occlusion used for the draw.
			if (regionOcclusionInputVersion != cameraOcclusionInputVersion) {
				occlusionResultManager.invalidateCameraOcclusionResult();
			}
		} else if (origin.squaredCameraChunkDistance() < occlusionResultManager.maxSquaredCameraChunkDistance()) {
			// Not yet drawn in current occlusion raster and could be nearer than a chunk that has been.

			// Need to invalidate the occlusion raster if both things are true:
			//   1) This region isn't empty (empty regions don't matter for culling)
			//   2) This region is in the view frustum
			if (origin.isPotentiallyVisibleFromCamera() && owner.getBuildState().canOcclude()) {
				occlusionResultManager.invalidateCameraOcclusionResult();
			}
		}

		// Check shadow occluder
		if (shadowOccluderResultVersion == occlusionResultManager.shadowOcclusionResultVersion()) {
			// Existing - has been drawn in occlusion raster

			// We don't need to check if region is still in view here because
			// a view change will invalidate the occluder on its own.

			// We also don't check if the region can occlude here because it
			// may have been an occluder previously and empty now.

			// The only check needed here is for a change in the region occlusion used for the draw.
			if (regionOcclusionInputVersion != shadowOcclusionInputVersion) {
				occlusionResultManager.invalidateShadowOcclusionResult();
			}
		// WIP: implement correct test and only invalidate if could be nearer than a chunk already drawn
		} else if (origin.isPotentiallyVisibleFromSkylight()) {
			// Not yet drawn in current occlusion raster and could be nearer than a chunk that has been.

			// Need to invalidate the occlusion raster if both things are true:
			//   1) This region isn't empty (empty regions don't matter for culling)
			//   2) This region is in the view frustum
			if (owner.getBuildState().canOcclude()) {
				occlusionResultManager.invalidateShadowOcclusionResult();
			}
		}
	}

	public boolean wasRecentlySeenFromCamera() {
		return cameraPVS.version() - lastSeenCameraPvsVersion < 4 && cameraOccluderResult;
	}

	/**
	 * Handles occluder invalidation and shadow cascade classification.
	 * Called when new regions are created and at the start of terrain iteration.
	 *
	 * <p>NB: PVS invalidation can't be done here because PVS invalidation is
	 * what triggers terrain iteration.
	 */
	public void update() {
		invalidateOcclusionResultIfNeeded();
	}

	boolean isInCurrentPVS() {
		return lastSeenCameraPvsVersion == cameraPVS.version() || ((Pipeline.shadowsEnabled() && lastSeenShadowPvsVersion == shadowPVS.version()));
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
	void notifyOfOcclusionChange() {
		++regionOcclusionInputVersion;

		// use pvs versions for this test, not occluder versions
		int flags = OcclusionInputManager.CURRENT;

		if (lastSeenCameraPvsVersion == cameraPVS.version()) {
			flags |= OcclusionInputManager.CAMERA_INVALID;
		}

		if (Pipeline.shadowsEnabled() && lastSeenShadowPvsVersion == shadowPVS.version()) {
			flags |= OcclusionInputManager.SHADOW_INVALID;
		}

		if (flags != OcclusionInputManager.CURRENT) {
			occlusionInputStatus.invalidateOcclusionInputs(flags);
		}
	}
}
