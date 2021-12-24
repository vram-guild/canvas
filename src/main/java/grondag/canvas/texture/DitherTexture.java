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

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;

// adapted from http://www.anisopteragames.com/how-to-fix-color-banding-with-dithering/
@SuppressWarnings("unused")
class DitherTexture implements AutoCloseable {
	private static DitherTexture instance;
	private final DynamicTexture texture;
	private final NativeImage image;
	private final ResourceLocation textureIdentifier;
	private final Minecraft client;
	private boolean needsInitialized = true;

	private DitherTexture() {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: DitherTexture init");
		}

		client = Minecraft.getInstance();
		texture = new DynamicTexture(8, 8, false);
		textureIdentifier = client.getTextureManager().register("dither", texture);
		image = texture.getPixels();
	}

	private static DitherTexture instance() {
		DitherTexture result = instance;

		if (result == null) {
			result = new DitherTexture();
			instance = result;
		}

		return result;
	}

	@Override
	public void close() throws Exception {
		texture.close();
	}

	public void disable() {
		//		if (!Configurator.lightmapNoise) {
		//			return;
		//		}
		//
		//		GlStateManager.activeTexture(TextureData.DITHER);
		//		GlStateManager.disableTexture();
		//		GlStateManager.activeTexture(TextureData.MC_SPRITE_ATLAS);
	}

	public void enable() {
		//		if (!Configurator.lightmapNoise) {
		//			return;
		//		}
		//
		//		GlStateManager.activeTexture(TextureData.DITHER);
		//		client.getTextureManager().bindTexture(textureIdentifier);
		//		GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		//		GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		//		GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		//		GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		//		GlStateManager.enableTexture();
		//		GlStateManager.activeTexture(TextureData.MC_SPRITE_ATLAS);
	}

	public void initializeIfNeeded() {
		if (needsInitialized) {
			final NativeImage image = this.image;

			final char[] pattern = {
				0, 32, 8, 40, 2, 34, 10, 42,   /* 8x8 Bayer ordered dithering  */
				48, 16, 56, 24, 50, 18, 58, 26,  /* pattern.  Each input pixel   */
				12, 44, 4, 36, 14, 46, 6, 38,  /* is scaled to the 0..63 range */
				60, 28, 52, 20, 62, 30, 54, 22,  /* before looking in this table */
				3, 35, 11, 43, 1, 33, 9, 41,   /* to determine the action.     */
				51, 19, 59, 27, 49, 17, 57, 25,
				15, 47, 7, 39, 13, 45, 5, 37,
				63, 31, 55, 23, 61, 29, 53, 21};

			for (int u = 0; u < 8; u++) {
				for (int v = 0; v < 8; v++) {
					image.setPixelRGBA(u, v, pattern[v * 8 + u]);
				}
			}

			needsInitialized = false;
			texture.upload();
		}
	}
}
