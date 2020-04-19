package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.A_POSITIVE;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_IN;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_NEEDS_CLIP;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_OUTSIDE_OR_TOO_SMALL;
import static grondag.canvas.chunk.occlusion.Constants.B_POSITIVE;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM_RIGHT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP_RIGHT;
import static grondag.canvas.chunk.occlusion.Constants.GUARD_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.GUARD_SIZE;
import static grondag.canvas.chunk.occlusion.Constants.GUARD_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.INSIDE_0;
import static grondag.canvas.chunk.occlusion.Constants.INSIDE_1;
import static grondag.canvas.chunk.occlusion.Constants.INSIDE_2;
import static grondag.canvas.chunk.occlusion.Constants.LOW_AXIS_MASK;
import static grondag.canvas.chunk.occlusion.Constants.LOW_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Constants.MAX_PIXEL_X;
import static grondag.canvas.chunk.occlusion.Constants.MAX_PIXEL_Y;
import static grondag.canvas.chunk.occlusion.Constants.MID_AXIS_MASK;
import static grondag.canvas.chunk.occlusion.Constants.MID_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Constants.OUTSIDE_0;
import static grondag.canvas.chunk.occlusion.Constants.OUTSIDE_1;
import static grondag.canvas.chunk.occlusion.Constants.OUTSIDE_2;
import static grondag.canvas.chunk.occlusion.Constants.PRECISE_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.PRECISE_HEIGHT_CLAMP;
import static grondag.canvas.chunk.occlusion.Constants.PRECISE_PIXEL_CENTER;
import static grondag.canvas.chunk.occlusion.Constants.PRECISE_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.PRECISE_WIDTH_CLAMP;
import static grondag.canvas.chunk.occlusion.Constants.PRECISION_BITS;
import static grondag.canvas.chunk.occlusion.Constants.SCALE_LOW;
import static grondag.canvas.chunk.occlusion.Constants.SCALE_MID;
import static grondag.canvas.chunk.occlusion.Constants.SCALE_POINT;
import static grondag.canvas.chunk.occlusion.Constants.SCANT_PRECISE_PIXEL_CENTER;
import static grondag.canvas.chunk.occlusion.Constants.TILE_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Data.event0;
import static grondag.canvas.chunk.occlusion.Data.event1;
import static grondag.canvas.chunk.occlusion.Data.event2;
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
import static grondag.canvas.chunk.occlusion.Data.maxPixelX;
import static grondag.canvas.chunk.occlusion.Data.maxPixelY;
import static grondag.canvas.chunk.occlusion.Data.midTileX;
import static grondag.canvas.chunk.occlusion.Data.midTileY;
import static grondag.canvas.chunk.occlusion.Data.minPixelX;
import static grondag.canvas.chunk.occlusion.Data.minPixelY;
import static grondag.canvas.chunk.occlusion.Data.position0;
import static grondag.canvas.chunk.occlusion.Data.position1;
import static grondag.canvas.chunk.occlusion.Data.position2;
import static grondag.canvas.chunk.occlusion.Data.positionHi;
import static grondag.canvas.chunk.occlusion.Data.positionLow;
import static grondag.canvas.chunk.occlusion.Data.px0;
import static grondag.canvas.chunk.occlusion.Data.px1;
import static grondag.canvas.chunk.occlusion.Data.px2;
import static grondag.canvas.chunk.occlusion.Data.py0;
import static grondag.canvas.chunk.occlusion.Data.py1;
import static grondag.canvas.chunk.occlusion.Data.py2;
import static grondag.canvas.chunk.occlusion.Data.scale;
import static grondag.canvas.chunk.occlusion.Data.vertexData;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PX;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PY;

