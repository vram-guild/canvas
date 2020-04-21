package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_IN;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_NEEDS_CLIP;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_OUTSIDE_OR_TOO_SMALL;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_FLR;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_FRL;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_LFR;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_LLR;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_LRF;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_LRL;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_LRR;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_RFL;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_RLF;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_RLL;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_RLR;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_RRL;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_POSITION_MASK;
import static grondag.canvas.chunk.occlusion.Constants.GUARD_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.GUARD_SIZE;
import static grondag.canvas.chunk.occlusion.Constants.GUARD_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.HALF_PRECISE_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.HALF_PRECISE_WIDTH;
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
import static grondag.canvas.chunk.occlusion.Data.ax0;
import static grondag.canvas.chunk.occlusion.Data.ax1;
import static grondag.canvas.chunk.occlusion.Data.ay0;
import static grondag.canvas.chunk.occlusion.Data.ay1;
import static grondag.canvas.chunk.occlusion.Data.bx0;
import static grondag.canvas.chunk.occlusion.Data.bx1;
import static grondag.canvas.chunk.occlusion.Data.by0;
import static grondag.canvas.chunk.occlusion.Data.by1;
import static grondag.canvas.chunk.occlusion.Data.clipOutputX;
import static grondag.canvas.chunk.occlusion.Data.clipOutputY;
import static grondag.canvas.chunk.occlusion.Data.cx0;
import static grondag.canvas.chunk.occlusion.Data.cx1;
import static grondag.canvas.chunk.occlusion.Data.cy0;
import static grondag.canvas.chunk.occlusion.Data.cy1;
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
import static grondag.canvas.chunk.occlusion.Data.tileIndex;
import static grondag.canvas.chunk.occlusion.Data.tileOriginX;
import static grondag.canvas.chunk.occlusion.Data.tileOriginY;
import static grondag.canvas.chunk.occlusion.Data.vertexData;
import static grondag.canvas.chunk.occlusion.Indexer.tileIndex;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PX;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PY;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_W;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_X;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_Y;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_Z;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.needsNearClip;


public final class Triangle {
	static void clipNear(final int[] data, int internal, int external) {
		final float intX = Float.intBitsToFloat(data[internal + PV_X]);
		final float intY = Float.intBitsToFloat(data[internal + PV_Y]);
		final float intZ = Float.intBitsToFloat(data[internal + PV_Z]);
		final float intW = Float.intBitsToFloat(data[internal + PV_W]);

		final float extX = Float.intBitsToFloat(data[external + PV_X]);
		final float extY = Float.intBitsToFloat(data[external + PV_Y]);
		final float extZ = Float.intBitsToFloat(data[external + PV_Z]);
		final float extW = Float.intBitsToFloat(data[external + PV_W]);

		// intersection point is the projection plane, at which point Z == 1
		// and w will be 0 but projection division isn't needed, so force output to W = 1
		// see https://www.cs.usfca.edu/~cruse/math202s11/homocoords.pdf

		assert extZ < 0;
		assert extZ < extW;
		assert intZ > 0;
		assert intZ < intW;

		final float wt  = (intZ + intW) / (-(extW - intW) - (extZ - intZ));
		//		final float wt  = (intZ - intW) / ((extW - intW) - (extZ - intZ));

		// note again that projection division isn't needed
		final float x = (intX + (extX - intX) * wt);
		final float y = (intY + (extY - intY) * wt);
		final float w = (intW + (extW - intW) * wt);
		final float iw = 1f / w;

		clipOutputX = Math.round(iw * x * HALF_PRECISE_WIDTH) + HALF_PRECISE_WIDTH;
		clipOutputY = Math.round(iw * y * HALF_PRECISE_HEIGHT) + HALF_PRECISE_HEIGHT;
	}

	static int prepareBoundsNew(int v0, int v1, int v2) {
		final int split = needsNearClip(vertexData, v0) | (needsNearClip(vertexData, v1) << 1) | (needsNearClip(vertexData, v2) << 2);

		switch (split) {
		case 0b000:
			return prepareBounds000(v0, v1, v2);

		default:
		case 0b001:
		case 0b010:
		case 0b011:
		case 0b100:
		case 0b101:
		case 0b110:
		case 0b111:
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}
	}

