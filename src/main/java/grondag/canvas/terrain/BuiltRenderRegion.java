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

package grondag.canvas.terrain;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.apiimpl.util.FaceConstants;
import grondag.canvas.buffer.encoding.VertexCollectorImpl;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.material.state.RenderLayerHelper;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.perf.ChunkRebuildCounters;
import grondag.canvas.render.CanvasFrustum;
import grondag.canvas.render.CanvasWorldRenderer;
import grondag.canvas.terrain.occlusion.TerrainDistanceSorter;
import grondag.canvas.terrain.occlusion.TerrainIterator;
import grondag.canvas.terrain.occlusion.region.OcclusionRegion;
import grondag.canvas.terrain.occlusion.region.PackedBox;
import grondag.canvas.terrain.render.DrawableChunk;
import grondag.canvas.terrain.render.UploadableChunk;
import grondag.canvas.varia.BlockPosHelper;
import grondag.frex.api.fluid.FluidQuadSupplier;

@Environment(EnvType.CLIENT)
public class BuiltRenderRegion {
	private static int frameIndex;
	private static final AtomicInteger BUILD_COUNTER = new AtomicInteger();

	private final RenderRegionBuilder renderRegionBuilder;
	private final RenderRegionStorage storage;
	private final AtomicReference<RegionData> buildData;
	private final ObjectOpenHashSet<BlockEntity> localNoCullingBlockEntities = new ObjectOpenHashSet<>();
	private final BlockPos origin;
	private final RegionChunkReference chunkReference;
	private final CanvasWorldRenderer cwr;
	private final boolean isBottom;
	private final boolean isTop;
	private final BuiltRenderRegion[] neighbors = new BuiltRenderRegion[6];
	public int occlusionRange;
	private int occluderVersion;
	private boolean occluderResult;
	public float cameraRelativeCenterX;
	public float cameraRelativeCenterY;
	public float cameraRelativeCenterZ;
	int squaredCameraDistance;
	// WIP: need above if using this?
	int squaredChunkDistance;
	private boolean needsRebuild;
	private boolean needsImportantRebuild;
	private volatile RegionBuildState buildState = new RegionBuildState();
	private DrawableChunk translucentDrawable = DrawableChunk.EMPTY_DRAWABLE;
	private DrawableChunk solidDrawable = DrawableChunk.EMPTY_DRAWABLE;
	private int frustumVersion;
	private boolean frustumResult;
	private int lastSeenFrameIndex;
	private boolean isClosed = false;
	private boolean isInsideRenderDistance;
	private final Consumer<TerrainRenderContext> buildTask = this::rebuildOnWorkerThread;
	private int buildCount = -1;
	// build count that was in effect last time drawn to occluder
	private int occlusionBuildCount;

	public BuiltRenderRegion(CanvasWorldRenderer cwr, RenderRegionStorage storage, RegionChunkReference chunkRef, long packedPos) {
		this.cwr = cwr;
		renderRegionBuilder = cwr.regionBuilder();
		this.storage = storage;
		chunkReference = chunkRef;
		chunkReference.retain(this);
		buildData = new AtomicReference<>(RegionData.UNBUILT);
		needsRebuild = true;
		origin = BlockPos.fromLong(packedPos);
		isBottom = origin.getY() == 0;
		isTop = origin.getY() == 240;
	}

	public void setOccluderResult(boolean occluderResult, int occluderVersion) {
		if (this.occluderVersion == occluderVersion) {
			assert occluderResult == this.occluderResult;
		} else {
			if (TerrainIterator.TRACE_OCCLUSION_OUTCOMES) {
				final String prefix = buildData.get().canOcclude() ? "Occluding result " : "Empty result ";
				CanvasMod.LOG.info(prefix + occluderResult + "  dist=" + squaredChunkDistance + "  buildCounter=" + buildCount + "  occluderVersion=" + occluderVersion + "  @" + origin.toShortString());
			}

			this.occluderResult = occluderResult;
			this.occluderVersion = occluderVersion;
			occlusionBuildCount = buildCount;
		}
	}

	public boolean occluderResult() {
		return occluderResult;
	}

	public int occluderVersion() {
		return occluderVersion;
	}

	private static <E extends BlockEntity> void addBlockEntity(List<BlockEntity> chunkEntities, Set<BlockEntity> globalEntities, E blockEntity) {
		final BlockEntityRenderer<E> blockEntityRenderer = BlockEntityRenderDispatcher.INSTANCE.get(blockEntity);

		if (blockEntityRenderer != null) {
			chunkEntities.add(blockEntity);

			if (blockEntityRenderer.rendersOutsideBoundingBox(blockEntity)) {
				globalEntities.add(blockEntity);
			}
		}
	}

