package grondag.canvas.render;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.LongHeapPriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.lwjgl.opengl.GL21;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
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
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.TransformingVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import grondag.canvas.buffer.allocation.VboBuffer;
import grondag.canvas.chunk.BuiltRenderRegion;
import grondag.canvas.chunk.DrawableChunk;
import grondag.canvas.chunk.RegionData;
import grondag.canvas.chunk.RenderRegionBuilder;
import grondag.canvas.chunk.RenderRegionStorage;
import grondag.canvas.chunk.draw.DrawableDelegate;
import grondag.canvas.chunk.occlusion.TerrainOccluder;
import grondag.canvas.chunk.occlusion.region.OcclusionRegion;
import grondag.canvas.chunk.occlusion.region.PackedBox;
import grondag.canvas.draw.DrawHandler;
import grondag.canvas.mixinterface.WorldRendererExt;
import grondag.canvas.perf.MicroTimer;

public class CanvasWorldRenderer {
	private static CanvasWorldRenderer instance;

	public static CanvasWorldRenderer instance() {
		return instance;
	}

	private int playerLightmap = 0;
	private RenderRegionBuilder chunkBuilder;
	private RenderRegionStorage renderRegionStorage;
	private final CanvasFrustum frustum = new CanvasFrustum();
	private final LongHeapPriorityQueue regionQueue = new LongHeapPriorityQueue();
	private int translucentSortPositionVersion;

	// TODO: redirect uses in MC WorldRenderer
	public Set<BuiltRenderRegion> chunksToRebuild = Sets.newLinkedHashSet();

	// TODO: remove
	private static final MicroTimer outerTimer = new MicroTimer("outer", 200);
	public static final MicroTimer innerTimer = new MicroTimer("inner", -1);
	private int lastSolidCount;
	private int lastTranlsucentCount;

	public void stopOuterTimer() {
		final long outerElapsed = outerTimer.elapsed();

		if (outerTimer.stop()) {
			System.out.println("Avg inner runs per frame = " + innerTimer.hits() / 100); // 100 because outer runs 2X per frame
			System.out.println("Inner elapsed is " + 100 * innerTimer.elapsed() / outerElapsed + "% of outer");
			System.out.println("Visible chunk count = " + completedChunkCount());
			System.out.println("lastSolidCount = " + lastSolidCount + "   lastTranlsucentCount = " + lastTranlsucentCount);
			innerTimer.reportAndClear();
			System.out.println();
		}
	}

	private BuiltRenderRegion[] visibleChunks = new BuiltRenderRegion[69696];
	private int visibleChunkCount = 0;

	private final WorldRendererExt wr;

	public CanvasWorldRenderer(WorldRendererExt wr) {
		this.wr = wr;
		instance = this;
	}

	public void clearChunkRenderers() {
		chunksToRebuild.clear();
		chunkBuilder.reset();
	}

	public void reload() {
		if (chunkBuilder == null) {
			chunkBuilder = new RenderRegionBuilder(wr.canvas_world(), (WorldRenderer) wr, wr.canvas_mc().is64Bit());
		} else {
			chunkBuilder.setWorld(wr.canvas_world());
		}

		if (renderRegionStorage != null) {
			renderRegionStorage.clear();
		}

		clearChunkRenderers();

		renderRegionStorage = new RenderRegionStorage(chunkBuilder, wr.canvas_mc().options.viewDistance);

		final Entity entity = wr.canvas_mc().getCameraEntity();

		if (entity != null) {
			renderRegionStorage.updateRegionOrigins(entity.getX(), entity.getZ());
		}
	}

	public RenderRegionBuilder chunkBuilder() {
		return chunkBuilder;
	}

	public RenderRegionStorage builtChunkStorage() {
		return renderRegionStorage;
	}

	public boolean isTerrainRenderComplete() {
		return chunksToRebuild.isEmpty() && chunkBuilder.isEmpty();
	}

	public void setWorld(@Nullable ClientWorld clientWorld) {
		final BuiltRenderRegion[] visibleChunks = this.visibleChunks;
		final int limit = visibleChunkCount;

		for (int i = 0; i < limit; i++) {
			visibleChunks[i] = null;
		}

		visibleChunkCount = 0;
	}

	private void resizeArraysIfNeeded(int size) {
		final int current = visibleChunks.length;

		if (current < size) {
			//			searchInfo = new int[size];
			//			searchDist = new int[size];
			visibleChunks = new BuiltRenderRegion[size];
			visibleChunkCount = 0;
		}
	}

