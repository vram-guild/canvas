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
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RunningTrimmedMean;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import grondag.canvas.CanvasMod;
import grondag.canvas.mixinterface.WorldRendererExt;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.render.FabulousFrameBuffer;
import grondag.canvas.render.world.CanvasWorldRenderer;

@Mixin(LevelRenderer.class)
public class MixinWorldRenderer implements WorldRendererExt {
	@Shadow private ClientLevel world;
	@Shadow private int frame;
	@Shadow private boolean cloudsDirty;
	@Shadow private TextureManager textureManager;
	@Shadow private EntityRenderDispatcher entityRenderDispatcher;
	// PERF: prevent wasteful allocation of these - they are not all used with Canvas and take a lot of space
	@Shadow private RenderBuffers bufferBuilders;
	@Shadow private int regularEntityCount;
	@Shadow private int blockEntityCount;
	@Shadow private RunningTrimmedMean chunkUpdateSmoother;
	@Shadow private RenderTarget entityOutlinesFramebuffer;
	@Shadow private PostChain entityOutlineShader;
	@Shadow private Set<BlockEntity> noCullingBlockEntities;
	@Shadow private Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions;
	@Shadow private RenderTarget translucentFramebuffer;
	@Shadow private RenderTarget entityFramebuffer;
	@Shadow private RenderTarget particlesFramebuffer;
	@Shadow private RenderTarget weatherFramebuffer;
	@Shadow private RenderTarget cloudsFramebuffer;
	@Shadow protected boolean canDrawEntityOutlines() {
		return false;
	}

	@Shadow private static void drawShapeOutline(PoseStack matrixStack, VertexConsumer vertexConsumer, VoxelShape voxelShape, double d, double e, double f, float g, float h, float i, float j) { }

	@Shadow private void renderWorldBorder(Camera camera) { }

	@Shadow private void renderEntity(Entity entity, double d, double e, double f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider) { }

	@Shadow private void renderWeather(LightTexture lightmapTextureManager, float f, double d, double e, double g) { }

	private static boolean shouldWarnOnSetupTerrain = true;

	@Inject(at = @At("HEAD"), method = "setupTerrain", cancellable = true)
	private void onSetupTerrain(Camera camera, Frustum frustum, boolean bl, int i, boolean bl2, CallbackInfo ci) {
		if (shouldWarnOnSetupTerrain) {
			CanvasMod.LOG.warn("[Canvas] WorldRenderer.setupTerrain() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.cancel();
			shouldWarnOnSetupTerrain = false;
		}
	}

	@Inject(at = @At("HEAD"), method = "renderChunkDebugInfo", cancellable = true)
	private void onRenderChunkDebugInfo(Camera camera, CallbackInfo ci) {
		ci.cancel();
	}

	@Redirect(method = "scheduleChunkRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BuiltChunkStorage;scheduleRebuild(IIIZ)V"), require = 1)
	private void onScheduleChunkRender(ViewArea storage, int x, int y, int z, boolean urgent) {
		((CanvasWorldRenderer) (Object) this).scheduleRegionRender(x, y, z, urgent);
	}

