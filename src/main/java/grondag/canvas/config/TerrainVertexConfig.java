package grondag.canvas.config;

import org.lwjgl.opengl.GL21;

import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.buffer.encoding.ArrayVertexCollector.QuadDistanceFunc;
import grondag.canvas.buffer.encoding.QuadEncoders;
import grondag.canvas.buffer.encoding.QuadTranscoder;
import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.texture.TextureData;
import grondag.canvas.vf.TerrainVertexFetch;
import grondag.frex.api.material.UniformRefreshFrequency;

public enum TerrainVertexConfig {
	DEFAULT(
		CanvasVertexFormats.COMPACT_MATERIAL,
		CanvasVertexFormats.COMPACT_MATERIAL.quadStrideInts,
		true,
		QuadEncoders.COMPACT_TRANSCODER
	),

	FETCH(
		CanvasVertexFormats.VF_MATERIAL,
		// VF quads use vertex stride because of indexing
		CanvasVertexFormats.VF_MATERIAL.vertexStrideInts,
		false,
		QuadEncoders.VF_TRANSCODER
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
	},

	REGION(
		CanvasVertexFormats.REGION_MATERIAL,
		CanvasVertexFormats.REGION_MATERIAL.quadStrideInts,
		true,
		QuadEncoders.COMPACT_TRANSCODER
	);

	TerrainVertexConfig(
		CanvasVertexFormat vertexFormat,
		int quadStrideInts,
		boolean shouldApplyBlockPosTranslation,
		QuadTranscoder transcoder
	) {
		this.vertexFormat = vertexFormat;
		this.quadStrideInts = quadStrideInts;
		this.shouldApplyBlockPosTranslation = shouldApplyBlockPosTranslation;
		this.transcoder = transcoder;
	}

	public final CanvasVertexFormat vertexFormat;

	/** Controls allocation in vertex collectors. */
	public final int quadStrideInts;

	/** If true, then vertex positions should be translated to block pos within the region. */
	public final boolean shouldApplyBlockPosTranslation;

	public final QuadTranscoder transcoder;

	public void setupUniforms(GlProgram program) { }

	public void reload() { }

	public void onDeactiveProgram() { }

	public void onActivateProgram() { }

	public QuadDistanceFunc selectQuadDistanceFunction(ArrayVertexCollector arrayVertexCollector) {
		return arrayVertexCollector.quadDistanceStandard;
	}

	public void prepareForDraw() { }
}
