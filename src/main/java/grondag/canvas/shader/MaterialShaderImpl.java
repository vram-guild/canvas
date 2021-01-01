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

import grondag.canvas.material.property.MaterialFog;
import grondag.canvas.material.property.MaterialMatrixState;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.texture.SpriteInfoTexture;

public final class MaterialShaderImpl {
	public final int index;
	public final int vertexShaderIndex;
	public final int fragmentShaderIndex;
	public final String vertexShaderSource;
	public final String fragmentShaderSource;

	public final ProgramType programType;
	private GlMaterialProgram program;

	public MaterialShaderImpl(int index, int vertexShaderIndex, int fragmentShaderIndex, ProgramType programType) {
		this.vertexShaderIndex = vertexShaderIndex;
		this.fragmentShaderIndex = fragmentShaderIndex;
		this.programType = programType;
		this.index = index;
		vertexShaderSource = MaterialShaderManager.VERTEX_INDEXER.fromHandle(vertexShaderIndex).toString();
		fragmentShaderSource = MaterialShaderManager.FRAGMENT_INDEXER.fromHandle(fragmentShaderIndex).toString();
	}

	private GlMaterialProgram getOrCreate() {
		GlMaterialProgram result = program;

		if (result == null) {
			result = MaterialProgramManager.INSTANCE.getOrCreateMaterialProgram(programType);
			program = result;
		}

		return result;
	}

	// WIP: all of this activation stuff is trash code
	// these should probably happen before program activation - change detection should upload as needed
	private void updateCommonUniforms(RenderState renderState) {
		program.programInfo.set(vertexShaderIndex, fragmentShaderIndex, renderState.gui ? 1 : 0);
		program.programInfo.upload();

		program.modelOriginType.set(MaterialMatrixState.getModelOrigin().ordinal());
		program.modelOriginType.upload();

		program.normalModelMatrix.set(MaterialMatrixState.getNormalModelMatrix());
		program.normalModelMatrix.upload();

		program.fogMode.set(MaterialFog.shaderParam());
		program.fogMode.upload();
	}

	public void setModelOrigin(int x, int y, int z) {
		getOrCreate().setModelOrigin(x, y, z);
	}

	public void activate(RenderState renderState) {
		getOrCreate().activate();
		updateCommonUniforms(renderState);
	}

	public void setContextInfo(SpriteInfoTexture atlasInfo, int targetIndex) {
		getOrCreate().setContextInfo(atlasInfo, targetIndex);
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

	@Override
	public String toString() {
		return "index: " + index + "  type: " + programType.name
				+ "  vertex: " + vertexShaderSource + "(" + vertexShaderIndex
				+ ")  fragment: " + fragmentShaderSource + "(" + fragmentShaderIndex + ")";
	}

	public static final int MAX_SHADERS = 4096;
}
