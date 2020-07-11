package grondag.canvas.terrain;

import java.util.function.Predicate;

import it.unimi.dsi.fastutil.Hash;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import grondag.canvas.render.CanvasWorldRenderer;

public class RenderRegionStorage {
	// Hat tip to JellySquid for the suggestion of using a hashmap
	// PERF: lock-free implementation
	private final HackedLong2ObjectMap<BuiltRenderRegion> regionMap = new HackedLong2ObjectMap<>(8192, Hash.VERY_FAST_LOAD_FACTOR, r -> r.close());
	private final HackedLong2ObjectMap<RegionChunkReference> chunkRefMap = new HackedLong2ObjectMap<>(2048, Hash.VERY_FAST_LOAD_FACTOR, r -> {});
	private int positionVersion;
	private final CanvasWorldRenderer cwr;

	private final int regionVersion = -1;

	public RenderRegionStorage(CanvasWorldRenderer cwr) {
		this.cwr = cwr;
	}

	private RegionChunkReference chunkRef(long packedOriginPos) {
		final long key = ChunkPos.toLong(BlockPos.unpackLongX(packedOriginPos) >> 4, BlockPos.unpackLongZ(packedOriginPos) >> 4);
		return chunkRefMap.computeIfAbsent(key, k -> new RegionChunkReference(cwr.getWorld(), key));
	}

	public void clear() {
		regionMap.clear();
		chunkRefMap.clear();
	}

	public void scheduleRebuild(int x, int y, int z, boolean urgent) {
		if ((y & 0xFFFFFF00) == 0) {
			final BuiltRenderRegion region = regionMap.get(BlockPos.asLong(x & 0xFFFFFFF0, y & 0xFFFFFFF0, z & 0xFFFFFFF0));

			if (region != null) {
				region.markForBuild(urgent);
			}
		}
	}

	private static final Predicate<BuiltRenderRegion> REGION_PRUNER = r -> {
		// TODO: confirm not creating/removing due to mismatch in distances
		if (!r.updateCameraDistance()) {
			r.close();
			return true;
		} else {
			return false;
		}
	};

	private static final Predicate<RegionChunkReference> CHUNK_REF_PRUNER = RegionChunkReference::isEmpty;

	/**
	 * Called each frame, but only updates when player has moved more than 1 block.
	 * Uses position version to detect the movement.
	 */
	public void updateCameraDistance(Vec3d cameraPos, int positionVersion, int renderDistance) {
		if (this.positionVersion == positionVersion) {
			return;
		}

		this.positionVersion = positionVersion;

		regionMap.prune(REGION_PRUNER);
		chunkRefMap.prune(CHUNK_REF_PRUNER);
	}

	public int regionCount() {
		return regionMap.size();
	}

	public int regionVersion() {
		return regionVersion;
	}

	private BuiltRenderRegion getOrCreateRegion(long packedOriginPos) {
		return regionMap.computeIfAbsent(packedOriginPos, k -> {
			final BuiltRenderRegion result = new BuiltRenderRegion(cwr, chunkRef(k), k);
			result.updateCameraDistance();
			return result;
		});
	}

	public BuiltRenderRegion getOrCreateRegion(int x, int y, int z) {
		return getOrCreateRegion(BlockPos.asLong(x & 0xFFFFFFF0, y & 0xFFFFFFF0, z & 0xFFFFFFF0));
	}

	public BuiltRenderRegion getOrCreateRegion(BlockPos pos) {
		return getOrCreateRegion(pos.getX(), pos.getY(), pos.getZ());
	}

	public BuiltRenderRegion getRegionIfExists(BlockPos pos) {
		return getRegionIfExists(pos.getX(), pos.getY(), pos.getZ());
	}

	public BuiltRenderRegion getRegionIfExists(int x, int y, int z) {
		return regionMap.get(BlockPos.asLong(x & 0xFFFFFFF0, y & 0xFFFFFFF0, z & 0xFFFFFFF0));
	}

	public boolean wasSeen(int x, int y, int z) {
		final BuiltRenderRegion r = getRegionIfExists(x, y, z);
		return r == null ? false : r.wasRecentlySeen();
	}
}
