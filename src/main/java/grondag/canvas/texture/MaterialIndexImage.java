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

import it.unimi.dsi.fastutil.ints.IntArrayList;

import net.minecraft.client.texture.Sprite;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.util.GlBufferAllocator;
import grondag.canvas.config.Configurator;
import grondag.canvas.varia.GFX;

/**
 * Texture stores information about material.
 * Each pixel component is a signed 16-bit float.
 * The upload buffer takes 32-bit floats.
 *
 * <p>Layout is as follows
 *
 * <p>R: vertex program ID<br>
 * G: fragment program ID<br>
 * B: program flags - currently only GUI<br>
 * A: reserved B
 */
@Environment(EnvType.CLIENT)
public final class MaterialIndexImage {
	private int bufferId;
	private final IntArrayList data = new IntArrayList();
	private int head = 0;
	private int tail = 0;
	private final boolean isAtlas;

	public MaterialIndexImage(boolean isAtlas) {
		this.isAtlas = isAtlas;

		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: MaterialInfoImage init");
		}
	}

	public void close() {
		if (bufferId != 0) {
			GlBufferAllocator.releaseBuffer(bufferId, isAtlas ? MaterialIndexTexture.ATLAS_BUFFER_SIZE_BYTES : MaterialIndexTexture.BUFFER_SIZE_BYTES);
			bufferId = 0;
		}
	}

	synchronized void set(int materialIndex, int vertexId, int fragmentId, int programFlags, int conditionId) {
		assert !isAtlas;

		data.add(vertexId | (fragmentId << 16));
		data.add(programFlags | (conditionId << 16));

		final int bufferIndex = materialIndex * MaterialIndexTexture.BYTES_PER_MATERIAL;
		assert bufferIndex >= head;
		assert bufferIndex == tail;
		tail = bufferIndex + MaterialIndexTexture.BYTES_PER_MATERIAL;
	}

	synchronized void set(int materialIndex, int vertexId, int fragmentId, int programFlags, int conditionId, Sprite sprite) {
		assert isAtlas;

		data.add(vertexId | (fragmentId << 16));
		data.add(programFlags | (conditionId << 16));
		data.add(Math.round(sprite.getMinU() * 0x8000) | (Math.round(sprite.getMinV() * 0x8000) << 16));
		data.add(Math.round((sprite.getMaxU() - sprite.getMinU()) * 0x8000) | (Math.round((sprite.getMaxV() - sprite.getMinV()) * 0x8000) << 16));

		final int bufferIndex = materialIndex * MaterialIndexTexture.ATLAS_BYTES_PER_MATERIAL;
		assert bufferIndex >= head;
		assert bufferIndex == tail;
		tail = bufferIndex + MaterialIndexTexture.ATLAS_BYTES_PER_MATERIAL;
	}

	// PERF: Should only be called on render thread but data updates can occur on other threads
	// so we currently sync here and in the set methods, even though set calls are also synchronized by caller.
	public synchronized void upload() {
		final int len = tail - head;
		assert len / 4 == data.size();

		if (len != 0) {
			if (bufferId == 0) {
				final int size = isAtlas ? MaterialIndexTexture.ATLAS_BUFFER_SIZE_BYTES : MaterialIndexTexture.BUFFER_SIZE_BYTES;
				bufferId = GlBufferAllocator.claimBuffer(size);
				GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
				GFX.bufferData(GFX.GL_TEXTURE_BUFFER, size, GFX.GL_STATIC_DRAW);
				GFX.texBuffer(GFX.GL_RGBA16UI, bufferId);
			} else {
				GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
			}

			final ByteBuffer bBuff = GFX.mapBufferRange(GFX.GL_TEXTURE_BUFFER, head, len, GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT);

			if (bBuff != null) {
				final IntBuffer iBuff = bBuff.asIntBuffer();
				iBuff.put(data.elements(), 0, len / 4);
				data.clear();
				head = tail;
			}

			GFX.flushMappedBufferRange(GFX.GL_TEXTURE_BUFFER, 0, len);
			GFX.unmapBuffer(GFX.GL_TEXTURE_BUFFER);

			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, 0);
		}
	}
}
