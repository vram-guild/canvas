package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PX;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PY;
import static grondag.canvas.chunk.occlusion._Constants.BOUNDS_IN;
import static grondag.canvas.chunk.occlusion._Constants.BOUNDS_NEEDS_CLIP;
import static grondag.canvas.chunk.occlusion._Constants.BOUNDS_OUTSIDE_OR_TOO_SMALL;
import static grondag.canvas.chunk.occlusion._Constants.GUARD_HEIGHT;
import static grondag.canvas.chunk.occlusion._Constants.GUARD_SIZE;
import static grondag.canvas.chunk.occlusion._Constants.GUARD_WIDTH;
import static grondag.canvas.chunk.occlusion._Constants.MAX_PIXEL_X;
import static grondag.canvas.chunk.occlusion._Constants.MAX_PIXEL_Y;
import static grondag.canvas.chunk.occlusion._Constants.PRECISE_HEIGHT;
import static grondag.canvas.chunk.occlusion._Constants.PRECISE_HEIGHT_CLAMP;
import static grondag.canvas.chunk.occlusion._Constants.PRECISE_PIXEL_CENTER;
import static grondag.canvas.chunk.occlusion._Constants.PRECISE_WIDTH;
import static grondag.canvas.chunk.occlusion._Constants.PRECISE_WIDTH_CLAMP;
import static grondag.canvas.chunk.occlusion._Constants.PRECISION_BITS;
import static grondag.canvas.chunk.occlusion._Constants.SCALE_LOW;
import static grondag.canvas.chunk.occlusion._Constants.SCALE_MID;
import static grondag.canvas.chunk.occlusion._Constants.SCALE_POINT;
import static grondag.canvas.chunk.occlusion._Constants.TILE_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion._Data.e0;
import static grondag.canvas.chunk.occlusion._Data.e1;
import static grondag.canvas.chunk.occlusion._Data.e2;
import static grondag.canvas.chunk.occlusion._Data.maxPixelX;
import static grondag.canvas.chunk.occlusion._Data.maxPixelY;
import static grondag.canvas.chunk.occlusion._Data.minPixelX;
import static grondag.canvas.chunk.occlusion._Data.minPixelY;
import static grondag.canvas.chunk.occlusion._Data.scale;
import static grondag.canvas.chunk.occlusion._Data.vertexData;

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

	static void prepareScan(int v0, int v1, int v2) {
		final int x0 = vertexData[v0 + PV_PX];
		final int y0 = vertexData[v0 + PV_PY];
		final int x1 = vertexData[v1 + PV_PX];
		final int y1 = vertexData[v1 + PV_PY];
		final int x2 = vertexData[v2 + PV_PX];
		final int y2 = vertexData[v2 + PV_PY];

		final int a0 = (y0 - y1);
		final int b0 = (x1 - x0);
		final int a1 = (y1 - y2);
		final int b1 = (x2 - x1);
		final int a2 = (y2 - y0);
		final int b2 = (x0 - x2);

		final boolean isTopLeft0 = a0 > 0 || (a0 == 0 && b0 < 0);
		final boolean isTopLeft1 = a1 > 0 || (a1 == 0 && b1 < 0);
		final boolean isTopLeft2 = a2 > 0 || (a2 == 0 && b2 < 0);

		// Barycentric coordinates at minX/minY corner
		// Can reduce precision (with accurate rounding) because increments will always be multiple of full pixel width
		final int c0 = (int) ((orient2d(x0, y0, x1, y1) + (isTopLeft0 ? PRECISE_PIXEL_CENTER : (PRECISE_PIXEL_CENTER - 1))) >> PRECISION_BITS);
		final int c1 = (int) ((orient2d(x1, y1, x2, y2) + (isTopLeft1 ? PRECISE_PIXEL_CENTER : (PRECISE_PIXEL_CENTER - 1))) >> PRECISION_BITS);
		final int c2 = (int) ((orient2d(x2, y2, x0, y0) + (isTopLeft2 ? PRECISE_PIXEL_CENTER : (PRECISE_PIXEL_CENTER - 1))) >> PRECISION_BITS);

		e0.prepare(a0, b0, c0);
		e1.prepare(a1, b1, c1);
		e2.prepare(a2, b2, c2);
	}

	static boolean isCcw(long x0, long y0, long x1, long y1, long x2, long y2) {
		return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0) > 0L;
	}

	static long orient2d(long x0, long y0, long x1, long y1) {
		return (y1 - y0) * x0 - (x1 - x0) * y0;
	}
}