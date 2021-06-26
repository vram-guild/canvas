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

package grondag.canvas.render;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.Option;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.impl.client.rendering.WorldRenderContextImpl;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.apiimpl.rendercontext.BlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.EntityBlockRenderContext;
import grondag.canvas.buffer.encoding.CanvasImmediate;
import grondag.canvas.buffer.encoding.DrawableBuffer;
import grondag.canvas.compat.FirstPersonModelHolder;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.property.MaterialTarget;
import grondag.canvas.material.state.RenderContextState;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.mixinterface.BufferBuilderStorageExt;
import grondag.canvas.mixinterface.WorldRendererExt;
import grondag.canvas.perf.Timekeeper;
import grondag.canvas.perf.Timekeeper.ProfilerGroup;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.PipelineManager;
import grondag.canvas.render.frustum.RegionCullingFrustum;
import grondag.canvas.render.frustum.TerrainFrustum;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.GlProgramManager;
import grondag.canvas.shader.data.MatrixData;
import grondag.canvas.shader.data.MatrixState;
import grondag.canvas.shader.data.ScreenRenderState;
import grondag.canvas.shader.data.ShaderDataManager;
import grondag.canvas.shader.data.ShadowMatrixData;
import grondag.canvas.terrain.occlusion.OcclusionInputManager;
import grondag.canvas.terrain.occlusion.OcclusionResultManager;
import grondag.canvas.terrain.occlusion.PotentiallyVisibleSetManager;
import grondag.canvas.terrain.occlusion.SortableVisibleRegionList;
import grondag.canvas.terrain.occlusion.TerrainIterator;
import grondag.canvas.terrain.occlusion.VisibleRegionList;
import grondag.canvas.terrain.region.RenderRegion;
import grondag.canvas.terrain.region.RenderRegionBuilder;
import grondag.canvas.terrain.region.RenderRegionStorage;
import grondag.canvas.terrain.render.TerrainLayerRenderer;
import grondag.canvas.varia.GFX;

public class CanvasWorldRenderer extends WorldRenderer {
	private static CanvasWorldRenderer instance;

	/** Tracks which regions had rebuilds requested, both camera and shadow view, and causes some to get built each frame. */
	public final RegionRebuildManager regionRebuildManager = new RegionRebuildManager();

	public final TerrainIterator terrainIterator = new TerrainIterator(this);
	public final PotentiallyVisibleSetManager potentiallyVisibleSetManager = new PotentiallyVisibleSetManager();
	public final RenderRegionStorage renderRegionStorage = new RenderRegionStorage(this);
	public final OcclusionInputManager occlusionInputStatus = new OcclusionInputManager(this);
	public final OcclusionResultManager occlusionStateManager = new OcclusionResultManager(this);

	/**
	 * Updated every frame and used by external callers looking for the vanilla world renderer frustum.
	 * Differs from vanilla in that it may not include FOV distortion in the frustum and can include
	 * some padding to minimize edge tearing.
	 *
	 * <p>A snapshot of this is used for terrain culling - usually off thread. The snapshot lives inside TerrainOccluder.
	 */
	public final TerrainFrustum terrainFrustum = new TerrainFrustum();
	public final SortableVisibleRegionList cameraVisibleRegions = new SortableVisibleRegionList();
	public final VisibleRegionList[] shadowVisibleRegions = new VisibleRegionList[ShadowMatrixData.CASCADE_COUNT];

	private final RegionCullingFrustum entityCullingFrustum = new RegionCullingFrustum(renderRegionStorage);
	private final RenderContextState contextState = new RenderContextState();
	private final CanvasImmediate worldRenderImmediate = new CanvasImmediate(new BufferBuilder(256), CanvasImmediate.entityBuilders(), contextState);
	/** Contains the player model output when not in 3rd-person view, separate to draw in shadow render only. */
	private final CanvasImmediate shadowExtrasImmediate = new CanvasImmediate(new BufferBuilder(256), new Object2ObjectLinkedOpenHashMap<>(), contextState);
	private final CanvasParticleRenderer particleRenderer = new CanvasParticleRenderer(entityCullingFrustum);
	private final WorldRenderContextImpl eventContext = new WorldRenderContextImpl();

	/** Used to avoid camera rotation in managed draws.  Kept to avoid reallocation every frame. */
	private final MatrixStack identityStack = new MatrixStack();

	private final WorldRendererExt vanillaWorldRenderer;

	private RenderRegionBuilder regionBuilder;
	private ClientWorld world;

	// these are measured in chunks, not blocks
	private int chunkRenderDistance;
	private int squaredChunkRenderDistance;
	private int squaredChunkRetentionDistance;

	public CanvasWorldRenderer(MinecraftClient client, BufferBuilderStorage bufferBuilders) {
		super(client, bufferBuilders);

		for (int i = 0; i < ShadowMatrixData.CASCADE_COUNT; ++i) {
			shadowVisibleRegions[i] = new VisibleRegionList();
		}

		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: CanvasWorldRenderer init");
		}

