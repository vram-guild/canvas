/*
 * Copyright 2019, 2020 grondag
 *
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
 */

package grondag.canvas.texture;

import java.nio.FloatBuffer;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.varia.CanvasGlHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.texture.Sprite;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SpriteInfoImage implements AutoCloseable {
	public final int size;
	private final int sizeBytes;
	private long pointer;
	private FloatBuffer floatBuffer;

	// FIX: make texture square to reduce chance of overrun/driver strangeness

	public SpriteInfoImage(ObjectArrayList<Sprite> spriteIndex, int spriteCount, int size) {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: SpriteInfoImage init");
		}

		this.size = size;

		// 16 because 4 floats per vector, 4 because 4 samples per sprite
		sizeBytes = size * 16 * 4;
		pointer = MemoryUtil.nmemAlloc(sizeBytes);
		floatBuffer = MemoryUtil.memFloatBuffer(pointer, sizeBytes / 4);

		for (int i = 0; i < spriteCount; ++i) {
			final Sprite s = spriteIndex.get(i);
			setPixel(i, s.getMinU(), s.getMinV(), s.getMaxU() - s.getMinU(), s.getMaxV() - s.getMinV());
		}
	}

	@Override
	public void close() {
		if (pointer != 0L) {
			floatBuffer = null;
			MemoryUtil.nmemFree(pointer);
		}

		pointer = 0L;
	}

	private void setPixel(int n, float x, float y, float z, float w) {
		assert n <= size;
		assert pointer != 0L : "Image not allocated.";
		n *= 16;
		floatBuffer.put(n, x);
		floatBuffer.put(n + 1, y);
		floatBuffer.put(n + 2, z);
		floatBuffer.put(n + 3, w);
	}

	public void upload() {
		assert pointer != 0L : "Image not allocated.";
		GL21.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL21.GL_RGBA16, 4, size, 0, GL21.GL_RGBA, GL21.GL_FLOAT, pointer);

		if (!CanvasGlHelper.checkError()) {
			CanvasMod.LOG.warn("Unable to upload material information texture due to unexpected OpenGL error. Game may crash or render incorrectly.");
		}
	}
}
