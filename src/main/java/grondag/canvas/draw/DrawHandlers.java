package grondag.canvas.draw;


import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.shader.ShaderContext;
import grondag.canvas.shader.ShaderManager;

public class DrawHandlers {
	private static int nextHandlerIndex = 0;

	public final int index = nextHandlerIndex++;

	private static final DrawHandler[] BLEND_MAP = new DrawHandler[5];

	public static final DrawHandler SOLID = new DrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, ShaderManager.INSTANCE.getDefault(), MaterialConditionImpl.ALWAYS) {
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
			shader.activate(ShaderContext.VANILLA_TERRAIN, format);
		}

		@SuppressWarnings("resource")
		@Override
		protected void teardownInner() {
			RenderSystem.disableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
			RenderSystem.disableFog();

		}
	};

	//	public static final DrawHandler CUTOUT = new DrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS) {
	//		@Override
	//		protected void setupInner() {
	//			RenderSystem.enableTexture();
	//			final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
	//			textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
	//			textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).setFilter(false, false);
	//			RenderSystem.disableBlend();
	//			RenderSystem.shadeModel(GL21.GL_SMOOTH);
	//			RenderSystem.enableAlphaTest();
	//			RenderSystem.alphaFunc(516, 0.5f);
	//			RenderSystem.enableDepthTest();
	//			RenderSystem.depthFunc(515);
	//			RenderSystem.enableCull();
	//			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
	//			BackgroundRenderer.setFogBlack();
	//			RenderSystem.enableFog();
	//		}
	//
	//		@Override
	//		protected void teardownInner() {
	//			RenderSystem.disableAlphaTest();
	//			RenderSystem.defaultAlphaFunc();
	//			RenderSystem.disableCull();
	//			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
	//			RenderSystem.disableFog();
	//		}
	//	};
	//
	//	public static final DrawHandler CUTOUT_MIPPED = new DrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS) {
	//		@Override
	//		protected void setupInner() {
	//			RenderSystem.enableTexture();
	//			final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
	//			textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
	//			textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).setFilter(false, true);
	//			RenderSystem.disableBlend();
	//			RenderSystem.shadeModel(GL21.GL_SMOOTH);
	//			RenderSystem.enableAlphaTest();
	//			RenderSystem.alphaFunc(516, 0.5f);
	//			RenderSystem.enableDepthTest();
	//			RenderSystem.depthFunc(515);
	//			RenderSystem.enableCull();
	//			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
	//			BackgroundRenderer.setFogBlack();
	//			RenderSystem.enableFog();
	//		}
	//
	//		@Override
	//		protected void teardownInner() {
	//			RenderSystem.disableAlphaTest();
	//			RenderSystem.defaultAlphaFunc();
	//			RenderSystem.disableCull();
	//			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
	//			RenderSystem.disableFog();
	//		}
	//	};

	public static final DrawHandler TRANSLUCENT = new DrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, ShaderManager.INSTANCE.getDefault(), MaterialConditionImpl.ALWAYS) {
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
			shader.activate(ShaderContext.VANILLA_TERRAIN, format);
		}

		@SuppressWarnings("resource")
		@Override
		protected void teardownInner() {
			RenderSystem.disableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
			RenderSystem.disableFog();
			RenderSystem.disableBlend();
		}
	};

	public static final DrawHandler VANILLA_SOLID = new DrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS) {
		@Override
		protected void setupInner() {
			RenderLayer.getSolid().startDrawing();
		}

		@Override
		protected void teardownInner() {
			RenderLayer.getSolid().endDrawing();
		}
	};

	//	public static final DrawHandler VANILLA_CUTOUT = new DrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS) {
	//		@Override
	//		protected void setupInner() {
	//			RenderLayer.getCutout().startDrawing();
	//		}
	//
	//		@Override
	//		protected void teardownInner() {
	//			RenderLayer.getCutout().endDrawing();
	//		}
	//	};
	//
	//	public static final DrawHandler VANILLA_CUTOUT_MIPPED = new DrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS) {
	//		@Override
	//		protected void setupInner() {
	//			RenderLayer.getCutoutMipped().startDrawing();
	//		}
	//
	//		@Override
	//		protected void teardownInner() {
	//			RenderLayer.getCutoutMipped().endDrawing();
	//		}
	//	};

	public static final DrawHandler VANILLA_TRANSLUCENT = new DrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS) {
		@Override
		protected void setupInner() {
			RenderLayer.getTranslucent().startDrawing();
		}

		@Override
		protected void teardownInner() {
			RenderLayer.getTranslucent().endDrawing();
		}
	};

	static {
		BLEND_MAP[BlendMode.DEFAULT.ordinal()] = SOLID;
		BLEND_MAP[BlendMode.SOLID.ordinal()] = SOLID;
		BLEND_MAP[BlendMode.CUTOUT.ordinal()] = SOLID;
		BLEND_MAP[BlendMode.CUTOUT_MIPPED.ordinal()] = SOLID;
		BLEND_MAP[BlendMode.TRANSLUCENT.ordinal()] = TRANSLUCENT;
	}

	public static DrawHandler get(MaterialContext context, Value mat) {
		return BLEND_MAP[mat.blendMode(0).ordinal()];
	}
}