		vanillaWorldRenderer = (WorldRendererExt) this;
		instance = this;
		computeDistances();
	}

	public static CanvasWorldRenderer instance() {
		return instance;
	}

	private void computeDistances() {
		@SuppressWarnings("resource")
		int renderDistance = MinecraftClient.getInstance().options.viewDistance;
		chunkRenderDistance = renderDistance;
		squaredChunkRenderDistance = renderDistance * renderDistance;
		renderDistance += 2;
		squaredChunkRetentionDistance = renderDistance * renderDistance;
	}

	public void updateProjection(Camera camera, float tickDelta, double fov) {
		terrainFrustum.updateProjection(camera, tickDelta, fov);
	}

	public RenderRegionBuilder regionBuilder() {
		return regionBuilder;
	}

	public ClientWorld getWorld() {
		return world;
	}

	@Override
	public void setWorld(@Nullable ClientWorld clientWorld) {
		// happens here to avoid creating before renderer is initialized
		if (regionBuilder == null) {
			regionBuilder = new RenderRegionBuilder();
		}

		// DitherTexture.instance().initializeIfNeeded();
		world = clientWorld;
		cameraVisibleRegions.clear();
		terrainIterator.reset();
		potentiallyVisibleSetManager.clear();
		renderRegionStorage.clear();
		// we don't want to use our collector unless we are in a world
		((BufferBuilderStorageExt) vanillaWorldRenderer.canvas_bufferBuilders()).canvas_setEntityConsumers(clientWorld == null ? null : worldRenderImmediate);
		// Mixins mostly disable what this does
		super.setWorld(clientWorld);
	}

	/**
	 * Terrain rebuild is partly lazy/incremental
	 * The occluder has a thread-safe version indicating visibility test validity.
	 * The raster must be redrawn whenever the frustum view changes but prior visibility
	 * checks remain valid until the player location changes more than 1 block
	 * (regions are fuzzed one block to allow this) or a region that was already drawn into
	 * the raster is updated with different visibility information.  New occluders can
	 * also  be added to the existing raster.
	 * or
	 */
	public void setupTerrain(Camera camera, int frameCounter, boolean shouldCullChunks) {
		final int renderDistance = chunkRenderDistance;
		final RenderRegionStorage regionStorage = renderRegionStorage;
		final TerrainIterator terrainIterator = this.terrainIterator;
		final MinecraftClient mc = MinecraftClient.getInstance();

		regionStorage.closeRegionsOnRenderThread();

		mc.getProfiler().push("camera");
		MaterialConditionImpl.update();
		GlProgramManager.INSTANCE.onRenderTick();
		final BlockPos cameraBlockPos = camera.getBlockPos();
		final RenderRegion cameraRegion = world == null || world.isOutOfHeightLimit(cameraBlockPos) ? null : regionStorage.getOrCreateRegion(cameraBlockPos);

		mc.getProfiler().swap("buildnear");

		if (cameraRegion != null) {
			regionRebuildManager.buildNearRegionIfNeeded(cameraRegion);
			cameraRegion.neighbors.forEachAvailable(regionRebuildManager::buildNearRegionIfNeeded);
		}

		regionRebuildManager.processExternalBuildRequests();

		Entity.setRenderDistanceMultiplier(MathHelper.clamp(mc.options.viewDistance / 8.0D, 1.0D, 2.5D));

		mc.getProfiler().swap("update");

		if (Configurator.terrainSetupOffThread) {
			int state = terrainIterator.state();

			if (state == TerrainIterator.COMPLETE) {
				copyVisibleRegionsFromIterator();
				regionRebuildManager.scheduleOrBuild(terrainIterator.updateRegions);
				terrainIterator.reset();
				state = TerrainIterator.IDLE;
			}

			if (state == TerrainIterator.IDLE) {
				final int occlusionInputFlags = occlusionInputStatus.getAndClearStatus();

				if (occlusionInputFlags != OcclusionInputManager.CURRENT) {
					terrainIterator.prepare(cameraRegion, camera, terrainFrustum, renderDistance, shouldCullChunks, occlusionInputFlags);
					regionBuilder.executor.execute(terrainIterator);
				}
			}
		} else {
			// Run iteration on main thread
			final int occlusionInputFlags = occlusionInputStatus.getAndClearStatus();

			if (occlusionInputFlags != OcclusionInputManager.CURRENT) {
				terrainIterator.prepare(cameraRegion, camera, terrainFrustum, renderDistance, shouldCullChunks, occlusionInputFlags);
				terrainIterator.run(null);
				copyVisibleRegionsFromIterator();
				terrainIterator.reset();
			}
		}

		mc.getProfiler().pop();
	}

	private void copyVisibleRegionsFromIterator() {
		if (terrainIterator.includeCamera()) {
			cameraVisibleRegions.copyFrom(terrainIterator.visibleRegions);
		}

		if (terrainIterator.includeShadow()) {
			shadowVisibleRegions[0].copyFrom(terrainIterator.shadowVisibleRegions[0]);
			shadowVisibleRegions[1].copyFrom(terrainIterator.shadowVisibleRegions[1]);
			shadowVisibleRegions[2].copyFrom(terrainIterator.shadowVisibleRegions[2]);
			shadowVisibleRegions[3].copyFrom(terrainIterator.shadowVisibleRegions[3]);
		}
	}

	private boolean shouldCullChunks(BlockPos pos) {
		final MinecraftClient mc = MinecraftClient.getInstance();
		boolean result = mc.chunkCullingEnabled;

		if (mc.player.isSpectator() && !world.isOutOfHeightLimit(pos.getY()) && world.getBlockState(pos).isOpaqueFullCube(world, pos)) {
			result = false;
		}

		return result;
	}

	public void renderWorld(MatrixStack viewMatrixStack, float tickDelta, long frameStartNanos, boolean blockOutlines, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix) {
		final WorldRendererExt wr = vanillaWorldRenderer;
		final MinecraftClient mc = MinecraftClient.getInstance();
		final WorldRenderer mcwr = mc.worldRenderer;
		final Framebuffer mcfb = mc.getFramebuffer();
		final BlockRenderContext blockContext = BlockRenderContext.get();
		final EntityBlockRenderContext entityBlockContext = EntityBlockRenderContext.get();
		final ClientWorld world = this.world;
		final BufferBuilderStorage bufferBuilders = wr.canvas_bufferBuilders();
		final EntityRenderDispatcher entityRenderDispatcher = wr.canvas_entityRenderDispatcher();
		final boolean advancedTranslucency = Pipeline.isFabulous();
		final Vec3d cameraVec3d = camera.getPos();
		final double frameCameraX = cameraVec3d.getX();
		final double frameCameraY = cameraVec3d.getY();
		final double frameCameraZ = cameraVec3d.getZ();
		final MatrixStack identityStack = this.identityStack;

		RenderSystem.setShaderGameTime(this.world.getTime(), tickDelta);
		MinecraftClient.getInstance().getBlockEntityRenderDispatcher().configure(world, camera, mc.crosshairTarget);
		entityRenderDispatcher.configure(world, camera, mc.targetedEntity);
		final Profiler profiler = world.getProfiler();

		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.StartWorld, "light_updates");
		mc.world.getChunkManager().getLightingProvider().doLightUpdates(Integer.MAX_VALUE, true, true);

		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.StartWorld, "clear");
		Pipeline.defaultFbo.bind();

		// This does not actually render anything - what it does do is set the current clear color
		// Color is captured via a mixin for use in shaders
		BackgroundRenderer.render(camera, tickDelta, mc.world, mc.options.viewDistance, gameRenderer.getSkyDarkness(tickDelta));
		// We don't depend on this but call it here for compatibility
		BackgroundRenderer.setFogBlack();

		if (Pipeline.config().runVanillaClear) {
			RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
		}

		final float viewDistance = gameRenderer.getViewDistance();
		final boolean thickFog = mc.world.getSkyProperties().useThickFog(MathHelper.floor(frameCameraX), MathHelper.floor(frameCameraY)) || mc.inGameHud.getBossBarHud().shouldThickenFog();

		if (mc.options.viewDistance >= 4) {
			// We call applyFog here to do some state capture - otherwise has no effect
			BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_SKY, viewDistance, thickFog);
			ShaderDataManager.captureFogDistances();
			WorldRenderDraws.profileSwap(profiler, ProfilerGroup.StartWorld, "sky");
			// NB: fog / sky renderer normalcy get viewMatrixStack but we apply camera rotation in VertexBuffer mixin
			RenderSystem.setShader(GameRenderer::getPositionShader);

			// Mojang passes applyFog as a lambda here because they sometimes call it twice.
			renderSky(viewMatrixStack, projectionMatrix, tickDelta, () -> {
				BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_SKY, viewDistance, thickFog);
			});
		}

		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.StartWorld, "fog");
		BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(viewDistance - 16.0F, 32.0F), thickFog);
		ShaderDataManager.captureFogDistances();

		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.StartWorld, "terrain_setup");
		setupTerrain(camera, wr.canvas_getAndIncrementFrameIndex(), shouldCullChunks(camera.getBlockPos()));
		eventContext.setFrustum(terrainFrustum);

		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.StartWorld, "after_setup_event");
		WorldRenderEvents.AFTER_SETUP.invoker().afterSetup(eventContext);

		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.StartWorld, "updatechunks");
		final int maxFps = mc.options.maxFps;
		long maxFpsLimit;

		if (maxFps == Option.FRAMERATE_LIMIT.getMax()) {
			maxFpsLimit = 0L;
		} else {
			maxFpsLimit = 1000000000 / maxFps;
		}

		final long nowTime = Util.getMeasuringTimeNano();
		final long usedTime = nowTime - frameStartNanos;

		// No idea what the 3/2 is for - looks like a hack
		final long updateBudget = wr.canvas_chunkUpdateSmoother().getTargetUsedTime(usedTime) * 3L / 2L;
		final long clampedBudget = MathHelper.clamp(updateBudget, maxFpsLimit, 33333333L);

		regionBuilder.upload();
		regionRebuildManager.processScheduledRegions(frameStartNanos + clampedBudget);

		// Note these don't have an effect when canvas pipeline is active - lighting happens in the shader
		// but they are left intact to handle any fix-function renders we don't catch
		if (this.world.getSkyProperties().isDarkened()) {
			// True for nether - yarn names here are not great
			// Causes lower face to be lit like top face
			DiffuseLighting.enableForLevel(MatrixData.viewMatrix);
		} else {
			DiffuseLighting.disableForLevel(MatrixData.viewMatrix);
		}

		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.StartWorld, "before_entities_event");
		WorldRenderEvents.BEFORE_ENTITIES.invoker().beforeEntities(eventContext);

		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.StartWorld, "entities");
		int entityCount = 0;
		int blockEntityCount = 0;

		if (advancedTranslucency) {
			final Framebuffer entityFramebuffer = mcwr.getEntityFramebuffer();
			entityFramebuffer.copyDepthFrom(mcfb);
		}

		final boolean canDrawEntityOutlines = wr.canvas_canDrawEntityOutlines();

		if (canDrawEntityOutlines) {
			wr.canvas_entityOutlinesFramebuffer().clear(MinecraftClient.IS_SYSTEM_MAC);
		}

		Pipeline.defaultFbo.bind();

		boolean didRenderOutlines = false;
		final CanvasImmediate immediate = worldRenderImmediate;
		final Iterator<Entity> entities = world.getEntities().iterator();
		final ShaderEffect entityOutlineShader = wr.canvas_entityOutlineShader();
		final SortableVisibleRegionList visibleRegions = cameraVisibleRegions;
		entityBlockContext.tickDelta(tickDelta);
		entityBlockContext.collectors = immediate.collectors;
		blockContext.collectors = immediate.collectors;
		SkyShadowRenderer.suppressEntityShadows(mc);

		// Because we are passing identity stack to entity renders we need to
		// apply the view transform to vanilla renders.
		final MatrixStack renderSystemModelViewStack = RenderSystem.getModelViewStack();
		renderSystemModelViewStack.push();
		renderSystemModelViewStack.method_34425(viewMatrixStack.peek().getModel());
		RenderSystem.applyModelViewMatrix();

		entityCullingFrustum.enableRegionCulling = Configurator.cullEntityRender;

		while (entities.hasNext()) {
			final Entity entity = entities.next();
			boolean isFirstPersonPlayer = false;

			if (!entityRenderDispatcher.shouldRender(entity, entityCullingFrustum, frameCameraX, frameCameraY, frameCameraZ) && !entity.hasPassengerDeep(mc.player)) {
				continue;
			}

			if ((entity == camera.getFocusedEntity() && !FirstPersonModelHolder.handler.isThirdPerson(this, camera, viewMatrixStack) && (!(camera.getFocusedEntity() instanceof LivingEntity)
					|| !((LivingEntity) camera.getFocusedEntity()).isSleeping()))
					|| (entity instanceof ClientPlayerEntity && camera.getFocusedEntity() != entity)
			) {
				if (!Pipeline.shadowsEnabled()) {
					continue;
				}

				isFirstPersonPlayer = true;
			}

			++entityCount;
			contextState.setCurrentEntity(entity);

			if (entity.age == 0) {
				entity.lastRenderX = entity.getX();
				entity.lastRenderY = entity.getY();
				entity.lastRenderZ = entity.getZ();
			}

			VertexConsumerProvider renderProvider;

			if (isFirstPersonPlayer) {
				// only render as shadow
				renderProvider = shadowExtrasImmediate;
			} else if (canDrawEntityOutlines && mc.hasOutline(entity)) {
				didRenderOutlines = true;
				final OutlineVertexConsumerProvider outlineVertexConsumerProvider = bufferBuilders.getOutlineVertexConsumers();
				renderProvider = outlineVertexConsumerProvider;
				final int teamColor = entity.getTeamColorValue();
				final int red = (teamColor >> 16 & 255);
				final int green = (teamColor >> 8 & 255);
				final int blue = teamColor & 255;
				outlineVertexConsumerProvider.setColor(red, green, blue, 255);
			} else {
				renderProvider = immediate;
			}

			entityBlockContext.setPosAndWorldFromEntity(entity);

			// Item entity translucent typically gets drawn here in vanilla because there's no dedicated buffer for it
			wr.canvas_renderEntity(entity, frameCameraX, frameCameraY, frameCameraZ, tickDelta, identityStack, renderProvider);
		}

		contextState.setCurrentEntity(null);
		SkyShadowRenderer.restoreEntityShadows(mc);

		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.StartWorld, "blockentities");
		final int visibleRegionCount = visibleRegions.size();
		final Set<BlockEntity> noCullingBlockEntities = wr.canvas_noCullingBlockEntities();

		for (int regionIndex = 0; regionIndex < visibleRegionCount; ++regionIndex) {
			final List<BlockEntity> list = visibleRegions.get(regionIndex).getBuildState().getBlockEntities();

			final Iterator<BlockEntity> itBER = list.iterator();

			while (itBER.hasNext()) {
				final BlockEntity blockEntity = itBER.next();
				final BlockPos blockPos = blockEntity.getPos();
				VertexConsumerProvider outputConsumer = immediate;
				contextState.setCurrentBlockEntity(blockEntity);

				identityStack.push();
				identityStack.translate(blockPos.getX() - frameCameraX, blockPos.getY() - frameCameraY, blockPos.getZ() - frameCameraZ);
				final SortedSet<BlockBreakingInfo> sortedSet = wr.canvas_blockBreakingProgressions().get(blockPos.asLong());

				if (sortedSet != null && !sortedSet.isEmpty()) {
					final int stage = sortedSet.last().getStage();

					if (stage >= 0) {
						final MatrixStack.Entry xform = identityStack.peek();
						final VertexConsumer overlayConsumer = new OverlayVertexConsumer(bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage)), xform.getModel(), xform.getNormal());

						outputConsumer = (renderLayer) -> {
							final VertexConsumer baseConsumer = immediate.getBuffer(renderLayer);
							return renderLayer.hasCrumbling() ? VertexConsumers.union(overlayConsumer, baseConsumer) : baseConsumer;
						};
					}
				}

				++blockEntityCount;
				WorldRenderDraws.renderBlockEntitySafely(blockEntity, tickDelta, identityStack, outputConsumer);
				identityStack.pop();
			}
		}

		synchronized (noCullingBlockEntities) {
			final Iterator<BlockEntity> globalBERs = noCullingBlockEntities.iterator();

			while (globalBERs.hasNext()) {
				final BlockEntity blockEntity2 = globalBERs.next();
				final BlockPos blockPos2 = blockEntity2.getPos();
				contextState.setCurrentBlockEntity(blockEntity2);
				identityStack.push();
				identityStack.translate(blockPos2.getX() - frameCameraX, blockPos2.getY() - frameCameraY, blockPos2.getZ() - frameCameraZ);
				++blockEntityCount;
				WorldRenderDraws.renderBlockEntitySafely(blockEntity2, tickDelta, identityStack, immediate);
				identityStack.pop();
			}
		}

		contextState.setCurrentBlockEntity(null);

		RenderState.disable();

		try (DrawableBuffer entityBuffer = immediate.prepareDrawable(MaterialTarget.MAIN);
			DrawableBuffer shadowExtrasBuffer = shadowExtrasImmediate.prepareDrawable(MaterialTarget.MAIN)
		) {
			WorldRenderDraws.profileSwap(profiler, ProfilerGroup.ShadowMap, "shadow_map");
			SkyShadowRenderer.render(this, frameCameraX, frameCameraY, frameCameraZ, entityBuffer, shadowExtrasBuffer);
			shadowExtrasBuffer.close();

			WorldRenderDraws.profileSwap(profiler, ProfilerGroup.EndWorld, "terrain_solid");
			MatrixState.set(MatrixState.REGION);
			renderTerrainLayer(false, frameCameraX, frameCameraY, frameCameraZ);
			MatrixState.set(MatrixState.CAMERA);

			WorldRenderDraws.profileSwap(profiler, ProfilerGroup.EndWorld, "entity_draw_solid");
			entityBuffer.draw(false);
			entityBuffer.close();
		}

		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.EndWorld, "after_entities_event");
		WorldRenderEvents.AFTER_ENTITIES.invoker().afterEntities(eventContext);

		bufferBuilders.getOutlineVertexConsumers().draw();

		if (didRenderOutlines) {
			entityOutlineShader.render(tickDelta);
			Pipeline.defaultFbo.bind();
		}

		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.EndWorld, "destroyProgress");

		// honor damage render layer irrespective of model material
		blockContext.collectors = null;

		final ObjectIterator<Entry<SortedSet<BlockBreakingInfo>>> breakings = wr.canvas_blockBreakingProgressions().long2ObjectEntrySet().iterator();

		while (breakings.hasNext()) {
			final Entry<SortedSet<BlockBreakingInfo>> entry = breakings.next();
			final BlockPos breakPos = BlockPos.fromLong(entry.getLongKey());
			final double y = breakPos.getX() - frameCameraX;
			final double z = breakPos.getY() - frameCameraY;
			final double aa = breakPos.getZ() - frameCameraZ;

			if (y * y + z * z + aa * aa <= 1024.0D) {
				final SortedSet<BlockBreakingInfo> breakSet = entry.getValue();

				if (breakSet != null && !breakSet.isEmpty()) {
					final int stage = breakSet.last().getStage();
					identityStack.push();
					identityStack.translate(breakPos.getX() - frameCameraX, breakPos.getY() - frameCameraY, breakPos.getZ() - frameCameraZ);
					final MatrixStack.Entry xform = identityStack.peek();
					final VertexConsumer vertexConsumer2 = new OverlayVertexConsumer(bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage)), xform.getModel(), xform.getNormal());
					mc.getBlockRenderManager().renderDamage(world.getBlockState(breakPos), breakPos, world, identityStack, vertexConsumer2);
					identityStack.pop();
				}
			}
		}

		blockContext.collectors = immediate.collectors;

		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.EndWorld, "outline");
		final HitResult hitResult = mc.crosshairTarget;

		if (WorldRenderEvents.BEFORE_BLOCK_OUTLINE.invoker().beforeBlockOutline(eventContext, hitResult)) {
			if (blockOutlines && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
				final BlockPos blockOutlinePos = ((BlockHitResult) hitResult).getBlockPos();
				final BlockState blockOutlineState = world.getBlockState(blockOutlinePos);

				if (!blockOutlineState.isAir() && world.getWorldBorder().contains(blockOutlinePos)) {
					// THIS IS WHEN LIGHTENING RENDERS IN VANILLA
					final VertexConsumer blockOutlineConumer = immediate.getBuffer(RenderLayer.getLines());

					eventContext.prepareBlockOutline(camera.getFocusedEntity(), frameCameraX, frameCameraY, frameCameraZ, blockOutlinePos, blockOutlineState);

					if (WorldRenderEvents.BLOCK_OUTLINE.invoker().onBlockOutline(eventContext, eventContext)) {
						wr.canvas_drawBlockOutline(identityStack, blockOutlineConumer, camera.getFocusedEntity(), frameCameraX, frameCameraY, frameCameraZ, blockOutlinePos, blockOutlineState);
					}
				}
			}
		}

		RenderState.disable();

		// NB: view matrix is already applied to GL state before renderWorld is called
		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.EndWorld, "before_debug_event");
		WorldRenderEvents.BEFORE_DEBUG_RENDER.invoker().beforeDebugRender(eventContext);
		// We still pass in the transformed stack because that is what debug renderer normally gets
		mc.debugRenderer.render(viewMatrixStack, immediate, frameCameraX, frameCameraY, frameCameraZ);

		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.EndWorld, "draw_solid");

		// Should generally not have anything here but draw in case content injected in hooks
		immediate.drawCollectors(MaterialTarget.MAIN);

		// These should be empty and probably won't work, but prevent them from accumulating if somehow used.
		immediate.draw(RenderLayer.getArmorGlint());
		immediate.draw(RenderLayer.getArmorEntityGlint());
		immediate.draw(RenderLayer.getGlint());
		immediate.draw(RenderLayer.getDirectGlint());
		immediate.draw(RenderLayer.method_30676());
		immediate.draw(RenderLayer.getEntityGlint());
		immediate.draw(RenderLayer.getDirectEntityGlint());

		// draw order is important and our sorting mechanism doesn't cover
		immediate.draw(RenderLayer.getWaterMask());

		bufferBuilders.getEffectVertexConsumers().draw();

		visibleRegions.scheduleResort(cameraVec3d);

		if (advancedTranslucency) {
			WorldRenderDraws.profileSwap(profiler, ProfilerGroup.EndWorld, "translucent");

			Pipeline.translucentTerrainFbo.copyDepthFrom(Pipeline.defaultFbo);
			Pipeline.translucentTerrainFbo.bind();

			// in fabulous mode, the only thing that renders to terrain translucency
			// is terrain itself - so everything else can be rendered first

			// Lines draw to entity (item) target
			immediate.draw(RenderLayer.getLines());

			// PERF: Why is this here? Should be empty
			immediate.drawCollectors(MaterialTarget.TRANSLUCENT);

			// This catches entity layer and any remaining non-main layers
			immediate.draw();

			MatrixState.set(MatrixState.REGION);
			renderTerrainLayer(true, frameCameraX, frameCameraY, frameCameraZ);
			MatrixState.set(MatrixState.CAMERA);

			// NB: vanilla renders tripwire here but we combine into translucent

			Pipeline.translucentParticlesFbo.copyDepthFrom(Pipeline.defaultFbo);
			Pipeline.translucentParticlesFbo.bind();

			WorldRenderDraws.profileSwap(profiler, ProfilerGroup.EndWorld, "particles");
			particleRenderer.renderParticles(mc.particleManager, identityStack, immediate.collectors, lightmapTextureManager, camera, tickDelta);

			Pipeline.defaultFbo.bind();
		} else {
			WorldRenderDraws.profileSwap(profiler, ProfilerGroup.EndWorld, "translucent");
			MatrixState.set(MatrixState.REGION);
			renderTerrainLayer(true, frameCameraX, frameCameraY, frameCameraZ);
			MatrixState.set(MatrixState.CAMERA);

			// without fabulous transparency important that lines
			// and other translucent elements get drawn on top of terrain
			immediate.draw(RenderLayer.getLines());

			// PERF: how is this needed? - would either have been drawn above or will be drawn below
			immediate.drawCollectors(MaterialTarget.TRANSLUCENT);

			// This catches entity layer and any remaining non-main layers
			immediate.draw();

			WorldRenderDraws.profileSwap(profiler, ProfilerGroup.EndWorld, "particles");
			particleRenderer.renderParticles(mc.particleManager, identityStack, immediate.collectors, lightmapTextureManager, camera, tickDelta);
		}

		renderSystemModelViewStack.pop();
		RenderSystem.applyModelViewMatrix();

		RenderState.disable();

		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.EndWorld, "after_translucent_event");
		WorldRenderEvents.AFTER_TRANSLUCENT.invoker().afterTranslucent(eventContext);

		// TODO: need a new event here for weather/cloud targets that has matrix applies to render state
		// TODO: move the Mallib world last to the new event when fabulous is on

		if (Configurator.debugOcclusionBoxes) {
			WorldRenderDraws.renderCullBoxes(renderRegionStorage, viewMatrixStack, immediate, frameCameraX, frameCameraY, frameCameraZ, tickDelta);
		}

		RenderState.disable();
		GlProgram.deactivate();

		renderClouds(mc, profiler, viewMatrixStack, projectionMatrix, tickDelta, frameCameraX, frameCameraY, frameCameraZ);

		// WIP: need to properly target the designated buffer here in both clouds and weather
		// also need to ensure works with non-fabulous pipelines
		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.EndWorld, "weather");

		// Apply view transform locally for vanilla weather and world border rendering
		renderSystemModelViewStack.push();
		renderSystemModelViewStack.method_34425(viewMatrixStack.peek().getModel());
		RenderSystem.applyModelViewMatrix();

		if (advancedTranslucency) {
			RenderPhase.WEATHER_TARGET.startDrawing();
			wr.canvas_renderWeather(lightmapTextureManager, tickDelta, frameCameraX, frameCameraY, frameCameraZ);
			wr.canvas_renderWorldBorder(camera);
			RenderPhase.WEATHER_TARGET.endDrawing();
			PipelineManager.beFabulous();

			Pipeline.defaultFbo.bind();
		} else {
			GFX.depthMask(false);
			wr.canvas_renderWeather(lightmapTextureManager, tickDelta, frameCameraX, frameCameraY, frameCameraZ);
			wr.canvas_renderWorldBorder(camera);
			GFX.depthMask(true);
		}

		renderSystemModelViewStack.pop();
		RenderSystem.applyModelViewMatrix();

		// doesn't make any sense with our chunk culling scheme
		// this.renderChunkDebugInfo(camera);
		WorldRenderDraws.profileSwap(profiler, ProfilerGroup.AfterFabulous, "render_last_event");
		WorldRenderEvents.LAST.invoker().onLast(eventContext);

		GFX.depthMask(true);
		GFX.disableBlend();
		RenderSystem.applyModelViewMatrix();
		BackgroundRenderer.method_23792();
		entityBlockContext.collectors = null;
		blockContext.collectors = null;

		wr.canvas_setEntityCounts(entityCount, blockEntityCount);

		//RenderState.enablePrint = true;

		Timekeeper.instance.swap(ProfilerGroup.AfterFabulous, "after world");
	}

	private void renderClouds(MinecraftClient mc, Profiler profiler, MatrixStack identityStack, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ) {
		if (mc.options.getCloudRenderMode() != CloudRenderMode.OFF) {
			WorldRenderDraws.profileSwap(profiler, ProfilerGroup.EndWorld, "clouds");

			if (Pipeline.fabCloudsFbo > 0) {
				GFX.bindFramebuffer(GFX.GL_FRAMEBUFFER, Pipeline.fabCloudsFbo);
			}

			// NB: cloud renderer normally gets stack with view rotation but we apply that in VertexBuffer mixin
			renderClouds(identityStack, projectionMatrix, tickDelta, cameraX, cameraY, cameraZ);

			if (Pipeline.fabCloudsFbo > 0) {
				Pipeline.defaultFbo.bind();
			}
		}
	}

	void renderTerrainLayer(boolean isTranslucent, double x, double y, double z) {
		TerrainLayerRenderer.render(cameraVisibleRegions, x, y, z, isTranslucent);
	}

	void renderShadowLayer(int cascadeIndex, double x, double y, double z) {
		TerrainLayerRenderer.render(shadowVisibleRegions[cascadeIndex], x, y, z, false);
	}

	public int maxSquaredChunkRenderDistance() {
		return squaredChunkRenderDistance;
	}

	public int maxSquaredChunkRetentionDistance() {
		return squaredChunkRetentionDistance;
	}

	public void updateNoCullingBlockEntities(ObjectOpenHashSet<BlockEntity> removedBlockEntities, ObjectOpenHashSet<BlockEntity> addedBlockEntities) {
		((WorldRenderer) vanillaWorldRenderer).updateNoCullingBlockEntities(removedBlockEntities, addedBlockEntities);
	}

	public void scheduleRegionRender(int x, int y, int z, boolean urgent) {
		renderRegionStorage.scheduleRebuild(x << 4, y << 4, z << 4, urgent);
	}

	@Override
	public void render(MatrixStack viewMatrixStack, float tickDelta, long frameStartNanos, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix) {
		final MinecraftClient mc = MinecraftClient.getInstance();
		final boolean wasFabulous = Pipeline.isFabulous();

		PipelineManager.reloadIfNeeded();

		if (wasFabulous != Pipeline.isFabulous()) {
			vanillaWorldRenderer.canvas_setupFabulousBuffers();
		}

		if (mc.options.viewDistance != chunkRenderDistance) {
			reload();
		}

		mc.getProfiler().swap("dynamic_lighting");

		// All managed draws - including anything targeting vertex consumer - will have camera rotation applied
		// in shader - this gives better consistency with terrain rendering and may be more intuitive for lighting.
		// Unmanaged draws that do direct drawing will expect the matrix stack to have camera rotation in it and may
		// use it either to transform the render state or to transform vertices.
		// For this reason we have two different stacks.
		identityStack.peek().getModel().loadIdentity();
		identityStack.peek().getNormal().loadIdentity();

		final Matrix4f viewMatrix = viewMatrixStack.peek().getModel();
		terrainFrustum.prepare(viewMatrix, tickDelta, camera, terrainIterator.cameraOccluder.hasNearOccluders());
		entityCullingFrustum.prepare(viewMatrix, tickDelta, camera, projectionMatrix);
		ShaderDataManager.update(viewMatrixStack.peek(), projectionMatrix, camera);
		MatrixState.set(MatrixState.CAMERA);

		eventContext.prepare(this, identityStack, tickDelta, frameStartNanos, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, projectionMatrix, worldRenderImmediate, mc.getProfiler(), MinecraftClient.isFabulousGraphicsOrBetter(), world);

		WorldRenderEvents.START.invoker().onStart(eventContext);
		PipelineManager.beforeWorldRender();
		renderWorld(viewMatrixStack, tickDelta, frameStartNanos, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, projectionMatrix);
		WorldRenderEvents.END.invoker().onEnd(eventContext);

		RenderSystem.applyModelViewMatrix();
		MatrixState.set(MatrixState.SCREEN);
		ScreenRenderState.setRenderingHand(true);
	}

	@Override
	public void reload() {
		PipelineManager.reloadIfNeeded();

		// cause injections to fire but disable all other vanilla logic
		// by setting world to null temporarily
		final ClientWorld swapWorld = vanillaWorldRenderer.canvas_world();
		vanillaWorldRenderer.canvas_setWorldNoSideEffects(null);
		super.reload();
		vanillaWorldRenderer.canvas_setWorldNoSideEffects(swapWorld);

		// has the logic from super.reload() that requires private access
		vanillaWorldRenderer.canvas_reload();

		computeDistances();
		terrainIterator.reset();
		regionRebuildManager.clear();

		if (regionBuilder != null) {
			regionBuilder.reset();
		}

		potentiallyVisibleSetManager.clear();
		renderRegionStorage.clear();
		cameraVisibleRegions.clear();
		terrainFrustum.reload();

		//ClassInspector.inspect();
	}

	@Override
	public boolean isTerrainRenderComplete() {
		return regionRebuildManager.isEmpty() && regionBuilder.isEmpty() && occlusionInputStatus.isCurrent();
	}

	@Override
	public int getCompletedChunkCount() {
		return cameraVisibleRegions.getActiveCount();
	}

	@Override
	@SuppressWarnings("resource")
	public String getChunksDebugString() {
		final int len = renderRegionStorage.loadedRegionCount();
		final int count = getCompletedChunkCount();
		String shadowRegionString = "";

		if (Pipeline.shadowsEnabled()) {
			int shadowCount = shadowVisibleRegions[0].getActiveCount()
					+ shadowVisibleRegions[1].getActiveCount()
					+ shadowVisibleRegions[2].getActiveCount()
					+ shadowVisibleRegions[3].getActiveCount();

			shadowRegionString = String.format("S: %d,", shadowCount);
		}

		return String.format("C: %d/%d %sD: %d, %s", count, len, MinecraftClient.getInstance().chunkCullingEnabled ? "(s) " : "", chunkRenderDistance, shadowRegionString);
	}
}
