/*
 * Copyright 2019, 2020 grondag
 *
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
import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.rendercontext.BlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.EntityBlockRenderContext;
import grondag.canvas.buffer.BindStateManager;
import grondag.canvas.buffer.VboBuffer;
import grondag.canvas.buffer.encoding.CanvasImmediate;
import grondag.canvas.compat.BborHolder;
import grondag.canvas.compat.CampanionHolder;
import grondag.canvas.compat.ClothHolder;
import grondag.canvas.compat.DynocapsHolder;
import grondag.canvas.compat.FirstPersonModelHolder;
import grondag.canvas.compat.GOMLHolder;
import grondag.canvas.compat.JustMapHolder;
import grondag.canvas.compat.LambDynLightsHolder;
import grondag.canvas.compat.LitematicaHolder;
import grondag.canvas.compat.MaliLibHolder;
import grondag.canvas.compat.SatinHolder;
import grondag.canvas.compat.VoxelMapHolder;
import grondag.canvas.light.LightmapHdTexture;
import grondag.canvas.material.property.MaterialMatrixState;
import grondag.canvas.material.state.RenderContextState;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.mixinterface.WorldRendererExt;
import grondag.canvas.shader.MaterialShaderManager;
import grondag.canvas.terrain.BuiltRenderRegion;
import grondag.canvas.terrain.RenderRegionBuilder;
import grondag.canvas.terrain.RenderRegionStorage;
import grondag.canvas.terrain.occlusion.TerrainIterator;
import grondag.canvas.terrain.occlusion.TerrainOccluder;
import grondag.canvas.terrain.occlusion.region.OcclusionRegion;
import grondag.canvas.terrain.occlusion.region.PackedBox;
import grondag.canvas.terrain.render.TerrainLayerRenderer;
import grondag.canvas.texture.DitherTexture;
import grondag.canvas.varia.CanvasGlHelper;
import grondag.canvas.varia.WorldDataManager;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;
import grondag.frex.api.event.WorldRenderEvent;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL21;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
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
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;

public class CanvasWorldRenderer extends WorldRenderer {
	public static final int MAX_REGION_COUNT = (32 * 2 + 1) * (32 * 2 + 1) * 16;
	private static CanvasWorldRenderer instance;
	public final TerrainOccluder terrainOccluder = new TerrainOccluder();
	// TODO: redirect uses in MC WorldRenderer
	public final Set<BuiltRenderRegion> regionsToRebuild = Sets.newLinkedHashSet();
	final TerrainLayerRenderer SOLID = new TerrainLayerRenderer("solid", null);
	private final RenderRegionStorage renderRegionStorage = new RenderRegionStorage(this);
	private final TerrainIterator terrainIterator = new TerrainIterator(this);
	private final CanvasFrustum frustum = new CanvasFrustum();
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
	private int viewVersion;
	private int occluderVersion;
	private ClientWorld world;
	private int squaredRenderDistance;
	private int squaredRetentionDistance;
	private Vec3d cameraPos;
	private int lastRegionDataVersion = -1;
	private int visibleRegionCount = 0;
	final TerrainLayerRenderer TRANSLUCENT = new TerrainLayerRenderer("translucemt", this::sortTranslucentTerrain);

	private final RenderContextState contextState = new RenderContextState();
	private final CanvasImmediate worldRenderImmediate = new CanvasImmediate(new BufferBuilder(256), CanvasImmediate.entityBuilders(), contextState);
	private final CanvasParticleRenderer particleRenderer = new CanvasParticleRenderer();

	public CanvasWorldRenderer(MinecraftClient client, BufferBuilderStorage bufferBuilders) {
		super(client, bufferBuilders);

		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: CanvasWorldRenderer init");
		}

		wr = (WorldRendererExt) this;
		instance = this;
		computeDistances();
	}

	// FIX: missing edges with off-thread iteration - try frustum check on thread but leave potentially visible set off
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
			default:
			case PackedBox.RANGE_NEAR:
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
		squaredRenderDistance = renderDistance * renderDistance * 256;
		renderDistance += 2;
		squaredRetentionDistance = renderDistance * renderDistance * 256;
	}

	public void forceVisibilityUpdate() {
		regionDataVersion.incrementAndGet();
	}

	public RenderRegionBuilder regionBuilder() {
		return regionBuilder;
	}

	public RenderRegionStorage regionStorage() {
		return renderRegionStorage;
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

		DitherTexture.instance().initializeIfNeeded();
		world = clientWorld;
		visibleRegionCount = 0;
		renderRegionStorage.clear();
		Arrays.fill(visibleRegions, null);
		terrainIterator.reset();
		renderRegionStorage.clear();
		Arrays.fill(terrainIterator.visibleRegions, null);

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
		final int frustumPositionVersion = frustum.positionVersion();

		if (mc.options.viewDistance != renderDistance) {
			wr.canvas_reload();
		}

		mc.getProfiler().push("camera");
		final Vec3d cameraPos = this.cameraPos;

		mc.getProfiler().swap("distance");
		regionStorage.updateCameraDistance(cameraPos, frustumPositionVersion, renderDistance);
		WorldDataManager.update(camera);
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

			if (state == TerrainIterator.IDLE && (newRegionDataVersion != lastRegionDataVersion || viewVersion != frustum.viewVersion() || occluderVersion != terrainOccluder.version())) {
				viewVersion = frustum.viewVersion();
				occluderVersion = terrainOccluder.version();
				lastRegionDataVersion = newRegionDataVersion;
				terrainOccluder.prepareScene(camera, frustum, renderRegionStorage.regionVersion());
				terrainIterator.prepare(cameraRegion, cameraBlockPos, frustum, renderDistance, shouldCullChunks);
				regionBuilder.executor.execute(terrainIterator, -1);
			}
		} else {
			final int newRegionDataVersion = regionDataVersion.get();

			if (newRegionDataVersion != lastRegionDataVersion || viewVersion != frustum.viewVersion() || occluderVersion != terrainOccluder.version()) {
				viewVersion = frustum.viewVersion();
				occluderVersion = terrainOccluder.version();
				lastRegionDataVersion = newRegionDataVersion;

				terrainOccluder.prepareScene(camera, frustum, renderRegionStorage.regionVersion());
				terrainIterator.prepare(cameraRegion, cameraBlockPos, frustum, renderDistance, shouldCullChunks);
				terrainIterator.accept(null);

				final BuiltRenderRegion[] visibleRegions = this.visibleRegions;
				final int size = terrainIterator.visibleRegionCount;
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

		if (mc.player.isSpectator() && !World.isHeightInvalid(pos.getY()) && world.getBlockState(pos).isOpaqueFullCube(world, pos)) {
			result = false;
		}

		return result;
	}

	public void renderWorld(MatrixStack matrixStack, float tickDelta, long limitTime, boolean blockOutlines, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix) {
		final WorldRendererExt wr = this.wr;
		final MinecraftClient mc = wr.canvas_mc();
		final WorldRenderer mcwr = mc.worldRenderer;
		final Framebuffer mcfb = mc.getFramebuffer();
		final BlockRenderContext blockContext = BlockRenderContext.get();
		final EntityBlockRenderContext entityBlockContext = EntityBlockRenderContext.get();

		updatePlayerLightmap(mc, tickDelta);
		final ClientWorld world = this.world;
		final BufferBuilderStorage bufferBuilders = wr.canvas_bufferBuilders();
		final EntityRenderDispatcher entityRenderDispatcher = wr.canvas_entityRenderDispatcher();
		final boolean advancedTranslucency = wr.canvas_transparencyShader() != null;

		BlockEntityRenderDispatcher.INSTANCE.configure(world, mc.getTextureManager(), mc.textRenderer, camera, mc.crosshairTarget);
		entityRenderDispatcher.configure(world, camera, mc.targetedEntity);
		final Profiler profiler = world.getProfiler();
		profiler.swap("light_updates");
		mc.world.getChunkManager().getLightingProvider().doLightUpdates(Integer.MAX_VALUE, true, true);
		final Vec3d cameraVec3d = camera.getPos();
		cameraPos = cameraVec3d;

		final double cameraX = cameraVec3d.getX();
		final double cameraY = cameraVec3d.getY();
		final double cameraZ = cameraVec3d.getZ();
		final Matrix4f modelMatrix = matrixStack.peek().getModel();
		MaterialMatrixState.set(MaterialMatrixState.ENTITY, matrixStack.peek().getNormal());

		profiler.swap("culling");

		final CanvasFrustum frustum = this.frustum;
		frustum.prepare(modelMatrix, projectionMatrix, camera);

		mc.getProfiler().swap("regions");

		profiler.swap("clear");
		BackgroundRenderer.render(camera, tickDelta, mc.world, mc.options.viewDistance, gameRenderer.getSkyDarkness(tickDelta));
		RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
		final float viewDistance = gameRenderer.getViewDistance();
		final boolean thickFog = mc.world.getSkyProperties().useThickFog(MathHelper.floor(cameraX), MathHelper.floor(cameraY)) || mc.inGameHud.getBossBarHud().shouldThickenFog();

		if (mc.options.viewDistance >= 4) {
			BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_SKY, viewDistance, thickFog);
			profiler.swap("sky");
			((WorldRenderer) wr).renderSky(matrixStack, tickDelta);
		}

		profiler.swap("fog");
		BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(viewDistance - 16.0F, 32.0F), thickFog);
		profiler.swap("terrain_setup");

		setupTerrain(camera, wr.canvas_getAndIncrementFrameIndex(), shouldCullChunks(camera.getBlockPos()));
		LitematicaHolder.litematicaTerrainSetup.accept(frustum);

		profiler.swap("updatechunks");
		final int maxFps = mc.options.maxFps;
		long maxFpsLimit;

		if (maxFps == Option.FRAMERATE_LIMIT.getMax()) {
			maxFpsLimit = 0L;
		} else {
			maxFpsLimit = 1000000000 / maxFps;
		}

		final long budget = Util.getMeasuringTimeNano() - limitTime;

		// No idea wtg the 3/2 is for - looks like a hack
		final long updateBudget = wr.canvas_chunkUpdateSmoother().getTargetUsedTime(budget) * 3L / 2L;
		final long clampedBudget = MathHelper.clamp(updateBudget, maxFpsLimit, 33333333L);

		updateRegions(limitTime + clampedBudget);

		LightmapHdTexture.instance().onRenderTick();

		profiler.swap("terrain");

		if (Configurator.enableBloom) CanvasFrameBufferHacks.prepareForFrame();

		MaterialMatrixState.set(MaterialMatrixState.REGION, null);
		renderTerrainLayer(false, matrixStack, cameraX, cameraY, cameraZ);
		MaterialMatrixState.set(MaterialMatrixState.ENTITY, matrixStack.peek().getNormal());

		LitematicaHolder.litematicaRenderSolids.accept(matrixStack);

		// Note these don't have an effect when canvas pipeline is active - lighting happens in the shader
		// but they are left intact to handle any fix-function renders we don't catch
		if (this.world.getSkyProperties().isDarkened()) {
			// True for nether - yarn names here are not great
			// Causes lower face to be lit like top face
			DiffuseLighting.enableForLevel(matrixStack.peek().getModel());
		} else {
			DiffuseLighting.method_27869(matrixStack.peek().getModel());
		}

		profiler.swap("entities");
		SatinHolder.beforeEntitiesRenderEvent.beforeEntitiesRender(camera, frustum, tickDelta);
		profiler.push("prepare");
		int entityCount = 0;
		final int blockEntityCount = 0;

		profiler.swap("entities");

		if (advancedTranslucency) {
			final Framebuffer entityFramebuffer = mcwr.getEntityFramebuffer();
			entityFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
			entityFramebuffer.copyDepthFrom(mcfb);

			final Framebuffer weatherFramebuffer = mcwr.getWeatherFramebuffer();
			weatherFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
		}

		final boolean canDrawEntityOutlines = wr.canvas_canDrawEntityOutlines();

		if (canDrawEntityOutlines) {
			wr.canvas_entityOutlinesFramebuffer().clear(MinecraftClient.IS_SYSTEM_MAC);
		}

		mcfb.beginWrite(false);

		boolean didRenderOutlines = false;
		final CanvasImmediate immediate = worldRenderImmediate;
		final Iterator<Entity> entities = world.getEntities().iterator();
		final ShaderEffect entityOutlineShader = wr.canvas_entityOutlineShader();
		final BuiltRenderRegion[] visibleRegions = this.visibleRegions;
		entityBlockContext.tickDelta(tickDelta);
		entityBlockContext.collectors = immediate.collectors;
		blockContext.collectors = immediate.collectors;

		while (entities.hasNext()) {
			final Entity entity = entities.next();
			if ((!entityRenderDispatcher.shouldRender(entity, frustum, cameraX, cameraY, cameraZ) && !entity.hasPassengerDeep(mc.player))
			|| (entity == camera.getFocusedEntity() && !FirstPersonModelHolder.handler.isThirdPerson(this, camera, matrixStack) && (!(camera.getFocusedEntity() instanceof LivingEntity) || !((LivingEntity) camera.getFocusedEntity()).isSleeping()))
			|| (entity instanceof ClientPlayerEntity && camera.getFocusedEntity() != entity)) {
				continue;
			}

			++entityCount;
			contextState.setCurrentEntity(
				entity);

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

			// WIP2: use the RenderContextState instead
			entityBlockContext.entity(entity);

			// Item entity translucent typically gets drawn here in vanilla because there's no dedicated buffer for it
			wr.canvas_renderEntity(entity, cameraX, cameraY, cameraZ, tickDelta, matrixStack, renderProvider);
		}

		contextState.setCurrentEntity(null);

		// WIP2 move these after bulk draw in new pipeline
		SatinHolder.onEntitiesRenderedEvent.onEntitiesRendered(camera, frustum, tickDelta);
		LitematicaHolder.litematicaEntityHandler.handle(matrixStack, tickDelta);
		DynocapsHolder.handler.render(profiler, matrixStack, immediate, cameraVec3d);
		GOMLHolder.HANDLER.render(this, matrixStack, tickDelta, limitTime, blockOutlines, camera, gameRenderer, lightmapTextureManager, projectionMatrix);

		profiler.swap("blockentities");

		final int visibleRegionCount = this.visibleRegionCount;
		final Set<BlockEntity> noCullingBlockEntities = wr.canvas_noCullingBlockEntities();

		for (int regionIndex = 0; regionIndex < visibleRegionCount; ++regionIndex) {
			assert visibleRegions[regionIndex] != null;
			assert visibleRegions[regionIndex].getRenderData() != null;

			final List<BlockEntity> list = visibleRegions[regionIndex].getRenderData().getBlockEntities();

			final Iterator<BlockEntity> itBER = list.iterator();

			while (itBER.hasNext()) {
				final BlockEntity blockEntity = itBER.next();
				final BlockPos blockPos = blockEntity.getPos();
				VertexConsumerProvider outputConsumer = immediate;

				matrixStack.push();
				matrixStack.translate(blockPos.getX() - cameraX, blockPos.getY() - cameraY, blockPos.getZ() - cameraZ);
				final SortedSet<BlockBreakingInfo> sortedSet = wr.canvas_blockBreakingProgressions().get(blockPos.asLong());

				if (sortedSet != null && !sortedSet.isEmpty()) {
					final int stage = sortedSet.last().getStage();

					if (stage >= 0) {
						final MatrixStack.Entry xform = matrixStack.peek();
						final VertexConsumer overlayConsumer = new OverlayVertexConsumer(bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage)), xform.getModel(), xform.getNormal());

						outputConsumer = (renderLayer) -> {
							final VertexConsumer baseConsumer = immediate.getBuffer(renderLayer);
							return renderLayer.hasCrumbling() ? VertexConsumers.dual(overlayConsumer, baseConsumer) : baseConsumer;
						};
					}
				}

				BlockEntityRenderDispatcher.INSTANCE.render(blockEntity, tickDelta, matrixStack, outputConsumer);
				matrixStack.pop();
			}
		}

		synchronized (noCullingBlockEntities) {
			final Iterator<BlockEntity> globalBERs = noCullingBlockEntities.iterator();

			while (globalBERs.hasNext()) {
				final BlockEntity blockEntity2 = globalBERs.next();
				final BlockPos blockPos2 = blockEntity2.getPos();
				matrixStack.push();
				matrixStack.translate(blockPos2.getX() - cameraX, blockPos2.getY() - cameraY, blockPos2.getZ() - cameraZ);
				BlockEntityRenderDispatcher.INSTANCE.render(blockEntity2, tickDelta, matrixStack, immediate);
				matrixStack.pop();
			}
		}

		assert matrixStack.isEmpty() : "Matrix stack not empty in world render when expected";

		immediate.drawCollectors(false);

		bufferBuilders.getOutlineVertexConsumers().draw();

		if (didRenderOutlines) {
			entityOutlineShader.render(tickDelta);
			mcfb.beginWrite(false);
		}

		CampanionHolder.HANDLER.render(this, matrixStack, tickDelta, limitTime, blockOutlines, camera, gameRenderer, lightmapTextureManager, projectionMatrix);
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
					matrixStack.push();
					matrixStack.translate(breakPos.getX() - cameraX, breakPos.getY() - cameraY, breakPos.getZ() - cameraZ);
					final MatrixStack.Entry xform = matrixStack.peek();
					final VertexConsumer vertexConsumer2 = new OverlayVertexConsumer(bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage)), xform.getModel(), xform.getNormal());
					mc.getBlockRenderManager().renderDamage(world.getBlockState(breakPos), breakPos, world, matrixStack, vertexConsumer2);
					matrixStack.pop();
				}
			}
		}

		blockContext.collectors = immediate.collectors;

		assert matrixStack.isEmpty() : "Matrix stack not empty in world render when expected";

		profiler.pop();
		final HitResult hitResult = mc.crosshairTarget;

		if (blockOutlines && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
			profiler.swap("outline");
			final BlockPos blockPos4 = ((BlockHitResult) hitResult).getBlockPos();
			final BlockState blockState = world.getBlockState(blockPos4);

			if (!blockState.isAir() && world.getWorldBorder().contains(blockPos4)) {
				// THIS IS WHEN LIGHTENING RENDERS IN VANILLA
				final VertexConsumer vertexConsumer3 = immediate.getBuffer(RenderLayer.getLines());
				wr.canvas_drawBlockOutline(matrixStack, vertexConsumer3, camera.getFocusedEntity(), cameraX, cameraY, cameraZ, blockPos4, blockState);
			}
		}

		RenderSystem.pushMatrix();
		RenderSystem.multMatrix(matrixStack.peek().getModel());
		ClothHolder.clothDebugPreEvent.run();
		mc.debugRenderer.render(matrixStack, immediate, cameraX, cameraY, cameraZ);
		RenderSystem.popMatrix();

		// Intention here seems to be to draw all the non-translucent layers before
		// enabling the translucent target - this is a very brittle way of handling
		// should be able to draw all layers for a given target
		immediate.drawCollectors(false);

		immediate.draw(RenderLayer.getArmorGlint());
		immediate.draw(RenderLayer.getArmorEntityGlint());
		immediate.draw(RenderLayer.getGlint());
		immediate.draw(RenderLayer.getDirectGlint());
		immediate.draw(RenderLayer.method_30676());
		immediate.draw(RenderLayer.getEntityGlint());
		immediate.draw(RenderLayer.getDirectEntityGlint());
		immediate.draw(RenderLayer.getWaterMask());
		bufferBuilders.getEffectVertexConsumers().draw();

		if (advancedTranslucency) {
			profiler.swap("translucent");

			// in fabulous mode, the only thing that renders to terrain translucency
			// is terrain itself - so everything else can be rendered first

			// Lines draw to entity (item) target
			immediate.draw(RenderLayer.getLines());

			immediate.drawCollectors(true);

			// This presumably catches any remaining translucent layers in vanilla
			immediate.draw();

			Framebuffer fb = mcwr.getTranslucentFramebuffer();
			fb.clear(MinecraftClient.IS_SYSTEM_MAC);
			fb.copyDepthFrom(mcfb);
			fb.beginWrite(false);

			MaterialMatrixState.set(MaterialMatrixState.REGION, null);
			renderTerrainLayer(true, matrixStack, cameraX, cameraY, cameraZ);
			MaterialMatrixState.set(MaterialMatrixState.ENTITY, matrixStack.peek().getNormal());

			// NB: vanilla renders tripwire here but we combine into translucent

			VoxelMapHolder.postRenderLayerHandler.render(this, RenderLayer.getTranslucent(), matrixStack, cameraX, cameraY, cameraZ);
			fb = mcwr.getParticlesFramebuffer();
			fb.clear(MinecraftClient.IS_SYSTEM_MAC);
			fb.copyDepthFrom(mcfb);
			fb.beginWrite(false);

			profiler.swap("particles");
			MaterialMatrixState.set(MaterialMatrixState.PARTICLE, null);
			particleRenderer.renderParticles(mc.particleManager, matrixStack, immediate, lightmapTextureManager, camera, tickDelta);
			MaterialMatrixState.set(MaterialMatrixState.ENTITY, matrixStack.peek().getNormal());

			mcfb.beginWrite(false);
		} else {
			profiler.swap("translucent");
			MaterialMatrixState.set(MaterialMatrixState.REGION, null);
			renderTerrainLayer(true, matrixStack, cameraX, cameraY, cameraZ);
			MaterialMatrixState.set(MaterialMatrixState.ENTITY, matrixStack.peek().getNormal());

			// without fabulous transparency important that lines
			// and other translucent elements get drawn on top of terrain
			immediate.draw(RenderLayer.getLines());

			immediate.drawCollectors(true);

			// This presumably catches any remaining translucent layers in vanilla
			immediate.draw();

			VoxelMapHolder.postRenderLayerHandler.render(this, RenderLayer.getTranslucent(), matrixStack, cameraX, cameraY, cameraZ);
			profiler.swap("particles");
			MaterialMatrixState.set(MaterialMatrixState.PARTICLE, null);
			particleRenderer.renderParticles(mc.particleManager, matrixStack, immediate, lightmapTextureManager, camera, tickDelta);
			MaterialMatrixState.set(MaterialMatrixState.ENTITY, matrixStack.peek().getNormal());
		}

		JustMapHolder.justMapRender.renderWaypoints(matrixStack, camera, tickDelta);
		LitematicaHolder.litematicaRenderTranslucent.accept(matrixStack);
		LitematicaHolder.litematicaRenderOverlay.accept(matrixStack);

		RenderSystem.pushMatrix();
		RenderSystem.multMatrix(matrixStack.peek().getModel());

		if (Configurator.debugOcclusionBoxes) {
			renderCullBoxes(matrixStack, immediate, cameraX, cameraY, cameraZ, tickDelta);
		}

		profiler.swap("clouds");

		if (advancedTranslucency) {
			// clear the cloud FB even when clouds are off - prevents leftover clouds
			final Framebuffer fb = mcwr.getCloudsFramebuffer();
			fb.clear(MinecraftClient.IS_SYSTEM_MAC);

			if (mc.options.getCloudRenderMode() != CloudRenderMode.OFF) {
				fb.beginWrite(false);
				((WorldRenderer) wr).renderClouds(matrixStack, tickDelta, cameraX, cameraY, cameraZ);
				mcfb.beginWrite(false);
			}
		} else if (mc.options.getCloudRenderMode() != CloudRenderMode.OFF) {
			((WorldRenderer) wr).renderClouds(matrixStack, tickDelta, cameraX, cameraY, cameraZ);
		}

		profiler.swap("weather");

		if (advancedTranslucency) {
			final Framebuffer fb = mcwr.getWeatherFramebuffer();
			fb.beginWrite(false);
			wr.canvas_renderWeather(lightmapTextureManager, tickDelta, cameraX, cameraY, cameraZ);
			wr.canvas_renderWorldBorder(camera);

			// litematica overlay uses fabulous buffer so must run before translucent shader
			MaliLibHolder.litematicaRenderWorldLast.render(matrixStack, mc, tickDelta);

			wr.canvas_transparencyShader().render(tickDelta);
			mcfb.beginWrite(false);
		} else {
			RenderSystem.depthMask(false);
			wr.canvas_renderWeather(lightmapTextureManager, tickDelta, cameraX, cameraY, cameraZ);
			wr.canvas_renderWorldBorder(camera);
			RenderSystem.depthMask(true);

			MaliLibHolder.litematicaRenderWorldLast.render(matrixStack, mc, tickDelta);
		}

		if (Configurator.enableBloom) {
			CanvasFrameBufferHacks.applyBloom();
		}

		SatinHolder.onWorldRenderedEvent.onWorldRendered(matrixStack, camera, tickDelta, limitTime);

		if (Configurator.enableBufferDebug) {
			BufferDebug.render();
		}

		//this.renderChunkDebugInfo(camera);
		BborHolder.bborHandler.render(matrixStack, tickDelta, mc.player);

		RenderSystem.shadeModel(7424);
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
		RenderSystem.popMatrix();
		BackgroundRenderer.method_23792();
		entityBlockContext.collectors = null;
		blockContext.collectors = null;

		wr.canvas_setEntityCounts(entityCount, blockEntityCount);
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

		final int[] boxes = region.getRenderData().getOcclusionData();

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

		if (translucentSortPositionVersion != frustum.positionVersion()) {
			translucentSortPositionVersion = frustum.positionVersion();

			int j = 0;
			for (int regionIndex = 0; regionIndex < visibleRegionCount; regionIndex++) {
				if (j < 15 && visibleRegions[regionIndex].scheduleSort()) {
					++j;
				}
			}
		}

		mc.getProfiler().pop();
	}

	private void renderTerrainLayer(boolean isTranslucent, MatrixStack matrixStack, double x, double y, double z) {
		final BuiltRenderRegion[] visibleRegions = this.visibleRegions;
		final int visibleRegionCount = this.visibleRegionCount;

		if (visibleRegionCount == 0) {
			return;
		}

		if (isTranslucent) {
			TRANSLUCENT.render(visibleRegions, visibleRegionCount, matrixStack, x, y, z);
		} else {
			SOLID.render(visibleRegions, visibleRegionCount, matrixStack, x, y, z);
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
		return frustum;
	}

	public Vec3d cameraPos() {
		return cameraPos;
	}

	public int maxSquaredDistance() {
		return squaredRenderDistance;
	}

	public int maxRetentionDistance() {
		return squaredRetentionDistance;
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

		if (!frustum.isVisible(x0 - 0.5, y0 - 0.5, z0 - 0.5, x1 + 0.5, y1 + 0.5, z1 + 0.5)) {
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
		regionStorage().scheduleRebuild(x << 4, y << 4, z << 4, urgent);
		forceVisibilityUpdate();
	}

	@Override
	public void render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f) {
		wr.canvas_mc().getProfiler().swap("dynamic_lighting");
		LambDynLightsHolder.updateAll.accept(this);

		WorldRenderEvent.BEFORE_WORLD_RENDER.invoker().beforeWorldRender(matrices, tickDelta, limitTime, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, matrix4f);
		renderWorld(matrices, tickDelta, limitTime, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, matrix4f);
		VoxelMapHolder.postRenderHandler.render(this, matrices, tickDelta, limitTime, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, matrix4f);
		WorldRenderEvent.AFTER_WORLD_RENDER.invoker().afterWorldRender(matrices, tickDelta, limitTime, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, matrix4f);
	}

	@Override
	public void reload() {
		super.reload();

		computeDistances();
		terrainIterator.reset();
		terrainSetupOffThread = Configurator.terrainSetupOffThread;
		regionsToRebuild.clear();
		if (regionBuilder != null) {
			regionBuilder.reset();
		}
		renderRegionStorage.clear();
		terrainOccluder.invalidate();
		visibleRegionCount = 0;

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
		final int len = regionStorage().regionCount();
		final int count = getCompletedChunkCount();
		final RenderRegionBuilder chunkBuilder = regionBuilder();
		return String.format("C: %d/%d %sD: %d, %s", count, len, wr.canvas_mc().chunkCullingEnabled ? "(s) " : "", wr.canvas_renderDistance(), chunkBuilder == null ? "null" : chunkBuilder.getDebugString());
	}
}
