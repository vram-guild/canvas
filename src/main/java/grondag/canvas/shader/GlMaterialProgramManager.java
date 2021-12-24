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

package grondag.canvas.shader;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.buffer.format.TerrainEncoder;
import grondag.canvas.config.Configurator;
import grondag.canvas.render.terrain.TerrainSectorMap;
import grondag.canvas.render.world.CanvasWorldRenderer;
import grondag.canvas.shader.data.ShaderUniforms;
import grondag.canvas.shader.data.UniformRefreshFrequency;
import grondag.canvas.varia.GFX;

public final class GlMaterialProgramManager {
	public static final GlMaterialProgramManager INSTANCE = new GlMaterialProgramManager();

	private GlMaterialProgramManager() {
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
			result = new GlMaterialProgram(vs, fs, programType.isTerrain ? TerrainEncoder.TERRAIN_MATERIAL : CanvasVertexFormats.STANDARD_MATERIAL_FORMAT, programType);
			ShaderUniforms.MATERIAL_UNIFORM_SETUP.accept(result);

			if (programType.isTerrain) {
				result.uniformArrayi("_cvu_sectors_int", UniformRefreshFrequency.PER_FRAME, u -> u.set(CanvasWorldRenderer.instance().worldRenderState.sectorManager.uniformData()), TerrainSectorMap.UNIFORM_ARRAY_LENGTH);
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
