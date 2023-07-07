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

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;

import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.GFX;

public class PbrMapAtlas {

	static final int GL_TARGET = GFX.GL_TEXTURE_2D_ARRAY;
	static final int GL_INTERNAL_FORMAT = GFX.GL_RGBA8;
	static final int GL_FORMAT = GFX.GL_RGBA;
	static final int GL_TYPE = GFX.GL_UNSIGNED_BYTE;

	final ResourceLocation location;
	final ArrayList<PbrMapSprite> sprites = new ArrayList<>();
	final int width;
	final int height;
	final int lod;
	int texId;

	public PbrMapAtlas(ResourceLocation atlasLocation, int width, int height, int lod) {
		this.location = atlasLocation;
		this.width = width;
		this.height = height;
		this.lod = lod;
	}

	void processAndUpload() {
		for (var sprite : sprites) {
			sprite.process();
		}

		GFX.pixelStore(GFX.GL_PACK_SKIP_PIXELS, 0);
		GFX.pixelStore(GFX.GL_PACK_SKIP_ROWS, 0);
		GFX.pixelStore(GFX.GL_PACK_ROW_LENGTH, 0);

		for (var sprite : sprites) {
			sprite.download();
		}

		texId = TextureUtil.generateTextureId();
		CanvasTextureState.bindTexture(GL_TARGET, texId);
		GFX.objectLabel(GFX.GL_TEXTURE, texId, "IMG " + labelAtlas());

		GFX.texParameter(GL_TARGET, GFX.GL_TEXTURE_MIN_FILTER, GFX.GL_NEAREST);
		GFX.texParameter(GL_TARGET, GFX.GL_TEXTURE_MAG_FILTER, GFX.GL_NEAREST);
		GFX.texParameter(GL_TARGET, GFX.GL_TEXTURE_WRAP_S, GFX.GL_CLAMP_TO_EDGE);
		GFX.texParameter(GL_TARGET, GFX.GL_TEXTURE_WRAP_T, GFX.GL_CLAMP_TO_EDGE);

		// if (lod > 0) {
		// 	GFX.texParameter(GL_TARGET, GFX.GL_TEXTURE_MIN_LOD, 0);
		// 	GFX.texParameter(GL_TARGET, GFX.GL_TEXTURE_MAX_LOD, lod);
		// 	GFX.texParameter(GL_TARGET, GFX.GL_TEXTURE_MAX_LEVEL, lod);
		// 	GFX.texParameter(GL_TARGET, GFX.GL_TEXTURE_LOD_BIAS, 0.0F);
		// }

		GFX.pixelStore(GFX.GL_UNPACK_SKIP_PIXELS, 0);
		GFX.pixelStore(GFX.GL_UNPACK_SKIP_ROWS, 0);
		GFX.pixelStore(GFX.GL_UNPACK_ROW_LENGTH, 0);
		GFX.pixelStore(GFX.GL_UNPACK_ALIGNMENT, 4);

		final int layerCount = PbrMapAtlasLayer.values().length;

		// prepare. necessary?
		GFX.texImage3D(GL_TARGET, 0, GL_INTERNAL_FORMAT, width, height, layerCount, 0, GL_FORMAT, GL_TYPE, null);

		// TODO: lod, CPU side or GPU side?
		final int w = width;
		final int h = height;

		for (var atlasLayer : PbrMapAtlasLayer.values()) {
			ByteBuffer pixels = MemoryUtil.memAlloc(width * height * 4 * layerCount);

			for (var sprite : sprites) {
				for (int y = 0; y < sprite.height; y++) {
					pixels.position(sprite.x * 4 + ((sprite.y + y) * width * 4));

					for (int x = 0; x < sprite.width; x++) {
						// pixels.putInt(ColorEncode.encode(sprite.r(PbrMapSpriteLayer.NORMAL, x, y), sprite.g(PbrMapSpriteLayer.NORMAL, x, y), sprite.b(PbrMapSpriteLayer.NORMAL, x, y), 255));
						pixels.putInt(ColorEncode.encode(atlasLayer.swizzler.r(sprite, x, y), atlasLayer.swizzler.g(sprite, x, y), atlasLayer.swizzler.b(sprite, x, y), atlasLayer.swizzler.a(sprite, x, y)));
						// pixels.putInt(ColorEncode.encode(atlasLayer.swizzler.r(sprite, x, y), atlasLayer.swizzler.g(sprite, x, y), atlasLayer.swizzler.b(sprite, x, y), 255));
					}
				}
			}

			pixels.position(0);
			GFX.glTexSubImage3D(PbrMapAtlas.GL_TARGET, 0, 0, 0, atlasLayer.layer, w, h, 1, PbrMapAtlas.GL_FORMAT, PbrMapAtlas.GL_TYPE, pixels);

			MemoryUtil.memFree(pixels);
		}

		for (var sprite : sprites) {
			sprite.close();
		}

		// debug output
		if (Configurator.debugSpriteAtlas) {
			debugOutput();
		}
	}

	public void debugOutput() {
		RenderSystem.assertOnRenderThreadOrInit();

		final int layerCount = PbrMapAtlasLayer.values().length;
		final ByteBuffer debugPixels = MemoryUtil.memAlloc(width * height * 4 * layerCount);
		CanvasTextureState.bindTexture(GL_TARGET, texId);
		GFX.pixelStore(GFX.GL_PACK_SKIP_PIXELS, 0);
		GFX.pixelStore(GFX.GL_PACK_SKIP_ROWS, 0);
		GFX.pixelStore(GFX.GL_PACK_ROW_LENGTH, 0);
		GFX.pixelStore(GFX.GL_PACK_ALIGNMENT, 4);
		GFX.glGetTexImage(GL_TARGET, 0, GL_FORMAT, GL_TYPE, debugPixels);

		Util.ioPool().execute(() -> {
			NativeImage[] images = new NativeImage[layerCount];

			try {
				int i = 0;

				for (var layer : PbrMapAtlasLayer.values()) {
					var location = label(layer);
					final Path path = Minecraft.getInstance().gameDirectory.toPath().normalize().resolve("atlas_debug");
					final File atlasDir = path.toFile();

					if (!atlasDir.exists()) {
						atlasDir.mkdir();
						CanvasMod.LOG.info("Created atlas debug output folder" + atlasDir.toString());
					}

					final File file = new File(atlasDir.getAbsolutePath() + File.separator + location.replaceAll("[_/:]", "_"));
					images[i] = new NativeImage(width, height, false);

					for (int x = 0; x < width; x++) {
						for (int y = 0; y < height; y++) {
							int pixel = debugPixels.getInt((i * width * height * 4) + x * 4 + width * y * 4);
							// we have faith in NativeImage byte ordering as the benchmark
							images[i].setPixelRGBA(x, y, pixel);
						}
					}

					images[i].writeToFile(file);

					i++;
				}

			} catch (final Exception e) {
				CanvasMod.LOG.warn("Couldn't save atlas image", e);
			} finally {
				for (int i = 0; i < layerCount; i++) {
					if (images[i] != null) {
						images[i].close();
					}
				}

				MemoryUtil.memFree(debugPixels);
			}
		});
	}

	String label(PbrMapAtlasLayer layer) {
		return location.toString().replace(".png", "") + "_" + layer.name().toLowerCase(Locale.ROOT) + ".png";
	}

	String labelAtlas() {
		return location.toString().replace(".png", "") + "_pbr.png";
	}
}
