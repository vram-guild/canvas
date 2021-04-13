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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.texture.Sprite;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.varia.GFX;

@Environment(EnvType.CLIENT)
public final class SpriteInfoImage implements AutoCloseable {
	public final int spriteCount;
	private final int sizeBytes;
	private long pointer;
	private ByteBuffer byteBuffer;
	private IntBuffer intBuffer;

	// FIX: make texture square to reduce chance of overrun/driver strangeness

	public SpriteInfoImage(ObjectArrayList<Sprite> spriteIndex, int spriteCount) {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: SpriteInfoImage init");
		}

		this.spriteCount = spriteCount;
		sizeBytes = spriteCount * 8;
		pointer = MemoryUtil.nmemAlloc(sizeBytes);
		byteBuffer = MemoryUtil.memByteBuffer(pointer, sizeBytes);
		intBuffer = byteBuffer.asIntBuffer();

		for (int i = 0; i < spriteCount; ++i) {
			final Sprite s = spriteIndex.get(i);
			setPixel(i, Math.round(s.getMinU() * 0x8000), Math.round(s.getMinV() * 0x8000),
					Math.round((s.getMaxU() - s.getMinU()) * 0x8000), Math.round((s.getMaxV() - s.getMinV()) * 0x8000));
		}
	}

	@Override
	public void close() {
		if (pointer != 0L) {
			intBuffer = null;
			byteBuffer = null;
			MemoryUtil.nmemFree(pointer);
			pointer = 0L;
		}
	}

	private void setPixel(int n, int minU, int minV, int spanU, int spanV) {
		assert n <= spriteCount;
		assert pointer != 0L : "Image not allocated.";
		n *= 2;
		intBuffer.put(n, minU | (minV << 16));
		intBuffer.put(n + 1, spanU | (spanV << 16));
	}

	public void upload(int bufferId) {
		assert pointer != 0L : "Image not allocated.";

		GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
		GFX.bufferData(GFX.GL_TEXTURE_BUFFER, byteBuffer, GFX.GL_STATIC_DRAW);
		GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, 0);

		GFX.texBuffer(GFX.GL_RGBA16UI, bufferId);
	}
}
