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

package grondag.canvas.vf;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import it.unimi.dsi.fastutil.HashCommon;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.CanvasMod;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.GFX;

@Environment(EnvType.CLIENT)
public class VfTexture<T extends VfElement<T>> {
	protected final ConcurrentHashMap<T, T> MAP = new ConcurrentHashMap<>();
	private final int textureUnit;
	private final int intsPerElement;

	private int glId = 0;
	protected VfImage<T> image = null;
	private boolean active = false;
	private int imageFormat;
	private Class<T> clazz;

	protected final Function<T, T> mapFunc = k -> {
		createImageIfNeeded();
		T result = k.copy();
		image.add(result);
		return result;
	};

	public VfTexture(int textureUnit, int imageFormat, int intsPerElement, Class<T> clazz) {
		this.textureUnit = textureUnit;
		this.imageFormat = imageFormat;
		this.intsPerElement = intsPerElement;
		this.clazz = clazz;
	}

	public void clear() {
		disable();

		if (image != null) {
			image.clear();
			image = null;
		}

		if (glId != 0) {
			disable();
			GFX.deleteTexture(glId);
			glId = 0;
		}
	}

	protected void createImageIfNeeded() {
		if (image == null) {
			try {
				image = new VfImage<>(0x10000, intsPerElement, clazz);
			} catch (final Exception e) {
				CanvasMod.LOG.warn("Unable to create vf texture due to error:", e);
				image = null;
			}
		}
	}

	public void disable() {
		if (active) {
			active = false;
			CanvasTextureState.activeTextureUnit(textureUnit);
			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_BUFFER, 0);
			CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
		}
	}

	private void uploadAndActivate() {
		try {
			boolean recreate = image.upload();

			if (recreate && glId != 0) {
				GFX.deleteTexture(glId);
				glId = 0;
			}

			if (glId == 0) {
				glId = GFX.genTexture();
			}

			CanvasTextureState.activeTextureUnit(textureUnit);
			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_BUFFER, glId);
			GFX.texBuffer(imageFormat, image.bufferId());
			CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
		} catch (final Exception e) {
			CanvasMod.LOG.warn("Unable to create vf texture due to error:", e);

			if (image != null) {
				image.close();
				image = null;
			}

			if (glId != 0) {
				GFX.deleteTexture(glId);
				glId = 0;
			}
		}
	}

	public void enable() {
		if (!active) {
			active = true;
			createImageIfNeeded();
			uploadAndActivate();
		}
	}

	protected static int hash4(int c0, int c1, int c2, int c3) {
		return (((HashCommon.mix(c0) * 31 + HashCommon.mix(c1)) * 31) + HashCommon.mix(c2)) * 31 + HashCommon.mix(c3);
	}
}
