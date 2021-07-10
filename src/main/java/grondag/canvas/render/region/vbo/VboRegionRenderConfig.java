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

package grondag.canvas.render.region.vbo;

import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.buffer.encoding.ArrayVertexCollector.QuadDistanceFunc;
import grondag.canvas.buffer.encoding.QuadEncoders;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.render.region.UploadableRegion;
import grondag.canvas.render.region.base.RegionRenderConfig;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.terrain.region.RenderRegion;

public class VboRegionRenderConfig extends RegionRenderConfig {
	public static final VboRegionRenderConfig INSTANCE = new VboRegionRenderConfig();

	private VboRegionRenderConfig() {
		super(
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
	public void reload() {
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
	public void prepareForDraw() {
		// NOOP
	}

	@Override
	public UploadableRegion createUploadableRegion(VertexCollectorList vertexCollectorList, boolean sorted, int bytes, long packedOriginBlockPos) {
		return new VboUploadableRegion(vertexCollectorList, sorted, bytes, packedOriginBlockPos);
	}

	@Override
	public void onRegionBuilt(int regionId, RenderRegion region) {
		// NOOP
	}

	@Override
	public void onRegionClosed(int regionId, RenderRegion region) {
		// NOOP
	}
}
