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

import org.lwjgl.opengl.GL21;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.buffer.format.CanvasVertexFormats;

public enum MaterialProgramManager {
	INSTANCE;

	{
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: GlShaderManager init");
		}
	}

	private final GlMaterialProgram[] materialPrograms = new GlMaterialProgram[ProgramType.values().length];

	GlMaterialProgram getOrCreateMaterialProgram(ProgramType programType) {
		assert programType != ProgramType.PROCESS;
		final int key = programType.ordinal();
		GlMaterialProgram result = materialPrograms[key];

		if (result == null) {
			final Shader vs = new GlMaterialShader(programType.vertexSource, GL21.GL_VERTEX_SHADER, programType);
			final Shader fs = new GlMaterialShader(programType.fragmentSource, GL21.GL_FRAGMENT_SHADER, programType);
			result = new GlMaterialProgram(vs, fs, CanvasVertexFormats.POSITION_COLOR_TEXTURE_MATERIAL_LIGHT_NORMAL, programType);
			ShaderData.MATERIAL_UNIFORM_SETUP.accept(result);
			materialPrograms[key] = result;
		}

		return result;
	}

	public void reload() {
		for (final GlMaterialProgram prog : materialPrograms) {
			if (prog != null) {
				prog.forceReload();
			}
		}
	}
}
