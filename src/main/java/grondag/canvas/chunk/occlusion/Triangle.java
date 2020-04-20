package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_IN;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_NEEDS_CLIP;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_OUTSIDE_OR_TOO_SMALL;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM_RIGHT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_RIGHT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP_LEFT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP_RIGHT;
import static grondag.canvas.chunk.occlusion.Constants.GUARD_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.GUARD_SIZE;
import static grondag.canvas.chunk.occlusion.Constants.GUARD_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.MAX_PIXEL_X;
import static grondag.canvas.chunk.occlusion.Constants.MAX_PIXEL_Y;
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
import static grondag.canvas.chunk.occlusion.Data.maxTileY;
import static grondag.canvas.chunk.occlusion.Data.minPixelX;
import static grondag.canvas.chunk.occlusion.Data.minPixelY;
import static grondag.canvas.chunk.occlusion.Data.minTileX;
import static grondag.canvas.chunk.occlusion.Data.minTileY;
import static grondag.canvas.chunk.occlusion.Data.position0;
import static grondag.canvas.chunk.occlusion.Data.position1;
import static grondag.canvas.chunk.occlusion.Data.position2;
import static grondag.canvas.chunk.occlusion.Data.tileEdgeOutcomes;
import static grondag.canvas.chunk.occlusion.Data.tileX;
import static grondag.canvas.chunk.occlusion.Data.tileY;
import static grondag.canvas.chunk.occlusion.Data.vertexData;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PX;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PY;
import static grondag.canvas.chunk.occlusion.Tile.tilePosition;

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

		minTileX = minPixelX & TILE_AXIS_MASK;
		minTileY = minPixelY & TILE_AXIS_MASK;
		maxTileY = ((maxPixelY + 8) & TILE_AXIS_MASK) - 1;

		tileX = (minPixelX >> TILE_AXIS_SHIFT);
		tileY = (minPixelY >> TILE_AXIS_SHIFT);

		position0 = populateEvents(x0, y0, x1, y1, 0);
		//		if(!compareEvents(event0, e0)) {
		//			populateEvents2(x0, y0, x1, y1, e0);
		//		}

		position1 = populateEvents(x1, y1, x2, y2, 1);
		//		if(!compareEvents(event1, e1)) {
		//			populateEvents2(x1, y1, x2, y2, e1);
		//		}

		position2 = populateEvents(x2, y2, x0, y0, 2);
		//		if(!compareEvents(event2, e2)) {
		//			populateEvents2(x2, y2, x0, y0, e2);
		//		}

		tileEdgeOutcomes = tilePosition(position0, 0)
				| (tilePosition(position1, 1) << 2)
				| (tilePosition(position2, 2) << 4);
	}

	static boolean compareEvents(int[] a, int[] b) {
		boolean result = true;
		for (int i = 0; i < 512; ++i) {
			if(a[i] >= 0 && Math.abs(a[i] - b[i]) > 1)  {
				System.out.println("For y = " + i + " was " + a[i] +  " is now " + b[i]);
				result = false;
			}
		}

		return result;
	}

	static int populateEvents(int x0In, int y0In, int x1In, int y1In, int index) {
		final int a = y0In - y1In;
		final int b = x1In - x0In;

		// signum of a and b, with shifted masks to derive the edge constant directly
		// the edge constants are specifically formulated to allow this, inline, avoids any pointer chases
		final int position = (1 << (((a >> 31) | (-a >>> 31)) + 1)) | (1 << (((b >> 31) | (-b >>> 31)) + 4));

		// equation of line: x = ny + c

		// n = rise over run slope = dx / dy
		final long n = y0In == y1In ?  0 : ((((long)(x1In - x0In)) << 16) / (y1In - y0In));

		// c = x-intercept = x - ny
		long c = (x0In << 16) - n * y0In;

		// add rounding per edge - extra  four bits because input coordinates have four bits extra
		c += (((position & EDGE_LEFT) != 0 || position == EDGE_TOP) ? 0x100000L : 0x7FFFFL);

		final long nStep = n << PRECISION_BITS;
		// compute starting x
		long psx = nStep * minTileY + c;


		final int y0 = minTileY;
		final int y1 = maxTileY;

		switch (position) {
		case EDGE_TOP: {
			events[index] = ((y0In + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
			break;
		}

		case EDGE_BOTTOM: {
			// NB: in last rev this got shifted one down in some cases - more inclusive - should be more accurate
			events[index] = y0In >> PRECISION_BITS;
		break;
		}

		case EDGE_LEFT: {
			// NB: in last rev this got shifted one to the left  in some cases - more inclusive - should be more accurate
			events[index] = x0In >> PRECISION_BITS;
		break;
		}

		case EDGE_RIGHT: {
			events[index] = ((x0In + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
			break;
		}

		case EDGE_TOP_RIGHT:
		case EDGE_BOTTOM_RIGHT:
		case EDGE_BOTTOM_LEFT:
		case EDGE_TOP_LEFT: {
			// map to event index
			final int limit  = (y1 << 2) + index;

			for (int y = (y0 << 2) + index; y <= limit; y += 4) {
				events[y] = (int) (psx >= 0 ? (psx >> 20) : -(-psx >> 20));
				psx += nStep;
			}

			break;
		}

		default:
			//NOOP;
			break;
		}

		return position;
	}

	static boolean isCcw(long x0, long y0, long x1, long y1, long x2, long y2) {
		return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0) > 0L;
	}
}