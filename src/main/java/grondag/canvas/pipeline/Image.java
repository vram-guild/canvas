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

import java.nio.ByteBuffer;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL46;

import net.minecraft.client.texture.TextureUtil;

import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.CanvasGlHelper;

public class Image {
	public final ImageConfig config;
	protected int glId = -1;
	private final int width;
	private final int height;

	Image(ImageConfig config, int width, int height) {
		this.config = config;
		this.width = width;
		this.height = height;
		open();
	}

	public int glId() {
		return glId;
	}

	protected void open() {
		if (glId == -1) {
			glId = TextureUtil.generateId();
			assert CanvasGlHelper.checkError();

			CanvasTextureState.bindTexture(config.target, glId);
			assert CanvasGlHelper.checkError();

			final int[] params = config.texParamPairs;
			final int limit = params.length;

			for (int i = 0; i < limit; ++i) {
				GlStateManager.texParameter(config.target, params[i], params[++i]);
				assert CanvasGlHelper.checkError();
			}

			if (config.target == GL46.GL_TEXTURE_2D_ARRAY || config.target == GL46.GL_TEXTURE_3D) {
				GL46.glTexImage3D(config.target, 0, config.internalFormat, width, height, config.depth, 0, config.pixelFormat, config.pixelDataType, (ByteBuffer) null);
				assert CanvasGlHelper.checkError();
			} else {
				assert config.target == GL46.GL_TEXTURE_2D;
				GL46.glTexImage2D(config.target, 0, config.internalFormat, width, height, 0, config.pixelFormat, config.pixelDataType, (ByteBuffer) null);
				assert CanvasGlHelper.checkError();
			}

			assert CanvasGlHelper.checkError();

			if (config.lod > 0) {
				setupLod();
			}

			assert CanvasGlHelper.checkError();
		}
	}

	private void setupLod() {
		GlStateManager.texParameter(config.target, GL21.GL_TEXTURE_MAX_LEVEL, config.lod);
		assert CanvasGlHelper.checkError();
		GlStateManager.texParameter(config.target, GL21.GL_TEXTURE_MIN_LOD, 0);
		GlStateManager.texParameter(config.target, GL21.GL_TEXTURE_MAX_LOD, config.lod);
		assert CanvasGlHelper.checkError();
		GlStateManager.texParameter(config.target, GL21.GL_TEXTURE_LOD_BIAS, 0.0F);

		for (int i = 1; i <= config.lod; ++i) {
			if (config.target == GL46.GL_TEXTURE_3D) {
				GL46.glTexImage3D(config.target, i, config.internalFormat, width >> i, height >> i, config.depth >> i, 0, config.pixelFormat, config.pixelDataType, (ByteBuffer) null);
			} else if (config.target == GL46.GL_TEXTURE_2D_ARRAY) {
				GL46.glTexImage3D(config.target, i, config.internalFormat, width >> i, height >> i, config.depth, 0, config.pixelFormat, config.pixelDataType, (ByteBuffer) null);
			} else {
				GL46.glTexImage2D(config.target, i, config.internalFormat, width >> i, height >> i, 0, config.pixelFormat, config.pixelDataType, (ByteBuffer) null);
			}

			assert CanvasGlHelper.checkError();
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
