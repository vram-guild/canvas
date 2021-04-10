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

import org.lwjgl.system.MemoryUtil;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.varia.GFX;

/**
 * Buffer Texture stores information about material state.
 *
 * <p>Layout is as follows
 *
 * <p>R: vertex program ID<br>
 * G: fragment program ID<br>
 * B: program flags - currently only GUI<br>
 * A: reserved B
 */
@Environment(EnvType.CLIENT)
public final class MaterialInfoBuffer {
	// vertex program id
	// fragment program id
	// condition id
	// bits
	//   emissive
	//   diffuse
	//   ao
	//   cutout (3 bits)
	//   mipped
	//   hurt
	//   flash
	//   fog
	//   glint
	//   shadows

	public static final int MAX_MATERIAL_COUNT = 0x10000;
	public static final int BYTES_PER_MATERIAL = 2 * 4;
	public static final int BUFFER_SIZE_BYTES = BYTES_PER_MATERIAL * MAX_MATERIAL_COUNT;
	private long pointer;
	private final ByteBuffer byteBuffer;
	private IntBuffer intBuffer;
	private boolean dirty = true;
	private int textureId = 0;

	public MaterialInfoBuffer() {
		pointer = MemoryUtil.nmemAlloc(BUFFER_SIZE_BYTES);
		byteBuffer = MemoryUtil.memByteBuffer(pointer, BUFFER_SIZE_BYTES);
		intBuffer = byteBuffer.asIntBuffer();
	}

	public void close() {
		if (pointer != 0L) {
			intBuffer = null;
			MemoryUtil.nmemFree(pointer);
			pointer = 0L;
		}

		if (textureId != 0) {
			GFX.deleteTexture(textureId);
			textureId = 0;
		}
	}

	void set(int materialIndex, int vertexId, int fragmentId, int programFlags, int conditionId) {
		assert pointer != 0L : "Image not allocated.";
		materialIndex *= 2;
		intBuffer.put(materialIndex, vertexId | (fragmentId << 16));
		intBuffer.put(materialIndex + 1, programFlags | (conditionId << 16));
		dirty = true;
	}

	public void upload() {
		if (dirty) {
			dirty = false;
			assert pointer != 0L : "Image not allocated.";

			final int buff = GFX.genBuffer();
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, buff);
			GFX.bufferData(GFX.GL_TEXTURE_BUFFER, byteBuffer, GFX.GL_DYNAMIC_DRAW);
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, 0);

			if (textureId == 0) {
				textureId = GFX.genTexture();
			}

			GFX.bindTexture(GFX.GL_TEXTURE_BUFFER, textureId);
			GFX.texBuffer(GFX.GL_RGBA16UI, buff);
		}
	}
}
