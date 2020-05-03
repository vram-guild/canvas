package grondag.canvas.draw;


import java.util.IdentityHashMap;

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
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.material.MaterialVertexFormats;

public class DrawHandlers {
	private static int nextHandlerIndex = 0;

	public final int index = nextHandlerIndex++;

	private  static final IdentityHashMap<RenderLayer, DrawHandler> LAYER_MAP = new IdentityHashMap<>();

	private static final DrawHandler[] BLEND_MAP = new DrawHandler[5];

	public static final DrawHandler SOLID = new DrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS) {
		@Override
		protected void setupInner() {
			RenderSystem.enableTexture();
			final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
			textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
			textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).setFilter(false, false);
			RenderSystem.disableBlend();
			RenderSystem.shadeModel(GL21.GL_SMOOTH);
			RenderSystem.disableAlphaTest();
			RenderSystem.enableDepthTest();
			RenderSystem.enableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
			BackgroundRenderer.setFogBlack();
			RenderSystem.enableFog();
		}

		@Override
		protected void teardownInner() {
			RenderSystem.disableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
			RenderSystem.disableFog();
		}
	};

	public static final DrawHandler CUTOUT = new DrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS) {
		@Override
		protected void setupInner() {
			//			RenderLayer.getCutout().startDrawing();

			RenderSystem.enableTexture();
			final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
			textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
			textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).setFilter(false, false);
			RenderSystem.disableBlend();
			RenderSystem.shadeModel(GL21.GL_SMOOTH);
			RenderSystem.enableAlphaTest();
			RenderSystem.alphaFunc(516, 0.5f);
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(515);
			RenderSystem.enableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
			BackgroundRenderer.setFogBlack();
			RenderSystem.enableFog();
		}

		@Override
		protected void teardownInner() {
			//			RenderLayer.getCutout().endDrawing();
			RenderSystem.disableAlphaTest();
			RenderSystem.defaultAlphaFunc();
			RenderSystem.disableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
			RenderSystem.disableFog();
		}
	};

	public static final DrawHandler CUTOUT_MIPPED = new DrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS) {
		@Override
		protected void setupInner() {
			//			RenderLayer.getCutoutMipped().startDrawing();

			RenderSystem.enableTexture();
			final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
			textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
			textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).setFilter(false, true);
			RenderSystem.disableBlend();
			RenderSystem.shadeModel(GL21.GL_SMOOTH);
			RenderSystem.enableAlphaTest();
			RenderSystem.alphaFunc(516, 0.5f);
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(515);
			RenderSystem.enableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
			BackgroundRenderer.setFogBlack();
			RenderSystem.enableFog();
		}

		@Override
		protected void teardownInner() {
			//			RenderLayer.getCutoutMipped().endDrawing();
			RenderSystem.disableAlphaTest();
			RenderSystem.defaultAlphaFunc();
			RenderSystem.disableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
			RenderSystem.disableFog();
		}
	};

	public static final DrawHandler TRANSLUCENT = new DrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS) {
		@Override
		protected void setupInner() {
			//			RenderLayer.getTranslucent().startDrawing();
			//			RenderSystem.shadeModel(GL21.GL_SMOOTH);
			//			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
			//			RenderSystem.enableTexture();
			//			final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
			//			textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
			//			textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).setFilter(false, true);
			//			RenderSystem.enableBlend();
			//			RenderSystem.defaultBlendFunc();
			//			RenderSystem.enableDepthTest();


			RenderSystem.enableTexture();
			final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
			textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
			textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).setFilter(false, true);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.shadeModel(GL21.GL_SMOOTH);
			//			RenderSystem.enableAlphaTest();
			//			RenderSystem.alphaFunc(516, 0.5f);
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(515);
			RenderSystem.enableCull();
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
			BackgroundRenderer.setFogBlack();
			RenderSystem.enableFog();
		}

		@Override
		protected void teardownInner() {
			//			RenderLayer.getTranslucent().endDrawing();
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

	public static final DrawHandler VANILLA_CUTOUT = new DrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS) {
		@Override
		protected void setupInner() {
			RenderLayer.getCutout().startDrawing();
		}

		@Override
		protected void teardownInner() {
			RenderLayer.getCutout().endDrawing();
		}
	};

	public static final DrawHandler VANILLA_CUTOUT_MIPPED = new DrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS) {
		@Override
		protected void setupInner() {
			RenderLayer.getCutoutMipped().startDrawing();
		}

		@Override
		protected void teardownInner() {
			RenderLayer.getCutoutMipped().endDrawing();
		}
	};

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
		LAYER_MAP.put(RenderLayer.getSolid(), SOLID);
		LAYER_MAP.put(RenderLayer.getCutout(), CUTOUT);
		LAYER_MAP.put(RenderLayer.getCutoutMipped(), CUTOUT_MIPPED);
		LAYER_MAP.put(RenderLayer.getTranslucent(), TRANSLUCENT);

		BLEND_MAP[BlendMode.DEFAULT.ordinal()] = SOLID;
		BLEND_MAP[BlendMode.SOLID.ordinal()] = SOLID;
		BLEND_MAP[BlendMode.CUTOUT.ordinal()] = CUTOUT;
		BLEND_MAP[BlendMode.CUTOUT_MIPPED.ordinal()] = CUTOUT_MIPPED;
		BLEND_MAP[BlendMode.TRANSLUCENT.ordinal()] = TRANSLUCENT;
	}

	public static DrawHandler get(MaterialContext context, MaterialVertexFormat format, Value mat) {
		return BLEND_MAP[mat.blendMode(0).ordinal()];
	}

	public static DrawHandler get(MaterialContext context, RenderLayer layer) {
		final DrawHandler result = LAYER_MAP.get(layer);
		return result == null ? SOLID : result;
	}
}
