/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.terrain.region;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import io.vram.frex.api.model.BlockModel;
import io.vram.frex.api.model.fluid.FluidModel;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.buffer.input.OldVertexCollector;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.material.state.TerrainRenderStates;
import grondag.canvas.perf.ChunkRebuildCounters;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.render.terrain.TerrainSectorMap.RegionRenderSector;
import grondag.canvas.render.terrain.base.DrawableRegion;
import grondag.canvas.render.terrain.base.UploadableRegion;
import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.terrain.occlusion.camera.CameraRegionVisibility;
import grondag.canvas.terrain.occlusion.geometry.RegionOcclusionCalculator;
import grondag.canvas.terrain.occlusion.shadow.ShadowRegionVisibility;
import grondag.canvas.terrain.region.input.InputRegion;
import grondag.canvas.terrain.region.input.PackedInputRegion;
import grondag.canvas.terrain.region.input.SignalInputRegion;
import grondag.canvas.terrain.util.RenderRegionStateIndexer;
import grondag.canvas.terrain.util.TerrainExecutor;
import grondag.canvas.terrain.util.TerrainExecutorTask;

public class RenderRegion implements TerrainExecutorTask {
	private final RenderRegionBuilder renderRegionBuilder;

	final WorldRenderState worldRenderState;
	final RenderRegionStorage storage;

	public final RenderChunk renderChunk;
	public final RegionPosition origin;
	public final CameraRegionVisibility cameraVisibility;
	public final ShadowRegionVisibility shadowVisibility;
	public final NeighborRegions neighbors;

	private RegionRenderSector renderSector = null;

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
	private DrawableRegion translucentDrawable = DrawableRegion.EMPTY_DRAWABLE;
	private DrawableRegion solidDrawable = DrawableRegion.EMPTY_DRAWABLE;
	public final BitSet animationBits = new BitSet();

	private boolean isClosed = false;

	public RenderRegion(RenderChunk chunk, long packedPos) {
		worldRenderState = chunk.worldRenderState;
		renderRegionBuilder = worldRenderState.regionBuilder();
		storage = worldRenderState.renderRegionStorage;
		storage.trackRegionLoaded();
		renderChunk = chunk;
		buildState = new AtomicReference<>(RegionBuildState.UNBUILT);
		needsRebuild = true;
		origin = new RegionPosition(packedPos, this);
		neighbors = new NeighborRegions(this);
		cameraVisibility = worldRenderState.terrainIterator.cameraVisibility.createRegionState(this);
		shadowVisibility = worldRenderState.terrainIterator.shadowVisibility.createRegionState(this);
		origin.update();
	}

	private static <E extends BlockEntity> void addBlockEntity(List<BlockEntity> chunkEntities, Set<BlockEntity> globalEntities, E blockEntity) {
		final BlockEntityRenderer<E> blockEntityRenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity);

		if (blockEntityRenderer != null) {
			chunkEntities.add(blockEntity);

			if (blockEntityRenderer.shouldRenderOffScreen(blockEntity)) {
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

			if (renderSector != null) {
				renderSector = renderSector.release(origin);
			}
		}
	}

