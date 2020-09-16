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

package grondag.canvas.shader.wip;

import java.util.ArrayList;
import java.util.function.Consumer;

import grondag.canvas.shader.Shader;
import grondag.canvas.shader.wip.WipGlProgram.Uniform3fImpl;
import grondag.canvas.shader.wip.encoding.WipVertexFormat;
import grondag.frex.api.material.MaterialShader;
import grondag.frex.api.material.UniformRefreshFrequency;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix3f;

public final class WipMaterialShaderImpl implements MaterialShader {
	public final int index;
	public final Identifier vertexShader;
	public final Identifier fragmentShader;
	public final WipProgramType programType;
	public final  WipVertexFormat format;

	private final ArrayList<Consumer<WipGlProgram>> programSetups = new ArrayList<>();
	private WipGlProgram program;


	public WipMaterialShaderImpl(int index, Identifier vertexShader, Identifier fragmentShader, WipProgramType programType, WipVertexFormat format) {
		this.vertexShader = vertexShader;
		this.fragmentShader = fragmentShader;
		this.programType = programType;
		this.format = format;
		this.index = index;
	}

	private WipGlProgram getOrCreate() {
		final WipGlProgram result = program;

		if (result == null) {
			final Shader vs = WipGlShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, programType, format);
			final Shader fs = WipGlShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader, programType, format);
			final WipGlProgram newProgram = new WipGlProgram(vs, fs, format, programType);
			programSetups.forEach(ps -> ps.accept(newProgram));
			newProgram.modelOrigin = (Uniform3fImpl) newProgram.uniform3f("_cvu_model_origin", UniformRefreshFrequency.ON_LOAD, u -> u.set(0, 0, 0));
			newProgram.normalModelMatrix = newProgram.uniformMatrix3f("_cvu_normal_model_matrix", UniformRefreshFrequency.ON_LOAD, u -> {});
			newProgram.load();
			program = newProgram;
			return newProgram;
		}

		return result;
	}

	public void activate(int x, int y, int z) {
		getOrCreate().actvateWithiModelOrigin(x, y, z);
	}

	public void activate() {
		getOrCreate().activate();
	}

	public void activate(Matrix3f normalmodelmatrix) {
		getOrCreate().actvateWithNormalModelMatrix(normalmodelmatrix);
	}

	public void reload() {
		if (program != null) {
			program.unload();
			program = null;
		}
	}

	public int getIndex() {
		return index;
	}

	public void addProgramSetup(Consumer<WipGlProgram> setup) {
		assert setup != null;
		programSetups.add(setup);
	}

	public void onRenderTick() {
		if (program != null) {
			program.onRenderTick();
		}
	}

	public void onGameTick() {
		if (program != null) {
			program.onGameTick();
		}
	}
}
