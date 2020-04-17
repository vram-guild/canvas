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
import static grondag.canvas.chunk.occlusion.Constants.INSIDE;
import static grondag.canvas.chunk.occlusion.Constants.INTERSECTING;
import static grondag.canvas.chunk.occlusion.Constants.OUTSIDE;
import static grondag.canvas.chunk.occlusion.Constants.TILE_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Data.a0;
import static grondag.canvas.chunk.occlusion.Data.a1;
import static grondag.canvas.chunk.occlusion.Data.a2;
import static grondag.canvas.chunk.occlusion.Data.b0;
import static grondag.canvas.chunk.occlusion.Data.b1;
import static grondag.canvas.chunk.occlusion.Data.b2;
import static grondag.canvas.chunk.occlusion.Data.hiCornerW0;
import static grondag.canvas.chunk.occlusion.Data.hiCornerW1;
import static grondag.canvas.chunk.occlusion.Data.hiCornerW2;
import static grondag.canvas.chunk.occlusion.Data.hiExtent0;
import static grondag.canvas.chunk.occlusion.Data.hiExtent1;
import static grondag.canvas.chunk.occlusion.Data.hiExtent2;
import static grondag.canvas.chunk.occlusion.Data.hiSpanA0;
import static grondag.canvas.chunk.occlusion.Data.hiSpanA1;
import static grondag.canvas.chunk.occlusion.Data.hiSpanA2;
import static grondag.canvas.chunk.occlusion.Data.hiSpanB0;
import static grondag.canvas.chunk.occlusion.Data.hiSpanB1;
import static grondag.canvas.chunk.occlusion.Data.hiSpanB2;
import static grondag.canvas.chunk.occlusion.Data.hiTileA0;
import static grondag.canvas.chunk.occlusion.Data.hiTileA1;
import static grondag.canvas.chunk.occlusion.Data.hiTileA2;
import static grondag.canvas.chunk.occlusion.Data.hiTileB0;
import static grondag.canvas.chunk.occlusion.Data.hiTileB1;
import static grondag.canvas.chunk.occlusion.Data.hiTileB2;
import static grondag.canvas.chunk.occlusion.Data.lowCornerW0;
import static grondag.canvas.chunk.occlusion.Data.lowCornerW1;
import static grondag.canvas.chunk.occlusion.Data.lowCornerW2;
import static grondag.canvas.chunk.occlusion.Data.lowExtent0;
import static grondag.canvas.chunk.occlusion.Data.lowExtent1;
import static grondag.canvas.chunk.occlusion.Data.lowExtent2;
import static grondag.canvas.chunk.occlusion.Data.lowSpanA0;
import static grondag.canvas.chunk.occlusion.Data.lowSpanA1;
import static grondag.canvas.chunk.occlusion.Data.lowSpanA2;
import static grondag.canvas.chunk.occlusion.Data.lowSpanB0;
import static grondag.canvas.chunk.occlusion.Data.lowSpanB1;
import static grondag.canvas.chunk.occlusion.Data.lowSpanB2;
import static grondag.canvas.chunk.occlusion.Data.lowTileA0;
import static grondag.canvas.chunk.occlusion.Data.lowTileA1;
import static grondag.canvas.chunk.occlusion.Data.lowTileA2;
import static grondag.canvas.chunk.occlusion.Data.lowTileB0;
import static grondag.canvas.chunk.occlusion.Data.lowTileB1;
import static grondag.canvas.chunk.occlusion.Data.lowTileB2;
import static grondag.canvas.chunk.occlusion.Data.lowTileX;
import static grondag.canvas.chunk.occlusion.Data.lowTileY;
import static grondag.canvas.chunk.occlusion.Data.midTileX;
import static grondag.canvas.chunk.occlusion.Data.midTileY;
import static grondag.canvas.chunk.occlusion.Data.position0;
import static grondag.canvas.chunk.occlusion.Data.position1;
import static grondag.canvas.chunk.occlusion.Data.position2;
import static grondag.canvas.chunk.occlusion.Data.positionHi0;
import static grondag.canvas.chunk.occlusion.Data.positionHi1;
import static grondag.canvas.chunk.occlusion.Data.positionHi2;
import static grondag.canvas.chunk.occlusion.Data.positionLow0;
import static grondag.canvas.chunk.occlusion.Data.positionLow1;
import static grondag.canvas.chunk.occlusion.Data.positionLow2;
import static grondag.canvas.chunk.occlusion.Data.save_hiCornerW0;
import static grondag.canvas.chunk.occlusion.Data.save_hiCornerW1;
import static grondag.canvas.chunk.occlusion.Data.save_hiCornerW2;
import static grondag.canvas.chunk.occlusion.Data.save_lowCornerW0;
import static grondag.canvas.chunk.occlusion.Data.save_lowCornerW1;
import static grondag.canvas.chunk.occlusion.Data.save_lowCornerW2;
import static grondag.canvas.chunk.occlusion.Data.save_lowTileX;
import static grondag.canvas.chunk.occlusion.Data.save_lowTileY;
import static grondag.canvas.chunk.occlusion.Data.save_midTileX;
import static grondag.canvas.chunk.occlusion.Data.save_midTileY;
import static grondag.canvas.chunk.occlusion.Data.save_positionHi0;
import static grondag.canvas.chunk.occlusion.Data.save_positionHi1;
import static grondag.canvas.chunk.occlusion.Data.save_positionHi2;
import static grondag.canvas.chunk.occlusion.Data.save_positionLow0;
import static grondag.canvas.chunk.occlusion.Data.save_positionLow1;
import static grondag.canvas.chunk.occlusion.Data.save_positionLow2;

