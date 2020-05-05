package grondag.canvas.chunk;

import static grondag.canvas.chunk.occlusion.Constants.DOWN;
import static grondag.canvas.chunk.occlusion.Constants.EAST;
import static grondag.canvas.chunk.occlusion.Constants.NORTH;
import static grondag.canvas.chunk.occlusion.Constants.SOUTH;
import static grondag.canvas.chunk.occlusion.Constants.UP;
import static grondag.canvas.chunk.occlusion.Constants.WEST;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.buffer.packing.VertexCollectorImpl;
import grondag.canvas.buffer.packing.VertexCollectorList;
import grondag.canvas.chunk.occlusion.TerrainOccluder;
import grondag.canvas.chunk.occlusion.region.OcclusionRegion;
import grondag.canvas.chunk.occlusion.region.PackedBox;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.material.MaterialState;
import grondag.canvas.perf.ChunkRebuildCounters;
import grondag.canvas.render.CanvasFrustum;
import grondag.canvas.render.CanvasWorldRenderer;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;

@Environment(EnvType.CLIENT)
public class BuiltRenderRegion {
	private final RenderRegionBuilder renderRegionBuilder;
	private final RenderRegionStorage storage;
	private final AtomicReference<RegionData> renderData;
	private final AtomicReference<RegionData> buildData;
	private final ObjectOpenHashSet<BlockEntity> localNoCullingBlockEntities = new ObjectOpenHashSet<>();
	private boolean needsRebuild;
	private final BlockPos.Mutable origin;
	private boolean needsImportantRebuild;
	private volatile RegionBuildState buildState = new RegionBuildState();
	private final Consumer<TerrainRenderContext> buildTask = this::rebuildOnWorkerThread;
	private final RegionChunkReference chunkReference;
	private final boolean isBottom;
	/**
	 * Index of this instance in region array.
	 */
	private int regionIndex;
	private final int[] neighborIndices = new int[6];
	private DrawableChunk translucentDrawable;
	private DrawableChunk solidDrawable;
	private int frustumVersion;
	private boolean frustumResult;
	private int lastSeenFrameIndex;

	int squaredCameraDistance;
	public int occlusionRange;
	public int occluderVersion;
	public boolean occluderResult;

	private boolean isInsideRenderDistance;

	public float cameraRelativeCenterX;
	public float cameraRelativeCenterY;
	public float cameraRelativeCenterZ;

	/**
	 * Holds value with face flags set for faces in the region that
	 * are back-facing for at least 64 blocks away from camera.
	 */
	public int backfaceCullFlags;

	public BuiltRenderRegion(RenderRegionBuilder renderRegionBuilder, RenderRegionStorage storage, RegionChunkReference chunkReference, boolean bottom) {
		this.renderRegionBuilder = renderRegionBuilder;
		this.storage = storage;
		this.chunkReference = chunkReference;
		isBottom = bottom;
		buildData = new AtomicReference<>(RegionData.EMPTY);
		renderData = new AtomicReference<>(RegionData.EMPTY);
		needsRebuild = true;
		origin = new BlockPos.Mutable(-1, -1, -1);
	}

	/**
	 * Assumes camera distance update has already happened.
	 *
	 * NB: tried a crude hierarchical scheme of checking chunk columns first
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

	/**
	 * @return True if nearby.  If not nearby and not outside view distance true if neighbors are loaded.
	 */
	public boolean shouldBuild() {
		return squaredCameraDistance <= 576
				|| (isInsideRenderDistance && chunkReference.areCornersLoaded());
	}

	public void setOrigin(int x, int y, int z,  int myIndex) {
		if (isBottom) {
			chunkReference.setOrigin(x, z);
		}

		regionIndex = myIndex;
		if (x != origin.getX() || y != origin.getY() || z != origin.getZ()) {
			clear();
			origin.set(x, y, z);

			final int[] neighborIndices = this.neighborIndices;

			for(int i = 0; i < 6; ++i) {
				final Direction face = ModelHelper.faceFromIndex(i);

				neighborIndices[i] = storage.getRegionIndexFromBlockPos(x + face.getOffsetX() * 16, y + face.getOffsetY() * 16, z + face.getOffsetZ() * 16);
			}
		}
	}

