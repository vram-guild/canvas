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

package grondag.canvas.render.region.vs;

import org.lwjgl.opengl.GL21;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.buffer.encoding.ArrayVertexCollector.QuadDistanceFunc;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.render.region.UploadableRegion;
import grondag.canvas.render.region.base.RegionRenderConfig;
import grondag.canvas.render.world.CanvasWorldRenderer;
import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.terrain.region.RegionPosition;
import grondag.canvas.terrain.region.RenderRegion;
import grondag.canvas.texture.TextureData;
import grondag.frex.api.material.UniformRefreshFrequency;

public class MultiClumpedRegionRenderConfig extends RegionRenderConfig {
	public static final MultiClumpedRegionRenderConfig INSTANCE = new MultiClumpedRegionRenderConfig();

	private MultiClumpedRegionRenderConfig() {
		super(
			"MULTICLUMPED",
			"REGION",
			VsFormat.VS_MATERIAL,
			VsFormat.VS_MATERIAL.quadStrideInts,
			true,
			VsFormat.VS_TRANSCODER,
			MultiClumpedDrawList::build
		);
	}

	@Override
	public void setupUniforms(GlProgram program) {
		program.uniformSampler("isamplerBuffer", "_cvu_vfRegions", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_REGIONS - GL21.GL_TEXTURE0));
		program.uniformArrayi("_cvu_sectors_int", UniformRefreshFrequency.PER_FRAME, u -> u.set(CanvasWorldRenderer.instance().worldRenderState.sectorManager.uniformData()), RegionSectorMap.UNIFORM_ARRAY_LENGTH);
	}

	@Override
	public void reload(WorldRenderState worldRenderState) {
		VsFormat.REGION_LOOKUP.clear();
		ClumpedVertexStorage.SOLID.clear();
		ClumpedVertexStorage.TRANSLUCENT.clear();
	}

	@Override
	public void onActivateProgram() {
		VsFormat.REGION_LOOKUP.enableTexture();
	}

	@Override
	public void onDeactiveProgram() {
		VsFormat.REGION_LOOKUP.disableTexture();
	}

	@Override
	public void beforeDrawListBuild() {
		// NOOP
	}

	@Override
	public void afterDrawListBuild() {
		// NOOP
	}

	@Override
	public QuadDistanceFunc selectQuadDistanceFunction(ArrayVertexCollector arrayVertexCollector) {
		return arrayVertexCollector.quadDistanceStandard;
	}

	@Override
	public void prepareForDraw(WorldRenderState worldRenderState) {
		VsFormat.REGION_LOOKUP.upload();
		final long cameraRegionOrigin = worldRenderState.terrainIterator.cameraRegionOrigin();
		worldRenderState.sectorManager.setCameraXZ(BlockPos.unpackLongX(cameraRegionOrigin), BlockPos.unpackLongZ(cameraRegionOrigin));
		ClumpedVertexStorage.SOLID.upload();
		ClumpedVertexStorage.TRANSLUCENT.upload();
	}

	@Override
	public UploadableRegion createUploadableRegion(VertexCollectorList vertexCollectorList, boolean sorted, int bytes, long packedOriginBlockPos) {
		return new ClumpedUploadableRegion(vertexCollectorList, sorted, bytes, packedOriginBlockPos);
	}

	@Override
	public void onRegionBuilt(int regionId, RenderRegion region) {
		final RegionPosition origin = region.origin;
		VsFormat.REGION_LOOKUP.set(regionId, origin.getX(), origin.getY(), origin.getZ());
	}

	@Override
	public void onRegionClosed(int regionId, RenderRegion region) {
		// NOOP
	}
}
