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

package grondag.canvas.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.ARBTextureFloat;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.texture.TextureUtil;

import grondag.canvas.pipeline.PipelineConfig.ImageConfig;

class Image {
	final ImageConfig config;
	protected int glId = -1;
	private final int width;
	private final int height;

	Image(ImageConfig config, int width, int height) {
		this.config = config;
		this.width = width;
		this.height = height;
		open();
	}

	int glId() {
		return glId;
	}

	protected void open() {
		if (glId == -1) {
			glId = TextureUtil.generateId();

			GlStateManager.bindTexture(glId);

			if (config.lod > 0) {
				GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_FILTER, GL21.GL_LINEAR_MIPMAP_NEAREST);
				GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAG_FILTER, GL21.GL_LINEAR);
			} else if (config.blur) {
				GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_FILTER, GL21.GL_LINEAR);
				GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAG_FILTER, GL21.GL_LINEAR);
			} else {
				GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_FILTER, GL21.GL_NEAREST);
				GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAG_FILTER, GL21.GL_NEAREST);
			}

			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_WRAP_S, GL21.GL_CLAMP);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_WRAP_T, GL21.GL_CLAMP);

			if (config.hdr) {
				GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, ARBTextureFloat.GL_RGBA16F_ARB, width, height, 0, GL21.GL_RGBA, GL21.GL_FLOAT, null);
			} else {
				GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, width, height, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, null);
			}

			if (config.lod > 0) {
				setupLod();
			}
		}
	}

	private void setupLod() {
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAX_LEVEL, config.lod);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_LOD, 0);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAX_LOD, config.lod);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_LOD_BIAS, 0.0F);

		for (int i = 1; i <= config.lod; ++i) {
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, i, GL21.GL_RGBA8, width >> i, height >> i, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, null);
		}
	}

	void close() {
		if (glId != -1) {
			TextureUtil.deleteId(glId);
			glId = -1;
		}
	}

	/**
	 * For attachments managed by minecraft itself.
	 */
	static class BuiltIn extends Image {
		BuiltIn(ImageConfig config, int width, int height, int glId) {
			super(config, width, height);
			this.glId = glId;
		}

		@Override
		protected void open() {
			// NOOP
		}

		@Override
		void close() {
			// NOOP
		}
	}
}