	void updateCameraDistance(double cameraX, double cameraY, double cameraZ, int maxRenderDistance) {
		final BlockPos.Mutable origin = this.origin;
		final float dx = (float) (origin.getX() + 8 - cameraX);
		final float dy = (float) (origin.getY() + 8 - cameraY);
		final float dz = (float) (origin.getZ() + 8 - cameraZ);
		cameraRelativeCenterX = dx;
		cameraRelativeCenterY = dy;
		cameraRelativeCenterZ = dz;

		// PERF: consider moving below to setOrigin
		final int idx = Math.round(dx);
		final int idy = Math.round(dy);
		final int idz = Math.round(dz);


		if (Configurator.terrainBackfaceCulling) {
			int backfaceCullFlags = 0b111111;

			if (idy < 64) {
				backfaceCullFlags &= ~UP;
			}

			if (idy > -64) {
				backfaceCullFlags &= ~DOWN;
			}

			if (idx < 64) {
				backfaceCullFlags &= ~EAST;
			}

			if (idx > -64) {
				backfaceCullFlags &= ~WEST;
			}

			if (idz < 64) {
				backfaceCullFlags &= ~SOUTH;
			}

			if (idz > -64) {
				backfaceCullFlags &= ~NORTH;
			}

			if (!needsRebuild && backfaceCullFlags != this.backfaceCullFlags) {
				final int oldFlags = renderData.get().backfaceCullFlags();

				// if old flags cull more than current, need to rebuild
				if  (oldFlags != backfaceCullFlags &&  (oldFlags | backfaceCullFlags) != backfaceCullFlags) {
					needsRebuild = true;
				}
			}

			this.backfaceCullFlags = backfaceCullFlags;
		} else {
			backfaceCullFlags = 0;
		}


		final int horizontalDistance = idx * idx + idz * idz;
		isInsideRenderDistance = horizontalDistance <= maxRenderDistance;

		final int squaredCameraDistance = horizontalDistance + idy * idy;
		occlusionRange = PackedBox.rangeFromSquareBlockDist(squaredCameraDistance);
		this.squaredCameraDistance = squaredCameraDistance;
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

	private void clear() {
		assert RenderSystem.isOnRenderThread();

		cancel();
		buildData.set(RegionData.EMPTY);
		renderData.set(RegionData.EMPTY);
		needsRebuild = true;

		if (solidDrawable != null) {
			solidDrawable.clear();
			solidDrawable = null;
		}

		if (translucentDrawable != null) {
			translucentDrawable.clear();
			translucentDrawable = null;
		}
	}

	public void delete() {
		clear();
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
		final ProtoRenderRegion region = ProtoRenderRegion.claim(renderRegionBuilder.world, origin, backfaceCullFlags);

		// null region is signal to reschedule
		if(buildState.protoRegion.getAndSet(region) == ProtoRenderRegion.IDLE) {
			renderRegionBuilder.executor.execute(buildTask, squaredCameraDistance);
		}
	}

	public boolean scheduleSort() {
		final RegionData regionData = buildData.get();

		if (regionData.translucentState == null) {
			return false;
		} else {
			if (buildState.protoRegion.compareAndSet(ProtoRenderRegion.IDLE,  ProtoRenderRegion.RESORT_ONLY)) {
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
			chunkData.complete(OcclusionRegion.EMPTY_CULL_DATA, 0);

			final int[] oldData = buildData.getAndSet(chunkData).occlusionData;

			if (oldData != null && oldData != OcclusionRegion.EMPTY_CULL_DATA) {
				TerrainOccluder.invalidate(occluderVersion);
			} else  {
				// fact that chunk is empty may still be needed for visibility search to progress
				CanvasWorldRenderer.instance().forceVisibilityUpdate();
			}

			renderData.set(chunkData);
			return;
		}

		// check loaded neighbors and camera distance, abort rebuild and restore needsRebuild if out of view/not ready
		if (!shouldBuild()) {
			markForBuild(false);
			region.release();
			return;
		}

		if(region == ProtoRenderRegion.RESORT_ONLY) {
			final RegionData regionData = buildData.get();
			final int[] state = regionData.translucentState;

			if (state != null) {
				final Vec3d cameraPos = renderRegionBuilder.getCameraPosition();
				final VertexCollectorList collectors = context.collectors;
				final MaterialState translucentState = MaterialState.get(MaterialContext.TERRAIN, RenderLayer.getTranslucent());
				final VertexCollectorImpl collector = collectors.get(translucentState);

				collector.loadState(state);
				collector.sortQuads((float)cameraPos.x - origin.getX(), (float)cameraPos.y - origin.getY(), (float)cameraPos.z - origin.getZ());
				regionData.translucentState = collector.saveState(state);

				if(runningState.protoRegion.get() != ProtoRenderRegion.INVALID) {
					final UploadableChunk upload = collectors.packUploadTranslucent(translucentState);

					if (upload != null) {
						renderRegionBuilder.scheduleUpload(() -> {
							if (ChunkRebuildCounters.ENABLED) {
								ChunkRebuildCounters.startUpload();
							}

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
			final RegionData chunkData = buildRegionData(context);

			final int[] oldData = buildData.getAndSet(chunkData).occlusionData;

			if (oldData != null && !Arrays.equals(oldData, chunkData.occlusionData)) {
				TerrainOccluder.invalidate(occluderVersion);
			}

			final VertexCollectorList collectors = context.collectors;

			if (runningState.protoRegion.get() == ProtoRenderRegion.INVALID) {
				collectors.clear();
				region.release();
				return;
			}

			buildTerrain(context, chunkData);

			if(runningState.protoRegion.get() != ProtoRenderRegion.INVALID) {
				final UploadableChunk solidUpload = collectors.packUploadSolid();
				final UploadableChunk translucentUpload = collectors.packUploadTranslucent(MaterialState.get(MaterialContext.TERRAIN, RenderLayer.getTranslucent()));

				if (solidUpload != null || translucentUpload != null) {
					renderRegionBuilder.scheduleUpload(() -> {
						if (ChunkRebuildCounters.ENABLED) {
							ChunkRebuildCounters.startUpload();
						}

						solidDrawable = solidUpload == null ? null : solidUpload.produceDrawable();
						translucentDrawable = translucentUpload == null ? null : translucentUpload.produceDrawable();
						renderData.set(chunkData);

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

	private RegionData buildRegionData(TerrainRenderContext context) {
		final RegionData regionData = new RegionData();

		regionData.complete(context.region.occlusion.build(), context.backfaceCullFlags());
		handleBlockEntities(regionData, context);
		buildData.set(regionData);
		return regionData;
	}

	private void buildTerrain(TerrainRenderContext context, RegionData regionData) {
		if(ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.startChunk();
		}

		final VertexCollectorList collectors = context.collectors;

		final BlockPos.Mutable searchPos = context.searchPos;
		final int xOrigin = origin.getX();
		final int yOrigin = origin.getY();
		final int zOrigin = origin.getZ();
		final FastRenderRegion region = context.region;
		final Vec3d cameraPos = renderRegionBuilder.getCameraPosition();
		final MatrixStack matrixStack = new MatrixStack();
		final BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
		final OcclusionRegion occlusionRegion = region.occlusion;

		for (int i = 0; i < RenderRegionAddressHelper.INTERIOR_CACHE_SIZE; i++) {
			if(occlusionRegion.shouldRender(i)) {
				final BlockState blockState = region.getLocalBlockState(i);
				final FluidState fluidState = blockState.getFluidState();
				final int x = i & 0xF;
				final int y = (i >> 4) & 0xF;
				final int z = (i >> 8) & 0xF;
				searchPos.set(xOrigin + x, yOrigin + y, zOrigin + z);

				if (!fluidState.isEmpty()) {
					final RenderLayer fluidLayer = RenderLayers.getFluidLayer(fluidState);
					final VertexCollectorImpl fluidBuffer = collectors.get(MaterialContext.TERRAIN, fluidLayer);

					blockRenderManager.renderFluid(searchPos, region, fluidBuffer, fluidState);
				}

				if (blockState.getRenderType() != BlockRenderType.INVISIBLE) {
					matrixStack.push();
					matrixStack.translate(x, y, z);

					if (blockState.getBlock().getOffsetType() != Block.OffsetType.NONE) {
						final Vec3d vec3d = blockState.getOffsetPos(region, searchPos);

						if (vec3d != Vec3d.ZERO) {
							matrixStack.translate(vec3d.x, vec3d.y, vec3d.z);
						}
					}

					context.tesselateBlock(blockState, searchPos, blockRenderManager.getModel(blockState), matrixStack);

					matrixStack.pop();
				}
			}
		}

		regionData.endBuffering((float) (cameraPos.x - xOrigin), (float) (cameraPos.y - yOrigin), (float) (cameraPos.z - zOrigin), collectors);

		if(ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.completeChunk();
		}
	}

	private void handleBlockEntities(RegionData regionData, TerrainRenderContext context) {
		final ObjectOpenHashSet<BlockEntity> nonCullBlockEntities = context.nonCullBlockEntities;
		final ObjectArrayList<BlockEntity> regionDataBlockEntities = regionData.blockEntities;

		for(final BlockEntity blockEntity : context.region.blockEntities) {
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

		renderRegionBuilder.worldRenderer.updateNoCullingBlockEntities(removedBlockEntities, addedBlockEntities);
	}

	public void rebuildOnMainThread() {
		final ProtoRenderRegion region = ProtoRenderRegion.claim(renderRegionBuilder.world, origin, backfaceCullFlags);

		if (region == ProtoRenderRegion.EMPTY) {
			final RegionData regionData = new RegionData();
			regionData.complete(OcclusionRegion.EMPTY_CULL_DATA, 0);
			final int[] oldData = buildData.getAndSet(regionData).occlusionData;

			if (oldData != null && oldData != OcclusionRegion.EMPTY_CULL_DATA) {
				TerrainOccluder.invalidate(occluderVersion);
			}

			// Note we don't force Canvas world renderer to update visibility next pass
			// because this is called from there and thus the WR can do that for us.

			return;
		}

		final TerrainRenderContext context = renderRegionBuilder.mainThreadContext.prepareRegion(region);
		final RegionData regionData = buildRegionData(context);
		final int[] oldData = buildData.getAndSet(regionData).occlusionData;

		if (oldData != null && !Arrays.equals(oldData, regionData.occlusionData)) {
			TerrainOccluder.invalidate(occluderVersion);
		}

		buildTerrain(context, regionData);

		if (ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.startUpload();
		}

		final VertexCollectorList collectors = context.collectors;
		final UploadableChunk solidUpload = collectors.packUploadSolid();
		final UploadableChunk translucentUpload = collectors.packUploadTranslucent(MaterialState.get(MaterialContext.TERRAIN, RenderLayer.getTranslucent()));
		solidDrawable = solidUpload == null ? null : solidUpload.produceDrawable();
		translucentDrawable = translucentUpload == null ? null : translucentUpload.produceDrawable();

		if (ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.completeUpload();
		}

		renderData.set(regionData);
		collectors.clear();
		region.release();
	}

	public int[] getNeighborIndices() {
		return neighborIndices;
	}

	public RegionData getBuildData() {
		return buildData.get();
	}

	public RegionData getRenderData() {
		return renderData.get();
	}

	public DrawableChunk translucentDrawable() {
		return translucentDrawable;
	}

	public DrawableChunk solidDrawable() {
		return solidDrawable;
	}

	public int squaredCameraDistance() {
		return squaredCameraDistance;
	}

	public boolean isNear() {
		return squaredCameraDistance < 768;
	}

	public int regionIndex() {
		return regionIndex;
	}

	private static int frameIndex;

	public static void advanceFrameIndex() {
		++frameIndex;
	}

	public void enqueueUnvistedNeighbors(SimpleUnorderedArrayList<BuiltRenderRegion> queue) {
		final int index = frameIndex;
		lastSeenFrameIndex = index;
		final BuiltRenderRegion regions[] = storage.regions();

		for (final int i : neighborIndices) {
			if (i != -1) {
				final BuiltRenderRegion r = regions[i];
				final int ri = r.lastSeenFrameIndex;

				if (ri != index) {
					r.lastSeenFrameIndex = index;
					queue.add(r);
				}
			}
		}
	}
}