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

package grondag.canvas.terrain.region;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vector4f;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

import grondag.bitraster.PackedBox;
import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.apiimpl.util.FaceConstants;
import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.material.state.RenderLayerHelper;
import grondag.canvas.perf.ChunkRebuildCounters;
import grondag.canvas.render.CanvasWorldRenderer;
import grondag.canvas.shader.data.ShadowMatrixData;
import grondag.canvas.terrain.occlusion.CameraPotentiallyVisibleRegionSet;
import grondag.canvas.terrain.occlusion.PotentiallyVisibleRegion;
import grondag.canvas.terrain.occlusion.TerrainIterator;
import grondag.canvas.terrain.occlusion.TerrainOccluder;
import grondag.canvas.terrain.occlusion.geometry.RegionOcclusionCalculator;
import grondag.canvas.terrain.render.DrawableChunk;
import grondag.canvas.terrain.render.UploadableChunk;
import grondag.canvas.terrain.util.RenderRegionStateIndexer;
import grondag.canvas.terrain.util.TerrainExecutor.TerrainExecutorTask;
import grondag.canvas.varia.BlockPosHelper;
import grondag.frex.api.fluid.FluidQuadSupplier;

@Environment(EnvType.CLIENT)
public class BuiltRenderRegion implements TerrainExecutorTask, PotentiallyVisibleRegion {
	private static final AtomicInteger BUILD_COUNTER = new AtomicInteger();

	private final RenderRegionBuilder renderRegionBuilder;
	private final RenderRegionStorage storage;
	private final TerrainOccluder cameraOccluder;

	private final AtomicReference<RegionData> buildData;
	private final ObjectOpenHashSet<BlockEntity> localNoCullingBlockEntities = new ObjectOpenHashSet<>();
	private final BlockPos origin;
	private final int chunkY;
	private final RenderRegionChunk renderRegionChunk;
	private final CanvasWorldRenderer cwr;
	private final boolean isBottom;
	private final boolean isTop;
	private final BuiltRenderRegion[] neighbors = new BuiltRenderRegion[6];

	/**
	 * Set by main thread during schedule. Retrieved and set to null by worker
	 * right before building.
	 *
	 * <p>Special values also signal the need for translucency sort and chunk reset.
	 */
	private volatile AtomicReference<ProtoRenderRegion> buildState = new AtomicReference<>(SignalRegion.IDLE);

	public int occlusionRange;
	private int cameraOccluderVersion;
	private boolean cameraOccluderResult;
	private int chunkDistVersion = -1;
	private int sortPositionVersion = -1;

	/** Used by frustum tests. Will be current only if region is within render distance. */
	public float cameraRelativeCenterX;
	public float cameraRelativeCenterY;
	public float cameraRelativeCenterZ;

	private int squaredCameraChunkDistance;
	private boolean isNear;
	private boolean isNeededForCameraVisibilityProgression = false;

	/**
	 * Indicates the region is not current with world state.
	 * This means, if the region needs to be rendered or used
	 * for visibility testing, then a rebuild should be run OR scheduled.
	 * Marked false as soon as a build is run on thread or when a rebuild it scheduled.
	 * If a scheduled rebuild is cancelled, this is reset to true.
	 *
	 * <p>This behavior allows a region to be re-scheduled after it is already in the execution queue.
	 * This is necessary because the region may have changed, invalidating the data cached in the execution queue.
	 * Note that rescheduling will not result in multiple redundant tasks - the region
	 * IS the task.  Rescheduling for a region already scheduled simply updates the input data.
	 */
	private boolean needsRebuild;
	private boolean needsImportantRebuild;
	private DrawableChunk translucentDrawable = DrawableChunk.EMPTY_DRAWABLE;
	private DrawableChunk solidDrawable = DrawableChunk.EMPTY_DRAWABLE;
	private int occlusionFrustumVersion = -1;
	private int positionVersion = -1;
	private boolean isPotentiallyVisibleFromCamera;
	private int lastSeenCameraPvsVersion;
	private boolean isClosed = false;
	private boolean isInsideRenderDistance;
	private int buildCount = -1;
	// build count that was in effect last time drawn to occluder
	private int cameraOcclusionBuildCount;

	public BuiltRenderRegion(RenderRegionChunk chunk, long packedPos) {
		cwr = chunk.storage.cwr;
		cameraOccluder = cwr.terrainIterator.cameraOccluder;
		renderRegionBuilder = cwr.regionBuilder();
		storage = chunk.storage;
		storage.trackRegionLoaded();
		renderRegionChunk = chunk;
		buildData = new AtomicReference<>(RegionData.UNBUILT);
		needsRebuild = true;
		origin = BlockPos.fromLong(packedPos);
		chunkY = origin.getY() >> 4;
		isBottom = origin.getY() == cwr.getWorld().getBottomY();
		isTop = origin.getY() == cwr.getWorld().getTopY() - 16;
	}

