package grondag.canvas.draw;


import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.apiimpl.MaterialShaderImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl.CompositeMaterial.DrawableMaterial;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.shader.ShaderContext;
import grondag.canvas.shader.ShaderContext.Type;
import grondag.canvas.shader.ShaderManager;

public class DrawHandlers {
	private static int nextHandlerIndex = 0;

	public final int index = nextHandlerIndex++;

	public static final DrawHandler SOLID = new SolidHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, ShaderManager.INSTANCE.getDefault(), MaterialConditionImpl.ALWAYS, ShaderContext.Type.SOLID);
	public static final DrawHandler SOLID_HD = new SolidHandler(MaterialVertexFormats.HD_TERRAIN, ShaderManager.INSTANCE.getDefault(), MaterialConditionImpl.ALWAYS, ShaderContext.Type.SOLID);

	private static class SolidHandler extends DrawHandler {
		SolidHandler(MaterialVertexFormat format, MaterialShaderImpl shader, MaterialConditionImpl condition, Type shaderType) {
			super(format, shader, condition, shaderType);
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
			shader.activate(ShaderContext.TERRAIN_SOLID, format);
		}

		@SuppressWarnings("resource")
		@Override
		protected void teardownInner() {
			RenderSystem.disableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
			RenderSystem.disableFog();

		}

	}

	public static final DrawHandler DECAL = new DecalHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, ShaderManager.INSTANCE.getDefault(), MaterialConditionImpl.ALWAYS, ShaderContext.Type.DECAL);
	public static final DrawHandler DECAL_HD = new DecalHandler(MaterialVertexFormats.HD_TERRAIN, ShaderManager.INSTANCE.getDefault(), MaterialConditionImpl.ALWAYS, ShaderContext.Type.DECAL);

	private static class DecalHandler extends DrawHandler {
		DecalHandler(MaterialVertexFormat format, MaterialShaderImpl shader, MaterialConditionImpl condition, Type shaderType) {
			super(format, shader, condition, shaderType);
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
			shader.activate(ShaderContext.TERRAIN_DECAL, format);
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


	public static final DrawHandler TRANSLUCENT = new TranslucentHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, ShaderManager.INSTANCE.getDefault(), MaterialConditionImpl.ALWAYS, ShaderContext.Type.TRANSLUCENT);
	public static final DrawHandler TRANSLUCENT_HD = new TranslucentHandler(MaterialVertexFormats.HD_TERRAIN, ShaderManager.INSTANCE.getDefault(), MaterialConditionImpl.ALWAYS, ShaderContext.Type.TRANSLUCENT);

	private static class TranslucentHandler extends DrawHandler {
		TranslucentHandler(MaterialVertexFormat format, MaterialShaderImpl shader, MaterialConditionImpl condition, Type shaderType) {
			super(format, shader, condition, shaderType);
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
			shader.activate(ShaderContext.TERRAIN_TRANSLUCENT, format);
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

	public static DrawHandler get(MaterialContext context, DrawableMaterial mat) {
		if  (context == MaterialContext.TERRAIN && Configurator.hdLightmaps) {
			switch (mat.shaderType) {
			case DECAL:
				return DECAL_HD;
			case TRANSLUCENT:
				return TRANSLUCENT_HD;
			default:
			case SOLID:
				return SOLID_HD;
			}
		} else {
			switch (mat.shaderType) {
			case DECAL:
				return DECAL;
			case TRANSLUCENT:
				return TRANSLUCENT;
			default:
			case SOLID:
				return SOLID;
			}
		}
	}
}
