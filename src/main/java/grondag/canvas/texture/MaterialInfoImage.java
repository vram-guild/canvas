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

import com.mojang.blaze3d.platform.GlStateManager;
import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.varia.CanvasGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.system.MemoryUtil;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Texture stores information about material.
 * Each pixel component is a signed 16-bit float.
 * The upload buffer takes 32-bit floats.
 *
 * Layout is as follows
 *
 * R: vertex program ID
 * G: fragment program ID
 * B: program flags - currently only GUI
 * A: reserved B
 *
 */
@Environment(EnvType.CLIENT)
public final class MaterialInfoImage {
	public final int squareSizePixels;
	private final int bufferSizeBytes;
	private long pointer;
	private FloatBuffer floatBuffer;
	private boolean dirty = true;

	public MaterialInfoImage(int squareSizePixels) {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: MaterialInfoImage init");
		}

		this.squareSizePixels = squareSizePixels;
		bufferSizeBytes = squareSizePixels * BUFFER_BYTES_PER_SPRITE;
		pointer = MemoryUtil.nmemAlloc(bufferSizeBytes);
		floatBuffer = MemoryUtil.memFloatBuffer(pointer, bufferSizeBytes / 4);
	}

	public void close() {
		if (pointer != 0L) {
			floatBuffer = null;
			MemoryUtil.nmemFree(pointer);
		}

		pointer = 0L;
	}

	void set(int materialIndex, int vertexId, int fragmentId, int programFlags, int reserved) {
		assert materialIndex <= squareSizePixels;
		assert pointer != 0L : "Image not allocated.";
		materialIndex *= 4;
		floatBuffer.put(materialIndex, vertexId);
		floatBuffer.put(materialIndex + 1, fragmentId);
		floatBuffer.put(materialIndex + 2, programFlags);
		floatBuffer.put(materialIndex + 3, reserved);
		dirty = true;
	}

	public void upload() {
		if (dirty) {
			dirty = false;
			assert pointer != 0L : "Image not allocated.";

			GlStateManager.pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
			GlStateManager.pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
			GlStateManager.pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
			GlStateManager.pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);

			GL21.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL21.GL_RGBA16, 4, squareSizePixels, 0, GL21.GL_RGBA, GL21.GL_FLOAT, pointer);
			assert CanvasGlHelper.checkError();
		}
	}

	// Four components per material, and four bytes per float
	private static final int BUFFER_BYTES_PER_SPRITE = 4 * 4;

}
