/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.light;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.mojang.blaze3d.platform.GlStateManager;

import grondag.canvas.config.Configurator;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.texture.SimpleImage;
import grondag.canvas.texture.SimpleTexture;
import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.GFX;

@SuppressWarnings("unused")
class LightmapHdTexture implements AutoCloseable {
	private static final ConcurrentLinkedQueue<LightmapHd> updates = new ConcurrentLinkedQueue<>();
	private static LightmapHdTexture instance;
	private final SimpleTexture texture;
	private final SimpleImage image;
	private int frameCounter = 0;

	private LightmapHdTexture() {
		texture = new SimpleTexture(new SimpleImage(4, GFX.GL_RGBA, LightmapSizer.texSize, LightmapSizer.texSize, false), GFX.GL_RGBA);
		image = texture.getImage();
		clear();
	}

	public static LightmapHdTexture instance() {
		LightmapHdTexture result = instance;

		if (result == null) {
			result = new LightmapHdTexture();
			instance = result;
		}

		return result;
	}

	public static void reload() {
		// just clear image if size is same
		if (instance != null && instance.texture.getImage().width == LightmapSizer.texSize) {
			instance.clear();
		} else if (instance != null) {
			instance.close();
			instance = null;
		}
	}

	private void clear() {
		image.clear((byte) 255);
		texture.upload();
	}

	public void enque(LightmapHd lightmap) {
		final SimpleImage image = this.image;
		final int uMap = lightmap.uMinImg;
		final int vMap = lightmap.vMinImg;

		for (int u = 0; u < LightmapSizer.paddedSize; u++) {
			for (int v = 0; v < LightmapSizer.paddedSize; v++) {
				image.setPixelRGBA(uMap + u, vMap + v, lightmap.pixel(u, v));
			}
		}

		updates.add(lightmap);
	}

	@Override
	public void close() {
		texture.close();
	}

	private void disable() {
		if (!Configurator.hdLightmaps()) {
			return;
		}

		// UGLY: should not be needed - enable doesn't affect shaders
		CanvasTextureState.activeTextureUnit(TextureData.HD_LIGHTMAP);
		GlStateManager._disableTexture();
		CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
	}

	private void enable() {
	//		if (!Configurator.hdLightmaps()) {
	//			return;
	//		}
	//
	//		GlStateManager.activeTexture(TextureData.HD_LIGHTMAP);
	//		texture.bindTexture();
	//
	//		final int mode = Configurator.lightmapDebug ? GL11.GL_NEAREST : GL11.GL_LINEAR;
	//		GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mode);
	//		GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mode);
	//		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
	//		GlStateManager.enableTexture();
	//		GlStateManager.activeTexture(TextureData.MC_SPRITE_ATLAS);
	}

	public void onRenderTick() {
		frameCounter++;

		if (updates.isEmpty() || frameCounter < 0) { //Configurator.maxLightmapDelayFrames) {
			return;
		}

		frameCounter = 0;

		int uMin = Integer.MAX_VALUE;
		int vMin = Integer.MAX_VALUE;
		int uMax = Integer.MIN_VALUE;
		int vMax = Integer.MIN_VALUE;

		LightmapHd map;

		while ((map = updates.poll()) != null) {
			final int uMap = map.uMinImg;
			final int vMap = map.vMinImg;
			uMin = Math.min(uMin, uMap);
			vMin = Math.min(vMin, vMap);
			uMax = Math.max(uMax, uMap + LightmapSizer.paddedSize);
			vMax = Math.max(vMax, vMap + LightmapSizer.paddedSize);
		}

		if (uMin == Integer.MAX_VALUE) {
			return;
		}

		uMin = (uMin / 4) * 4;
		final int w = ((uMax - uMin + 3) / 4) * 4;

		texture.uploadPartial(uMin, vMin, w, vMax - vMin);
	}
}
