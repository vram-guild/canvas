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

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.material.state.RenderLayerHelper;
import grondag.canvas.perf.ChunkRebuildCounters;
import grondag.canvas.render.CanvasWorldRenderer;
import grondag.canvas.terrain.occlusion.PotentiallyVisibleRegion;
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
	private final RenderRegionBuilder renderRegionBuilder;

	final CanvasWorldRenderer cwr;
	final RenderRegionStorage storage;
	final TerrainOccluder cameraOccluder;
	final RenderChunk renderChunk;

	public final RegionPosition origin;
	public final RegionOcclusionState occlusionState;
	public final NeighborRegions neighbors;

	/**
	 * Set by main thread during schedule. Retrieved and set to null by worker
	 * right before building.
	 *
	 * <p>Special values also signal the need for translucency sort and chunk reset.
	 */
	private volatile AtomicReference<PackedInputRegion> inputState = new AtomicReference<>(SignalInputRegion.IDLE);

	private final AtomicReference<RegionBuildState> buildState;
	private final ObjectOpenHashSet<BlockEntity> localNoCullingBlockEntities = new ObjectOpenHashSet<>();

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

	boolean isClosed = false;

	public RenderRegion(RenderChunk chunk, long packedPos) {
		cwr = chunk.storage.cwr;
		cameraOccluder = cwr.terrainIterator.cameraOccluder;
		renderRegionBuilder = cwr.regionBuilder();
		storage = chunk.storage;
		storage.trackRegionLoaded();
		renderChunk = chunk;
		buildState = new AtomicReference<>(RegionBuildState.UNBUILT);
		needsRebuild = true;
		origin = new RegionPosition(packedPos, this);
		neighbors = new NeighborRegions(this);
		occlusionState = new RegionOcclusionState(this);
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
	 * Regions should not be built unless or until this is true.
	 * @return True if nearby or if all neighbors are loaded.
	 */
	public boolean isNearOrHasLoadedNeighbors() {
		return origin.isNear() || renderChunk.areCornersLoaded();
	}

	void updatePositionAndVisibility() {
		origin.update();
		occlusionState.update();
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

		if (!origin.checkAndUpdateSortNeeded(sortPositionVersion)) {
			return false;
		}

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
				// Even if empty the chunk may still be needed for visibility search to progress
				occlusionState.notifyOfOcclusionChange();
			}

			return;
		}

		// If we are no longer in potentially visible region, abort build and restore needsRebuild.
		// We also don't force a vis update here because, obviously, we can't affect it.
		if (!origin.isPotentiallyVisibleFromCamera() && !origin.isPotentiallyVisibleFromSkylight()) {
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
			// Marking the region for rebuild doesn't cause iteration to restart and we
			// may need visibility to restart if it was waiting for this region to progress.
			// WIP: better way to handle that won't force excessive reiteration?
			occlusionState.notifyOfOcclusionChange();
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
			context.prepareForRegion(protoRegion);
			final RegionBuildState chunkData = captureBuildState(context, origin.isNear());

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

	private RegionBuildState captureBuildState(TerrainRenderContext context, boolean isNear) {
		final RegionBuildState newBuildState = new RegionBuildState();
		newBuildState.complete(context.region.occlusion.build(isNear));
		handleBlockEntities(newBuildState, context);

		// don't rebuild occlusion if occlusion did not change
		final RegionBuildState oldBuildState = buildState.getAndSet(newBuildState);

		if (oldBuildState == RegionBuildState.UNBUILT || !Arrays.equals(newBuildState.occlusionData, oldBuildState.occlusionData)) {
			occlusionState.notifyOfOcclusionChange();
		}

		return newBuildState;
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
				// Even if empty the chunk may still be needed for visibility search to progress
				occlusionState.notifyOfOcclusionChange();
			}
		} else {
			final TerrainRenderContext context = renderRegionBuilder.mainThreadContext.prepareForRegion(region);
			final RegionBuildState regionData = captureBuildState(context, origin.isNear());

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

	@Override
	public BlockPos origin() {
		return origin;
	}
}
