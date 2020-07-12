package grondag.canvas.terrain;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
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

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.buffer.encoding.VertexCollectorImpl;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.material.MaterialState;
import grondag.canvas.perf.ChunkRebuildCounters;
import grondag.canvas.render.CanvasFrustum;
import grondag.canvas.render.CanvasWorldRenderer;
import grondag.canvas.shader.ShaderPass;
import grondag.canvas.terrain.occlusion.TerrainOccluder;
import grondag.canvas.terrain.occlusion.region.OcclusionRegion;
import grondag.canvas.terrain.occlusion.region.PackedBox;
import grondag.canvas.terrain.render.DrawableChunk;
import grondag.canvas.terrain.render.UploadableChunk;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;
import grondag.frex.api.fluid.FluidQuadSupplier;

@Environment(EnvType.CLIENT)
public class BuiltRenderRegion {
	private final RenderRegionBuilder renderRegionBuilder;
	private final RenderRegionStorage storage;
	private final AtomicReference<RegionData> renderData;
	private final AtomicReference<RegionData> buildData;
	private final ObjectOpenHashSet<BlockEntity> localNoCullingBlockEntities = new ObjectOpenHashSet<>();
	private boolean needsRebuild;
	private final BlockPos origin;
	private boolean needsImportantRebuild;
	private volatile RegionBuildState buildState = new RegionBuildState();
	private final Consumer<TerrainRenderContext> buildTask = this::rebuildOnWorkerThread;
	private final RegionChunkReference chunkReference;
	private final CanvasWorldRenderer cwr;
	private final boolean isBottom;
	private final boolean isTop;
	private final BuiltRenderRegion[] neighbors = new BuiltRenderRegion[6];
	private final TerrainOccluder terrainOccluder;
	private DrawableChunk translucentDrawable = DrawableChunk.EMPTY_DRAWABLE;
	private DrawableChunk solidDrawable = DrawableChunk.EMPTY_DRAWABLE;
	private int frustumVersion;
	private boolean frustumResult;
	private int lastSeenFrameIndex;
	private boolean isClosed = false;

	int squaredCameraDistance;
	public int occlusionRange;
	public int occluderVersion;
	public boolean occluderResult;

	private boolean isInsideRenderDistance;

	public float cameraRelativeCenterX;
	public float cameraRelativeCenterY;
	public float cameraRelativeCenterZ;