	static int prepareBounds000(int v0, int v1, int v2) {
		int ax0 = 0, ay0 = 0;
		int bx0 = 0, by0 = 0;
		int cx0 = 0, cy0 = 0;
		int minY = 0, maxY = 0, minX = 0, maxX = 0;

		ax0 = vertexData[v0 + PV_PX];
		ay0 = vertexData[v0 + PV_PY];
		bx0 = vertexData[v1 + PV_PX];
		by0 = vertexData[v1 + PV_PY];
		cx0 = vertexData[v2 + PV_PX];
		cy0 = vertexData[v2 + PV_PY];
		ax1 = bx0;
		ay1 = by0;
		bx1 = cx0;
		by1 = cy0;
		cx1 = ax0;
		cy1 = ay0;

		minX = ax0;
		maxX = ax0;

		if (bx0 < minX) minX = bx0; else if (bx0 > maxX) maxX = bx0;
		if (cx0 < minX) minX = cx0; else if (cx0 > maxX) maxX = cx0;

		minY = ay0;
		maxY = ay0;

		if (by0 < minY) minY = by0; else if (by0 > maxY) maxY = by0;
		if (cy0 < minY) minY = cy0; else if (cy0 > maxY) maxY = cy0;

		Data.minX = minX;
		Data.maxX = maxX;
		Data.minY = minY;
		Data.maxY = maxY;

		Data.ax0 = ax0;
		Data.ay0 = ay0;
		Data.bx0 = bx0;
		Data.by0 = by0;
		Data.cx0 = cx0;
		Data.cy0 = cy0;

		return prepareBoundsInner();
	}

