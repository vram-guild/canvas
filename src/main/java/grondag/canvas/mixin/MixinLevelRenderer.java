/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.mixin;

import java.util.Set;
import java.util.SortedSet;

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
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import grondag.canvas.CanvasMod;
import grondag.canvas.mixinterface.LevelRendererExt;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.render.FabulousFrameBuffer;
import grondag.canvas.render.world.CanvasWorldRenderer;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer implements LevelRendererExt {
	@Shadow private ClientLevel level;
	@Shadow private boolean generateClouds;
	@Shadow private EntityRenderDispatcher entityRenderDispatcher;
	// PERF: prevent wasteful allocation of these - they are not all used with Canvas and take a lot of space
	@Shadow private RenderBuffers renderBuffers;
	@Shadow private int renderedEntities;
	@Shadow private int culledEntities;
	@Shadow private RunningTrimmedMean frameTimes;
	@Shadow private RenderTarget entityTarget;
	@Shadow private PostChain entityEffect;
	@Shadow private Set<BlockEntity> globalBlockEntities;
	@Shadow private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;
	@Shadow private RenderTarget translucentTarget;
	@Shadow private RenderTarget itemEntityTarget;
	@Shadow private RenderTarget particlesTarget;
	@Shadow private RenderTarget weatherTarget;
	@Shadow private RenderTarget cloudsTarget;
	@Shadow protected boolean shouldShowEntityOutlines() {
		return false;
	}

	@Shadow private static void renderShape(PoseStack matrixStack, VertexConsumer vertexConsumer, VoxelShape voxelShape, double d, double e, double f, float g, float h, float i, float j) { }

	@Shadow private void renderWorldBorder(Camera camera) { }

	@Shadow private void renderEntity(Entity entity, double d, double e, double f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider) { }

	@Shadow private void renderSnowAndRain(LightTexture lightmapTextureManager, float f, double d, double e, double g) { }

	private static boolean shouldWarnOnSetupTerrain = true;

	@Inject(at = @At("HEAD"), method = "setupRender", cancellable = true)
	private void onSetupRender(Camera camera, Frustum frustum, boolean bl, boolean bl2, CallbackInfo ci) {
		if (shouldWarnOnSetupTerrain) {
			CanvasMod.LOG.warn("[Canvas] WorldRenderer.setupRender() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.cancel();
			shouldWarnOnSetupTerrain = false;
		}
	}

	@Inject(at = @At("HEAD"), method = "renderDebug", cancellable = true)
	private void onRenderDebug(Camera camera, CallbackInfo ci) {
		ci.cancel();
	}

	@Redirect(method = "Lnet/minecraft/client/renderer/LevelRenderer;setSectionDirty(IIIZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ViewArea;setDirty(IIIZ)V"), require = 1)
	private void onScheduleChunkRender(ViewArea viewArea, int x, int y, int z, boolean urgent) {
		((CanvasWorldRenderer) (Object) this).scheduleRegionRender(x, y, z, urgent);
	}

	@Redirect(method = "allChanged()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getEffectiveRenderDistance()I", ordinal = 1))
	private int onReloadZeroChunkStorage(Options options) {
		return 0;
	}

	private static boolean shouldWarnOnRenderChunkLayer = true;

	@Inject(at = @At("HEAD"), method = "renderChunkLayer", cancellable = true)
	private void onRenderChunkLayer(CallbackInfo ci) {
		if (shouldWarnOnRenderChunkLayer) {
			CanvasMod.LOG.warn("[Canvas] LevelRenderer.renderChunkLayer() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.cancel();
			shouldWarnOnRenderChunkLayer = false;
		}
	}

	private static boolean shouldWarnGetRelativeFrom = true;

	@Inject(at = @At("HEAD"), method = "getRelativeFrom", cancellable = true)
	private void onGetRelativeFrom(CallbackInfoReturnable<RenderChunk> ci) {
		if (shouldWarnGetRelativeFrom) {
			CanvasMod.LOG.warn("[Canvas] LevelRenderer.getRelativeFrom() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.setReturnValue(null);
			shouldWarnGetRelativeFrom = false;
		}
	}

	private static boolean shouldWarnOnUpdateRenderChunks = true;

	@Inject(at = @At("HEAD"), method = "updateRenderChunks", cancellable = true)
	private void onUpdateRenderChunks(CallbackInfo ci) {
		if (shouldWarnOnUpdateRenderChunks) {
			CanvasMod.LOG.warn("[Canvas] WorldRenderer.updateRenderChunks() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.cancel();
			shouldWarnOnUpdateRenderChunks = false;
		}
	}

	/**
	 * @author grondag
	 * @reason prevent mishap
	 */
	@Overwrite
	private void deinitTransparency() {
		// NOOP
	}

	/**
	 * @author grondag
	 * @reason prevent mishap
	 */
	@Overwrite
	private void initTransparency() {
		// Will be called by WorldRenderer.apply() during resource load (and ignored)
		// NOOP
	}

	@Override
	public void canvas_setupFabulousBuffers() {
		if (Pipeline.isFabulous()) {
			translucentTarget = new FabulousFrameBuffer(Pipeline.fabTranslucentFbo, Pipeline.fabTranslucentColor, Pipeline.fabTranslucentDepth);
			itemEntityTarget = new FabulousFrameBuffer(Pipeline.fabEntityFbo, Pipeline.fabEntityColor, Pipeline.fabEntityDepth);
			particlesTarget = new FabulousFrameBuffer(Pipeline.fabParticleFbo, Pipeline.fabParticleColor, Pipeline.fabParticleDepth);
			weatherTarget = new FabulousFrameBuffer(Pipeline.fabWeatherFbo, Pipeline.fabWeatherColor, Pipeline.fabWeatherDepth);
			cloudsTarget = new FabulousFrameBuffer(Pipeline.fabCloudsFbo, Pipeline.fabCloudsColor, Pipeline.fabCloudsDepth);
		} else {
			translucentTarget = null;
			itemEntityTarget = null;
			particlesTarget = null;
			weatherTarget = null;
			cloudsTarget = null;
		}
	}

	@Override
	public void canvas_reload() {
		// not used by us
		//needsTerrainUpdate = true;

		canvas_setupFabulousBuffers();

		if (level != null) {
			level.clearTintCaches();
		}

		generateClouds = true;
		ItemBlockRenderTypes.setFancy(true);

		synchronized (globalBlockEntities) {
			globalBlockEntities.clear();
		}
	}

	@Override
	public ClientLevel canvas_world() {
		return level;
	}

	@Override
	public void canvas_setWorldNoSideEffects(ClientLevel world) {
		this.level = world;
	}

	@Override
	public EntityRenderDispatcher canvas_entityRenderDispatcher() {
		return entityRenderDispatcher;
	}

	@Override
	public RenderBuffers canvas_bufferBuilders() {
		return renderBuffers;
	}

	@Override
	public RunningTrimmedMean canvas_chunkUpdateSmoother() {
		return frameTimes;
	}

	@Override
	public boolean canvas_canDrawEntityOutlines() {
		return shouldShowEntityOutlines();
	}

	@Override
	public RenderTarget canvas_entityOutlinesFramebuffer() {
		return entityTarget;
	}

	@Override
	public PostChain canvas_entityOutlineShader() {
		return entityEffect;
	}

	@Override
	public Set<BlockEntity> canvas_noCullingBlockEntities() {
		return globalBlockEntities;
	}

	@Override
	public void canvas_drawBlockOutline(PoseStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState) {
		renderShape(matrixStack, vertexConsumer, blockState.getShape(level, blockPos, CollisionContext.of(entity)), blockPos.getX() - d, blockPos.getY() - e, blockPos.getZ() - f, 0.0F, 0.0F, 0.0F, 0.4F);
	}

	@Override
	public void canvas_renderWorldBorder(Camera camera) {
		renderWorldBorder(camera);
	}

	@Override
	public Long2ObjectMap<SortedSet<BlockDestructionProgress>> canvas_blockBreakingProgressions() {
		return destructionProgress;
	}

	@Override
	public void canvas_renderEntity(Entity entity, double d, double e, double f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider) {
		renderEntity(entity, d, e, f, g, matrixStack, vertexConsumerProvider);
	}

	@Override
	public void canvas_renderWeather(LightTexture lightmapTextureManager, float f, double d, double e, double g) {
		renderSnowAndRain(lightmapTextureManager, f, d, e, g);
	}

	@Override
	public void canvas_setEntityCounts(int regularEntityCountIn, int blockEntityCountIn) {
		renderedEntities = regularEntityCountIn;
		culledEntities = blockEntityCountIn;
	}
}
