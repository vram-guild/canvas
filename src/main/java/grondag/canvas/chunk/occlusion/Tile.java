package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.A_NEGATIVE;
import static grondag.canvas.chunk.occlusion.Constants.A_POSITIVE;
import static grondag.canvas.chunk.occlusion.Constants.B_NEGATIVE;
import static grondag.canvas.chunk.occlusion.Constants.B_POSITIVE;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM_RIGHT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_RIGHT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP_RIGHT;
import static grondag.canvas.chunk.occlusion.Constants.INSIDE_0;
import static grondag.canvas.chunk.occlusion.Constants.INSIDE_1;
import static grondag.canvas.chunk.occlusion.Constants.INSIDE_2;
import static grondag.canvas.chunk.occlusion.Constants.INTERSECT;
import static grondag.canvas.chunk.occlusion.Constants.OUTSIDE_0;
import static grondag.canvas.chunk.occlusion.Constants.OUTSIDE_1;
import static grondag.canvas.chunk.occlusion.Constants.OUTSIDE_2;
import static grondag.canvas.chunk.occlusion.Constants.POS_012_III;
import static grondag.canvas.chunk.occlusion.Constants.POS_012_IIX;
import static grondag.canvas.chunk.occlusion.Constants.POS_012_IXI;
import static grondag.canvas.chunk.occlusion.Constants.POS_012_IXX;
import static grondag.canvas.chunk.occlusion.Constants.POS_012_XII;
import static grondag.canvas.chunk.occlusion.Constants.POS_012_XIX;
import static grondag.canvas.chunk.occlusion.Constants.POS_012_XXI;
import static grondag.canvas.chunk.occlusion.Constants.POS_012_XXX;
import static grondag.canvas.chunk.occlusion.Constants.POS_INVERSE_MASK_0;
import static grondag.canvas.chunk.occlusion.Constants.POS_INVERSE_MASK_1;
import static grondag.canvas.chunk.occlusion.Constants.POS_INVERSE_MASK_2;
import static grondag.canvas.chunk.occlusion.Constants.TILE_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.TILE_INDEX_HIGH_X;
import static grondag.canvas.chunk.occlusion.Constants.TILE_INDEX_HIGH_Y;
import static grondag.canvas.chunk.occlusion.Constants.TILE_INDEX_LOW_X_MASK;
import static grondag.canvas.chunk.occlusion.Constants.TILE_INDEX_LOW_Y;
import static grondag.canvas.chunk.occlusion.Constants.TILE_INDEX_LOW_Y_MASK;
import static grondag.canvas.chunk.occlusion.Constants.TILE_WIDTH;
import static grondag.canvas.chunk.occlusion.Data.events;
import static grondag.canvas.chunk.occlusion.Data.maxPixelX;
import static grondag.canvas.chunk.occlusion.Data.maxPixelY;
import static grondag.canvas.chunk.occlusion.Data.minPixelX;
import static grondag.canvas.chunk.occlusion.Data.minPixelY;
import static grondag.canvas.chunk.occlusion.Data.position0;
import static grondag.canvas.chunk.occlusion.Data.position1;
import static grondag.canvas.chunk.occlusion.Data.position2;
import static grondag.canvas.chunk.occlusion.Data.save_tileEdgeOutcomes;
import static grondag.canvas.chunk.occlusion.Data.save_tileIndex;
import static grondag.canvas.chunk.occlusion.Data.save_tileX;
import static grondag.canvas.chunk.occlusion.Data.save_tileY;
import static grondag.canvas.chunk.occlusion.Data.tileEdgeOutcomes;
import static grondag.canvas.chunk.occlusion.Data.tileIndex;
import static grondag.canvas.chunk.occlusion.Data.tileX;
import static grondag.canvas.chunk.occlusion.Data.tileY;
import static grondag.canvas.chunk.occlusion.Indexer.tileIndex;
import static grondag.canvas.chunk.occlusion.Rasterizer.printMask8x8;

abstract class Tile {
	private Tile() {}

