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
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryUtil;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.buffer.util.GlBufferAllocator;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.GFX;

@Environment(EnvType.CLIENT)
public abstract class AbstractLookupImage {
	private final int textureUnit;
	private int imageFormat;
	protected final int texelCapacity;
	protected final int intsPerTexel;
	protected final int integerCapacity;
	protected ByteBuffer byteValues;
	protected IntBuffer intValues;

	private int bufferIdA;
	private int bufferIdB;
	private int currentBufferId;
	private boolean isCurrentB = true;
	private int textureId;
	protected AtomicBoolean isDirty = new AtomicBoolean(false);
	private boolean isActive = false;

	boolean logging = false;

	/**
	 * Image format MUST be four bytes.
	 */
	public AbstractLookupImage(int textureUnit, int imageFormat, int texelCapacity, int intsPerTexel) {
		this.textureUnit = textureUnit;
		this.imageFormat = imageFormat;
		this.texelCapacity = texelCapacity;
		this.intsPerTexel = intsPerTexel;
		integerCapacity = texelCapacity * intsPerTexel;
		byteValues = MemoryUtil.memAlloc(integerCapacity * 4);
		intValues = byteValues.asIntBuffer();

		for (int i = 0; i < integerCapacity; ++i) {
			intValues.put(i, 0);
		}
	}

	public synchronized void clear() {
		assert RenderSystem.isOnRenderThread();

		disableTexture();

		if (textureId != 0) {
			GFX.deleteTexture(textureId);
			textureId = 0;
		}

		if (bufferIdA != 0) {
			assert bufferIdB != 0;
			GlBufferAllocator.releaseBuffer(bufferIdA, integerCapacity * 4);
			GlBufferAllocator.releaseBuffer(bufferIdB, integerCapacity * 4);
			bufferIdA = 0;
			bufferIdB = 0;
		}

		for (int i = 0; i < integerCapacity; ++i) {
			intValues.put(i, 0);
		}

		currentBufferId = 0;
		isDirty.set(false);
		isActive = false;
		isCurrentB = true;
	}

	final int bufferId() {
		return currentBufferId;
	}

	public final void enableTexture() {
		assert RenderSystem.isOnRenderThread();

		if (!isActive) {
			isActive = true;

			if (textureId == 0) {
				textureId = GFX.genTexture();
			}

			CanvasTextureState.activeTextureUnit(textureUnit);
			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_BUFFER, textureId);
			GFX.texBuffer(imageFormat, currentBufferId);
			CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
		}
	}

	public final void disableTexture() {
		assert RenderSystem.isOnRenderThread();

		if (isActive) {
			isActive = false;
			CanvasTextureState.activeTextureUnit(textureUnit);
			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_BUFFER, 0);
			CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
		}
	}

	public final void upload() {
		assert RenderSystem.isOnRenderThread();

		if (bufferIdA == 0) {
			assert bufferIdB == 0;
			bufferIdA = GlBufferAllocator.claimBuffer(integerCapacity * 4);
			bufferIdB = GlBufferAllocator.claimBuffer(integerCapacity * 4);

			// PERF: any benefit to a different hint here or different approach?
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferIdA);
			GFX.bufferData(GFX.GL_TEXTURE_BUFFER, byteValues, GFX.GL_DYNAMIC_DRAW);

			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferIdB);
			GFX.bufferData(GFX.GL_TEXTURE_BUFFER, byteValues, GFX.GL_DYNAMIC_DRAW);
		} else if (isDirty.compareAndSet(true, false)) {
			if (isCurrentB) {
				isCurrentB = false;
				currentBufferId = bufferIdA;
			} else {
				isCurrentB = true;
				currentBufferId = bufferIdB;
			}

			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, currentBufferId);
			GL46C.glBufferSubData(GFX.GL_TEXTURE_BUFFER, 0, byteValues);
		}
	}
}