abstract class Tile {
	private Tile() {}

	static void moveLowTileToParentOrigin() {
		lowTileX = midTileX << TILE_AXIS_SHIFT;
		lowTileY = midTileY << TILE_AXIS_SHIFT;

		final int highW0 = hiCornerW0 - ((position0 & A_POSITIVE) == 0 ? 0 : hiSpanA0) - ((position0 & B_POSITIVE) == 0 ? 0 : hiSpanB0);
		lowCornerW0 = highW0 + ((position0 & A_POSITIVE) == 0 ? 0 : lowSpanA0) + ((position0 & B_POSITIVE) == 0 ? 0 : lowSpanB0);
		positionLow0 = lowCornerW0 < 0 ? OUTSIDE : lowCornerW0 >= lowExtent0 ? INSIDE : INTERSECTING;

		final int highW1 = hiCornerW1 - ((position1 & A_POSITIVE) == 0 ? 0 : hiSpanA1) - ((position1 & B_POSITIVE) == 0 ? 0 : hiSpanB1);
		lowCornerW1 = highW1 + ((position1 & A_POSITIVE) == 0 ? 0 : lowSpanA1) + ((position1 & B_POSITIVE) == 0 ? 0 : lowSpanB1);
		positionLow1 = lowCornerW1 < 0 ? OUTSIDE : lowCornerW1 >= lowExtent1 ? INSIDE : INTERSECTING;

		final int highW2 = hiCornerW2 - ((position2 & A_POSITIVE) == 0 ? 0 : hiSpanA2) - ((position2 & B_POSITIVE) == 0 ? 0 : hiSpanB2);
		lowCornerW2 = highW2 + ((position2 & A_POSITIVE) == 0 ? 0 : lowSpanA2) + ((position2 & B_POSITIVE) == 0 ? 0 : lowSpanB2);
		positionLow2 = lowCornerW2 < 0 ? OUTSIDE : lowCornerW2 >= lowExtent2 ? INSIDE : INTERSECTING;
	}

	static void moveMidTileRight() {
		++midTileX;

		hiCornerW0 += hiTileA0;
		hiCornerW1 += hiTileA1;
		hiCornerW2 += hiTileA2;

		if ((positionHi0 != OUTSIDE && (position0 & A_NEGATIVE) != 0) || (positionHi0 != INSIDE && (position0 & A_POSITIVE) != 0)) {
			positionHi0 = hiCornerW0 < 0 ? OUTSIDE : hiCornerW0 >= hiExtent0 ? INSIDE : INTERSECTING;
		}

		if ((positionHi1 != OUTSIDE && (position1 & A_NEGATIVE) != 0) || (positionHi1 != INSIDE && (position1 & A_POSITIVE) != 0)) {
			positionHi1 = hiCornerW1 < 0 ? OUTSIDE : hiCornerW1 >= hiExtent1 ? INSIDE : INTERSECTING;
		}

		if ((positionHi2 != OUTSIDE && (position2 & A_NEGATIVE) != 0) || (positionHi2 != INSIDE && (position2 & A_POSITIVE) != 0)) {
			positionHi2 = hiCornerW2 < 0 ? OUTSIDE : hiCornerW2 >= hiExtent2 ? INSIDE : INTERSECTING;
		}
	}