	public static void advanceFrameIndex() {
		++frameIndex;
	}

	// PERF: make this lazy?

	/**
	 * Assumes camera distance update has already happened.
	 *
	 * <p>NB: tried a crude hierarchical scheme of checking chunk columns first
	 * but didn't pay off.  Would probably  need to propagate per-plane results
	 * over a more efficient region but that might not even help. Is already
	 * quite fast and typically only one or a few regions per chunk must be tested.
	 */
	public boolean isInFrustum(CanvasFrustum frustum) {
		final int v = frustum.viewVersion();

		if (v == frustumVersion) {
			return frustumResult;
		} else {
			frustumVersion = v;
			//  PERF: implement hierarchical tests with propagation of per-plane inside test results
			final boolean result = frustum.isRegionVisible(this);
			frustumResult = result;
			return result;
		}
	}

	public boolean wasRecentlySeen() {
		return frameIndex - lastSeenFrameIndex < 4 && occluderResult;
	}

	/**
	 * @return True if nearby.  If not nearby and not outside view distance true if neighbors are loaded.
	 */
	public boolean shouldBuild() {
		return squaredCameraDistance <= 576
				|| (isInsideRenderDistance && chunkReference.areCornersLoaded());
	}

	/**
	 * Returns true if could be visible.
	 */
	boolean updateCameraDistanceAndVisibilityInfo(RenderRegionPruner pruner) {
		// WIP: avoid these if camera origin hasn't changed
		final BlockPos origin = this.origin;
		final Vec3d cameraPos = cwr.cameraPos();

		final float dx = (float) (origin.getX() + 8 - cameraPos.x);
		final float dy = (float) (origin.getY() + 8 - cameraPos.y);
		final float dz = (float) (origin.getZ() + 8 - cameraPos.z);
		cameraRelativeCenterX = dx;
		cameraRelativeCenterY = dy;
		cameraRelativeCenterZ = dz;

		final int idx = Math.round(dx);
		final int idy = Math.round(dy);
		final int idz = Math.round(dz);

		final int horizontalSquaredDistance = idx * idx + idz * idz;
		isInsideRenderDistance = horizontalSquaredDistance <= cwr.maxSquaredDistance();

		final int squaredCameraDistance = horizontalSquaredDistance + idy * idy;
		occlusionRange = PackedBox.rangeFromSquareBlockDist(squaredCameraDistance);
		this.squaredCameraDistance = squaredCameraDistance;

		final int cx = pruner.cameraChunkX() - (origin.getX() >> 4);
		final int cy = pruner.cameraChunkY() - (origin.getY() >> 4);
		final int cz = pruner.cameraChunkZ() - (origin.getZ() >> 4);

		squaredChunkDistance = cx * cx + cy * cy + cz * cz;

		// WIP - clean up docs
		// We check here to know if the occlusion raster must be redrawn.
		//
		// The check depends on classifying this region as one of:
		//   new - has not been drawn in raster - occluder version doesn't match
		//   existing - has been drawn in rater - occluder version matches
		//
		// The raster must be redrawn if either is true:
		//   1) A new chunk has a chunk distance less than the current max drawn (we somehow went backwards towards the camera)
		//   2) An existing chunk has been reloaded - the buildCounter doesn't match the buildCounter when it was marked existing

		if (buildData.get().canOcclude() && pruner.frustum.isRegionVisible(this)) {
			if (occluderVersion == pruner.occluderVersion()) {
				// Existing - has been drawn in occlusion raster
				if (buildCount != occlusionBuildCount) {
					if (TerrainIterator.TRACE_OCCLUSION_OUTCOMES) {
						CanvasMod.LOG.info("Invalidate - redraw: " + this.origin.toShortString() + "  occluder version:" + occluderVersion);
					}

					pruner.invalidateOccluder();
				}
			} else if (squaredChunkDistance < pruner.maxSquaredChunkDistance()) {
				// Not yet drawn in current occlusion raster and could be nearer than a chunk that has been
				// Need to invalidate the occlusion raster if both things are true:
				//   1) This region isn't empty (empty regions don't matter for culling)
				//   2) This region is in the view frustum

				if (TerrainIterator.TRACE_OCCLUSION_OUTCOMES) {
					CanvasMod.LOG.info("Invalidate - backtrack: " + this.origin.toShortString() + "  occluder max:" + pruner.maxSquaredChunkDistance()
						+ "  chunk max:" + squaredChunkDistance + "  occluder version:" + pruner.occluderVersion() + "  chunk version:" + occluderVersion);
				}

				pruner.invalidateOccluder();
			}
		}

		return horizontalSquaredDistance < cwr.maxRetentionDistance();
	}

