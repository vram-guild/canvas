package grondag.canvas.render;

import static grondag.canvas.chunk.ChunkInfoEncoder.decodeBacktrackFlags;
import static grondag.canvas.chunk.ChunkInfoEncoder.decodeChunkIndex;
import static grondag.canvas.chunk.ChunkInfoEncoder.decodeEntryFaceOrdinal;
import static grondag.canvas.chunk.ChunkInfoEncoder.encodeChunkInfo;
import static grondag.canvas.chunk.ChunkInfoEncoder.isBacktrack;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.CloudRenderMode;
import net.minecraft.client.options.Option;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.TransformingVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import grondag.canvas.chunk.BuiltRenderRegion;
import grondag.canvas.chunk.DrawableChunk;
import grondag.canvas.chunk.RegionData;
import grondag.canvas.chunk.RenderRegionBuilder;
import grondag.canvas.chunk.RenderRegionStorage;
import grondag.canvas.chunk.draw.DrawableDelegate;
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

	// TODO: redirect uses in MC WorldRenderer
	public Set<BuiltRenderRegion> chunksToRebuild = Sets.newLinkedHashSet();

	// TODO: remove
	@SuppressWarnings("unused")
	private static final MicroTimer timer = new MicroTimer("setupTerrain", 200);
	public static final MicroTimer innerTimer = new MicroTimer("inner", -1);

	private final IntComparator comparator = new IntComparator() {
		@Override
		public int compare(int a, int b) {
			final int[] searchDist = CanvasWorldRenderer.this.searchDist;
			return Integer.compare(searchDist[a], searchDist[b]);
		}
	};

	private final Swapper swapper = new Swapper() {
		@Override
		public void swap(int a, int b) {
			final int[] searchDist = CanvasWorldRenderer.this.searchDist;
			final int[] searchInfo = CanvasWorldRenderer.this.searchInfo;
			final int distSwap = searchDist[a];
			searchDist[a] = searchDist[b];
			searchDist[b] = distSwap;

			final int infoSwap = searchInfo[a];
			searchInfo[a] = searchInfo[b];
			searchInfo[b] = infoSwap;
		}
	};

	private final IntArrayFIFOQueue searchQueue = new IntArrayFIFOQueue();
	private int[] searchInfo = new int[69696];
	private int[] searchDist = new int[69696];

	private BuiltRenderRegion[] visibleChunks = new BuiltRenderRegion[69696];
	//	private int[] visibleChunkInfo = new int[69696];
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
		final int current = searchInfo.length;

		if (current < size) {
			searchInfo = new int[size];
			searchDist = new int[size];
			visibleChunks = new BuiltRenderRegion[size];
			visibleChunkCount = 0;
		}
	}

	public void setupTerrain(Camera camera, Frustum frustum, boolean capturedFrustum, int frameCounter, boolean isSpectator) {
		final WorldRendererExt wr = this.wr;
		final MinecraftClient mc = wr.canvas_mc();
		final int renderDistance = wr.canvas_renderDistance();

		if (mc.options.viewDistance != renderDistance) {
			wr.canvas_reload();
		}

		final RenderRegionStorage chunkStorage = renderRegionStorage;
		final BuiltRenderRegion[] regions = chunkStorage.regions();
		resizeArraysIfNeeded(regions.length);

		final ClientWorld world = wr.canvas_world();
		final RenderRegionBuilder chunkBuilder = this.chunkBuilder;

		world.getProfiler().push("camera");
		final Vec3d cameraPos = camera.getPos();
		setupCamera(wr, mc, chunkBuilder, cameraPos);

		world.getProfiler().swap("cull");
		mc.getProfiler().swap("culling");
		final BlockPos cameraBlockPos = camera.getBlockPos();

		final int cameraChunkIndex = chunkStorage.getRegionIndexSafely(cameraBlockPos);
		final BuiltRenderRegion cameraChunk = cameraChunkIndex == -1 ? null : regions[cameraChunkIndex];

		mc.getProfiler().swap("update");
		int visibleChunkCount = this.visibleChunkCount;

		if (!capturedFrustum && wr.canvas_checkNeedsTerrainUpdate(cameraPos, camera.getPitch(), camera.getYaw())) {
			wr.canvas_setNeedsTerrainUpdate(false);
			visibleChunkCount = 0;
			final IntArrayFIFOQueue searchQueue = this.searchQueue;
			Entity.setRenderDistanceMultiplier(MathHelper.clamp(mc.options.viewDistance / 8.0D, 1.0D, 2.5D));
			boolean chunkCullingEnabled = mc.chunkCullingEnabled;

			if (cameraChunk != null) {
				// start from camera chunk if camera is in the world
				final Set<Direction> set = getOpenChunkFaces(world, cameraBlockPos);

				if (set.size() == 1) {
					final Vector3f vector3f = camera.getHorizontalPlane();
					final Direction direction = Direction.getFacing(vector3f.getX(), vector3f.getY(), vector3f.getZ()).getOpposite();
					set.remove(direction);
				}

				if (set.isEmpty() && !isSpectator) {
					visibleChunks[visibleChunkCount++] = cameraChunk;
				} else {
					if (isSpectator && world.getBlockState(cameraBlockPos).isFullOpaque(world, cameraBlockPos)) {
						chunkCullingEnabled = false;
					}

					cameraChunk.setFrameIndex(frameCounter);
					searchQueue.enqueue(encodeChunkInfo(cameraChunkIndex, null, 0));
				}
			} else {
				// start from top or bottom of world if camera is outside of world
				startSearchFromOutsideWorld(cameraBlockPos, cameraPos, renderDistance, frustum, frameCounter);
			}

			mc.getProfiler().push("iteration");


			// selectivity estimates
			//			[14:09:08] [main/INFO]: [STDOUT]: nullNeighbor 105
			//			[14:09:08] [main/INFO]: [STDOUT]: visited 461014
			//			[14:09:08] [main/INFO]: [STDOUT]: noThruVis 57950
			//			[14:09:08] [main/INFO]: [STDOUT]: backtrack 283620
			//			[14:09:08] [main/INFO]: [STDOUT]: outsideFrustum 23048
			//			[14:09:08] [main/INFO]: [STDOUT]: notReady 2353

			while(!searchQueue.isEmpty()) {
				final int chunkInfo = searchQueue.dequeueInt();
				final BuiltRenderRegion builtChunk = regions[decodeChunkIndex(chunkInfo)];
				visibleChunks[visibleChunkCount++] = builtChunk;
				final RegionData chunkData = builtChunk.getBuildData();

				if(chunkData == RegionData.EMPTY) {
					// all tests will fail if not loaded
					continue;
				}

				final Direction currentChunkEntryFace = decodeEntryFaceOrdinal(chunkInfo);
				final int[] neighborIndices = builtChunk.getNeighborIndices();

				for(final Direction face : DIRECTIONS) {
					final int adjacentChunkIndex = neighborIndices[face.ordinal()];
					// don't visit if adjacent chunk is null
					if (adjacentChunkIndex == -1) {
						continue;
					}

					final BuiltRenderRegion adjacentChunk = regions[adjacentChunkIndex];

					// don't visit if already visited this frame
					if (adjacentChunk.getFrameIndex() == frameCounter) {
						continue;
					}

					// don't visit if chunk culling is enabled and face is backwards-pointing for current chunk
					if (chunkCullingEnabled) {
						// don't visit if chunk culling is enabled and not visible through our chunk
						if (currentChunkEntryFace != null && !chunkData.isVisibleThrough(currentChunkEntryFace.getOpposite(), face)) {
							continue;
						}

						if(isBacktrack(chunkInfo, face.getOpposite())) {
							continue;
						}
					}

					// all outcomes below here mark chunk as visited
					adjacentChunk.setFrameIndex(frameCounter);

					// don't visit if not in frustum
					if(!frustum.isVisible(adjacentChunk.boundingBox)) {
						continue;
					}

					if (!adjacentChunk.shouldBuild()) {
						// don't visit if chunk is outside near distance and doesn't have all 4 neighbors loaded
						continue;
					}

					// backtrack faces for adjacent will those of current chunk plus the face from which we are visiting
					// this is confusing, because it's actually the opposite of the backwards face because Mojang flips faces when checking
					// FIX: this seems likely to be the source of over-optimistic occlusion bug seen earlier
					final int adjacentChunkInfo = encodeChunkInfo(adjacentChunkIndex, face, decodeBacktrackFlags(chunkInfo));

					searchQueue.enqueue(adjacentChunkInfo);
				}
			}

			this.visibleChunkCount = visibleChunkCount;
			mc.getProfiler().pop();
		}

		mc.getProfiler().swap("rebuildNear");
		final Set<BuiltRenderRegion> oldChunksToRebuild  = chunksToRebuild;
		final Set<BuiltRenderRegion> chunksToRebuild = Sets.newLinkedHashSet();
		this.chunksToRebuild = chunksToRebuild;

		boolean needsTerrainUpdate = false;

		for (int i = 0; i < visibleChunkCount; i++) {
			final BuiltRenderRegion builtChunk = visibleChunks[i];

			if (builtChunk.needsRebuild() || oldChunksToRebuild.contains(builtChunk)) {
				needsTerrainUpdate = true;
				final boolean isNear = squaredDistance(builtChunk.getOrigin(), cameraBlockPos) < 768.0D;

				if (!builtChunk.needsImportantRebuild() && !isNear) {
					chunksToRebuild.add(builtChunk);
				} else {
					mc.getProfiler().push("build near");
					builtChunk.rebuildOnMainThread();
					builtChunk.cancelRebuild();
					mc.getProfiler().pop();
				}
			}
		}

		if (needsTerrainUpdate) {
			wr.canvas_setNeedsTerrainUpdate(true);
		}

		chunksToRebuild.addAll(oldChunksToRebuild);
		mc.getProfiler().pop();
	}

	private final void startSearchFromOutsideWorld(BlockPos cameraBlockPos, Vec3d cameraPos, int renderDistance, Frustum frustum, int frameCounter) {
		final RenderRegionStorage regionStorage = renderRegionStorage;
		final BuiltRenderRegion[] regions = regionStorage.regions();
		final int yLevel = cameraBlockPos.getY() > 0 ? 248 : 8;
		final int xCenter = MathHelper.floor(cameraPos.x / 16.0D) * 16;
		final int zCenter = MathHelper.floor(cameraPos.z / 16.0D) * 16;
		final int[] searchList = searchInfo;
		final int[] searchDist = this.searchDist;

		int searchIndex = 0;

		for(int zOffset = -renderDistance; zOffset <= renderDistance; ++zOffset) {
			for(int xOffset = -renderDistance; xOffset <= renderDistance; ++xOffset) {
				final int regionIndex = regionStorage.getRegionIndexSafely(xCenter + (xOffset << 4) + 8, yLevel, zCenter + (zOffset << 4) + 8);
				final BuiltRenderRegion region = regionIndex == -1 ? null : regions[regionIndex];

				if (region != null && frustum.isVisible(region.boundingBox)) {
					region.setFrameIndex(frameCounter);

					final int chunkInfo = encodeChunkInfo(regionIndex, null, 0);

					searchList[searchIndex] = chunkInfo;
					searchDist[searchIndex++] = squaredDistance(region.getOrigin(), cameraBlockPos);
				}
			}
		}

		it.unimi.dsi.fastutil.Arrays.quickSort(0, searchIndex, comparator, swapper);

		for(int i = 0; i < searchIndex; i++) {
			searchQueue.enqueue(searchList[i]);
		}
	}

	// TODO: remove
	private static int squaredDistance(BlockPos chunkOrigin, BlockPos cameraBlockPos) {
		final int dx = chunkOrigin.getX() + 8 - cameraBlockPos.getX();
		final int dy = chunkOrigin.getY() + 8 - cameraBlockPos.getY();
		final int dz = chunkOrigin.getZ() + 8 - cameraBlockPos.getZ();
		return dx * dx + dy * dy + dz * dz;

	}

	private static Set<Direction> getOpenChunkFaces(World world, BlockPos blockPos) {
		final ChunkOcclusionDataBuilder chunkOcclusionDataBuilder = new ChunkOcclusionDataBuilder();
		final BlockPos blockPos2 = new BlockPos(blockPos.getX() >> 4 << 4, blockPos.getY() >> 4 << 4, blockPos.getZ() >> 4 << 4);
		final WorldChunk worldChunk = world.getWorldChunk(blockPos2);
		final Iterator<?> var5 = BlockPos.iterate(blockPos2, blockPos2.add(15, 15, 15)).iterator();

		while(var5.hasNext()) {
			final BlockPos blockPos3 = (BlockPos)var5.next();
			if (worldChunk.getBlockState(blockPos3).isFullOpaque(world, blockPos3)) {
				chunkOcclusionDataBuilder.markClosed(blockPos3);
			}
		}

		return chunkOcclusionDataBuilder.getOpenFaces(blockPos);
	}

	private void setupCamera(WorldRendererExt wr, MinecraftClient mc, RenderRegionBuilder chunkBuilder, Vec3d cameraPos) {
		final RenderRegionStorage chunks = renderRegionStorage;
		final double dx = mc.player.getX() - wr.canvas_lastCameraChunkUpdateX();
		final double dy = mc.player.getY() - wr.canvas_lastCameraChunkUpdateY();
		final double dz = mc.player.getZ() - wr.canvas_lastCameraChunkUpdateZ();
		final int cameraChunkX = wr.canvas_camereChunkX();
		final int cameraChunkY = wr.canvas_camereChunkY();
		final int cameraChunkZ = wr.canvas_camereChunkZ();

		if (cameraChunkX != mc.player.chunkX || cameraChunkY != mc.player.chunkY || cameraChunkZ != mc.player.chunkZ || dx * dx + dy * dy + dz * dz > 16.0D) {
			wr.canvas_updateLastCameraChunkPositions();
			chunks.updateRegionOrigins(mc.player.getX(), mc.player.getZ());
		}

		chunks.updateCameraDistance(cameraPos);
		chunkBuilder.setCameraPosition(cameraPos);
	}

	public static int playerLightmap() {
		return instance == null ? 0 : instance.playerLightmap;
	}

	private void updatePlayerLightmap(MinecraftClient mc, float f) {
		playerLightmap = mc.getEntityRenderManager().getLight(mc.player, f);
	}

	public void renderWorld(MatrixStack matrixStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f) {
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
		final double d = vec3d.getX();
		final double e = vec3d.getY();
		final double g = vec3d.getZ();
		final Matrix4f matrix4f2 = matrixStack.peek().getModel();
		profiler.swap("culling");

		final Frustum capturedFrustum = wr.canvas_getCapturedFrustum();
		final boolean hasCapturedFrustum = capturedFrustum != null;
		Frustum frustum2;

		if (hasCapturedFrustum) {
			frustum2 = capturedFrustum;
			wr.canvas_setCapturedFrustumPosition(frustum2);
		} else {
			frustum2 = new Frustum(matrix4f2, matrix4f);
			frustum2.setPosition(d, e, g);
		}

		wr.canvas_captureFrustumIfNeeded(matrix4f2, matrix4f, vec3d, hasCapturedFrustum, frustum2);

		profiler.swap("clear");
		BackgroundRenderer.render(camera, f, mc.world, mc.options.viewDistance, gameRenderer.getSkyDarkness(f));
		RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
		final float h = gameRenderer.getViewDistance();
		final boolean bl3 = mc.world.dimension.isFogThick(MathHelper.floor(d), MathHelper.floor(e)) || mc.inGameHud.getBossBarHud().shouldThickenFog();
		if (mc.options.viewDistance >= 4) {
			BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_SKY, h, bl3);
			profiler.swap("sky");
			((WorldRenderer) wr).renderSky(matrixStack, f);
		}

		profiler.swap("fog");
		BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(h - 16.0F, 32.0F), bl3);
		profiler.swap("terrain_setup");

		setupTerrain(camera, frustum2, hasCapturedFrustum, wr.canvas_getAndIncrementFrameIndex(), mc.player.isSpectator());

		profiler.swap("updatechunks");
		final int j = mc.options.maxFps;
		long o;

		if (j == Option.FRAMERATE_LIMIT.getMax()) {
			o = 0L;
		} else {
			o = 1000000000 / j;
		}

		final long p = Util.getMeasuringTimeNano() - l;
		final long q = wr.canvas_chunkUpdateSmoother().getTargetUsedTime(p);
		final long r = q * 3L / 2L;
		final long s = MathHelper.clamp(r, o, 33333333L);

		//		timer.start();
		//innerTimer.start();
		updateChunks(l + s);
		//innerTimer.stop();
		//		if(timer.stop()) {
		//CanvasWorldRenderer.innerTimer.reportAndClear();
		//		}

		profiler.swap("terrain");
		renderLayer(false, matrixStack, d, e, g);
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
		final Iterator<Entity> var39 = world.getEntities().iterator();
		final ShaderEffect entityOutlineShader = wr.canvas_entityOutlineShader();
		final BuiltRenderRegion[] visibleChunks = this.visibleChunks;

		while(true) {
			Entity entity;
			int x;
			do {
				do {
					do {
						if (!var39.hasNext()) {
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
												matrixStack.translate(blockPos2.getX() - d, blockPos2.getY() - e, blockPos2.getZ() - g);
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
											final double y = blockPos3.getX() - d;
											final double z = blockPos3.getY() - e;
											final double aa = blockPos3.getZ() - g;

											if (y * y + z * z + aa * aa <= 1024.0D) {
												final SortedSet<BlockBreakingInfo> sortedSet2 = entry.getValue();

												if (sortedSet2 != null && !sortedSet2.isEmpty()) {
													final int ab = sortedSet2.last().getStage();
													matrixStack.push();
													matrixStack.translate(blockPos3.getX() - d, blockPos3.getY() - e, blockPos3.getZ() - g);
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
												wr.canvas_drawBlockOutline(matrixStack, vertexConsumer3, camera.getFocusedEntity(), d, e, g, blockPos4, blockState);
											}
										}

										RenderSystem.pushMatrix();
										RenderSystem.multMatrix(matrixStack.peek().getModel());
										mc.debugRenderer.render(matrixStack, immediate, d, e, g);
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
										renderLayer(true, matrixStack, d, e, g);
										profiler.swap("particles");
										mc.particleManager.renderParticles(matrixStack, immediate, lightmapTextureManager, camera, f);
										RenderSystem.pushMatrix();
										RenderSystem.multMatrix(matrixStack.peek().getModel());
										profiler.swap("cloudsLayers");
										if (mc.options.getCloudRenderMode() != CloudRenderMode.OFF) {
											profiler.swap("clouds");
											((WorldRenderer) wr).renderClouds(matrixStack, f, d, e, g);
										}

										RenderSystem.depthMask(false);
										profiler.swap("weather");
										wr.canvas_renderWeather(lightmapTextureManager, f, d, e, g);
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
									matrixStack.translate(blockPos.getX() - d, blockPos.getY() - e, blockPos.getZ() - g);
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

						entity = var39.next();
					} while(!entityRenderDispatcher.shouldRender(entity, frustum2, d, e, g) && !entity.hasPassengerDeep(mc.player));
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

			wr.canvas_renderEntity(entity, d, e, g, f, matrixStack, (VertexConsumerProvider)vertexConsumerProvider2);
		}
	}

	private void renderLayer(boolean isTranslucent, MatrixStack matrixStack, double x, double y, double z) {
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

			if (wr.canvas_shouldSortTranslucent(x, y, z)) {
				int j = 0;
				for (int chunkIndex = 0; chunkIndex < visibleChunkCount; chunkIndex++) {
					if (j < 15 && visibleChunks[chunkIndex].enqueueSort()) {
						++j;
					}
				}
			}

			mc.getProfiler().pop();
		}

		mc.getProfiler().push("filterempty");
		mc.getProfiler().swap(() -> {
			return "render_" + (isTranslucent ? "translucent" : "solid");
		});

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

		// TODO: handle unbind in Canvas code
		VertexBuffer.unbind();
		RenderSystem.clearCurrentColor();
		vertexFormat.endDrawing();
		DrawHandler.teardown();
		mc.getProfiler().pop();
	}

	private void updateChunks(long l) {
		final WorldRendererExt wr = this.wr;
		final Set<BuiltRenderRegion> chunksToRebuild  = this.chunksToRebuild;

		if (chunkBuilder.upload()) {
			wr.canvas_setNeedsTerrainUpdate(true);
		}

		// TODO: put back, plus below
		//		final long m = Util.getMeasuringTimeNano();
		//		int i = 0;
		if (!chunksToRebuild.isEmpty()) {
			final Iterator<BuiltRenderRegion> iterator = chunksToRebuild.iterator();

			while(iterator.hasNext()) {
				final BuiltRenderRegion builtChunk = iterator.next();
				if (builtChunk.needsImportantRebuild()) {
					builtChunk.rebuildOnMainThread();
				} else {
					builtChunk.enqueuRebuild();
				}

				builtChunk.cancelRebuild();
				iterator.remove();
				//				++i;
				//				final long n = Util.getMeasuringTimeNano();
				//				final long o = n - m;
				//				final long p = o / i;
				//				final long q = l - n;
				//				if (q < p) {
				//					break;
				//				}
			}
		}
	}

	private static final Direction[] DIRECTIONS = Direction.values();

	public int completedChunkCount() {
		final int result = 0;
		final BuiltRenderRegion[] visibleChunks = this.visibleChunks;
		final int limit = visibleChunkCount;

		for (int i = 0; i < limit; i++) {
			final BuiltRenderRegion chunk = visibleChunks[i];

			if (chunk.solidDrawable() != null || chunk.translucentDrawable() != null) {
				++i;
			}
		}

		return result;
	}
}