	@Override
	public String toString() {
		return String.format("%s  sqcd=%d  rebuild=%b  closed=%b  frustVer=%d  frustResult=%b  posVer=%d  lastSeenSortVer=%d  inRenderDist=%b",
			origin.toShortString(),
			squaredCameraChunkDistance,
			needsRebuild,
			isClosed,
			occlusionFrustumVersion,
			isPotentiallyVisibleFromCamera,
			positionVersion,
			lastSeenCameraPvsVersion,
			isInsideRenderDistance);
	}

	public void setCameraOccluderResult(boolean occluderResult, int occluderVersion) {
		if (cameraOccluderVersion == occluderVersion) {
			assert occluderResult == cameraOccluderResult;
		} else {
			if (TerrainIterator.TRACE_OCCLUSION_OUTCOMES) {
				final String prefix = buildData.get().canOcclude() ? "Occluding result " : "Empty result ";
				CanvasMod.LOG.info(prefix + occluderResult + "  dist=" + squaredCameraChunkDistance + "  buildCounter=" + buildCount + "  occluderVersion=" + occluderVersion + "  @" + origin.toShortString());
			}

			cameraOccluderResult = occluderResult;
			cameraOccluderVersion = occluderVersion;
			cameraOcclusionBuildCount = buildCount;
		}
	}

	public boolean cameraOccluderResult() {
		return cameraOccluderResult;
	}

	public int cameraOccluderVersion() {
		return cameraOccluderVersion;
	}

	private static <E extends BlockEntity> void addBlockEntity(List<BlockEntity> chunkEntities, Set<BlockEntity> globalEntities, E blockEntity) {
		final BlockEntityRenderer<E> blockEntityRenderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().get(blockEntity);

		if (blockEntityRenderer != null) {
			chunkEntities.add(blockEntity);

			if (blockEntityRenderer.rendersOutsideBoundingBox(blockEntity)) {
				globalEntities.add(blockEntity);
			}
		}
	}

	/**
	 * True when region is within render distance and also within the camera frustum.
	 *
	 * <p>NB: tried a crude hierarchical scheme of checking chunk columns first
	 * but didn't pay off.  Would probably  need to propagate per-plane results
	 * over a more efficient region but that might not even help. Is already
	 * quite fast and typically only one or a few regions per chunk must be tested.
	 */
	public boolean isPotentiallyVisibleFromCamera() {
		return isPotentiallyVisibleFromCamera;
	}

	/**
	 * Called by terrain iterator when this region is needed for terrain iteration to progress.
	 * When this region completes building or when a build is cancelled, will trigger a visibility update.
	 */
	public void markNeededForCameraVisibilityProgression() {
		isNeededForCameraVisibilityProgression = true;
	}

	private void notifyCameraVisibilityProgressionIfNeeded() {
		if (isNeededForCameraVisibilityProgression) {
			isNeededForCameraVisibilityProgression = false;
			cwr.forceVisibilityUpdate();
		}
	}

	public boolean wasRecentlySeenFromCamera() {
		return storage.cameraPVS.version() - lastSeenCameraPvsVersion < 4 && cameraOccluderResult;
	}

	/**
	 * Regions should not be built unless or until this is true.
	 * @return True if nearby or if all neighbors are loaded.
	 */
	public boolean isNearOrHasLoadedNeighbors() {
		return isNear || renderRegionChunk.areCornersLoaded();
	}

	void updateCameraDistanceAndVisibilityInfo() {
		final int fv = cameraOccluder.frustumViewVersion();

		if (chunkDistVersion != renderRegionChunk.chunkDistVersion) {
			chunkDistVersion = renderRegionChunk.chunkDistVersion;
			computeDistanceChecks();
		}

		if (fv != occlusionFrustumVersion) {
			occlusionFrustumVersion = fv;
			computeFrustumChecks();
		}

		invalidateCameraOccluderIfNeeded();
	}

	private void computeDistanceChecks() {
		final int cy = storage.cameraChunkY() - chunkY;
		final int horizontalSquaredDistance = renderRegionChunk.horizontalSquaredDistance;
		isInsideRenderDistance = horizontalSquaredDistance <= cwr.maxSquaredChunkRenderDistance();
		squaredCameraChunkDistance = horizontalSquaredDistance + cy * cy;
		isNear = squaredCameraChunkDistance <= 3;
		occlusionRange = PackedBox.rangeFromSquareChunkDist(squaredCameraChunkDistance);
	}