	static void moveTileRight() {
		++tileX;

		if ((tileIndex & TILE_INDEX_LOW_X_MASK) == TILE_INDEX_LOW_X_MASK) {
			tileIndex = (tileIndex & ~TILE_INDEX_LOW_X_MASK) + TILE_INDEX_HIGH_X;
		} else {
			tileIndex += 1;
		}

		assert tileIndex == tileIndex(tileX, tileY);
		assert tileX < TILE_WIDTH;

		int pos = tileEdgeOutcomes;

		if (((pos & OUTSIDE_0) == 0 && (position0 & A_NEGATIVE) != 0) || ((pos & INSIDE_0) == 0 && (position0 & A_POSITIVE) != 0)) {
			pos = (pos & POS_INVERSE_MASK_0) | tilePosition(position0, 0);
		}

		if (((pos & OUTSIDE_1) == 0 && (position1 & A_NEGATIVE) != 0) || ((pos & INSIDE_1) == 0 && (position1 & A_POSITIVE) != 0)) {
			pos = (pos & POS_INVERSE_MASK_1) | (tilePosition(position1, 1) << 2);
		}

		if (((pos & OUTSIDE_2) == 0 && (position2 & A_NEGATIVE) != 0) || ((pos & INSIDE_2) == 0 && (position2 & A_POSITIVE) != 0)) {
			pos = (pos & POS_INVERSE_MASK_2) | (tilePosition(position2, 2) << 4);
		}

		tileEdgeOutcomes = pos;
	}

	static void moveTileLeft() {
		--tileX;

		if ((tileIndex & TILE_INDEX_LOW_X_MASK) == 0) {
			tileIndex |= TILE_INDEX_LOW_X_MASK;
			tileIndex -= TILE_INDEX_HIGH_X;
		} else {
			tileIndex -= 1;
		}

		assert tileIndex == tileIndex(tileX, tileY);
		assert tileX >= 0;

		int pos = tileEdgeOutcomes;

		if (((pos & OUTSIDE_0) == 0 && (position0 & A_POSITIVE) != 0) || ((pos & INSIDE_0) == 0 && (position0 & A_NEGATIVE) != 0)) {
			pos = (pos & POS_INVERSE_MASK_0) | tilePosition(position0, 0);
		}

		if (((pos & OUTSIDE_1) == 0 && (position1 & A_POSITIVE) != 0) || ((pos & INSIDE_1) == 0 && (position1 & A_NEGATIVE) != 0)) {
			pos = (pos & POS_INVERSE_MASK_1) | (tilePosition(position1, 1) << 2);
		}

		if (((pos & OUTSIDE_2) == 0 && (position2 & A_POSITIVE) != 0) || ((pos & INSIDE_2) == 0 && (position2 & A_NEGATIVE) != 0)) {
			pos = (pos & POS_INVERSE_MASK_2) | (tilePosition(position2, 2) << 4);
		}

		tileEdgeOutcomes  = pos;
	}

	static void moveTileUp() {
		++tileY;

		if ((tileIndex & TILE_INDEX_LOW_Y_MASK) == TILE_INDEX_LOW_Y_MASK) {
			tileIndex = (tileIndex & ~TILE_INDEX_LOW_Y_MASK) + TILE_INDEX_HIGH_Y;
		} else {
			tileIndex += TILE_INDEX_LOW_Y;
		}

		assert tileY < TILE_HEIGHT;
		assert tileIndex == tileIndex(tileX, tileY);

		int pos = tileEdgeOutcomes;

		if (((pos & OUTSIDE_0) == 0 && (position0 & B_NEGATIVE) != 0) || ((pos & INSIDE_0) == 0 && (position0 & B_POSITIVE) != 0)) {
			pos = (pos & POS_INVERSE_MASK_0) | tilePosition(position0, 0);
		}

		if (((pos & OUTSIDE_1) == 0 && (position1 & B_NEGATIVE) != 0) || ((pos & INSIDE_1) == 0 && (position1 & B_POSITIVE) != 0)) {
			pos = (pos & POS_INVERSE_MASK_1) | (tilePosition(position1, 1) << 2);
		}

		if (((pos & OUTSIDE_2) == 0 && (position2 & B_NEGATIVE) != 0) || ((pos & INSIDE_2) == 0 && (position2 & B_POSITIVE) != 0)) {
			pos = (pos & POS_INVERSE_MASK_2) | (tilePosition(position2, 2) << 4);
		}

		tileEdgeOutcomes = pos;
	}

