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
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.material.state.RenderLayerHelper;
import grondag.canvas.perf.ChunkRebuildCounters;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.render.CanvasWorldRenderer;
import grondag.canvas.terrain.occlusion.CameraPotentiallyVisibleRegionSet;
import grondag.canvas.terrain.occlusion.PotentiallyVisibleRegion;
import grondag.canvas.terrain.occlusion.ShadowPotentiallyVisibleRegionSet;
import grondag.canvas.terrain.occlusion.TerrainIterator;
import grondag.canvas.terrain.occlusion.TerrainOccluder;
import grondag.canvas.terrain.occlusion.geometry.RegionOcclusionCalculator;
import grondag.canvas.terrain.region.input.InputRegion;
import grondag.canvas.terrain.region.input.PackedInputRegion;
import grondag.canvas.terrain.region.input.SignalInputRegion;
import grondag.canvas.terrain.render.DrawableChunk;
import grondag.canvas.terrain.render.UploadableChunk;
import grondag.canvas.terrain.util.RenderRegionStateIndexer;
import grondag.canvas.terrain.util.TerrainExecutor.TerrainExecutorTask;
import grondag.frex.api.fluid.FluidQuadSupplier;

@Environment(EnvType.CLIENT)
public class RenderRegion implements TerrainExecutorTask, PotentiallyVisibleRegion {
	private static final AtomicInteger BUILD_COUNTER = new AtomicInteger();

	final CanvasWorldRenderer cwr;
	private final RenderRegionBuilder renderRegionBuilder;
	final RenderRegionStorage storage;
	final TerrainOccluder cameraOccluder;
	final RenderRegionChunk renderRegionChunk;
	public final RegionPosition origin;
	public final RenderRegionNeighbors neighbors;

	/**
	 * Set by main thread during schedule. Retrieved and set to null by worker
	 * right before building.
	 *
	 * <p>Special values also signal the need for translucency sort and chunk reset.
	 */
	private volatile AtomicReference<PackedInputRegion> inputState = new AtomicReference<>(SignalInputRegion.IDLE);

	private final AtomicReference<RegionBuildState> buildState;
	private final ObjectOpenHashSet<BlockEntity> localNoCullingBlockEntities = new ObjectOpenHashSet<>();

	private int cameraOcclusionVersion;
	private boolean cameraOccluderResult;
	private int sortPositionVersion = -1;

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
	private int lastSeenCameraPvsVersion;
	boolean isClosed = false;

	/** Incremented when this region is built and the occlusion data changes (including first time). */
	private int buildVersion = -1;

	/** Build version that was in effect last time drawn to occluder. */
	private int cameraOcclusionBuildVersion;

	/** Concatenated bit flags marking the shadow cascades that include this region. */
	private int shadowCascadeFlags;

	private int lastSeenShadowPvsVersion;
	private int shadowOccluderVersion;
	private boolean shadowOccluderResult;
	private int shadowOcclusionBuildVersion;

	public RenderRegion(RenderRegionChunk chunk, long packedPos) {
		cwr = chunk.storage.cwr;
		cameraOccluder = cwr.terrainIterator.cameraOccluder;
		renderRegionBuilder = cwr.regionBuilder();
		storage = chunk.storage;
		storage.trackRegionLoaded();
		renderRegionChunk = chunk;
		buildState = new AtomicReference<>(RegionBuildState.UNBUILT);
		needsRebuild = true;
		origin = new RegionPosition(packedPos, this);
		neighbors = new RenderRegionNeighbors(this);
	}

	public void setCameraOccluderResult(boolean occluderResult, int occluderVersion) {
		if (cameraOcclusionVersion == occluderVersion) {
			assert occluderResult == cameraOccluderResult;
		} else {
			if (TerrainIterator.TRACE_OCCLUSION_OUTCOMES) {
				final String prefix = buildState.get().canOcclude() ? "Occluding result " : "Empty result ";
				CanvasMod.LOG.info(prefix + occluderResult + "  dist=" + origin.squaredCameraChunkDistance() + "  buildCounter=" + buildVersion + "  occluderVersion=" + occluderVersion + "  @" + origin.toShortString());
			}

			cameraOccluderResult = occluderResult;
			cameraOcclusionVersion = occluderVersion;
			cameraOcclusionBuildVersion = buildVersion;
		}
	}

	public void setShadowOccluderResult(boolean occluderResult, int occluderVersion) {
		if (shadowOccluderVersion == occluderVersion) {
			assert occluderResult == shadowOccluderResult;
		} else {
			shadowOccluderResult = occluderResult;
			shadowOccluderVersion = occluderVersion;
			shadowOcclusionBuildVersion = buildVersion;
		}
	}

	public boolean cameraOccluderResult() {
		return cameraOccluderResult;
	}

	public boolean shadowOccluderResult() {
		return shadowOccluderResult;
	}

	public boolean matchesCameraOccluderVersion(int occluderVersion) {
		return cameraOcclusionVersion == occluderVersion;
	}