	private void computeFrustumChecks() {
		// position version can only be different if overall frustum version is different
		final int pv = cameraOccluder.frustumPositionVersion();

		if (pv != positionVersion) {
			positionVersion = pv;

			// these are needed by the frustum - only need to recompute when position moves
			// not needed at all if outside of render distance
			if (isInsideRenderDistance) {
				final Vec3d cameraPos = cameraOccluder.frustumCameraPos();
				final float dx = (float) (origin.getX() + 8 - cameraPos.x);
				final float dy = (float) (origin.getY() + 8 - cameraPos.y);
				final float dz = (float) (origin.getZ() + 8 - cameraPos.z);
				cameraRelativeCenterX = dx;
				cameraRelativeCenterY = dy;
				cameraRelativeCenterZ = dz;
			}
		}

		//  PERF: implement hierarchical tests with propagation of per-plane inside test results
		isPotentiallyVisibleFromCamera = isInsideRenderDistance && cameraOccluder.isRegionVisible(this);
	}

	/**
	 * Called for camera region because frustum checks on near plane appear to be a little wobbly.
	 */
	public void forceCameraPotentialVisibility() {
		isPotentiallyVisibleFromCamera = true;
	}

	/**
	 * We check here to know if the occlusion raster must be redrawn.
	 *
	 * <p>The check depends on classifying this region as one of:<ul>
	 *   <li>new - has not been drawn in raster - occluder version doesn't match
	 *   <li>existing - has been drawn in rater - occluder version matches</ul>
	 *
	 * <p>The raster must be redrawn if either is true:<ul>
	 *   <li>A new chunk has a chunk distance less than the current max drawn (we somehow went backwards towards the camera)
	 *   <li>An existing chunk has been reloaded - the buildCounter doesn't match the buildCounter when it was marked existing</ul>
	 */
	private void invalidateCameraOccluderIfNeeded() {
		if (isPotentiallyVisibleFromCamera && buildData.get().canOcclude()) {
			if (cameraOccluderVersion == storage.cameraOccluderVersion()) {
				// Existing - has been drawn in occlusion raster
				if (buildCount != cameraOcclusionBuildCount) {
					if (TerrainIterator.TRACE_OCCLUSION_OUTCOMES) {
						CanvasMod.LOG.info("Invalidate - redraw: " + origin.toShortString() + "  occluder version:" + cameraOccluderVersion);
					}

					storage.invalidateCameraOccluder();
				}
			} else if (squaredCameraChunkDistance < storage.maxSquaredCameraChunkDistance()) {
				// Not yet drawn in current occlusion raster and could be nearer than a chunk that has been
				// Need to invalidate the occlusion raster if both things are true:
				//   1) This region isn't empty (empty regions don't matter for culling)
				//   2) This region is in the view frustum

				if (TerrainIterator.TRACE_OCCLUSION_OUTCOMES) {
					CanvasMod.LOG.info("Invalidate - backtrack: " + origin.toShortString() + "  occluder max:" + storage.maxSquaredCameraChunkDistance()
						+ "  chunk max:" + squaredCameraChunkDistance + "  occluder version:" + storage.cameraOccluderVersion() + "  chunk version:" + cameraOccluderVersion);
				}

				storage.invalidateCameraOccluder();
			}
		}
	}