	static int tilePosition(int pos, int index) {
		// PERF: check shouldn't be needed - shouldn't be called in this case
		final int ty = tileY << 3;

		if (ty > maxPixelY || ty + 7 < minPixelY) {
			return OUTSIDE_0;
		}

		final int tx = tileX << 3;

		if (tx > maxPixelX || tx + 7 < minPixelX) {
			return OUTSIDE_0;
		}

		switch  (pos) {
		case EDGE_TOP: {
			final int py = events[index] - ty;

			if (py < 0) {
				return OUTSIDE_0;
			} else if (py >= 7) {
				return INSIDE_0;
			} else {
				return INTERSECT;
			}
		}

		case EDGE_BOTTOM: {
			final int py = events[index] - ty;

			if (py > 7) {
				return OUTSIDE_0;
			} else if (py <= 0) {
				return INSIDE_0;
			} else {
				return INTERSECT;
			}
		}

		case EDGE_RIGHT: {
			final int px = events[index] - tx;

			if (px < 0) {
				return OUTSIDE_0;
			} else if (px >= 7) {
				return INSIDE_0;
			} else {
				return INTERSECT;
			}
		}

		case EDGE_LEFT: {
			final int px = events[index] - tx;

			if (px > 7) {
				return OUTSIDE_0;
			} else if (px <= 0) {
				return INSIDE_0;
			} else {
				return INTERSECT;
			}
		}

		case EDGE_TOP_LEFT: {
			if(events[(ty << 2) + index] > tx + 7) {
				return OUTSIDE_0;
			} else if (events[((ty + 7) << 2) + index] <= tx) {
				return INSIDE_0;
			} else {
				return INTERSECT;
			}
		}

		case EDGE_BOTTOM_LEFT: {
			if(events[((ty + 7) << 2) + index] > tx + 7) {
				return OUTSIDE_0;
			} else if (events[(ty << 2) + index] <= tx) {
				return INSIDE_0;
			} else {
				return INTERSECT;
			}
		}

		case EDGE_TOP_RIGHT: {
			if(events[(ty << 2) + index] < tx) {
				return OUTSIDE_0;
			} else if (events[((ty + 7) << 2) + index] >= tx + 7) {
				return INSIDE_0;
			} else {
				return INTERSECT;
			}
		}

		case EDGE_BOTTOM_RIGHT: {
			if(events[((ty + 7) << 2) + index] < tx) {
				return OUTSIDE_0;
			} else if (events[(ty << 2) + index] >= tx + 7) {
				return INSIDE_0;
			} else {
				return INTERSECT;
			}
		}

		default:
			assert false : "Edge flag out of bounds.";
		return 0;
		}
	}

	static long computeTileCoverage() {
		switch(tileEdgeOutcomes)  {

		default:
			// most cases have at least one outside edge
			return 0L;

		case POS_012_III:
			// all inside
			return -1L;

		case POS_012_XII:
			return buildTileMask(position0, 0);

		case POS_012_IXI:
			return buildTileMask(position1, 1);

		case POS_012_IIX:
			return buildTileMask(position2, 2);

		case POS_012_XIX:
			return buildTileMask(position0, 0)
					& buildTileMask(position2, 2);

		case POS_012_XXI:
			return buildTileMask(position0, 0)
					& buildTileMask(position1, 1);

		case POS_012_IXX:
			return buildTileMask(position1, 1)
					& buildTileMask(position2, 2);

		case POS_012_XXX:
			return buildTileMask(position0, 0)
					& buildTileMask(position1, 1)
					& buildTileMask(position2, 2);
		}
	}

	static void pushTile() {
		save_tileX = tileX;
		save_tileY = tileY;
		save_tileIndex = tileIndex;
		save_tileEdgeOutcomes = tileEdgeOutcomes;
	}

