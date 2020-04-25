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
package grondag.canvas.mixin;

import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FpsSmoother;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import grondag.canvas.CanvasMod;
import grondag.canvas.chunk.RenderRegionBuilder;
import grondag.canvas.mixinterface.WorldRendererExt;
import grondag.canvas.render.CanvasWorldRenderer;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer implements WorldRendererExt {
	@Shadow private MinecraftClient client;
	@Shadow private int renderDistance;
	@Shadow private ClientWorld world;
	@Shadow private double lastCameraX;
	@Shadow private double lastCameraY;
	@Shadow private double lastCameraZ;
	@Shadow private double lastCameraPitch;
	@Shadow private double lastCameraYaw;
	@Shadow private int frame;
	@Shadow private boolean needsTerrainUpdate;
	@Shadow private boolean cloudsDirty;
	@Shadow private TextureManager textureManager;
	@Shadow private EntityRenderDispatcher entityRenderDispatcher;
	// PERF: prevent wasteful allocation of these - they are never used with Canvas and take a lot of space - should allocate only 1 for immediate use
	@Shadow private BufferBuilderStorage bufferBuilders;
	@Shadow private int regularEntityCount;
	@Shadow private FpsSmoother chunkUpdateSmoother;
	@Shadow private Framebuffer entityOutlinesFramebuffer;
	@Shadow private ShaderEffect entityOutlineShader;
	@Shadow private Set<BlockEntity> noCullingBlockEntities;
	@Shadow private Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions;
	@Shadow private VertexFormat vertexFormat;

	@Shadow private void captureFrustum(Matrix4f matrix4f, Matrix4f matrix4f2, double d, double e, double f, Frustum frustum) {}
	@Shadow protected boolean canDrawEntityOutlines() { return false; }
	@Shadow private void drawBlockOutline(MatrixStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState) {}
	@Shadow private void renderWorldBorder(Camera camera) {}
	@Shadow private void renderEntity(Entity entity, double d, double e, double f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider) {}
	@Shadow private void renderWeather(LightmapTextureManager lightmapTextureManager, float f, double d, double e, double g) {}

	private final CanvasWorldRenderer canvasWorldRenderer = new CanvasWorldRenderer(this);

	@Inject(at = @At("HEAD"), method = "setupTerrain", cancellable = true)
	private void onSetupTerrain(Camera camera, Frustum frustum, boolean bl, int i, boolean bl2, CallbackInfo ci) {
		// TODO: suppress repeat warnings
		CanvasMod.LOG.warn("[Canvas] WorldRendererer.setupTerrain() called unexpectedly.");
		ci.cancel();
	}

	@Inject(at = @At("HEAD"), method = "getCompletedChunkCount", cancellable = true)
	private void onGetCompletedChunkCount(CallbackInfoReturnable<Integer> ci) {
		ci.setReturnValue(canvasWorldRenderer.completedChunkCount());
	}

	@Inject(at = @At("HEAD"), method = "renderChunkDebugInfo", cancellable = true)
	private void onRenderChunkDebugInfo(Camera camera, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(at = @At("HEAD"), method = "clearChunkRenderers", cancellable = true)
	private void onClearChunkRenderers(CallbackInfo ci) {
		canvasWorldRenderer.clearChunkRenderers();
		ci.cancel();
	}

	@Inject(at = @At("HEAD"), method = "setWorld")
	private void onSetWorld(@Nullable ClientWorld clientWorld, CallbackInfo ci) {
		canvasWorldRenderer.setWorld(clientWorld);
	}

	@Inject(at = @At("HEAD"), method = "isTerrainRenderComplete", cancellable = true)
	private void onIsTerrainRenderComplete(CallbackInfoReturnable<Boolean> ci) {
		ci.setReturnValue(canvasWorldRenderer.isTerrainRenderComplete());
	}

	@Inject(at = @At("HEAD"), method = "getChunksDebugString", cancellable = true)
	private void onGetChunksDebugString(CallbackInfoReturnable<String> ci) {
		final int len = canvasWorldRenderer.builtChunkStorage().regions().length;
		final int count = canvasWorldRenderer.completedChunkCount();
		final RenderRegionBuilder chunkBuilder = canvasWorldRenderer.chunkBuilder();
		final String result = String.format("C: %d/%d %sD: %d, %s", count, len, client.chunkCullingEnabled ? "(s) " : "", renderDistance, chunkBuilder == null ? "null" : chunkBuilder.getDebugString());
		ci.setReturnValue(result);
	}

	/**
	 * @reason performance
	 * @author grondag
	 */
	@Overwrite
	private void scheduleChunkRender(int x, int y, int z, boolean urgent) {
		canvasWorldRenderer.builtChunkStorage().scheduleRebuild(x, y, z, urgent);
	}

	// circumvent vanilla logic by faking null world and then do our load after
	ClientWorld saveWorld = null;

	@Inject(at = @At("HEAD"), method = "reload")
	private void beforeReload(CallbackInfo ci) {
		saveWorld = world;
		world = null;
	}

	@Inject(at = @At("RETURN"), method = "reload")
	private void afterReload(CallbackInfo ci) {
		world = saveWorld;
		if (world != null) {
			world.reloadColor();
			RenderLayers.setFancyGraphics(client.options.fancyGraphics);
			renderDistance = client.options.viewDistance;

			canvasWorldRenderer.reload();

			needsTerrainUpdate = true;
			cloudsDirty = true;

			synchronized(noCullingBlockEntities) {
				noCullingBlockEntities.clear();
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "renderLayer", cancellable = true)
	private void onRenderLayer(CallbackInfo ci) {
		// TODO: suppress repeat warnings
		CanvasMod.LOG.warn("[Canvas] WorldRendererer.renderLayer() called unexpectedly.");
		ci.cancel();
	}

	@Inject(at = @At("HEAD"), method = "getAdjacentChunk", cancellable = true)
	private void onGetAdjacentChunk(CallbackInfoReturnable<BuiltChunk> ci) {
		// TODO: suppress repeat warnings
		CanvasMod.LOG.warn("[Canvas] WorldRendererer.getAdjacentChunk() called unexpectedly.");
		ci.setReturnValue(null);
	}

	@Inject(at = @At("HEAD"), method = "updateChunks", cancellable = true)
	private void onUpdateChunks(CallbackInfo ci) {
		// TODO: suppress repeat warnings
		CanvasMod.LOG.warn("[Canvas] WorldRendererer.udpateChunks() called unexpectedly.");
		ci.cancel();
	}

	@Inject(at = @At("HEAD"), method = "render", cancellable = true)
	public void render(MatrixStack matrixStack, float f, long startTime, boolean bl, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
		canvasWorldRenderer.renderWorld(matrixStack, f, startTime, bl, camera, gameRenderer, lightmapTextureManager, matrix4f);
		ci.cancel();
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
		((WorldRenderer)(Object) this).reload();
	}

	@Override
	public ClientWorld canvas_world() {
		return world;
	}

	@Override
	public boolean canvas_checkNeedsTerrainUpdate(Vec3d cameraPos, float pitch, float yaw) {
		needsTerrainUpdate = needsTerrainUpdate || !canvasWorldRenderer.chunksToRebuild.isEmpty() || cameraPos.x != lastCameraX || cameraPos.y != lastCameraY || cameraPos.z != lastCameraZ || pitch != lastCameraPitch || yaw != lastCameraYaw;
		lastCameraX = cameraPos.x;
		lastCameraY = cameraPos.y;
		lastCameraZ = cameraPos.z;
		lastCameraPitch = pitch;
		lastCameraYaw = yaw;

		return needsTerrainUpdate;
	}

	@Override
	public void canvas_setNeedsTerrainUpdate(boolean needsUpdate) {
		needsTerrainUpdate = needsUpdate;
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
	public void canvas_setEntityCount(int count) {
		regularEntityCount = count;
	}

	@Override
	public VertexFormat canvas_vertexFormat() {
		return vertexFormat;
	}
}