	/**
	TODO: temporal controls on rebuilds

	Reasons terrain needs to be rebuilt
		rotation
			remain visible if in frustum
		translation
		occluder change (chunk load)

	state components and dependencies
		region sort order
			camera position

		potential visibility
			camera position
			occluders

		actual visibility
			potential visibility
			rotation (frustum)

	most impactful changes - most to least
		camera position
			only update 1x per block distance
			fuzz vis boxes by 1 in each direction
		occluders
			track pvs version
			only retest chunk visiblity when an already-drawn occluder changes
			check for occlusion state changes when chunks rebuild - don't invalidate  state otherwise
		frustum
			rebuild occluder only when new chunks come into frustum without current pvs version
			don't retest chunks that were already drawn with current pvs version

	specific strategies / changes
		track a pvs version
			chunks are always tested in distance order
			invalidated when...
				camera moves more than 1 block
				occluder already drawn into current pvs changes
			chunks determined to be in or out are marked with current pvs version
			chunks with current pvs version do not need to be retested against occluder - only against frustum
		occluder
			DONE: fuzz occlusion test boxes by 1 block all directions
			DONE: test occlusion volumes, not whole chunk volume
			track pvs version
			update occluder incrementally
				if pvs version AND frustum are same, only need to draw and test new chunks
				if pvs version is same but frustum is different, draw all occluders but only test new
		frustum
			DONE: ditch vanilla frustum and use optimized code in shouldRender
			unbork main render loop
			add check for region visibility to entity shouldRender
			cull particle rendering?
		region
			DONE: don't test vis of empty chunks and don't add them to visibles
			track pvs version
			track if visible in current pvs
			check for occlusion state changes  - invalidate PVS only when it changes
			backface culling
			lod culling
			fix small occluder box generation

	 */
	public void setupTerrain(Camera camera, CanvasFrustum frustum, int frameCounter, boolean isSpectator) {
		final WorldRendererExt wr = this.wr;
		final MinecraftClient mc = wr.canvas_mc();
		final int renderDistance = wr.canvas_renderDistance();
		final RenderRegionBuilder chunkBuilder = this.chunkBuilder;
		final RenderRegionStorage chunkStorage = renderRegionStorage;
		final BuiltRenderRegion[] regions = chunkStorage.regions();

		if (mc.options.viewDistance != renderDistance) {
			wr.canvas_reload();
		}

		mc.getProfiler().push("regions");
		resizeArraysIfNeeded(regions.length);
		chunkStorage.updateRegionOriginsIfNeeded(mc);

		mc.getProfiler().swap("camera");
		final Vec3d cameraPos = camera.getPos();
		chunkBuilder.setCameraPosition(cameraPos);
		mc.getProfiler().swap("distance");
		chunkStorage.updateCameraDistance(cameraPos, frustum.positionVersion());

		final BlockPos cameraBlockPos = camera.getBlockPos();
		final int cameraChunkIndex = chunkStorage.getRegionIndexSafely(cameraBlockPos);
		final BuiltRenderRegion cameraChunk = cameraChunkIndex == -1 ? null : regions[cameraChunkIndex];

		// TODO: integrate with sets used below, or come up with a better scheme
		final ObjectArrayList<BuiltRenderRegion> buildList = new ObjectArrayList<>();

		mc.getProfiler().swap("update");

		if (wr.canvas_checkNeedsTerrainUpdate(cameraPos, camera.getPitch(), camera.getYaw())) {

			//outerTimer.start();
			BuiltRenderRegion.advanceFrameIndex();

			final LongHeapPriorityQueue regionQueue = this.regionQueue;
			if (cameraChunk == null) {
				// TODO: prime visible when above or below world and camera chunk is null
			}  else {
				regionQueue.enqueue(cameraChunk.queueKey());
			}

			wr.canvas_setNeedsTerrainUpdate(false);
			int visibleChunkCount = 0;
			TerrainOccluder.clearScene();

			Entity.setRenderDistanceMultiplier(MathHelper.clamp(mc.options.viewDistance / 8.0D, 1.0D, 2.5D));
			final boolean chunkCullingEnabled = mc.chunkCullingEnabled;

			while(!regionQueue.isEmpty()) {
				final BuiltRenderRegion builtChunk = regions[(int) regionQueue.dequeueLong()];

				// don't visit if not in frustum
				if(!builtChunk.isInFrustum(frustum)) {
					continue;
				}

				// don't visit if chunk is outside near distance and doesn't have all 4 neighbors loaded
				if (!builtChunk.shouldBuild()) {
					continue;
				}

				final RegionData regionData = builtChunk.getBuildData();
				final int[] visData =  regionData.getOcclusionData();

				if (visData == null) {
					buildList.add(builtChunk);
					continue;
				}

				if (builtChunk.needsRebuild()) {
					buildList.add(builtChunk);
				}

				// for empty regions, check neighbors but don't add to visible set
				if (visData == OcclusionRegion.EMPTY_CULL_DATA) {
					builtChunk.enqueueUnvistedNeighbors(regionQueue);
					continue;
				}

				TerrainOccluder.prepareChunk(builtChunk.getOrigin(), builtChunk.occlusionRange);

				if (!chunkCullingEnabled || builtChunk == cameraChunk || builtChunk.isVeryNear() || TerrainOccluder.isBoxVisible(visData[OcclusionRegion.CULL_DATA_CHUNK_BOUNDS])) {
					builtChunk.enqueueUnvistedNeighbors(regionQueue);
					visibleChunks[visibleChunkCount++] = builtChunk;
					TerrainOccluder.occlude(visData);
				}
			}

			this.visibleChunkCount = visibleChunkCount;

			//stopOuterTimer();
		}

		TerrainOccluder.outputRaster();

		mc.getProfiler().swap("rebuildNear");
		final Set<BuiltRenderRegion> oldChunksToRebuild  = chunksToRebuild;
		final Set<BuiltRenderRegion> chunksToRebuild = Sets.newLinkedHashSet();
		this.chunksToRebuild = chunksToRebuild;

		final boolean needsTerrainUpdate = !buildList.isEmpty();

		//		for (int i = 0; i < visibleChunkCount; i++) {
		//			final BuiltRenderRegion builtChunk = visibleChunks[i];

		for (final BuiltRenderRegion builtChunk : buildList) {
			// FIX: why was this check here? Was it a concurrency thing?
			//			if (builtChunk.needsRebuild() || oldChunksToRebuild.contains(builtChunk)) {
			if (builtChunk.needsImportantRebuild() || builtChunk.isNear()) {
				//mc.getProfiler().push("build near");
				builtChunk.rebuildOnMainThread();
				builtChunk.markBuilt();
				//				mc.getProfiler().pop();
			} else {
				chunksToRebuild.add(builtChunk);
			}
			//			}
		}

		// TODO: still needed in old WR?
		if (needsTerrainUpdate) {
			wr.canvas_setNeedsTerrainUpdate(true);
		}

		chunksToRebuild.addAll(oldChunksToRebuild);
		mc.getProfiler().pop();
	}