	static void popTile() {
		tileX = save_tileX;
		tileY = save_tileY;
		tileIndex = save_tileIndex;
		tileEdgeOutcomes = save_tileEdgeOutcomes;

		assert tileX < TILE_WIDTH;
		assert tileY < TILE_HEIGHT;
	}


	static long buildTileMaskTest(int pos, int index) {
		final long  oldResult = buildTileMaskOld(pos, index);
		final long  newResult = buildTileMask(pos, index);

		if (oldResult != newResult) {
			System.out.println();
			System.out.println("OLD - POSITION = " + pos);
			printMask8x8(oldResult);
			System.out.println("NEW");
			printMask8x8(newResult);
			buildTileMask(pos, index);
		}

		return oldResult;
	}


	static long buildTileMask(int pos, int index) {
		// PERF: check shouldn't be needed - shouldn't be called in this case
		int ty = tileY << 3;

		if (ty > maxPixelY || ty + 7 < minPixelY) {
			return 0L;
		}

		final int tx = tileX << 3;

		if (tx > maxPixelX || tx + 7 < minPixelX) {
			return 0L;
		}

		switch  (pos) {
		case EDGE_TOP: {
			final int py = events[index] - ty;

			if (py < 0) {
				return 0L;
			} else if (py >= 7) {
				return -1L;
			} else {
				return -1L >>> ((7 - py) << 3);
			}
		}

		case EDGE_BOTTOM: {
			final int py = events[index] - ty;

			if (py > 7) {
				return 0L;
			} else if (py <= 0) {
				return -1L;
			} else {
				return -1L << (py << 3);
			}
		}

		case EDGE_RIGHT: {
			final int px = events[index] - tx;

			if (px < 0) {
				return 0L;
			} else if (px >= 7) {
				return -1L;
			} else {
				long mask = (0xFF >> (7 - px));

				mask |= mask << 8;
				mask |= mask << 16;
				mask |= mask << 32;

				return mask;
			}
		}

		case EDGE_LEFT: {
			final int px = events[index] - tx;

			if (px > 7) {
				return 0L;
			} else if (px <= 0) {
				return -1L;
			} else {
				long mask = (0xFF << px) & 0xFF;

				mask |= mask << 8;
				mask |= mask << 16;
				mask |= mask << 32;

				return mask;
			}
		}

		case EDGE_TOP_LEFT: {
			long mask = 0;
			int yShift = 0;
			ty <<= 2;
			ty += index;

			while (yShift < 64) {
				final int x = events[ty] - tx;
				ty += 4;

				if(x > 7) return mask;

				if (x <= 0) {
					mask |= 0xFFL << yShift;
				} else {
					mask |= ((long) ((0xFF << x) & 0xFF)) << yShift;
				}

				yShift += 8;
			}

			return mask;
		}

		case EDGE_BOTTOM_LEFT: {
			int yShift = 56;
			long mask = 0;
			ty += 7;
			ty <<= 2;
			ty += index;

			while (yShift >= 0) {
				final int x = events[ty] - tx;
				ty -= 4;

				if(x > 7) return mask;

				if (x <= 0) {
					mask |= 0xFFL << yShift;
				} else {
					mask |= ((long) ((0xFF << x) & 0xFF)) << yShift;
				}

				yShift -= 8;
			}

			return mask;
		}

		case EDGE_TOP_RIGHT: {
			long mask = 0;
			int yShift = 0;
			ty <<= 2;
			ty += index;

			while(yShift < 64) {
				final int x = events[ty] - tx;
				ty += 4;

				if(x < 0) return mask;

				if (x >= 7) {
					mask |= 0xFFL << yShift;
				} else {
					mask |= ((long) (0xFF >> (7 - x))) << yShift;
				}

				yShift += 8;
			}

			return mask;
		}

		case EDGE_BOTTOM_RIGHT: {
			int yShift = 56;
			long mask = 0;
			ty += 7;
			ty <<= 2;
			ty += index;

			while (yShift >= 0) {
				final int x = events[ty] - tx;
				ty -= 4;

				if(x < 0) return mask;

				if (x >= 7) {
					mask |= 0xFFL << yShift;
				} else {
					mask |= ((long) (0xFF >> (7 - x))) << yShift;
				}

				yShift -= 8;
			}

			return mask;
		}

		default:
			assert false : "Edge flag out of bounds.";
		return 0L;
		}
	}

