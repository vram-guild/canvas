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

package grondag.canvas.light.color;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.GFX;

public class LightDataTexture {
	public static class Format {
		public static int TARGET = GFX.GL_TEXTURE_2D;
		public static int PIXEL_BYTES = 2;
		public static int INTERNAL_FORMAT = GFX.GL_RGBA4;
		public static int PIXEL_FORMAT = GFX.GL_RGBA;
		public static int TYPE = GFX.GL_UNSIGNED_SHORT_4_4_4_4;
	}

	private final int glId;
	private final int width;
	private boolean closed = false;

	LightDataTexture(int width, int height) {
		this.width = width;

		glId = TextureUtil.generateTextureId();
		CanvasTextureState.bindTexture(glId);

		GFX.objectLabel(GL11.GL_TEXTURE, glId, "IMG auto_colored_lights");

		GFX.texParameter(Format.TARGET, GFX.GL_TEXTURE_MIN_FILTER, GFX.GL_NEAREST);
		GFX.texParameter(Format.TARGET, GFX.GL_TEXTURE_MAG_FILTER, GFX.GL_NEAREST);
		GFX.texParameter(Format.TARGET, GFX.GL_TEXTURE_WRAP_S, GFX.GL_CLAMP_TO_EDGE);
		GFX.texParameter(Format.TARGET, GFX.GL_TEXTURE_WRAP_T, GFX.GL_CLAMP_TO_EDGE);

		GFX.texImage2D(Format.TARGET, 0, Format.INTERNAL_FORMAT, width, height, 0, Format.PIXEL_FORMAT, Format.TYPE, (ByteBuffer) null);
	}

	public int texId() {
		if (closed) {
			return 0;
		}

		return glId;
	}

	public void close() {
		if (closed) {
			return;
		}

		TextureUtil.releaseTextureId(glId);

		closed = true;
	}

	public void upload(int row, ByteBuffer buffer) {
		upload(row, 1, buffer);
	}

	public void upload(int rowStart, int rowCount, ByteBuffer buffer) {
		uploadDirect(0, rowStart, width, rowCount, buffer);
	}

	public void uploadDirect(int x, int y, int width, int height, ByteBuffer buffer) {
		if (closed) {
			throw new IllegalStateException("Uploading to a closed light texture!");
		}

		RenderSystem.assertOnRenderThread();

		CanvasTextureState.bindTexture(glId);

		// Gotta clean up some states, otherwise will cause memory access violation
		GFX.pixelStore(GFX.GL_UNPACK_SKIP_PIXELS, 0);
		GFX.pixelStore(GFX.GL_UNPACK_SKIP_ROWS, 0);
		GFX.pixelStore(GFX.GL_UNPACK_ROW_LENGTH, 0);
		GFX.pixelStore(GFX.GL_UNPACK_ALIGNMENT, 2);

		// Importantly, reset the pointer without flip
		buffer.position(0);

		GFX.glTexSubImage2D(Format.TARGET, 0, x, y, width, height, Format.PIXEL_FORMAT, Format.TYPE, buffer);
	}
}
