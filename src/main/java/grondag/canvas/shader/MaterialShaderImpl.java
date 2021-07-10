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

import grondag.canvas.config.Configurator;
import grondag.canvas.texture.SpriteIndex;

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

	public void setModelOrigin(int x, int y, int z, int regionBaseIndex, int quadMapBaseIndex) {
		getOrCreate().activate();
		program.setModelOrigin(x, y, z);

		switch (Configurator.terrainRenderConfigOption) {
			case DEFAULT:
				break;
			case FETCH:
				program.baseRegionIndex.set(regionBaseIndex, quadMapBaseIndex);
				program.baseRegionIndex.upload();
				break;
			case REGION:
				break;
			default:
				assert false : "Unhandled terrain vertex config in setModelOrigin";
				break;
		}
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

	public int getIndex() {
		return index;
	}

	@Override
	public String toString() {
		return "index: " + index + "  type: " + programType.name
				+ "  vertex: " + vertexShaderSource + "(" + vertexShaderIndex
				+ ")  fragment: " + fragmentShaderSource + "(" + fragmentShaderIndex + ")";
	}

	public static final int MAX_SHADERS = 4096;
}
