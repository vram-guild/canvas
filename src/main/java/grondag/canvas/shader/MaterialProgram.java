/*
 * Copyright © Original Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.shader;

import io.vram.frex.api.texture.SpriteIndex;

public final class MaterialProgram {
	public final ProgramType programType;
	private GlMaterialProgram program;

	private MaterialProgram(ProgramType programType) {
		this.programType = programType;
	}

	private GlMaterialProgram getOrCreate() {
		GlMaterialProgram result = program;

		if (result == null) {
			result = GlMaterialProgramManager.INSTANCE.getOrCreateMaterialProgram(programType);
			program = result;
		}

		return result;
	}

	public void setModelOrigin(int x, int y, int z) {
		getOrCreate().activate();
		program.setModelOrigin(x, y, z);
	}

	public void setCascade(int cascade) {
		getOrCreate().activate();
		program.cascade.set(cascade);
		program.cascade.upload();
	}

	public void updateContextInfo(SpriteIndex atlasInfo, int targetIndex) {
		getOrCreate().activate();
		program.updateContextInfo(atlasInfo, targetIndex);
	}

	public void reload() {
		if (program != null) {
			program.unload();
			program = null;
		}
	}

	public static final MaterialProgram COLOR = new MaterialProgram(ProgramType.MATERIAL_COLOR);
	public static final MaterialProgram COLOR_TERRAIN = new MaterialProgram(ProgramType.MATERIAL_COLOR_TERRAIN);
	public static final MaterialProgram DEPTH = new MaterialProgram(ProgramType.MATERIAL_DEPTH);
	public static final MaterialProgram DEPTH_TERRAIN = new MaterialProgram(ProgramType.MATERIAL_DEPTH_TERRAIN);
}
