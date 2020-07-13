/*******************************************************************************
 * Copyright 2019, 2020 grondag
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package grondag.canvas.texture;

import java.nio.IntBuffer;
import java.util.Collection;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.mixinterface.SpriteAtlasTextureExt;
import grondag.canvas.mixinterface.SpriteExt;

@Environment(EnvType.CLIENT)
public final class SpriteInfoImage implements AutoCloseable {
	public final int size;
	private long pointer;
	private final int sizeBytes;
	private IntBuffer intBuffer;

	public SpriteInfoImage(SpriteAtlasTexture atlas) {
		final Collection<Sprite> sprites = ((SpriteAtlasTextureExt) atlas).canvas_sprites().values();

		size = sprites.size();
		sizeBytes = size * 8;
		pointer = MemoryUtil.nmemAlloc(sizeBytes);
		intBuffer = MemoryUtil.memIntBuffer(pointer, sizeBytes / 4);

		int id = 0;

		for (final Sprite s : sprites) {
			setPixelUnsignedShort(id, Math.round(s.getMinU() * 0xFFFF), Math.round(s.getMinV() * 0xFFFF),
					Math.round((s.getMaxU() - s.getMinU()) * 0xFFFF), Math.round((s.getMaxV() - s.getMinV()) * 0xFFFF));

			((SpriteExt) s).canvas_id(id++);
		}
	}

	@Override
	public void close() {
		if (pointer != 0L) {
			intBuffer = null;
			MemoryUtil.nmemFree(pointer);
		}

		pointer = 0L;
	}

	private void setPixelUnsignedShort(int n, int x, int y, int z, int w) {
		assert n <= size;
		assert pointer != 0L : "Image not allocated.";
		n *= 2;
		intBuffer.put(n, x | (y << 16));
		intBuffer.put(n + 1, z | (w << 16));
	}

	public void upload() {
		assert pointer != 0L : "Image not allocated.";
		GL21.glTexImage1D(GL11.GL_TEXTURE_1D, 0, GL21.GL_RGBA16, size, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_SHORT, pointer);
	}
}
