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

package grondag.canvas.terrain.occlusion.shadow;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.render.frustum.TerrainFrustum;
import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.shader.data.ShaderDataManager;
import grondag.canvas.terrain.occlusion.base.AbstractVisbility;
import grondag.canvas.terrain.region.RegionPosition;
import grondag.canvas.terrain.region.RenderRegion;

public class ShadowVisibility extends AbstractVisbility<ShadowVisibility, ShadowRegionVisibility, ShadowPotentiallyVisibleRegionSet, ShadowOccluder> {
	public final ShadowOccluder targetOccluder = new ShadowOccluder("canvas_shadow_target_occlusion_raster.png");

	public ShadowVisibility(WorldRenderState worldRenderState) {
		super(worldRenderState, new ShadowPotentiallyVisibleRegionSet(), new ShadowOccluder("canvas_shadow_occlusion_raster.png"));
	}

	public int cascade(RegionPosition regionPosition) {
		return occluder.cascade(regionPosition);
	}

	public int distanceRank(RenderRegion owner) {
		return pvrs.distanceRank(owner.shadowVisibility);
	}

	@Override
	public ShadowRegionVisibility createRegionState(RenderRegion region) {
		return new ShadowRegionVisibility(this, region);
	}

	@Override
	public void updateView(TerrainFrustum frustum, long cameraRegionOrigin) {
		if (lastCameraRegionOrigin != cameraRegionOrigin) {
			pvrs.setCameraChunkOriginAndClear(BlockPos.unpackLongX(cameraRegionOrigin), BlockPos.unpackLongZ(cameraRegionOrigin));
		}

		pvrs.setLightVectorAndRestart(ShaderDataManager.skyLightVector);

		occluder.copyState(frustum);
		occluder.setLightVector(ShaderDataManager.skyLightVector);
		targetOccluder.copyState(frustum);
		targetOccluder.setLightVector(ShaderDataManager.skyLightVector);

		super.updateView(frustum, cameraRegionOrigin);
	}

	@Override
	public void outputRaster() {
		super.outputRaster();
		targetOccluder.outputRaster();
	}

	@Override
	protected void invalidateOccluder() {
		super.invalidateOccluder();
		targetOccluder.invalidate();
	}

	@Override
	protected boolean prepareOccluder() {
		return super.prepareOccluder();
	}

	@Override
	public void prepareRegion(RegionPosition origin) {
		occluder.prepareRegion(origin);
		targetOccluder.prepareRegion(origin);
	}

	@Override
	public boolean isBoxVisible(int packedBox, int fuzz) {
		// If can't shadow any terrain then consider it invisible
		return targetOccluder.isBoxOccluded(packedBox) && occluder.isBoxVisible(packedBox, fuzz);
	}

	@Override
	public void occlude(int[] occlusionData) {
		occluder.occlude(occlusionData);
	}
}