	static void moveLowTileRight() {
		++lowTileX;

		lowCornerW0 += lowTileA0;
		lowCornerW1 += lowTileA1;
		lowCornerW2 += lowTileA2;

		if ((positionLow0 != OUTSIDE && (position0 & A_NEGATIVE) != 0) || (positionLow0 != INSIDE && (position0 & A_POSITIVE) != 0)) {
			positionLow0 = lowCornerW0 < 0 ? OUTSIDE : lowCornerW0 >= lowExtent0 ? INSIDE : INTERSECTING;
		}

		if ((positionLow1 != OUTSIDE && (position1 & A_NEGATIVE) != 0) || (positionLow1 != INSIDE && (position1 & A_POSITIVE) != 0)) {
			positionLow1 = lowCornerW1 < 0 ? OUTSIDE : lowCornerW1 >= lowExtent1 ? INSIDE : INTERSECTING;
		}

		if ((positionLow2 != OUTSIDE && (position2 & A_NEGATIVE) != 0) || (positionLow2 != INSIDE && (position2 & A_POSITIVE) != 0)) {
			positionLow2 = lowCornerW2 < 0 ? OUTSIDE : lowCornerW2 >= lowExtent2 ? INSIDE : INTERSECTING;
		}
	}

	static void moveMidTileLeft() {
		--midTileX;

		hiCornerW0 -= hiTileA0;
		hiCornerW1 -= hiTileA1;
		hiCornerW2 -= hiTileA2;

		if ((positionHi0 != OUTSIDE && (position0 & A_POSITIVE) != 0) || (positionHi0 != INSIDE && (position0 & A_NEGATIVE) != 0)) {
			positionHi0 = hiCornerW0 < 0 ? OUTSIDE : hiCornerW0 >= hiExtent0 ? INSIDE : INTERSECTING;
		}

		if ((positionHi1 != OUTSIDE && (position1 & A_POSITIVE) != 0) || (positionHi1 != INSIDE && (position1 & A_NEGATIVE) != 0)) {
			positionHi1 = hiCornerW1 < 0 ? OUTSIDE : hiCornerW1 >= hiExtent1 ? INSIDE : INTERSECTING;
		}

		if ((positionHi2 != OUTSIDE && (position2 & A_POSITIVE) != 0) || (positionHi2 != INSIDE && (position2 & A_NEGATIVE) != 0)) {
			positionHi2 = hiCornerW2 < 0 ? OUTSIDE : hiCornerW2 >= hiExtent2 ? INSIDE : INTERSECTING;
		}
	}

	static void moveLowTileLeft() {
		--lowTileX;

		lowCornerW0 -= lowTileA0;
		lowCornerW1 -= lowTileA1;
		lowCornerW2 -= lowTileA2;

		if ((positionLow0 != OUTSIDE && (position0 & A_POSITIVE) != 0) || (positionLow0 != INSIDE && (position0 & A_NEGATIVE) != 0)) {
			positionLow0 = lowCornerW0 < 0 ? OUTSIDE : lowCornerW0 >= lowExtent0 ? INSIDE : INTERSECTING;
		}

		if ((positionLow1 != OUTSIDE && (position1 & A_POSITIVE) != 0) || (positionLow1 != INSIDE && (position1 & A_NEGATIVE) != 0)) {
			positionLow1 = lowCornerW1 < 0 ? OUTSIDE : lowCornerW1 >= lowExtent1 ? INSIDE : INTERSECTING;
		}

		if ((positionLow2 != OUTSIDE && (position2 & A_POSITIVE) != 0) || (positionLow2 != INSIDE && (position2 & A_NEGATIVE) != 0)) {
			positionLow2 = lowCornerW2 < 0 ? OUTSIDE : lowCornerW2 >= lowExtent2 ? INSIDE : INTERSECTING;
		}
	}

	static void moveMidTileUp() {
		++midTileY;

		hiCornerW0 += hiTileB0;
		hiCornerW1 += hiTileB1;
		hiCornerW2 += hiTileB2;

		if ((positionHi0 != OUTSIDE && (position0 & B_NEGATIVE) != 0) || (positionHi0 != INSIDE && (position0 & B_POSITIVE) != 0)) {
			positionHi0 = hiCornerW0 < 0 ? OUTSIDE : hiCornerW0 >= hiExtent0 ? INSIDE : INTERSECTING;
		}

		if ((positionHi1 != OUTSIDE && (position1 & B_NEGATIVE) != 0) || (positionHi1 != INSIDE && (position1 & B_POSITIVE) != 0)) {
			positionHi1 = hiCornerW1 < 0 ? OUTSIDE : hiCornerW1 >= hiExtent1 ? INSIDE : INTERSECTING;
		}

		if ((positionHi2 != OUTSIDE && (position2 & B_NEGATIVE) != 0) || (positionHi2 != INSIDE && (position2 & B_POSITIVE) != 0)) {
			positionHi2 = hiCornerW2 < 0 ? OUTSIDE : hiCornerW2 >= hiExtent2 ? INSIDE : INTERSECTING;
		}
	}

