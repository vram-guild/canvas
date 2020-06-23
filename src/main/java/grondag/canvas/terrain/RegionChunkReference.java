package grondag.canvas.terrain;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.ChunkStatus;

public class RegionChunkReference {
	private final ClientWorld world;

	private int chunkX;
	private int chunkZ;
	private boolean areCornersLoadedCache = false;

	public  RegionChunkReference(ClientWorld world) {
		this.world = world;
	}

	public void setOrigin(int x, int z) {
		areCornersLoadedCache = false;
		chunkX = x >> 4;
		chunkZ = z >> 4;
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
}
