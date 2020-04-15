package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.INSIDE;
import static grondag.canvas.chunk.occlusion.Constants.INTERSECTING;
import static grondag.canvas.chunk.occlusion.Constants.LOW_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Constants.LOW_TILE_PIXEL_DIAMETER;
import static grondag.canvas.chunk.occlusion.Constants.LOW_TILE_SPAN;
import static grondag.canvas.chunk.occlusion.Constants.MID_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Constants.MID_TILE_SPAN;
import static grondag.canvas.chunk.occlusion.Constants.OUTSIDE;
import static grondag.canvas.chunk.occlusion.Constants.TILE_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Data.a0;
import static grondag.canvas.chunk.occlusion.Data.a1;
import static grondag.canvas.chunk.occlusion.Data.a2;
import static grondag.canvas.chunk.occlusion.Data.b0;
import static grondag.canvas.chunk.occlusion.Data.b1;
import static grondag.canvas.chunk.occlusion.Data.b2;
import static grondag.canvas.chunk.occlusion.Data.c0;
import static grondag.canvas.chunk.occlusion.Data.c1;
import static grondag.canvas.chunk.occlusion.Data.c2;
import static grondag.canvas.chunk.occlusion.Data.hiExtent0;
import static grondag.canvas.chunk.occlusion.Data.hiExtent1;
import static grondag.canvas.chunk.occlusion.Data.hiExtent2;
import static grondag.canvas.chunk.occlusion.Data.hiSpanA0;
import static grondag.canvas.chunk.occlusion.Data.hiSpanA1;
import static grondag.canvas.chunk.occlusion.Data.hiSpanA2;
import static grondag.canvas.chunk.occlusion.Data.hiSpanB0;
import static grondag.canvas.chunk.occlusion.Data.hiSpanB1;
import static grondag.canvas.chunk.occlusion.Data.hiSpanB2;
import static grondag.canvas.chunk.occlusion.Data.hiStepA0;
import static grondag.canvas.chunk.occlusion.Data.hiStepA1;
import static grondag.canvas.chunk.occlusion.Data.hiStepA2;
import static grondag.canvas.chunk.occlusion.Data.hiStepB0;
import static grondag.canvas.chunk.occlusion.Data.hiStepB1;
import static grondag.canvas.chunk.occlusion.Data.hiStepB2;
import static grondag.canvas.chunk.occlusion.Data.lowExtent0;
import static grondag.canvas.chunk.occlusion.Data.lowExtent1;
import static grondag.canvas.chunk.occlusion.Data.lowExtent2;
import static grondag.canvas.chunk.occlusion.Data.lowSpanA0;
import static grondag.canvas.chunk.occlusion.Data.lowSpanA1;
import static grondag.canvas.chunk.occlusion.Data.lowSpanA2;
import static grondag.canvas.chunk.occlusion.Data.lowSpanB0;
import static grondag.canvas.chunk.occlusion.Data.lowSpanB1;
import static grondag.canvas.chunk.occlusion.Data.lowSpanB2;
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
import static grondag.canvas.chunk.occlusion.Data.save_x0y0Hi0;
import static grondag.canvas.chunk.occlusion.Data.save_x0y0Hi1;
import static grondag.canvas.chunk.occlusion.Data.save_x0y0Hi2;
import static grondag.canvas.chunk.occlusion.Data.save_x0y0Low0;
import static grondag.canvas.chunk.occlusion.Data.save_x0y0Low1;
import static grondag.canvas.chunk.occlusion.Data.save_x0y0Low2;
import static grondag.canvas.chunk.occlusion.Data.x0y0Hi0;
import static grondag.canvas.chunk.occlusion.Data.x0y0Hi1;
import static grondag.canvas.chunk.occlusion.Data.x0y0Hi2;
import static grondag.canvas.chunk.occlusion.Data.x0y0Low0;
import static grondag.canvas.chunk.occlusion.Data.x0y0Low1;
import static grondag.canvas.chunk.occlusion.Data.x0y0Low2;

abstract class Tile {
	private Tile() {}

