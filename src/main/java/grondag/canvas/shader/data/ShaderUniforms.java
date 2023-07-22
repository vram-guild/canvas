/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.shader.data;

import java.util.function.Consumer;

import org.lwjgl.opengl.GL21;

import grondag.canvas.shader.GlProgram;
import grondag.canvas.texture.TextureData;

public class ShaderUniforms {
	public static final Consumer<GlProgram> MATERIAL_UNIFORM_SETUP = program -> {
		program.uniformSampler("frxs_spriteAltas", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.MC_SPRITE_ATLAS - GL21.GL_TEXTURE0));

		program.uniformSampler("frxs_lightmap", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.MC_LIGHTMAP - GL21.GL_TEXTURE0));

		program.uniformSampler("frxs_shadowMap", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.SHADOWMAP - GL21.GL_TEXTURE0));
		program.uniformSampler("frxs_shadowMapTexture", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.SHADOWMAP_TEXTURE - GL21.GL_TEXTURE0));
		program.uniformSampler("frxs_lightData", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.COLORED_LIGHTS_DATA - GL21.GL_TEXTURE0));

		//program.uniformSampler2d("frxs_dither", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.DITHER - GL21.GL_TEXTURE0));

		//program.uniformSampler2d("frxs_hdLightmap", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.HD_LIGHTMAP - GL21.GL_TEXTURE0));

		program.uniformSampler("_cvu_materialInfo", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.MATERIAL_INFO - GL21.GL_TEXTURE0));
	};

	public static final Consumer<GlProgram> COMMON_UNIFORM_SETUP = program -> {
		program.uniformArray4f("_cvu_world", UniformRefreshFrequency.PER_FRAME, u -> u.setExternal(FloatData.FLOAT_VECTOR_DATA), FloatData.FLOAT_VECTOR_COUNT);

		program.uniformArrayui("_cvu_world_uint", UniformRefreshFrequency.PER_FRAME, u -> u.setExternal(IntData.UINT_DATA), IntData.UINT_COUNT);

		program.uniformArrayui("_cvu_flags", UniformRefreshFrequency.PER_FRAME, u -> u.setExternal(IntData.INT_DATA), IntData.INT_LENGTH);

		program.uniformMatrix4fArray("_cvu_matrix", UniformRefreshFrequency.PER_FRAME, u -> u.set(MatrixData.MATRIX_DATA));

		program.uniformMatrix3f("_cvu_normal_model_matrix", UniformRefreshFrequency.PER_FRAME, u -> u.set(MatrixData.viewNormalMatrix));
	};
}
