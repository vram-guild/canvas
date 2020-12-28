/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.mixin;

import java.util.Set;
import java.util.SortedSet;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FpsSmoother;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import grondag.canvas.CanvasMod;
import grondag.canvas.mixinterface.WorldRendererExt;
import grondag.canvas.render.CanvasWorldRenderer;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer implements WorldRendererExt {
	private static boolean shouldWarnOnSetupTerrain = true;
	private static boolean shouldWarnOnRenderLayer = true;
	private static boolean shouldWarnGetAdjacentChunk = true;
	private static boolean shouldWarnOnUpdateChunks = true;

	@Shadow private MinecraftClient client;
	@Shadow private int renderDistance;
	@Shadow private ClientWorld world;
	@Shadow private int frame;
	@Shadow private boolean cloudsDirty;
	@Shadow private TextureManager textureManager;
	@Shadow private EntityRenderDispatcher entityRenderDispatcher;
	// PERF: prevent wasteful allocation of these - they are not all used with Canvas and take a lot of space
	@Shadow private BufferBuilderStorage bufferBuilders;
	@Shadow private int regularEntityCount;
	@Shadow private int blockEntityCount;
	@Shadow private FpsSmoother chunkUpdateSmoother;
	@Shadow private Framebuffer entityOutlinesFramebuffer;
	@Shadow private ShaderEffect entityOutlineShader;
	@Shadow private Set<BlockEntity> noCullingBlockEntities;
	@Shadow private Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions;
	@Shadow private VertexFormat vertexFormat;
	@Shadow private Framebuffer translucentFramebuffer;
	@Shadow private Framebuffer entityFramebuffer;
	@Shadow private Framebuffer particlesFramebuffer;
	@Shadow private Framebuffer weatherFramebuffer;
	@Shadow private Framebuffer cloudsFramebuffer;
	@Shadow private ShaderEffect transparencyShader;
	@Shadow protected boolean canDrawEntityOutlines() {
		return false;
	}

	@Shadow private void drawBlockOutline(MatrixStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState) { }

	@Shadow private void renderWorldBorder(Camera camera) { }

	@Shadow private void renderEntity(Entity entity, double d, double e, double f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider) { }

	@Shadow private void renderWeather(LightmapTextureManager lightmapTextureManager, float f, double d, double e, double g) { }

	@Shadow private void resetTransparencyShader() { }

	@Shadow private void loadTransparencyShader() { }

	@Inject(at = @At("HEAD"), method = "setupTerrain", cancellable = true)
	private void onSetupTerrain(Camera camera, Frustum frustum, boolean bl, int i, boolean bl2, CallbackInfo ci) {
		if (shouldWarnOnSetupTerrain) {
			CanvasMod.LOG.warn("[Canvas] WorldRendererer.setupTerrain() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.cancel();
			shouldWarnOnSetupTerrain = false;
		}
	}

	@Inject(at = @At("HEAD"), method = "renderChunkDebugInfo", cancellable = true)
	private void onRenderChunkDebugInfo(Camera camera, CallbackInfo ci) {
		ci.cancel();
	}

	@Redirect(method = "scheduleChunkRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BuiltChunkStorage;scheduleRebuild(IIIZ)V"), require = 1)
	private void onScheduleChunkRender(BuiltChunkStorage storage, int x, int y, int z, boolean urgent) {
		((CanvasWorldRenderer) (Object) this).scheduleRegionRender(x, y, z, urgent);
	}

	@Redirect(method = "reload", at = @At(value = "FIELD", target = "Lnet/minecraft/client/options/GameOptions;viewDistance:I", ordinal = 1))
	private int onReloadZeroChunkStorage(GameOptions options) {
		return 0;
	}

	@Inject(at = @At("HEAD"), method = "renderLayer", cancellable = true)
	private void onRenderLayer(CallbackInfo ci) {
		if (shouldWarnOnRenderLayer) {
			CanvasMod.LOG.warn("[Canvas] WorldRendererer.renderLayer() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.cancel();
			shouldWarnOnRenderLayer = false;
		}
	}

	@Inject(at = @At("HEAD"), method = "getAdjacentChunk", cancellable = true)
	private void onGetAdjacentChunk(CallbackInfoReturnable<BuiltChunk> ci) {
		if (shouldWarnGetAdjacentChunk) {
			CanvasMod.LOG.warn("[Canvas] WorldRendererer.getAdjacentChunk() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.setReturnValue(null);
			shouldWarnGetAdjacentChunk = false;
		}
	}

	@Inject(at = @At("HEAD"), method = "updateChunks", cancellable = true)
	private void onUpdateChunks(CallbackInfo ci) {
		if (shouldWarnOnUpdateChunks) {
			CanvasMod.LOG.warn("[Canvas] WorldRendererer.udpateChunks() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.cancel();
			shouldWarnOnUpdateChunks = false;
		}
	}

	@Inject(at = @At("HEAD"), method = "loadTransparencyShader", cancellable = true)
	private void onLoadTransparencyShader(CallbackInfo ci) {
		System.out.println("loadTransparencyShaderHook");
		resetTransparencyShader();
	}

	@Override
	public MinecraftClient canvas_mc() {
		return client;
	}

	@Override
	public int canvas_renderDistance() {
		return renderDistance;
	}

	@Override
	public void canvas_reload() {
		// WIP: replace
		if (MinecraftClient.isFabulousGraphicsOrBetter()) {
			loadTransparencyShader();
		} else {
			resetTransparencyShader();
		}

		//		if (translucentFramebuffer != null) {
		//			translucentFramebuffer.delete();
		//			entityFramebuffer.delete();
		//			particlesFramebuffer.delete();
		//			weatherFramebuffer.delete();
		//			cloudsFramebuffer.delete();
		//			translucentFramebuffer = null;
		//			entityFramebuffer = null;
		//			particlesFramebuffer = null;
		//			weatherFramebuffer = null;
		//			cloudsFramebuffer = null;
		//		}
		//
		//		if (MinecraftClient.isFabulousGraphicsOrBetter()) {
		//		}

		// not used by us
		//needsTerrainUpdate = true;
		if (world != null) {
			world.reloadColor();
		}

		cloudsDirty = true;
		RenderLayers.setFancyGraphicsOrBetter(MinecraftClient.isFancyGraphicsOrBetter());
		renderDistance = client.options.viewDistance;

		synchronized (noCullingBlockEntities) {
			noCullingBlockEntities.clear();
		}
	}

	@Override
	public ClientWorld canvas_world() {
		return world;
	}

	@Override
	public void canvas_setWorldNoSideEffects(ClientWorld world) {
		this.world = world;
	}

	@Override
	public TextureManager canvas_textureManager() {
		return textureManager;
	}

	@Override
	public EntityRenderDispatcher canvas_entityRenderDispatcher() {
		return entityRenderDispatcher;
	}

	@Override
	public BufferBuilderStorage canvas_bufferBuilders() {
		return bufferBuilders;
	}

	@Override
	public int canvas_getAndIncrementFrameIndex() {
		return frame++;
	}

	@Override
	public FpsSmoother canvas_chunkUpdateSmoother() {
		return chunkUpdateSmoother;
	}

	@Override
	public boolean canvas_canDrawEntityOutlines() {
		return canDrawEntityOutlines();
	}

	@Override
	public Framebuffer canvas_entityOutlinesFramebuffer() {
		return entityOutlinesFramebuffer;
	}

	@Override
	public ShaderEffect canvas_entityOutlineShader() {
		return entityOutlineShader;
	}

	@Override
	public Set<BlockEntity> canvas_noCullingBlockEntities() {
		return noCullingBlockEntities;
	}

	@Override
	public void canvas_drawBlockOutline(MatrixStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState) {
		drawBlockOutline(matrixStack, vertexConsumer, entity, d, e, f, blockPos, blockState);
	}

	@Override
	public void canvas_renderWorldBorder(Camera camera) {
		renderWorldBorder(camera);
	}

	@Override
	public Long2ObjectMap<SortedSet<BlockBreakingInfo>> canvas_blockBreakingProgressions() {
		return blockBreakingProgressions;
	}

	@Override
	public void canvas_renderEntity(Entity entity, double d, double e, double f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider) {
		renderEntity(entity, d, e, f, g, matrixStack, vertexConsumerProvider);
	}

	@Override
	public void canvas_renderWeather(LightmapTextureManager lightmapTextureManager, float f, double d, double e, double g) {
		renderWeather(lightmapTextureManager, f, d, e, g);
	}

	@Override
	public void canvas_setEntityCounts(int regularEntityCountIn, int blockEntityCountIn) {
		regularEntityCount = regularEntityCountIn;
		blockEntityCount = blockEntityCountIn;
	}

	@Override
	public VertexFormat canvas_vertexFormat() {
		return vertexFormat;
	}

	@Override
	public ShaderEffect canvas_transparencyShader() {
		return transparencyShader;
	}
}