	static void prepareLowTile() {
		lowSpanA0 = a0 * LOW_TILE_SPAN;
		lowSpanB0 = b0 * LOW_TILE_SPAN;
		lowExtent0 = Math.abs(lowSpanA0) + Math.abs(lowSpanB0);

		lowSpanA1 = a1 * LOW_TILE_SPAN;
		lowSpanB1 = b1 * LOW_TILE_SPAN;
		lowExtent1 = Math.abs(lowSpanA1) + Math.abs(lowSpanB1);

		lowSpanA2 = a2 * LOW_TILE_SPAN;
		lowSpanB2 = b2 * LOW_TILE_SPAN;
		lowExtent2 = Math.abs(lowSpanA2) + Math.abs(lowSpanB2);
	}

	/**
	 *
	 * Edge functions are line equations: ax + by + c = 0 where c is the origin value
	 * a and b are normal to the line/edge.
	 *
	 * Distance from point to line is given by (ax + by + c) / magnitude
	 * where magnitude is sqrt(a^2 + b^2).
	 *
	 * A tile is fully outside the edge if signed distance less than -extent, where
	 * extent is the 7x7 diagonal vector projected onto the edge normal.
	 *
	 * The length of the extent is given by  (|a| + |b|) * 7 / magnitude.
	 *
	 * Given that magnitude is a common denominator of both the signed distance and the extent
	 * we can avoid computing square root and compare the weight directly with the un-normalized  extent.
	 *
	 * In summary,  if extent e = (|a| + |b|) * 7 and w = ax + by + c then
	 *    when w < -e  tile is fully outside edge
	 *    when w >= 0 tile is fully inside edge (or touching)
	 *    else (-e <= w < 0) tile is intersection (at least one pixel is covered.
	 *
	 * For background, see Real Time Rendering, 4th Ed.  Sec 23.1 on Rasterization, esp. Figure 23.3
	 */
	static void prepareHiTile() {
		hiSpanA0 = a0 * MID_TILE_SPAN;
		hiSpanB0 = b0 * MID_TILE_SPAN;
		hiStepA0 = a0 * LOW_TILE_PIXEL_DIAMETER;
		hiStepB0 = b0 * LOW_TILE_PIXEL_DIAMETER;
		hiExtent0 = Math.abs(hiSpanA0) + Math.abs(hiSpanB0);

		hiSpanA1 = a1 * MID_TILE_SPAN;
		hiSpanB1 = b1 * MID_TILE_SPAN;
		hiStepA1 = a1 * LOW_TILE_PIXEL_DIAMETER;
		hiStepB1 = b1 * LOW_TILE_PIXEL_DIAMETER;
		hiExtent1 = Math.abs(hiSpanA1) + Math.abs(hiSpanB1);

		hiSpanA2 = a2 * MID_TILE_SPAN;
		hiSpanB2 = b2 * MID_TILE_SPAN;
		hiStepA2 = a2 * LOW_TILE_PIXEL_DIAMETER;
		hiStepB2 = b2 * LOW_TILE_PIXEL_DIAMETER;
		hiExtent2 = Math.abs(hiSpanA2) + Math.abs(hiSpanB2);
	}

	static void moveLowTileTo(int tileX, int tileY) {
		lowTileX = tileX;
		lowTileY = tileY;
		final int x = tileX << LOW_AXIS_SHIFT;
		final int y = tileY << LOW_AXIS_SHIFT;
		x0y0Low0 =  c0 + a0 * x + b0 * y;
		x0y0Low1 =  c1 + a1 * x + b1 * y;
		x0y0Low2 =  c2 + a2 * x + b2 * y;
		positionLow0 = classify(position0, x0y0Low0, lowSpanA0, lowSpanB0, lowExtent0);
		positionLow1 = classify(position1, x0y0Low1, lowSpanA1, lowSpanB1, lowExtent1);
		positionLow2 = classify(position2, x0y0Low2, lowSpanA2, lowSpanB2, lowExtent2);
	}

