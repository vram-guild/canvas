package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM_RIGHT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_RIGHT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP_RIGHT;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.TILE_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Constants.TILE_INDEX_HIGH_X;
import static grondag.canvas.chunk.occlusion.Constants.TILE_INDEX_HIGH_Y;
import static grondag.canvas.chunk.occlusion.Constants.TILE_INDEX_LOW_X_MASK;
import static grondag.canvas.chunk.occlusion.Constants.TILE_INDEX_LOW_Y;
import static grondag.canvas.chunk.occlusion.Constants.TILE_INDEX_LOW_Y_MASK;
import static grondag.canvas.chunk.occlusion.Data.events;
import static grondag.canvas.chunk.occlusion.Data.events2;
import static grondag.canvas.chunk.occlusion.Data.position0;
import static grondag.canvas.chunk.occlusion.Data.position1;
import static grondag.canvas.chunk.occlusion.Data.position2;
import static grondag.canvas.chunk.occlusion.Data.save_tileIndex;
import static grondag.canvas.chunk.occlusion.Data.save_tileOriginX;
import static grondag.canvas.chunk.occlusion.Data.save_tileOriginY;
import static grondag.canvas.chunk.occlusion.Data.temp;
import static grondag.canvas.chunk.occlusion.Data.tileIndex;
import static grondag.canvas.chunk.occlusion.Data.tileOriginX;
import static grondag.canvas.chunk.occlusion.Data.tileOriginY;
import static grondag.canvas.chunk.occlusion.Indexer.tileIndex;

abstract class Tile {
	private Tile() {}

	static void moveTileRight() {
		tileOriginX += 8;

		if ((tileIndex & TILE_INDEX_LOW_X_MASK) == TILE_INDEX_LOW_X_MASK) {
			tileIndex = (tileIndex & ~TILE_INDEX_LOW_X_MASK) + TILE_INDEX_HIGH_X;
		} else {
			tileIndex += 1;
		}

		assert tileIndex == tileIndex(tileOriginX >> TILE_AXIS_SHIFT, tileOriginY >> TILE_AXIS_SHIFT);
		assert tileOriginX < PIXEL_WIDTH;
	}

	static void moveTileLeft() {
		tileOriginX -= 8;

		if ((tileIndex & TILE_INDEX_LOW_X_MASK) == 0) {
			tileIndex |= TILE_INDEX_LOW_X_MASK;
			tileIndex -= TILE_INDEX_HIGH_X;
		} else {
			tileIndex -= 1;
		}

		assert tileIndex == tileIndex(tileOriginX >> TILE_AXIS_SHIFT, tileOriginY >> TILE_AXIS_SHIFT);
		assert tileOriginX >= 0;
	}

	static void moveTileUp() {
		tileOriginY += 8;

		if ((tileIndex & TILE_INDEX_LOW_Y_MASK) == TILE_INDEX_LOW_Y_MASK) {
			tileIndex = (tileIndex & ~TILE_INDEX_LOW_Y_MASK) + TILE_INDEX_HIGH_Y;
		} else {
			tileIndex += TILE_INDEX_LOW_Y;
		}

		assert tileIndex == tileIndex(tileOriginX >> TILE_AXIS_SHIFT, tileOriginY >> TILE_AXIS_SHIFT);
		assert tileOriginY < PIXEL_HEIGHT;
	}


	static long computeTileCoverageOld() {
		return buildTileMask(position0, 0)
				& buildTileMask(position1, 1)
				& buildTileMask(position2, 2);
	}

	static void pushTile() {
		save_tileOriginX = tileOriginX;
		save_tileOriginY = tileOriginY;
		save_tileIndex = tileIndex;
	}

	static void popTile() {
		tileOriginX = save_tileOriginX;
		tileOriginY = save_tileOriginY;
		tileIndex = save_tileIndex;
	}


