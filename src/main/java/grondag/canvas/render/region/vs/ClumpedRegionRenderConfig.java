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

import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.buffer.encoding.ArrayVertexCollector.QuadDistanceFunc;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.render.region.UploadableRegion;
import grondag.canvas.render.region.base.RegionRenderConfig;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.terrain.region.RegionPosition;
import grondag.canvas.terrain.region.RenderRegion;
import grondag.canvas.texture.TextureData;
import grondag.frex.api.material.UniformRefreshFrequency;

public class ClumpedRegionRenderConfig extends RegionRenderConfig {
	public static final ClumpedRegionRenderConfig INSTANCE = new ClumpedRegionRenderConfig();

	private ClumpedRegionRenderConfig() {
		super(
			"CLUMPED",
			VsFormat.VS_MATERIAL,
			VsFormat.VS_MATERIAL.quadStrideInts,
			true,
			VsFormat.VS_TRANSCODER,
			ClumpedDrawList::build
		);
	}

	@Override
	public void setupUniforms(GlProgram program) {
		program.uniformSampler("isamplerBuffer", "_cvu_vfRegions", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_REGIONS - GL21.GL_TEXTURE0));
	}

	@Override
	public void reload() {
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
		// WIP
	}

	@Override
	public void afterDrawListBuild() {
		// WIP
	}

	@Override
	public QuadDistanceFunc selectQuadDistanceFunction(ArrayVertexCollector arrayVertexCollector) {
		return arrayVertexCollector.quadDistanceStandard;
	}

	@Override
	public void prepareForDraw() {
		VsFormat.REGION_LOOKUP.upload();
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
