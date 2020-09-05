/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.shader;

import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.WorldDataManager;
import grondag.frex.api.material.UniformRefreshFrequency;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL21;

import java.util.function.Consumer;

public class ShaderData {
	public static final Identifier DEFAULT_VERTEX_SOURCE = new Identifier("canvas:shaders/material/default.vert");
	public static final Identifier DEFAULT_FRAGMENT_SOURCE = new Identifier("canvas:shaders/material/default.frag");

	public static final Identifier API_TARGET = new Identifier("canvas:apitarget");

	public static final Identifier HD_VERTEX = new Identifier("canvas:shaders/internal/hd/hd.vert");
	public static final Identifier HD_FRAGMENT = new Identifier("canvas:shaders/internal/hd/hd.frag");

	public static final Identifier VANILLA_VERTEX = new Identifier("canvas:shaders/internal/vanilla/vanilla.vert");
	public static final Identifier VANILLA_FRAGMENT = new Identifier("canvas:shaders/internal/vanilla/vanilla.frag");

	public static final Identifier DEFAULT_ENTITY_VERTEX = new Identifier("canvas:shaders/internal/entity/default.vert");
	public static final Identifier DEFAULT_ENTITY_FRAGMENT = new Identifier("canvas:shaders/internal/entity/default.frag");

	private static final float[] BITWISE_DIVISORS = {0.5f, 0.25f, 0.125f, 0.0625f, 0.03125f, 0.015625f, 0.0078125f, 0.00390625f};

	public static final Consumer<GlProgram> STANDARD_UNIFORM_SETUP = program -> {
		program.uniformArrayf("_cvu_world", UniformRefreshFrequency.PER_TICK, u -> u.set(WorldDataManager.data()), WorldDataManager.LENGTH);

		program.uniformSampler2d("frxs_spriteAltas", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.MC_SPRITE_ATLAS - GL21.GL_TEXTURE0));

		program.uniformSampler2d("frxs_overlay", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.MC_OVELAY - GL21.GL_TEXTURE0));

		program.uniformSampler2d("frxs_lightmap", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.MC_LIGHTMAP - GL21.GL_TEXTURE0));

		program.uniformSampler2d("frxs_dither", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.DITHER - GL21.GL_TEXTURE0));

		program.uniformSampler2d("frxs_hdLightmap", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.HD_LIGHTMAP - GL21.GL_TEXTURE0));

		program.uniformSampler2d("frxs_spriteInfo", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.SPRITE_INFO - GL21.GL_TEXTURE0));

		program.uniformArrayf("_fru_bitwise_divisors", UniformRefreshFrequency.ON_LOAD, u -> u.set(BITWISE_DIVISORS), 8);
	};
}