	public int squaredChunkDistance() {
		return squaredChunkDistance;
	}

	void close() {
		assert RenderSystem.isOnRenderThread();

		if (!isClosed) {
			releaseDrawables();

			isClosed = true;

			for (int i = 0; i < 6; ++i) {
				final BuiltRenderRegion nr = neighbors[i];

				if (nr != null) {
					nr.notifyNeighborClosed(BlockPosHelper.oppositeFaceIndex(i), this);
				}
			}

			chunkReference.release(this);

			cancel();
			buildData.set(RegionData.UNBUILT);
			needsRebuild = true;
		}
	}

	private void releaseDrawables() {
		solidDrawable.close();
		solidDrawable = DrawableChunk.EMPTY_DRAWABLE;

		translucentDrawable.close();
		translucentDrawable = DrawableChunk.EMPTY_DRAWABLE;
	}

	public BlockPos getOrigin() {
		return origin;
	}

	public void markForBuild(boolean isImportant) {
		final boolean neededRebuild = needsRebuild;
		needsRebuild = true;
		needsImportantRebuild = isImportant | (neededRebuild && needsImportantRebuild);
	}

	public void markBuilt() {
		needsRebuild = false;
		needsImportantRebuild = false;
	}

	public boolean needsRebuild() {
		return needsRebuild;
	}

	public boolean needsImportantRebuild() {
		return needsRebuild && needsImportantRebuild;
	}

	public void scheduleRebuild() {
		final ProtoRenderRegion region = ProtoRenderRegion.claim(cwr.getWorld(), origin);

		// null region is signal to reschedule
		if (buildState.protoRegion.getAndSet(region) == ProtoRenderRegion.IDLE) {
			renderRegionBuilder.executor.execute(buildTask, squaredCameraDistance);
		}
	}

	public boolean scheduleSort() {
		final RegionData regionData = buildData.get();

		if (regionData.translucentState == null) {
			return false;
		} else {
			if (buildState.protoRegion.compareAndSet(ProtoRenderRegion.IDLE, ProtoRenderRegion.RESORT_ONLY)) {
				// null means need to reschedule, otherwise was already scheduled for either
				// resort or rebuild, or is invalid, not ready to be built.
				renderRegionBuilder.executor.execute(buildTask, squaredCameraDistance);
			}

			return true;
		}
	}

	protected void cancel() {
		buildState.protoRegion.set(ProtoRenderRegion.INVALID);
		buildState = new RegionBuildState();
	}

