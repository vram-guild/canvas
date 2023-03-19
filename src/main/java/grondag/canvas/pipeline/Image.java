/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

			if(config.lod > 0) {
				GFX.texParameter(config.target, GFX.GL_TEXTURE_MIN_LOD, 0);
				GFX.texParameter(config.target, GFX.GL_TEXTURE_MAX_LOD, config.lod);
				GFX.texParameter(config.target, GFX.GL_TEXTURE_MAX_LEVEL, config.lod);
				GFX.texParameter(config.target, GFX.GL_TEXTURE_LOD_BIAS, 0.0F);
			}

			for (int i = 0; i <= config.lod; ++i) {
				if (config.target == GFX.GL_TEXTURE_3D) {
					GFX.texImage3D(config.target, i, config.internalFormat, width >> i, height >> i, config.depth >> i, 0, config.pixelFormat, config.pixelDataType, (ByteBuffer) null);
				} else if (config.target == GFX.GL_TEXTURE_2D_ARRAY) {
					GFX.texImage3D(config.target, i, config.internalFormat, width >> i, height >> i, config.depth, 0, config.pixelFormat, config.pixelDataType, (ByteBuffer) null);
				} else if (config.target == GFX.GL_TEXTURE_CUBE_MAP) {
					for (int face = 0; face < 6; ++face) {
						GFX.texImage2D(GFX.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, i, config.internalFormat, width >> i, height >> i, 0, config.pixelFormat, config.pixelDataType, (ByteBuffer) null);
					}
				} else {
					GFX.texImage2D(config.target, i, config.internalFormat, width >> i, height >> i, 0, config.pixelFormat, config.pixelDataType, (ByteBuffer) null);
				}
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