	public boolean matchesShadowOccluderVersion(int occluderVersion) {
		return shadowOccluderVersion == occluderVersion;
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
		return origin.isNear() || renderRegionChunk.areCornersLoaded();
	}

	void updateCameraDistanceAndVisibilityInfo() {
		origin.update();
		invalidateCameraOccluderIfNeeded();
		shadowCascadeFlags = Pipeline.shadowsEnabled() ? cwr.terrainIterator.shadowOccluder.cascadeFlags(origin) : 0;
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
		// WIP track shadow invalidation separately - may change without camera movement

		if (origin.isPotentiallyVisibleFromCamera() && buildState.get().canOcclude()) {
			if (cameraOcclusionVersion == storage.cameraOcclusionVersion()) {
				// Existing - has been drawn in occlusion raster
				if (buildVersion != cameraOcclusionBuildVersion) {
					if (TerrainIterator.TRACE_OCCLUSION_OUTCOMES) {
						CanvasMod.LOG.info("Invalidate - redraw: " + origin.toShortString() + "  occluder version:" + cameraOcclusionVersion);
					}

					storage.invalidateCameraOccluder();
					storage.invalidateShadowOccluder();
				}
			} else if (origin.squaredCameraChunkDistance() < storage.maxSquaredCameraChunkDistance()) {
				// Not yet drawn in current occlusion raster and could be nearer than a chunk that has been
				// Need to invalidate the occlusion raster if both things are true:
				//   1) This region isn't empty (empty regions don't matter for culling)
				//   2) This region is in the view frustum

				if (TerrainIterator.TRACE_OCCLUSION_OUTCOMES) {
					CanvasMod.LOG.info("Invalidate - backtrack: " + origin.toShortString() + "  occluder max:" + storage.maxSquaredCameraChunkDistance()
						+ "  chunk max:" + origin.squaredCameraChunkDistance() + "  occlusion version:" + storage.cameraOcclusionVersion() + "  chunk version:" + cameraOcclusionVersion);
				}

				storage.invalidateCameraOccluder();
			}
		}
	}

	void close() {
		assert RenderSystem.isOnRenderThread();

		if (!isClosed) {
			releaseDrawables();

			isClosed = true;

			neighbors.close();
			storage.trackRegionClosed();
			cancel();
			buildState.set(RegionBuildState.UNBUILT);
			needsRebuild = true;
			origin.close();
		}
	}

