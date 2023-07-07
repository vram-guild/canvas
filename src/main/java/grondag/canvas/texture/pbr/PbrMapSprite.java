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

import java.nio.ByteBuffer;
import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.TextureUtil;

import net.minecraft.resources.ResourceLocation;

import grondag.canvas.CanvasMod;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.GFX;

public class PbrMapSprite implements PbrMapSpriteLayer.LayeredImage {
	private final ResourceLocation atlasLocation;
	private final ResourceLocation location;
	final int x;
	final int y;
	final int width;
	final int height;
	private final Map<PbrMapSpriteLayer, ProcessInfo> processMap = new Object2ObjectOpenHashMap<>();
	private final Map<PbrMapSpriteLayer, ByteBuffer> pixelMap = new Object2ObjectOpenHashMap<>();
	private boolean processed = false;
	private boolean downloaded = false;
	private boolean closed = false;

	public PbrMapSprite(ResourceLocation atlasLocation, ResourceLocation location, int x, int y, int width, int height) {
		this.atlasLocation = atlasLocation;
		this.location = location;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	void withProcess(PbrMapSpriteLayer layer, PbrProcess process, InputTextureManager.InputTexture[] inputTexture) {
		if (processMap.containsKey(layer)) {
			CanvasMod.LOG.warn("Not applying duplicate PBR processor for sprite " + location + " on layer " + layer.name());
		} else {
			int pbrOutput = TextureUtil.generateTextureId();
			CanvasTextureState.bindTexture(pbrOutput);

			GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MIN_FILTER, GFX.GL_NEAREST);
			GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MAG_FILTER, GFX.GL_NEAREST);
			GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_WRAP_S, GFX.GL_CLAMP_TO_EDGE);
			GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_WRAP_T, GFX.GL_CLAMP_TO_EDGE);

			// if (lod > 0) {
			// 	GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MIN_LOD, 0);
			// 	GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MAX_LOD, lod);
			// 	GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MAX_LEVEL, lod);
			// 	GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_LOD_BIAS, 0.0F);
			// }

			GFX.texImage2D(GFX.GL_TEXTURE_2D, 0, layer.glInternalFormat, width, height, 0, layer.glFormat, layer.glPixelDataType, (ByteBuffer) null);

			processMap.put(layer, new ProcessInfo(process, inputTexture, pbrOutput));
		}
	}

	void process() {
		if (processed) {
			throw new IllegalStateException("Processing PBR map twice for " + location);
		}

		for (var process : processMap.values()) {
			process.processStatus = process.process.process(location.toString(), width, height, process.inputTexture, process.outputTexId);
		}

		processed = true;
	}

	void download() {
		if (downloaded) {
			throw new IllegalStateException("Downloading PBR map result twice for " + location);
		}

		for (var layer : PbrMapSpriteLayer.values()) {
			if (processMap.containsKey(layer) && processMap.get(layer).processStatus) {
				final ByteBuffer pixelBuffer = MemoryUtil.memAlloc(width * height * layer.bytes);

				GFX.pixelStore(GFX.GL_PACK_ALIGNMENT, layer.alignment);

				CanvasTextureState.bindTexture(processMap.get(layer).outputTexId);
				GFX.glGetTexImage(GFX.GL_TEXTURE_2D, 0, layer.glFormat, layer.glPixelDataType, pixelBuffer);

				pixelMap.put(layer, pixelBuffer);
			} else {
				// TODO: this is DEBUG, should just spit out default instead of this
				// var input = PbrLoader.inputTextureManager.getSpriteDefault(location);
				//
				// if (input instanceof InputTextureManager.ImageInput imageInput) {
				// 	final ByteBuffer pixelBuffer = MemoryUtil.memAlloc(width * height * 4);
				// 	final var nativeImage = imageInput.image;
				//
				// 	for (int row = 0; row < height; row++) {
				// 		for (int i = 0; i < width; i++) {
				// 			// all of this is unnecessarily complex for the sake of consistency
				// 			for (int byteOrder = 0; byteOrder < layer.bytes; byteOrder++) {
				// 				byte toPut = switch (byteOrder) {
				// 					case (ColorEncode.RED_BYTE_OFFSET) -> nativeImage.format().hasRed() ? nativeImage.getRedOrLuminance(i, row) : 0;
				// 					case (ColorEncode.GREEN_BYTE_OFFSET) -> nativeImage.format().hasGreen() ? nativeImage.getGreenOrLuminance(i, row) : 0;
				// 					case (ColorEncode.BLUE_BYTE_OFFSET) -> nativeImage.format().hasBlue() ? nativeImage.getBlueOrLuminance(i, row) : 0;
				// 					case (ColorEncode.ALPHA_BYTE_OFFSET) -> nativeImage.format().hasAlpha() ? nativeImage.getLuminanceOrAlpha(i, row) : 0xF;
				// 					default -> 0;
				// 				};
				//
				// 				pixelBuffer.put(toPut);
				// 			}
				// 		}
				// 	}
				//
				// 	pixelMap.put(layer, pixelBuffer);
				// }
			}
		}

		downloaded = true;
	}

	void close() {
		if (closed) {
			throw new IllegalStateException("Closing PBR sprite twice for " + location);
		}

		for (var pixel : pixelMap.values()) {
			MemoryUtil.memFree(pixel);
		}

		for (var process : processMap.values()) {
			TextureUtil.releaseTextureId(process.outputTexId);
		}

		processMap.clear();
		pixelMap.clear();

		closed = true;
	}

	@Override
	public int r(PbrMapSpriteLayer layer, int x, int y) {
		if (closed) {
			throw new IllegalStateException("Accessed closed PBR sprite " + location);
		}

		if (pixelMap.containsKey(layer)) {
			return pixelMap.get(layer).get((x + y * width) * layer.bytes + Math.min(layer.bytes, ColorEncode.RED_BYTE_OFFSET));
		} else {
			return ColorEncode.r(layer.defaultValue);
		}
	}

	@Override
	public int g(PbrMapSpriteLayer layer, int x, int y) {
		if (closed) {
			throw new IllegalStateException("Accessed closed PBR sprite " + location);
		}

		if (pixelMap.containsKey(layer)) {
			return pixelMap.get(layer).get((x + y * width) * layer.bytes + Math.min(layer.bytes, ColorEncode.GREEN_BYTE_OFFSET));
		} else {
			return ColorEncode.g(layer.defaultValue);
		}
	}

	@Override
	public int b(PbrMapSpriteLayer layer, int x, int y) {
		if (closed) {
			throw new IllegalStateException("Accessed closed PBR sprite " + location);
		}

		if (pixelMap.containsKey(layer)) {
			return pixelMap.get(layer).get((x + y * width) * layer.bytes + Math.min(layer.bytes, ColorEncode.BLUE_BYTE_OFFSET));
		} else {
			return ColorEncode.b(layer.defaultValue);
		}
	}

	@Override
	public int a(PbrMapSpriteLayer layer, int x, int y) {
		if (closed) {
			throw new IllegalStateException("Accessed closed PBR sprite " + location);
		}

		if (pixelMap.containsKey(layer)) {
			return pixelMap.get(layer).get((x + y * width) * layer.bytes + Math.min(layer.bytes, ColorEncode.ALPHA_BYTE_OFFSET));
		} else {
			return ColorEncode.a(layer.defaultValue);
		}
	}

	static final class ProcessInfo {
		private final PbrProcess process;
		private final InputTextureManager.InputTexture[] inputTexture;
		private final int outputTexId;
		private boolean processStatus = false;

		ProcessInfo(PbrProcess process, InputTextureManager.InputTexture[] inputTexture, int outputTexId) {
			this.process = process;
			this.inputTexture = inputTexture;
			this.outputTexId = outputTexId;
		}
	}
}