	static void moveMidTileTo(int tileX, int tileY) {
		midTileX = tileX;
		midTileY = tileY;
		final int x = tileX << MID_AXIS_SHIFT;
		final int y = tileY << MID_AXIS_SHIFT;
		x0y0Hi0 =  c0 + a0 * x + b0 * y;
		x0y0Hi1 =  c1 + a1 * x + b1 * y;
		x0y0Hi2 =  c2 + a2 * x + b2 * y;

		positionHi0 = classify(position0, x0y0Hi0, hiSpanA0, hiSpanB0, hiExtent0);
		positionHi1 = classify(position1, x0y0Hi1, hiSpanA1, hiSpanB1, hiExtent1);
		positionHi2 = classify(position2, x0y0Hi2, hiSpanA2, hiSpanB2, hiExtent2);
	}

	static void moveLowTileToParentOrigin() {
		lowTileX = midTileX << TILE_AXIS_SHIFT;
		lowTileY = midTileY << TILE_AXIS_SHIFT;
		x0y0Low0 =  x0y0Hi0;
		x0y0Low1 =  x0y0Hi1;
		x0y0Low2 =  x0y0Hi2;
		positionLow0 = classify(position0, x0y0Low0, lowSpanA0, lowSpanB0, lowExtent0);
		positionLow1 = classify(position1, x0y0Low1, lowSpanA1, lowSpanB1, lowExtent1);
		positionLow2 = classify(position2, x0y0Low2, lowSpanA2, lowSpanB2, lowExtent2);
	}

	static void moveMidTileRight() {
		++midTileX;
		x0y0Hi0 += a0 + hiSpanA0;
		x0y0Hi1 += a1 + hiSpanA1;
		x0y0Hi2 += a2 + hiSpanA2;

		if (updateRightPosition(position0, positionHi0)) {
			positionHi0 = classify(position0, x0y0Hi0, hiSpanA0, hiSpanB0, hiExtent0);
		}

		if (updateRightPosition(position1, positionHi1)) {
			positionHi1 = classify(position1, x0y0Hi1, hiSpanA1, hiSpanB1, hiExtent1);
		}

		if (updateRightPosition(position2, positionHi2)) {
			positionHi2 = classify(position2, x0y0Hi2, hiSpanA2, hiSpanB2, hiExtent2);
		}
	}

	static void moveLowTileRight() {
		++lowTileX;
		x0y0Low0 += a0 + lowSpanA0;
		x0y0Low1 += a1 + lowSpanA1;
		x0y0Low2 += a2 + lowSpanA2;

		if (updateRightPosition(position0, positionLow0)) {
			positionLow0 = classify(position0, x0y0Low0, lowSpanA0, lowSpanB0, lowExtent0);
		}

		if (updateRightPosition(position1, positionLow1)) {
			positionLow1 = classify(position1, x0y0Low1, lowSpanA1, lowSpanB1, lowExtent1);
		}

		if (updateRightPosition(position2, positionLow2)) {
			positionLow2 = classify(position2, x0y0Low2, lowSpanA2, lowSpanB2, lowExtent2);
		}
	}

	static boolean updateRightPosition(EdgePosition edgePos, int currentPosition) {
		if (edgePos.isRight) {
			if (currentPosition != OUTSIDE) {
				return true;
			}
		} else if (edgePos.isLeft && currentPosition != INSIDE) {
			return true;
		}

		return false;
	}

	static void moveMidTileLeft() {
		--midTileX;
		x0y0Hi0 -= (a0 + hiSpanA0);
		x0y0Hi1 -= (a1 + hiSpanA1);
		x0y0Hi2 -= (a2 + hiSpanA2);

		if (updateLeftPosition(position0, positionHi0)) {
			positionHi0 = classify(position0, x0y0Hi0, hiSpanA0, hiSpanB0, hiExtent0);
		}

		if (updateLeftPosition(position1, positionHi1)) {
			positionHi1 = classify(position1, x0y0Hi1, hiSpanA1, hiSpanB1, hiExtent1);
		}

		if (updateLeftPosition(position2, positionHi2)) {
			positionHi2 = classify(position2, x0y0Hi2, hiSpanA2, hiSpanB2, hiExtent2);
		}
	}

