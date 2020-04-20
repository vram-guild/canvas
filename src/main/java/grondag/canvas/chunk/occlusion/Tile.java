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
import static grondag.canvas.chunk.occlusion.Data.temp;
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
			final int py = temp[index] - ty;

			if (py < 0) {
				return OUTSIDE_0;
			} else if (py >= 7) {
				return INSIDE_0;
			} else {
				return INTERSECT;
			}
		}

		case EDGE_BOTTOM: {
			final int py = temp[index] - ty;

			if (py > 7) {
				return OUTSIDE_0;
			} else if (py <= 0) {
				return INSIDE_0;
			} else {
				return INTERSECT;
			}
		}

		case EDGE_RIGHT: {
			final int px = temp[index] - tx;

			if (px < 0) {
				return OUTSIDE_0;
			} else if (px >= 7) {
				return INSIDE_0;
			} else {
				return INTERSECT;
			}
		}

		case EDGE_LEFT: {
			final int px = temp[index] - tx;

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
		//		switch(tileEdgeOutcomes)  {
		//
		//		default:
		//			// most cases have at least one outside edge
		//			return 0L;
		//
		//		case POS_012_III:
		//			// all inside
		//			return -1L;
		//
		//		case POS_012_XII:
		//			return buildTileMask(position0, 0);
		//
		//		case POS_012_IXI:
		//			return buildTileMask(position1, 1);
		//
		//		case POS_012_IIX:
		//			return buildTileMask(position2, 2);
		//
		//		case POS_012_XIX:
		//			return buildTileMask(position0, 0)
		//					& buildTileMask(position2, 2);
		//
		//		case POS_012_XXI:
		//			return buildTileMask(position0, 0)
		//					& buildTileMask(position1, 1);
		//
		//		case POS_012_IXX:
		//			return buildTileMask(position1, 1)
		//					& buildTileMask(position2, 2);
		//
		//		case POS_012_XXX:
		return buildTileMask(position0, 0)
				& buildTileMask(position1, 1)
				& buildTileMask(position2, 2);
		//		}
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


	static long buildTileMask(int pos, int index) {
		final long  oldResult = buildTileMaskOld(pos, index);
		final long  newResult = buildTileMaskNew(pos, index);

		if (oldResult != newResult) {
			System.out.println();
			System.out.println("OLD - POSITION = " + pos);
			printMask8x8(oldResult);
			System.out.println("NEW");
			printMask8x8(newResult);
			buildTileMaskNew(pos, index);
		}

		return oldResult;
	}


	static long buildTileMaskNew(int pos, int index) {
		// PERF: track pixel coordinates directly to avoid 2 shifts each tile
		final int ty = tileY << 3;

		if (ty > maxPixelY || ty + 7 < minPixelY) {
			return 0L;
		}

		final int tx = tileX << 3;

		if (tx > maxPixelX || tx + 7 < minPixelX) {
			return 0L;
		}

		final int yIndex = (ty << 2) + index;

		switch  (pos) {

		case EDGE_BOTTOM: {
			final int py = temp[index] - ty;

			if (py > 7) {
				return 0L;
			} else if (py <= 0) {
				return -1L;
			} else {
				return leftMask(index);
			}
		}

		case EDGE_LEFT: {
			final int px = temp[index] - tx;

			if (px > 7) {
				return 0L;
			} else if (px <= 0) {
				return -1L;
			} else {
				return leftMask(index);
			}
		}

		case EDGE_TOP_LEFT: {
			if(events[yIndex] > tx + 7) {
				return 0L;
			} else if (events[yIndex + 28] <= tx) {
				return -1L;
			} else {
				return leftMask(index);
			}
		}

		case EDGE_BOTTOM_LEFT: {
			if(events[yIndex + 28] > tx + 7) {
				return 0L;
			} else if (events[yIndex] <= tx) {
				return -1L;
			} else {
				return leftMask(index);
			}
		}

		case EDGE_TOP:{
			final int py = temp[index] - ty;

			if (py < 0) {
				return 0L;
			} else if (py >= 7) {
				return -1L;
			} else {
				return rightMask(index);
			}
		}

		case EDGE_RIGHT: {
			final int px = temp[index] - tx;

			if (px < 0) {
				return 0L;
			} else if (px >= 7) {
				return -1L;
			} else {
				return rightMask(index);
			}
		}

		case EDGE_TOP_RIGHT:  {
			if(events[yIndex] < tx) {
				return 0L;
			} else if (events[yIndex + 28] >= tx + 7) {
				return -1L;
			} else {
				return rightMask(index);
			}
		}

		case EDGE_BOTTOM_RIGHT: {
			if(events[yIndex + 28] < tx) {
				return 0;
			} else if (events[yIndex] >= tx + 7) {
				return -1L;
			} else {
				return rightMask(index);
			}
		}

		default:
			assert false : "Edge flag out of bounds.";
		}

		return 0L;
	}

	static long rightMask(int index) {
		int ty = tileY << 3;
		final int tx = tileX << 3;
		long mask = 0;
		int yShift = 0;
		ty = (ty << 2) + index;

		final int limit = ty + 32;

		final int baseX = 7 + tx;

		for (int y = ty; y < limit; y += 4) {
			final int x = baseX - events[y];

			if (x <= 0) {
				mask |= 0xFFL << yShift;
			} else if (x < 8){
				mask |= (0xFFL >> x) << yShift;
			}

			yShift += 8;
		}

		return mask;
	}

	static long leftMask(int index) {
		int ty = tileY << 3;
		final int tx = tileX << 3;
		long mask = 0;
		int yShift = 0;
		ty = (ty << 2) + index;

		final int limit = ty + 32;

		for (int y = ty; y < limit; y += 4) {
			final int x = events[y] - tx;

			if (x <= 0) {
				mask |= 0xFFL << yShift;
			} else if (x < 8){
				mask |= ((0xFFL << x) & 0xFFL) << yShift;
			}

			yShift += 8;
		}

		return mask;
	}

	// TODO: remove
	static long buildTileMaskOld(int pos, int index) {
		int ty = tileY << 3;

		if (ty > maxPixelY || ty + 7 < minPixelY) {
			return 0L;
		}

		final int tx = tileX << 3;

		if (tx > maxPixelX || tx + 7 < minPixelX) {
			return 0L;
		}

		long mask = 0;
		int yShift = 0;
		ty = (ty << 2) + index;
		final int limit = ty + 32;

		switch  (pos) {

		case EDGE_BOTTOM:
		case EDGE_LEFT:
		case EDGE_TOP_LEFT:
		case EDGE_BOTTOM_LEFT: {
			for (int y = ty; y < limit; y += 4) {
				final int x = events[y] - tx;

				if (x <= 0) {
					mask |= 0xFFL << yShift;
				} else if (x < 8){
					mask |= ((0xFFL << x) & 0xFFL) << yShift;
				}

				yShift += 8;
			}

			break;
		}

		case EDGE_TOP:
		case EDGE_RIGHT:
		case EDGE_TOP_RIGHT:
		case EDGE_BOTTOM_RIGHT: {
			final int baseX = 7 + tx;

			for (int y = ty; y < limit; y += 4) {
				final int x = baseX - events[y];

				if (x <= 0) {
					mask |= 0xFFL << yShift;
				} else if (x < 8){
					mask |= (0xFFL >> x) << yShift;
				}

				yShift += 8;
			}

			break;
		}

		default:
			assert false : "Edge flag out of bounds.";
		}

		return mask;
	}
}