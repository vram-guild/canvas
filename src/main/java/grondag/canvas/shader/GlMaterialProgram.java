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

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL21;

import net.minecraft.util.math.Matrix4f;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.shader.data.MatrixState;
import grondag.canvas.shader.data.ScreenRenderState;
import grondag.canvas.shader.data.ShaderDataManager;
import grondag.canvas.shader.data.UniformRefreshFrequency;
import grondag.canvas.texture.SpriteIndex;
import grondag.canvas.texture.TextureData;
import grondag.fermion.bits.BitPacker32;

public class GlMaterialProgram extends GlProgram {
	// UGLY: special casing, public
	public final Uniform1i cascade;

	private final UniformArray4f modelOrigin;
	private final UniformArrayi contextInfo;
	private final Uniform1i modelOriginType;
	private final UniformMatrix4f guiViewProjMatrix;
	private final ObjectArrayList<UniformSampler> configuredSamplers;

	private static final FloatBuffer MODEL_ORIGIN = BufferUtils.createFloatBuffer(8);
	private static final BitPacker32<Void> CONTEXT_FLAGS = new BitPacker32<>(null, null);
	private static final BitPacker32<Void>.BooleanElement CONTEXT_FLAG_HAND = CONTEXT_FLAGS.createBooleanElement();

	GlMaterialProgram(Shader vertexShader, Shader fragmentShader, CanvasVertexFormat format, ProgramType programType) {
		super(vertexShader, fragmentShader, format, programType);
		modelOrigin = (UniformArray4f) uniformArray4f("_cvu_model_origin", UniformRefreshFrequency.ON_LOAD, u -> u.setExternal(null), 2);
		contextInfo = (UniformArrayi) uniformArrayi("_cvu_context", UniformRefreshFrequency.ON_LOAD, u -> { }, 4);
		modelOriginType = (Uniform1i) uniform1i("_cvu_model_origin_type", UniformRefreshFrequency.ON_LOAD, u -> u.set(MatrixState.get().ordinal()));
		cascade = (Uniform1i) uniform1i("frxu_cascade", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		configuredSamplers = new ObjectArrayList<>();
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
		MODEL_ORIGIN.put(4, (float) (x - ShaderDataManager.cameraXd));
		MODEL_ORIGIN.put(5, (float) (y - ShaderDataManager.cameraYd));
		MODEL_ORIGIN.put(6, (float) (z - ShaderDataManager.cameraZd));
	}

	private void setCameraOrigin() {
		// camera is the model origin, so to get world just add camera pos
		MODEL_ORIGIN.put(0, ShaderDataManager.cameraX);
		MODEL_ORIGIN.put(1, ShaderDataManager.cameraY);
		MODEL_ORIGIN.put(2, ShaderDataManager.cameraZ);

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

	private final int[] contextInfoData = new int[4];

	private static final int _CV_ATLAS_WIDTH = 0;
	private static final int _CV_ATLAS_HEIGHT = 1;
	private static final int _CV_MATERIAL_TARGET = 2;
	private static final int _CV_CONTEXT_FLAGS = 3;

	private MatrixState lastMatrixState = null;

	private static final Matrix4f guiMatrix = new Matrix4f();
	private static final Matrix4fExt guiMatrixExt = (Matrix4fExt) (Object) guiMatrix;

	public void updateContextInfo(SpriteIndex atlasInfo, int targetIndex) {
		final MatrixState ms = MatrixState.get();

		if (lastMatrixState != ms) {
			modelOriginType.set(ms.ordinal());
			modelOriginType.upload();

			lastMatrixState = ms;
		}

		// updates once for hand, unlimited amount of times for GUI render because GUI transform is baked into view matrix.
		// NB: unreachable in depth pass
		if (ms == MatrixState.SCREEN && (ScreenRenderState.stateChanged() || !ScreenRenderState.renderingHand())) {
			guiMatrixExt.set(RenderSystem.getProjectionMatrix());
			guiMatrix.multiply(RenderSystem.getModelViewMatrix());
			guiViewProjMatrix.set(guiMatrix);
			guiViewProjMatrix.upload();
			ScreenRenderState.clearStateChange();
		}

		if (atlasInfo == null) {
			contextInfoData[_CV_ATLAS_WIDTH] = 0;
			contextInfoData[_CV_ATLAS_HEIGHT] = 0;
		} else {
			contextInfoData[_CV_ATLAS_WIDTH] = atlasInfo.atlasWidth();
			contextInfoData[_CV_ATLAS_HEIGHT] = atlasInfo.atlasHeight();
		}

		contextInfoData[_CV_MATERIAL_TARGET] = targetIndex;
		contextInfoData[_CV_CONTEXT_FLAGS] = CONTEXT_FLAG_HAND.setValue(ScreenRenderState.renderingHand(), contextInfoData[_CV_CONTEXT_FLAGS]);

		contextInfo.set(contextInfoData);
		contextInfo.upload();
	}

	@Override
	public void load() {
		loadConfigurableSamplers();
		super.load();
	}

	private void loadConfigurableSamplers() {
		configuredSamplers.forEach(this::removeUniform);
		configuredSamplers.clear();

		for (int i = 0; i < Pipeline.config().materialProgram.samplerNames.length; i++) {
			final int texId = i;
			final String samplerName = Pipeline.config().materialProgram.samplerNames[i];
			final String samplerType = SamplerTypeHelper.getSamplerType(this, samplerName);
			configuredSamplers.add((UniformSampler) uniformSampler(samplerType, samplerName, UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.PROGRAM_SAMPLERS - GL21.GL_TEXTURE0 + texId)));
		}
	}
}
