package grondag.canvas.chunk;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public abstract class RenderRegionAddressHelper {
	private RenderRegionAddressHelper() {}

	protected static int mainChunkBlockIndex(int x, int y, int z) {
		return mainChunkLocalIndex(x & 0xF, y & 0xF, z & 0xF);
	}

	/**
	 * Assumes values 0-15
	 */
	protected static int mainChunkLocalIndex(int x, int y, int z) {
		return x | (y << 4) | (z << 8);
	}

	protected static int mainChunkBlockIndex(BlockPos pos) {
		return mainChunkBlockIndex(pos.getX(), pos.getY(), pos.getZ());
	}

	protected static int localXfaceIndex(boolean high, int y, int z) {
		return FACE_CACHE_START + (high ? 256 : 0) | y | (z << 4);
	}

	protected static int localYfaceIndex(int x, boolean high, int z) {
		return FACE_CACHE_START + (high ? 768 : 512) | x | (z << 4);
	}

	protected static int localZfaceIndex(int x, int y, boolean high) {
		return FACE_CACHE_START + (high ? 1280 : 1024) | x | (y << 4);
	}

	protected static int localXEdgeIndex(int x, boolean highY, boolean highZ) {
		final int subindex = highY ? highZ ? 48 : 32 : highZ ? 16 : 0;
		return EDGE_CACHE_START + subindex + x;
	}

	protected static int localYEdgeIndex(boolean highX, int y, boolean highZ) {
		final int subindex = highX ? highZ ? 48 : 32 : highZ ? 16 : 0;
		return EDGE_CACHE_START + 64 + subindex + y;
	}

	protected static int localZEdgeIndex(boolean highX, boolean highY, int z) {
		final int subindex = highX ? highY ? 48 : 32 : highY ? 16 : 0;
		return EDGE_CACHE_START + 128 + subindex + z;
	}


	protected static int localCornerIndex(boolean highX, boolean highY, boolean highZ) {
		int subindex = highX ? 0 : 1;

		if(highY) {
			subindex |= 2;
		}

		if(highZ) {
			subindex |= 4;
		}

		return CORNER_CACHE_START + subindex;
	}

	protected static final BlockState AIR = Blocks.AIR.getDefaultState();

	protected static final int INTERIOR_CACHE_SIZE = 4096;
	protected static final int FACE_CACHE_START = INTERIOR_CACHE_SIZE;
	protected static final int FACE_CACHE_SIZE = 256 * 6;
	protected static final int EDGE_CACHE_START = FACE_CACHE_START + FACE_CACHE_SIZE;
	protected static final int EDGE_CACHE_SIZE = 16 * 12;
	protected static final int CORNER_CACHE_START = EDGE_CACHE_START + EDGE_CACHE_SIZE;
	protected static final int CORNER_CACHE_SIZE = 8;
	protected static final int TOTAL_CACHE_SIZE = INTERIOR_CACHE_SIZE + FACE_CACHE_SIZE + EDGE_CACHE_SIZE + CORNER_CACHE_SIZE;

	protected static final int EXTERIOR_CACHE_SIZE = TOTAL_CACHE_SIZE - INTERIOR_CACHE_SIZE;

	protected static final int INTERIOR_CACHE_WORDS = INTERIOR_CACHE_SIZE / 64;
	protected static final int EXTERIOR_CACHE_WORDS = (EXTERIOR_CACHE_SIZE + 63) / 64;
	protected static final int TOTAL_CACHE_WORDS = INTERIOR_CACHE_WORDS + EXTERIOR_CACHE_WORDS;

}
