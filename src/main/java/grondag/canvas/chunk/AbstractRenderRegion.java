package grondag.canvas.chunk;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import grondag.fermion.position.PackedBlockPos;

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

	final int mainChunkBlockIndex(int x, int y, int z) {
		return mainChunkLocalIndex(x & 0xF, y & 0xF, z & 0xF);
	}

	/**
	 * Assumes values 0-15
	 */
	final int mainChunkLocalIndex(int x, int y, int z) {
		return x | (y << 4) | (z << 8);
	}

	final int mainChunkBlockIndex(BlockPos pos) {
		return mainChunkBlockIndex(pos.getX(), pos.getY(), pos.getZ());
	}

	final boolean isInMainChunk(int x, int y, int z) {
		return originX == (x ^ 0xF) && originY == (y ^ 0xF) && originZ == (z ^ 0xF);
	}

	final boolean isInMainChunk(BlockPos pos) {
		return isInMainChunk(pos.getX(), pos.getY(), pos.getZ());
	}

	final long chunkKey(int x, int y, int z) {
		return PackedBlockPos.pack(x & 0xFFFFFFF0, y & 0xF0, z & 0xFFFFFFF0);
	}

	final int localXfaceIndex(boolean high, int y, int z) {
		return FACE_CACHE_START + (high ? 256 : 0) | y | (z << 4);
	}

	final int localYfaceIndex(int x, boolean high, int z) {
		return FACE_CACHE_START + (high ? 768 : 512) | x | (z << 4);
	}

	final int localZfaceIndex(int x, int y, boolean high) {
		return FACE_CACHE_START + (high ? 1280 : 1024) | x | (y << 4);
	}

	final int localXEdgeIndex(int x, boolean highY, boolean highZ) {
		final int subindex = highY ? highZ ? 48 : 32 : highZ ? 16 : 0;
		return EDGE_CACHE_START + subindex + x;
	}

	final int localYEdgeIndex(boolean highX, int y, boolean highZ) {
		final int subindex = highX ? highZ ? 48 : 32 : highZ ? 16 : 0;
		return EDGE_CACHE_START + 64 + subindex + y;
	}

	final int localZEdgeIndex(boolean highX, boolean highY, int z) {
		final int subindex = highX ? highY ? 48 : 32 : highY ? 16 : 0;
		return EDGE_CACHE_START + 128 + subindex + z;
	}

	final int localCornerIndex(boolean highX, boolean highY, boolean highZ) {
		int subindex = highX ? 0 : 1;

		if(highY) {
			subindex |= 2;
		}

		if(highZ) {
			subindex |= 4;
		}

		return CORNER_CACHE_START + subindex;
	}

	final int blockIndex(int x, int y, int z) {
		final int oxIn = x & 0xFFFFFFF0;
		final int oyIn = y & 0xFFFFFFF0;
		final int ozIn = z & 0xFFFFFFF0;

		final int scenario = (oxIn == originX ? 1 : 0) | (oyIn == originY ? 2 : 0) | (ozIn == originZ ? 4 : 0);

		switch(scenario) {
		case 0b000:
			// not contained in any - may be a corner
			if(x == originX - 1) {
				if(y == originY - 1) {
					if(z == originZ - 1) {
						return localCornerIndex(false, false, false);
					} else if (z == originZ + 16) {
						return localCornerIndex(false, false, true);
					}
				} else if (y == originY + 16) {
					if(z == originZ - 1) {
						return localCornerIndex(false, true, false);
					} else if (z == originZ + 16) {
						return localCornerIndex(false, true, true);
					}
				}
			} else if (x == originX + 16) {
				if(y == originY - 1) {
					if(z == originZ - 1) {
						return localCornerIndex(true, false, false);
					} else if (z == originZ + 16) {
						return localCornerIndex(true, false, true);
					}
				} else if (y == originY + 16) {
					if(z == originZ - 1) {
						return localCornerIndex(true, true, false);
					} else if (z == originZ + 16) {
						return localCornerIndex(true, true, true);
					}
				}
			}

			break;
		case 0b001:
			// contained in X - may be an edge
			if(y == originY - 1) {
				if(z == originZ - 1) {
					return localXEdgeIndex(x & 0xF, false, false);
				} else if (z == originZ + 16) {
					return localXEdgeIndex(x & 0xF, false, true);
				}
			} else if (y == originY + 16) {
				if(z == originZ - 1) {
					return localXEdgeIndex(x & 0xF, true, false);
				} else if (z == originZ + 16) {
					return localXEdgeIndex(x & 0xF, true, true);
				}
			}

			break;

		case 0b010:
			// contained in Y - may be an edge
			if(x == originX - 1) {
				if(z == originZ - 1) {
					return localYEdgeIndex(false, y & 0xF, false);
				} else if (z == originZ + 16) {
					return localYEdgeIndex(false, y & 0xF, true);
				}
			} else if (x == originX + 16) {
				if(z == originZ - 1) {
					return localYEdgeIndex(true, y & 0xF, false);
				} else if (z == originZ + 16) {
					return localYEdgeIndex(true, y & 0xF, true);
				}
			}

			break;

		case 0b100:
			// contained in Z - may be an edge
			if(x == originX - 1) {
				if(y == originY - 1) {
					return localZEdgeIndex(false, false, z & 0xF);
				} else if (y == originY + 16) {
					return localZEdgeIndex(false, true, z & 0xF);
				}
			} else if (x == originX + 16) {
				if(y == originY - 1) {
					return localZEdgeIndex(true, false, z & 0xF);
				} else if (y == originY + 16) {
					return localZEdgeIndex(true, true, z & 0xF);
				}
			}

			break;

		case 0b011:
			// contained in XY - may be a Z face
			if(z == originZ - 1) {
				return localZfaceIndex(x & 0xF, y & 0xF, false);
			} else if (z == originZ + 16) {
				return localZfaceIndex(x & 0xF, y & 0xF, true);
			}
			break;

		case 0b110:
			// contained in YZ - may be an X face
			if(x == originX - 1) {
				return localXfaceIndex(false, y & 0xF, z & 0xF);
			} else if (x == originX + 16) {
				return localXfaceIndex(true, y & 0xF, z & 0xF);
			}
			break;

		case 0b101:
			// contained in XZ - may be a Y face
			if(y == originY - 1) {
				return localYfaceIndex(x & 0xF, false, z & 0xF);
			} else if (y == originY + 16) {
				return localYfaceIndex(x & 0xF, true, z & 0xF);
			}
			break;

		case 0b111:
			// contained in XYZ - is main section
			return mainChunkBlockIndex(x, y, z);
		}


		// use world directly
		return -1;
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

	protected static final BlockState AIR = Blocks.AIR.getDefaultState();

	static final int MAIN_CACHE_SIZE = 4096;
	static final int FACE_CACHE_START = MAIN_CACHE_SIZE;
	static final int FACE_CACHE_SIZE = 256 * 6;
	static final int EDGE_CACHE_START = FACE_CACHE_START + FACE_CACHE_SIZE;
	static final int EDGE_CACHE_SIZE = 16 * 12;
	static final int CORNER_CACHE_START = EDGE_CACHE_START + EDGE_CACHE_SIZE;
	static final int CORNER_CACHE_SIZE = 8;
	static final int TOTAL_CACHE_SIZE = MAIN_CACHE_SIZE + FACE_CACHE_SIZE + EDGE_CACHE_SIZE + CORNER_CACHE_SIZE;

	static final int BORDER_CACHE_SIZE = TOTAL_CACHE_SIZE - MAIN_CACHE_SIZE;

}