public final class Triangle {
	static int prepareBounds(int v0, int v1, int v2) {
		final int x0 = vertexData[v0 + PV_PX];
		final int y0 = vertexData[v0 + PV_PY];
		final int x1 = vertexData[v1 + PV_PX];
		final int y1 = vertexData[v1 + PV_PY];
		final int x2 = vertexData[v2 + PV_PX];
		final int y2 = vertexData[v2 + PV_PY];

		// rejects triangles too small to render or where all points are on a line
		if(!isCcw(x0, y0, x1, y1, x2, y2)) {
			return  BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		int minY = y0;
		int maxY = y0;

		if (y1 < minY) {
			minY = y1;
		} else if (y1 > maxY) {
			maxY = y1;
		}

		if (y2 < minY) {
			minY = y2;
		} else if (y2 > maxY) {
			maxY = y2;
		}

		if (maxY <= 0 || minY >= PRECISE_HEIGHT) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		int minX = x0;
		int maxX = x0;

		if (x1 < minX) {
			minX = x1;
		} else if (x1 > maxX) {
			maxX = x1;
		}

		if (x2 < minX) {
			minX = x2;
		} else if (x2 > maxX) {
			maxX = x2;
		}

		if (maxX <= 0 || minX >= PRECISE_WIDTH) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		if (minX < -GUARD_SIZE || minY < -GUARD_SIZE || maxX > GUARD_WIDTH || maxY > GUARD_HEIGHT) {
			return BOUNDS_NEEDS_CLIP;
		}

		if (minX < 0) {
			minX = 0;
		}

		if (maxX >= PRECISE_WIDTH_CLAMP)  {
			maxX = PRECISE_WIDTH_CLAMP;

			if(minX > PRECISE_WIDTH_CLAMP) {
				minX = PRECISE_WIDTH_CLAMP;
			}
		}

		if (minY < 0) {
			minY = 0;
		}

		if (maxY >= PRECISE_HEIGHT_CLAMP)  {
			maxY = PRECISE_HEIGHT_CLAMP;

			if(minY > PRECISE_HEIGHT_CLAMP) {
				minY = PRECISE_HEIGHT_CLAMP;
			}
		}

		minPixelX = ((minX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		minPixelY = ((minY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		maxPixelX = ((maxX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		maxPixelY = ((maxY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);

		assert minPixelX >= 0;
		assert maxPixelX >= 0;
		assert minPixelY >= 0;
		assert maxPixelY >= 0;
		assert minPixelX <= MAX_PIXEL_X;
		assert maxPixelX <= MAX_PIXEL_X;
		assert minPixelY <= MAX_PIXEL_Y;
		assert maxPixelY <= MAX_PIXEL_Y;
		assert minPixelX <= maxPixelX;
		assert minPixelY <= maxPixelY;

		Data.x0 = x0;
		Data.y0 = y0;
		Data.x1 = x1;
		Data.y1 = y1;
		Data.x2 = x2;
		Data.y2 = y2;

		computeScale();

		return BOUNDS_IN;
	}

	static void computeScale() {
		int x0 = minPixelX;
		int y0 = minPixelY;
		int x1 = maxPixelX;
		int y1 = maxPixelY;

		if (x0 == x1 && y0 == y1) {
			scale = SCALE_POINT;
			return;
		}

		//PERF: probably a better way - maybe save outputs?

		x0  >>= TILE_AXIS_SHIFT;
		y0  >>= TILE_AXIS_SHIFT;
		x1  >>= TILE_AXIS_SHIFT;
		y1  >>= TILE_AXIS_SHIFT;

		if (x1 <= x0 + 1 && y1 <= y0 + 1) {
			scale = SCALE_LOW;
		}  else {
			scale = SCALE_MID;
		}
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
	static void prepareScan() {
		final int x0 = Data.x0;
		final int y0 = Data.y0;
		final int x1 = Data.x1;
		final int y1 = Data.y1;
		final int x2 = Data.x2;
		final int y2 = Data.y2;

		final int a0 = y0 - y1;
		final int b0 = x1 - x0;
		final int a1 = y1 - y2;
		final int b1 = x2 - x1;
		final int a2 = y2 - y0;
		final int b2 = x0 - x2;

		px0 = ((x0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		py0 = ((y0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		px1 = ((x1 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		py1 = ((y1 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		px2 = ((x2 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		py2 = ((y2 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);

		// signum of a and b, with shifted masks to derive the edge constant directly
		// the edge constants are specifically formulated to allow this, inline, avoids any pointer chases
		position0 = (1 << (((a0 >> 31) | (-a0 >>> 31)) + 1)) | (1 << (((b0 >> 31) | (-b0 >>> 31)) + 4));
		position1 = (1 << (((a1 >> 31) | (-a1 >>> 31)) + 1)) | (1 << (((b1 >> 31) | (-b1 >>> 31)) + 4));
		position2 = (1 << (((a2 >> 31) | (-a2 >>> 31)) + 1)) | (1 << (((b2 >> 31) | (-b2 >>> 31)) + 4));

		// PERF: check for triangle outside framebuffer as soon as orientation is known
		// for example if TOP-LEFT, then lower right screen corner must be inside edge

		lowTileA0 = a0 << LOW_AXIS_SHIFT;
		lowTileB0 = b0 << LOW_AXIS_SHIFT;
		lowSpanA0 = lowTileA0 - a0;
		lowSpanB0 = lowTileB0 - b0;
		lowExtent0 = ((lowSpanA0 < 0) ? -lowSpanA0 : lowSpanA0) + ((lowSpanB0 < 0) ? -lowSpanB0 : lowSpanB0);

		lowTileA1 = a1 << LOW_AXIS_SHIFT;
		lowTileB1 = b1 << LOW_AXIS_SHIFT;
		lowSpanA1 = lowTileA1 - a1;
		lowSpanB1 = lowTileB1 - b1;
		lowExtent1 = ((lowSpanA1 < 0) ? -lowSpanA1 : lowSpanA1) + ((lowSpanB1 < 0) ? -lowSpanB1 : lowSpanB1);

		lowTileA2 = a2 << LOW_AXIS_SHIFT;
		lowTileB2 = b2 << LOW_AXIS_SHIFT;
		lowSpanA2 = lowTileA2 - a2;
		lowSpanB2 = lowTileB2 - b2;
		lowExtent2 = ((lowSpanA2 < 0) ? -lowSpanA2 : lowSpanA2) + ((lowSpanB2 < 0) ? -lowSpanB2 : lowSpanB2);

		// PERF: remove? Interesting range should be written each tri
		//		System.arraycopy(EMPTY_EVENT, 0, event0, 0, PIXEL_HEIGHT);
		//		System.arraycopy(EMPTY_EVENT, 0, event1, 0, PIXEL_HEIGHT);
		//		System.arraycopy(EMPTY_EVENT, 0, event2, 0, PIXEL_HEIGHT);

		if (scale == SCALE_LOW) {
			lowTileX = (minPixelX >> LOW_AXIS_SHIFT);
			lowTileY = (minPixelY >> LOW_AXIS_SHIFT);

			final int tileX = minPixelX & LOW_AXIS_MASK;
			final int tileY = minPixelY & LOW_AXIS_MASK;

			// Compute barycentric coordinates at oriented corner of first tile
			// Can reduce precision (with accurate rounding) because increments will always be multiple of full pixel width
			int cornerX = (position0 & A_POSITIVE) == 0 ? tileX : (tileX + 7);
			int cornerY = (position0 & B_POSITIVE) == 0 ? tileY : (tileY + 7);
			lowCornerW0 = (int) ((-a0 * (x0 - ((long) cornerX << PRECISION_BITS)) - b0 * (y0 - ((long) cornerY << PRECISION_BITS)) + (((position0 & EDGE_LEFT) != 0 || position0 == EDGE_TOP) ? PRECISE_PIXEL_CENTER : SCANT_PRECISE_PIXEL_CENTER)) >> PRECISION_BITS);
			populateEvents(position0, lowCornerW0, cornerX, cornerY, a0, b0, event0);

			cornerX = (position1 & A_POSITIVE) == 0 ? tileX : (tileX + 7);
			cornerY = (position1 & B_POSITIVE) == 0 ? tileY : (tileY + 7);
			lowCornerW1 = (int) ((-a1 * (x1 - ((long) cornerX << PRECISION_BITS)) - b1 * (y1 - ((long) cornerY << PRECISION_BITS)) + (((position1 & EDGE_LEFT) != 0 || position1 == EDGE_TOP) ? PRECISE_PIXEL_CENTER : SCANT_PRECISE_PIXEL_CENTER)) >> PRECISION_BITS);
			populateEvents(position1, lowCornerW1, cornerX, cornerY, a1, b1, event1);

			cornerX = (position2 & A_POSITIVE) == 0 ? tileX : (tileX + 7);
			cornerY = (position2 & B_POSITIVE) == 0 ? tileY : (tileY + 7);
			lowCornerW2 = (int) ((-a2 * (x2 - ((long) cornerX << PRECISION_BITS)) - b2 * (y2 - ((long) cornerY << PRECISION_BITS)) + (((position2 & EDGE_LEFT) != 0 || position2 == EDGE_TOP) ? PRECISE_PIXEL_CENTER : SCANT_PRECISE_PIXEL_CENTER)) >> PRECISION_BITS);
			populateEvents(position2, lowCornerW2, cornerX, cornerY, a2, b2, event2);

			int pos = 0;
			if (lowCornerW0 < 0) pos |= OUTSIDE_0; else if (lowCornerW0 >= lowExtent0) pos |= INSIDE_0;
			if (lowCornerW1 < 0) pos |= OUTSIDE_1; else if (lowCornerW1 >= lowExtent1) pos |= INSIDE_1;
			if (lowCornerW2 < 0) pos |= OUTSIDE_2; else if (lowCornerW2 >= lowExtent2) pos |= INSIDE_2;
			positionLow = pos;
		}  else {
			assert scale == SCALE_MID;

			midTileX = (minPixelX >> MID_AXIS_SHIFT);
			midTileY = (minPixelY >> MID_AXIS_SHIFT);

			hiTileA0 = a0 << MID_AXIS_SHIFT;
			hiTileB0 = b0 << MID_AXIS_SHIFT;
			hiSpanA0 = hiTileA0 - a0;
			hiSpanB0 = hiTileB0 - b0;
			hiExtent0 = ((hiSpanA0 < 0) ? -hiSpanA0 : hiSpanA0) + ((hiSpanB0 < 0) ? -hiSpanB0 : hiSpanB0);

			hiTileA1 = a1 << MID_AXIS_SHIFT;
			hiTileB1 = b1 << MID_AXIS_SHIFT;
			hiSpanA1 = hiTileA1 - a1;
			hiSpanB1 = hiTileB1 - b1;
			hiExtent1 = ((hiSpanA1 < 0) ? -hiSpanA1 : hiSpanA1) + ((hiSpanB1 < 0) ? -hiSpanB1 : hiSpanB1);

			hiTileA2 = a2 << MID_AXIS_SHIFT;
			hiTileB2 = b2 << MID_AXIS_SHIFT;
			hiSpanA2 = hiTileA2 - a2;
			hiSpanB2 = hiTileB2 - b2;
			hiExtent2 = ((hiSpanA2 < 0) ? -hiSpanA2 : hiSpanA2) + ((hiSpanB2 < 0) ? -hiSpanB2 : hiSpanB2);

			final int tileX = minPixelX & MID_AXIS_MASK;
			final int tileY = minPixelY & MID_AXIS_MASK;

			int cornerX = (position0 & A_POSITIVE) == 0 ? tileX : (tileX + 63);
			int cornerY = (position0 & B_POSITIVE) == 0 ? tileY : (tileY + 63);
			hiCornerW0 = (int) ((-a0 * (x0 - ((long) cornerX << PRECISION_BITS)) - b0 * (y0 - ((long) cornerY << PRECISION_BITS)) + (((position0 & EDGE_LEFT) != 0 || position0 == EDGE_TOP) ? PRECISE_PIXEL_CENTER : SCANT_PRECISE_PIXEL_CENTER)) >> PRECISION_BITS);
			populateEvents(position0, hiCornerW0, cornerX, cornerY, a0, b0, event0);

			cornerX = (position1 & A_POSITIVE) == 0 ? tileX : (tileX + 63);
			cornerY = (position1 & B_POSITIVE) == 0 ? tileY : (tileY + 63);
			hiCornerW1 = (int) ((-a1 * (x1 - ((long) cornerX << PRECISION_BITS)) - b1 * (y1 - ((long) cornerY << PRECISION_BITS)) + (((position1 & EDGE_LEFT) != 0 || position1 == EDGE_TOP) ? PRECISE_PIXEL_CENTER : SCANT_PRECISE_PIXEL_CENTER)) >> PRECISION_BITS);
			populateEvents(position1, hiCornerW1, cornerX, cornerY, a1, b1, event1);

			cornerX = (position2 & A_POSITIVE) == 0 ? tileX : (tileX + 63);
			cornerY = (position2 & B_POSITIVE) == 0 ? tileY : (tileY + 63);
			hiCornerW2 = (int) ((-a2 * (x2 - ((long) cornerX << PRECISION_BITS)) - b2 * (y2 - ((long) cornerY << PRECISION_BITS)) + (((position2 & EDGE_LEFT) != 0 || position2 == EDGE_TOP) ? PRECISE_PIXEL_CENTER : SCANT_PRECISE_PIXEL_CENTER)) >> PRECISION_BITS);
			populateEvents(position2, hiCornerW2, cornerX, cornerY, a2, b2, event2);

			int pos = 0;
			if (hiCornerW0 < 0) pos |= OUTSIDE_0; else if (hiCornerW0 >= hiExtent0) pos |= INSIDE_0;
			if (hiCornerW1 < 0) pos |= OUTSIDE_1; else if (hiCornerW1 >= hiExtent1) pos |= INSIDE_1;
			if (hiCornerW2 < 0) pos |= OUTSIDE_2; else if (hiCornerW2 >= hiExtent2) pos |= INSIDE_2;
			positionHi = pos;
		}

		Data.a0 = a0;
		Data.b0 = b0;
		Data.a1 = a1;
		Data.b1 = b1;
		Data.a2 = a2;
		Data.b2 = b2;
	}

	static void populateEvents(int position, int ow, int ox, int oy, int a, int b, int[] events) {
		//		CanvasWorldRenderer.innerTimer.start();
		final int x0 = minPixelX & LOW_AXIS_MASK;
		final int y0 = minPixelY & LOW_AXIS_MASK;
		final int y1 = ((maxPixelY + 8) & LOW_AXIS_MASK) - 1;
		int w = ow + (y0 - oy) * b + (x0 - ox) * a;
		int x = x0;

		switch (position) {
		case EDGE_TOP_LEFT: {
			//			assert b < 0;
			//			assert a > 0;

			if (w >= a) {
				final int dx = w / a;
				w -= dx * a;
				x -= dx;
			}

			for (int y = y0; y <= y1; ++y) {
				if (w < 0) {
					final int dx = (-w + a - 1) / a;
					x += dx;
					w += a * dx;
				}

				events[y] = x;
				w += b;
			}

			break;
		}

		case EDGE_BOTTOM_LEFT: {
			//			assert b > 0;
			//			assert a > 0;

			if (w < 0) {
				final int dx = (-w + a - 1) / a;
				w += dx * a;
				x += dx;
			}

			for (int y = y0; y <= y1; ++y) {
				if (w >= a) {
					final int dx = w / a;
					x -= dx;
					w -= a * dx;
				}

				events[y] = x;
				w += b;
			}

			break;
		}

		case EDGE_TOP_RIGHT: {
			//			assert b < 0;
			//			assert a < 0;

			if (w >= -a) {
				final int dx = w / -a;
				w += a * dx;
				x += dx;
			}

			for (int y = y0; y <= y1; ++y) {
				if (w < 0) {
					final int dx = (w + a + 1) / a;
					x -= dx;
					w -= a * dx;
				}

				events[y] = x;
				w += b;
			}

			break;
		}

		case EDGE_BOTTOM_RIGHT: {
			//			assert b > 0;
			//			assert a < 0;

			if (w < 0) {
				final int dx = (w + a + 1) / a;
				w -= a * dx;
				x -= dx;
			}

			for (int y = y0; y <= y1; ++y) {
				if (w >= -a) {
					final int dx = w / -a;
					x += dx;
					w += a * dx;
				}

				events[y] = x;
				w += b;
			}

			break;
		}

		default:
			//NOOP;
			break;
		}

		//		CanvasWorldRenderer.innerTimer.stop();
	}

	static boolean isCcw(long x0, long y0, long x1, long y1, long x2, long y2) {
		return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0) > 0L;
	}
}