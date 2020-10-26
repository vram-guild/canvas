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

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.material.property.MaterialMatrixState;
import grondag.canvas.shader.GlProgram.Uniform1iImpl;
import grondag.canvas.shader.GlProgram.Uniform2iImpl;
import grondag.canvas.shader.GlProgram.Uniform3fImpl;
import grondag.canvas.shader.GlProgram.UniformArrayfImpl;
import grondag.frex.api.material.UniformRefreshFrequency;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.lwjgl.opengl.GL21;

public enum MaterialProgramManager {
	INSTANCE;

	{
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: GlShaderManager init");
		}
	}

	private final Int2ObjectOpenHashMap<GlProgram> materialPrograms = new Int2ObjectOpenHashMap<>();

	public void reload() {
		GlShader.forceReloadErrors();
		materialPrograms.values().forEach(s -> s.forceReload());
	}

	GlProgram getOrCreateMaterialProgram(ProgramType programType) {
		assert programType == ProgramType.MATERIAL_UNIFORM_LOGIC ||  programType == ProgramType.MATERIAL_VERTEX_LOGIC;
		final int key = programType.ordinal();
		GlProgram result = materialPrograms.get(key);

		if (result == null) {
			final Shader vs =  new GlMaterialShader(ShaderData.MATERIAL_MAIN_VERTEX, GL21.GL_VERTEX_SHADER, programType);
			final Shader fs = new GlMaterialShader(ShaderData.MATERIAL_MAIN_FRAGMENT, GL21.GL_FRAGMENT_SHADER, programType);
			result = new GlProgram(vs, fs, MaterialVertexFormats.POSITION_COLOR_TEXTURE_MATERIAL_LIGHT_NORMAL, programType);
			ShaderData.STANDARD_UNIFORM_SETUP.accept(result);
			result.modelOrigin = (Uniform3fImpl) result.uniform3f("_cvu_model_origin", UniformRefreshFrequency.ON_LOAD, u -> u.set(0, 0, 0));
			result.normalModelMatrix = result.uniformMatrix3f("_cvu_normal_model_matrix", UniformRefreshFrequency.ON_LOAD, u -> {});
			result.materialArray = (UniformArrayfImpl) result.uniformArrayf("_cvu_material", UniformRefreshFrequency.ON_LOAD, u -> {}, 4);
			result.programId = (Uniform2iImpl) result.uniform2i("_cvu_program", UniformRefreshFrequency.ON_LOAD, u -> {});
			result.modelOriginType = (Uniform1iImpl) result.uniform1i("_cvu_model_origin_type", UniformRefreshFrequency.ON_LOAD, u -> u.set(MaterialMatrixState.getModelOrigin().ordinal()));
			materialPrograms.put(key, result);
		}

		return result;
	}
}
