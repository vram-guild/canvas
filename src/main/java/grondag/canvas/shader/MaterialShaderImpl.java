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

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.util.math.Matrix4f;

import grondag.canvas.material.state.RenderState;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.texture.SpriteInfoTexture;
import grondag.canvas.varia.MatrixState;

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

	private static final Matrix4f guiMatrix = new Matrix4f();
	private static final Matrix4fExt guiMatrixExt = (Matrix4fExt) (Object) guiMatrix;

	// WIP: all of this activation stuff is trash code
	// these should probably happen before program activation - change detection should upload as needed
	private void updateCommonUniforms(RenderState renderState) {
		final MatrixState ms = MatrixState.get();

		program.modelOriginType.set(ms.ordinal());
		program.modelOriginType.upload();

		if (ms == MatrixState.SCREEN) {
			guiMatrixExt.set(RenderSystem.getProjectionMatrix());
			guiMatrix.multiply(RenderSystem.getModelViewMatrix());
			program.guiViewProjMatrix.set(guiMatrix);
			program.guiViewProjMatrix.upload();
		}

		program.fogInfo.set(RenderSystem.getShaderFogStart(), RenderSystem.getShaderFogEnd());
		program.fogInfo.upload();
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

	@Override
	public String toString() {
		return "index: " + index + "  type: " + programType.name
				+ "  vertex: " + vertexShaderSource + "(" + vertexShaderIndex
				+ ")  fragment: " + fragmentShaderSource + "(" + fragmentShaderIndex + ")";
	}

	public static final int MAX_SHADERS = 4096;
}
