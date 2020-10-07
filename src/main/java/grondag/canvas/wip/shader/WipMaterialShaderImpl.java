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

import grondag.canvas.texture.SpriteInfoTexture;
import grondag.canvas.wip.encoding.WipVertexFormat;
import grondag.canvas.wip.state.WipProgramType;
import grondag.frex.api.material.MaterialShader;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix3f;

public final class WipMaterialShaderImpl implements MaterialShader {
	public final int index;
	public final Identifier vertexShader;
	public final Identifier fragmentShader;
	public final WipProgramType programType;
	public final  WipVertexFormat format;
	private WipGlProgram program;

	public WipMaterialShaderImpl(int index, Identifier vertexShader, Identifier fragmentShader, WipProgramType programType, WipVertexFormat format) {
		this.vertexShader = vertexShader;
		this.fragmentShader = fragmentShader;
		this.programType = programType;
		this.format = format;
		this.index = index;
	}

	private WipGlProgram getOrCreate() {
		WipGlProgram result = program;

		if (result == null) {
			result = WipGlShaderManager.INSTANCE.getOrCreateMaterialProgram(programType, format);
			program = result;
			result.addMaterial(this);
		}

		return result;
	}

	public void activate(int x, int y, int z) {
		getOrCreate().actvateWithiModelOrigin(x, y, z);
	}

	public void activate() {
		getOrCreate().activate();
	}

	public void activate(Matrix3f normalmodelmatrix, SpriteInfoTexture atlasInfo) {
		getOrCreate().actvateWithNormalModelMatrix(normalmodelmatrix, atlasInfo);
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
