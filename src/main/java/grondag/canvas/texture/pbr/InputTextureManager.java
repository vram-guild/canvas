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

package grondag.canvas.texture.pbr;

import java.io.InputStream;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;

import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import grondag.canvas.CanvasMod;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.GFX;

public class InputTextureManager {
	final Object2ObjectOpenHashMap<ResourceLocation, InputTexture> textures = new Object2ObjectOpenHashMap<>();
	private final InputTexture EMPTY_INPUT = new InputTexture();

	void addInputResource(ResourceManager resourceManager, ResourceLocation location) {
		if (!textures.containsKey(location)) {
			final var resource = resourceManager.getResource(location);
			InputTexture added = EMPTY_INPUT;

			if (resource.isPresent()) {
				try (InputStream inputStream = resource.get().open()) {
					added = new ImageInput(location, NativeImage.read(inputStream), true);
				} catch (Throwable e) {
					CanvasMod.LOG.error("Unable to load PBR texture " + location + " due to exception:\n" + e);
				}
			}

			textures.put(location, added);
		}
	}

	void addInputImage(ResourceLocation id, NativeImage image) {
		var location = SpriteSource.TEXTURE_ID_CONVERTER.idToFile(id);

		if (!textures.containsKey(location)) {
			textures.put(location, new ImageInput(location, image, false));
		}
	}

	InputTexture getInput(ResourceLocation location) {
		return textures.get(location);
	}

	InputTexture getSpriteDefault(ResourceLocation id) {
		var location = SpriteSource.TEXTURE_ID_CONVERTER.idToFile(id);

		return textures.get(location);
	}

	void uploadInputs() {
		GFX.pixelStore(GFX.GL_UNPACK_SKIP_PIXELS, 0);
		GFX.pixelStore(GFX.GL_UNPACK_SKIP_ROWS, 0);
		GFX.pixelStore(GFX.GL_UNPACK_ROW_LENGTH, 0);
		GFX.pixelStore(GFX.GL_UNPACK_ALIGNMENT, 1);

		for (var i : textures.values()) {
			i.upload();
		}
	}

	void clear() {
		for (var entry : textures.values()) {
			entry.close();
		}

		textures.clear();
	}

	static class InputTexture {
		boolean present() {
			return false;
		}

		int getTexId() {
			return -1;
		}

		protected void upload() {
		}

		protected void close() {
		}
	}

	static class ImageInput extends InputTexture {
		final ResourceLocation location;
		final NativeImage image;
		final boolean autoClose;
		private int texId = -1;

		public ImageInput(ResourceLocation location, NativeImage image, boolean autoClose) {
			this.location = location;
			this.image = image;
			this.autoClose = autoClose;
		}

		@Override
		public boolean present() {
			return texId != -1;
		}

		@Override
		public int getTexId() {
			return texId;
		}

		@Override
		protected void upload() {
			if (texId == -1) {
				texId = TextureUtil.generateTextureId();
			}

			CanvasTextureState.bindTexture(texId);
			image.upload(0, 0, 0, autoClose);
		}

		@Override
		protected void close() {
			if (texId != -1) {
				TextureUtil.releaseTextureId(texId);
				texId = -1;
			}

			if (autoClose) {
				image.close();
			}
		}
	}
}