	public BuiltRenderRegion(CanvasWorldRenderer cwr, RegionChunkReference chunkRef, long packedPos) {
		this.cwr = cwr;
		terrainOccluder = cwr.terrainOccluder;
		renderRegionBuilder = cwr.regionBuilder();
		storage = cwr.regionStorage();
		chunkReference = chunkRef;
		chunkReference.retain(this);
		buildData = new AtomicReference<>(RegionData.EMPTY);
		renderData = new AtomicReference<>(RegionData.EMPTY);
		needsRebuild = true;
		origin = BlockPos.fromLong(packedPos);
		isBottom = origin.getY() == 0;
		isTop = origin.getY() == 240;
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

	// PERF: make this lazy?
	/**
	 * Returns true if inside retentiom distance;
	 */
	boolean updateCameraDistance() {
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

		return horizontalSquaredDistance < cwr.maxRetentionDistance();
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

	void close() {
		assert RenderSystem.isOnRenderThread();

		releaseDrawables();

		if (!isClosed) {
			isClosed = true;
			chunkReference.release(this);

			cancel();
			buildData.set(RegionData.EMPTY);
			renderData.set(RegionData.EMPTY);
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
			chunkData.complete(OcclusionRegion.EMPTY_CULL_DATA);

			final int[] oldData = buildData.getAndSet(chunkData).occlusionData;

			if (oldData != null && oldData != OcclusionRegion.EMPTY_CULL_DATA) {
				terrainOccluder.invalidate();
			}

			// Even if empty the chunk may still be needed for visibility search to progress
			cwr.forceVisibilityUpdate();

			renderData.set(chunkData);
			return;
		}

		// check loaded neighbors and camera distance, abort rebuild and restore needsRebuild if out of view/not ready
		if (!shouldBuild()) {
			markForBuild(false);
			region.release();
			cwr.forceVisibilityUpdate();
			return;
		}

		if(region == ProtoRenderRegion.RESORT_ONLY) {
			final RegionData regionData = buildData.get();
			final int[] state = regionData.translucentState;

			if (state != null) {
				final Vec3d cameraPos = cwr.cameraPos();
				final VertexCollectorList collectors = context.collectors;
				final MaterialState translucentState = MaterialState.getDefault(MaterialContext.TERRAIN, ShaderPass.TRANSLUCENT);
				final VertexCollectorImpl collector = collectors.get(translucentState);

				collector.loadState(translucentState, state);

				if (Configurator.batchedChunkRender) {
					collector.sortQuads(
							(float)cameraPos.x - TerrainModelSpace.renderCubeOrigin(origin.getX()),
							(float)cameraPos.y - TerrainModelSpace.renderCubeOrigin(origin.getY()),
							(float)cameraPos.z - TerrainModelSpace.renderCubeOrigin(origin.getZ()));
				} else {
					collector.sortQuads((float)cameraPos.x - origin.getX(), (float)cameraPos.y - origin.getY(), (float)cameraPos.z - origin.getZ());
				}

				regionData.translucentState = collector.saveState(state);

				if(runningState.protoRegion.get() != ProtoRenderRegion.INVALID) {
					final UploadableChunk upload = collectors.toUploadableChunk(MaterialContext.TERRAIN, true);

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

			final int[] oldData = buildData.getAndSet(chunkData).occlusionData;

			if (oldData != null && !Arrays.equals(oldData, chunkData.occlusionData)) {
				terrainOccluder.invalidate(occluderVersion);
			}

			cwr.forceVisibilityUpdate();

			final VertexCollectorList collectors = context.collectors;

			if (runningState.protoRegion.get() == ProtoRenderRegion.INVALID) {
				collectors.clear();
				region.release();
				return;
			}

			buildTerrain(context, chunkData);

			if(runningState.protoRegion.get() != ProtoRenderRegion.INVALID) {
				final UploadableChunk solidUpload = collectors.toUploadableChunk(MaterialContext.TERRAIN, false);
				final UploadableChunk translucentUpload = collectors.toUploadableChunk(MaterialContext.TERRAIN, true);

				if (solidUpload != UploadableChunk.EMPTY_UPLOADABLE || translucentUpload != UploadableChunk.EMPTY_UPLOADABLE) {
					renderRegionBuilder.scheduleUpload(() -> {
						if (ChunkRebuildCounters.ENABLED) {
							ChunkRebuildCounters.startUpload();
						}

						releaseDrawables();
						solidDrawable = solidUpload.produceDrawable();
						translucentDrawable = translucentUpload.produceDrawable();
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

	private RegionData buildRegionData(TerrainRenderContext context, boolean isNear) {
		final RegionData regionData = new RegionData();
		regionData.complete(context.region.occlusion.build(isNear));
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
			if(occlusionRegion.shouldRender(i)) {
				final BlockState blockState = region.getLocalBlockState(i);
				final FluidState fluidState = blockState.getFluidState();
				final int x = i & 0xF;
				final int y = (i >> 4) & 0xF;
				final int z = (i >> 8) & 0xF;
				searchPos.set(xOrigin + x, yOrigin + y, zOrigin + z);

				if (!fluidState.isEmpty()) {
					matrixStack.push();
					matrixStack.translate(x + xModelOffset, y + yModelOffset, z + zModelOffset);

					context.tesselateBlock(blockState, searchPos, false, FluidQuadSupplier.get(fluidState.getFluid()), matrixStack);

					matrixStack.pop();

					//					final CompositeMaterial fluidLayer = StandardMaterials.get(RenderLayers.getFluidLayer(fluidState));
					//					// FEAT: explicit fluid materials/models, make this lookup suck less
					//					final VertexCollectorImpl fluidBuffer = collectors.get(MaterialState.getDefault(MaterialContext.TERRAIN, fluidLayer.isTranslucent ? ShaderPass.TRANSLUCENT : ShaderPass.SOLID));
					//
					//					blockRenderManager.renderFluid(searchPos, region, fluidBuffer, fluidState);
				}

				if (blockState.getRenderType() != BlockRenderType.INVISIBLE) {
					matrixStack.push();
					matrixStack.translate(x + xModelOffset, y + yModelOffset, z + zModelOffset);

					if (blockState.getBlock().getOffsetType() != Block.OffsetType.NONE) {
						final Vec3d vec3d = blockState.getModelOffset(region, searchPos);

						if (vec3d != Vec3d.ZERO) {
							matrixStack.translate(vec3d.x, vec3d.y, vec3d.z);
						}
					}

					final BakedModel model = blockRenderManager.getModel(blockState);
					context.tesselateBlock(blockState, searchPos, model.useAmbientOcclusion(), (FabricBakedModel) model, matrixStack);

					matrixStack.pop();
				}
			}
		}

		regionData.endBuffering((float) (cameraPos.x - xOrigin + xModelOffset), (float) (cameraPos.y - yOrigin + yModelOffset), (float) (cameraPos.z - zOrigin + zModelOffset), collectors);

		if(ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.completeChunk();
		}
	}

	private void handleBlockEntities(RegionData regionData, TerrainRenderContext context) {
		final ObjectOpenHashSet<BlockEntity> nonCullBlockEntities = context.nonCullBlockEntities;
		final ObjectArrayList<BlockEntity> regionDataBlockEntities = regionData.blockEntities;

		// PERF: benchmark vs list, empty indicator, or some other structure
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

		cwr.updateNoCullingBlockEntities(removedBlockEntities, addedBlockEntities);
	}

	public void rebuildOnMainThread() {
		final ProtoRenderRegion region = ProtoRenderRegion.claim(cwr.getWorld(), origin);

		if (region == ProtoRenderRegion.EMPTY) {
			final RegionData regionData = new RegionData();
			regionData.complete(OcclusionRegion.EMPTY_CULL_DATA);
			final int[] oldData = buildData.getAndSet(regionData).occlusionData;

			if (oldData != null && oldData != OcclusionRegion.EMPTY_CULL_DATA) {
				terrainOccluder.invalidate(occluderVersion);
			}

			renderData.set(regionData);

			// Even if empty the chunk may still be needed for visibility search to progress
			cwr.forceVisibilityUpdate();

			return;
		}

		final TerrainRenderContext context = renderRegionBuilder.mainThreadContext.prepareRegion(region);
		final RegionData regionData = buildRegionData(context, isNear());
		final int[] oldData = buildData.getAndSet(regionData).occlusionData;

		if (oldData != null && !Arrays.equals(oldData, regionData.occlusionData)) {
			terrainOccluder.invalidate(occluderVersion);
		}

		cwr.forceVisibilityUpdate();

		buildTerrain(context, regionData);

		if (ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.startUpload();
		}

		final VertexCollectorList collectors = context.collectors;
		final UploadableChunk solidUpload = collectors.toUploadableChunk(MaterialContext.TERRAIN, false);
		final UploadableChunk translucentUpload = collectors.toUploadableChunk(MaterialContext.TERRAIN, true);

		releaseDrawables();
		solidDrawable = solidUpload.produceDrawable();
		translucentDrawable = translucentUpload.produceDrawable();

		if (ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.completeUpload();
		}

		renderData.set(regionData);
		collectors.clear();
		region.release();
	}

	public BuiltRenderRegion getNeighbor(int faceIndex) {
		BuiltRenderRegion region = neighbors[faceIndex];

		if (region == null || region.isClosed) {
			final Direction face = ModelHelper.faceFromIndex(faceIndex);
			region = storage.getOrCreateRegion(origin.getX() + face.getOffsetX() * 16, origin.getY() + face.getOffsetY() * 16, origin.getZ() + face.getOffsetZ() * 16);
			neighbors[faceIndex] = region;
		}

		return region;
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

	private static int frameIndex;

	public static void advanceFrameIndex() {
		++frameIndex;
	}

	public void enqueueUnvistedNeighbors(SimpleUnorderedArrayList<BuiltRenderRegion> queue) {
		final int index = frameIndex;
		lastSeenFrameIndex = index;

		enqueNeighbor(index, getNeighbor(EAST_INDEX), queue);
		enqueNeighbor(index, getNeighbor(WEST_INDEX), queue);
		enqueNeighbor(index, getNeighbor(NORTH_INDEX), queue);
		enqueNeighbor(index, getNeighbor(SOUTH_INDEX), queue);

		if (!isTop) {
			enqueNeighbor(index, getNeighbor(UP_INDEX), queue);
		}

		if (!isBottom) {
			enqueNeighbor(index, getNeighbor(DOWN_INDEX), queue);
		}
	}

	private void enqueNeighbor(int index, BuiltRenderRegion r, SimpleUnorderedArrayList<BuiltRenderRegion> queue) {
		if (r.lastSeenFrameIndex != index) {
			r.lastSeenFrameIndex = index;
			queue.add(r);
		}
	}

	final static int NORTH_INDEX = ModelHelper.toFaceIndex(Direction.NORTH);
	final static int SOUTH_INDEX = ModelHelper.toFaceIndex(Direction.SOUTH);
	final static int EAST_INDEX = ModelHelper.toFaceIndex(Direction.EAST);
	final static int WEST_INDEX = ModelHelper.toFaceIndex(Direction.WEST);
	final static int UP_INDEX = ModelHelper.toFaceIndex(Direction.UP);
	final static int DOWN_INDEX = ModelHelper.toFaceIndex(Direction.DOWN);
}