	static void moveLowTileLeft() {
		--lowTileX;
		x0y0Low0 -= (a0 + lowSpanA0);
		x0y0Low1 -= (a1 + lowSpanA1);
		x0y0Low2 -= (a2 + lowSpanA2);

		if (updateLeftPosition(position0, positionLow0)) {
			positionLow0 = classify(position0, x0y0Low0, lowSpanA0, lowSpanB0, lowExtent0);
		}

		if (updateLeftPosition(position1, positionLow1)) {
			positionLow1 = classify(position1, x0y0Low1, lowSpanA1, lowSpanB1, lowExtent1);
		}

		if (updateLeftPosition(position2, positionLow2)) {
			positionLow2 = classify(position2, x0y0Low2, lowSpanA2, lowSpanB2, lowExtent2);
		}
	}

	static boolean updateLeftPosition(EdgePosition edgePos, int currentPosition) {
		if (edgePos.isLeft) {
			if (currentPosition != OUTSIDE) {
				return true;
			}
		} else if (edgePos.isRight && currentPosition != INSIDE) {
			return true;
		}

		return false;
	}

	static void moveMidTileUp() {
		++midTileY;
		x0y0Hi0 += (b0 + hiSpanB0);
		x0y0Hi1 += (b1 + hiSpanB1);
		x0y0Hi2 += (b2 + hiSpanB2);

		if (updateTopPosition(position0, positionHi0)) {
			positionHi0 = classify(position0, x0y0Hi0, hiSpanA0, hiSpanB0, hiExtent0);
		}

		if (updateTopPosition(position1, positionHi1)) {
			positionHi1 = classify(position1, x0y0Hi1, hiSpanA1, hiSpanB1, hiExtent1);
		}

		if (updateTopPosition(position2, positionHi2)) {
			positionHi2 = classify(position2, x0y0Hi2, hiSpanA2, hiSpanB2, hiExtent2);
		}
	}

	static void moveLowTileUp() {
		++lowTileY;
		x0y0Low0 += (b0 + lowSpanB0);
		x0y0Low1 += (b1 + lowSpanB1);
		x0y0Low2 += (b2 + lowSpanB2);

		if (updateTopPosition(position0, positionLow0)) {
			positionLow0 = classify(position0, x0y0Low0, lowSpanA0, lowSpanB0, lowExtent0);
		}

		if (updateTopPosition(position1, positionLow1)) {
			positionLow1 = classify(position1, x0y0Low1, lowSpanA1, lowSpanB1, lowExtent1);
		}

		if (updateTopPosition(position2, positionLow2)) {
			positionLow2 = classify(position2, x0y0Low2, lowSpanA2, lowSpanB2, lowExtent2);
		}
	}

	static boolean updateTopPosition(EdgePosition edgePos, int currentPosition) {
		if (edgePos.isTop) {
			if (currentPosition != OUTSIDE) {
				return true;
			}
		} else if (edgePos.isBottom && currentPosition != INSIDE) {
			return true;
		}

		return false;
	}

	static long computeLowTileCoverage() {
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
			result &= buildMask(position0, x0y0Low0, a0, b0,lowSpanA0, lowSpanB0);
		}

		if (c1 == INTERSECTING) {
			result &= buildMask(position1, x0y0Low1, a1, b1,lowSpanA1, lowSpanB1);
		}

		if (c2 == INTERSECTING) {
			result &= buildMask(position2, x0y0Low2, a2, b2,lowSpanA2, lowSpanB2);
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
			result &= buildMask(position0, x0y0Hi0, hiStepA0, hiStepB0, hiSpanA0, hiSpanB0);
		}

		if (c1 == INTERSECTING) {
			result &= buildMask(position1, x0y0Hi1, hiStepA1, hiStepB1, hiSpanA1, hiSpanB1);
		}

		if (c2 == INTERSECTING) {
			result &= buildMask(position2, x0y0Hi2, hiStepA2, hiStepB2, hiSpanA2, hiSpanB2);
		}

