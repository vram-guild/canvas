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

import io.vram.bitkit.BitKit;
import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.texture.SpriteIndex;

public final class MaterialProgram {
	public final ProgramType programType;
	public final int target;
	private GlMaterialProgram program;

	private MaterialProgram(ProgramType programType, int target) {
		this.programType = programType;
		this.target = target;
	}

	private GlMaterialProgram getOrCreate() {
		GlMaterialProgram result = program;

		if (result == null) {
			result = GlMaterialProgramManager.INSTANCE.getOrCreateMaterialProgram(programType, target);
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

	private static final int PROGRAM_TYPE_INDEX_BITS = BitKit.bitLength(ProgramType.values().length - 1);
	private static final int TARGET_INDEX_BITS = BitKit.bitLength(MaterialConstants.TARGET_COUNT - 1);
	public static final int MAX_MATERIAL_PROGRAM_INDEX = (1 << (PROGRAM_TYPE_INDEX_BITS + TARGET_INDEX_BITS)) - 1;

	static int index(ProgramType programType, int target) {
		return programType.ordinal() | (target << PROGRAM_TYPE_INDEX_BITS);
	}

	private static final MaterialProgram[] VALUES = new MaterialProgram[MAX_MATERIAL_PROGRAM_INDEX];

	public static MaterialProgram get(ProgramType programType, int target) {
		final var index = index(programType, target);
		var result = VALUES[index];

		if (result == null) {
			result = new MaterialProgram(programType, target);
			VALUES[index] = result;
		}

		return result;
	}

	public static void reload() {
		for (final var val : VALUES) {
			if (val != null) {
				val.program = null;
			}
		}
	}
}
