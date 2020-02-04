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

	public static int relativeBlockIndex(int x, int y, int z) {
		final int scenario = ((x & 0xF) == x ? 1 : 0) | ((y & 0xF) == y ? 2 : 0) | ((z & 0xF) == z ? 4 : 0);

		switch(scenario) {
		case 0b000:
			// not contained in any - may be a corner
			if(x == -1) {
				if(y == -1) {
					if(z == -1) {
						return localCornerIndex(false, false, false);
					} else if (z == 16) {
						return localCornerIndex(false, false, true);
					}
				} else if (y == 16) {
					if(z == -1) {
						return localCornerIndex(false, true, false);
					} else if (z == 16) {
						return localCornerIndex(false, true, true);
					}
				}
			} else if (x == 16) {
				if(y == -1) {
					if(z == -1) {
						return localCornerIndex(true, false, false);
					} else if (z == 16) {
						return localCornerIndex(true, false, true);
					}
				} else if (y == 16) {
					if(z == -1) {
						return localCornerIndex(true, true, false);
					} else if (z == 16) {
						return localCornerIndex(true, true, true);
					}
				}
			}

			break;
		case 0b001:
			// contained in X - may be an edge
			if(y == -1) {
				if(z == -1) {
					return localXEdgeIndex(x & 0xF, false, false);
				} else if (z == 16) {
					return localXEdgeIndex(x & 0xF, false, true);
				}
			} else if (y == 16) {
				if(z == -1) {
					return localXEdgeIndex(x & 0xF, true, false);
				} else if (z == 16) {
					return localXEdgeIndex(x & 0xF, true, true);
				}
			}

			break;

		case 0b010:
			// contained in Y - may be an edge
			if(x == -1) {
				if(z == -1) {
					return localYEdgeIndex(false, y & 0xF, false);
				} else if (z == 16) {
					return localYEdgeIndex(false, y & 0xF, true);
				}
			} else if (x == 16) {
				if(z == -1) {
					return localYEdgeIndex(true, y & 0xF, false);
				} else if (z == +16) {
					return localYEdgeIndex(true, y & 0xF, true);
				}
			}

			break;

		case 0b100:
			// contained in Z - may be an edge
			if(x == -1) {
				if(y == -1) {
					return localZEdgeIndex(false, false, z & 0xF);
				} else if (y == 16) {
					return localZEdgeIndex(false, true, z & 0xF);
				}
			} else if (x == 16) {
				if(y == -1) {
					return localZEdgeIndex(true, false, z & 0xF);
				} else if (y == 16) {
					return localZEdgeIndex(true, true, z & 0xF);
				}
			}

			break;

		case 0b011:
			// contained in XY - may be a Z face
			if(z == -1) {
				return localZfaceIndex(x & 0xF, y & 0xF, false);
			} else if (z == 16) {
				return localZfaceIndex(x & 0xF, y & 0xF, true);
			}
			break;

		case 0b110:
			// contained in YZ - may be an X face
			if(x == -1) {
				return localXfaceIndex(false, y & 0xF, z & 0xF);
			} else if (x == 16) {
				return localXfaceIndex(true, y & 0xF, z & 0xF);
			}
			break;

		case 0b101:
			// contained in XZ - may be a Y face
			if(y == -1) {
				return localYfaceIndex(x & 0xF, false, z & 0xF);
			} else if (y == 16) {
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