	//	static long computeTileCoverageTest() {
	//		final long  oldResult = computeTileCoverageOld();
	//		final long  newResult = computeTileCoverage();
	//
	//		if (oldResult != newResult) {
	//			System.out.println();
	//			System.out.println("OLD  0: " + position0);
	//			printMask8x8(buildTileMask(position0, 0));
	//			System.out.println("OLD  1: " + position1);
	//			printMask8x8(buildTileMask(position1, 1));
	//			System.out.println("OLD  2: " + position2);
	//			printMask8x8(buildTileMask(position2, 2));
	//			System.out.println("OLD  COMBINED");
	//			printMask8x8(oldResult);
	//			System.out.println("NEW");
	//			printMask8x8(newResult);
	//			computeTileCoverage();
	//		}
	//
	//		return oldResult;
	//	}

	static long buildTileMask(int pos, int index) {
		final int ty = tileOriginY;
		final int tx = tileOriginX;
		final int yIndex = ((ty & ~7) << 2) + (index << 3) + (ty & 7);

		switch  (pos) {

		case EDGE_BOTTOM: {
			final int py = temp[index] - ty;

			if (py > 7) {
				return 0L;
			} else if (py <= 0) {
				return -1L;
			} else {
				return leftMask(yIndex);
			}
		}

		case EDGE_LEFT: {
			final int px = temp[index] - tx;

			if (px > 7) {
				return 0L;
			} else if (px <= 0) {
				return -1L;
			} else {
				return leftMask(yIndex);
			}
		}

		case EDGE_TOP_LEFT: {
			if(events[yIndex] > tx + 7) {
				return 0L;
			} else if (events[yIndex + 7] <= tx) {
				return -1L;
			} else {
				return leftMask(yIndex);
			}
		}

		case EDGE_BOTTOM_LEFT: {
			if(events[yIndex + 7] > tx + 7) {
				return 0L;
			} else if (events[yIndex] <= tx) {
				return -1L;
			} else {
				return leftMask(yIndex);
			}
		}

		case EDGE_TOP:{
			final int py = temp[index] - ty;

			if (py < 0) {
				return 0L;
			} else if (py >= 7) {
				return -1L;
			} else {
				return rightMask(yIndex);
			}
		}

		case EDGE_RIGHT: {
			final int px = temp[index] - tx;

			if (px < 0) {
				return 0L;
			} else if (px >= 7) {
				return -1L;
			} else {
				return rightMask(yIndex);
			}
		}

		case EDGE_TOP_RIGHT:  {
			if(events[yIndex] < tx) {
				return 0L;
			} else if (events[yIndex + 7] >= tx + 7) {
				return -1L;
			} else {
				return rightMask(yIndex);
			}
		}

		case EDGE_BOTTOM_RIGHT: {
			if(events[yIndex + 7] < tx) {
				return 0;
			} else if (events[yIndex] >= tx + 7) {
				return -1L;
			} else {
				return rightMask(yIndex);
			}
		}

		default:
			assert false : "Edge flag out of bounds.";
		}

		return 0L;
	}

	static long rightMask(int y) {
		final int baseX = tileOriginX + 7;

		long mask = 0;

		int x = baseX - events[y];

		if (x <= 0) {
			mask |= 0xFFL;
		} else if (x < 8){
			mask |= (0xFFL >> x);
		}

		x = baseX - events[++y];

		if (x <= 0) {
			mask |= 0xFF00L;
		} else if (x < 8){
			mask |= (0xFFL >> x) << 8;
		}

		x = baseX - events[++y];

		if (x <= 0) {
			mask |= 0xFF0000L;
		} else if (x < 8){
			mask |= (0xFFL >> x) << 16;
		}

		x = baseX - events[++y];

		if (x <= 0) {
			mask |= 0xFF000000L;
		} else if (x < 8){
			mask |= (0xFFL >> x) << 24;
		}

		x = baseX - events[++y];

		if (x <= 0) {
			mask |= 0xFF00000000L;
		} else if (x < 8){
			mask |= (0xFFL >> x) << 32;
		}

		x = baseX - events[++y];

		if (x <= 0) {
			mask |= 0xFF0000000000L;
		} else if (x < 8){
			mask |= (0xFFL >> x) << 40;
		}

		x = baseX - events[++y];

		if (x <= 0) {
			mask |= 0xFF000000000000L;
		} else if (x < 8){
			mask |= (0xFFL >> x) << 48;
		}

		x = baseX - events[++y];

		if (x <= 0) {
			mask |= 0xFF00000000000000L;
		} else if (x < 8){
			mask |= (0xFFL >> x) << 56;
		}

		return mask;
	}

