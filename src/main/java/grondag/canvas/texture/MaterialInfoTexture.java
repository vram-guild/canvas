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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.texture.TextureUtil;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.material.state.RenderMaterialImpl;

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
			TextureUtil.deleteId(glId);
			glId = -1;
		}
	}

	public synchronized void set(int materialIndex, int vertexId, int fragmentId, int programFlags, int reserved) {
		createImageIfNeeded();

		if (image != null) {
			image.set(materialIndex, vertexId, fragmentId, programFlags, reserved);
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
			GlStateManager.activeTexture(TextureData.MATERIAL_INFO);
			GlStateManager.bindTexture(0);
			GlStateManager.disableTexture();
			GlStateManager.activeTexture(TextureData.MC_SPRITE_ATLAS);
		}
	}

	private void uploadAndActivate() {
		try {
			boolean isNew = false;

			if (glId == -1) {
				glId = TextureUtil.generateId();
				isNew = true;
			}

			GlStateManager.activeTexture(TextureData.MATERIAL_INFO);
			GlStateManager.bindTexture(glId);

			image.upload();

			GlStateManager.enableTexture();

			if (isNew) {
				RenderSystem.matrixMode(GL21.GL_TEXTURE);
				RenderSystem.loadIdentity();
				RenderSystem.matrixMode(GL21.GL_MODELVIEW);
				GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAX_LEVEL, 0);
				GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_LOD, 0);
				GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAX_LOD, 0);
				GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_LOD_BIAS, 0.0F);
				GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_FILTER, GL21.GL_NEAREST);
				GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAG_FILTER, GL21.GL_NEAREST);
				GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_WRAP_S, GL21.GL_REPEAT);
				GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_WRAP_T, GL21.GL_REPEAT);
			}

			GlStateManager.activeTexture(TextureData.MC_SPRITE_ATLAS);
		} catch (final Exception e) {
			CanvasMod.LOG.warn("Unable to create material info texture due to error:", e);

			if (image != null) {
				image.close();
				image = null;
			}

			if (glId != -1) {
				TextureUtil.deleteId(glId);
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
