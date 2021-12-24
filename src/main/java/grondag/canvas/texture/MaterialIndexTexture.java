/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.texture;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.GFX;

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
