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

package grondag.canvas.render.terrain.vbo;

import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.buffer.format.QuadEncoders;
import grondag.canvas.buffer.input.ArrayVertexCollector;
import grondag.canvas.buffer.input.ArrayVertexCollector.QuadDistanceFunc;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.render.terrain.TerrainRenderConfig;
import grondag.canvas.render.terrain.base.UploadableRegion;
import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.shader.GlProgram;

public class VboRegionRenderConfig extends TerrainRenderConfig {
	public static final VboRegionRenderConfig INSTANCE = new VboRegionRenderConfig();

	private VboRegionRenderConfig() {
		super(
			"DEFAULT (VBO)",
			"DEFAULT",
			CanvasVertexFormats.STANDARD_MATERIAL_FORMAT,
			CanvasVertexFormats.STANDARD_MATERIAL_FORMAT.quadStrideInts,
			true,
			QuadEncoders.STANDARD_TRANSCODER,
			VboDrawList::build
		);
	}

	@Override
	public void setupUniforms(GlProgram program) {
		// NOOP
	}

	@Override
	public void onDeactiveProgram() {
		// NOOP
	}

	@Override
	public void onActivateProgram() {
		// NOOP
	}

	@Override
	public QuadDistanceFunc selectQuadDistanceFunction(ArrayVertexCollector arrayVertexCollector) {
		return arrayVertexCollector.quadDistanceStandard;
	}

	@Override
	public void prepareForDraw(WorldRenderState worldRenderState) {
		// NOOP
	}

	@Override
	public UploadableRegion createUploadableRegion(VertexCollectorList vertexCollectorList, boolean sorted, int bytes, long packedOriginBlockPos, WorldRenderState worldRenderState) {
		return VboDrawableRegion.uploadable(vertexCollectorList, sorted, bytes, packedOriginBlockPos);
	}
}
