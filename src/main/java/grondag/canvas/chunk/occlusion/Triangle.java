package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.A_POSITIVE;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_IN;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_NEEDS_CLIP;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_OUTSIDE_OR_TOO_SMALL;
import static grondag.canvas.chunk.occlusion.Constants.B_POSITIVE;
import static grondag.canvas.chunk.occlusion.Constants.GUARD_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.GUARD_SIZE;
import static grondag.canvas.chunk.occlusion.Constants.GUARD_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.INSIDE;
import static grondag.canvas.chunk.occlusion.Constants.INTERSECTING;
import static grondag.canvas.chunk.occlusion.Constants.LOW_AXIS_MASK;
import static grondag.canvas.chunk.occlusion.Constants.LOW_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Constants.LOW_TILE_PIXEL_DIAMETER;
import static grondag.canvas.chunk.occlusion.Constants.LOW_TILE_SPAN;
import static grondag.canvas.chunk.occlusion.Constants.MAX_PIXEL_X;
import static grondag.canvas.chunk.occlusion.Constants.MAX_PIXEL_Y;
import static grondag.canvas.chunk.occlusion.Constants.MID_AXIS_MASK;
import static grondag.canvas.chunk.occlusion.Constants.MID_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Constants.MID_TILE_SPAN;
import static grondag.canvas.chunk.occlusion.Constants.OUTSIDE;
import static grondag.canvas.chunk.occlusion.Constants.PRECISE_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.PRECISE_HEIGHT_CLAMP;
import static grondag.canvas.chunk.occlusion.Constants.PRECISE_PIXEL_CENTER;
import static grondag.canvas.chunk.occlusion.Constants.PRECISE_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.PRECISE_WIDTH_CLAMP;
import static grondag.canvas.chunk.occlusion.Constants.PRECISION_BITS;
import static grondag.canvas.chunk.occlusion.Constants.SCALE_LOW;
import static grondag.canvas.chunk.occlusion.Constants.SCALE_MID;
import static grondag.canvas.chunk.occlusion.Constants.SCALE_POINT;
import static grondag.canvas.chunk.occlusion.Constants.TILE_AXIS_SHIFT;
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
import static grondag.canvas.chunk.occlusion.Data.hiStepA0;
import static grondag.canvas.chunk.occlusion.Data.hiStepA1;
import static grondag.canvas.chunk.occlusion.Data.hiStepA2;
import static grondag.canvas.chunk.occlusion.Data.hiStepB0;
import static grondag.canvas.chunk.occlusion.Data.hiStepB1;
import static grondag.canvas.chunk.occlusion.Data.hiStepB2;
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
import static grondag.canvas.chunk.occlusion.Data.positionHi0;
import static grondag.canvas.chunk.occlusion.Data.positionHi1;
import static grondag.canvas.chunk.occlusion.Data.positionHi2;
import static grondag.canvas.chunk.occlusion.Data.positionLow0;
import static grondag.canvas.chunk.occlusion.Data.positionLow1;
import static grondag.canvas.chunk.occlusion.Data.positionLow2;
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

		minPixelX = ((minX + PRECISE_PIXEL_CENTER - 1) >> PRECISION_BITS);
		minPixelY = ((minY + PRECISE_PIXEL_CENTER - 1) >> PRECISION_BITS);
		maxPixelX = ((maxX + PRECISE_PIXEL_CENTER - 1) >> PRECISION_BITS);
		maxPixelY = ((maxY + PRECISE_PIXEL_CENTER - 1) >> PRECISION_BITS);

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

		final int a0 = (y0 - y1);
		final int b0 = (x1 - x0);
		final int a1 = (y1 - y2);
		final int b1 = (x2 - x1);
		final int a2 = (y2 - y0);
		final int b2 = (x0 - x2);

		// signum of a and b, b is shifted left two bits to derive the edge constant directly
		// the edge constants are specifically formulated to allow this, inline, avoids any pointer chases
		position0 = ((((a0 >> 31) | (-a0 >>> 31)) + 1) | ((((b0 >> 31) | (-b0 >>> 31)) + 1) << 2));
		position1 = ((((a1 >> 31) | (-a1 >>> 31)) + 1) | ((((b1 >> 31) | (-b1 >>> 31)) + 1) << 2));
		position2 = ((((a2 >> 31) | (-a2 >>> 31)) + 1) | ((((b2 >> 31) | (-b2 >>> 31)) + 1) << 2));

		// PERF: derive from position
		final boolean isTopLeft0 = a0 > 0 || (a0 == 0 && b0 < 0);
		final boolean isTopLeft1 = a1 > 0 || (a1 == 0 && b1 < 0);
		final boolean isTopLeft2 = a2 > 0 || (a2 == 0 && b2 < 0);

		lowSpanA0 = a0 * LOW_TILE_SPAN;
		lowSpanB0 = b0 * LOW_TILE_SPAN;
		lowExtent0 = Math.abs(lowSpanA0) + Math.abs(lowSpanB0);

		lowSpanA1 = a1 * LOW_TILE_SPAN;
		lowSpanB1 = b1 * LOW_TILE_SPAN;
		lowExtent1 = Math.abs(lowSpanA1) + Math.abs(lowSpanB1);

		lowSpanA2 = a2 * LOW_TILE_SPAN;
		lowSpanB2 = b2 * LOW_TILE_SPAN;
		lowExtent2 = Math.abs(lowSpanA2) + Math.abs(lowSpanB2);

		// Compute barycentric coordinates at oriented corner of first tile
		// Can reduce precision (with accurate rounding) because increments will always be multiple of full pixel width


		if (scale == SCALE_LOW) {
			lowTileX = (minPixelX >> LOW_AXIS_SHIFT);
			lowTileY = minPixelY >> LOW_AXIS_SHIFT;

			final int ox = (minPixelX & LOW_AXIS_MASK) << PRECISION_BITS;
			final int oy = (minPixelY & LOW_AXIS_MASK) << PRECISION_BITS;

			long x = x0 - ((position0 & A_POSITIVE) == 0 ? ox : (ox + (7 << PRECISION_BITS)));
			long y = y0 - ((position0 & B_POSITIVE) == 0 ? oy : (oy + (7 << PRECISION_BITS)));
			lowCornerW0 = (int) ((-a0 * x - b0 * y + (isTopLeft0 ? PRECISE_PIXEL_CENTER : (PRECISE_PIXEL_CENTER - 1))) >> PRECISION_BITS);
			positionLow0 = lowCornerW0 < 0 ? OUTSIDE : lowCornerW0 >= lowExtent0 ? INSIDE : INTERSECTING;

			x = x1 - ((position1 & A_POSITIVE) == 0 ? ox : (ox + (7 << PRECISION_BITS)));
			y = y1 - ((position1 & B_POSITIVE) == 0 ? oy : (oy + (7 << PRECISION_BITS)));
			lowCornerW1 = (int) ((-a1 * x - b1 * y + (isTopLeft1 ? PRECISE_PIXEL_CENTER : (PRECISE_PIXEL_CENTER - 1))) >> PRECISION_BITS);
			positionLow1 = lowCornerW1 < 0 ? OUTSIDE : lowCornerW1 >= lowExtent1 ? INSIDE : INTERSECTING;

			x = x2 - ((position2 & A_POSITIVE) == 0 ? ox : (ox + (7 << PRECISION_BITS)));
			y = y2 - ((position2 & B_POSITIVE) == 0 ? oy : (oy + (7 << PRECISION_BITS)));
			lowCornerW2 = (int) ((-a2 * x - b2 * y + (isTopLeft2 ? PRECISE_PIXEL_CENTER : (PRECISE_PIXEL_CENTER - 1))) >> PRECISION_BITS);
			positionLow2 = lowCornerW2 < 0 ? OUTSIDE : lowCornerW2 >= lowExtent2 ? INSIDE : INTERSECTING;

		}  else {
			assert scale == SCALE_MID;

			midTileX = minPixelX >> MID_AXIS_SHIFT;
			midTileY = minPixelY >> MID_AXIS_SHIFT;

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

			final int ox = (minPixelX & MID_AXIS_MASK) << PRECISION_BITS;
			final int oy = (minPixelY & MID_AXIS_MASK) << PRECISION_BITS;

			long x = x0 - ((position0 & A_POSITIVE) == 0 ? ox : (ox + (63 << PRECISION_BITS)));
			long y = y0 - ((position0 & B_POSITIVE) == 0 ? oy : (oy + (63 << PRECISION_BITS)));
			hiCornerW0 = (int) ((-a0 * x - b0 * y + (isTopLeft0 ? PRECISE_PIXEL_CENTER : (PRECISE_PIXEL_CENTER - 1))) >> PRECISION_BITS);
			positionHi0 = hiCornerW0 < 0 ? OUTSIDE : hiCornerW0 >= hiExtent0 ? INSIDE : INTERSECTING;

			x = x1 - ((position1 & A_POSITIVE) == 0 ? ox : (ox + (63 << PRECISION_BITS)));
			y = y1 - ((position1 & B_POSITIVE) == 0 ? oy : (oy + (63 << PRECISION_BITS)));
			hiCornerW1 = (int) ((-a1 * x - b1 * y + (isTopLeft1 ? PRECISE_PIXEL_CENTER : (PRECISE_PIXEL_CENTER - 1))) >> PRECISION_BITS);
			positionHi1 = hiCornerW1 < 0 ? OUTSIDE : hiCornerW1 >= hiExtent1 ? INSIDE : INTERSECTING;

			x = x2 - ((position2 & A_POSITIVE) == 0 ? ox : (ox + (63 << PRECISION_BITS)));
			y = y2 - ((position2 & B_POSITIVE) == 0 ? oy : (oy + (63 << PRECISION_BITS)));
			hiCornerW2 = (int) ((-a2 * x - b2 * y + (isTopLeft2 ? PRECISE_PIXEL_CENTER : (PRECISE_PIXEL_CENTER - 1))) >> PRECISION_BITS);
			positionHi2 = hiCornerW2 < 0 ? OUTSIDE : hiCornerW2 >= hiExtent2 ? INSIDE : INTERSECTING;
		}

		Data.a0 = a0;
		Data.b0 = b0;
		Data.a1 = a1;
		Data.b1 = b1;
		Data.a2 = a2;
		Data.b2 = b2;
	}

	static boolean isCcw(long x0, long y0, long x1, long y1, long x2, long y2) {
		return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0) > 0L;
	}
}