	static long leftMask(int y) {
		final int baseX = tileOriginX;

		long mask = 0;

		int x = events[y] - baseX;

		if (x <= 0) {
			mask |= 0xFFL;
		} else if (x < 8){
			mask |= ((0xFFL << x) & 0xFFL);
		}

		x = events[++y] - baseX;

		// PERF shift left from left end and then shift right to avoid a mask in most cases
		if (x <= 0) {
			mask |= 0xFF00L;
		} else if (x < 8){
			mask |= ((0xFFL << x) & 0xFFL) << 8;
		}

		x = events[++y] - baseX;

		if (x <= 0) {
			mask |= 0xFF0000L;
		} else if (x < 8){
			mask |= ((0xFFL << x) & 0xFFL) << 16;
		}

		x = events[++y] - baseX;

		if (x <= 0) {
			mask |= 0xFF000000L;
		} else if (x < 8){
			mask |= ((0xFFL << x) & 0xFFL) << 24;
		}

		x = events[++y] - baseX;

		if (x <= 0) {
			mask |= 0xFF00000000L;
		} else if (x < 8){
			mask |= ((0xFFL << x) & 0xFFL) << 32;
		}

		x = events[++y] - baseX;

		if (x <= 0) {
			mask |= 0xFF0000000000L;
		} else if (x < 8){
			mask |= ((0xFFL << x) & 0xFFL) << 40;
		}

		x = events[++y] - baseX;

		if (x <= 0) {
			mask |= 0xFF000000000000L;
		} else if (x < 8){
			mask |= ((0xFFL << x) & 0xFFL) << 48;
		}

		x = events[++y] - baseX;

		if (x <= 0) {
			mask |= 0xFF00000000000000L;
		} else if (x < 8){
			mask |= ((0xFFL << x) & 0xFFL) << 56;
		}

		return mask;
	}

	static long computeTileCoverageNew() {
		final int[] e = events2;

		int y = tileOriginY << 1;
		final int tx = tileOriginX;

		final int baseX = tileOriginX + 7;

		long mask = 0;

		int leftX = e[y] - tx;
		int rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask = m;
		}


		leftX = e[++y] - tx;
		rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 8;
		}


		leftX = e[++y] - tx;
		rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 16;
		}


		leftX = e[++y] - tx;
		rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 24;
		}


		leftX = e[++y] - tx;
		rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 32;
		}


		leftX = e[++y] - tx;
		rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 40;
		}


		leftX = e[++y] - tx;
		rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 48;
		}


		leftX = e[++y] - tx;
		rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 56;
		}


		return mask;
	}

	static long computeTileCoverage() {
		final int[] e = events2;

		int y = tileOriginY << 1;
		final int tx = tileOriginX;

		final int baseX = tileOriginX + 7;

		long mask = 0;

		int leftX = e[y] - tx;
		int rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask = m;
		}


		leftX = e[++y] - tx;
		rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 8;
		}


		leftX = e[++y] - tx;
		rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 16;
		}


		leftX = e[++y] - tx;
		rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 24;
		}


		leftX = e[++y] - tx;
		rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 32;
		}


		leftX = e[++y] - tx;
		rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 40;
		}


		leftX = e[++y] - tx;
		rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 48;
		}


		leftX = e[++y] - tx;
		rightX = baseX - e[++y];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 56;
		}


		return mask;
	}
}