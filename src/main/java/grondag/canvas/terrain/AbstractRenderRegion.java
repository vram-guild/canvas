package grondag.canvas.terrain;

import static grondag.canvas.terrain.RenderRegionAddressHelper.relativeCacheIndex;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Tries to prevent FastRenderRegion from being unreadably big. Fails.
 */
abstract class AbstractRenderRegion {
	protected int originX;
	protected int originY;
	protected int originZ;

	protected int chunkBaseX;
	protected int chunkBaseY;
	protected int chunkBaseZ;

	protected World world;

	// larger than needed to speed up indexing
	protected final WorldChunk[] chunks = new WorldChunk[16];

	final boolean isInMainChunk(int x, int y, int z) {
		return originX == (x & 0xFFFFFFF0) && originY == (y & 0xFFFFFFF0) && originZ == (z & 0xFFFFFFF0);
	}

	final boolean isInMainChunk(BlockPos pos) {
		return isInMainChunk(pos.getX(), pos.getY(), pos.getZ());
	}

	final int blockIndex(int x, int y, int z) {
		return relativeCacheIndex(x - originX, y - originY, z - originZ);
	}

	protected ChunkSection getSection(int x, int y, int z) {
		// TODO: handle world border

		if ((y == 0 && chunkBaseY < 0) || (y == 2 && chunkBaseY > 13)) {
			return null;
		}

		return chunks[x | (z << 2)].getSectionArray()[chunkBaseY + y];
	}

	protected WorldChunk getChunk(int cx, int cz) {
		final int chunkBaseX = this.chunkBaseX;
		final int chunkBaseZ = this.chunkBaseZ;

		if (cx < chunkBaseX || cx > (chunkBaseZ + 2) || cz < chunkBaseZ || cz > (chunkBaseZ + 2)) {
			return world.getChunk(cx, cz);
		} else {
			return chunks[(cx - chunkBaseX) | ((cz - chunkBaseZ) << 2)];
		}
	}
}
