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

import java.nio.FloatBuffer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL21;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.texture.SpriteIndex;
import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.MatrixState;
import grondag.canvas.varia.WorldDataManager;
import grondag.frex.api.material.UniformRefreshFrequency;

public class GlMaterialProgram extends GlProgram {
	// UGLY: special casing, public
	public final UniformArray4fImpl modelOrigin;
	public final UniformArrayiImpl contextInfo;
	public final Uniform1iImpl modelOriginType;
	public final Uniform1iImpl cascade;
	public final UniformMatrix4fImpl guiViewProjMatrix;

	private final ObjectArrayList<UniformSamplerImpl> configuredSamplers;

	private static final FloatBuffer MODEL_ORIGIN = BufferUtils.createFloatBuffer(8);

	GlMaterialProgram(Shader vertexShader, Shader fragmentShader, CanvasVertexFormat format, ProgramType programType) {
		super(vertexShader, fragmentShader, format, programType);
		modelOrigin = (UniformArray4fImpl) uniformArray4f("_cvu_model_origin", UniformRefreshFrequency.ON_LOAD, u -> u.setExternal(null), 2);
		contextInfo = (UniformArrayiImpl) uniformArrayi("_cvu_context", UniformRefreshFrequency.ON_LOAD, u -> { }, 3);
		modelOriginType = (Uniform1iImpl) uniform1i("_cvu_model_origin_type", UniformRefreshFrequency.ON_LOAD, u -> u.set(MatrixState.get().ordinal()));
		cascade = (Uniform1iImpl) uniform1i("frxu_cascade", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		configuredSamplers = new ObjectArrayList<>();
		reloadConfigurableSamplers();
		guiViewProjMatrix = uniformMatrix4f("_cvu_guiViewProjMatrix", UniformRefreshFrequency.ON_LOAD, u -> { });
	}

	public void setModelOrigin(int x, int y, int z) {
		switch (MatrixState.get()) {
			case CAMERA:
				setCameraOrigin();
				break;
			case REGION:
				setRegionOrigin(x, y, z);
				break;
			case SCREEN:
			default:
				setScreenOrigin();
				break;
		}

		modelOrigin.setExternal(MODEL_ORIGIN);
		modelOrigin.upload();
	}

	private void setRegionOrigin(int x, int y, int z) {
		// region origin is model origin
		MODEL_ORIGIN.put(0, x);
		MODEL_ORIGIN.put(1, y);
		MODEL_ORIGIN.put(2, z);

		// to get to view/camera space, add world and subtract camera
		MODEL_ORIGIN.put(4, (float) (x - WorldDataManager.cameraXd));
		MODEL_ORIGIN.put(5, (float) (y - WorldDataManager.cameraYd));
		MODEL_ORIGIN.put(6, (float) (z - WorldDataManager.cameraZd));
	}

	private void setCameraOrigin() {
		// camera is the model origin, so to get world just add camera pos
		MODEL_ORIGIN.put(0, WorldDataManager.cameraX);
		MODEL_ORIGIN.put(1, WorldDataManager.cameraY);
		MODEL_ORIGIN.put(2, WorldDataManager.cameraZ);

		// relative to camera, so model to view space is zero
		MODEL_ORIGIN.put(4, 0);
		MODEL_ORIGIN.put(5, 0);
		MODEL_ORIGIN.put(6, 0);
	}

	private void setScreenOrigin() {
		// everything already in screen space
		MODEL_ORIGIN.put(0, 0);
		MODEL_ORIGIN.put(1, 0);
		MODEL_ORIGIN.put(2, 0);

		// everything already in screen space
		MODEL_ORIGIN.put(4, 0);
		MODEL_ORIGIN.put(5, 0);
		MODEL_ORIGIN.put(6, 0);
	}

	private final int[] materialData = new int[3];

	private static final int _CV_ATLAS_WIDTH = 0;
	private static final int _CV_ATLAS_HEIGHT = 1;
	private static final int _CV_MATERIAL_TARGET = 2;

	public void setContextInfo(SpriteIndex atlasInfo, int targetIndex) {
		if (atlasInfo == null) {
			materialData[_CV_ATLAS_WIDTH] = 0;
			materialData[_CV_ATLAS_HEIGHT] = 0;
		} else {
			materialData[_CV_ATLAS_WIDTH] = atlasInfo.atlasWidth();
			materialData[_CV_ATLAS_HEIGHT] = atlasInfo.atlasHeight();
		}

		materialData[_CV_MATERIAL_TARGET] = targetIndex;

		contextInfo.set(materialData);
		contextInfo.upload();
	}

	public void reloadConfigurableSamplers() {
		configuredSamplers.forEach(this::removeUniform);
		configuredSamplers.clear();

		for (int i = 0; i < Pipeline.config().materialProgram.samplerNames.length; i++) {
			final int texId = i;
			final String samplerName = Pipeline.config().materialProgram.samplerNames[i];
			final String samplerType = SamplerTypeHelper.getSamplerType(this, samplerName);
			configuredSamplers.add((UniformSamplerImpl) uniformSampler(samplerType, samplerName, UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.PROGRAM_SAMPLERS - GL21.GL_TEXTURE0 + texId)));
		}
	}
}
