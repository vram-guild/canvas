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

package grondag.canvas.shader.data;

import java.util.function.Consumer;

import org.lwjgl.opengl.GL21;

import grondag.canvas.config.Configurator;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.texture.TextureData;
import grondag.frex.api.material.UniformRefreshFrequency;

public class ShaderUniforms {
	public static final Consumer<GlProgram> MATERIAL_UNIFORM_SETUP = program -> {
		program.uniformSampler("sampler2D", "frxs_spriteAltas", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.MC_SPRITE_ATLAS - GL21.GL_TEXTURE0));

		program.uniformSampler("sampler2D", "frxs_lightmap", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.MC_LIGHTMAP - GL21.GL_TEXTURE0));

		program.uniformSampler("sampler2DArrayShadow", "frxs_shadowMap", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.SHADOWMAP - GL21.GL_TEXTURE0));
		program.uniformSampler("sampler2DArray", "frxs_shadowMapTexture", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.SHADOWMAP_TEXTURE - GL21.GL_TEXTURE0));

		//program.uniformSampler2d("frxs_dither", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.DITHER - GL21.GL_TEXTURE0));

		//program.uniformSampler2d("frxs_hdLightmap", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.HD_LIGHTMAP - GL21.GL_TEXTURE0));

		program.uniformSampler("isamplerBuffer", "_cvu_materialInfo", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.MATERIAL_INFO - GL21.GL_TEXTURE0));

		if (Configurator.vf) {
			program.uniformSampler("samplerBuffer", "_cvu_vfColor", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_COLOR - GL21.GL_TEXTURE0));
			program.uniformSampler("samplerBuffer", "_cvu_vfUV", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_UV - GL21.GL_TEXTURE0));
			program.uniformSampler("isamplerBuffer", "_cvu_vfVertex", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_VERTEX - GL21.GL_TEXTURE0));
			program.uniformSampler("samplerBuffer", "_cvu_vfLight", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_LIGHT - GL21.GL_TEXTURE0));
			program.uniformSampler("isamplerBuffer", "_cvu_vfQuads", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_QUADS - GL21.GL_TEXTURE0));
			program.uniformSampler("isamplerBuffer", "_cvu_vfRegions", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_REGIONS - GL21.GL_TEXTURE0));
		}
	};

	public static final Consumer<GlProgram> COMMON_UNIFORM_SETUP = program -> {
		program.uniformArray4f("_cvu_world", UniformRefreshFrequency.PER_FRAME, u -> u.setExternal(FloatData.FLOAT_VECTOR_DATA), FloatData.FLOAT_VECTOR_COUNT);

		program.uniformArrayui("_cvu_world_uint", UniformRefreshFrequency.PER_FRAME, u -> u.setExternal(IntData.UINT_DATA), IntData.UINT_COUNT);

		program.uniformArrayui("_cvu_flags", UniformRefreshFrequency.PER_FRAME, u -> u.setExternal(IntData.INT_DATA), IntData.INT_LENGTH);

		program.uniformMatrix4fArray("_cvu_matrix", UniformRefreshFrequency.PER_FRAME, u -> u.set(MatrixData.MATRIX_DATA));

		program.uniformMatrix3f("_cvu_normal_model_matrix", UniformRefreshFrequency.PER_FRAME, u -> u.set(MatrixData.viewNormalMatrix));
	};
}
