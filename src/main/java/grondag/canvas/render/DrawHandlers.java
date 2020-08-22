/*******************************************************************************
 * Copyright 2020 grondag
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

package grondag.canvas.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.math.MathHelper;

import grondag.canvas.Configurator;
import grondag.canvas.material.EncodingContext;
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.shader.ShaderPass;
import grondag.canvas.texture.SpriteInfoTexture;

public class DrawHandlers {

	private static class SolidHandler extends DrawHandler {
		SolidHandler(MaterialVertexFormat format,  ShaderPass shaderPass) {
			super(format, shaderPass);
		}

		@SuppressWarnings("resource")
		@Override
		protected void setupInner() {
			SpriteInfoTexture.instance().enable();
			RenderSystem.enableTexture();
			final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
			textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
			textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).setFilter(false, true);
			RenderSystem.disableBlend();
			RenderSystem.shadeModel(GL21.GL_SMOOTH);
			//			RenderSystem.enableAlphaTest();
			//			RenderSystem.alphaFunc(516, 0.5f);
			RenderSystem.enableDepthTest();
			RenderSystem.enableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
			BackgroundRenderer.setFogBlack();
			RenderSystem.enableFog(); // TODO:  needed?
		}

		@SuppressWarnings("resource")
		@Override
		protected void teardownInner() {
			RenderSystem.disableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
			RenderSystem.disableFog();
			SpriteInfoTexture.instance().disable();

		}

	}

	private static class DecalHandler extends DrawHandler {
		DecalHandler(MaterialVertexFormat format, ShaderPass shaderType) {
			super(format, shaderType);
		}

		@SuppressWarnings("resource")
		@Override
		protected void setupInner() {
			SpriteInfoTexture.instance().enable();
			RenderSystem.enableTexture();
			final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
			textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
			textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).setFilter(false, true);
			RenderSystem.enableBlend();
			RenderSystem.depthMask(false);
			RenderSystem.defaultBlendFunc();
			RenderSystem.shadeModel(GL21.GL_SMOOTH);
			RenderSystem.disableAlphaTest();
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(515);
			RenderSystem.enableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
			BackgroundRenderer.setFogBlack();
			RenderSystem.enableFog();
		}

		@SuppressWarnings("resource")
		@Override
		protected void teardownInner() {
			RenderSystem.disableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
			RenderSystem.disableFog();
			RenderSystem.disableBlend();
			RenderSystem.depthMask(true);
			SpriteInfoTexture.instance().disable();
		}
	}

	private static class TranslucentHandler extends DrawHandler {
		TranslucentHandler(MaterialVertexFormat format, ShaderPass shaderPass) {
			super(format, shaderPass);
		}

		@SuppressWarnings("resource")
		@Override
		protected void setupInner() {
			SpriteInfoTexture.instance().enable();
			RenderSystem.enableTexture();
			final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
			textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
			textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).setFilter(false, true);
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.shadeModel(GL21.GL_SMOOTH);
			RenderSystem.disableAlphaTest();
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(GL21.GL_LEQUAL);
			RenderSystem.enableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
			BackgroundRenderer.setFogBlack();
			RenderSystem.enableFog();
		}

		@SuppressWarnings("resource")
		@Override
		protected void teardownInner() {
			RenderSystem.disableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
			RenderSystem.disableFog();
			RenderSystem.disableBlend();
			RenderSystem.defaultBlendFunc();
			SpriteInfoTexture.instance().disable();
		}
	}

	private static final DrawHandler[] HANDLERS = new DrawHandler[MathHelper.smallestEncompassingPowerOfTwo(EncodingContext.values().length) * MathHelper.smallestEncompassingPowerOfTwo(ShaderPass.values().length)];
	private static final DrawHandler[] HD_HANDLERS = new DrawHandler[MathHelper.smallestEncompassingPowerOfTwo(EncodingContext.values().length) * MathHelper.smallestEncompassingPowerOfTwo(ShaderPass.values().length)];

	private static int lookupIndex(EncodingContext context, ShaderPass shaderType) {
		return shaderType.ordinal() | (context.ordinal() << 2);
	}

	static {
		HANDLERS[lookupIndex(EncodingContext.TERRAIN, ShaderPass.SOLID)] = new SolidHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, ShaderPass.SOLID);
		HANDLERS[lookupIndex(EncodingContext.TERRAIN, ShaderPass.DECAL)] = new DecalHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, ShaderPass.DECAL);
		HANDLERS[lookupIndex(EncodingContext.TERRAIN, ShaderPass.TRANSLUCENT)] = new TranslucentHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, ShaderPass.TRANSLUCENT);

		HD_HANDLERS[lookupIndex(EncodingContext.TERRAIN, ShaderPass.SOLID)] = new SolidHandler(MaterialVertexFormats.HD_TERRAIN, ShaderPass.SOLID);
		HD_HANDLERS[lookupIndex(EncodingContext.TERRAIN, ShaderPass.DECAL)] = new DecalHandler(MaterialVertexFormats.HD_TERRAIN, ShaderPass.DECAL);
		HD_HANDLERS[lookupIndex(EncodingContext.TERRAIN, ShaderPass.TRANSLUCENT)] = new TranslucentHandler(MaterialVertexFormats.HD_TERRAIN, ShaderPass.TRANSLUCENT);
	}

	public static DrawHandler get(EncodingContext context, ShaderPass shaderPass) {
		assert shaderPass != ShaderPass.PROCESS;

		final boolean isHD = context == EncodingContext.TERRAIN && Configurator.hdLightmaps();
		final int index = lookupIndex(context, shaderPass);
		return isHD ? HD_HANDLERS[index] : HANDLERS[index];
	}
}
