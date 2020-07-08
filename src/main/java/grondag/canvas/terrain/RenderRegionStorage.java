package grondag.canvas.terrain;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import grondag.canvas.render.CanvasWorldRenderer;

public class RenderRegionStorage {
	// Hat tip to JellySquid for the suggestion
	// PERF: lock-free implementation
	private final Long2ObjectMap<BuiltRenderRegion> regions = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<BuiltRenderRegion>(8192, 0.5f));
	private final Long2ObjectMap<RegionChunkReference> chunkRefs = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<RegionChunkReference>(2048, 0.5f));
	private int positionVersion;
	private final CanvasWorldRenderer cwr;

	private final int regionVersion = -1;

	public RenderRegionStorage(CanvasWorldRenderer cwr) {
		this.cwr = cwr;
	}

	private RegionChunkReference chunkRef(long packedOriginPos) {
		final long key = ChunkPos.toLong(BlockPos.unpackLongX(packedOriginPos) >> 4, BlockPos.unpackLongZ(packedOriginPos) >> 4);
		return chunkRefs.computeIfAbsent(key, k -> new RegionChunkReference(cwr.getWorld(), key));
	}

	public void clear() {
		regions.values().forEach(r -> r.close());
		regions.clear();
		chunkRefs.clear();
	}

	public void scheduleRebuild(int x, int y, int z, boolean urgent) {
		if ((y & 0xFFFFFF00) == 0) {
			final BuiltRenderRegion region = regions.get(BlockPos.asLong(x & 0xFFFFFFF0, y & 0xFFFFFFF0, z & 0xFFFFFFF0));

			if (region != null) {
				region.markForBuild(urgent);
			}
		}
	}

	private final LongArrayList deleted = new LongArrayList();

	/**
	 * Called each frame, but only updates when player has moved more than 1 block.
	 * Uses position version to detect the movement.
	 */
	public void updateCameraDistance(Vec3d cameraPos, int positionVersion, int renderDistance) {
		if (this.positionVersion == positionVersion) {
			return;
		}

		this.positionVersion = positionVersion;

		final LongArrayList deleted = this.deleted;
		final ObjectIterator<Entry<BuiltRenderRegion>> itRegion = regions.long2ObjectEntrySet().iterator();

		while (itRegion.hasNext()) {
			final Entry<BuiltRenderRegion> e = itRegion.next();
			final BuiltRenderRegion region = e.getValue();

			// FIX: confirm not creating/removing due to mismatch in distances
			if (!region.updateCameraDistance()) {
				region.close();
				deleted.add(e.getLongKey());
			}
		}

		deleted.forEach((long l) -> regions.remove(l));
		deleted.clear();

		final ObjectIterator<Entry<RegionChunkReference>> itChunkRef = chunkRefs.long2ObjectEntrySet().iterator();

		while (itChunkRef.hasNext()) {
			final Entry<RegionChunkReference> e = itChunkRef.next();
			final RegionChunkReference ref = e.getValue();

			if (ref.isEmpty()) {
				deleted.add(e.getLongKey());
			}
		}

		deleted.forEach((long l) -> chunkRefs.remove(l));
		deleted.clear();
	}

	public int regionCount() {
		return regions.size();
	}

	public int regionVersion() {
		return regionVersion;
	}

	private BuiltRenderRegion getOrCreateRegion(long packedOriginPos) {
		return regions.computeIfAbsent(packedOriginPos, k -> {
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
		return regions.get(BlockPos.asLong(x & 0xFFFFFFF0, y & 0xFFFFFFF0, z & 0xFFFFFFF0));
	}

	public boolean wasSeen(int x, int y, int z) {
		final BuiltRenderRegion r = getRegionIfExists(x, y, z);
		return r == null ? false : r.wasRecentlySeen();
	}
}
