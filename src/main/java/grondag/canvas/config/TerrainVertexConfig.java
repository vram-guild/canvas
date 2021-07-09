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

package grondag.canvas.config;

import java.util.function.Function;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.opengl.GL21;

import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.buffer.encoding.ArrayVertexCollector.QuadDistanceFunc;
import grondag.canvas.buffer.encoding.QuadEncoders;
import grondag.canvas.buffer.encoding.QuadTranscoder;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.render.region.DrawableRegion;
import grondag.canvas.render.region.RegionDrawList;
import grondag.canvas.render.region.UploadableRegion;
import grondag.canvas.render.region.vbo.VboUploadableRegion;
import grondag.canvas.render.region.vf.VfDrawList;
import grondag.canvas.render.region.vf.VfUploadableRegion;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.texture.TextureData;
import grondag.canvas.vf.TerrainVertexFetch;
import grondag.frex.api.material.UniformRefreshFrequency;

public enum TerrainVertexConfig {
	DEFAULT(
		CanvasVertexFormats.COMPACT_MATERIAL,
		CanvasVertexFormats.COMPACT_MATERIAL.quadStrideInts,
		true,
		QuadEncoders.COMPACT_TRANSCODER,
		null
	),

	FETCH(
		CanvasVertexFormats.VF_MATERIAL,
		// VF quads use vertex stride because of indexing
		CanvasVertexFormats.VF_MATERIAL.vertexStrideInts,
		false,
		QuadEncoders.VF_TRANSCODER,
		VfDrawList::build
	) {
		@Override
		public void setupUniforms(GlProgram program) {
			program.uniformSampler("samplerBuffer", "_cvu_vfColor", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_COLOR - GL21.GL_TEXTURE0));
			program.uniformSampler("samplerBuffer", "_cvu_vfUV", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_UV - GL21.GL_TEXTURE0));
			program.uniformSampler("isamplerBuffer", "_cvu_vfVertex", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_VERTEX - GL21.GL_TEXTURE0));
			program.uniformSampler("samplerBuffer", "_cvu_vfLight", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_LIGHT - GL21.GL_TEXTURE0));
			program.uniformSampler("isamplerBuffer", "_cvu_vfQuads", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_QUADS - GL21.GL_TEXTURE0));
			program.uniformSampler("isamplerBuffer", "_cvu_vfRegions", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_REGIONS - GL21.GL_TEXTURE0));
			program.uniformSampler("usamplerBuffer", "_cvu_vfQuadRegions", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_QUAD_REGIONS - GL21.GL_TEXTURE0));
		}

		@Override
		public void reload() {
			TerrainVertexFetch.clear();
		}

		@Override
		public void onDeactiveProgram() {
			TerrainVertexFetch.disable();
		}

		@Override
		public void onActivateProgram() {
			TerrainVertexFetch.enable();
		}

		@Override
		public QuadDistanceFunc selectQuadDistanceFunction(ArrayVertexCollector arrayVertexCollector) {
			return arrayVertexCollector.quadDistanceVertexFetch;
		}

		@Override
		public void prepareForDraw() {
			TerrainVertexFetch.upload();
		}

		@Override
		public UploadableRegion createUploadableRegion(VertexCollectorList vertexCollectorList, boolean sorted, int bytes, long packedOriginBlockPos) {
			return new VfUploadableRegion(vertexCollectorList, sorted, bytes, packedOriginBlockPos);
		}
	},

	REGION(
		CanvasVertexFormats.REGION_MATERIAL,
		CanvasVertexFormats.REGION_MATERIAL.quadStrideInts,
		true,
		QuadEncoders.COMPACT_TRANSCODER,
		null
	);

	TerrainVertexConfig(
		CanvasVertexFormat vertexFormat,
		int quadStrideInts,
		boolean shouldApplyBlockPosTranslation,
		QuadTranscoder transcoder,
		Function<ObjectArrayList<DrawableRegion>, RegionDrawList> drawListFunc
	) {
		this.vertexFormat = vertexFormat;
		this.quadStrideInts = quadStrideInts;
		this.shouldApplyBlockPosTranslation = shouldApplyBlockPosTranslation;
		this.transcoder = transcoder;
		this.drawListFunc = drawListFunc;
	}

	public final CanvasVertexFormat vertexFormat;

	/** Controls allocation in vertex collectors. */
	public final int quadStrideInts;

	/** If true, then vertex positions should be translated to block pos within the region. */
	public final boolean shouldApplyBlockPosTranslation;

	public final QuadTranscoder transcoder;

	public final Function<ObjectArrayList<DrawableRegion>, RegionDrawList> drawListFunc;

	public void setupUniforms(GlProgram program) { }

	public void reload() { }

	public void onDeactiveProgram() { }

	public void onActivateProgram() { }

	public QuadDistanceFunc selectQuadDistanceFunction(ArrayVertexCollector arrayVertexCollector) {
		return arrayVertexCollector.quadDistanceStandard;
	}

	public void prepareForDraw() { }

	public UploadableRegion createUploadableRegion(VertexCollectorList vertexCollectorList, boolean sorted, int bytes, long packedOriginBlockPos) {
		return new VboUploadableRegion(vertexCollectorList, sorted, bytes, packedOriginBlockPos);
	}
}