	static int prepareBoundsInner()  {
		int minX = Data.minX;
		int maxX = Data.maxX;
		int minY = Data.minY;
		int maxY = Data.maxY;

		if (maxY <= 0 || minY >= PRECISE_HEIGHT) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
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

		minTileOriginX = minPixelX & TILE_AXIS_MASK;
		maxTileOriginX = maxPixelX & TILE_AXIS_MASK;
		maxTileOriginY = maxPixelY & TILE_AXIS_MASK;

		tileOriginX = minTileOriginX;
		tileOriginY = minPixelY & TILE_AXIS_MASK;
		tileIndex = tileIndex(minPixelX >> TILE_AXIS_SHIFT, minPixelY >> TILE_AXIS_SHIFT);

		position0 = edgePosition(ax0, ay0, ax1, ay1);
		position1 = edgePosition(bx0, by0, bx1, by1);
		position2 = edgePosition(cx0, cy0, cx1, cy1);

		final int eventKey = (position0 - 1) & EVENT_POSITION_MASK | (((position1 - 1) & EVENT_POSITION_MASK) << 2) | (((position2 - 1) & EVENT_POSITION_MASK) << 4);

		switch (eventKey) {
		case EVENT_012_LLR:
			populateLeftEvents2(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1);
			populateRightEvents(cx0, cy0, cx1, cy1);
			break;

		case EVENT_012_RLL:
			populateLeftEvents2(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
			populateRightEvents(ax0, ay0, ax1, ay1);
			break;

		case EVENT_012_LRL:
			populateLeftEvents2(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1);
			populateRightEvents(bx0, by0, bx1, by1);
			break;

		case EVENT_012_RRL:
			populateRightEvents2(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1);
			populateLeftEvents(cx0, cy0, cx1, cy1);
			break;

		case EVENT_012_LRR:
			populateRightEvents2(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
			populateLeftEvents(ax0, ay0, ax1, ay1);
			break;

		case EVENT_012_RLR:
			populateRightEvents2(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1);
			populateLeftEvents(bx0, by0, bx1, by1);
			break;

		case EVENT_012_FLR:
			populateLeftEvents(bx0, by0, bx1, by1);
			populateRightEvents(cx0, cy0, cx1, cy1);
			populateFlatEvents(position0, ax0, ay0, ax1, ay1);
			break;

		case EVENT_012_RFL:
			populateRightEvents(ax0, ay0, ax1, ay1);
			populateLeftEvents(cx0, cy0, cx1, cy1);
			populateFlatEvents(position1, bx0, by0, bx1, by1);
			break;

		case EVENT_012_LRF:
			populateLeftEvents(ax0, ay0, ax1, ay1);
			populateRightEvents(bx0, by0, bx1, by1);
			populateFlatEvents(position2, cx0, cy0, cx1, cy1);
			break;

		case EVENT_012_FRL:
			populateRightEvents(bx0, by0, bx1, by1);
			populateLeftEvents(cx0, cy0, cx1, cy1);
			populateFlatEvents(position0, ax0, ay0, ax1, ay1);
			break;

		case EVENT_012_LFR:
			populateLeftEvents(ax0, ay0, ax1, ay1);
			populateRightEvents(cx0, cy0, cx1, cy1);
			populateFlatEvents(position1, bx0, by0, bx1, by1);
			break;

		case EVENT_012_RLF:
			populateRightEvents(ax0, ay0, ax1, ay1);
			populateLeftEvents(bx0, by0, bx1, by1);
			populateFlatEvents(position2, cx0, cy0, cx1, cy1);
			break;

		default:
			assert false : "base edge combination";
		}

		return BOUNDS_IN;
	}

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

		position0 = edgePosition(x0, y0, x1, y1);
		position1 = edgePosition(x1, y1, x2, y2);
		position2 = edgePosition(x2, y2, x0, y0);

		final int eventKey = (position0 - 1) & EVENT_POSITION_MASK | (((position1 - 1) & EVENT_POSITION_MASK) << 2) | (((position2 - 1) & EVENT_POSITION_MASK) << 4);

		switch (eventKey) {
		case EVENT_012_LLR:
			populateLeftEvents2(x0, y0, x1, y1, x1, y1, x2, y2);
			populateRightEvents(x2, y2, x0, y0);
			break;

		case EVENT_012_RLL:
			populateLeftEvents2(x1, y1, x2, y2, x2, y2, x0, y0);
			populateRightEvents(x0, y0, x1, y1);
			break;

		case EVENT_012_LRL:
			populateLeftEvents2(x0, y0, x1, y1, x2, y2, x0, y0);
			populateRightEvents(x1, y1, x2, y2);
			break;

		case EVENT_012_RRL:
			populateRightEvents2(x0, y0, x1, y1, x1, y1, x2, y2);
			populateLeftEvents(x2, y2, x0, y0);
			break;

		case EVENT_012_LRR:
			populateRightEvents2(x1, y1, x2, y2, x2, y2, x0, y0);
			populateLeftEvents(x0, y0, x1, y1);
			break;

		case EVENT_012_RLR:
			populateRightEvents2(x0, y0, x1, y1, x2, y2, x0, y0);
			populateLeftEvents(x1, y1, x2, y2);
			break;

		case EVENT_012_FLR:
			populateLeftEvents(x1, y1, x2, y2);
			populateRightEvents(x2, y2, x0, y0);
			populateFlatEvents(position0, x0, y0, x1, y1);
			break;

		case EVENT_012_RFL:
			populateRightEvents(x0, y0, x1, y1);
			populateLeftEvents(x2, y2, x0, y0);
			populateFlatEvents(position1, x1, y1, x2, y2);
			break;

		case EVENT_012_LRF:
			populateLeftEvents(x0, y0, x1, y1);
			populateRightEvents(x1, y1, x2, y2);
			populateFlatEvents(position2, x2, y2, x0, y0);
			break;

		case EVENT_012_FRL:
			populateRightEvents(x1, y1, x2, y2);
			populateLeftEvents(x2, y2, x0, y0);
			populateFlatEvents(position0, x0, y0, x1, y1);
			break;

		case EVENT_012_LFR:
			populateLeftEvents(x0, y0, x1, y1);
			populateRightEvents(x2, y2, x0, y0);
			populateFlatEvents(position1, x1, y1, x2, y2);
			break;

		case EVENT_012_RLF:
			populateRightEvents(x0, y0, x1, y1);
			populateLeftEvents(x1, y1, x2, y2);
			populateFlatEvents(position2, x2, y2, x0, y0);
			break;

		default:
			assert false : "base edge combination";
		}
	}

	// TODO: remove if not used - doesn't work with new indexing
	//	static boolean compareEvents() {
	//		boolean result = true;
	//
	//		final int limit = maxTileOriginY + 7;
	//
	//		for (int i = minPixelY & TILE_AXIS_MASK; i <= limit; ++i) {
	//			int oldLeft = Integer.MIN_VALUE, oldRight = Integer.MAX_VALUE;
	//
	//			final int j = ((i & ~7) << 2) + (i & 7);
	//
	//			if ((position0 & A_NEGATIVE) != 0) {
	//				oldRight =  events[j];
	//			} else if ((position0 & A_POSITIVE) != 0) {
	//				oldLeft =  events[j];
	//			}
	//
	//			if ((position1 & A_NEGATIVE) != 0) {
	//				oldRight =  Math.min(oldRight, events[j +  8]);
	//			} else if ((position1 & A_POSITIVE) != 0) {
	//				oldLeft =  Math.max(oldLeft, events[j + 8]);
	//			}
	//
	//			if ((position2 & A_NEGATIVE) != 0) {
	//				oldRight =  Math.min(oldRight, events[j +  16]);
	//			} else if ((position2 & A_POSITIVE) != 0) {
	//				oldLeft =  Math.max(oldLeft, events[j + 16]);
	//			}
	//
	//			oldLeft = MathHelper.clamp(oldLeft, -1, 1024);
	//			oldRight = MathHelper.clamp(oldRight, -1, 1024);
	//			final int newLeft = MathHelper.clamp(events2[i << 1], -1, 1024);
	//			final int newRight = MathHelper.clamp(events2[(i << 1) + 1], -1, 1024);
	//
	//			if(oldLeft != newLeft)  {
	//				System.out.println("For y = " + i + " LEFT was " + oldLeft +  " and is now " + newLeft);
	//				result = false;
	//			}
	//
	//			if(oldRight != newRight)  {
	//				System.out.println("For y = " + i + " RIGHT was " + oldRight +  " and is now " + newRight);
	//				result = false;
	//			}
	//		}
	//
	//		return result;
	//	}

	static int edgePosition(int x0In, int y0In, int x1In, int y1In) {
		final int dy = y1In - y0In;
		final int dx = x1In - x0In;
		// signum of dx and dy, with shifted masks to derive the edge constant directly
		// the edge constants are specifically formulated to allow this, inline, avoids any pointer chases
		// sign of dy is inverted for historical reasons
		return (1 << (((-dy >> 31) | (dy >>> 31)) + 1)) | (1 << (((dx >> 31) | (-dx >>> 31)) + 4));
	}

	static void populateFlatEvents(int position, int x0In, int y0In, int x1In, int y1In) {
		if (position == EDGE_TOP) {
			final int py = ((y0In + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) + 1;

			if (py == MAX_PIXEL_Y) return;

			final int y1 = maxTileOriginY + 7;
			final int start = (py << 1);
			final int limit = (y1 << 1);

			for (int y = start; y <= limit; ) {
				events[y++] = PIXEL_WIDTH;
				events[y++] = -1;
			}
		}  else {
			assert position == EDGE_BOTTOM;

			final int py = (y0In >> PRECISION_BITS);

			assert py <= MAX_PIXEL_Y;

			if (py == 0) return;

			final int y0 = minPixelY & TILE_AXIS_MASK;
			final int start = (y0 << 1);
			final int limit = (py << 1);

			for (int y = start; y < limit; ) {
				events[y++] = PIXEL_WIDTH;
				events[y++] = -1;
			}
		}
	}

	static void populateLeftEvents(int x0In, int y0In, int x1In, int y1In) {
		final int y0 = minPixelY & TILE_AXIS_MASK;
		final int y1 = maxTileOriginY + 7;
		final int limit = (y1 << 1);
		final int dx = x1In - x0In;

		final long nStep;
		long x;

		if (dx == 0) {
			x = ((x0In + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			nStep = 0;
		} else {
			final int dy = y1In - y0In;
			final long n = (((long)dx) << 16) / dy;
			nStep = n << PRECISION_BITS;
			x = (x0In << 16) - n * y0In + nStep * y0 + 0x100000L;
		}

		for (int y = (y0 << 1); y <= limit; y += 2) {
			events[y] = (int) (x >= 0 ? (x >> 20) : -(-x >> 20));
			x += nStep;
		}
	}

	static void populateRightEvents(int x0In, int y0In, int x1In, int y1In) {
		final int y0 = minPixelY & TILE_AXIS_MASK;
		final int y1 = maxTileOriginY + 7;
		// difference from left: is high index in pairs
		final int limit = (y1 << 1) + 1;
		final int dx = x1In - x0In;

		final long nStep;
		long x;

		if (dx == 0) {
			x = ((x0In + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			nStep = 0;
		} else {
			final int dy = y1In - y0In;
			final long n = (((long)dx) << 16) / dy;
			nStep = n << PRECISION_BITS;
			// difference from left: rounding looses tie
			x = (x0In << 16) - n * y0In + nStep * y0 + 0x7FFFFL;
		}

		// difference from left: is high index in pairs
		for (int y = (y0 << 1) + 1; y <= limit; y += 2) {
			events[y] = (int) (x >= 0 ? (x >> 20) : -(-x >> 20));
			x += nStep;
		}
	}

	static void populateLeftEvents2(int ax0, int ay0, int ax1, int ay1, int bx0, int by0, int bx1, int by1) {
		final int y0 = minPixelY & TILE_AXIS_MASK;
		final int y1 = maxTileOriginY + 7;
		final int limit = (y1 << 1);

		final long aStep;
		long ax;
		final long bStep;
		long bx;

		if (ax0 == ax1) {
			ax = ((ax0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			aStep = 0;
		} else {
			final int ady = ay1 - ay0;
			final int adx = ax1 - ax0;
			final long an = (((long)adx) << 16) / ady;
			aStep = an << PRECISION_BITS;
			ax = (ax0 << 16) - an * ay0 + aStep * y0 + 0x100000L;
		}

		if (bx0 == bx1) {
			bx = ((bx0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			bStep = 0;
		} else {
			final int bdx = bx1 - bx0;
			final int bdy = by1 - by0;
			final long bn = (((long)bdx) << 16) / bdy;
			bStep = bn << PRECISION_BITS;
			bx = (bx0 << 16) - bn * by0 + bStep * y0 + 0x100000L;
		}

		for (int y = (y0 << 1); y <= limit; y += 2) {
			final long x = ax > bx ? ax : bx;

			events[y] = (int) (x >= 0 ? (x >> 20) : -(-x >> 20));

			ax += aStep;
			bx += bStep;
		}
	}

	static void populateRightEvents2(int ax0, int ay0, int ax1, int ay1, int bx0, int by0, int bx1, int by1) {
		final int y0 = minPixelY & TILE_AXIS_MASK;
		final int y1 = maxTileOriginY + 7;
		// difference from left: is high index in pairs
		final int limit = (y1 << 1) + 1;

		final long aStep;
		long ax;
		final long bStep;
		long bx;

		if (ax0 == ax1) {
			ax = ((ax0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			aStep = 0;
		} else {
			final int ady = ay1 - ay0;
			final int adx = ax1 - ax0;
			final long an = (((long)adx) << 16) / ady;
			aStep = an << PRECISION_BITS;
			// difference from left: rounding looses tie
			ax = (ax0 << 16) - an * ay0 + aStep * y0 + 0x7FFFFL;
		}

		if (bx0 == bx1) {
			bx = ((bx0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			bStep = 0;
		} else {
			final int bdx = bx1 - bx0;
			final int bdy = by1 - by0;
			final long bn = (((long)bdx) << 16) / bdy;
			bStep = bn << PRECISION_BITS;
			// difference from left: rounding looses tie
			bx = (bx0 << 16) - bn * by0 + bStep * y0 + 0x7FFFFL;
		}

		// difference from left: is high index in pairs
		for (int y = (y0 << 1) + 1; y <= limit; y += 2) {
			// difference from left: lower value wins
			final long x = ax < bx ? ax : bx;

			events[y] = (int) (x >= 0 ? (x >> 20) : -(-x >> 20));

			ax += aStep;
			bx += bStep;
		}
	}

	static boolean isCcw(long x0, long y0, long x1, long y1, long x2, long y2) {
		return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0) > 0L;
	}
}