	@Redirect(method = "reload()V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;viewDistance:I", ordinal = 1))
	private int onReloadZeroChunkStorage(Options options) {
		return 0;
	}

	private static boolean shouldWarnOnRenderLayer = true;

	@Inject(at = @At("HEAD"), method = "renderLayer", cancellable = true)
	private void onRenderLayer(CallbackInfo ci) {
		if (shouldWarnOnRenderLayer) {
			CanvasMod.LOG.warn("[Canvas] WorldRenderer.renderLayer() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.cancel();
			shouldWarnOnRenderLayer = false;
		}
	}

	private static boolean shouldWarnGetAdjacentChunk = true;

	@Inject(at = @At("HEAD"), method = "getAdjacentChunk", cancellable = true)
	private void onGetAdjacentChunk(CallbackInfoReturnable<RenderChunk> ci) {
		if (shouldWarnGetAdjacentChunk) {
			CanvasMod.LOG.warn("[Canvas] WorldRenderer.getAdjacentChunk() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.setReturnValue(null);
			shouldWarnGetAdjacentChunk = false;
		}
	}

	private static boolean shouldWarnOnUpdateChunks = true;

	@Inject(at = @At("HEAD"), method = "updateChunks", cancellable = true)
	private void onUpdateChunks(CallbackInfo ci) {
		if (shouldWarnOnUpdateChunks) {
			CanvasMod.LOG.warn("[Canvas] WorldRenderer.updateChunks() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.cancel();
			shouldWarnOnUpdateChunks = false;
		}
	}

	/**
	 * @author grondag
	 * @reason prevent mishap
	 */
	@Overwrite
	private void resetTransparencyShader() {
		// NOOP
	}

	/**
	 * @author grondag
	 * @reason prevent mishap
	 */
	@Overwrite
	private void loadTransparencyShader() {
		// Will be called by WorldRenderer.apply() during resource load (and ignored)
		// NOOP
	}

	@Override
	public void canvas_setupFabulousBuffers() {
		if (Pipeline.isFabulous()) {
			translucentFramebuffer = new FabulousFrameBuffer(Pipeline.fabTranslucentFbo, Pipeline.fabTranslucentColor, Pipeline.fabTranslucentDepth);
			entityFramebuffer = new FabulousFrameBuffer(Pipeline.fabEntityFbo, Pipeline.fabEntityColor, Pipeline.fabEntityDepth);
			particlesFramebuffer = new FabulousFrameBuffer(Pipeline.fabParticleFbo, Pipeline.fabParticleColor, Pipeline.fabParticleDepth);
			weatherFramebuffer = new FabulousFrameBuffer(Pipeline.fabWeatherFbo, Pipeline.fabWeatherColor, Pipeline.fabWeatherDepth);
			cloudsFramebuffer = new FabulousFrameBuffer(Pipeline.fabCloudsFbo, Pipeline.fabCloudsColor, Pipeline.fabCloudsDepth);
		} else {
			translucentFramebuffer = null;
			entityFramebuffer = null;
			particlesFramebuffer = null;
			weatherFramebuffer = null;
			cloudsFramebuffer = null;
		}
	}

	@Override
	public void canvas_reload() {
		// not used by us
		//needsTerrainUpdate = true;

		canvas_setupFabulousBuffers();

		if (world != null) {
			world.clearTintCaches();
		}

		cloudsDirty = true;
		ItemBlockRenderTypes.setFancy(true);

		synchronized (noCullingBlockEntities) {
			noCullingBlockEntities.clear();
		}
	}

	@Override
	public ClientLevel canvas_world() {
		return world;
	}

	@Override
	public void canvas_setWorldNoSideEffects(ClientLevel world) {
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
	public RenderBuffers canvas_bufferBuilders() {
		return bufferBuilders;
	}

	@Override
	public int canvas_getAndIncrementFrameIndex() {
		return frame++;
	}

	@Override
	public RunningTrimmedMean canvas_chunkUpdateSmoother() {
		return chunkUpdateSmoother;
	}

	@Override
	public boolean canvas_canDrawEntityOutlines() {
		return canDrawEntityOutlines();
	}

	@Override
	public RenderTarget canvas_entityOutlinesFramebuffer() {
		return entityOutlinesFramebuffer;
	}

	@Override
	public PostChain canvas_entityOutlineShader() {
		return entityOutlineShader;
	}

	@Override
	public Set<BlockEntity> canvas_noCullingBlockEntities() {
		return noCullingBlockEntities;
	}

	@Override
	public void canvas_drawBlockOutline(PoseStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState) {
		drawShapeOutline(matrixStack, vertexConsumer, blockState.getShape(world, blockPos, CollisionContext.of(entity)), blockPos.getX() - d, blockPos.getY() - e, blockPos.getZ() - f, 0.0F, 0.0F, 0.0F, 0.4F);
	}

	@Override
	public void canvas_renderWorldBorder(Camera camera) {
		renderWorldBorder(camera);
	}

	@Override
	public Long2ObjectMap<SortedSet<BlockDestructionProgress>> canvas_blockBreakingProgressions() {
		return blockBreakingProgressions;
	}

	@Override
	public void canvas_renderEntity(Entity entity, double d, double e, double f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider) {
		renderEntity(entity, d, e, f, g, matrixStack, vertexConsumerProvider);
	}

	@Override
	public void canvas_renderWeather(LightTexture lightmapTextureManager, float f, double d, double e, double g) {
		renderWeather(lightmapTextureManager, f, d, e, g);
	}

	@Override
	public void canvas_setEntityCounts(int regularEntityCountIn, int blockEntityCountIn) {
		regularEntityCount = regularEntityCountIn;
		blockEntityCount = blockEntityCountIn;
	}
}
