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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.GFX;

@Environment(EnvType.CLIENT)
public class MaterialIndexTexture {
	public static final int MAX_INDEX_COUNT = 0x10000;
	public static final int INTS_PER_MATERIAL = 2;
	public static final int BYTES_PER_MATERIAL = INTS_PER_MATERIAL * 4;
	public static final int BUFFER_SIZE_BYTES = BYTES_PER_MATERIAL * MAX_INDEX_COUNT;

	public static final int ATLAS_INTS_PER_MATERIAL = 4;
	public static final int ATLAS_BYTES_PER_MATERIAL = ATLAS_INTS_PER_MATERIAL * 4;
	public static final int ATLAS_BUFFER_SIZE_BYTES = ATLAS_BYTES_PER_MATERIAL * MAX_INDEX_COUNT;

	private int glId = 0;
	private MaterialIndexImage image = null;
	private final boolean isAtlas;

	private static MaterialIndexTexture active = null;

	MaterialIndexTexture(boolean isAtlas) {
		this.isAtlas = isAtlas;
	}

	public void reset() {
		if (Configurator.traceTextureLoad) {
			CanvasMod.LOG.info("Lifecycle Event: MaterialInfoTexture init");
		}

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

	public synchronized void set(int materialIndex, int vertexId, int fragmentId, int programFlags, int conditionId) {
		assert !isAtlas;

		createImageIfNeeded();

		if (image != null) {
			image.set(materialIndex, vertexId, fragmentId, programFlags, conditionId);
		}
	}

	public synchronized void set(int materialIndex, int vertexId, int fragmentId, int programFlags, int conditionId, TextureAtlasSprite sprite) {
		assert isAtlas;

		createImageIfNeeded();

		if (image != null) {
			image.set(materialIndex, vertexId, fragmentId, programFlags, conditionId, sprite);
		}
	}

	private void createImageIfNeeded() {
		if (image == null) {
			try {
				image = new MaterialIndexImage(isAtlas);
			} catch (final Exception e) {
				CanvasMod.LOG.warn("Unable to create material info texture due to error:", e);
				image = null;
			}
		}
	}

	public static void disable() {
		if (active != null) {
			active = null;
			CanvasTextureState.activeTextureUnit(TextureData.MATERIAL_INFO);
			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_BUFFER, 0);
			CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
		}
	}

	private void uploadAndActivate() {
		try {
			if (glId == 0) {
				glId = GFX.genTexture();
			}

			CanvasTextureState.activeTextureUnit(TextureData.MATERIAL_INFO);
			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_BUFFER, glId);

			image.upload();

			CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
		} catch (final Exception e) {
			CanvasMod.LOG.warn("Unable to create material info texture due to error:", e);

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
		if (active != this) {
			active = this;
			createImageIfNeeded();
			uploadAndActivate();
		}
	}
}
