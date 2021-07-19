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

package grondag.canvas.render.terrain.cluster;

import grondag.canvas.buffer.input.ArrayVertexCollector;
import grondag.canvas.buffer.input.ArrayVertexCollector.QuadDistanceFunc;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.render.terrain.RegionRenderSectorMap;
import grondag.canvas.render.terrain.TerrainFormat;
import grondag.canvas.render.terrain.TerrainRenderConfig;
import grondag.canvas.render.terrain.base.UploadableRegion;
import grondag.canvas.render.world.CanvasWorldRenderer;
import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.shader.GlProgram;
import grondag.frex.api.material.UniformRefreshFrequency;

public class ClusteredRegionRenderConfig extends TerrainRenderConfig {
	public static final ClusteredRegionRenderConfig INSTANCE = new ClusteredRegionRenderConfig();

	private ClusteredRegionRenderConfig() {
		super(
			"CLUSTERED",
			"REGION",
			TerrainFormat.TERRAIN_MATERIAL,
			TerrainFormat.TERRAIN_MATERIAL.quadStrideInts,
			true,
			TerrainFormat.TERRAIN_TRANSCODER,
			DrawListRealm::build
		);
	}

	@Override
	public void setupUniforms(GlProgram program) {
		program.uniformArrayi("_cvu_sectors_int", UniformRefreshFrequency.PER_FRAME, u -> u.set(CanvasWorldRenderer.instance().worldRenderState.sectorManager.uniformData()), RegionRenderSectorMap.UNIFORM_ARRAY_LENGTH);
	}

	@Override
	public void reload(WorldRenderState worldRenderState) {
		VertexClusterRealm.SOLID.clear();
		VertexClusterRealm.TRANSLUCENT.clear();
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
		VertexClusterRealm.SOLID.upload();
		VertexClusterRealm.TRANSLUCENT.upload();
	}

	@Override
	public UploadableRegion createUploadableRegion(VertexCollectorList vertexCollectorList, boolean sorted, int bytes, long packedOriginBlockPos) {
		return ClusteredDrawableRegion.uploadable(vertexCollectorList, sorted, bytes, packedOriginBlockPos);
	}
}