	public static int playerLightmap() {
		return instance == null ? 0 : instance.playerLightmap;
	}

	private void updatePlayerLightmap(MinecraftClient mc, float f) {
		playerLightmap = mc.getEntityRenderManager().getLight(mc.player, f);
	}

	public void renderWorld(MatrixStack matrixStack, float f, long startTime, boolean bl, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix) {
		final WorldRendererExt wr = this.wr;
		final MinecraftClient mc = wr.canvas_mc();
		updatePlayerLightmap(mc, f);
		final ClientWorld world = wr.canvas_world();
		final BufferBuilderStorage bufferBuilders = wr.canvas_bufferBuilders();

		final EntityRenderDispatcher entityRenderDispatcher = wr.canvas_entityRenderDispatcher();

		BlockEntityRenderDispatcher.INSTANCE.configure(world, mc.getTextureManager(), mc.textRenderer, camera, mc.crosshairTarget);
		entityRenderDispatcher.configure(world, camera, mc.targetedEntity);
		final Profiler profiler = world.getProfiler();
		profiler.swap("light_updates");
		mc.world.getChunkManager().getLightingProvider().doLightUpdates(Integer.MAX_VALUE, true, true);
		final Vec3d vec3d = camera.getPos();
		final double cameraX = vec3d.getX();
		final double cameraY = vec3d.getY();
		final double cameraZ = vec3d.getZ();
		final Matrix4f modelMatrix = matrixStack.peek().getModel();

		TerrainOccluder.prepareScene(projectionMatrix, modelMatrix, camera);

		profiler.swap("culling");

		final CanvasFrustum frustum = this.frustum;
		frustum.prepare(modelMatrix, projectionMatrix, camera);

		profiler.swap("clear");
		BackgroundRenderer.render(camera, f, mc.world, mc.options.viewDistance, gameRenderer.getSkyDarkness(f));
		RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
		final float h = gameRenderer.getViewDistance();
		final boolean bl3 = mc.world.dimension.isFogThick(MathHelper.floor(cameraX), MathHelper.floor(cameraY)) || mc.inGameHud.getBossBarHud().shouldThickenFog();
		if (mc.options.viewDistance >= 4) {
			BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_SKY, h, bl3);
			profiler.swap("sky");
			((WorldRenderer) wr).renderSky(matrixStack, f);
		}

