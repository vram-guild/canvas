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

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.TextureUtil;

import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.GFX;

public class Image {
	public final ImageConfig config;
	protected int glId = -1;
	public final int width;
	public final int height;

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
			glId = TextureUtil.generateTextureId();

			CanvasTextureState.bindTexture(config.target, glId);
			GFX.objectLabel(GL11.GL_TEXTURE, glId, "IMG " + config.name); // Yes, it needs to be GL_TEXTURE and not GL_TEXTURE_2D or the one its binding to

			final int[] params = config.texParamPairs;
			final int limit = params.length;

			for (int i = 0; i < limit; ++i) {
				GFX.texParameter(config.target, params[i], params[++i]);
			}

			if (config.target == GFX.GL_TEXTURE_2D_ARRAY || config.target == GFX.GL_TEXTURE_3D) {
				GFX.texImage3D(config.target, 0, config.internalFormat, width, height, config.depth, 0, config.pixelFormat, config.pixelDataType, (ByteBuffer) null);
			} else {
				assert config.target == GFX.GL_TEXTURE_2D;
				GFX.texImage2D(config.target, 0, config.internalFormat, width, height, 0, config.pixelFormat, config.pixelDataType, (ByteBuffer) null);
			}

			if (config.lod > 0) {
				setupLod();
			}
		}
	}

	private void setupLod() {
		GFX.texParameter(config.target, GFX.GL_TEXTURE_MAX_LEVEL, config.lod);
		GFX.texParameter(config.target, GFX.GL_TEXTURE_MIN_LOD, 0);
		GFX.texParameter(config.target, GFX.GL_TEXTURE_MAX_LOD, config.lod);
		GFX.texParameter(config.target, GFX.GL_TEXTURE_LOD_BIAS, 0.0F);

		for (int i = 1; i <= config.lod; ++i) {
			if (config.target == GFX.GL_TEXTURE_3D) {
				GFX.texImage3D(config.target, i, config.internalFormat, width >> i, height >> i, config.depth >> i, 0, config.pixelFormat, config.pixelDataType, (ByteBuffer) null);
			} else if (config.target == GFX.GL_TEXTURE_2D_ARRAY) {
				GFX.texImage3D(config.target, i, config.internalFormat, width >> i, height >> i, config.depth, 0, config.pixelFormat, config.pixelDataType, (ByteBuffer) null);
			} else {
				GFX.texImage2D(config.target, i, config.internalFormat, width >> i, height >> i, 0, config.pixelFormat, config.pixelDataType, (ByteBuffer) null);
			}
		}
	}

	void close() {
		if (glId != -1) {
			TextureUtil.releaseTextureId(glId);
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
