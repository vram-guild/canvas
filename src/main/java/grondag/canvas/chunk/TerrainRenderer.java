package grondag.canvas.chunk;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.chunk.ChunkBuilder;
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

public class TerrainRenderer {
	private TerrainRenderer() {}

	private static final Direction[] DIRECTIONS = Direction.values();

	@SuppressWarnings("unchecked")
	public static void setupTerrain(WorldRendererExt wr, Camera camera, Frustum frustum, boolean bl, int i, boolean bl2) {
		final Vec3d cameraPos = camera.getPos();
		final MinecraftClient mc = wr.canvas_mc();
		final int renderDistance = wr.canvas_renderDistance();

		if (mc.options.viewDistance != renderDistance) {
			wr.canvas_reload();
		}

		final ClientWorld world = wr.canvas_world();
		final BuiltChunkStorage chunks = wr.canvas_chunks();
		Set<ChunkBuilder.BuiltChunk> chunksToRebuild  = wr.canvas_chunkToRebuild();
		final ChunkBuilder chunkBuilder = wr.canvas_chunkBuilder();
		@SuppressWarnings("rawtypes")
		final ObjectList visibleChunks = wr.canvas_visibleChunks();

		world.getProfiler().push("camera");
		final double d = mc.player.getX() - wr.canvas_lastCameraChunkUpdateX();
		final double e = mc.player.getY() - wr.canvas_lastCameraChunkUpdateY();
		final double f = mc.player.getZ() - wr.canvas_lastCameraChunkUpdateZ();
		int cameraChunkX = wr.canvas_camereChunkX();
		int cameraChunkY = wr.canvas_camereChunkY();
		int cameraChunkZ = wr.canvas_camereChunkZ();

		if (cameraChunkX != mc.player.chunkX || cameraChunkY != mc.player.chunkY || cameraChunkZ != mc.player.chunkZ || d * d + e * e + f * f > 16.0D) {
			wr.canvas_updateLastCameraChunkPositions();
			cameraChunkX = wr.canvas_camereChunkX();
			cameraChunkY = wr.canvas_camereChunkY();
			cameraChunkZ = wr.canvas_camereChunkZ();
			chunks.updateCameraPosition(mc.player.getX(), mc.player.getZ());
		}

		chunkBuilder.setCameraPosition(cameraPos);
		world.getProfiler().swap("cull");
		mc.getProfiler().swap("culling");
		final BlockPos blockPos = camera.getBlockPos();
		final ChunkBuilder.BuiltChunk builtChunk = wr.canvas_getRenderedChunk(blockPos);
		final BlockPos blockPos2 = new BlockPos(MathHelper.floor(cameraPos.x / 16.0D) * 16, MathHelper.floor(cameraPos.y / 16.0D) * 16, MathHelper.floor(cameraPos.z / 16.0D) * 16);
		final float pitch = camera.getPitch();
		final float yaw = camera.getYaw();
		boolean needsTerrainUpdate = wr.canvas_checkNeedsTerrainUpdate(cameraPos, pitch, yaw);

		mc.getProfiler().swap("update");
		ChunkInfoExt chunkInfo2;
		ChunkBuilder.BuiltChunk builtChunk3;
		if (!bl && needsTerrainUpdate) {
			needsTerrainUpdate = wr.canvas_setNeedsTerrainUpdate(false);
			visibleChunks.clear();
			final Queue<ChunkInfoExt> queue = Queues.newArrayDeque();
			Entity.setRenderDistanceMultiplier(MathHelper.clamp(mc.options.viewDistance / 8.0D, 1.0D, 2.5D));
			boolean bl3 = mc.chunkCullingEnabled;
			int n;
			int o;
			if (builtChunk != null) {
				boolean bl4 = false;
				final ChunkInfoExt chunkInfo = ChunkInfoFactory.INSTANCE.get(wr, builtChunk, (Direction)null, 0);
				final Set<Direction> set = getOpenChunkFaces(world, blockPos);

				if (set.size() == 1) {
					final Vector3f vector3f = camera.getHorizontalPlane();
					final Direction direction = Direction.getFacing(vector3f.getX(), vector3f.getY(), vector3f.getZ()).getOpposite();
					set.remove(direction);
				}

				if (set.isEmpty()) {
					bl4 = true;
				}

				if (bl4 && !bl2) {
					visibleChunks.add(chunkInfo);
				} else {
					if (bl2 && world.getBlockState(blockPos).isFullOpaque(world, blockPos)) {
						bl3 = false;
					}

					builtChunk.setRebuildFrame(i);
					queue.add(chunkInfo);
				}
			} else {
				final int k = blockPos.getY() > 0 ? 248 : 8;
				final int l = MathHelper.floor(cameraPos.x / 16.0D) * 16;
				final int m = MathHelper.floor(cameraPos.z / 16.0D) * 16;
				final List<ChunkInfoExt> list = Lists.newArrayList();
				n = -renderDistance;

				while(true) {
					if (n > renderDistance) {
						list.sort(Comparator.comparingDouble((chunkInfox) -> {
							return blockPos.getSquaredDistance(chunkInfox.canvas_chunk().getOrigin().add(8, 8, 8));
						}));
						queue.addAll(list);
						break;
					}

					for(o = -renderDistance; o <= renderDistance; ++o) {
						final ChunkBuilder.BuiltChunk builtChunk2 = wr.canvas_getRenderedChunk(new BlockPos(l + (n << 4) + 8, k, m + (o << 4) + 8));
						if (builtChunk2 != null && frustum.isVisible(builtChunk2.boundingBox)) {
							builtChunk2.setRebuildFrame(i);
							list.add(ChunkInfoFactory.INSTANCE.get(wr, builtChunk2, (Direction)null, 0));
						}
					}

					++n;
				}
			}

			mc.getProfiler().push("iteration");

			while(!queue.isEmpty()) {
				chunkInfo2 = queue.poll();
				builtChunk3 = chunkInfo2.canvas_chunk();
				final Direction direction2 = chunkInfo2.canvas_direction();
				visibleChunks.add(chunkInfo2);
				final Direction[] var40 = DIRECTIONS;
				n = var40.length;

				for(o = 0; o < n; ++o) {
					final Direction direction3 = var40[o];
					final ChunkBuilder.BuiltChunk builtChunk4 = getAdjacentChunk(wr, blockPos2, builtChunk3, direction3);
					if ((!bl3 || !chunkInfo2.canvas_canCull(direction3.getOpposite())) && (!bl3 || direction2 == null || builtChunk3.getData().isVisibleThrough(direction2.getOpposite(), direction3)) && builtChunk4 != null && builtChunk4.shouldBuild() && builtChunk4.setRebuildFrame(i) && frustum.isVisible(builtChunk4.boundingBox)) {
						final ChunkInfoExt chunkInfo3 = ChunkInfoFactory.INSTANCE.get(wr, builtChunk4, direction3, chunkInfo2.canvas_propagationLevel() + 1);
						chunkInfo3.canvas_updateCullingState(chunkInfo2.canvas_cullingState(), direction3);
						queue.add(chunkInfo3);
					}
				}
			}

			mc.getProfiler().pop();
		}

		mc.getProfiler().swap("rebuildNear");
		final Set<ChunkBuilder.BuiltChunk> set2 = chunksToRebuild;
		chunksToRebuild = wr.canvas_newChunkToRebuild();
		@SuppressWarnings("rawtypes")
		final ObjectListIterator var31 = visibleChunks.iterator();

		while(true) {
			while(true) {
				do {
					if (!var31.hasNext()) {
						chunksToRebuild.addAll(set2);
						mc.getProfiler().pop();
						return;
					}

					chunkInfo2 = (ChunkInfoExt) var31.next();
					builtChunk3 = chunkInfo2.canvas_chunk();
				} while(!builtChunk3.needsRebuild() && !set2.contains(builtChunk3));

				needsTerrainUpdate = wr.canvas_setNeedsTerrainUpdate(true);
				final BlockPos blockPos3 = builtChunk3.getOrigin().add(8, 8, 8);
				final boolean bl5 = blockPos3.getSquaredDistance(blockPos) < 768.0D;
				if (!builtChunk3.needsImportantRebuild() && !bl5) {
					chunksToRebuild.add(builtChunk3);
				} else {
					mc.getProfiler().push("build near");
					chunkBuilder.rebuild(builtChunk3);
					builtChunk3.cancelRebuild();
					mc.getProfiler().pop();
				}
			}
		}
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
}
