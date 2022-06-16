/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.terrain.occlusion.camera;

import net.minecraft.world.phys.Vec3;

import io.vram.frex.api.config.FlawlessFrames;

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

	@Override
	protected void invalidateOccluder() {
		super.invalidateOccluder();
		// the frustum needs to be invalidated too FOR SOME REASON otherwise the missing regions still won't render
		occluder.invalidateFrustum();
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
