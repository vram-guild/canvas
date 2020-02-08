package grondag.canvas.chunk;

import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.client.render.chunk.ChunkBuilder.ChunkData;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import grondag.canvas.mixinterface.ChunkInfoExt;
import grondag.canvas.mixinterface.WorldRendererExt;
import grondag.canvas.perf.MicroTimer;

public class TerrainRenderer {
	public static final MicroTimer innerTimer = new MicroTimer("inner", -1);

	private final WorldRendererExt wr;

	public TerrainRenderer(WorldRendererExt wr) {
		this.wr = wr;
	}

	private final IntComparator comparator = new IntComparator() {
		@Override
		public int compare(int a, int b) {
			return Integer.compare(searchDist.getInt(a), searchDist.getInt(b));
		}
	};

	private final Swapper swapper = new Swapper() {
		@Override
		public void swap(int a, int b) {
			final int distSwap = searchDist.getInt(a);
			searchDist.set(a, searchDist.getInt(b));
			searchDist.set(b, distSwap);

			final ChunkInfoExt chunkSwap = searchList.get(a);
			searchList.set(a, searchList.get(b));
			searchList.set(b, chunkSwap);
		}
	};

	private final ObjectArrayFIFOQueue<ChunkInfoExt> searchQueue = new ObjectArrayFIFOQueue<>();
	private final ObjectArrayList<ChunkInfoExt> searchList = new ObjectArrayList<>();
	private final IntArrayList searchDist = new IntArrayList();