	private void releaseDrawables() {
		solidDrawable.close();
		solidDrawable = DrawableChunk.EMPTY_DRAWABLE;

		translucentDrawable.close();
		translucentDrawable = DrawableChunk.EMPTY_DRAWABLE;
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
		final PackedInputRegion region = PackedInputRegion.claim(cwr.getWorld(), origin);

		// Idle region is signal to reschedule
		// If region is something other than idle, we are already in the queue
		// and we only need to update the input protoRegion (which we do here.)
		if (inputState.getAndSet(region) == SignalInputRegion.IDLE) {
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
		final RegionBuildState regionData = buildState.get();

		if (sortPositionVersion == this.sortPositionVersion) {
			return false;
		}

		this.sortPositionVersion = sortPositionVersion;

		if (regionData.translucentState != null && inputState.compareAndSet(SignalInputRegion.IDLE, SignalInputRegion.RESORT_ONLY)) {
			// null means need to reschedule, otherwise was already scheduled for either
			// resort or rebuild, or is invalid, not ready to be built.
			renderRegionBuilder.executor.execute(this);
			return true;
		} else {
			return false;
		}
	}

	protected void cancel() {
		inputState.set(SignalInputRegion.INVALID);
		inputState = new AtomicReference<>(SignalInputRegion.IDLE);
	}

	@Override
	public int priority() {
		return origin.squaredCameraChunkDistance();
	}

	@Override
	public void run(TerrainRenderContext context) {
		final AtomicReference<PackedInputRegion> runningState = inputState;
		final PackedInputRegion protoRegion = runningState.getAndSet(SignalInputRegion.IDLE);

		if (protoRegion == null || protoRegion == SignalInputRegion.INVALID) {
			return;
		}

		if (protoRegion == SignalInputRegion.EMPTY) {
			final RegionBuildState chunkData = new RegionBuildState();
			chunkData.complete(RegionOcclusionCalculator.EMPTY_OCCLUSION_RESULT);

			// don't rebuild occlusion if occlusion did not change
			final RegionBuildState oldBuildData = buildState.getAndSet(chunkData);

			if (oldBuildData == RegionBuildState.UNBUILT || !Arrays.equals(chunkData.occlusionData, oldBuildData.occlusionData)) {
				if (TerrainIterator.TRACE_OCCLUSION_OUTCOMES) {
					final int oldCounter = buildVersion;
					buildVersion = BUILD_COUNTER.incrementAndGet();
					CanvasMod.LOG.info("Updating build counter from " + oldCounter + " to " + buildVersion + " @" + origin.toShortString() + "  (WT empty)");
				} else {
					buildVersion = BUILD_COUNTER.incrementAndGet();
				}

				// Even if empty the chunk may still be needed for visibility search to progress
				notifyCameraVisibilityProgressionIfNeeded();
			}

			return;
		}

		// If we are no longer in potentially visible region, abort build and restore needsRebuild.
		// We also don't force a vis update here because, obviously, we can't affect it.
		if (!origin.isPotentiallyVisibleFromCamera() && !isPotentiallyVisibleFromSkylight()) {
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

		if (protoRegion == SignalInputRegion.RESORT_ONLY) {
			final RegionBuildState regionData = buildState.get();
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

					if (runningState.get() != SignalInputRegion.INVALID) {
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
			final RegionBuildState chunkData = buildRegionData(context, origin.isNear());

			final VertexCollectorList collectors = context.collectors;

			if (runningState.get() == SignalInputRegion.INVALID) {
				collectors.clear();
				protoRegion.release();
				return;
			}

			buildTerrain(context, chunkData);

			if (runningState.get() != SignalInputRegion.INVALID) {
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

	private RegionBuildState buildRegionData(TerrainRenderContext context, boolean isNear) {
		final RegionBuildState regionData = new RegionBuildState();
		regionData.complete(context.region.occlusion.build(isNear));
		handleBlockEntities(regionData, context);

		// don't rebuild occlusion if occlusion did not change
		final RegionBuildState oldBuildData = buildState.getAndSet(regionData);

		if (oldBuildData == RegionBuildState.UNBUILT || !Arrays.equals(regionData.occlusionData, oldBuildData.occlusionData)) {
			if (TerrainIterator.TRACE_OCCLUSION_OUTCOMES) {
				final int oldCounter = buildVersion;
				buildVersion = BUILD_COUNTER.incrementAndGet();
				CanvasMod.LOG.info("Updating build counter from " + oldCounter + " to " + buildVersion + " @" + origin.toShortString());
			} else {
				buildVersion = BUILD_COUNTER.incrementAndGet();
			}

			notifyCameraVisibilityProgressionIfNeeded();
		}

		return regionData;
	}

	private void buildTerrain(TerrainRenderContext context, RegionBuildState regionData) {
		if (ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.startChunk();
		}

		final VertexCollectorList collectors = context.collectors;

		final BlockPos.Mutable searchPos = context.searchPos;
		final int xOrigin = origin.getX();
		final int yOrigin = origin.getY();
		final int zOrigin = origin.getZ();

		final InputRegion region = context.region;
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

	private void handleBlockEntities(RegionBuildState regionData, TerrainRenderContext context) {
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
		final PackedInputRegion region = PackedInputRegion.claim(cwr.getWorld(), origin);

		if (region == SignalInputRegion.EMPTY) {
			final RegionBuildState regionData = new RegionBuildState();
			regionData.complete(RegionOcclusionCalculator.EMPTY_OCCLUSION_RESULT);

			// don't rebuild occlusion if occlusion did not change
			final RegionBuildState oldBuildData = buildState.getAndSet(regionData);

			if (oldBuildData == RegionBuildState.UNBUILT || !Arrays.equals(regionData.occlusionData, oldBuildData.occlusionData)) {
				if (TerrainIterator.TRACE_OCCLUSION_OUTCOMES) {
					final int oldCounter = buildVersion;
					buildVersion = BUILD_COUNTER.incrementAndGet();
					CanvasMod.LOG.info("Updating build counter from " + oldCounter + " to " + buildVersion + " @" + origin.toShortString() + "  (MTR empty)");
				} else {
					buildVersion = BUILD_COUNTER.incrementAndGet();
				}

				// Even if empty the chunk may still be needed for visibility search to progress
				notifyCameraVisibilityProgressionIfNeeded();
			}
		} else {
			final TerrainRenderContext context = renderRegionBuilder.mainThreadContext.prepareRegion(region);
			final RegionBuildState regionData = buildRegionData(context, origin.isNear());

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

	public RegionBuildState getBuildState() {
		return buildState.get();
	}

	public DrawableChunk translucentDrawable() {
		return translucentDrawable;
	}

	public DrawableChunk solidDrawable() {
		return solidDrawable;
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
		// WIP: is frustum version check still correct/needed?
		if (lastSeenCameraPvsVersion != pvsVersion && origin.hasValidFrustumVersion()) {
			lastSeenCameraPvsVersion = pvsVersion;
			cameraPVS.add(this);
		}
	}

	public void addToShadowPvsIfValid() {
		final ShadowPotentiallyVisibleRegionSet<RenderRegion> shadowPVS = storage.shadowPVS;
		final int pvsVersion = shadowPVS.version();

		// The frustum version check is necessary to skip regions without valid info.
		// WIP: is frustum version check still correct/needed?
		if (lastSeenShadowPvsVersion != pvsVersion && origin.hasValidFrustumVersion()) {
			lastSeenShadowPvsVersion = pvsVersion;
			shadowPVS.add(this);
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

	public int shadowCascadeFlags() {
		return shadowCascadeFlags;
	}

	public boolean isPotentiallyVisibleFromSkylight() {
		return origin.isInsideRenderDistance() & shadowCascadeFlags != 0;
	}
}
