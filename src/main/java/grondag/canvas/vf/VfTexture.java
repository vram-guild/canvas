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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.CanvasMod;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.GFX;

@Environment(EnvType.CLIENT)
public class VfTexture {
	private final int textureUnit;
	private int glId = 0;
	protected VfImage image = null;
	private boolean active = false;

	public VfTexture(int textureUnit) {
		this.textureUnit = textureUnit;
	}

	public void reset() {
		disable();

		if (image != null) {
			image.close();
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
				image = new VfImage();
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
			GFX.texBuffer(GFX.GL_RGBA32UI, image.bufferId());
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
}