	@SuppressWarnings("unchecked")
	public void setupTerrain(Camera camera, Frustum frustum, boolean capturedFrustum, int frameCounter, boolean isSpectator) {
		final WorldRendererExt wr = this.wr;
		final MinecraftClient mc = wr.canvas_mc();
		final int renderDistance = wr.canvas_renderDistance();

		if (mc.options.viewDistance != renderDistance) {
			wr.canvas_reload();
		}

		final ClientWorld world = wr.canvas_world();
		final ChunkBuilder chunkBuilder = wr.canvas_chunkBuilder();
		@SuppressWarnings("rawtypes")
		final ObjectList visibleChunks = wr.canvas_visibleChunks();

		world.getProfiler().push("camera");
		final Vec3d cameraPos = camera.getPos();
		setupCamera(wr, mc, chunkBuilder, cameraPos);

		world.getProfiler().swap("cull");
		mc.getProfiler().swap("culling");
		final BlockPos cameraBlockPos = camera.getBlockPos();
		final ChunkBuilder.BuiltChunk cameraChunk = wr.canvas_getRenderedChunk(cameraBlockPos);
		final BlockPos cameraChunkOrigin = new BlockPos(MathHelper.floor(cameraPos.x / 16.0D) * 16, MathHelper.floor(cameraPos.y / 16.0D) * 16, MathHelper.floor(cameraPos.z / 16.0D) * 16);
		boolean needsTerrainUpdate = wr.canvas_checkNeedsTerrainUpdate(cameraPos, camera.getPitch(), camera.getYaw());

		mc.getProfiler().swap("update");

		if (!capturedFrustum && needsTerrainUpdate) {
			needsTerrainUpdate = wr.canvas_setNeedsTerrainUpdate(false);
			visibleChunks.clear();
			final ObjectArrayFIFOQueue<ChunkInfoExt> searchQueue = this.searchQueue;
			Entity.setRenderDistanceMultiplier(MathHelper.clamp(mc.options.viewDistance / 8.0D, 1.0D, 2.5D));
			boolean chunkCullingEnabled = mc.chunkCullingEnabled;

			if (cameraChunk != null) {
				// start from camera chunk if camera is in the world
				final ChunkInfoExt chunkInfo = ChunkInfoFactory.INSTANCE.get(wr, cameraChunk, (Direction)null, 0);
				final Set<Direction> set = getOpenChunkFaces(world, cameraBlockPos);

				if (set.size() == 1) {
					final Vector3f vector3f = camera.getHorizontalPlane();
					final Direction direction = Direction.getFacing(vector3f.getX(), vector3f.getY(), vector3f.getZ()).getOpposite();
					set.remove(direction);
				}

				if (set.isEmpty() && !isSpectator) {
					visibleChunks.add(chunkInfo);
				} else {
					if (isSpectator && world.getBlockState(cameraBlockPos).isFullOpaque(world, cameraBlockPos)) {
						chunkCullingEnabled = false;
					}

					cameraChunk.setRebuildFrame(frameCounter);
					searchQueue.enqueue(chunkInfo);
				}
			} else {
				// start from top or bottom of world if camera is outside of world
				startSearchFromOutsideWorld(cameraBlockPos, cameraPos, renderDistance, frustum, frameCounter);
			}

			mc.getProfiler().push("iteration");

			innerTimer.start();

			while(!searchQueue.isEmpty()) {
				final ChunkInfoExt chunkInfo = searchQueue.dequeue();
				final BuiltChunk builtChunk = chunkInfo.canvas_chunk();
				final ChunkData chunkData = builtChunk.getData();
				final Direction currentChunkEntryFace = chunkInfo.canvas_entryFace();
				visibleChunks.add(chunkInfo);

				for(final Direction face : DIRECTIONS) {
					final BuiltChunk adjacentChunk = getAdjacentChunk(wr, cameraChunkOrigin, builtChunk, face);

					if (
							// don't visit if chunk culling is enabled and face is backwards-pointing for current chunk
							(!chunkCullingEnabled || !chunkInfo.canvas_isBacktrack(face.getOpposite()))

							// don't visit if chunk culling is enabled and not visible through our chunk
							&& (!chunkCullingEnabled
									|| currentChunkEntryFace == null
									|| chunkData.isVisibleThrough(currentChunkEntryFace.getOpposite(), face))

							// don't visit if adjacent chunk is null
							&& adjacentChunk != null

							// don't visit if chunk is outside near distance and doesn't have all 4 neighbors loaded
							&& adjacentChunk.shouldBuild()

							// don't visit if already visited this frame
							&& adjacentChunk.setRebuildFrame(frameCounter)

							// don't visit if not in frustum
							&& frustum.isVisible(adjacentChunk.boundingBox)) {

						final ChunkInfoExt adjacentChunkInfo = ChunkInfoFactory.INSTANCE.get(wr, adjacentChunk, face, chunkInfo.canvas_propagationLevel() + 1);

						// backtrack faces for adjacent will those of current chunk plus the face from which we are visiting
						// this is confusing, because it's actually the opposite of the backwards face because Mojang flips faces when checking
						// FIX: this seems likely to be the source of over-optimistic occlusion bug seen earlier
						adjacentChunkInfo.canvas_updateBacktrackState(chunkInfo.canvas_backtrackState(), face);
						searchQueue.enqueue(adjacentChunkInfo);
					}
				}
			}

			mc.getProfiler().pop();
		}

		innerTimer.stop();

		mc.getProfiler().swap("rebuildNear");
		final Set<ChunkBuilder.BuiltChunk> oldChunksToRebuild  = wr.canvas_chunkToRebuild();
		final Set<ChunkBuilder.BuiltChunk> chunksToRebuild = wr.canvas_newChunkToRebuild();
		@SuppressWarnings("rawtypes")
		final ObjectListIterator visibles = visibleChunks.iterator();

		while (visibles.hasNext()) {
			final ChunkBuilder.BuiltChunk builtChunk = ((ChunkInfoExt) visibles.next()).canvas_chunk();

			if (builtChunk.needsRebuild() || oldChunksToRebuild.contains(builtChunk)) {
				needsTerrainUpdate = wr.canvas_setNeedsTerrainUpdate(true);
				final boolean isNear = squaredDistance(builtChunk.getOrigin(), cameraBlockPos) < 768.0D;

				if (!builtChunk.needsImportantRebuild() && !isNear) {
					chunksToRebuild.add(builtChunk);
				} else {
					mc.getProfiler().push("build near");
					chunkBuilder.rebuild(builtChunk);
					builtChunk.cancelRebuild();
					mc.getProfiler().pop();
				}
			}
		}

		chunksToRebuild.addAll(oldChunksToRebuild);
		mc.getProfiler().pop();
	}

