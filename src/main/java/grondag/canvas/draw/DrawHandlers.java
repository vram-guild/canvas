package grondag.canvas.draw;


import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.math.MathHelper;

import grondag.canvas.Configurator;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.shader.ShaderPass;

public class DrawHandlers {

	private static class SolidHandler extends DrawHandler {
		SolidHandler(MaterialVertexFormat format,  ShaderPass shaderType) {
			super(format, shaderType);
		}

		@SuppressWarnings("resource")
		@Override
		protected void setupInner() {
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

		}

	}

	private static class DecalHandler extends DrawHandler {
		DecalHandler(MaterialVertexFormat format, ShaderPass shaderType) {
			super(format, shaderType);
		}

		@SuppressWarnings("resource")
		@Override
		protected void setupInner() {
			RenderSystem.enableTexture();
			final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
			textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
			textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).setFilter(false, true);
			RenderSystem.enableBlend();
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
		}
	}

	private static class TranslucentHandler extends DrawHandler {
		TranslucentHandler(MaterialVertexFormat format, ShaderPass shaderType) {
			super(format, shaderType);
		}

		@SuppressWarnings("resource")
		@Override
		protected void setupInner() {
			RenderSystem.enableTexture();
			final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
			textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
			textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).setFilter(false, true);
			RenderSystem.enableBlend();
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
		}
	}

	private static final DrawHandler[] HANDLERS = new DrawHandler[MathHelper.smallestEncompassingPowerOfTwo(MaterialContext.values().length) * MathHelper.smallestEncompassingPowerOfTwo(ShaderPass.values().length)];
	private static final DrawHandler[] HD_HANDLERS = new DrawHandler[MathHelper.smallestEncompassingPowerOfTwo(MaterialContext.values().length) * MathHelper.smallestEncompassingPowerOfTwo(ShaderPass.values().length)];

	private static int lookupIndex(MaterialContext context, ShaderPass shaderType) {
		return shaderType.ordinal() | (context.ordinal() << 2);
	}

	static {
		HANDLERS[lookupIndex(MaterialContext.TERRAIN, ShaderPass.SOLID)] = new SolidHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, ShaderPass.SOLID);
		HANDLERS[lookupIndex(MaterialContext.TERRAIN, ShaderPass.DECAL)] = new DecalHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, ShaderPass.DECAL);
		HANDLERS[lookupIndex(MaterialContext.TERRAIN, ShaderPass.TRANSLUCENT)] = new TranslucentHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, ShaderPass.TRANSLUCENT);

		HD_HANDLERS[lookupIndex(MaterialContext.TERRAIN, ShaderPass.SOLID)] = new SolidHandler(MaterialVertexFormats.HD_TERRAIN, ShaderPass.SOLID);
		HD_HANDLERS[lookupIndex(MaterialContext.TERRAIN, ShaderPass.DECAL)] = new DecalHandler(MaterialVertexFormats.HD_TERRAIN, ShaderPass.DECAL);
		HD_HANDLERS[lookupIndex(MaterialContext.TERRAIN, ShaderPass.TRANSLUCENT)] = new TranslucentHandler(MaterialVertexFormats.HD_TERRAIN, ShaderPass.TRANSLUCENT);
	}

	public static DrawHandler get(MaterialContext context, ShaderPass shaderType) {
		final boolean isHD = context == MaterialContext.TERRAIN && Configurator.hdLightmaps();
		final int index = lookupIndex(context, shaderType);
		return isHD ? HD_HANDLERS[index] : HANDLERS[index];
	}
}
