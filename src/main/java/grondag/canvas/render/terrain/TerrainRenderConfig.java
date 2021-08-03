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

package grondag.canvas.render.terrain;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.format.QuadTranscoder;
import grondag.canvas.buffer.input.ArrayVertexCollector;
import grondag.canvas.buffer.input.ArrayVertexCollector.QuadDistanceFunc;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.render.terrain.base.DrawableRegionList.DrawableRegionListFunc;
import grondag.canvas.render.terrain.base.UploadableRegion;
import grondag.canvas.render.world.WorldRenderState;

public abstract class TerrainRenderConfig {
	public final String name;

	public final String shaderConfigTag;

	public final CanvasVertexFormat vertexFormat;

	/** Controls allocation in vertex collectors. */
	public final int quadStrideInts;

	/** If true, then vertex positions should be translated to block pos within the region. */
	public final boolean shouldApplyBlockPosTranslation;

	public final QuadTranscoder transcoder;

	public final DrawableRegionListFunc drawListFunc;

	protected TerrainRenderConfig(
		String name,
		String shaderConfigTag,
		CanvasVertexFormat vertexFormat,
		int quadStrideInts,
		boolean shouldApplyBlockPosTranslation,
		QuadTranscoder transcoder,
		DrawableRegionListFunc drawListFunc
	) {
		this.name = name;
		this.shaderConfigTag = shaderConfigTag;
		this.vertexFormat = vertexFormat;
		this.quadStrideInts = quadStrideInts;
		this.shouldApplyBlockPosTranslation = shouldApplyBlockPosTranslation;
		this.transcoder = transcoder;
		this.drawListFunc = drawListFunc;
	}

	public abstract QuadDistanceFunc selectQuadDistanceFunction(ArrayVertexCollector arrayVertexCollector);

	public abstract void prepareForDraw(WorldRenderState worldRenderState);

	public abstract UploadableRegion createUploadableRegion(VertexCollectorList vertexCollectorList, boolean sorted, int bytes, long packedOriginBlockPos, WorldRenderState worldRenderStat);
}