	private final void startSearchFromOutsideWorld(BlockPos cameraBlockPos, Vec3d cameraPos, int renderDistance, Frustum frustum, int frameCounter) {
		final int yLevel = cameraBlockPos.getY() > 0 ? 248 : 8;
		final int xCenter = MathHelper.floor(cameraPos.x / 16.0D) * 16;
		final int zCenter = MathHelper.floor(cameraPos.z / 16.0D) * 16;
		final ObjectArrayList<ChunkInfoExt> searchList = this.searchList;
		final IntArrayList searchDist = this.searchDist;
		searchList.clear();
		searchDist.clear();

		for(int zOffset = -renderDistance; zOffset <= renderDistance; ++zOffset) {
			for(int xOffset = -renderDistance; xOffset <= renderDistance; ++xOffset) {
				final ChunkBuilder.BuiltChunk builtChunk = wr.canvas_getRenderedChunk(new BlockPos(xCenter + (xOffset << 4) + 8, yLevel, zCenter + (zOffset << 4) + 8));

				if (builtChunk != null && frustum.isVisible(builtChunk.boundingBox)) {
					builtChunk.setRebuildFrame(frameCounter);
					searchList.add(ChunkInfoFactory.INSTANCE.get(wr, builtChunk, (Direction)null, 0));
					searchDist.add(squaredDistance(builtChunk.getOrigin(), cameraBlockPos));
				}
			}
		}

		final int limit = searchList.size();
		it.unimi.dsi.fastutil.Arrays.quickSort(0, limit, comparator, swapper);

		for(int i = 0; i < limit; i++) {
			searchQueue.enqueue(searchList.get(i));
		}
	}

	private static int squaredDistance(BlockPos chunkOrigin, BlockPos cameraBlockPos) {
		final int dx = chunkOrigin.getX() + 8 - cameraBlockPos.getX();
		final int dy = chunkOrigin.getY() + 8 - cameraBlockPos.getY();
		final int dz = chunkOrigin.getZ() + 8 - cameraBlockPos.getZ();
		return dx * dx + dy * dy + dz * dz;

	}
	@Nullable
	private static ChunkBuilder.BuiltChunk getAdjacentChunk(WorldRendererExt wr, BlockPos blockPos, ChunkBuilder.BuiltChunk builtChunk, Direction direction) {
		final BlockPos blockPos2 = builtChunk.getNeighborPosition(direction);
		if (MathHelper.abs(blockPos.getX() - blockPos2.getX()) > wr.canvas_renderDistance() * 16) {
			return null;
		} else if (blockPos2.getY() >= 0 && blockPos2.getY() < 256) {
			return MathHelper.abs(blockPos.getZ() - blockPos2.getZ()) > wr.canvas_renderDistance() * 16 ? null : wr.canvas_getRenderedChunk(blockPos2);
		} else {
			return null;
		}
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

	private void setupCamera(WorldRendererExt wr, MinecraftClient mc, ChunkBuilder chunkBuilder, Vec3d cameraPos) {
		final BuiltChunkStorage chunks = wr.canvas_chunks();
		final double dx = mc.player.getX() - wr.canvas_lastCameraChunkUpdateX();
		final double dy = mc.player.getY() - wr.canvas_lastCameraChunkUpdateY();
		final double dz = mc.player.getZ() - wr.canvas_lastCameraChunkUpdateZ();
		final int cameraChunkX = wr.canvas_camereChunkX();
		final int cameraChunkY = wr.canvas_camereChunkY();
		final int cameraChunkZ = wr.canvas_camereChunkZ();

		if (cameraChunkX != mc.player.chunkX || cameraChunkY != mc.player.chunkY || cameraChunkZ != mc.player.chunkZ || dx * dx + dy * dy + dz * dz > 16.0D) {
			wr.canvas_updateLastCameraChunkPositions();
			chunks.updateCameraPosition(mc.player.getX(), mc.player.getZ());
		}

		chunkBuilder.setCameraPosition(cameraPos);
	}

	private static final Direction[] DIRECTIONS = Direction.values();
}
