/*******************************************************************************
 * Copyright 2019, 2020 grondag
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package grondag.canvas.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import grondag.canvas.Configurator;

// adapted from http://www.anisopteragames.com/how-to-fix-color-banding-with-dithering/
public class DitherTexture implements AutoCloseable {

	private static DitherTexture instance;

	public static DitherTexture instance() {
		DitherTexture result = instance;
		if(result == null) {
			result = new DitherTexture();
			instance = result;
		}
		return result;
	}

	private final NativeImageBackedTexture texture;
	private final NativeImage image;
	private final Identifier textureIdentifier;
	private final MinecraftClient client;

	private boolean needsInitialized = true;

	private DitherTexture() {
		client = MinecraftClient.getInstance();
		texture = new NativeImageBackedTexture(8, 8, false);
		textureIdentifier = client.getTextureManager().registerDynamicTexture("dither", texture);
		image = texture.getImage();
	}

	@Override
	public void close() throws Exception {
		texture.close();
	}

	public void disable() {
		if(!Configurator.lightmapNoise) {
			return;
		}

		GlStateManager.activeTexture(TextureData.DITHER);
		GlStateManager.disableTexture();
		GlStateManager.activeTexture(TextureData.MC_SPRITE_ATLAS);
	}

	public void enable() {
		if(!Configurator.lightmapNoise) {
			return;
		}

		GlStateManager.activeTexture(TextureData.DITHER);
		client.getTextureManager().bindTexture(textureIdentifier);
		GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GlStateManager.texParameter(GL11.GL_TEXTURE_2D,  GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GlStateManager.texParameter(GL11.GL_TEXTURE_2D,  GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GlStateManager.enableTexture();
		GlStateManager.activeTexture(TextureData.MC_SPRITE_ATLAS);
	}

	public void initializeIfNeeded() {
		if(needsInitialized) {
			final NativeImage image = this.image;

			final char pattern[] = {
					0, 32,  8, 40,  2, 34, 10, 42,   /* 8x8 Bayer ordered dithering  */
					48, 16, 56, 24, 50, 18, 58, 26,  /* pattern.  Each input pixel   */
					12, 44,  4, 36, 14, 46,  6, 38,  /* is scaled to the 0..63 range */
					60, 28, 52, 20, 62, 30, 54, 22,  /* before looking in this table */
					3, 35, 11, 43,  1, 33,  9, 41,   /* to determine the action.     */
					51, 19, 59, 27, 49, 17, 57, 25,
					15, 47,  7, 39, 13, 45,  5, 37,
					63, 31, 55, 23, 61, 29, 53, 21 };

			for(int u = 0; u < 8; u++) {
				for(int v = 0; v < 8; v++) {
					image.setPixelColor(u, v, pattern[v * 8 + u]);
				}
			}

			needsInitialized = false;
			texture.upload();
		}
	}
}