		profiler.swap("fog");
		BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(h - 16.0F, 32.0F), bl3);
		profiler.swap("terrain_setup");

		setupTerrain(camera, frustum, wr.canvas_getAndIncrementFrameIndex(), mc.player.isSpectator());

		profiler.swap("updatechunks");
		final int maxFps = mc.options.maxFps;
		long maxFpsLimit;

		if (maxFps == Option.FRAMERATE_LIMIT.getMax()) {
			maxFpsLimit = 0L;
		} else {
			maxFpsLimit = 1000000000 / maxFps;
		}

		final long budget = Util.getMeasuringTimeNano() - startTime;

		// No idea wtg the 3/2 is for - looks like a hack
		final long updateBudget = wr.canvas_chunkUpdateSmoother().getTargetUsedTime(budget) * 3L / 2L;
		final long clampedBudget = MathHelper.clamp(updateBudget, maxFpsLimit, 33333333L);

		updateChunks(startTime + clampedBudget);

		profiler.swap("terrain");
		renderTerrainLayer(false, matrixStack, cameraX, cameraY, cameraZ);
		DiffuseLighting.enableForLevel(matrixStack.peek().getModel());

		profiler.swap("entities");
		profiler.push("prepare");
		int entityCount = 0;
		profiler.swap("entities");

		final boolean canDrawEntityOutlines = wr.canvas_canDrawEntityOutlines();

		if (canDrawEntityOutlines) {
			wr.canvas_entityOutlinesFramebuffer().clear(MinecraftClient.IS_SYSTEM_MAC);
			mc.getFramebuffer().beginWrite(false);
		}

		boolean bl4 = false;
		final VertexConsumerProvider.Immediate immediate = bufferBuilders.getEntityVertexConsumers();
		final Iterator<Entity> entities = world.getEntities().iterator();
		final ShaderEffect entityOutlineShader = wr.canvas_entityOutlineShader();
		final BuiltRenderRegion[] visibleChunks = this.visibleChunks;

		while(true) {
			Entity entity;
			int x;
			do {
				do {
					do {
						if (!entities.hasNext()) {
							immediate.draw(RenderLayer.getEntitySolid(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							immediate.draw(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							immediate.draw(RenderLayer.getEntityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							immediate.draw(RenderLayer.getEntitySmoothCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							profiler.swap("blockentities");

							final int visibleChunkCount = this.visibleChunkCount;
							final Set<BlockEntity> noCullingBlockEntities = wr.canvas_noCullingBlockEntities();
							int chunkIndex = 0;

							while(true) {
								List<BlockEntity> list;
								do {
									if (chunkIndex == visibleChunkCount) {
										// exits here
										synchronized(noCullingBlockEntities) {
											final Iterator<BlockEntity> var56 = noCullingBlockEntities.iterator();

											while(true) {
												if (!var56.hasNext()) {
													break;
												}

												final BlockEntity blockEntity2 = var56.next();
												final BlockPos blockPos2 = blockEntity2.getPos();
												matrixStack.push();
												matrixStack.translate(blockPos2.getX() - cameraX, blockPos2.getY() - cameraY, blockPos2.getZ() - cameraZ);
												BlockEntityRenderDispatcher.INSTANCE.render(blockEntity2, f, matrixStack, immediate);
												matrixStack.pop();
											}
										}

										immediate.draw(RenderLayer.getSolid());
										immediate.draw(TexturedRenderLayers.getEntitySolid());
										immediate.draw(TexturedRenderLayers.getEntityCutout());
										immediate.draw(TexturedRenderLayers.getBeds());
										immediate.draw(TexturedRenderLayers.getShulkerBoxes());
										immediate.draw(TexturedRenderLayers.getSign());
										immediate.draw(TexturedRenderLayers.getChest());
										bufferBuilders.getOutlineVertexConsumers().draw();

										if (bl4) {
											entityOutlineShader.render(f);
											mc.getFramebuffer().beginWrite(false);
										}

										profiler.swap("destroyProgress");
										final ObjectIterator<Entry<SortedSet<BlockBreakingInfo>>> var53 = wr.canvas_blockBreakingProgressions().long2ObjectEntrySet().iterator();

										while(var53.hasNext()) {
											final Entry<SortedSet<BlockBreakingInfo>> entry = var53.next();
											final BlockPos blockPos3 = BlockPos.fromLong(entry.getLongKey());
											final double y = blockPos3.getX() - cameraX;
											final double z = blockPos3.getY() - cameraY;
											final double aa = blockPos3.getZ() - cameraZ;

											if (y * y + z * z + aa * aa <= 1024.0D) {
												final SortedSet<BlockBreakingInfo> sortedSet2 = entry.getValue();

												if (sortedSet2 != null && !sortedSet2.isEmpty()) {
													final int ab = sortedSet2.last().getStage();
													matrixStack.push();
													matrixStack.translate(blockPos3.getX() - cameraX, blockPos3.getY() - cameraY, blockPos3.getZ() - cameraZ);
													final VertexConsumer vertexConsumer2 = new TransformingVertexConsumer(bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(ab)), matrixStack.peek());
													mc.getBlockRenderManager().renderDamage(world.getBlockState(blockPos3), blockPos3, world, matrixStack, vertexConsumer2);
													matrixStack.pop();
												}
											}
										}

										profiler.pop();
										final HitResult hitResult = mc.crosshairTarget;

										if (bl && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
											profiler.swap("outline");
											final BlockPos blockPos4 = ((BlockHitResult)hitResult).getBlockPos();
											final BlockState blockState = world.getBlockState(blockPos4);

											if (!blockState.isAir() && world.getWorldBorder().contains(blockPos4)) {
												final VertexConsumer vertexConsumer3 = immediate.getBuffer(RenderLayer.getLines());
												wr.canvas_drawBlockOutline(matrixStack, vertexConsumer3, camera.getFocusedEntity(), cameraX, cameraY, cameraZ, blockPos4, blockState);
											}
										}

										RenderSystem.pushMatrix();
										RenderSystem.multMatrix(matrixStack.peek().getModel());
										mc.debugRenderer.render(matrixStack, immediate, cameraX, cameraY, cameraZ);
										wr.canvas_renderWorldBorder(camera);
										RenderSystem.popMatrix();
										immediate.draw(TexturedRenderLayers.getEntityTranslucent());
										immediate.draw(TexturedRenderLayers.getBannerPatterns());
										immediate.draw(TexturedRenderLayers.getShieldPatterns());
										immediate.draw(RenderLayer.getGlint());
										immediate.draw(RenderLayer.getEntityGlint());
										immediate.draw(RenderLayer.getWaterMask());
										bufferBuilders.getEffectVertexConsumers().draw();
										immediate.draw(RenderLayer.getLines());
										immediate.draw();

										profiler.swap("translucent");
										renderTerrainLayer(true, matrixStack, cameraX, cameraY, cameraZ);

										profiler.swap("particles");
										mc.particleManager.renderParticles(matrixStack, immediate, lightmapTextureManager, camera, f);
										RenderSystem.pushMatrix();
										RenderSystem.multMatrix(matrixStack.peek().getModel());

										renderCullBoxes(matrixStack, immediate, cameraX, cameraY, cameraZ, f);

										profiler.swap("cloudsLayers");

										if (mc.options.getCloudRenderMode() != CloudRenderMode.OFF) {
											profiler.swap("clouds");
											((WorldRenderer) wr).renderClouds(matrixStack, f, cameraX, cameraY, cameraZ);
										}

										RenderSystem.depthMask(false);
										profiler.swap("weather");
										wr.canvas_renderWeather(lightmapTextureManager, f, cameraX, cameraY, cameraZ);
										RenderSystem.depthMask(true);
										//this.renderChunkDebugInfo(camera);
										RenderSystem.shadeModel(7424);
										RenderSystem.depthMask(true);
										RenderSystem.disableBlend();
										RenderSystem.popMatrix();
										BackgroundRenderer.method_23792();

										wr.canvas_setEntityCount(entityCount);

										return;
									}

									list = visibleChunks[chunkIndex++].getRenderData().getBlockEntities();
								} while(list.isEmpty());

								final Iterator<BlockEntity> var60 = list.iterator();

								while(var60.hasNext()) {
									final BlockEntity blockEntity = var60.next();
									final BlockPos blockPos = blockEntity.getPos();
									VertexConsumerProvider vertexConsumerProvider3 = immediate;
									matrixStack.push();
									matrixStack.translate(blockPos.getX() - cameraX, blockPos.getY() - cameraY, blockPos.getZ() - cameraZ);
									final SortedSet<BlockBreakingInfo> sortedSet = wr.canvas_blockBreakingProgressions().get(blockPos.asLong());

									if (sortedSet != null && !sortedSet.isEmpty()) {
										x = sortedSet.last().getStage();
										if (x >= 0) {
											final VertexConsumer vertexConsumer = new TransformingVertexConsumer(bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(x)), matrixStack.peek());
											vertexConsumerProvider3 = (renderLayer) -> {
												final VertexConsumer vertexConsumer2 = immediate.getBuffer(renderLayer);
												return renderLayer.method_23037() ? VertexConsumers.dual(vertexConsumer, vertexConsumer2) : vertexConsumer2;
											};
										}
									}

									BlockEntityRenderDispatcher.INSTANCE.render(blockEntity, f, matrixStack, vertexConsumerProvider3);
									matrixStack.pop();
								}
							}
						}

						entity = entities.next();
					} while(!entityRenderDispatcher.shouldRender(entity, frustum, cameraX, cameraY, cameraZ) && !entity.hasPassengerDeep(mc.player));
				} while(entity == camera.getFocusedEntity() && !camera.isThirdPerson() && (!(camera.getFocusedEntity() instanceof LivingEntity) || !((LivingEntity)camera.getFocusedEntity()).isSleeping()));
			} while(entity instanceof ClientPlayerEntity && camera.getFocusedEntity() != entity);

			++entityCount;
			if (entity.age == 0) {
				entity.lastRenderX = entity.getX();
				entity.lastRenderY = entity.getY();
				entity.lastRenderZ = entity.getZ();
			}

			Object vertexConsumerProvider2;
			if (canDrawEntityOutlines && entity.isGlowing()) {
				bl4 = true;
				final OutlineVertexConsumerProvider outlineVertexConsumerProvider = bufferBuilders.getOutlineVertexConsumers();
				vertexConsumerProvider2 = outlineVertexConsumerProvider;
				final int k = entity.getTeamColorValue();
				final int u = k >> 16 & 255;
				final int v = k >> 8 & 255;
				x = k & 255;
				outlineVertexConsumerProvider.setColor(u, v, x, 255);
			} else {
				vertexConsumerProvider2 = immediate;
			}

			// PERF: don't render entities if chunk is not visible and outlines are off
			wr.canvas_renderEntity(entity, cameraX, cameraY, cameraZ, f, matrixStack, (VertexConsumerProvider)vertexConsumerProvider2);
		}
	}

	private void renderCullBoxes(MatrixStack matrixStack, Immediate immediate, double cameraX, double cameraY, double cameraZ,  float tickDelta) {
		final Entity entity = MinecraftClient.getInstance().gameRenderer.getCamera().getFocusedEntity();

		final HitResult hit = entity.rayTrace(12 * 16, tickDelta, true);

		if (hit.getType() != HitResult.Type.BLOCK) {
			return;
		}

		final BlockPos pos = ((BlockHitResult) (hit)).getBlockPos();

		final int regionIndex = renderRegionStorage.getRegionIndexSafely(pos);

		if (regionIndex == -1) {
			return;
		}

		final BuiltRenderRegion region = renderRegionStorage.regions()[regionIndex];
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

	private static int rangeColor(int range) {
		switch (range) {
		default:
		case  PackedBox.RANGE_NEAR:
			return 0x80FF8080;

		case  PackedBox.RANGE_MID:
			return 0x80FFFF80;

		case  PackedBox.RANGE_FAR:
			return 0x8080FF80;

		case  PackedBox.RANGE_EXTREME:
			return 0x808080FF;
		}
	}

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

	private void renderTerrainLayer(boolean isTranslucent, MatrixStack matrixStack, double x, double y, double z) {
		final int visibleChunkCount = this.visibleChunkCount;

		if (visibleChunkCount == 0) {
			return;
		}

		final WorldRendererExt wr = this.wr;
		final MinecraftClient mc = wr.canvas_mc();
		final BuiltRenderRegion[] visibleChunks = this.visibleChunks;
		final VertexFormat vertexFormat = wr.canvas_vertexFormat();

		if (isTranslucent) {
			mc.getProfiler().push("translucent_sort");

			if (translucentSortPositionVersion != frustum.positionVersion()) {
				translucentSortPositionVersion = frustum.positionVersion();

				int j = 0;
				for (int chunkIndex = 0; chunkIndex < visibleChunkCount; chunkIndex++) {
					if (j < 15 && visibleChunks[chunkIndex].enqueueSort()) {
						++j;
					}
				}
			}

			mc.getProfiler().pop();
		}

		mc.getProfiler().push("render_" + (isTranslucent ? "translucent" : "solid"));

		// PERF: things to try
		// more culling
		// backface culling
		// single-draw solid layer via shaders
		// render larger cubes - avoid matrix state changes
		// shared buffers
		// render leaves as solid at distance - omit interior faces
		// optimize frustum tests - consider skipping far plane test
		// retain vertex bindings when possible, use VAO
		// don't render grass, cobwebs, flowers, etc. at longer ranges

		final int startIndex = isTranslucent ? visibleChunkCount - 1 : 0 ;
		final int endIndex = isTranslucent ? -1 : visibleChunkCount;
		final int step = isTranslucent ? -1 : 1;

		for (int chunkIndex = startIndex; chunkIndex != endIndex; chunkIndex += step) {
			final BuiltRenderRegion builtChunk = visibleChunks[chunkIndex];

			final DrawableChunk drawable = isTranslucent ? builtChunk.translucentDrawable() : builtChunk.solidDrawable();

			if (drawable != null && !drawable.isEmpty()) {
				matrixStack.push();
				final BlockPos blockPos = builtChunk.getOrigin();
				matrixStack.translate(blockPos.getX() - x, blockPos.getY() - y, blockPos.getZ() - z);
				RenderSystem.pushMatrix();
				RenderSystem.loadIdentity();
				RenderSystem.multMatrix(matrixStack.peek().getModel());

				final ObjectArrayList<DrawableDelegate> delegates = drawable.delegates();
				final int limit = delegates.size();

				for(int i = 0; i < limit; i++) {
					if (isTranslucent) {
						++lastTranlsucentCount;
					} else {
						++lastSolidCount;
					}

					final DrawableDelegate d = delegates.get(i);
					d.materialState().drawHandler.setup();
					d.bind();
					// TODO: confirm everything that used to happen below happens in bind above
					vertexFormat.startDrawing(d.byteOffset());
					d.draw();
				}

				RenderSystem.popMatrix();
				matrixStack.pop();
			}
		}

		VboBuffer.unbind();
		RenderSystem.clearCurrentColor();
		vertexFormat.endDrawing();
		DrawHandler.teardown();

		mc.getProfiler().pop();
	}

	private void updateChunks(long endNanos) {
		final WorldRendererExt wr = this.wr;
		final Set<BuiltRenderRegion> chunksToRebuild  = this.chunksToRebuild;

		// PERF: don't update terrain unless occlusion data changed and chunk was in view for at least one uploaded chunk
		if (chunkBuilder.upload()) {
			wr.canvas_setNeedsTerrainUpdate(true);
		}

		//final long start = Util.getMeasuringTimeNano();
		//int builtCount = 0;

		if (!chunksToRebuild.isEmpty()) {
			final Iterator<BuiltRenderRegion> iterator = chunksToRebuild.iterator();

			while(iterator.hasNext()) {
				final BuiltRenderRegion builtChunk = iterator.next();

				if (builtChunk.needsImportantRebuild()) {
					builtChunk.rebuildOnMainThread();
				} else {
					builtChunk.enqueuRebuild();
				}

				builtChunk.markBuilt();
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

	//	private static final Direction[] DIRECTIONS = Direction.values();

	public int completedChunkCount() {
		int result = 0;
		final BuiltRenderRegion[] visibleChunks = this.visibleChunks;
		final int limit = visibleChunkCount;

		for (int i = 0; i < limit; i++) {
			final BuiltRenderRegion chunk = visibleChunks[i];

			if (chunk.solidDrawable() != null || chunk.translucentDrawable() != null) {
				++result;
			}
		}

		return result;
	}
}
