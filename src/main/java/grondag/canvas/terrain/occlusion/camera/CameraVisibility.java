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

package grondag.canvas.terrain.occlusion.camera;

import io.vram.frex.api.config.FlawlessFrames;

import net.minecraft.world.phys.Vec3;

import grondag.canvas.config.Configurator;
import grondag.canvas.render.frustum.TerrainFrustum;
import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.terrain.occlusion.base.AbstractVisbility;
import grondag.canvas.terrain.occlusion.geometry.RegionOcclusionCalculator;
import grondag.canvas.terrain.occlusion.shadow.ShadowOccluder;
import grondag.canvas.terrain.region.RegionPosition;
import grondag.canvas.terrain.region.RenderRegion;

public class CameraVisibility extends AbstractVisbility<CameraVisibility, CameraRegionVisibility, CameraPotentiallyVisibleRegionSet, CameraOccluder> {
	private final ShadowOccluder targetOccluder;

	/** Stashed during prepare in case we need it to draw to target occluder. */
	private RegionPosition lastOrigin;

	public CameraVisibility(WorldRenderState worldRenderState, ShadowOccluder targetOccluder) {
		super(worldRenderState, new CameraPotentiallyVisibleRegionSet(), new CameraOccluder());
		this.targetOccluder = targetOccluder;
	}

	@Override
	public CameraRegionVisibility createRegionState(RenderRegion region) {
		return new CameraRegionVisibility(this, region);
	}

	public int frustumViewVersion() {
		return occluder.frustumViewVersion();
	}

	public int frustumPositionVersion() {
		return occluder.frustumPositionVersion();
	}

	public Vec3 frustumCameraPos() {
		return occluder.frustumCameraPos();
	}

	public boolean hasNearOccluders() {
		return occluder.hasNearOccluders();
	}

	@Override
	public void updateView(TerrainFrustum frustum, long cameraRegionOrigin) {
		occluder.copyFrustum(frustum);

		// Player can elect not to occlude near regions to prevent transient gaps
		occluder.drawNearOccluders(Configurator.enableNearOccluders && !FlawlessFrames.isActive());

		super.updateView(frustum, cameraRegionOrigin);
	}

	@Override
	public void prepareRegion(RegionPosition origin) {
		// Check for backtracking and invalidate if we detect it.
		// Will force redraw on the next pass.
		if (!shouldInvalidateNextPass && origin.squaredCameraChunkDistance() < occluder.maxSquaredChunkDistance()) {
			//System.out.println("invalidate camera occlusion due to backtrack from " + occluder.maxSquaredChunkDistance() + " to " + origin.squaredCameraChunkDistance() + " with origin " + origin.toShortString());
			shouldInvalidateNextPass = true;
		}

		lastOrigin = origin;

		occluder.prepareRegion(origin);
	}

	@Override
	public boolean isBoxVisible(int packedBox, int fuzz) {
		return occluder.isBoxVisible(packedBox, fuzz);
	}

	@Override
	public void occlude(int[] occlusionData) {
		// Note some occluders may not be drawn if near occluders are disabled.
		occluder.occlude(occlusionData);

		if (worldRenderState.shadowsEnabled()) {
			targetOccluder.prepareRegion(lastOrigin);
			targetOccluder.occludeBox(occlusionData[RegionOcclusionCalculator.OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX]);
		}
	}
}
