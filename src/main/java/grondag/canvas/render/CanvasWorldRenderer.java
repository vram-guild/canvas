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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL21;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.CloudRenderMode;
import net.minecraft.client.options.Option;
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
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.impl.client.rendering.WorldRenderContextImpl;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.Configurator.FogMode;
import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.apiimpl.rendercontext.BlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.EntityBlockRenderContext;
import grondag.canvas.buffer.BindStateManager;
import grondag.canvas.buffer.VboBuffer;
import grondag.canvas.buffer.encoding.CanvasImmediate;
import grondag.canvas.compat.FirstPersonModelHolder;
import grondag.canvas.light.LightmapHdTexture;
import grondag.canvas.material.property.MaterialFog;
import grondag.canvas.material.property.MaterialTarget;
import grondag.canvas.material.property.MatrixState;
import grondag.canvas.material.state.RenderContextState;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.mixinterface.BufferBuilderStorageExt;
import grondag.canvas.mixinterface.WorldRendererExt;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.PipelineManager;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.MaterialShaderManager;
import grondag.canvas.terrain.occlusion.PotentiallyVisibleRegionSorter;
import grondag.canvas.terrain.occlusion.TerrainIterator;
import grondag.canvas.terrain.occlusion.TerrainOccluder;
import grondag.canvas.terrain.occlusion.geometry.OcclusionRegion;
import grondag.canvas.terrain.occlusion.geometry.PackedBox;
import grondag.canvas.terrain.region.BuiltRenderRegion;
import grondag.canvas.terrain.region.RenderRegionBuilder;
import grondag.canvas.terrain.region.RenderRegionPruner;
import grondag.canvas.terrain.region.RenderRegionStorage;
import grondag.canvas.terrain.render.TerrainLayerRenderer;
import grondag.canvas.texture.DitherTexture;
import grondag.canvas.varia.CanvasGlHelper;
import grondag.canvas.varia.WorldDataManager;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;
import grondag.frex.api.event.WorldRenderEvent;

public class CanvasWorldRenderer extends WorldRenderer {
	public static final int MAX_REGION_COUNT = (32 * 2 + 1) * (32 * 2 + 1) * 16;
	private static CanvasWorldRenderer instance;
	// TODO: redirect uses in MC WorldRenderer
	public final Set<BuiltRenderRegion> regionsToRebuild = Sets.newLinkedHashSet();
	final TerrainLayerRenderer SOLID = new TerrainLayerRenderer("solid", null);
	private final PotentiallyVisibleRegionSorter distanceSorter = new PotentiallyVisibleRegionSorter();
	private final TerrainOccluder terrainOccluder = new TerrainOccluder();
	private final RenderRegionPruner pruner = new RenderRegionPruner(terrainOccluder, distanceSorter);
	private final RenderRegionStorage renderRegionStorage = new RenderRegionStorage(this, pruner);
	private final TerrainIterator terrainIterator = new TerrainIterator(renderRegionStorage, terrainOccluder, distanceSorter);
	public final TerrainFrustum terrainFrustum = new TerrainFrustum();

	/**
	 * Incremented whenever regions are built so visibility search can progress or to indicate visibility might be changed.
	 * Distinct from occluder state, which indiciates if/when occluder must be reset or redrawn.
	 */
	private final AtomicInteger regionDataVersion = new AtomicInteger();
	private final BuiltRenderRegion[] visibleRegions = new BuiltRenderRegion[MAX_REGION_COUNT];
	private final WorldRendererExt wr;
	private boolean terrainSetupOffThread = Configurator.terrainSetupOffThread;
	private int playerLightmap = 0;
	private RenderRegionBuilder regionBuilder;
	private int translucentSortPositionVersion;
	private ClientWorld world;
	// both of these are measured in chunks, not blocks
	private int squaredChunkRenderDistance;
	private int squaredChunkRetentionDistance;
	private Vec3d cameraPos;
	private int lastRegionDataVersion = -1;
	private int lastViewVersion = -1;
	private int visibleRegionCount = 0;
	final TerrainLayerRenderer TRANSLUCENT = new TerrainLayerRenderer("translucemt", this::sortTranslucentTerrain);

	private final RenderContextState contextState = new RenderContextState();
	public final CanvasImmediate worldRenderImmediate = new CanvasImmediate(new BufferBuilder(256), CanvasImmediate.entityBuilders(), contextState);
	private final CanvasParticleRenderer particleRenderer = new CanvasParticleRenderer();
	public final WorldRenderContextImpl eventContext = new WorldRenderContextImpl();

	public CanvasWorldRenderer(MinecraftClient client, BufferBuilderStorage bufferBuilders) {
		super(client, bufferBuilders);

		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: CanvasWorldRenderer init");
		}

