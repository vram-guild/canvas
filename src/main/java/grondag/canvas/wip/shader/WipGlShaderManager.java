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

package grondag.canvas.wip.shader;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.shader.Shader;
import grondag.canvas.wip.encoding.WipVertexFormat;
import grondag.canvas.wip.shader.WipGlProgram.Uniform3fImpl;
import grondag.canvas.wip.shader.WipGlProgram.UniformArrayfImpl;
import grondag.canvas.wip.state.WipProgramType;
import grondag.frex.api.material.UniformRefreshFrequency;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.lwjgl.opengl.GL21;

public enum WipGlShaderManager {
	INSTANCE;

	{
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: GlShaderManager init");
		}
	}

	private final Int2ObjectOpenHashMap<WipGlProgram> materialPrograms = new Int2ObjectOpenHashMap<>();

	public void reload() {
		WipGlShader.forceReloadErrors();
		materialPrograms.values().forEach(s -> s.forceReload());
	}

	WipGlProgram getOrCreateMaterialShader(WipProgramType programType, WipVertexFormat format) {
		final int key = format.formatIndex | (programType.ordinal() << 16);
		WipGlProgram result = materialPrograms.get(key);

		if (result == null) {
			final ObjectOpenHashSet<WipMaterialShaderImpl> materials = new ObjectOpenHashSet<>();
			final Shader vs =  new WipGlMaterialShader(WipShaderData.MATERIAL_MAIN_VERTEX, GL21.GL_VERTEX_SHADER, programType, format, materials);
			final Shader fs = new WipGlMaterialShader(WipShaderData.MATERIAL_MAIN_FRAGMENT, GL21.GL_FRAGMENT_SHADER, programType, format, materials);
			result = new WipGlProgram(vs, fs, format, programType, materials);
			WipShaderData.STANDARD_UNIFORM_SETUP.accept(result);
			result.modelOrigin = (Uniform3fImpl) result.uniform3f("_cvu_model_origin", UniformRefreshFrequency.ON_LOAD, u -> u.set(0, 0, 0));
			result.normalModelMatrix = result.uniformMatrix3f("_cvu_normal_model_matrix", UniformRefreshFrequency.ON_LOAD, u -> {});
			result.materialArray = (UniformArrayfImpl) result.uniformArrayf("_cvu_material", UniformRefreshFrequency.ON_LOAD, u -> {}, 4);
			materialPrograms.put(key, result);
		}

		return result;
	}
}