	// TODO: remove - currently same - left for later
	static long buildTileMaskOld(int pos, int index) {
		// PERF: check shouldn't be needed - shouldn't be called in this case
		int ty = tileY << 3;

		if (ty > maxPixelY || ty + 7 < minPixelY) {
			return 0L;
		}

		final int tx = tileX << 3;

		if (tx > maxPixelX || tx + 7 < minPixelX) {
			return 0L;
		}

		switch  (pos) {
		case EDGE_TOP: {
			final int py = events[index] - ty;

			if (py < 0) {
				return 0L;
			} else if (py >= 7) {
				return -1L;
			} else {
				return -1L >>> ((7 - py) << 3);
			}
		}

		case EDGE_BOTTOM: {
			final int py = events[index] - ty;

			if (py > 7) {
				return 0L;
			} else if (py <= 0) {
				return -1L;
			} else {
				return -1L << (py << 3);
			}
		}

		case EDGE_RIGHT: {
			final int px = events[index] - tx;

			if (px < 0) {
				return 0L;
			} else if (px >= 7) {
				return -1L;
			} else {
				long mask = (0xFF >> (7 - px));

				mask |= mask << 8;
				mask |= mask << 16;
				mask |= mask << 32;

				return mask;
			}
		}

		case EDGE_LEFT: {
			final int px = events[index] - tx;

			if (px > 7) {
				return 0L;
			} else if (px <= 0) {
				return -1L;
			} else {
				long mask = (0xFF << px) & 0xFF;

				mask |= mask << 8;
				mask |= mask << 16;
				mask |= mask << 32;

				return mask;
			}
		}

		case EDGE_TOP_LEFT: {
			long mask = 0;
			int yShift = 0;
			ty <<= 2;
			ty += index;

			while (yShift < 64) {
				final int x = events[ty] - tx;
				ty += 4;

				if(x > 7) return mask;

				if (x <= 0) {
					mask |= 0xFFL << yShift;
				} else {
					mask |= ((long) ((0xFF << x) & 0xFF)) << yShift;
				}

				yShift += 8;
			}

			return mask;
		}

		case EDGE_BOTTOM_LEFT: {
			int yShift = 56;
			long mask = 0;
			ty += 7;
			ty <<= 2;
			ty += index;

			while (yShift >= 0) {
				final int x = events[ty] - tx;
				ty -= 4;

				if(x > 7) return mask;

				if (x <= 0) {
					mask |= 0xFFL << yShift;
				} else {
					mask |= ((long) ((0xFF << x) & 0xFF)) << yShift;
				}

				yShift -= 8;
			}

			return mask;
		}

		case EDGE_TOP_RIGHT: {
			long mask = 0;
			int yShift = 0;
			ty <<= 2;
			ty += index;

			while(yShift < 64) {
				final int x = events[ty] - tx;
				ty += 4;

				if(x < 0) return mask;

				if (x >= 7) {
					mask |= 0xFFL << yShift;
				} else {
					mask |= ((long) (0xFF >> (7 - x))) << yShift;
				}

				yShift += 8;
			}

			return mask;
		}

		case EDGE_BOTTOM_RIGHT: {
			int yShift = 56;
			long mask = 0;
			ty += 7;
			ty <<= 2;
			ty += index;

			while (yShift >= 0) {
				final int x = events[ty] - tx;
				ty -= 4;

				if(x < 0) return mask;

				if (x >= 7) {
					mask |= 0xFFL << yShift;
				} else {
					mask |= ((long) (0xFF >> (7 - x))) << yShift;
				}

				yShift -= 8;
			}

			return mask;
		}

		default:
			assert false : "Edge flag out of bounds.";
		return 0L;
		}
	}

}