	public int squaredCameraChunkDistance() {
		return squaredCameraChunkDistance;
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

			storage.trackRegionClosed();
			cancel();
			buildData.set(RegionData.UNBUILT);
			needsRebuild = true;
			occlusionFrustumVersion = -1;
			positionVersion = -1;
			isInsideRenderDistance = false;
			isNear = false;
			isPotentiallyVisibleFromCamera = false;
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

	/**
	 * To be called after rebuild on main thread or region added to the execution queue.
	 */
	private void markBuilt() {
		needsRebuild = false;
		needsImportantRebuild = false;
	}

	public boolean needsRebuild() {
		return needsRebuild;
	}

	public boolean needsImportantRebuild() {
		return needsRebuild && needsImportantRebuild;
	}

	public void prepareAndExecuteRebuildTask() {
		final ProtoRenderRegion region = ProtoRenderRegion.claim(cwr.getWorld(), origin);

		// Idle region is signal to reschedule
		// If region is something other than idle, we are already in the queue
		// and we only need to update the input protoRegion (which we do here.)
		if (buildState.getAndSet(region) == SignalRegion.IDLE) {
			renderRegionBuilder.executor.execute(this);
		}

		markBuilt();
	}

	/**
	 * Schedules a resort of this region if all of the following are true.
	 * 1) region has translucency
	 * 2) region sort version doesn't match the input version
	 * 3) resort isn't already scheduled for this region
	 *
	 * <p>If a resort is already scheduled then the region sort version is
	 * updated to match the input version.
	 *
	 * @param sortPositionVersion The most recent position version counter - for comparision.
	 * @return true if a resort was scheduled
	 */
	public boolean scheduleSort(int sortPositionVersion) {
		final RegionData regionData = buildData.get();

		if (sortPositionVersion == this.sortPositionVersion) {
			return false;
		}

		this.sortPositionVersion = sortPositionVersion;

		if (regionData.translucentState != null && buildState.compareAndSet(SignalRegion.IDLE, SignalRegion.RESORT_ONLY)) {
			// null means need to reschedule, otherwise was already scheduled for either
			// resort or rebuild, or is invalid, not ready to be built.
			renderRegionBuilder.executor.execute(this);
			return true;
		} else {
			return false;
		}
	}

	protected void cancel() {
		buildState.set(SignalRegion.INVALID);
		buildState = new AtomicReference<>(SignalRegion.IDLE);
	}

	@Override
	public int priority() {
		return squaredCameraChunkDistance;
	}

	@Override
	public void run(TerrainRenderContext context) {
		final AtomicReference<ProtoRenderRegion> runningState = buildState;
		final ProtoRenderRegion protoRegion = runningState.getAndSet(SignalRegion.IDLE);

		if (protoRegion == null || protoRegion == SignalRegion.INVALID) {
			return;
		}

		if (protoRegion == SignalRegion.EMPTY) {
			final RegionData chunkData = new RegionData();
			chunkData.complete(RegionOcclusionCalculator.EMPTY_OCCLUSION_RESULT);

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
				notifyCameraVisibilityProgressionIfNeeded();
			}

			return;
		}

		// If we are no longer in potentially visible region, abort build and restore needsRebuild.
		// We also don't force a vis update here because, obviously, we can't affect it.
		if (!isPotentiallyVisibleFromCamera() && !isPotentiallyVisibleFromSkylight()) {
			protoRegion.release();
			// Causes region to be rescheduled if/when it comes back into view
			markForBuild(false);
			return;
		}

		// Abort rebuild and restore needsRebuild if not ready to build because neighbors aren't loaded
		if (!isNearOrHasLoadedNeighbors()) {
			// Causes region to be rescheduled when it becomes ready
			markForBuild(false);
			protoRegion.release();
			// May need visibility to restart if it was waiting for this region to progress
			notifyCameraVisibilityProgressionIfNeeded();
			return;
		}

		if (protoRegion == SignalRegion.RESORT_ONLY) {
			final RegionData regionData = buildData.get();
			final int[] state = regionData.translucentState;

			if (state != null) {
				final VertexCollectorList collectors = context.collectors;
				final ArrayVertexCollector collector = collectors.get(RenderLayerHelper.TRANSLUCENT_TERRAIN);
				final Vec3d sortPos = cwr.cameraVisibleRegions.lastSortPos();
				collector.loadState(state);

				if (collector.sortQuads(
					(float) (sortPos.x - origin.getX()),
					(float) (sortPos.y - origin.getY()),
					(float) (sortPos.z - origin.getZ()))
				) {
					regionData.translucentState = collector.saveState(state);

					if (runningState.get() != SignalRegion.INVALID) {
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
				}

				collectors.clear();
			}
		} else {
			context.prepareRegion(protoRegion);
			final RegionData chunkData = buildRegionData(context, isNear());

			final VertexCollectorList collectors = context.collectors;

			if (runningState.get() == SignalRegion.INVALID) {
				collectors.clear();
				protoRegion.release();
				return;
			}

			buildTerrain(context, chunkData);

			if (runningState.get() != SignalRegion.INVALID) {
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
			protoRegion.release();
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

			notifyCameraVisibilityProgressionIfNeeded();
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

		final FastRenderRegion region = context.region;
		final MatrixStack matrixStack = new MatrixStack();
		final MatrixStack.Entry entry = matrixStack.peek();
		final Matrix4f modelMatrix = entry.getModel();
		final Matrix3f normalMatrix = entry.getNormal();

		final BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
		final RegionOcclusionCalculator occlusionRegion = region.occlusion;

		for (int i = 0; i < RenderRegionStateIndexer.INTERIOR_STATE_COUNT; i++) {
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
					// Vanilla does a push/pop for each block but that creates needless allocation spam.
					modelMatrix.loadIdentity();
					modelMatrix.multiplyByTranslation(x, y, z);
					normalMatrix.loadIdentity();

					if (hasFluid) {
						context.renderFluid(blockState, searchPos, false, FluidQuadSupplier.get(fluidState.getFluid()), matrixStack);
					}

					if (hasBlock) {
						if (blockState.getBlock().getOffsetType() != Block.OffsetType.NONE) {
							final Vec3d vec3d = blockState.getModelOffset(region, searchPos);

							if (vec3d != Vec3d.ZERO) {
								modelMatrix.multiplyByTranslation((float) vec3d.x, (float) vec3d.y, (float) vec3d.z);
							}
						}

						final BakedModel model = blockRenderManager.getModel(blockState);
						context.renderBlock(blockState, searchPos, model.useAmbientOcclusion(), (FabricBakedModel) model, matrixStack);
					}
				}
			}
		}

		final Vec3d sortPos = cwr.cameraVisibleRegions.lastSortPos();
		regionData.endBuffering((float) (sortPos.x - xOrigin), (float) (sortPos.y - yOrigin), (float) (sortPos.z - zOrigin), collectors);

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

		if (region == SignalRegion.EMPTY) {
			final RegionData regionData = new RegionData();
			regionData.complete(RegionOcclusionCalculator.EMPTY_OCCLUSION_RESULT);

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
				notifyCameraVisibilityProgressionIfNeeded();
			}
		} else {
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

		markBuilt();
	}

	public BuiltRenderRegion getNeighbor(int faceIndex) {
		BuiltRenderRegion region = neighbors[faceIndex];

		if (region == null || region.isClosed) {
			if ((faceIndex == FaceConstants.UP_INDEX && isTop) || (faceIndex == FaceConstants.DOWN_INDEX && isBottom)) {
				return null;
			}

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

	/**
	 * Our logic for this is a little different than vanilla, which checks for squared distance
	 * to chunk center from camera < 768.0.  Ours will always return true for all 26 chunks adjacent
	 * (including diagonal) to the achunk containing the camera.
	 *
	 * <p>This logic is in {@link #updateCameraDistanceAndVisibilityInfo(TerrainVisibilityState)}.
	 */
	public boolean isNear() {
		return isNear;
	}

	public void enqueueUnvistedCameraNeighbors() {
		getNeighbor(FaceConstants.EAST_INDEX).addToCameraPvsIfValid();
		getNeighbor(FaceConstants.WEST_INDEX).addToCameraPvsIfValid();
		getNeighbor(FaceConstants.NORTH_INDEX).addToCameraPvsIfValid();
		getNeighbor(FaceConstants.SOUTH_INDEX).addToCameraPvsIfValid();

		if (!isTop) {
			getNeighbor(FaceConstants.UP_INDEX).addToCameraPvsIfValid();
		}

		if (!isBottom) {
			getNeighbor(FaceConstants.DOWN_INDEX).addToCameraPvsIfValid();
		}
	}

	public void addToCameraPvsIfValid() {
		// Previously checked for r.squaredChunkDistance > squaredChunkDistance
		// but some progression patterns seem to require it or chunks are missed.
		// This is probably because a nearer path has an occlude chunk and so it
		// has to be found reaching around. This will cause some backtracking and
		// thus redraw of the occluder, but that already happens and is handled.

		final CameraPotentiallyVisibleRegionSet cameraPVS = storage.cameraPVS;
		final int pvsVersion = cameraPVS.version();

		// The frustum version check is necessary to skip regions without valid info.
		if (lastSeenCameraPvsVersion != pvsVersion && occlusionFrustumVersion != -1) {
			lastSeenCameraPvsVersion = pvsVersion;
			cameraPVS.add(this);
		}
	}

	/** For debugging. */
	public boolean sharesOriginWith(int blockX, int blockY, int blockZ) {
		return origin.getX() >> 4 == blockX >> 4 && origin.getY() >> 4 == blockY >> 4 && origin.getZ() >> 4 == blockZ >> 4;
	}

	@Override
	public BlockPos origin() {
		return origin;
	}

	private final Vector4f lightSpaceChunkCenter = new Vector4f();

	public int shadowCascade() {
		// WIP: elsewhere need to compute max radius of corners in skylight view

		// Compute center position in light space

		lightSpaceChunkCenter.set(cameraRelativeCenterX, cameraRelativeCenterY, cameraRelativeCenterZ, 1.0f);
		lightSpaceChunkCenter.transform(ShadowMatrixData.shadowViewMatrix);

		return 0;
	}

	/**
	 * Always false when shadow map is disabled.
	 */
	private boolean isPotentiallyVisibleFromSkylight() {
		// WIP: implement
		return false;
	}
}
