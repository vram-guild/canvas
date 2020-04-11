package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PX;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PY;

public final class Triangle {
	// Boumds of current triangle - pixel coordinates
	protected int minPixelX;
	protected int minPixelY;
	protected int maxPixelX;
	protected int maxPixelY;

	public final Edge e0 = new Edge();
	public final Edge e1 = new Edge();
	public final Edge e2 = new Edge();


	// TODO: remove
	protected int v0 = 0, v1 = 0, v2 = 0;

	protected static final boolean DEBUG_VERTEX = false;

	/**
	 *
	 * @param v0
	 * @param v1
	 * @param v2
	 * @return constant value from BoundsResult
	 */
	protected int prepareBounds(final int[] vertexData, int v0, int v1, int v2) {
		final int x0 = vertexData[v0 + PV_PX];
		final int y0 = vertexData[v0 + PV_PY];
		final int x1 = vertexData[v1 + PV_PX];
		final int y1 = vertexData[v1 + PV_PY];
		final int x2 = vertexData[v2 + PV_PX];
		final int y2 = vertexData[v2 + PV_PY];

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

		if (maxY < 0 || minY >= AbstractTerrainOccluder.PRECISE_HEIGHT) {
			return BoundsResult.OUT_OF_BOUNDS;
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

		if (maxX < 0 || minX >= AbstractTerrainOccluder.PRECISE_WIDTH) {
			return BoundsResult.OUT_OF_BOUNDS;
		}

		if (minX < -AbstractTerrainOccluder.GUARD_SIZE || minY < -AbstractTerrainOccluder.GUARD_SIZE || maxX > AbstractTerrainOccluder.GUARD_WIDTH || maxY > AbstractTerrainOccluder.GUARD_HEIGHT) {
			return BoundsResult.NEEDS_CLIP;
		}

		int minPixelX = (minX + AbstractTerrainOccluder.PRECISE_PIXEL_CENTER - 1) >> AbstractTerrainOccluder.PRECISION_BITS;
	int minPixelY = (minY + AbstractTerrainOccluder.PRECISE_PIXEL_CENTER - 1) >> AbstractTerrainOccluder.PRECISION_BITS;
	int maxPixelX = (maxX - AbstractTerrainOccluder.PRECISE_PIXEL_CENTER) >> AbstractTerrainOccluder.PRECISION_BITS;
	int maxPixelY = (maxY - AbstractTerrainOccluder.PRECISE_PIXEL_CENTER) >> AbstractTerrainOccluder.PRECISION_BITS;

	if (minPixelX < 0) {
		minPixelX = 0;
	}

	if (maxPixelX > AbstractTerrainOccluder.PIXEL_WIDTH - 1)  {
		maxPixelX = AbstractTerrainOccluder.PIXEL_WIDTH  - 1;
	}

	if (minPixelY < 0) {
		minPixelY = 0;
	}

	if (maxPixelY > AbstractTerrainOccluder.PIXEL_HEIGHT - 1)  {
		maxPixelY = AbstractTerrainOccluder.PIXEL_HEIGHT - 1;
	}

	this.minPixelX = minPixelX;
	this.minPixelY = minPixelY;
	this.maxPixelX = maxPixelX;
	this.maxPixelY = maxPixelY;

	return BoundsResult.IN_BOUNDS;
	}

	protected void prepareScan(final int[] vertexData, int v0, int v1, int v2) {
		if (DEBUG_VERTEX) {
			this.v0 = v0;
			this.v1 = v1;
			this.v2 = v2;
		}

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
		final int c0 = (int) ((orient2d(x0, y0, x1, y1) + (isTopLeft0 ? AbstractTerrainOccluder.PRECISE_PIXEL_CENTER : (AbstractTerrainOccluder.PRECISE_PIXEL_CENTER - 1))) >> AbstractTerrainOccluder.PRECISION_BITS);
		final int c1 = (int) ((orient2d(x1, y1, x2, y2) + (isTopLeft1 ? AbstractTerrainOccluder.PRECISE_PIXEL_CENTER : (AbstractTerrainOccluder.PRECISE_PIXEL_CENTER - 1))) >> AbstractTerrainOccluder.PRECISION_BITS);
		final int c2 = (int) ((orient2d(x2, y2, x0, y0) + (isTopLeft2 ? AbstractTerrainOccluder.PRECISE_PIXEL_CENTER : (AbstractTerrainOccluder.PRECISE_PIXEL_CENTER - 1))) >> AbstractTerrainOccluder.PRECISION_BITS);

		e0.prepare(a0, b0, c0);
		e1.prepare(a1, b1, c1);
		e2.prepare(a2, b2, c2);
	}

	protected long orient2d(long x0, long y0, long x1, long y1) {
		return (y1 - y0) * x0 - (x1 - x0) * y0;
	}
}