	static void moveLowTileUp() {
		++lowTileY;

		lowCornerW0 += (b0 + lowSpanB0);
		lowCornerW1 += (b1 + lowSpanB1);
		lowCornerW2 += (b2 + lowSpanB2);

		if ((positionLow0 != OUTSIDE && (position0 & B_NEGATIVE) != 0) || (positionLow0 != INSIDE && (position0 & B_POSITIVE) != 0)) {
			positionLow0 = lowCornerW0 < 0 ? OUTSIDE : lowCornerW0 >= lowExtent0 ? INSIDE : INTERSECTING;
		}

		if ((positionLow1 != OUTSIDE && (position1 & B_NEGATIVE) != 0) || (positionLow1 != INSIDE && (position1 & B_POSITIVE) != 0)) {
			positionLow1 = lowCornerW1 < 0 ? OUTSIDE : lowCornerW1 >= lowExtent1 ? INSIDE : INTERSECTING;
		}

		if ((positionLow2 != OUTSIDE && (position2 & B_NEGATIVE) != 0) || (positionLow2 != INSIDE && (position2 & B_POSITIVE) != 0)) {
			positionLow2 = lowCornerW2 < 0 ? OUTSIDE : lowCornerW2 >= lowExtent2 ? INSIDE : INTERSECTING;
		}
	}

	static long computeLowTileCoverage() {
		// PERF: use switch somehow - only 27 possible outcomes
		final int c0 = positionLow0;

		if (c0 == OUTSIDE) {
			return 0L;
		}

		final int c1 = positionLow1;

		if (c1 == OUTSIDE) {
			return 0L;
		}

		final int c2 = positionLow2;

		if (c2 == OUTSIDE) {
			return 0L;
		}

		if ((c0 | c1 | c2) == INSIDE)  {
			return -1L;
		}

		long result = -1L;

		if (c0 == INTERSECTING) {
			result &= buildMask(position0, a0, b0, lowCornerW0);
		}

		if (c1 == INTERSECTING) {
			result &= buildMask(position1, a1, b1, lowCornerW1);
		}

		if (c2 == INTERSECTING) {
			result &= buildMask(position2, a2, b2, lowCornerW2);
		}

		return result;
	}

	static long computeMidTileCoverage() {
		final int c0 = positionHi0;

		if (c0 == OUTSIDE) {
			return 0L;
		}

		final int c1 = positionHi1;

		if (c1 == OUTSIDE) {
			return 0L;
		}

		final int c2 = positionHi2;

		if (c2 == OUTSIDE) {
			return 0L;
		}

		if ((c0 | c1 | c2) == INSIDE)  {
			return -1L;
		}

		long result = -1L;

		if (c0 == INTERSECTING) {
			result &= buildMask(position0, lowTileA0, lowTileB0, hiCornerW0);
		}

		if (c1 == INTERSECTING) {
			result &= buildMask(position1, lowTileA1, lowTileB1, hiCornerW1);
		}

		if (c2 == INTERSECTING) {
			result &= buildMask(position2, lowTileA2, lowTileB2, hiCornerW2);
		}

		return result;
	}

	static void pushMidTile() {
		save_midTileX = midTileX;
		save_midTileY = midTileY;
		save_hiCornerW0 = hiCornerW0;
		save_hiCornerW1 = hiCornerW1;
		save_hiCornerW2 = hiCornerW2;
		save_positionHi0 = positionHi0;
		save_positionHi1 = positionHi1;
		save_positionHi2 = positionHi2;
	}

	static void pushLowTile() {
		save_lowTileX = lowTileX;
		save_lowTileY = lowTileY;
		save_lowCornerW0 = lowCornerW0;
		save_lowCornerW1 = lowCornerW1;
		save_lowCornerW2 = lowCornerW2;
		save_positionLow0 = positionLow0;
		save_positionLow1 = positionLow1;
		save_positionLow2 = positionLow2;
	}

