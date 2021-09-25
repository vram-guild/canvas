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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.mojang.blaze3d.platform.DebugMemoryUntracker;
import org.lwjgl.system.MemoryUtil;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.varia.GFX;

/**
 * Leaner adaptation of Minecraft NativeImage suitable for our needs.
 */
@Environment(EnvType.CLIENT)
public final class SimpleImage implements AutoCloseable {
	public final int width;
	public final int height;
	public final int bytesPerPixel;
	public final int pixelDataFormat;
	public final int pixelDataType = GFX.GL_UNSIGNED_BYTE;
	private final int sizeBytes;
	private long pointer;
	private ByteBuffer byteBuffer;
	private IntBuffer intBuffer;

	public SimpleImage(int bytesPerPixel, int pixelDataFormat, int width, int height, boolean calloc) {
		this.bytesPerPixel = bytesPerPixel;
		this.pixelDataFormat = pixelDataFormat;
		this.width = width;
		this.height = height;
		sizeBytes = width * height * bytesPerPixel;

		if (calloc) {
			pointer = MemoryUtil.nmemCalloc(1L, sizeBytes);
		} else {
			pointer = MemoryUtil.nmemAlloc(sizeBytes);
		}

		byteBuffer = MemoryUtil.memByteBuffer(pointer, sizeBytes);
		intBuffer = MemoryUtil.memIntBuffer(pointer, sizeBytes / 4);
	}

	private static void setTextureClamp(boolean clamp) {
		final int wrap = clamp ? GFX.GL_CLAMP_TO_EDGE : GFX.GL_REPEAT;
		GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_WRAP_S, wrap);
		GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_WRAP_T, wrap);
	}

	private static void setTextureFilter(boolean interpolate, boolean mipmap) {
		if (interpolate) {
			GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MIN_FILTER, mipmap ? GFX.GL_LINEAR_MIPMAP_LINEAR : GFX.GL_LINEAR);
			GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MAG_FILTER, GFX.GL_LINEAR);
		} else {
			GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MIN_FILTER, mipmap ? GFX.GL_NEAREST_MIPMAP_LINEAR : GFX.GL_NEAREST);
			GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MAG_FILTER, GFX.GL_NEAREST);
		}
	}

	@Override
	public void close() {
		if (pointer != 0L) {
			byteBuffer = null;
			intBuffer = null;
			MemoryUtil.nmemFree(pointer);
		}

		pointer = 0L;
	}

	public int getPixelRGBA(int x, int y) {
		assert bytesPerPixel == 4;
		assert x <= width && y <= height;
		assert pointer != 0L : "Image not allocated.";
		return intBuffer.get(x + y * width);
	}

	public void setPixelRGBA(int x, int y, int rgba) {
		assert bytesPerPixel == 4;
		assert x <= width && y <= height;
		assert pointer != 0L : "Image not allocated.";
		intBuffer.put(x + y * width, rgba);
	}

	public void setLuminance(int u, int v, byte value) {
		assert bytesPerPixel == 1;
		assert u <= width && v <= height;
		assert pointer != 0L : "Image not allocated.";
		byteBuffer.put(u + v * width, value);
	}

	public void clear(byte value) {
		assert pointer != 0L : "Image not allocated.";

		final int limit;

		if (bytesPerPixel == 1) {
			limit = width * height;
		} else {
			assert bytesPerPixel == 4;
			limit = width * height * 4;
		}

		for (int i = 0; i < limit; i++) {
			byteBuffer.put(i, value);
		}
	}

	public void upload(int lod, int x, int y, boolean clamp) {
		this.upload(lod, x, y, 0, 0, width, height, clamp);
	}

	public void upload(int lod, int x, int y, int skipPixels, int skipRows, int width, int height, boolean clamp) {
		this.upload(lod, x, y, skipPixels, skipRows, width, height, false, false, clamp);
	}

	public void upload(int lod, int x, int y, int skipPixels, int skipRows, int width, int height, boolean interpolate, boolean clamp, boolean mipmap) {
		assert pointer != 0L : "Image not allocated.";
		setTextureFilter(interpolate, mipmap);
		setTextureClamp(clamp);
		GFX.pixelStore(GFX.GL_UNPACK_ALIGNMENT, bytesPerPixel);
		GFX.pixelStore(GFX.GL_UNPACK_ROW_LENGTH, this.width);
		GFX.pixelStore(GFX.GL_UNPACK_SKIP_PIXELS, skipPixels);
		GFX.pixelStore(GFX.GL_UNPACK_SKIP_ROWS, skipRows);
		GFX.texSubImage2D(GFX.GL_TEXTURE_2D, lod, x, y, width, height, pixelDataFormat, pixelDataType, pointer);

		// reset defaults to avoid leaking unexpected / unhandled state
		GFX.pixelStore(GFX.GL_UNPACK_ALIGNMENT, 4);
		GFX.pixelStore(GFX.GL_UNPACK_ROW_LENGTH, 0);
		GFX.pixelStore(GFX.GL_UNPACK_SKIP_PIXELS, 0);
		GFX.pixelStore(GFX.GL_UNPACK_SKIP_ROWS, 0);
	}

	public void untrack() {
		DebugMemoryUntracker.untrack(pointer);
	}
}