		return result;
	}

	static void pushMidTile() {
		save_midTileX = midTileX;
		save_midTileY = midTileY;
		save_x0y0Hi0 = x0y0Hi0;
		save_x0y0Hi1 = x0y0Hi1;
		save_x0y0Hi2 = x0y0Hi2;
		save_positionHi0 = positionHi0;
		save_positionHi1 = positionHi1;
		save_positionHi2 = positionHi2;
	}

	static void pushLowTile() {
		save_lowTileX = lowTileX;
		save_lowTileY = lowTileY;
		save_x0y0Low0 = x0y0Low0;
		save_x0y0Low1 = x0y0Low1;
		save_x0y0Low2 = x0y0Low2;
		save_positionLow0 = positionLow0;
		save_positionLow1 = positionLow1;
		save_positionLow2 = positionLow2;
	}

	static void popMidTile() {
		midTileX = save_midTileX;
		midTileY = save_midTileY;
		x0y0Hi0 = save_x0y0Hi0;
		x0y0Hi1 = save_x0y0Hi1;
		x0y0Hi2 = save_x0y0Hi2;
		positionHi0 = save_positionHi0;
		positionHi1 = save_positionHi1;
		positionHi2 = save_positionHi2;
	}

	static void popLowTile() {
		lowTileX = save_lowTileX;
		lowTileY = save_lowTileY;
		x0y0Low0 = save_x0y0Low0;
		x0y0Low1 = save_x0y0Low1;
		x0y0Low2 = save_x0y0Low2;
		positionLow0 = save_positionLow0;
		positionLow1 = save_positionLow1;
		positionLow2 = save_positionLow2;
	}

	private static int chooseEdgeValue(EdgePosition pos, int x0y0, int spanA, int spanB) {
		switch  (pos) {
		case TOP: // uses x0y0
		case RIGHT: // uses x0y0
		case TOP_RIGHT: // uses  x0y0
			return x0y0;

		case BOTTOM: // uses x0y1
		case BOTTOM_RIGHT: // uses x0y1
			return x0y0 + spanB;

		case BOTTOM_LEFT: // uses x1y1
			return x0y0 + spanA + spanB;

		case LEFT: // uses x1y0
		case TOP_LEFT: // uses x1y0
			return x0y0 + spanA;

		default:
			assert false : "Edge position invalid.";
		return -1;
		}
	}

	private static int classify(EdgePosition pos, int x0y0, int spanA, int spanB, int extent)  {
		final int w = chooseEdgeValue(pos, x0y0, spanA, spanB);

		if (w < 0) {
			// fully outside edge
			return OUTSIDE;
		} else if (w >= extent) {
			// fully inside or touching edge
			return INSIDE;
		} else {
			// intersecting - at least one pixel is set
			return INTERSECTING;
		}
	}

	static long buildMask(EdgePosition pos, int x0y0, int stepA, int stepB, int spanA, int spanB) {

		switch  (pos) {
		case TOP: {
			int wy = x0y0; // bottom left will always be inside
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

		case BOTTOM: {
			int wy = x0y0 + spanB; // top left will always be inside
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

		case RIGHT: {
			final int wy = x0y0; // bottom left will always be inside
			assert wy >= 0;
			assert stepA < 0;

			final int x = 7 - Math.min(7, -wy / stepA);
			long mask = (0xFF >> x);

			mask |= mask << 8;
			mask |= mask << 16;
			mask |= mask << 32;

			return mask;
		}

		case LEFT: {
			final int wy = x0y0 + spanA; // bottom right will always be inside
			assert wy >= 0;
			assert stepA > 0;

			final int x =  7 - Math.min(7, wy / stepA);
			long mask = (0xFF << x) & 0xFF;

			mask |= mask << 8;
			mask |= mask << 16;
			mask |= mask << 32;

			return mask;
		}

		case TOP_LEFT: {
			// PERF: optimize case when shallow slope and several bottom rows are full

			int wy = x0y0 + spanA; // bottom right will always be inside
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

		case BOTTOM_LEFT: {
			int wy = x0y0 + spanA + spanB; // top right will always be inside
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

		case TOP_RIGHT: {
			// PERF: optimize case when shallow slope and several bottom rows are full

			// max y will occur at x = 0
			// Find highest y index of pixels filled at given x.
			// All pixels with lower y value will also be filled in given x.
			// ax + by + c = 0 so y at intersection will be y = -(ax + c) / b
			// Exploit step-wise nature of a/b here to avoid computing the first term
			// logic in other cases is similar
			int wy = x0y0; // bottom left will always be inside
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

		case BOTTOM_RIGHT: {
			// PERF: optimize case when shallow slope and several top rows are full

			int wy = x0y0 + spanB; // top left will always be inside
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