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
package grondag.canvas.light;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.Configurator;
import grondag.canvas.texture.SimpleImage;
import grondag.canvas.texture.SimpleTexture;
import grondag.canvas.texture.TextureData;

@Environment(EnvType.CLIENT)
public class LightmapHdTexture implements AutoCloseable {
	private static LightmapHdTexture instance;

	public static LightmapHdTexture instance() {
		LightmapHdTexture result = instance;
		if(result == null) {
			result = new LightmapHdTexture();
			instance = result;
		}
		return result;
	}

	public static void reload() {
		// just clear image if size is same
		if(instance != null && instance.texture.getImage().width == LightmapSizer.texSize) {
			instance.clear();
		} else if(instance != null) {
			instance.close();
			instance = null;
		}
	}

	private final SimpleTexture texture;
	private final SimpleImage image;

	private LightmapHdTexture() {
		texture = new SimpleTexture(new SimpleImage(4, GL11.GL_RGBA, LightmapSizer.texSize, LightmapSizer.texSize, false), GL11.GL_RGBA);
		image = texture.getImage();
		clear();
	}

	private void clear() {
		image.clear((byte)255);
		texture.upload();
	}

	private static final ConcurrentLinkedQueue<LightmapHd> updates = new ConcurrentLinkedQueue<>();

	public void enque(LightmapHd lightmap) {
		final SimpleImage image = this.image;
		final int uMap = lightmap.uMinImg;
		final int vMap = lightmap.vMinImg;

		for(int u = 0; u < LightmapSizer.paddedSize; u++) {
			for(int v = 0; v < LightmapSizer.paddedSize; v++) {
				image.setPixelRGBA(uMap + u, vMap + v, lightmap.pixel(u,v));
			}
		}

		updates.add(lightmap);
	}

	@Override
	public void close() {
		texture.close();
	}

	public void disable() {
		if(!Configurator.hdLightmaps()) {
			return;
		}

		GlStateManager.activeTexture(TextureData.HD_LIGHTMAP);
		GlStateManager.disableTexture();
		GlStateManager.activeTexture(TextureData.MC_SPRITE_ATLAS);
	}

	public void enable() {
		if(!Configurator.hdLightmaps()) {
			return;
		}

		GlStateManager.activeTexture(TextureData.HD_LIGHTMAP);
		texture.bindTexture();

		final int mode = Configurator.lightmapDebug ? GL11.GL_NEAREST : GL11.GL_LINEAR;
		GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mode);
		GlStateManager.texParameter(GL11.GL_TEXTURE_2D,  GL11.GL_TEXTURE_MAG_FILTER, mode);
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableTexture();
		GlStateManager.activeTexture(TextureData.MC_SPRITE_ATLAS);
	}

	private int frameCounter = 0;

	public void onRenderTick() {
		frameCounter++;

		if(updates.isEmpty() || frameCounter < Configurator.maxLightmapDelayFrames) {
			return;
		}

		frameCounter = 0;

		int uMin = Integer.MAX_VALUE;
		int vMin = Integer.MAX_VALUE;
		int uMax = Integer.MIN_VALUE;
		int vMax = Integer.MIN_VALUE;

		LightmapHd map;
		while((map = updates.poll()) != null) {
			final int uMap = map.uMinImg;
			final int vMap = map.vMinImg;
			uMin = Math.min(uMin, uMap);
			vMin = Math.min(vMin, vMap);
			uMax = Math.max(uMax, uMap + LightmapSizer.paddedSize);
			vMax = Math.max(vMax, vMap + LightmapSizer.paddedSize);
		}

		if(uMin == Integer.MAX_VALUE) {
			return;
		}

		uMin = (uMin / 4) * 4;
		final int w = ((uMax - uMin + 3) / 4) * 4;

		texture.uploadPartial(uMin, vMin, w, vMax - vMin);
	}
}