	private void rebuildOnWorkerThread(TerrainRenderContext context) {
		final RegionBuildState runningState = buildState;
		final ProtoRenderRegion region = runningState.protoRegion.getAndSet(ProtoRenderRegion.IDLE);

		if (region == null || region == ProtoRenderRegion.INVALID) {
			return;
		}

		if (region == ProtoRenderRegion.EMPTY) {
			final RegionData chunkData = new RegionData();
			chunkData.complete(OcclusionRegion.EMPTY_CULL_DATA);

			// don't rebuild occlusion if occlusion did not change
			final RegionData oldBuildData = buildData.getAndSet(chunkData);

			if (oldBuildData == RegionData.UNBUILT || !Arrays.equals(chunkData.occlusionData, oldBuildData.occlusionData)) {
				if (TerrainIterator.TRACE_OCCLUSION_OUTCOMES) {
					final int oldCounter = buildCount;
					buildCount = BUILD_COUNTER.incrementAndGet();
					CanvasMod.LOG.info("Updating build counter from " + oldCounter + " to " + buildCount + " @" + origin.toShortString() + "  (WT empty)");
				} else {
					buildCount = BUILD_COUNTER.incrementAndGet();
				}

				// Even if empty the chunk may still be needed for visibility search to progress
				cwr.forceVisibilityUpdate();
			}

			return;
		}

		// check loaded neighbors and camera distance, abort rebuild and restore needsRebuild if out of view/not ready
		if (!shouldBuild()) {
			markForBuild(false);
			region.release();
			cwr.forceVisibilityUpdate();
			return;
		}

		if (region == ProtoRenderRegion.RESORT_ONLY) {
			final RegionData regionData = buildData.get();
			final int[] state = regionData.translucentState;

			if (state != null) {
				final Vec3d cameraPos = cwr.cameraPos();
				final VertexCollectorList collectors = context.collectors;
				final RenderMaterialImpl translucentState = RenderLayerHelper.TRANSLUCENT_TERRAIN;
				final VertexCollectorImpl collector = collectors.get(translucentState);

				collector.loadState(translucentState, state);

				if (Configurator.batchedChunkRender) {
					collector.sortQuads(
						(float) (cameraPos.x - TerrainModelSpace.renderCubeOrigin(origin.getX())),
						(float) (cameraPos.y - TerrainModelSpace.renderCubeOrigin(origin.getY())),
						(float) (cameraPos.z - TerrainModelSpace.renderCubeOrigin(origin.getZ())));
				} else {
					collector.sortQuads(
						(float) (cameraPos.x - origin.getX()),
						(float) (cameraPos.y - origin.getY()),
						(float) (cameraPos.z - origin.getZ()));
				}

				regionData.translucentState = collector.saveState(state);

				if (runningState.protoRegion.get() != ProtoRenderRegion.INVALID) {
					final UploadableChunk upload = collectors.toUploadableChunk(true);

					if (upload != UploadableChunk.EMPTY_UPLOADABLE) {
						renderRegionBuilder.scheduleUpload(() -> {
							if (ChunkRebuildCounters.ENABLED) {
								ChunkRebuildCounters.startUpload();
							}

							translucentDrawable.close();
							translucentDrawable = upload.produceDrawable();

							if (ChunkRebuildCounters.ENABLED) {
								ChunkRebuildCounters.completeUpload();
							}
						});
					}
				}

				collectors.clear();
			}
		} else {
			context.prepareRegion(region);
			final RegionData chunkData = buildRegionData(context, isNear());

			final VertexCollectorList collectors = context.collectors;

			if (runningState.protoRegion.get() == ProtoRenderRegion.INVALID) {
				collectors.clear();
				region.release();
				return;
			}

			buildTerrain(context, chunkData);

			if (runningState.protoRegion.get() != ProtoRenderRegion.INVALID) {
				final UploadableChunk solidUpload = collectors.toUploadableChunk(false);
				final UploadableChunk translucentUpload = collectors.toUploadableChunk(true);

				if (solidUpload != UploadableChunk.EMPTY_UPLOADABLE || translucentUpload != UploadableChunk.EMPTY_UPLOADABLE) {
					renderRegionBuilder.scheduleUpload(() -> {
						if (ChunkRebuildCounters.ENABLED) {
							ChunkRebuildCounters.startUpload();
						}

						releaseDrawables();
						solidDrawable = solidUpload.produceDrawable();
						translucentDrawable = translucentUpload.produceDrawable();

						if (ChunkRebuildCounters.ENABLED) {
							ChunkRebuildCounters.completeUpload();
						}
					});
				}
			}

			collectors.clear();
			region.release();
		}
	}

	private RegionData buildRegionData(TerrainRenderContext context, boolean isNear) {
		final RegionData regionData = new RegionData();
		regionData.complete(context.region.occlusion.build(isNear));
		handleBlockEntities(regionData, context);

		// don't rebuild occlusion if occlusion did not change
		final RegionData oldBuildData = buildData.getAndSet(regionData);

		if (oldBuildData == RegionData.UNBUILT || !Arrays.equals(regionData.occlusionData, oldBuildData.occlusionData)) {
			if (TerrainIterator.TRACE_OCCLUSION_OUTCOMES) {
				final int oldCounter = buildCount;
				buildCount = BUILD_COUNTER.incrementAndGet();
				CanvasMod.LOG.info("Updating build counter from " + oldCounter + " to " + buildCount + " @" + origin.toShortString());
			} else {
				buildCount = BUILD_COUNTER.incrementAndGet();
			}

			cwr.forceVisibilityUpdate();
		}

		return regionData;
	}

