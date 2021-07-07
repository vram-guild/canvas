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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.GFX;

@Environment(EnvType.CLIENT)
public final class VfLookupImage {
	private final int textureUnit;
	private int imageFormat;
	private final int capacity;
	private final int[] values;

	private int bufferId;
	private int textureId;
	private int nextIndex = 0;
	private int inUseCount = 0;
	private boolean isDirty = false;
	private boolean isActive = false;

	boolean logging = false;

	/**
	 * Image format MUST be four bytes.
	 */
	public VfLookupImage(int textureUnit, int imageFormat, int capacity) {
		values = new int[capacity];
		Arrays.fill(values, -1);
		this.capacity = capacity;
		this.textureUnit = textureUnit;
		this.imageFormat = imageFormat;
	}

	public synchronized void clear() {
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
		isDirty = false;
		nextIndex = 0;
		isActive = false;
	}

	int bufferId() {
		return bufferId;
	}

	public void enableTexture() {
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

	public void disableTexture() {
		if (isActive) {
			isActive = false;
			CanvasTextureState.activeTextureUnit(textureUnit);
			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_BUFFER, 0);
			CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
		}
	}

	public synchronized int createIndexForValue(final int value) {
		if (inUseCount < capacity) {
			++inUseCount;
			isDirty = true;

			while (values[nextIndex] != -1) {
				if (++nextIndex >= capacity) {
					nextIndex = 0;
				}
			}

			final int result = nextIndex;
			values[result] = value;

			if (++nextIndex >= capacity) {
				nextIndex = 0;
			}

			return result;
		}

		assert false : "failure in createIndexForValue";
		return 0;
	}

	public synchronized void releaseIndex(final int index) {
		values[index] = -1;
	}

	public synchronized void changeValueForIndex(final int index, final int value) {
		assert value >= 0;
		values[index] = value;
		isDirty = true;
	}

	public synchronized void upload() {
		if (isDirty) {
			if (bufferId == 0) {
				bufferId = GFX.genBuffer();
			}

			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
			// Always respecify/orphan buffer
			GFX.bufferData(GFX.GL_TEXTURE_BUFFER, capacity, GFX.GL_STATIC_DRAW);

			final ByteBuffer bBuff = GFX.mapBuffer(GFX.GL_TEXTURE_BUFFER, GFX.GL_MAP_WRITE_BIT);
			bBuff.asIntBuffer().put(values);
			GFX.unmapBuffer(GFX.GL_TEXTURE_BUFFER);
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, 0);

			isDirty = false;
		}
	}
}
