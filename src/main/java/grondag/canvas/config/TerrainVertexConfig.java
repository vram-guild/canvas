package grondag.canvas.config;

import java.util.function.Consumer;

import org.lwjgl.opengl.GL21;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.texture.TextureData;
import grondag.frex.api.material.UniformRefreshFrequency;

public enum TerrainVertexConfig {
	DEFAULT(
		CanvasVertexFormats.COMPACT_MATERIAL,
		CanvasVertexFormats.COMPACT_MATERIAL.quadStrideInts,
			p -> { }
	),

	FETCH(
		CanvasVertexFormats.VF_MATERIAL,
		// VF quads use vertex stride because of indexing
		CanvasVertexFormats.VF_MATERIAL.vertexStrideInts,
			p -> {
				p.uniformSampler("samplerBuffer", "_cvu_vfColor", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_COLOR - GL21.GL_TEXTURE0));
				p.uniformSampler("samplerBuffer", "_cvu_vfUV", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_UV - GL21.GL_TEXTURE0));
				p.uniformSampler("isamplerBuffer", "_cvu_vfVertex", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_VERTEX - GL21.GL_TEXTURE0));
				p.uniformSampler("samplerBuffer", "_cvu_vfLight", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_LIGHT - GL21.GL_TEXTURE0));
				p.uniformSampler("isamplerBuffer", "_cvu_vfQuads", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_QUADS - GL21.GL_TEXTURE0));
				p.uniformSampler("isamplerBuffer", "_cvu_vfRegions", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_REGIONS - GL21.GL_TEXTURE0));
				p.uniformSampler("usamplerBuffer", "_cvu_vfQuadRegions", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_QUAD_REGIONS - GL21.GL_TEXTURE0));
			}
	),

	REGION(
		CanvasVertexFormats.REGION_MATERIAL,
		CanvasVertexFormats.REGION_MATERIAL.quadStrideInts,
			p -> { }
	);

	TerrainVertexConfig(
		CanvasVertexFormat vertexFormat,
		int quadStrideInts,
		Consumer<GlProgram> uniformSetup
	) {
		this.vertexFormat = vertexFormat;
		this.quadStrideInts = quadStrideInts;
		this.uniformSetup = uniformSetup;
	}

	public final CanvasVertexFormat vertexFormat;

	/** Controls allocation in vertex collectors. */
	public final int quadStrideInts;

	public final Consumer<GlProgram> uniformSetup;
}
