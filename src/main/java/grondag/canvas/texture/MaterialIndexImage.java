/*
 * Copyright © Contributing Authors
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

import it.unimi.dsi.fastutil.ints.IntArrayList;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.buffer.render.TransferBuffers;
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

	synchronized void set(int materialIndex, int vertexId, int fragmentId, int programFlags, int conditionId, TextureAtlasSprite sprite) {
		assert isAtlas;

		data.add(vertexId | (fragmentId << 16));
		data.add(programFlags | (conditionId << 16));
		data.add(Math.round(sprite.getU0() * 0x8000) | (Math.round(sprite.getV0() * 0x8000) << 16));
		data.add(Math.round((sprite.getU1() - sprite.getU0()) * 0x8000) | (Math.round((sprite.getV1() - sprite.getV0()) * 0x8000) << 16));

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
			final TransferBuffer xferBuff = TransferBuffers.claim(len);
			xferBuff.put(data.elements(), 0, 0, len / 4);
			data.clear();

			if (bufferId == 0) {
				final int size = isAtlas ? MaterialIndexTexture.ATLAS_BUFFER_SIZE_BYTES : MaterialIndexTexture.BUFFER_SIZE_BYTES;
				bufferId = GlBufferAllocator.claimBuffer(size);
				GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
				GFX.bufferData(GFX.GL_TEXTURE_BUFFER, size, GFX.GL_STATIC_DRAW);
				GFX.texBuffer(GFX.GL_RGBA16UI, bufferId);
			} else {
				GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
			}

			xferBuff.releaseToBoundBuffer(GFX.GL_TEXTURE_BUFFER, head);
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, 0);
			head = tail;
		}
	}
}
