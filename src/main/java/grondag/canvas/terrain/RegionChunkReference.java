package grondag.canvas.terrain;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;

public class RegionChunkReference {
	private final ClientWorld world;

	private final int chunkX;
	private final int chunkZ;
	private boolean areCornersLoadedCache = false;
	private int refCount = 0;

	public  RegionChunkReference(ClientWorld world, long chunkPos) {
		this.world = world;
		chunkX = ChunkPos.getPackedX(chunkPos);
		chunkZ = ChunkPos.getPackedZ(chunkPos);
	}

	public boolean areCornersLoaded() {
		return areCornersLoadedCache || areCornerChunksLoaded();
	}

	private boolean areCornerChunksLoaded() {
		final ClientWorld world = this.world;

		final boolean result = world.getChunk(chunkX - 1, chunkZ - 1, ChunkStatus.FULL, false) != null
				&& world.getChunk(chunkX - 1, chunkZ + 1, ChunkStatus.FULL, false) != null
				&& world.getChunk(chunkX + 1, chunkZ - 1, ChunkStatus.FULL, false) != null
				&& world.getChunk(chunkX + 1, chunkZ + 1, ChunkStatus.FULL, false) != null;

		areCornersLoadedCache = result;

		return result;
	}

	public void retain(BuiltRenderRegion region) {
		++refCount;
	}

	public void release(BuiltRenderRegion region) {
		--refCount;
	}

	public boolean isEmpty() {
		return refCount == 0;
	}
}
