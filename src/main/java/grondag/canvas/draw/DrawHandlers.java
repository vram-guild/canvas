package grondag.canvas.draw;


import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
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

	private static final Long2ObjectOpenHashMap<DrawHandler> MAP = new Long2ObjectOpenHashMap<>();

	private static long lookupIndex(MaterialContext context, MaterialVertexFormat format, MaterialShaderImpl shader,  MaterialConditionImpl condition, ShaderContext.Type shaderType) {
		return shaderType.ordinal() | ((long) context.ordinal() << 2) | ((long) format.index << 10) | ((long) condition.index << 16) | ((long) shader.getIndex() << 32);
	}

	// PERF: cache in the drawable material
	public static DrawHandler get(MaterialContext context, DrawableMaterial mat) {
		final boolean isHD = context == MaterialContext.TERRAIN && Configurator.hdLightmaps();

		if (mat.shaderType == ShaderContext.Type.TRANSLUCENT) {
			return isHD ? TRANSLUCENT_HD : TRANSLUCENT;
		}

		final MaterialVertexFormat format = isHD ? MaterialVertexFormats.HD_TERRAIN : MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS;

		final long index = lookupIndex(context, format, mat.shader(), mat.condition(), mat.shaderType);

		DrawHandler result = MAP.get(index);

		if (result == null) {
			result = mat.shaderType == ShaderContext.Type.DECAL
					? new DecalHandler(format, mat.shader(), mat.condition(), ShaderContext.Type.DECAL)
							: new SolidHandler(format, mat.shader(), mat.condition(), ShaderContext.Type.SOLID);

					MAP.put(index, result);
		}

		return result;
	}
}