	private void releaseDrawables() {
		solidDrawable.releaseFromRegion();
		solidDrawable = DrawableRegion.EMPTY_DRAWABLE;

		translucentDrawable.releaseFromRegion();
		translucentDrawable = DrawableRegion.EMPTY_DRAWABLE;
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
		final PackedInputRegion region = PackedInputRegion.claim(worldRenderState.getWorld(), origin);

		// Idle region is signal to reschedule
		// If region is something other than idle, we are already in the queue
		// and we only need to update the input protoRegion (which we do here.)
		if (inputState.getAndSet(region) == SignalInputRegion.IDLE) {
			TerrainExecutor.INSTANCE.execute(this);
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
			TerrainExecutor.INSTANCE.execute(this);
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

	private void notifyOcclusionChange() {
		cameraVisibility.notifyOfOcclusionChange();
		shadowVisibility.notifyOfOcclusionChange();
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
			chunkData.setOcclusionResult(RegionOcclusionCalculator.EMPTY_OCCLUSION_RESULT);

			// don't rebuild occlusion if occlusion did not change
			final RegionBuildState oldBuildData = buildState.getAndSet(chunkData);

			if (oldBuildData == RegionBuildState.UNBUILT || !Arrays.equals(chunkData.occlusionResult.occlusionData(), oldBuildData.occlusionResult.occlusionData())) {
				// Even if empty the chunk may still be needed for visibility search to progress
				notifyOcclusionChange();
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
		// Note that we don't do anything here to restart iteration or invalidate occlusion state
		// because regions shouldn't be scheduled if neighbors aren't available and this should never happen.
		if (!isNearOrHasLoadedNeighbors()) {
			// Causes region to be rescheduled when it is next encountered
			markForBuild(false);
			protoRegion.release();
			assert false : "Region without loaded neighbors encountered in off-thread execution.";
			return;
		}

		if (protoRegion == SignalInputRegion.RESORT_ONLY) {
			final RegionBuildState regionData = buildState.get();
			final int[] state = regionData.translucentState;

			if (state != null) {
				final VertexCollectorList collectors = context.collectors;
				final OldVertexCollector collector = collectors.get(TerrainRenderStates.TRANSLUCENT_TERRAIN);
				collector.loadState(state);

				if (collector.sortTerrainQuads(worldRenderState.sectorManager.cameraPos(), renderSector)) {
					regionData.translucentState = collector.saveState(state);

					if (runningState.get() != SignalInputRegion.INVALID) {
						final UploadableRegion upload = collectors.toUploadableChunk(true, origin, worldRenderState);

						if (upload != UploadableRegion.EMPTY_UPLOADABLE) {
							renderRegionBuilder.scheduleUpload(() -> {
								if (ChunkRebuildCounters.ENABLED) {
									ChunkRebuildCounters.startUpload();
								}

								translucentDrawable.releaseFromRegion();
								translucentDrawable = upload.produceDrawable();
								worldRenderState.invalidateDrawLists();

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
			final RegionBuildState newBuildState = captureAndSetBuildState(context, origin.isNear());
			context.encodingContext.updateSector(renderSector, origin);
			final VertexCollectorList collectors = context.collectors;

			if (runningState.get() == SignalInputRegion.INVALID) {
				collectors.clear();
				protoRegion.release();
				return;
			}

			buildTerrain(context, newBuildState);

			if (runningState.get() != SignalInputRegion.INVALID) {
				final UploadableRegion solidUpload = collectors.toUploadableChunk(false, origin, worldRenderState);
				final UploadableRegion translucentUpload = collectors.toUploadableChunk(true, origin, worldRenderState);

				renderRegionBuilder.scheduleUpload(() -> {
					if (ChunkRebuildCounters.ENABLED) {
						ChunkRebuildCounters.startUpload();
					}

					releaseDrawables();
					solidDrawable = solidUpload.produceDrawable();
					translucentDrawable = translucentUpload.produceDrawable();
					animationBits.clear();
					animationBits.or(context.animationBits);
					worldRenderState.invalidateDrawLists();

					if (ChunkRebuildCounters.ENABLED) {
						ChunkRebuildCounters.completeUpload();
					}
				});
			}

			collectors.clear();
			protoRegion.release();
		}
	}

	private RegionBuildState captureAndSetBuildState(TerrainRenderContext context, boolean isNear) {
		final RegionBuildState newBuildState = new RegionBuildState();
		newBuildState.setOcclusionResult(context.region.occlusion.build(isNear));
		handleBlockEntities(newBuildState, context);

		// don't rebuild occlusion if occlusion did not change
		final RegionBuildState oldBuildState = buildState.getAndSet(newBuildState);

		assert renderSector == null || oldBuildState != RegionBuildState.UNBUILT;

		if (renderSector == null) {
			renderSector = worldRenderState.sectorManager.findSector(origin);
		}

		if (oldBuildState == RegionBuildState.UNBUILT || !Arrays.equals(newBuildState.occlusionResult.occlusionData(), oldBuildState.occlusionResult.occlusionData())) {
			notifyOcclusionChange();
		}

		return newBuildState;
	}

	private void buildTerrain(TerrainRenderContext context, RegionBuildState buildState) {
		if (ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.startChunk();
		}

		final VertexCollectorList collectors = context.collectors;

		final BlockPos.MutableBlockPos searchPos = context.searchPos;
		final int xOrigin = origin.getX();
		final int yOrigin = origin.getY();
		final int zOrigin = origin.getZ();

		final InputRegion region = context.region;
		final PoseStack matrixStack = new PoseStack();
		final PoseStack.Pose entry = matrixStack.last();
		final Matrix4f modelMatrix = entry.pose();
		final Matrix3f normalMatrix = entry.normal();

		final BlockRenderDispatcher blockRenderManager = Minecraft.getInstance().getBlockRenderer();
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
				final boolean hasBlock = blockState.getRenderShape() != RenderShape.INVISIBLE;

				if (hasFluid || hasBlock) {
					// Vanilla does a push/pop for each block but that creates needless allocation spam.
					modelMatrix.setIdentity();
					modelMatrix.multiplyWithTranslation(x, y, z);

					normalMatrix.setIdentity();

					if (hasFluid) {
						context.renderFluid(blockState, searchPos, false, FluidModel.get(fluidState.getType()), matrixStack);
					}

					if (hasBlock) {
						if (blockState.getBlock().getOffsetType() != Block.OffsetType.NONE) {
							final Vec3 vec3d = blockState.getOffset(region, searchPos);

							if (vec3d != Vec3.ZERO) {
								modelMatrix.multiplyWithTranslation((float) vec3d.x, (float) vec3d.y, (float) vec3d.z);
							}
						}

						final BakedModel model = blockRenderManager.getBlockModel(blockState);
						context.renderBlock(blockState, searchPos, model.useAmbientOcclusion(), (BlockModel) model, matrixStack);
					}
				}
			}
		}

		buildState.prepareTranslucentIfNeeded(worldRenderState.sectorManager.cameraPos(), renderSector, collectors);

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

		worldRenderState.cwr.updateNoCullingBlockEntities(removedBlockEntities, addedBlockEntities);
	}

	public void rebuildOnMainThread() {
		final PackedInputRegion inputRegion = PackedInputRegion.claim(worldRenderState.getWorld(), origin);

		if (inputRegion == SignalInputRegion.EMPTY) {
			final RegionBuildState newBuildState = new RegionBuildState();
			newBuildState.setOcclusionResult(RegionOcclusionCalculator.EMPTY_OCCLUSION_RESULT);

			// don't rebuild occlusion if occlusion did not change
			final RegionBuildState oldBuildState = buildState.getAndSet(newBuildState);

			if (oldBuildState == RegionBuildState.UNBUILT || !Arrays.equals(newBuildState.occlusionResult.occlusionData(), oldBuildState.occlusionResult.occlusionData())) {
				// Even if empty the chunk may still be needed for visibility search to progress
				notifyOcclusionChange();
			}
		} else {
			final TerrainRenderContext context = renderRegionBuilder.mainThreadContext.prepareForRegion(inputRegion);
			final RegionBuildState newBuildState = captureAndSetBuildState(context, origin.isNear());
			context.encodingContext.updateSector(renderSector, origin);

			buildTerrain(context, newBuildState);

			if (ChunkRebuildCounters.ENABLED) {
				ChunkRebuildCounters.startUpload();
			}

			final VertexCollectorList collectors = context.collectors;
			final UploadableRegion solidUpload = collectors.toUploadableChunk(false, origin, worldRenderState);
			final UploadableRegion translucentUpload = collectors.toUploadableChunk(true, origin, worldRenderState);

			releaseDrawables();
			solidDrawable = solidUpload.produceDrawable();
			translucentDrawable = translucentUpload.produceDrawable();
			animationBits.clear();
			animationBits.or(context.animationBits);

			worldRenderState.invalidateDrawLists();

			if (ChunkRebuildCounters.ENABLED) {
				ChunkRebuildCounters.completeUpload();
			}

			collectors.clear();
			inputRegion.release();
		}

		markBuilt();
	}

	public RegionBuildState getBuildState() {
		return buildState.get();
	}

	public DrawableRegion translucentDrawable() {
		return translucentDrawable;
	}

	public DrawableRegion solidDrawable() {
		return solidDrawable;
	}

	public boolean isClosed() {
		return isClosed;
	}

	public void enqueueAsUnvistedCameraNeighbor(int entryFaceFlags, int fromSquaredDistance) {
		assert !Pipeline.advancedTerrainCulling();
		final var origin = this.origin;

		if ((origin.squaredCameraChunkDistance() >= fromSquaredDistance && (origin.visibleFaceFlags() & entryFaceFlags) != 0) || origin.isNear()) {
			cameraVisibility.addIfValid(entryFaceFlags);
		}
	}
}