	static void popMidTile() {
		midTileX = save_midTileX;
		midTileY = save_midTileY;
		hiCornerW0 = save_hiCornerW0;
		hiCornerW1 = save_hiCornerW1;
		hiCornerW2 = save_hiCornerW2;
		positionHi0 = save_positionHi0;
		positionHi1 = save_positionHi1;
		positionHi2 = save_positionHi2;
	}

	static void popLowTile() {
		lowTileX = save_lowTileX;
		lowTileY = save_lowTileY;
		lowCornerW0 = save_lowCornerW0;
		lowCornerW1 = save_lowCornerW1;
		lowCornerW2 = save_lowCornerW2;
		positionLow0 = save_positionLow0;
		positionLow1 = save_positionLow1;
		positionLow2 = save_positionLow2;
	}

	static long buildMask(int pos, int stepA, int stepB, int wy) {

		switch  (pos) {
		case EDGE_TOP: {
			assert wy >= 0;
			assert stepB < 0;

			long yMask = 0xFFL;
			long mask = 0;

			while (wy >= 0 && yMask != 0L) {
				mask |= yMask;
				yMask <<= 8;
				wy += stepB; //NB: b will be negative
			}

			return mask;
		}

		case EDGE_BOTTOM: {
			assert wy >= 0;
			assert stepB > 0;

			long yMask = 0xFF00000000000000L;
			long mask = 0;

			while (wy >= 0 && yMask != 0L) {
				mask |= yMask;
				yMask = (yMask >>> 8); // parens are to help eclipse auto-formatting
				wy -= stepB;
			}

			return mask;
		}

		case EDGE_RIGHT: {
			assert wy >= 0;
			assert stepA < 0;

			final int x = 7 - Math.min(7, -wy / stepA);
			long mask = (0xFF >> x);

			mask |= mask << 8;
			mask |= mask << 16;
			mask |= mask << 32;

			return mask;
		}

		case EDGE_LEFT: {
			assert wy >= 0;
			assert stepA > 0;

			final int x =  7 - Math.min(7, wy / stepA);
			long mask = (0xFF << x) & 0xFF;

			mask |= mask << 8;
			mask |= mask << 16;
			mask |= mask << 32;

			return mask;
		}

		case EDGE_TOP_LEFT: {
			// PERF: optimize case when shallow slope and several bottom rows are full

			assert wy >= 0;
			assert stepB < 0;
			assert stepA > 0;

			// min y will occur at x = 0;

			long mask = 0;
			int yShift = 0;

			while (yShift < 64 && wy >= 0) {
				// x  here is first not last
				final int x =  7 - Math.min(7, wy / stepA);
				final int yMask = (0xFF << x) & 0xFF;
				mask |= ((long) yMask) << yShift;
				wy += stepB; //NB: b will be negative
				yShift += 8;
			}

			return mask;
		}

		case EDGE_BOTTOM_LEFT: {
			assert wy >= 0;
			assert stepB > 0;
			assert stepA > 0;

			// min y will occur at x = 7;

			int yShift = 8 * 7;
			long mask = 0;

			while (yShift >= 0 && wy >= 0) {
				// x  here is first not last
				final int x =  7 - Math.min(7, wy / stepA);
				final int yMask = (0xFF << x) & 0xFF;
				mask |= ((long) yMask) << yShift;
				wy -= stepB;
				yShift -= 8;
			}

			return mask;
		}

		case EDGE_TOP_RIGHT: {
			// PERF: optimize case when shallow slope and several bottom rows are full

			// max y will occur at x = 0
			// Find highest y index of pixels filled at given x.
			// All pixels with lower y value will also be filled in given x.
			// ax + by + c = 0 so y at intersection will be y = -(ax + c) / b
			// Exploit step-wise nature of a/b here to avoid computing the first term
			// logic in other cases is similar
			assert wy >= 0;
			assert stepB < 0;
			assert stepA < 0;

			long mask = 0;
			int yShift = 0;

			while(yShift < 64 && wy >= 0) {
				final int x =  7  - Math.min(7, -wy / stepA);
				final int yMask = (0xFF >> x);
				mask |= ((long) yMask) << yShift;
				wy += stepB;
				yShift +=  8;
			}

			return mask;
		}

		case EDGE_BOTTOM_RIGHT: {
			// PERF: optimize case when shallow slope and several top rows are full

			assert wy >= 0;
			assert stepB > 0;
			assert stepA < 0;

			int yShift = 8 * 7;
			long mask = 0;

			while (yShift >= 0 && wy >= 0) {
				final int x = 7 - Math.min(7, -wy / stepA);
				final int yMask = (0xFF >> x);
				mask |= ((long) yMask) << yShift;
				wy -= stepB;
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