		wr = (WorldRendererExt) this;
		instance = this;
		computeDistances();
	}

	// PERF: render larger cubes - avoid matrix state changes
	// PERF: cull particle rendering?
	// PERF: reduce garbage generation
	// PERF: lod culling: don't render grass, cobwebs, flowers, etc. at longer ranges
	// PERF: render leaves as solid at distance - omit interior faces
	// PERF: get VAO working again
	// PERF: consider trying backface culling again but at draw time w/ glMultiDrawArrays

	public static int playerLightmap() {
		return instance == null ? 0 : instance.playerLightmap;
	}

	private static int rangeColor(int range) {
		switch (range) {
			case PackedBox.RANGE_NEAR:
			default:
				return 0x80FF8080;

			case PackedBox.RANGE_MID:
				return 0x80FFFF80;

			case PackedBox.RANGE_FAR:
				return 0x8080FF80;

			case PackedBox.RANGE_EXTREME:
				return 0x808080FF;
		}
	}

	public static CanvasWorldRenderer instance() {
		return instance;
	}

	private void computeDistances() {
		int renderDistance = wr.canvas_renderDistance();
		squaredChunkRenderDistance = renderDistance * renderDistance;
		renderDistance += 2;
		squaredChunkRetentionDistance = renderDistance * renderDistance;
	}

	public void forceVisibilityUpdate() {
		regionDataVersion.incrementAndGet();
	}

	public RenderRegionBuilder regionBuilder() {
		return regionBuilder;
	}

	//	public RenderRegionStorage regionStorage() {
	//		return renderRegionStorage;
	//	}

	public ClientWorld getWorld() {
		return world;
	}

	@Override
	public void setWorld(@Nullable ClientWorld clientWorld) {
		// happens here to avoid creating before renderer is initialized
		if (regionBuilder == null) {
			regionBuilder = new RenderRegionBuilder();
		}

		DitherTexture.instance().initializeIfNeeded();
		world = clientWorld;
		visibleRegionCount = 0;
		terrainIterator.reset();
		renderRegionStorage.clear();
		Arrays.fill(visibleRegions, null);
		Arrays.fill(terrainIterator.visibleRegions, null);
		// we don't want to use our collector unless we are in a world
		((BufferBuilderStorageExt) wr.canvas_bufferBuilders()).canvas_setEntityConsumers(clientWorld == null ? null : worldRenderImmediate);
		// Mixins mostly disable what this does
		super.setWorld(clientWorld);
	}

	/**
	 * Terrain rebuild is partly lazy/incremental
	 * The occluder has a thread-safe version indicating visibility test validity.
	 * The raster must be redrawn whenever the frustum view changes but prior visibility
	 * checks remain valid until the player location changes more than 1 block
	 * (regions are fuzzed one block to allow this) or a region that was already drawn into
	 * the raster is updated with different visibility informaiton.  New occluders can
	 * also  be added to the existing raster.
	 * or
	 */
	public void setupTerrain(Camera camera, int frameCounter, boolean shouldCullChunks) {
		final WorldRendererExt wr = this.wr;
		final MinecraftClient mc = wr.canvas_mc();
		final int renderDistance = wr.canvas_renderDistance();
		final RenderRegionStorage regionStorage = renderRegionStorage;
		final TerrainIterator terrainIterator = this.terrainIterator;

		regionStorage.closeRegionsOnRenderThread();

		mc.getProfiler().push("camera");
		WorldDataManager.update(camera);
		MaterialConditionImpl.update();
		MaterialShaderManager.INSTANCE.onRenderTick();
		final BlockPos cameraBlockPos = camera.getBlockPos();
		final BuiltRenderRegion cameraRegion = cameraBlockPos.getY() < 0 || cameraBlockPos.getY() > 255 ? null : regionStorage.getOrCreateRegion(cameraBlockPos);

		mc.getProfiler().swap("buildnear");

		if (cameraRegion != null) {
			buildNearRegion(cameraRegion);

			for (int i = 0; i < 6; ++i) {
				final BuiltRenderRegion r = cameraRegion.getNeighbor(i);

				if (r != null) {
					buildNearRegion(r);
				}
			}
		}

		Entity.setRenderDistanceMultiplier(MathHelper.clamp(mc.options.viewDistance / 8.0D, 1.0D, 2.5D));

		mc.getProfiler().swap("update");

		if (terrainSetupOffThread) {
			int state = terrainIterator.state();

			if (state == TerrainIterator.COMPLETE) {
				final BuiltRenderRegion[] visibleRegions = this.visibleRegions;
				final int size = terrainIterator.visibleRegionCount;
				visibleRegionCount = size;
				System.arraycopy(terrainIterator.visibleRegions, 0, visibleRegions, 0, size);
				assert size == 0 || visibleRegions[0] != null;
				scheduleOrBuild(terrainIterator.updateRegions);
				terrainIterator.reset();
				state = TerrainIterator.IDLE;
			}

			final int newRegionDataVersion = regionDataVersion.get();

			if (state == TerrainIterator.IDLE && (terrainFrustum.viewVersion() != lastViewVersion || lastRegionDataVersion != newRegionDataVersion)) {
				lastRegionDataVersion = newRegionDataVersion;
				lastViewVersion = terrainFrustum.viewVersion();
				terrainIterator.prepare(cameraRegion, camera, terrainFrustum, renderDistance, shouldCullChunks);
				regionBuilder.executor.execute(terrainIterator, -1);
			}
		} else {
			final int newRegionDataVersion = regionDataVersion.get();

			if (terrainFrustum.viewVersion() != lastViewVersion || newRegionDataVersion != lastRegionDataVersion) {
				lastRegionDataVersion = newRegionDataVersion;
				terrainIterator.prepare(cameraRegion, camera, terrainFrustum, renderDistance, shouldCullChunks);
				terrainIterator.accept(null);

				final BuiltRenderRegion[] visibleRegions = this.visibleRegions;
				final int size = terrainIterator.visibleRegionCount;
				lastViewVersion = terrainFrustum.viewVersion();
				visibleRegionCount = size;
				System.arraycopy(terrainIterator.visibleRegions, 0, visibleRegions, 0, size);
				scheduleOrBuild(terrainIterator.updateRegions);
				terrainIterator.reset();
			}
		}

		mc.getProfiler().pop();
	}

	private void scheduleOrBuild(SimpleUnorderedArrayList<BuiltRenderRegion> updateRegions) {
		final int limit = updateRegions.size();
		final Set<BuiltRenderRegion> regionsToRebuild = this.regionsToRebuild;

		if (limit == 0) {
			return;
		}

		for (int i = 0; i < limit; ++i) {
			final BuiltRenderRegion region = updateRegions.get(i);

			if (region.needsRebuild()) {
				if (region.needsImportantRebuild() || region.isNear()) {
					regionsToRebuild.remove(region);
					region.rebuildOnMainThread();
					region.markBuilt();
				} else {
					regionsToRebuild.add(region);
				}
			}
		}
	}

	private void buildNearRegion(BuiltRenderRegion region) {
		if (region.needsRebuild()) {
			regionsToRebuild.remove(region);
			region.rebuildOnMainThread();
			region.markBuilt();
		}
	}

	private void updatePlayerLightmap(MinecraftClient mc, float f) {
		playerLightmap = mc.getEntityRenderDispatcher().getLight(mc.player, f);
	}

	@SuppressWarnings("resource")
	private boolean shouldCullChunks(BlockPos pos) {
		final MinecraftClient mc = wr.canvas_mc();
		boolean result = wr.canvas_mc().chunkCullingEnabled;

		if (mc.player.isSpectator() && !World.isOutOfBuildLimitVertically(pos.getY()) && world.getBlockState(pos).isOpaqueFullCube(world, pos)) {
			result = false;
		}

		return result;
	}

	public void renderWorld(MatrixStack viewMatrixStack, MatrixStack identityStack, float tickDelta, long frameStartNanos, boolean blockOutlines, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix) {
		Configurator.lagFinder.swap("WorldRenderer-Setup");

		final WorldRendererExt wr = this.wr;
		final MinecraftClient mc = wr.canvas_mc();
		final WorldRenderer mcwr = mc.worldRenderer;
		final Framebuffer mcfb = mc.getFramebuffer();
		final BlockRenderContext blockContext = BlockRenderContext.get();
		final EntityBlockRenderContext entityBlockContext = EntityBlockRenderContext.get();
		MaterialFog.allow(Configurator.fogMode != FogMode.NONE || mc.player.hasStatusEffect(StatusEffects.BLINDNESS));

		updatePlayerLightmap(mc, tickDelta);
		final ClientWorld world = this.world;
		final BufferBuilderStorage bufferBuilders = wr.canvas_bufferBuilders();
		final EntityRenderDispatcher entityRenderDispatcher = wr.canvas_entityRenderDispatcher();
		final boolean advancedTranslucency = Pipeline.isFabulous();

		BlockEntityRenderDispatcher.INSTANCE.configure(world, mc.getTextureManager(), mc.textRenderer, camera, mc.crosshairTarget);
		entityRenderDispatcher.configure(world, camera, mc.targetedEntity);
		final Profiler profiler = world.getProfiler();
		profiler.swap("light_updates");

		Configurator.lagFinder.swap("WorldRenderer-LightUpdates");
		mc.world.getChunkManager().getLightingProvider().doLightUpdates(Integer.MAX_VALUE, true, true);

		Configurator.lagFinder.swap("WorldRenderer-Culling");
		final Vec3d cameraVec3d = camera.getPos();
		cameraPos = cameraVec3d;
		final double cameraX = cameraVec3d.getX();
		final double cameraY = cameraVec3d.getY();
		final double cameraZ = cameraVec3d.getZ();

		profiler.swap("culling");

		final TerrainFrustum frustum = terrainFrustum;
		frustum.prepare(MatrixState.viewMatrix, tickDelta, camera);
		particleRenderer.frustum.prepare(MatrixState.viewMatrix, tickDelta, camera, projectionMatrix);

		mc.getProfiler().swap("regions");

		Configurator.lagFinder.swap("WorldRenderer-Background");

		Pipeline.defaultFbo.bind();

		profiler.swap("clear");

		// This does not actually render anything - what it does do is set the current clear color
		// Color is captured via a mixin for use in shaders
		BackgroundRenderer.render(camera, tickDelta, mc.world, mc.options.viewDistance, gameRenderer.getSkyDarkness(tickDelta));

		if (Pipeline.config().runVanillaClear) {
			RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
		}

		final float viewDistance = gameRenderer.getViewDistance();
		final boolean thickFog = mc.world.getSkyProperties().useThickFog(MathHelper.floor(cameraX), MathHelper.floor(cameraY)) || mc.inGameHud.getBossBarHud().shouldThickenFog();

		if (mc.options.viewDistance >= 4) {
			// NB: fog / sky renderer really wants it this way
			RenderSystem.popMatrix();
			BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_SKY, viewDistance, thickFog);
			profiler.swap("sky");
			((WorldRenderer) wr).renderSky(viewMatrixStack, tickDelta);
			RenderSystem.pushMatrix();
			RenderSystem.multMatrix(MatrixState.viewMatrix);
		}

		profiler.swap("fog");
		BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(viewDistance - 16.0F, 32.0F), thickFog);
		profiler.swap("terrain_setup");

		Configurator.lagFinder.swap("WorldRenderer-TerrainSetup");

		setupTerrain(camera, wr.canvas_getAndIncrementFrameIndex(), shouldCullChunks(camera.getBlockPos()));
		eventContext.setFrustum(frustum);
		WorldRenderEvents.AFTER_SETUP.invoker().afterSetup(eventContext);

		Configurator.lagFinder.swap("WorldRenderer-TerrainUpdate");

		profiler.swap("updatechunks");
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

		updateRegions(frameStartNanos + clampedBudget);

		//LightmapHdTexture.instance().onRenderTick();

		profiler.swap("terrain");
		Configurator.lagFinder.swap("WorldRenderer-TerrainRenderSolid");

		MatrixState.set(MatrixState.REGION);
		renderTerrainLayer(false, cameraX, cameraY, cameraZ);
		MatrixState.set(MatrixState.CAMERA);

		// Note these don't have an effect when canvas pipeline is active - lighting happens in the shader
		// but they are left intact to handle any fix-function renders we don't catch
		if (this.world.getSkyProperties().isDarkened()) {
			// True for nether - yarn names here are not great
			// Causes lower face to be lit like top face
			DiffuseLighting.enableForLevel(MatrixState.viewMatrix);
		} else {
			DiffuseLighting.method_27869(MatrixState.viewMatrix);
		}

		Configurator.lagFinder.swap("WorldRenderer-EntityRender");

		profiler.swap("entities");

		WorldRenderEvents.BEFORE_ENTITIES.invoker().beforeEntities(eventContext);
		profiler.push("prepare");
		int entityCount = 0;
		final int blockEntityCount = 0;

		profiler.swap("entities");

		if (advancedTranslucency) {
			final Framebuffer entityFramebuffer = mcwr.getEntityFramebuffer();
			//entityFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
			entityFramebuffer.copyDepthFrom(mcfb);

			//final Framebuffer weatherFramebuffer = mcwr.getWeatherFramebuffer();
			//weatherFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
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
		final BuiltRenderRegion[] visibleRegions = this.visibleRegions;
		entityBlockContext.tickDelta(tickDelta);
		entityBlockContext.collectors = immediate.collectors;
		blockContext.collectors = immediate.collectors;

		// PERF: find way to reduce allocation for this and MatrixStack generally

		while (entities.hasNext()) {
			final Entity entity = entities.next();
			if ((!entityRenderDispatcher.shouldRender(entity, frustum, cameraX, cameraY, cameraZ) && !entity.hasPassengerDeep(mc.player))
					|| (entity == camera.getFocusedEntity() && !FirstPersonModelHolder.handler.isThirdPerson(this, camera, viewMatrixStack) && (!(camera.getFocusedEntity() instanceof LivingEntity) || !((LivingEntity) camera.getFocusedEntity()).isSleeping()))
					|| (entity instanceof ClientPlayerEntity && camera.getFocusedEntity() != entity)) {
				continue;
			}

			++entityCount;
			contextState.setCurrentEntity(entity);

			if (entity.age == 0) {
				entity.lastRenderX = entity.getX();
				entity.lastRenderY = entity.getY();
				entity.lastRenderZ = entity.getZ();
			}

			VertexConsumerProvider renderProvider;

			if (canDrawEntityOutlines && mc.hasOutline(entity)) {
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
			wr.canvas_renderEntity(entity, cameraX, cameraY, cameraZ, tickDelta, identityStack, renderProvider);
		}

		contextState.setCurrentEntity(null);

		profiler.swap("blockentities");

		final int visibleRegionCount = this.visibleRegionCount;
		final Set<BlockEntity> noCullingBlockEntities = wr.canvas_noCullingBlockEntities();

		for (int regionIndex = 0; regionIndex < visibleRegionCount; ++regionIndex) {
			assert visibleRegions[regionIndex] != null;

			final List<BlockEntity> list = visibleRegions[regionIndex].getBuildData().getBlockEntities();

			final Iterator<BlockEntity> itBER = list.iterator();

			while (itBER.hasNext()) {
				final BlockEntity blockEntity = itBER.next();
				final BlockPos blockPos = blockEntity.getPos();
				VertexConsumerProvider outputConsumer = immediate;
				contextState.setCurrentBlockEntity(blockEntity);

				identityStack.push();
				identityStack.translate(blockPos.getX() - cameraX, blockPos.getY() - cameraY, blockPos.getZ() - cameraZ);
				final SortedSet<BlockBreakingInfo> sortedSet = wr.canvas_blockBreakingProgressions().get(blockPos.asLong());

				if (sortedSet != null && !sortedSet.isEmpty()) {
					final int stage = sortedSet.last().getStage();

					if (stage >= 0) {
						final MatrixStack.Entry xform = identityStack.peek();
						final VertexConsumer overlayConsumer = new OverlayVertexConsumer(bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage)), xform.getModel(), xform.getNormal());

						outputConsumer = (renderLayer) -> {
							final VertexConsumer baseConsumer = immediate.getBuffer(renderLayer);
							return renderLayer.hasCrumbling() ? VertexConsumers.dual(overlayConsumer, baseConsumer) : baseConsumer;
						};
					}
				}

				renderBlockEntitySafely(blockEntity, tickDelta, identityStack, outputConsumer);
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
				identityStack.translate(blockPos2.getX() - cameraX, blockPos2.getY() - cameraY, blockPos2.getZ() - cameraZ);
				renderBlockEntitySafely(blockEntity2, tickDelta, identityStack, immediate);
				identityStack.pop();
			}
		}

		contextState.setCurrentBlockEntity(null);

		immediate.drawCollectors(MaterialTarget.MAIN);

		WorldRenderEvents.AFTER_ENTITIES.invoker().afterEntities(eventContext);

		bufferBuilders.getOutlineVertexConsumers().draw();

		if (didRenderOutlines) {
			entityOutlineShader.render(tickDelta);
			Pipeline.defaultFbo.bind();
		}

		Configurator.lagFinder.swap("WorldRenderer-Breaking");
		profiler.swap("destroyProgress");

		// honor damage render layer irrespective of model material
		blockContext.collectors = null;

		final ObjectIterator<Entry<SortedSet<BlockBreakingInfo>>> breakings = wr.canvas_blockBreakingProgressions().long2ObjectEntrySet().iterator();

		while (breakings.hasNext()) {
			final Entry<SortedSet<BlockBreakingInfo>> entry = breakings.next();
			final BlockPos breakPos = BlockPos.fromLong(entry.getLongKey());
			final double y = breakPos.getX() - cameraX;
			final double z = breakPos.getY() - cameraY;
			final double aa = breakPos.getZ() - cameraZ;

			if (y * y + z * z + aa * aa <= 1024.0D) {
				final SortedSet<BlockBreakingInfo> breakSet = entry.getValue();

				if (breakSet != null && !breakSet.isEmpty()) {
					final int stage = breakSet.last().getStage();
					identityStack.push();
					identityStack.translate(breakPos.getX() - cameraX, breakPos.getY() - cameraY, breakPos.getZ() - cameraZ);
					final MatrixStack.Entry xform = identityStack.peek();
					final VertexConsumer vertexConsumer2 = new OverlayVertexConsumer(bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage)), xform.getModel(), xform.getNormal());
					mc.getBlockRenderManager().renderDamage(world.getBlockState(breakPos), breakPos, world, identityStack, vertexConsumer2);
					identityStack.pop();
				}
			}
		}

		blockContext.collectors = immediate.collectors;

		profiler.pop();
		profiler.swap("outline");
		Configurator.lagFinder.swap("WorldRenderer-OutlineAndDebug");
		final HitResult hitResult = mc.crosshairTarget;

		if (WorldRenderEvents.BEFORE_BLOCK_OUTLINE.invoker().beforeBlockOutline(eventContext, hitResult)) {
			if (blockOutlines && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
				final BlockPos blockOutlinePos = ((BlockHitResult) hitResult).getBlockPos();
				final BlockState blockOutlineState = world.getBlockState(blockOutlinePos);

				if (!blockOutlineState.isAir() && world.getWorldBorder().contains(blockOutlinePos)) {
					// THIS IS WHEN LIGHTENING RENDERS IN VANILLA
					final VertexConsumer blockOutlineConumer = immediate.getBuffer(RenderLayer.getLines());

					eventContext.prepareBlockOutline(blockOutlineConumer, camera.getFocusedEntity(), cameraX, cameraY, cameraZ, blockOutlinePos, blockOutlineState);

					if (WorldRenderEvents.BLOCK_OUTLINE.invoker().onBlockOutline(eventContext, eventContext)) {
						wr.canvas_drawBlockOutline(identityStack, blockOutlineConumer, camera.getFocusedEntity(), cameraX, cameraY, cameraZ, blockOutlinePos, blockOutlineState);
					}
				}
			}
		}

		// NB: view matrix is already applied to GL state before renderWorkd is called
		WorldRenderEvents.BEFORE_DEBUG_RENDER.invoker().beforeDebugRender(eventContext);
		// We still pass in the transformed stack because that is what debug renderer normally gets
		mc.debugRenderer.render(viewMatrixStack, immediate, cameraX, cameraY, cameraZ);

		Configurator.lagFinder.swap("WorldRenderer-SolidDraws");

		// Should generally not have anything here but draw in case content injected in hooks
		immediate.drawCollectors(MaterialTarget.MAIN);

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

		Configurator.lagFinder.swap("WorldRenderer-Transluent");

		if (advancedTranslucency) {
			profiler.swap("translucent");

			Framebuffer fb = mcwr.getTranslucentFramebuffer();
			fb.copyDepthFrom(mcfb);

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
			renderTerrainLayer(true, cameraX, cameraY, cameraZ);
			MatrixState.set(MatrixState.CAMERA);

			// NB: vanilla renders tripwire here but we combine into translucent

			fb = mcwr.getParticlesFramebuffer();
			fb.copyDepthFrom(mcfb);
			Pipeline.translucentParticlesFbo.bind();

			profiler.swap("particles");
			particleRenderer.renderParticles(mc.particleManager, identityStack, immediate, lightmapTextureManager, camera, tickDelta);

			Pipeline.defaultFbo.bind();
		} else {
			profiler.swap("translucent");
			MatrixState.set(MatrixState.REGION);
			renderTerrainLayer(true, cameraX, cameraY, cameraZ);
			MatrixState.set(MatrixState.CAMERA);

			// without fabulous transparency important that lines
			// and other translucent elements get drawn on top of terrain
			immediate.draw(RenderLayer.getLines());

			// PERF: how is this needed? - would either have been drawn above or will be drawn below
			immediate.drawCollectors(MaterialTarget.TRANSLUCENT);

			// This catches entity layer and any remaining non-main layers
			immediate.draw();

			profiler.swap("particles");
			particleRenderer.renderParticles(mc.particleManager, identityStack, immediate, lightmapTextureManager, camera, tickDelta);
		}

		RenderState.disable();

		WorldRenderEvents.AFTER_TRANSLUCENT.invoker().afterTranslucent(eventContext);

		// TODO: need a new event here for weather/cloud targets that has matrix applies to render state
		// TODO: move the Mallib world last to the new event when fabulous is on

		if (Configurator.debugOcclusionBoxes) {
			renderCullBoxes(identityStack, immediate, cameraX, cameraY, cameraZ, tickDelta);
		}

		Configurator.lagFinder.swap("WorldRenderer-PostTransluent");

		profiler.swap("clouds");

		RenderState.disable();
		GlProgram.deactivate();

		// WIP: need to properly target the designated buffer here in both clouds and weather
		// also may not work with non-fabulous pipelines

		// NB: important to clear the cloud FB even when clouds are off - prevents leftover clouds
		if (mc.options.getCloudRenderMode() != CloudRenderMode.OFF) {
			getCloudsFramebuffer().clear(MinecraftClient.IS_SYSTEM_MAC);
			RenderPhase.CLOUDS_TARGET.startDrawing();
			// NB: vanilla cloud renderer wants/needs the transformed stack even though it is already applied
			renderClouds(viewMatrixStack, tickDelta, cameraX, cameraY, cameraZ);
			RenderPhase.CLOUDS_TARGET.endDrawing();
		}

		profiler.swap("weather");

		if (advancedTranslucency) {
			RenderPhase.WEATHER_TARGET.startDrawing();
			wr.canvas_renderWeather(lightmapTextureManager, tickDelta, cameraX, cameraY, cameraZ);
			wr.canvas_renderWorldBorder(camera);
			RenderPhase.WEATHER_TARGET.endDrawing();
			PipelineManager.beFabulous();

			Pipeline.defaultFbo.bind();
		} else {
			RenderSystem.depthMask(false);
			wr.canvas_renderWeather(lightmapTextureManager, tickDelta, cameraX, cameraY, cameraZ);
			wr.canvas_renderWorldBorder(camera);
			RenderSystem.depthMask(true);
		}

		// doesn't make any sense with our chunk culling scheme
		// this.renderChunkDebugInfo(camera);
		WorldRenderEvents.LAST.invoker().onLast(eventContext);

		RenderSystem.shadeModel(7424);
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
		BackgroundRenderer.method_23792();
		entityBlockContext.collectors = null;
		blockContext.collectors = null;

		wr.canvas_setEntityCounts(entityCount, blockEntityCount);

		// prevents fog in GUI
		MaterialFog.allow(false);

		//RenderState.enablePrint = true;

		Configurator.lagFinder.complete();
	}

	private static final ReferenceOpenHashSet<BlockEntityType<?>> CAUGHT_BER_ERRORS = new ReferenceOpenHashSet<>();

	private static void renderBlockEntitySafely(BlockEntity blockEntity, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider outputConsumer) {
		try {
			BlockEntityRenderDispatcher.INSTANCE.render(blockEntity, tickDelta, matrixStack, outputConsumer);
		} catch (final Exception e) {
			if (CAUGHT_BER_ERRORS.add(blockEntity.getType())) {
				CanvasMod.LOG.warn(String.format("Unhandled exception rendering while rendering BlockEntity %s @ %s.  Stack trace follows. Subsequent errors will be suppressed.",
						Registry.BLOCK_ENTITY_TYPE.getId(blockEntity.getType()).toString(), blockEntity.getPos().toShortString()));

				// Passing this to .(warn) causes "Negative index in crash report handler" spam, so printing separately
				e.printStackTrace();
			}
		}
	}

	private void renderCullBoxes(MatrixStack matrixStack, Immediate immediate, double cameraX, double cameraY, double cameraZ, float tickDelta) {
		@SuppressWarnings("resource") final Entity entity = MinecraftClient.getInstance().gameRenderer.getCamera().getFocusedEntity();

		final HitResult hit = entity.raycast(12 * 16, tickDelta, true);

		if (hit.getType() != HitResult.Type.BLOCK) {
			return;
		}

		final BlockPos pos = ((BlockHitResult) (hit)).getBlockPos();
		final BuiltRenderRegion region = renderRegionStorage.getRegionIfExists(pos);

		if (region == null) {
			return;
		}

		final int[] boxes = region.getBuildData().getOcclusionData();

		if (boxes == null || boxes.length < OcclusionRegion.CULL_DATA_FIRST_BOX) {
			return;
		}

		RenderSystem.pushMatrix();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableTexture();

		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder bufferBuilder = tessellator.getBuffer();

		final int cb = boxes[0];
		final int limit = boxes.length;

		final double x = (pos.getX() & ~0xF) - cameraX;
		final double y = (pos.getY() & ~0xF) - cameraY;
		final double z = (pos.getZ() & ~0xF) - cameraZ;

		RenderSystem.lineWidth(6.0F);
		bufferBuilder.begin(GL21.GL_LINES, VertexFormats.POSITION_COLOR);
		final int regionRange = region.occlusionRange;

		drawOutline(bufferBuilder, x + PackedBox.x0(cb), y + PackedBox.y0(cb), z + PackedBox.z0(cb), x + PackedBox.x1(cb), y + PackedBox.y1(cb), z + PackedBox.z1(cb), 0xFFAAAAAA);

		for (int i = OcclusionRegion.CULL_DATA_FIRST_BOX; i < limit; ++i) {
			final int b = boxes[i];
			final int range = PackedBox.range(b);

			if (regionRange > range) {
				break;
			}

			drawOutline(bufferBuilder, x + PackedBox.x0(b), y + PackedBox.y0(b), z + PackedBox.z0(b), x + PackedBox.x1(b), y + PackedBox.y1(b), z + PackedBox.z1(b), rangeColor(range));
		}

		tessellator.draw();
		RenderSystem.disableDepthTest();
		RenderSystem.lineWidth(3.0F);
		bufferBuilder.begin(GL21.GL_LINES, VertexFormats.POSITION_COLOR);

		drawOutline(bufferBuilder, x + PackedBox.x0(cb), y + PackedBox.y0(cb), z + PackedBox.z0(cb), x + PackedBox.x1(cb), y + PackedBox.y1(cb), z + PackedBox.z1(cb), 0xFFAAAAAA);

		for (int i = OcclusionRegion.CULL_DATA_FIRST_BOX; i < limit; ++i) {
			final int b = boxes[i];
			final int range = PackedBox.range(b);

			if (regionRange > range) {
				break;
			}

			drawOutline(bufferBuilder, x + PackedBox.x0(b), y + PackedBox.y0(b), z + PackedBox.z0(b), x + PackedBox.x1(b), y + PackedBox.y1(b), z + PackedBox.z1(b), rangeColor(range));
		}

		tessellator.draw();

		RenderSystem.enableDepthTest();
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
		RenderSystem.popMatrix();
	}

	//	private static final Direction[] DIRECTIONS = Direction.values();

	private void drawOutline(BufferBuilder bufferBuilder, double x0, double y0, double z0, double x1, double y1, double z1, int color) {
		final int a = (color >>> 24) & 0xFF;
		final int r = (color >> 16) & 0xFF;
		final int g = (color >> 8) & 0xFF;
		final int b = color & 0xFF;

		bufferBuilder.vertex(x0, y0, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y1, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y0, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y1, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y0, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y1, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y0, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y1, z1).color(r, g, b, a).next();

		bufferBuilder.vertex(x0, y0, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y0, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y1, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y1, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y0, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y0, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y1, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y1, z1).color(r, g, b, a).next();

		bufferBuilder.vertex(x0, y0, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y0, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y0, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y0, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y1, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x0, y1, z1).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y1, z0).color(r, g, b, a).next();
		bufferBuilder.vertex(x1, y1, z1).color(r, g, b, a).next();
	}

	private void sortTranslucentTerrain() {
		final MinecraftClient mc = MinecraftClient.getInstance();

		mc.getProfiler().push("translucent_sort");

		if (translucentSortPositionVersion != terrainFrustum.positionVersion()) {
			translucentSortPositionVersion = terrainFrustum.positionVersion();

			int j = 0;

			for (int regionIndex = 0; regionIndex < visibleRegionCount; regionIndex++) {
				if (j < 15 && visibleRegions[regionIndex].scheduleSort()) {
					++j;
				}
			}
		}

		mc.getProfiler().pop();
	}

	private void renderTerrainLayer(boolean isTranslucent, double x, double y, double z) {
		final BuiltRenderRegion[] visibleRegions = this.visibleRegions;
		final int visibleRegionCount = this.visibleRegionCount;

		if (visibleRegionCount == 0) {
			return;
		}

		if (isTranslucent) {
			TRANSLUCENT.render(visibleRegions, visibleRegionCount, x, y, z);
		} else {
			SOLID.render(visibleRegions, visibleRegionCount, x, y, z);
		}

		RenderState.disable();

		// Important this happens BEFORE anything that could affect vertex state
		if (CanvasGlHelper.isVaoEnabled()) {
			CanvasGlHelper.glBindVertexArray(0);
		}

		if (Configurator.hdLightmaps()) {
			LightmapHdTexture.instance().disable();
			DitherTexture.instance().disable();
		}

		VboBuffer.unbind();
		RenderSystem.clearCurrentColor();
		BindStateManager.unbind();
	}

	private void updateRegions(long endNanos) {
		regionBuilder.upload();

		final Set<BuiltRenderRegion> regionsToRebuild = this.regionsToRebuild;

		//final long start = Util.getMeasuringTimeNano();
		//int builtCount = 0;

		if (!regionsToRebuild.isEmpty()) {
			final Iterator<BuiltRenderRegion> iterator = regionsToRebuild.iterator();

			while (iterator.hasNext()) {
				final BuiltRenderRegion builtRegion = iterator.next();

				if (builtRegion.needsImportantRebuild()) {
					builtRegion.rebuildOnMainThread();
				} else {
					builtRegion.scheduleRebuild();
				}

				builtRegion.markBuilt();
				iterator.remove();

				// this seemed excessive
				//				++builtCount;
				//
				//				final long now = Util.getMeasuringTimeNano();
				//				final long elapsed = now - start;
				//				final long avg = elapsed / builtCount;
				//				final long remaining = endNanos - now;
				//
				//				if (remaining < avg) {
				//					break;
				//				}

				if (Util.getMeasuringTimeNano() >= endNanos) {
					break;
				}
			}
		}
	}

	public CanvasFrustum frustum() {
		return terrainFrustum;
	}

	public Vec3d cameraPos() {
		return cameraPos;
	}

	public int maxSquaredChunkRenderDistance() {
		return squaredChunkRenderDistance;
	}

	public int maxSquaredChunkRetentionDistance() {
		return squaredChunkRetentionDistance;
	}

	public void updateNoCullingBlockEntities(ObjectOpenHashSet<BlockEntity> removedBlockEntities, ObjectOpenHashSet<BlockEntity> addedBlockEntities) {
		((WorldRenderer) wr).updateNoCullingBlockEntities(removedBlockEntities, addedBlockEntities);
	}

	// PERF: stash frustum version and terrain version in entity - only retest when changed
	public <T extends Entity> boolean isEntityVisible(T entity) {
		final Box box = entity.getVisibilityBoundingBox();

		final double x0, y0, z0, x1, y1, z1;

		// NB: this method is mis-named
		if (box.isValid()) {
			x0 = entity.getX() - 1.5;
			y0 = entity.getY() - 1.5;
			z0 = entity.getZ() - 1.5;
			x1 = x0 + 3.0;
			y1 = y0 + 3.0;
			z1 = z0 + 3.0;
		} else {
			x0 = box.minX;
			y0 = box.minY;
			z0 = box.minZ;
			x1 = box.maxX;
			y1 = box.maxY;
			z1 = box.maxZ;
		}

		if (!terrainFrustum.isVisible(x0 - 0.5, y0 - 0.5, z0 - 0.5, x1 + 0.5, y1 + 0.5, z1 + 0.5)) {
			return false;
		}

		final int rx0 = MathHelper.floor(x0) & 0xFFFFFFF0;
		final int ry0 = MathHelper.floor(y0) & 0xFFFFFFF0;
		final int rz0 = MathHelper.floor(z0) & 0xFFFFFFF0;
		final int rx1 = MathHelper.floor(x1) & 0xFFFFFFF0;
		final int ry1 = MathHelper.floor(y1) & 0xFFFFFFF0;
		final int rz1 = MathHelper.floor(z1) & 0xFFFFFFF0;

		int flags = rx0 == rz1 ? 0 : 1;
		if (ry0 != ry1) flags |= 2;
		if (rz0 != rz1) flags |= 4;

		final RenderRegionStorage regions = renderRegionStorage;

		switch (flags) {
			case 0b000:
				return regions.wasSeen(rx0, ry0, rz0);

			case 0b001:
				return regions.wasSeen(rx0, ry0, rz0) || regions.wasSeen(rx1, ry0, rz0);

			case 0b010:
				return regions.wasSeen(rx0, ry0, rz0) || regions.wasSeen(rx0, ry1, rz0);

			case 0b011:
				return regions.wasSeen(rx0, ry0, rz0) || regions.wasSeen(rx1, ry0, rz0)
						|| regions.wasSeen(rx0, ry1, rz0) || regions.wasSeen(rx1, ry1, rz0);

			case 0b100:
				return regions.wasSeen(rx0, ry0, rz0) || regions.wasSeen(rx0, ry0, rz1);

			case 0b101:
				return regions.wasSeen(rx0, ry0, rz0) || regions.wasSeen(rx1, ry0, rz0)
						|| regions.wasSeen(rx0, ry0, rz1) || regions.wasSeen(rx1, ry0, rz1);

			case 0b110:
				return regions.wasSeen(rx0, ry0, rz0) || regions.wasSeen(rx0, ry1, rz0)
						|| regions.wasSeen(rx0, ry0, rz1) || regions.wasSeen(rx0, ry1, rz1);

			case 0b111:
				return regions.wasSeen(rx0, ry0, rz0) || regions.wasSeen(rx1, ry0, rz0)
						|| regions.wasSeen(rx0, ry1, rz0) || regions.wasSeen(rx1, ry1, rz0)
						|| regions.wasSeen(rx0, ry0, rz1) || regions.wasSeen(rx1, ry0, rz1)
						|| regions.wasSeen(rx0, ry1, rz1) || regions.wasSeen(rx1, ry1, rz1);
		}

		return true;
	}

	public void scheduleRegionRender(int x, int y, int z, boolean urgent) {
		renderRegionStorage.scheduleRebuild(x << 4, y << 4, z << 4, urgent);
		forceVisibilityUpdate();
	}

	@Override
	public void render(MatrixStack viewMatrixStack, float tickDelta, long frameStartNanos, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix) {
		final WorldRendererExt wr = this.wr;
		final MinecraftClient mc = wr.canvas_mc();
		final boolean wasFabulous = Pipeline.isFabulous();

		PipelineManager.reloadIfNeeded();

		if (wasFabulous != Pipeline.isFabulous()) {
			wr.canvas_setupFabulousBuffers();
		}

		if (mc.options.viewDistance != wr.canvas_renderDistance()) {
			reload();
		}

		wr.canvas_mc().getProfiler().swap("dynamic_lighting");

		// All managed draws - including anything targeting vertex consumer - will be transformed in shader
		// Unmanaged draws that do direct drawing will expect the stack to have the view matrix in it and may
		// use it either to transform the GL state or to transform vertices.
		// Transforming vertices would break managed draws, so we use an identity stack and set the
		// GL state to include the view transform by default.  The GL state will be ignored by managed draws
		// and managed draws will correctly have no rotation applies.  Direct draws may attempt to transform
		// the GL state to the view matrix but it will have no effect - it is already applied.

		MatrixState.set(MatrixState.CAMERA, viewMatrixStack.peek(), projectionMatrix);
		RenderSystem.pushMatrix();
		RenderSystem.multMatrix(MatrixState.viewMatrix);

		final MatrixStack identityStack = new MatrixStack();
		identityStack.peek().getModel().loadIdentity();
		identityStack.peek().getNormal().loadIdentity();

		eventContext.prepare(this, identityStack, tickDelta, frameStartNanos, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, projectionMatrix, worldRenderImmediate, wr.canvas_mc().getProfiler(), MinecraftClient.isFabulousGraphicsOrBetter(), world);

		WorldRenderEvents.START.invoker().onStart(eventContext);
		PipelineManager.beforeWorldRender();
		WorldRenderEvent.BEFORE_WORLD_RENDER.invoker().beforeWorldRender(identityStack, tickDelta, frameStartNanos, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, projectionMatrix);
		renderWorld(viewMatrixStack, identityStack, tickDelta, frameStartNanos, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, projectionMatrix);
		WorldRenderEvent.AFTER_WORLD_RENDER.invoker().afterWorldRender(identityStack, tickDelta, frameStartNanos, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, projectionMatrix);
		WorldRenderEvents.END.invoker().onEnd(eventContext);

		RenderSystem.popMatrix();
		MatrixState.set(MatrixState.SCREEN);
	}

	@Override
	public void reload() {
		PipelineManager.reloadIfNeeded();

		// cause injections to fire but disable all other vanilla logic
		// by setting world to null temporarily
		final ClientWorld swapWorld = wr.canvas_world();
		wr.canvas_setWorldNoSideEffects(null);
		super.reload();
		wr.canvas_setWorldNoSideEffects(swapWorld);

		// has the logic from super.reload() that requires private access
		wr.canvas_reload();

		computeDistances();
		terrainIterator.reset();
		terrainOccluder.invalidate();
		terrainSetupOffThread = Configurator.terrainSetupOffThread;
		regionsToRebuild.clear();

		if (regionBuilder != null) {
			regionBuilder.reset();
		}

		renderRegionStorage.clear();
		distanceSorter.clear();
		visibleRegionCount = 0;
		terrainFrustum.reload();

		//ClassInspector.inspect();
	}

	@Override
	public boolean isTerrainRenderComplete() {
		return regionsToRebuild.isEmpty() && regionBuilder.isEmpty() && regionDataVersion.get() == lastRegionDataVersion;
	}

	@Override
	public int getCompletedChunkCount() {
		int result = 0;
		final BuiltRenderRegion[] visibleRegions = this.visibleRegions;
		final int limit = visibleRegionCount;

		for (int i = 0; i < limit; i++) {
			final BuiltRenderRegion region = visibleRegions[i];

			if (!region.solidDrawable().isClosed() || !region.translucentDrawable().isClosed()) {
				++result;
			}
		}

		return result;
	}

	@Override
	@SuppressWarnings("resource")
	public String getChunksDebugString() {
		final int len = renderRegionStorage.regionCount();
		final int count = getCompletedChunkCount();
		final RenderRegionBuilder chunkBuilder = regionBuilder();
		return String.format("C: %d/%d %sD: %d, %s", count, len, wr.canvas_mc().chunkCullingEnabled ? "(s) " : "", wr.canvas_renderDistance(), chunkBuilder == null ? "null" : chunkBuilder.getDebugString());
	}
}
