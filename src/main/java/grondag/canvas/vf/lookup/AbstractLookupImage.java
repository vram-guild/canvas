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

package grondag.canvas.vf.lookup;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.GFX;

@Environment(EnvType.CLIENT)
public abstract class AbstractLookupImage {
	private final int textureUnit;
	private int imageFormat;
	protected final int texelCapacity;
	protected final int intsPerTexel;
	protected final int integerCapacity;
	protected final int[] values;

	private int bufferId;
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
		values = new int[texelCapacity * intsPerTexel];
		Arrays.fill(values, -1);
	}

	public synchronized void clear() {
		assert RenderSystem.isOnRenderThread();

		disableTexture();

		if (textureId != 0) {
			GFX.deleteTexture(textureId);
			textureId = 0;
		}

		if (bufferId != 0) {
			GFX.deleteBuffers(bufferId);
			bufferId = 0;
		}

		Arrays.fill(values, -1);
		isDirty.set(false);
		isActive = false;
	}

	final int bufferId() {
		return bufferId;
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
			GFX.texBuffer(imageFormat, bufferId);
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

		if (isDirty.compareAndSet(true, false)) {
			if (bufferId == 0) {
				bufferId = GFX.genBuffer();
			}

			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
			// Always respecify/orphan buffer
			GFX.bufferData(GFX.GL_TEXTURE_BUFFER, integerCapacity * 4, GFX.GL_STATIC_DRAW);

			final ByteBuffer bBuff = GFX.mapBuffer(GFX.GL_TEXTURE_BUFFER, GFX.GL_MAP_WRITE_BIT);
			bBuff.asIntBuffer().put(values);
			GFX.unmapBuffer(GFX.GL_TEXTURE_BUFFER);
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, 0);
		}
	}
}
