package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_IN;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_OUTSIDE_OR_TOO_SMALL;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_BOTTOM;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_POINT;
import static grondag.canvas.chunk.occlusion.Constants.EDGE_TOP;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_FFF;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_FFL;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_FFR;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_FLF;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_FLL;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_FLR;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_FRF;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_FRL;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_FRR;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_LFF;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_LFL;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_LFR;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_LLF;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_LLL;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_LLR;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_LRF;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_LRL;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_LRR;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_RFF;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_RFL;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_RFR;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_RLF;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_RLL;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_RLR;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_RRF;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_RRL;
import static grondag.canvas.chunk.occlusion.Constants.EVENT_012_RRR;
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
	private static void clipNear(final int[] data, int internal, int external) {
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

	static int prepareBounds(int v0, int v1, int v2) {
		// puts bits in lexical order
		final int split = needsNearClip(vertexData, v2) | (needsNearClip(vertexData, v1) << 1) | (needsNearClip(vertexData, v0) << 2);

		switch (split) {
		case 0b000:
			return prepareBounds000(v0, v1, v2);

		case 0b001:
			return prepareBounds001(v0, v1, v2);

		case 0b010:
			return prepareBounds001(v2, v0, v1);

		case 0b100:
			return prepareBounds001(v1, v2, v0);

		case 0b011:
			return prepareBounds011(v0, v1, v2);

		case 0b101:
			return prepareBounds011(v1, v2, v0);

		case 0b110:
			return prepareBounds011(v2, v0, v1);

		case 0b111:
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;

		default:
			assert false : "Invalid split";
		}

		return BOUNDS_OUTSIDE_OR_TOO_SMALL;
	}

	private static int prepareBounds000(int v0, int v1, int v2) {
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

	private static void clipToGuards() {
		// PERF: use fixed precision

		if (ax0 < -GUARD_SIZE) {
			if (ax1 < -GUARD_SIZE) {
				ax1 = -GUARD_SIZE;
			} else {
				final float wt = (float) (-GUARD_SIZE - ax1) / (ax0 - ax1);
				ay0 = Math.round(ay1 + wt * (ay0 - ay1));
			}

			ax0 = -GUARD_SIZE;
		}  else if (ax0 > GUARD_WIDTH) {
			if (ax1 > GUARD_WIDTH) {
				ax1 = GUARD_WIDTH;
			} else {
				final float wt = (float) (GUARD_WIDTH - ax1) / (ax0 - ax1);
				ay0 = Math.round(ay1 + wt * (ay0 - ay1));
			}

			ax0 = GUARD_WIDTH;
		}

		if (ay0 < -GUARD_SIZE) {
			if (ay1 < -GUARD_SIZE) {
				ay1 = -GUARD_SIZE;
			} else {
				final float wt = (float) (-GUARD_SIZE - ay1) / (ay0 - ay1);
				ax0 = Math.round(ax1 + wt * (ax0 - ax1));
			}

			ay0 = -GUARD_SIZE;
		}  else if (ay0 > GUARD_HEIGHT) {
			if (ay1 > GUARD_HEIGHT) {
				ay1 = GUARD_HEIGHT;
			}  else {
				final float wt = (float) (GUARD_HEIGHT - ay1) / (ay0 - ay1);
				ax0 = Math.round(ax1 + wt * (ax0 - ax1));
			}

			ay0 = GUARD_HEIGHT;
		}

		if (ax1 < -GUARD_SIZE) {
			final float wt = (float) (-GUARD_SIZE - ax0) / (ax1 - ax0);
			ax1 = -GUARD_SIZE;
			ay1 = Math.round(ay0 + wt * (ay1 - ay0));
		}  else if (ax1 > GUARD_WIDTH) {
			final float wt = (float) (GUARD_WIDTH - ax0) / (ax1 - ax0);
			ax1 = GUARD_WIDTH;
			ay1 = Math.round(ay0 + wt * (ay1 - ay0));
		}

		if (ay1 < -GUARD_SIZE) {
			final float wt = (float) (-GUARD_SIZE - ay0) / (ay1 - ay0);
			ax1 = Math.round(ax0 + wt * (ax1 - ax0));
			ay1 = -GUARD_SIZE;
		}  else if (ay1 > GUARD_HEIGHT) {
			final float wt = (float) (GUARD_HEIGHT - ay0) / (ay1 - ay0);
			ax1 = Math.round(ax0 + wt * (ax1 - ax0));
			ay1 = GUARD_HEIGHT;
		}

		if (bx0 < -GUARD_SIZE) {
			if (bx1 < -GUARD_SIZE) {
				bx1 = -GUARD_SIZE;
			} else {
				final float wt = (float) (-GUARD_SIZE - bx1) / (bx0 - bx1);
				by0 = Math.round(by1 + wt * (by0 - by1));
			}

			bx0 = -GUARD_SIZE;
		}  else if (bx0 > GUARD_WIDTH) {
			if (bx1 > GUARD_WIDTH) {
				bx1 = GUARD_WIDTH;
			} else {
				final float wt = (float) (GUARD_WIDTH - bx1) / (bx0 - bx1);
				by0 = Math.round(by1 + wt * (by0 - by1));
			}

			bx0 = GUARD_WIDTH;
		}

		if (by0 < -GUARD_SIZE) {
			if (by1 < -GUARD_SIZE) {
				by1 = -GUARD_SIZE;
			} else {
				final float wt = (float) (-GUARD_SIZE - by1) / (by0 - by1);
				bx0 = Math.round(bx1 + wt * (bx0 - bx1));
			}

			by0 = -GUARD_SIZE;
		}  else if (by0 > GUARD_HEIGHT) {
			if (by1 > GUARD_HEIGHT) {
				by1 = GUARD_HEIGHT;
			}  else {
				final float wt = (float) (GUARD_HEIGHT - by1) / (by0 - by1);
				bx0 = Math.round(bx1 + wt * (bx0 - bx1));
			}

			by0 = GUARD_HEIGHT;
		}

		if (bx1 < -GUARD_SIZE) {
			final float wt = (float) (-GUARD_SIZE - bx0) / (bx1 - bx0);
			bx1 = -GUARD_SIZE;
			by1 = Math.round(by0 + wt * (by1 - by0));
		}  else if (bx1 > GUARD_WIDTH) {
			final float wt = (float) (GUARD_WIDTH - bx0) / (bx1 - bx0);
			bx1 = GUARD_WIDTH;
			by1 = Math.round(by0 + wt * (by1 - by0));
		}

		if (by1 < -GUARD_SIZE) {
			final float wt = (float) (-GUARD_SIZE - by0) / (by1 - by0);
			bx1 = Math.round(bx0 + wt * (bx1 - bx0));
			by1 = -GUARD_SIZE;
		}  else if (by1 > GUARD_HEIGHT) {
			final float wt = (float) (GUARD_HEIGHT - by0) / (by1 - by0);
			bx1 = Math.round(bx0 + wt * (bx1 - bx0));
			by1 = GUARD_HEIGHT;
		}

		if (cx0 < -GUARD_SIZE) {
			if (cx1 < -GUARD_SIZE) {
				cx1 = -GUARD_SIZE;
			} else {
				final float wt = (float) (-GUARD_SIZE - cx1) / (cx0 - cx1);
				cy0 = Math.round(cy1 + wt * (cy0 - cy1));
			}

			cx0 = -GUARD_SIZE;
		}  else if (cx0 > GUARD_WIDTH) {
			if (cx1 > GUARD_WIDTH) {
				cx1 = GUARD_WIDTH;
			} else {
				final float wt = (float) (GUARD_WIDTH - cx1) / (cx0 - cx1);
				cy0 = Math.round(cy1 + wt * (cy0 - cy1));
			}

			cx0 = GUARD_WIDTH;
		}

		if (cy0 < -GUARD_SIZE) {
			if (cy1 < -GUARD_SIZE) {
				cy1 = -GUARD_SIZE;
			} else {
				final float wt = (float) (-GUARD_SIZE - cy1) / (cy0 - cy1);
				cx0 = Math.round(cx1 + wt * (cx0 - cx1));
			}

			cy0 = -GUARD_SIZE;
		}  else if (cy0 > GUARD_HEIGHT) {
			if (cy1 > GUARD_HEIGHT) {
				cy1 = GUARD_HEIGHT;
			}  else {
				final float wt = (float) (GUARD_HEIGHT - cy1) / (cy0 - cy1);
				cx0 = Math.round(cx1 + wt * (cx0 - cx1));
			}

			cy0 = GUARD_HEIGHT;
		}

		if (cx1 < -GUARD_SIZE) {
			final float wt = (float) (-GUARD_SIZE - cx0) / (cx1 - cx0);
			cx1 = -GUARD_SIZE;
			cy1 = Math.round(cy0 + wt * (cy1 - cy0));
		}  else if (cx1 > GUARD_WIDTH) {
			final float wt = (float) (GUARD_WIDTH - cx0) / (cx1 - cx0);
			cx1 = GUARD_WIDTH;
			cy1 = Math.round(cy0 + wt * (cy1 - cy0));
		}

		if (cy1 < -GUARD_SIZE) {
			final float wt = (float) (-GUARD_SIZE - cy0) / (cy1 - cy0);
			cx1 = Math.round(cx0 + wt * (cx1 - cx0));
			cy1 = -GUARD_SIZE;
		}  else if (cy1 > GUARD_HEIGHT) {
			final float wt = (float) (GUARD_HEIGHT - cy0) / (cy1 - cy0);
			cx1 = Math.round(cx0 + wt * (cx1 - cx0));
			cy1 = GUARD_HEIGHT;
		}
	}

	private static int prepareBounds001(int v0, int v1, int ext) {
		int minY = 0, maxY = 0, minX = 0, maxX = 0;

		ax0 = vertexData[v0 + PV_PX];
		ay0 = vertexData[v0 + PV_PY];

		bx0 = vertexData[v1 + PV_PX];
		by0 = vertexData[v1 + PV_PY];

		ax1 = bx0;
		ay1 = by0;

		clipNear(vertexData, v1, ext);
		bx1 = clipOutputX;
		by1 = clipOutputY;

		clipNear(vertexData, v0, ext);
		cx0 = clipOutputX;
		cy0 = clipOutputY;

		cx1 = ax0;
		cy1 = ay0;

		minX = ax0;
		maxX = ax0;

		// ax1 = bx0 and cx1 = ax0,  so no need to test those
		if (bx0 < minX) minX = bx0; else if (bx0 > maxX) maxX = bx0;
		if (bx1 < minX) minX = bx1; else if (bx1 > maxX) maxX = bx1;
		if (cx0 < minX) minX = cx0; else if (cx0 > maxX) maxX = cx0;

		minY = ay0;
		maxY = ay0;

		if (by0 < minY) minY = by0; else if (by0 > maxY) maxY = by0;
		if (by1 < minX) minX = by1; else if (by1 > maxX) maxX = by1;
		if (cy0 < minY) minY = cy0; else if (cy0 > maxY) maxY = cy0;

		Data.minX = minX;
		Data.maxX = maxX;
		Data.minY = minY;
		Data.maxY = maxY;

		return prepareBoundsInner();
	}

	private static int prepareBounds011(int v0, int ext1, int ext2) {
		int minY = 0, maxY = 0, minX = 0, maxX = 0;

		ax0 = vertexData[v0 + PV_PX];
		ay0 = vertexData[v0 + PV_PY];

		clipNear(vertexData, v0, ext1);
		bx0 = clipOutputX;
		by0 = clipOutputY;

		clipNear(vertexData, v0, ext2);
		cx0 = clipOutputX;
		cy0 = clipOutputY;

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

		return prepareBoundsInner();
	}

	private static int prepareBoundsInner()  {
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
			clipToGuards();
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

		case EVENT_012_FFR:
			populateRightEvents(cx0, cy0, cx1, cy1);
			populateFlatEvents(position0, ax0, ay0, ax1, ay1);
			populateFlatEvents(position1, bx0, by0, bx1, by1);
			break;

		case EVENT_012_FFL:
			populateLeftEvents(cx0, cy0, cx1, cy1);
			populateFlatEvents(position0, ax0, ay0, ax1, ay1);
			populateFlatEvents(position1, bx0, by0, bx1, by1);
			break;

		case EVENT_012_FRF:
			populateRightEvents(bx0, by0, bx1, by1);
			populateFlatEvents(position0, ax0, ay0, ax1, ay1);
			populateFlatEvents(position2, cx0, cy0, cx1, cy1);
			break;

		case EVENT_012_FLF:
			populateLeftEvents(bx0, by0, bx1, by1);
			populateFlatEvents(position0, ax0, ay0, ax1, ay1);
			populateFlatEvents(position2, cx0, cy0, cx1, cy1);
			break;

		case EVENT_012_RFF:
			populateRightEvents(ax0, ay0, ax1, ay1);
			populateFlatEvents(position1, bx0, by0, bx1, by1);
			populateFlatEvents(position2, cx0, cy0, cx1, cy1);
			break;

		case EVENT_012_LFF:
			populateLeftEvents(ax0, ay0, ax1, ay1);
			populateFlatEvents(position1, bx0, by0, bx1, by1);
			populateFlatEvents(position2, cx0, cy0, cx1, cy1);
			break;

		case EVENT_012_FLL:
			populateLeftEvents2(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
			populateFlatEvents(position0, ax0, ay0, ax1, ay1);
			break;

		case EVENT_012_LFL:
			populateLeftEvents2(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1);
			populateFlatEvents(position1, bx0, by0, bx1, by1);
			break;

		case EVENT_012_LLF:
			populateLeftEvents2(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1);
			populateFlatEvents(position2, cx0, cy0, cx1, cy1);
			break;

		case EVENT_012_FRR:
			populateRightEvents2(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
			populateFlatEvents(position0, ax0, ay0, ax1, ay1);
			break;

		case EVENT_012_RFR:
			populateRightEvents2(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1);
			populateFlatEvents(position1, bx0, by0, bx1, by1);
			break;

		case EVENT_012_RRF:
			populateRightEvents2(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1);
			populateFlatEvents(position2, cx0, cy0, cx1, cy1);
			break;

		case EVENT_012_FFF:
			// fill it
			populateLeftEvents(0, 0, 0, MAX_PIXEL_Y);
			populateFlatEvents(position0, ax0, ay0, ax1, ay1);
			populateFlatEvents(position1, bx0, by0, bx1, by1);
			populateFlatEvents(position2, cx0, cy0, cx1, cy1);
			break;

		case EVENT_012_RRR:
		case EVENT_012_LLL:
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;

		default:
			assert false : "bad edge combination";
		return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		return BOUNDS_IN;
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

	private static int edgePosition(int x0In, int y0In, int x1In, int y1In) {
		final int dy = y1In - y0In;
		final int dx = x1In - x0In;
		// signum of dx and dy, with shifted masks to derive the edge constant directly
		// the edge constants are specifically formulated to allow this, inline, avoids any pointer chases
		// sign of dy is inverted for historical reasons
		return (1 << (((-dy >> 31) | (dy >>> 31)) + 1)) | (1 << (((dx >> 31) | (-dx >>> 31)) + 4));
	}

	private static void populateFlatEvents(int position, int x0In, int y0In, int x1In, int y1In) {
		if (position == EDGE_TOP) {
			final int py = ((y0In + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) + 1;

			if (py == MAX_PIXEL_Y) return;

			final int y1 = maxTileOriginY + 7;
			final int start = py < 0 ? 0 : (py << 1);
			final int limit = (y1 << 1);

			assert limit < events.length;

			for (int y = start; y <= limit; ) {
				events[y++] = PIXEL_WIDTH;
				events[y++] = -1;
			}
		}  else if (position == EDGE_BOTTOM) {
			final int py = (y0In >> PRECISION_BITS);

			if (py == 0) return;

			final int y0 = minPixelY & TILE_AXIS_MASK;
			final int start = (y0 << 1);
			final int limit = py > MAX_PIXEL_Y ? (MAX_PIXEL_Y << 1) :(py << 1);

			assert limit < events.length;

			for (int y = start; y < limit; ) {
				events[y++] = PIXEL_WIDTH;
				events[y++] = -1;
			}
		} else {
			assert position == EDGE_POINT;
		}
	}

	private static void populateLeftEvents(int x0In, int y0In, int x1In, int y1In) {
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

	private static void populateRightEvents(int x0In, int y0In, int x1In, int y1In) {
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

	private static void populateLeftEvents2(int ax0, int ay0, int ax1, int ay1, int bx0, int by0, int bx1, int by1) {
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

	private static void populateRightEvents2(int ax0, int ay0, int ax1, int ay1, int bx0, int by0, int bx1, int by1) {
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
}