	private void buildTerrain(TerrainRenderContext context, RegionData regionData) {
		if (ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.startChunk();
		}

		final VertexCollectorList collectors = context.collectors;

		final BlockPos.Mutable searchPos = context.searchPos;
		final int xOrigin = origin.getX();
		final int yOrigin = origin.getY();
		final int zOrigin = origin.getZ();

		final int xModelOffset, yModelOffset, zModelOffset;

		if (Configurator.batchedChunkRender) {
			xModelOffset = TerrainModelSpace.renderCubeRelative(xOrigin);
			yModelOffset = TerrainModelSpace.renderCubeRelative(yOrigin);
			zModelOffset = TerrainModelSpace.renderCubeRelative(zOrigin);
		} else {
			xModelOffset = 0;
			yModelOffset = 0;
			zModelOffset = 0;
		}

		final FastRenderRegion region = context.region;
		final Vec3d cameraPos = cwr.cameraPos();
		final MatrixStack matrixStack = new MatrixStack();
		final BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
		final OcclusionRegion occlusionRegion = region.occlusion;

		for (int i = 0; i < RenderRegionAddressHelper.INTERIOR_CACHE_SIZE; i++) {
			if (occlusionRegion.shouldRender(i)) {
				final BlockState blockState = region.getLocalBlockState(i);
				final FluidState fluidState = blockState.getFluidState();
				final int x = i & 0xF;
				final int y = (i >> 4) & 0xF;
				final int z = (i >> 8) & 0xF;
				searchPos.set(xOrigin + x, yOrigin + y, zOrigin + z);

				final boolean hasFluid = !fluidState.isEmpty();
				final boolean hasBlock = blockState.getRenderType() != BlockRenderType.INVISIBLE;

				if (hasFluid || hasBlock) {
					// PERF: allocation, speed
					matrixStack.push();
					matrixStack.translate(x + xModelOffset, y + yModelOffset, z + zModelOffset);

					if (hasFluid) {
						context.renderFluid(blockState, searchPos, false, FluidQuadSupplier.get(fluidState.getFluid()), matrixStack);
					}

					if (hasBlock) {
						if (blockState.getBlock().getOffsetType() != Block.OffsetType.NONE) {
							final Vec3d vec3d = blockState.getModelOffset(region, searchPos);

							if (vec3d != Vec3d.ZERO) {
								matrixStack.translate(vec3d.x, vec3d.y, vec3d.z);
							}
						}

						final BakedModel model = blockRenderManager.getModel(blockState);
						context.renderBlock(blockState, searchPos, model.useAmbientOcclusion(), (FabricBakedModel) model, matrixStack);
					}

					matrixStack.pop();
				}
			}
		}

		regionData.endBuffering((float) (cameraPos.x - xOrigin + xModelOffset), (float) (cameraPos.y - yOrigin + yModelOffset), (float) (cameraPos.z - zOrigin + zModelOffset), collectors);

		if (ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.completeChunk();
		}
	}

	private void handleBlockEntities(RegionData regionData, TerrainRenderContext context) {
		final ObjectOpenHashSet<BlockEntity> nonCullBlockEntities = context.nonCullBlockEntities;
		final ObjectArrayList<BlockEntity> regionDataBlockEntities = regionData.blockEntities;

		// PERF: benchmark vs list, empty indicator, or some other structure
		for (final BlockEntity blockEntity : context.region.blockEntities) {
			if (blockEntity != null) {
				addBlockEntity(regionDataBlockEntities, nonCullBlockEntities, blockEntity);
			}
		}

		final ObjectOpenHashSet<BlockEntity> addedBlockEntities = context.addedBlockEntities;
		final ObjectOpenHashSet<BlockEntity> removedBlockEntities = context.removedBlockEntities;

		if (!localNoCullingBlockEntities.isEmpty()) {
			final ObjectIterator<BlockEntity> it = localNoCullingBlockEntities.iterator();

			while (it.hasNext()) {
				final BlockEntity be = it.next();

				if (!nonCullBlockEntities.contains(be)) {
					it.remove();
					removedBlockEntities.add(be);
				}
			}
		}

		if (!nonCullBlockEntities.isEmpty()) {
			final ObjectIterator<BlockEntity> it = nonCullBlockEntities.iterator();

			while (it.hasNext()) {
				final BlockEntity be = it.next();

				if (localNoCullingBlockEntities.add(be)) {
					addedBlockEntities.add(be);
				}
			}
		}

		cwr.updateNoCullingBlockEntities(removedBlockEntities, addedBlockEntities);
	}

