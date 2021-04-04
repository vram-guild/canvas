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

package grondag.canvas.texture;

import com.mojang.blaze3d.platform.TextureUtil;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.GFX;

@Environment(EnvType.CLIENT)
public class MaterialInfoTexture {
	private final int squareSizePixels = computeSquareSizeInPixels();
	private int glId = -1;
	private MaterialInfoImage image = null;
	private boolean enabled = false;

	private MaterialInfoTexture() { }

	public void reset() {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: MaterialInfoTexture init");
		}

		disable();

		if (image != null) {
			image.close();
			image = null;
		}

		if (glId != -1) {
			disable();
			TextureUtil.releaseTextureId(glId);
			glId = -1;
		}
	}

	public synchronized void set(int materialIndex, int vertexId, int fragmentId, int programFlags, int conditionId) {
		createImageIfNeeded();

		if (image != null) {
			image.set(materialIndex, vertexId, fragmentId, programFlags, conditionId);
		}
	}

	private void createImageIfNeeded() {
		if (image == null) {
			try {
				image = new MaterialInfoImage(squareSizePixels);
			} catch (final Exception e) {
				CanvasMod.LOG.warn("Unable to create material info texture due to error:", e);
				image = null;
			}
		}
	}

	public void disable() {
		if (enabled) {
			enabled = false;
			CanvasTextureState.activeTextureUnit(TextureData.MATERIAL_INFO);
			CanvasTextureState.bindTexture(0);
			CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
		}
	}

	private void uploadAndActivate() {
		try {
			boolean isNew = false;

			if (glId == -1) {
				glId = TextureUtil.generateTextureId();
				isNew = true;
			}

			CanvasTextureState.activeTextureUnit(TextureData.MATERIAL_INFO);
			CanvasTextureState.bindTexture(glId);

			image.upload();

			if (isNew) {
				GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MAX_LEVEL, 0);
				GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MIN_LOD, 0);
				GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MAX_LOD, 0);
				GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_LOD_BIAS, 0.0F);
				GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MIN_FILTER, GFX.GL_NEAREST);
				GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MAG_FILTER, GFX.GL_NEAREST);
				GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_WRAP_S, GFX.GL_REPEAT);
				GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_WRAP_T, GFX.GL_REPEAT);
			}

			CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
		} catch (final Exception e) {
			CanvasMod.LOG.warn("Unable to create material info texture due to error:", e);

			if (image != null) {
				image.close();
				image = null;
			}

			if (glId != -1) {
				TextureUtil.releaseTextureId(glId);
				glId = -1;
			}
		}
	}

	public void enable() {
		if (!enabled) {
			enabled = true;
			createImageIfNeeded();
			uploadAndActivate();
		}
	}

	public int squareSizePixels() {
		return squareSizePixels;
	}

	private static int computeSquareSizeInPixels() {
		int size = 64;
		int capacity = size * size;

		while (capacity < RenderMaterialImpl.MAX_MATERIAL_COUNT) {
			size *= 2;
			capacity = size * size;
		}

		return size;
	}

	public static final MaterialInfoTexture INSTANCE = new MaterialInfoTexture();
}
