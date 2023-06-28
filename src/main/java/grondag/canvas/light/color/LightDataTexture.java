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

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.pipeline.Image;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.GFX;

public class LightDataTexture {
	public static class Format {
		public static int target = GFX.GL_TEXTURE_3D;
		public static int pixelBytes = 2;
		public static int internalFormat = GFX.GL_RGBA4;
		public static int pixelFormat = GFX.GL_RGBA;
		public static int pixelDataType = GFX.GL_UNSIGNED_SHORT_4_4_4_4;
	}

	private final Image image;

	LightDataTexture(Image image) {
		this.image = image;

		// ByteBuffer clearer = MemoryUtil.memAlloc(image.config.width * image.config.height * image.config.depth * Format.pixelBytes);
		//
		// while (clearer.position() < clearer.limit()) {
		// 	clearer.putShort((short) 0);
		// }

		// // clear?? NOTE: this is wrong
		// upload(0, 0, 0, clearer);

		// clearer.position(0);
		// MemoryUtil.memFree(clearer);
	}

	public void close() {
		// Image closing is already handled by pipeline manager
	}

	public void upload(int x, int y, int z, ByteBuffer buffer) {
		upload(x, y, z, LightRegionData.Const.WIDTH, buffer);
	}

	public void upload(int x, int y, int z, int regionSize, ByteBuffer buffer) {
		RenderSystem.assertOnRenderThread();

		CanvasTextureState.bindTexture(LightDataTexture.Format.target, image.glId());

		// Gotta clean up some states, otherwise will cause memory access violation
		GFX.pixelStore(GFX.GL_UNPACK_SKIP_PIXELS, 0);
		GFX.pixelStore(GFX.GL_UNPACK_SKIP_ROWS, 0);
		GFX.pixelStore(GFX.GL_UNPACK_ROW_LENGTH, 0);
		GFX.pixelStore(GFX.GL_UNPACK_ALIGNMENT, 2);

		// Importantly, reset the pointer without flip
		buffer.position(0);

		GFX.glTexSubImage3D(Format.target, 0, x, y, z, regionSize, regionSize, regionSize, Format.pixelFormat, Format.pixelDataType, buffer);
	}
}
