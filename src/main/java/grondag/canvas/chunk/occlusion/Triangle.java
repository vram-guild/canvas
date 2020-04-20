package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_IN;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_NEEDS_CLIP;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_OUTSIDE_OR_TOO_SMALL;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_RIGHT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP;
import static grondag.canvas.chunk.occlusion.Constants.GUARD_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.GUARD_SIZE;
import static grondag.canvas.chunk.occlusion.Constants.GUARD_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.MAX_PIXEL_X;
import static grondag.canvas.chunk.occlusion.Constants.MAX_PIXEL_Y;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.PRECISE_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.PRECISE_HEIGHT_CLAMP;
import static grondag.canvas.chunk.occlusion.Constants.PRECISE_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.PRECISE_WIDTH_CLAMP;
import static grondag.canvas.chunk.occlusion.Constants.PRECISION_BITS;
import static grondag.canvas.chunk.occlusion.Constants.SCANT_PRECISE_PIXEL_CENTER;
import static grondag.canvas.chunk.occlusion.Constants.TILE_AXIS_MASK;
import static grondag.canvas.chunk.occlusion.Constants.TILE_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Data.events;
import static grondag.canvas.chunk.occlusion.Data.maxPixelX;
import static grondag.canvas.chunk.occlusion.Data.maxPixelY;
import static grondag.canvas.chunk.occlusion.Data.maxTileOriginX;
import static grondag.canvas.chunk.occlusion.Data.maxTileOriginY;
import static grondag.canvas.chunk.occlusion.Data.minPixelX;
import static grondag.canvas.chunk.occlusion.Data.minPixelY;
import static grondag.canvas.chunk.occlusion.Data.minTileOriginX;
import static grondag.canvas.chunk.occlusion.Data.position0;
import static grondag.canvas.chunk.occlusion.Data.position1;
import static grondag.canvas.chunk.occlusion.Data.position2;
import static grondag.canvas.chunk.occlusion.Data.temp;
import static grondag.canvas.chunk.occlusion.Data.tileIndex;
import static grondag.canvas.chunk.occlusion.Data.tileOriginX;
import static grondag.canvas.chunk.occlusion.Data.tileOriginY;
import static grondag.canvas.chunk.occlusion.Data.vertexData;
import static grondag.canvas.chunk.occlusion.Indexer.tileIndex;
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

		return BOUNDS_IN;
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

		// PERF: check for triangle outside framebuffer as soon as orientation is known
		// for example if TOP-LEFT, then lower right screen corner must be inside edge

		minTileOriginX = minPixelX & TILE_AXIS_MASK;
		maxTileOriginX = maxPixelX & TILE_AXIS_MASK;
		maxTileOriginY = maxPixelY & TILE_AXIS_MASK;

		tileOriginX = minTileOriginX;
		tileOriginY = minPixelY & TILE_AXIS_MASK;
		tileIndex = tileIndex(minPixelX >> TILE_AXIS_SHIFT, minPixelY >> TILE_AXIS_SHIFT);

		position0 = populateEvents(x0, y0, x1, y1, 0);
		//		if(populateEvents2(x0, y0, x1, y1, 0) != position0 || !compareEvents(0)) {
		//			populateEvents2(x0, y0, x1, y1, 0);
		//		}

		position1 = populateEvents(x1, y1, x2, y2, 1);
		//		if(populateEvents2(x1, y1, x2, y2, 1) != position1 || !compareEvents(1)) {
		//			populateEvents2(x1, y1, x2, y2, 1);
		//		}

		position2 = populateEvents(x2, y2, x0, y0, 2);
		//		if(populateEvents2(x2, y2, x0, y0, 2) != position2 || !compareEvents(2)) {
		//			populateEvents2(x2, y2, x0, y0, 2);
		//		}
	}

	// TODO: remove if not used - doesn't work with new indexing
	//	static boolean compareEvents(int index) {
	//		boolean result = true;
	//
	//		final int limit = maxTileOriginY + 7;
	//
	//		for (int i = minPixelY & TILE_AXIS_MASK; i <= limit; ++i) {
	//			final int j = (i << 2) + index;
	//
	//			if(events[j] >= 0 && Math.abs(events[j] - events2[j]) > 1)  {
	//				System.out.println("For y = " + i + " was " + events[j] +  " is now " + events[j]);
	//				result = false;
	//			}
	//		}
	//
	//		return result;
	//	}

	static int populateEvents(int x0In, int y0In, int x1In, int y1In, int index) {
		final int dy = y1In - y0In;
		final int dx = x1In - x0In;
		final int y0 = minPixelY & TILE_AXIS_MASK;
		final int y1 = maxTileOriginY + 7;
		// signum of dx and dy, with shifted masks to derive the edge constant directly
		// the edge constants are specifically formulated to allow this, inline, avoids any pointer chases
		// sign of dy is inverted for historical reasons
		final int position = (1 << (((-dy >> 31) | (dy >>> 31)) + 1)) | (1 << (((dx >> 31) | (-dx >>> 31)) + 4));
		final long nStep;
		long x;

		switch (position) {
		case EDGE_TOP: { // build mask as for right edge
			final int py = ((y0In + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
			temp[index] = py;
			nStep = -PIXEL_WIDTH << 20;
			x = (py - y0 + 1) * -nStep - (1 << 20);
			break;
		}

		case EDGE_BOTTOM: {  // build mask as for left edge
			// NB: in last rev this got shifted one down in some cases - more inclusive - should be more accurate
			final int py = y0In >> PRECISION_BITS;
			temp[index] = py;
			nStep = -PIXEL_WIDTH << 20;
			x = (py - y0) * -nStep;
			break;
		}

		case EDGE_LEFT: {
			// NB: in last rev this got shifted one to the left  in some cases - more inclusive - should be more accurate
			final int px = ((x0In + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
			temp[index] = px;
			x = px << 20;
			nStep = 0;
			break;
		}

		case EDGE_RIGHT: {
			final int px = ((x0In + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
			temp[index] = px;
			x = px << 20;
			nStep = 0;
			break;
		}

		default:
			// equation of line: x = ny + c
			// n = rise over run slope = dx / dy
			final long n = (((long)dx) << 16) / dy;
			nStep = n << PRECISION_BITS;
			// c = x intercept = x - ny, then add tile  minY * slope for starting X
			// add rounding per edge - extra  four bits because input coordinates have four bits extra
			// left edge (a > 0) is more inclusive as a tie-breaker, not sure if actually necessary/works
			x = (x0In << 16) - n * y0In + nStep * y0 + ((dx > 0) ? 0x100000L : 0x7FFFFL);
		}


		final int limit = ((y1 & ~7) << 2) + (index << 3) + (y1 & 7);

		// one pass per tile
		for (int y = ((y0 & ~7) << 2) + (index << 3) + (y0 & 7); y <= limit; y += 32) {
			int i = y;

			events[i] = (int) (x >= 0 ? (x >> 20) : -(-x >> 20));
			x += nStep;
			events[++i] = (int) (x >= 0 ? (x >> 20) : -(-x >> 20));
			x += nStep;
			events[++i] = (int) (x >= 0 ? (x >> 20) : -(-x >> 20));
			x += nStep;
			events[++i] = (int) (x >= 0 ? (x >> 20) : -(-x >> 20));
			x += nStep;
			events[++i] = (int) (x >= 0 ? (x >> 20) : -(-x >> 20));
			x += nStep;
			events[++i] = (int) (x >= 0 ? (x >> 20) : -(-x >> 20));
			x += nStep;
			events[++i] = (int) (x >= 0 ? (x >> 20) : -(-x >> 20));
			x += nStep;
			events[++i] = (int) (x >= 0 ? (x >> 20) : -(-x >> 20));
			x += nStep;
		}

		return position;
	}

	static boolean isCcw(long x0, long y0, long x1, long y1, long x2, long y2) {
		return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0) > 0L;
	}
}