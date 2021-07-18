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

package grondag.canvas.render.terrain.clump;

import org.lwjgl.opengl.GL21;

import grondag.canvas.buffer.input.ArrayVertexCollector;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.buffer.input.ArrayVertexCollector.QuadDistanceFunc;
import grondag.canvas.render.terrain.TerrainRenderConfig;
import grondag.canvas.render.terrain.base.UploadableRegion;
import grondag.canvas.render.terrain.TerrainFormat;
import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.texture.TextureData;
import grondag.frex.api.material.UniformRefreshFrequency;

public class ClumpedRegionRenderConfig extends TerrainRenderConfig {
	public static final ClumpedRegionRenderConfig INSTANCE = new ClumpedRegionRenderConfig();

	private ClumpedRegionRenderConfig() {
		super(
			"CLUMPED",
			"REGION",
			TerrainFormat.TERRAIN_MATERIAL,
			TerrainFormat.TERRAIN_MATERIAL.quadStrideInts,
			true,
			TerrainFormat.TERRAIN_TRANSCODER,
			ClumpedDrawList::build
		);
	}

	@Override
	public void setupUniforms(GlProgram program) {
		program.uniformSampler("isamplerBuffer", "_cvu_vfRegions", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_REGIONS - GL21.GL_TEXTURE0));
	}

	@Override
	public void reload(WorldRenderState worldRenderState) {
		ClumpedVertexStorage.SOLID.clear();
		ClumpedVertexStorage.TRANSLUCENT.clear();
	}

	@Override
	public void onActivateProgram() {
		// NOOP
	}

	@Override
	public void onDeactiveProgram() {
		// NOOP
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
		ClumpedVertexStorage.SOLID.upload();
		ClumpedVertexStorage.TRANSLUCENT.upload();
	}

	@Override
	public UploadableRegion createUploadableRegion(VertexCollectorList vertexCollectorList, boolean sorted, int bytes, long packedOriginBlockPos) {
		return new ClumpedUploadableRegion(vertexCollectorList, sorted, bytes, packedOriginBlockPos);
	}
}
