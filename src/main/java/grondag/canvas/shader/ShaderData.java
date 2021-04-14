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

package grondag.canvas.shader;

import java.util.function.Consumer;

import org.lwjgl.opengl.GL21;

import net.minecraft.util.Identifier;

import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.FlagData;
import grondag.canvas.varia.MatrixState;
import grondag.canvas.varia.WorldDataManager;
import grondag.frex.api.material.UniformRefreshFrequency;

public class ShaderData {
	public static final Identifier DEFAULT_VERTEX_SOURCE = new Identifier("canvas:shaders/material/default.vert");
	public static final Identifier DEFAULT_FRAGMENT_SOURCE = new Identifier("canvas:shaders/material/default.frag");

	public static final Identifier MATERIAL_MAIN_VERTEX = new Identifier("canvas:shaders/internal/material_main.vert");
	public static final Identifier MATERIAL_MAIN_FRAGMENT = new Identifier("canvas:shaders/internal/material_main.frag");

	public static final Identifier DEPTH_MAIN_VERTEX = new Identifier("canvas:shaders/internal/shadow_main.vert");
	public static final Identifier DEPTH_MAIN_FRAGMENT = new Identifier("canvas:shaders/internal/shadow_main.frag");

	public static final String API_TARGET = "#include canvas:apitarget";
	public static final String FRAGMENT_START = "#include canvas:startfragment";
	public static final String VERTEX_START = "#include canvas:startvertex";

	public static final Consumer<GlProgram> MATERIAL_UNIFORM_SETUP = program -> {
		program.uniformSampler("sampler2D", "frxs_spriteAltas", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.MC_SPRITE_ATLAS - GL21.GL_TEXTURE0));

		program.uniformSampler("sampler2D", "frxs_lightmap", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.MC_LIGHTMAP - GL21.GL_TEXTURE0));

		program.uniformSampler("sampler2DArrayShadow", "frxs_shadowMap", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.SHADOWMAP - GL21.GL_TEXTURE0));
		program.uniformSampler("sampler2DArray", "frxs_shadowMapTexture", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.SHADOWMAP_TEXTURE - GL21.GL_TEXTURE0));

		//program.uniformSampler2d("frxs_dither", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.DITHER - GL21.GL_TEXTURE0));

		//program.uniformSampler2d("frxs_hdLightmap", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.HD_LIGHTMAP - GL21.GL_TEXTURE0));

		program.uniformSampler("isamplerBuffer", "_cvu_materialInfo", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.MATERIAL_INFO - GL21.GL_TEXTURE0));
	};

	public static final Consumer<GlProgram> COMMON_UNIFORM_SETUP = program -> {
		program.uniformArray4f("_cvu_world", UniformRefreshFrequency.PER_FRAME, u -> u.setExternal(WorldDataManager.DATA), WorldDataManager.VECTOR_COUNT);

		program.uniformArrayui("_cvu_flags", UniformRefreshFrequency.PER_FRAME, u -> u.setExternal(FlagData.DATA), FlagData.LENGTH);

		program.uniformMatrix4fArray("_cvu_matrix", UniformRefreshFrequency.PER_FRAME, u -> u.set(MatrixState.DATA));

		program.uniformMatrix3f("_cvu_normal_model_matrix", UniformRefreshFrequency.PER_FRAME, u -> u.set(MatrixState.viewNormalMatrix));
	};
}
