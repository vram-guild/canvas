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

package grondag.canvas.texture.pbr;

import java.nio.ByteBuffer;

import grondag.canvas.varia.GFX;

public enum PbrMapSpriteLayer {
	NORMAL(ColorEncode.encode(128, 128, 255, 0), 1, 3, GFX.GL_RGB8, GFX.GL_RGB, GFX.GL_UNSIGNED_BYTE),
	HEIGHT(ColorEncode.encode(128, 0, 0, 0), 1, 1, GFX.GL_R8, GFX.GL_RED, GFX.GL_UNSIGNED_BYTE),
	REFLECTANCE(ColorEncode.encode(10, 0, 0, 0), 1, 1, GFX.GL_R8, GFX.GL_RED, GFX.GL_UNSIGNED_BYTE),
	ROUGHNESS(ColorEncode.encode(255, 0, 0, 0), 1, 1, GFX.GL_R8, GFX.GL_RED, GFX.GL_UNSIGNED_BYTE),
	EMISSIVE(ColorEncode.encode(0, 0, 0, 0), 1, 1, GFX.GL_R8, GFX.GL_RED, GFX.GL_UNSIGNED_BYTE),
	AO(ColorEncode.encode(255, 0, 0, 0), 1, 1, GFX.GL_R8, GFX.GL_RED, GFX.GL_UNSIGNED_BYTE);

	public final int defaultValue;
	public final int alignment;
	public final int bytes;
	public final int glInternalFormat;
	public final int glFormat;
	public final int glPixelDataType;

	PbrMapSpriteLayer(int defaultValue, int alignment, int bytes, int glInternalFormat, int glFormat, int glPixelDataType) {
		this.defaultValue = defaultValue;
		this.alignment = alignment;
		this.bytes = bytes;
		this.glInternalFormat = glInternalFormat;
		this.glFormat = glFormat;
		this.glPixelDataType = glPixelDataType;
	}

	public void drawDefault(ByteBuffer buffer) {
		buffer.position(0);

		while (buffer.position() < buffer.limit() - 4) {
			buffer.putInt(defaultValue);
		}
	}

	public interface LayeredImage {
		int r(PbrMapSpriteLayer layer, int x, int y);

		int g(PbrMapSpriteLayer layer, int x, int y);

		int b(PbrMapSpriteLayer layer, int x, int y);

		int a(PbrMapSpriteLayer layer, int x, int y);
	}
}
