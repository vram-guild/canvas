/*
 * Copyright © Original Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.texture;

import java.nio.ByteBuffer;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.TextureUtil;

import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.GFX;

/**
 * Leaner adaptation of Minecraft NativeImageBackedTexture suitable for our needs.
 */
public class SimpleTexture implements AutoCloseable {
	protected int glId = -1;
	private SimpleImage image;

	public SimpleTexture(SimpleImage image, int internalFormat) {
		this.image = image;

		bindTexture();
		GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MAX_LEVEL, 0);
		GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MIN_LOD, 0);
		GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MAX_LOD, 0);
		GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_LOD_BIAS, 0.0F);
		GFX.texImage2D(GFX.GL_TEXTURE_2D, 0, internalFormat, image.width, image.height, 0, image.pixelDataFormat, image.pixelDataType, (ByteBuffer) null);
	}

	public int getGlId() {
		if (glId == -1) {
			glId = TextureUtil.generateTextureId();
		}

		return glId;
	}

	public void clearGlId() {
		if (glId != -1) {
			TextureUtil.releaseTextureId(glId);
			glId = -1;
		}
	}

	public void bindTexture() {
		CanvasTextureState.bindTexture(getGlId());
	}

	public void upload() {
		bindTexture();
		image.upload(0, 0, 0, false);
	}

	public void uploadPartial(int x, int y, int width, int height) {
		bindTexture();
		image.upload(0, x, y, x, y, width, height, false);
	}

	@Nullable
	public SimpleImage getImage() {
		return image;
	}

	public void setImage(SimpleImage image) throws Exception {
		this.image.close();
		this.image = image;
	}

	@Override
	public void close() {
		image.close();
		clearGlId();
		image = null;
	}
}
