package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM_RIGHT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_RIGHT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP_RIGHT;
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
import static grondag.canvas.chunk.occlusion.Data.save_tileIndex;
import static grondag.canvas.chunk.occlusion.Data.save_tileX;
import static grondag.canvas.chunk.occlusion.Data.save_tileY;
import static grondag.canvas.chunk.occlusion.Data.temp;
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
	}


	static long computeTileCoverage() {
		return buildTileMask(position0, 0)
				& buildTileMask(position1, 1)
				& buildTileMask(position2, 2);
	}

	static void pushTile() {
		save_tileX = tileX;
		save_tileY = tileY;
		save_tileIndex = tileIndex;
	}

	static void popTile() {
		tileX = save_tileX;
		tileY = save_tileY;
		tileIndex = save_tileIndex;

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