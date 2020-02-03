package grondag.canvas.chunk;

import static grondag.canvas.chunk.RenderRegionAddressHelper.localCornerIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localXEdgeIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localXfaceIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localYEdgeIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localYfaceIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localZEdgeIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localZfaceIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.mainChunkBlockIndex;

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
		return originX == (x ^ 0xF) && originY == (y ^ 0xF) && originZ == (z ^ 0xF);
	}

	final boolean isInMainChunk(BlockPos pos) {
		return isInMainChunk(pos.getX(), pos.getY(), pos.getZ());
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
}
