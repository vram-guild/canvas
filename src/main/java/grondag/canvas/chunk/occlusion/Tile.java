package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.PIXEL_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.TILE_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Constants.TILE_INDEX_HIGH_X;
import static grondag.canvas.chunk.occlusion.Constants.TILE_INDEX_HIGH_Y;
import static grondag.canvas.chunk.occlusion.Constants.TILE_INDEX_LOW_X_MASK;
import static grondag.canvas.chunk.occlusion.Constants.TILE_INDEX_LOW_Y;
import static grondag.canvas.chunk.occlusion.Constants.TILE_INDEX_LOW_Y_MASK;
import static grondag.canvas.chunk.occlusion.Data.events;
import static grondag.canvas.chunk.occlusion.Data.save_tileIndex;
import static grondag.canvas.chunk.occlusion.Data.save_tileOriginX;
import static grondag.canvas.chunk.occlusion.Data.save_tileOriginY;
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

	static long computeTileCoverage() {
		final int[] e = events;

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