	public void rebuildOnMainThread() {
		final ProtoRenderRegion region = ProtoRenderRegion.claim(cwr.getWorld(), origin);

		if (region == ProtoRenderRegion.EMPTY) {
			final RegionData regionData = new RegionData();
			regionData.complete(OcclusionRegion.EMPTY_CULL_DATA);

			// don't rebuild occlusion if occlusion did not change
			final RegionData oldBuildData = buildData.getAndSet(regionData);

			if (oldBuildData == RegionData.UNBUILT || !Arrays.equals(regionData.occlusionData, oldBuildData.occlusionData)) {
				if (TerrainIterator.TRACE_OCCLUSION_OUTCOMES) {
					final int oldCounter = buildCount;
					buildCount = BUILD_COUNTER.incrementAndGet();
					CanvasMod.LOG.info("Updating build counter from " + oldCounter + " to " + buildCount + " @" + origin.toShortString() + "  (MTR empty)");
				} else {
					buildCount = BUILD_COUNTER.incrementAndGet();
				}

				// Even if empty the chunk may still be needed for visibility search to progress
				cwr.forceVisibilityUpdate();
			}

			return;
		}

		final TerrainRenderContext context = renderRegionBuilder.mainThreadContext.prepareRegion(region);
		final RegionData regionData = buildRegionData(context, isNear());

		buildTerrain(context, regionData);

		if (ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.startUpload();
		}

		final VertexCollectorList collectors = context.collectors;
		final UploadableChunk solidUpload = collectors.toUploadableChunk(false);
		final UploadableChunk translucentUpload = collectors.toUploadableChunk(true);

		releaseDrawables();
		solidDrawable = solidUpload.produceDrawable();
		translucentDrawable = translucentUpload.produceDrawable();

		if (ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.completeUpload();
		}

		collectors.clear();
		region.release();
	}

	public BuiltRenderRegion getNeighbor(int faceIndex) {
		BuiltRenderRegion region = neighbors[faceIndex];

		if (region == null || region.isClosed) {
			final Direction face = ModelHelper.faceFromIndex(faceIndex);
			region = storage.getOrCreateRegion(origin.getX() + face.getOffsetX() * 16, origin.getY() + face.getOffsetY() * 16, origin.getZ() + face.getOffsetZ() * 16);
			neighbors[faceIndex] = region;
			region.attachOrConfirmVisitingNeighbor(BlockPosHelper.oppositeFaceIndex(faceIndex), this);
		}

		return region;
	}

	private void attachOrConfirmVisitingNeighbor(int visitingFaceIndex, BuiltRenderRegion visitingNeighbor) {
		assert neighbors[visitingFaceIndex] == null || neighbors[visitingFaceIndex] == visitingNeighbor
			: "Visting render region is attaching to a position that already has a non-null region";

		neighbors[visitingFaceIndex] = visitingNeighbor;
	}

	private void notifyNeighborClosed(int closedFaceIndex, BuiltRenderRegion closingNeighbor) {
		assert neighbors[closedFaceIndex] == closingNeighbor
			: "Closing neighbor render region does not match current attachment";

		neighbors[closedFaceIndex] = null;
	}

	public RegionData getBuildData() {
		return buildData.get();
	}

	public DrawableChunk translucentDrawable() {
		return translucentDrawable;
	}

	public DrawableChunk solidDrawable() {
		return solidDrawable;
	}

	public boolean isNear() {
		return squaredCameraDistance < 768;
	}

	public void enqueueUnvistedNeighbors(TerrainDistanceSorter distanceSorter) {
		final int index = frameIndex;
		lastSeenFrameIndex = index;

		enqueNeighbor(index, getNeighbor(FaceConstants.EAST_INDEX), distanceSorter);
		enqueNeighbor(index, getNeighbor(FaceConstants.WEST_INDEX), distanceSorter);
		enqueNeighbor(index, getNeighbor(FaceConstants.NORTH_INDEX), distanceSorter);
		enqueNeighbor(index, getNeighbor(FaceConstants.SOUTH_INDEX), distanceSorter);

		if (!isTop) {
			enqueNeighbor(index, getNeighbor(FaceConstants.UP_INDEX), distanceSorter);
		}

		if (!isBottom) {
			enqueNeighbor(index, getNeighbor(FaceConstants.DOWN_INDEX), distanceSorter);
		}
	}

	private void enqueNeighbor(int index, BuiltRenderRegion r, TerrainDistanceSorter queue) {
		// WIP: reduce checks by storing farther neighbors when distance is updated
		if (r.lastSeenFrameIndex != index && r.squaredChunkDistance > squaredChunkDistance) {
			r.lastSeenFrameIndex = index;
			queue.add(r);
		}
	}
}
