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

import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.buffer.encoding.ArrayVertexCollector.QuadDistanceFunc;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.render.region.UploadableRegion;
import grondag.canvas.render.region.base.RegionRenderConfig;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.terrain.region.RegionPosition;
import grondag.canvas.terrain.region.RenderRegion;

public class VsRegionRenderConfig extends RegionRenderConfig {
	public static final VsRegionRenderConfig INSTANCE = new VsRegionRenderConfig();

	private VsRegionRenderConfig() {
		super(
			VsFormat.VS_MATERIAL,
			VsFormat.VS_MATERIAL.quadStrideInts,
			true,
			VsFormat.VS_TRANSCODER,
			VsDrawList::build
		);
	}

	@Override
	public void setupUniforms(GlProgram program) {
		// WIP
	}

	@Override
	public void reload() {
		// WIP
	}

	@Override
	public void onDeactiveProgram() {
		// WIP
	}

	@Override
	public void onActivateProgram() {
		// WIP
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
		// WIP
	}

	@Override
	public UploadableRegion createUploadableRegion(VertexCollectorList vertexCollectorList, boolean sorted, int bytes, long packedOriginBlockPos) {
		return new VsUploadableRegion(vertexCollectorList, sorted, bytes, packedOriginBlockPos);
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
