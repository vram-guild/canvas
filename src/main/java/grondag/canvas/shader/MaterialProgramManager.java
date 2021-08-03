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

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.config.Configurator;
import grondag.canvas.render.terrain.RegionRenderSectorMap;
import grondag.canvas.render.terrain.TerrainFormat;
import grondag.canvas.render.world.CanvasWorldRenderer;
import grondag.canvas.shader.data.ShaderUniforms;
import grondag.canvas.varia.GFX;
import grondag.frex.api.material.UniformRefreshFrequency;

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
			final Shader vs = new GlMaterialShader(programType.vertexSource, GFX.GL_VERTEX_SHADER, programType);
			final Shader fs = new GlMaterialShader(programType.fragmentSource, GFX.GL_FRAGMENT_SHADER, programType);
			result = new GlMaterialProgram(vs, fs, programType.isTerrain ? TerrainFormat.TERRAIN_MATERIAL : CanvasVertexFormats.STANDARD_MATERIAL_FORMAT, programType);
			ShaderUniforms.MATERIAL_UNIFORM_SETUP.accept(result);

			if (programType.isTerrain) {
				result.uniformArrayi("_cvu_sectors_int", UniformRefreshFrequency.PER_FRAME, u -> u.set(CanvasWorldRenderer.instance().worldRenderState.sectorManager.uniformData()), RegionRenderSectorMap.UNIFORM_ARRAY_LENGTH);
			}

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
