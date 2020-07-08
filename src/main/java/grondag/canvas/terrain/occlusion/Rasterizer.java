package grondag.canvas.terrain.occlusion;

import static grondag.canvas.terrain.occlusion.Constants.*;
import static grondag.canvas.terrain.occlusion.Indexer.tileIndex;
import static grondag.canvas.terrain.occlusion.Matrix4L.MATRIX_PRECISION_HALF;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;


// Some elements are adapted from content found at
// https://fgiesen.wordpress.com/2013/02/17/optimizing-sw-occlusion-culling-index/
// by Fabian “ryg” Giesen. That content is in the public domain.
class Rasterizer  {
	final Matrix4L mvpMatrix = new Matrix4L();
	final int[] data = new int[DATA_LENGTH];
	//	final int[] events = new int[PIXEL_HEIGHT * 2];
	final int[] vertexData = new int[VERTEX_DATA_LENGTH];
	final long[] tiles = new long[TILE_COUNT];

	// Boumds of current triangle - pixel coordinates
	int minPixelX;
	int minPixelY;
	int maxPixelX;
	int maxPixelY;

	int clipX0;
	int clipY0;
	int clipX1;
	int clipY1;

	int position0;
	int position1;
	int position2;
	int position3;

	int ax0;
	int ay0;
	int ax1;
	int ay1;

	int bx0;
	int by0;
	int bx1;
	int by1;

	int cx0;
	int cy0;
	int cx1;
	int cy1;

	int dx0;
	int dy0;
	int dx1;
	int dy1;

	int minTileOriginX;
	int maxTileOriginX;
	int maxTileOriginY;

	int tileIndex;
	int tileOriginX;
	int tileOriginY;
	int save_tileIndex;
	int save_tileOriginX;
	int save_tileOriginY;

	final void drawQuad(int v0, int v1, int v2, int v3) {
		final int boundsResult  = prepareBounds(v0, v1, v2, v3);

		if (boundsResult == BOUNDS_OUTSIDE_OR_TOO_SMALL) {
			return;
		}

		// Don't draw single points
		if((minPixelX == maxPixelX && minPixelY == maxPixelY)) {
			return;
		}

		drawQuad();
	}

	boolean testQuad(int v0, int v1, int v2, int v3) {
		final int boundsResult  = prepareBounds(v0, v1, v2, v3);

		if (boundsResult == BOUNDS_OUTSIDE_OR_TOO_SMALL) {
			return false;
		}

		if((minPixelX == maxPixelX && minPixelY == maxPixelY)) {
			final int px = minPixelX;
			final int py = minPixelY;
			return px >= 0 && py >= 0 && px < PIXEL_WIDTH && py < PIXEL_HEIGHT && testPixel(px, py);
		} else {
			return testQuad();
		}
	}

	boolean testQuad() {
		boolean goRight = true;

		while(true) {
			if(testQuadInner()) {
				return true;
			}

			if (goRight) {
				if (tileOriginX == maxTileOriginX) {
					if(tileOriginY == maxTileOriginY) {
						return false;
					} else {
						moveTileUp();
						goRight = !goRight;
					}
				} else {
					moveTileRight();
				}
			} else {
				if (tileOriginX == minTileOriginX) {
					if(tileOriginY == maxTileOriginY) {
						return false;
					} else {
						moveTileUp();
						goRight = !goRight;
					}
				} else {
					moveTileLeft();
				}
			}
		}
	}

	boolean testQuadInner() {
		final long word = tiles[tileIndex];

		// nothing to test if fully occluded
		if  (word == -1L) {
			return false;
		}

		return (~word & computeTileCoverage()) != 0;
	}

	void drawQuad() {
		boolean goRight = true;

		while(true) {
			drawQuadInner();

			if (goRight) {
				if (tileOriginX == maxTileOriginX) {
					if(tileOriginY == maxTileOriginY) {
						return;
					} else {
						moveTileUp();
						goRight = !goRight;
					}
				} else {
					moveTileRight();
				}
			} else {
				if (tileOriginX == minTileOriginX) {
					if(tileOriginY == maxTileOriginY) {
						return;
					} else {
						moveTileUp();
						goRight = !goRight;
					}
				} else {
					moveTileLeft();
				}
			}
		}
	}

	void drawQuadInner() {
		assert tileOriginY < PIXEL_HEIGHT;
		assert tileOriginX < PIXEL_WIDTH;
		assert tileOriginX >= 0;

		long word = tiles[tileIndex];

		// nothing to do if fully occluded
		if  (word != -1L) {
			word |= computeTileCoverage();
			tiles[tileIndex] = word;
		}
	}

	void printMask8x8(long mask) {
		final String s = Strings.padStart(Long.toBinaryString(mask), 64, '0');
		System.out.println(StringUtils.reverse(s.substring(0, 8)).replace("0", "- ").replace("1", "X "));
		System.out.println(StringUtils.reverse(s.substring(8, 16)).replace("0", "- ").replace("1", "X "));
		System.out.println(StringUtils.reverse(s.substring(16, 24)).replace("0", "- ").replace("1", "X "));
		System.out.println(StringUtils.reverse(s.substring(24, 32)).replace("0", "- ").replace("1", "X "));
		System.out.println(StringUtils.reverse(s.substring(32, 40)).replace("0", "- ").replace("1", "X "));
		System.out.println(StringUtils.reverse(s.substring(40, 48)).replace("0", "- ").replace("1", "X "));
		System.out.println(StringUtils.reverse(s.substring(48, 56)).replace("0", "- ").replace("1", "X "));
		System.out.println(StringUtils.reverse(s.substring(56, 64)).replace("0", "- ").replace("1", "X "));

		System.out.println();
	}

	private void clipNear(int internal, int external) {
		final int[] vertexData = this.vertexData;

		final float intX = Float.intBitsToFloat(vertexData[internal + PV_X]);
		final float intY = Float.intBitsToFloat(vertexData[internal + PV_Y]);
		final float intZ = Float.intBitsToFloat(vertexData[internal + PV_Z]);
		final float intW = Float.intBitsToFloat(vertexData[internal + PV_W]);

		final float extX = Float.intBitsToFloat(vertexData[external + PV_X]);
		final float extY = Float.intBitsToFloat(vertexData[external + PV_Y]);
		final float extZ = Float.intBitsToFloat(vertexData[external + PV_Z]);
		final float extW = Float.intBitsToFloat(vertexData[external + PV_W]);

		// intersection point is the projection plane, at which point Z == 1
		// and w will be 0 but projection division isn't needed, so force output to W = 1
		// see https://www.cs.usfca.edu/~cruse/math202s11/homocoords.pdf

		final float wt = intZ  / -(extZ - intZ);

		// note again that projection division isn't needed
		final float x = (intX + (extX - intX) * wt);
		final float y = (intY + (extY - intY) * wt);
		final float w = (intW + (extW - intW) * wt);
		final float iw = 1f / w;

		clipX0 = Math.round(iw * x * HALF_PRECISE_WIDTH) + HALF_PRECISE_WIDTH;
		clipY0 = Math.round(iw * y * HALF_PRECISE_HEIGHT) + HALF_PRECISE_HEIGHT;
	}

	int prepareBounds(int v0, int v1, int v2, int v3) {
		// puts bits in lexical order
		final int split = needsNearClip(v3) | (needsNearClip(v2) << 1) | (needsNearClip(v1) << 2) | (needsNearClip(v0) << 3);

		switch (split) {
		case 0b0000:
			return prepareBounds0000(v0, v1, v2, v3);


		case 0b0001:
			return prepareBounds0001(v0, v1, v2, v3);

		case 0b0010:
			return prepareBounds0001(v3, v0, v1, v2);

		case 0b0100:
			return prepareBounds0001(v2, v3, v0, v1);

		case 0b1000:
			return prepareBounds0001(v1, v2, v3, v0);


		case 0b0011:
			return prepareBounds0011(v0, v1, v2, v3);

		case 0b1001:
			return prepareBounds0011(v1, v2, v3, v0);

		case 0b1100:
			return prepareBounds0011(v2, v3, v0, v1);

		case 0b0110:
			return prepareBounds0011(v3, v0, v1, v2);


		case 0b0111:
			return prepareBounds0111(v0, v1, v2, v3);

		case 0b1011:
			return prepareBounds0111(v1, v2, v3, v0);

		case 0b1101:
			return prepareBounds0111(v2, v3, v0, v1);

		case 0b1110:
			return prepareBounds0111(v3, v0, v1, v2);

		case 0b1111:
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;

		default:
			if (Configurator.traceOcclusionEdgeCases) {
				// Note: happens in rare cases that opposite corners are clipped.
				// Appears to be edge cases, possibly caused by rounding.
				// Does't seem to have
				CanvasMod.LOG.info("Invalid occlusion quad split. Printing z, w, z / w for each vertex.");

				final int[] data = vertexData;
				float w = Float.intBitsToFloat(data[v0 + PV_W]);
				float z = Float.intBitsToFloat(data[v0 + PV_Z]);
				CanvasMod.LOG.info(z + ",    " + w + ",   " + (z / w));

				w = Float.intBitsToFloat(data[v1 + PV_W]);
				z = Float.intBitsToFloat(data[v1 + PV_Z]);
				CanvasMod.LOG.info(z + ",    " + w + ",   " + (z / w));

				w = Float.intBitsToFloat(data[v2 + PV_W]);
				z = Float.intBitsToFloat(data[v2 + PV_Z]);
				CanvasMod.LOG.info(z + ",    " + w + ",   " + (z / w));

				w = Float.intBitsToFloat(data[v3 + PV_W]);
				z = Float.intBitsToFloat(data[v3 + PV_Z]);
				CanvasMod.LOG.info(z + ",    " + w + ",   " + (z / w));

				CanvasMod.LOG.info("");
			}
		}

		return BOUNDS_OUTSIDE_OR_TOO_SMALL;
	}

	private int prepareBounds0000(int v0, int v1, int v2, int v3) {
		final int[] vertexData = this.vertexData;
		int ax0, ay0, ax1, ay1;
		int bx0, by0, bx1, by1;
		int cx0, cy0, cx1, cy1;
		int dx0, dy0, dx1, dy1;
		int minY = 0, maxY = 0, minX = 0, maxX = 0;

		ax0 = vertexData[v0 + PV_PX];
		ay0 = vertexData[v0 + PV_PY];
		bx0 = vertexData[v1 + PV_PX];
		by0 = vertexData[v1 + PV_PY];
		cx0 = vertexData[v2 + PV_PX];
		cy0 = vertexData[v2 + PV_PY];
		dx0 = vertexData[v3 + PV_PX];
		dy0 = vertexData[v3 + PV_PY];

		ax1 = bx0;
		ay1 = by0;
		bx1 = cx0;
		by1 = cy0;
		cx1 = dx0;
		cy1 = dy0;
		dx1 = ax0;
		dy1 = ay0;

		minX = ax0;
		maxX = ax0;

		if (bx0 < minX) minX = bx0; else if (bx0 > maxX) maxX = bx0;
		if (cx0 < minX) minX = cx0; else if (cx0 > maxX) maxX = cx0;
		if (dx0 < minX) minX = dx0; else if (dx0 > maxX) maxX = dx0;

		minY = ay0;
		maxY = ay0;

		if (by0 < minY) minY = by0; else if (by0 > maxY) maxY = by0;
		if (cy0 < minY) minY = cy0; else if (cy0 > maxY) maxY = cy0;
		if (dy0 < minY) minY = dy0; else if (dy0 > maxY) maxY = dy0;

		if (((maxY - 1) | (maxX - 1) | (PRECISE_HEIGHT - 1 - minY) | (PRECISE_WIDTH - 1 - minX)) < 0) {

		}

		if (maxY <= 0 || minY >= PRECISE_HEIGHT) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		if (maxX <= 0 || minX >= PRECISE_WIDTH) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
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

		final int minPixelX = ((minX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int minPixelY = ((minY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelX = ((maxX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelY = ((maxY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);

		minTileOriginX = minPixelX & TILE_AXIS_MASK;
		maxTileOriginX = maxPixelX & TILE_AXIS_MASK;
		maxTileOriginY = maxPixelY & TILE_AXIS_MASK;

		tileOriginX = minPixelX & TILE_AXIS_MASK;
		tileOriginY = minPixelY & TILE_AXIS_MASK;
		tileIndex = tileIndex(minPixelX >> TILE_AXIS_SHIFT, minPixelY >> TILE_AXIS_SHIFT);

		final int position0 = edgePosition(ax0, ay0, ax1, ay1);
		final int position1 = edgePosition(bx0, by0, bx1, by1);
		final int position2 = edgePosition(cx0, cy0, cx1, cy1);
		final int position3 = edgePosition(dx0, dy0, dx1, dy1);

		this.minPixelX = minPixelX;
		this.minPixelY = minPixelY;
		this.maxPixelX = maxPixelX;
		this.maxPixelY = maxPixelY;
		this.ax0 = ax0;
		this.ay0 = ay0;
		this.ax1 = ax1;
		this.ay1 = ay1;
		this.bx0 = bx0;
		this.by0 = by0;
		this.bx1 = bx1;
		this.by1 = by1;
		this.cx0 = cx0;
		this.cy0 = cy0;
		this.cx1 = cx1;
		this.cy1 = cy1;
		this.dx0 = dx0;
		this.dy0 = dy0;
		this.dx1 = dx1;
		this.dy1 = dy1;
		this.position0 = position0;
		this.position1 = position1;
		this.position2 = position2;
		this.position3 = position3;

		final int eventKey = (position0 - 1) & EVENT_POSITION_MASK
				| (((position1 - 1) & EVENT_POSITION_MASK) << 2)
				| (((position2 - 1) & EVENT_POSITION_MASK) << 4)
				| (((position3 - 1) & EVENT_POSITION_MASK) << 6);

		EVENT_FILLERS[eventKey].apply();

		return BOUNDS_IN;
	}


	//	private void clipToGuards() {
	//		if ((((ax0 + GUARD_SIZE) | (ay0 + GUARD_SIZE) | (ax1 + GUARD_SIZE) | (ay1 + GUARD_SIZE)) & CLIP_MASK) != 0) {
	//			clipLine(ax0, ay0, ax1, ay1);
	//			ax0 = clipX0;
	//			ay0 = clipY0;
	//			ax1 = clipX1;
	//			ay1 = clipY1;
	//		}
	//
	//		if ((((bx0 + GUARD_SIZE) | (by0 + GUARD_SIZE) | (bx1 + GUARD_SIZE) | (by1 + GUARD_SIZE)) & CLIP_MASK) != 0) {
	//			clipLine(bx0, by0, bx1, by1);
	//			bx0 = clipX0;
	//			by0 = clipY0;
	//			bx1 = clipX1;
	//			by1 = clipY1;
	//		}
	//
	//		if ((((cx0 + GUARD_SIZE) | (cy0 + GUARD_SIZE) | (cx1 + GUARD_SIZE) | (cy1 + GUARD_SIZE)) & CLIP_MASK) != 0) {
	//			clipLine(cx0, cy0, cx1, cy1);
	//			cx0 = clipX0;
	//			cy0 = clipY0;
	//			cx1 = clipX1;
	//			cy1 = clipY1;
	//		}
	//
	//		if ((((dx0 + GUARD_SIZE) | (dy0 + GUARD_SIZE) | (dx1 + GUARD_SIZE) | (dy1 + GUARD_SIZE)) & CLIP_MASK) != 0) {
	//			clipLine(dx0, dy0, dx1, dy1);
	//			dx0 = clipX0;
	//			dy0 = clipY0;
	//			dx1 = clipX1;
	//			dy1 = clipY1;
	//		}
	//	}

	private int prepareBounds0001(int v0, int v1, int v2, int ext3) {
		final int[] vertexData = this.vertexData;
		int ax0, ay0, ax1, ay1;
		int bx0, by0, bx1, by1;
		int cx0, cy0, cx1, cy1;
		int dx0, dy0, dx1, dy1;
		int minY = 0, maxY = 0, minX = 0, maxX = 0;

		ax0 = vertexData[v0 + PV_PX];
		ay0 = vertexData[v0 + PV_PY];
		ax1 = vertexData[v1 + PV_PX];
		ay1 = vertexData[v1 + PV_PY];

		bx0 = ax1;
		by0 = ay1;
		bx1 = vertexData[v2 + PV_PX];
		by1 = vertexData[v2 + PV_PY];

		cx0 = bx1;
		cy0 = by1;
		clipNear(v2, ext3);
		cx1 = clipX0;
		cy1 = clipY0;

		clipNear(v0, ext3);
		dx0 = clipX0;
		dy0 = clipY0;
		dx1 = ax0;
		dy1 = ay0;

		minX = ax0;
		maxX = ax0;

		// ax1 = bx0 and dx1 = ax0,  so no need to test those
		if (bx0 < minX) minX = bx0; else if (bx0 > maxX) maxX = bx0;
		if (cx0 < minX) minX = cx0; else if (cx0 > maxX) maxX = cx0;
		if (cx1 < minX) minX = cx1; else if (cx1 > maxX) maxX = cx1;
		if (dx0 < minX) minX = dx0; else if (dx0 > maxX) maxX = dx0;

		minY = ay0;
		maxY = ay0;

		if (by0 < minY) minY = by0; else if (by0 > maxY) maxY = by0;
		if (cy0 < minY) minY = cy0; else if (cy0 > maxY) maxY = cy0;
		if (cy1 < minY) minY = cy1; else if (cy1 > maxY) maxY = cy1;
		if (dy0 < minY) minY = dy0; else if (dy0 > maxY) maxY = dy0;

		if (maxY <= 0 || minY >= PRECISE_HEIGHT) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		if (maxX <= 0 || minX >= PRECISE_WIDTH) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
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

		final int minPixelX = ((minX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int minPixelY = ((minY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelX = ((maxX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelY = ((maxY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);

		minTileOriginX = minPixelX & TILE_AXIS_MASK;
		maxTileOriginX = maxPixelX & TILE_AXIS_MASK;
		maxTileOriginY = maxPixelY & TILE_AXIS_MASK;

		tileOriginX = minPixelX & TILE_AXIS_MASK;
		tileOriginY = minPixelY & TILE_AXIS_MASK;
		tileIndex = tileIndex(minPixelX >> TILE_AXIS_SHIFT, minPixelY >> TILE_AXIS_SHIFT);

		final int position0 = edgePosition(ax0, ay0, ax1, ay1);
		final int position1 = edgePosition(bx0, by0, bx1, by1);
		final int position2 = edgePosition(cx0, cy0, cx1, cy1);
		final int position3 = edgePosition(dx0, dy0, dx1, dy1);

		this.minPixelX = minPixelX;
		this.minPixelY = minPixelY;
		this.maxPixelX = maxPixelX;
		this.maxPixelY = maxPixelY;
		this.ax0 = ax0;
		this.ay0 = ay0;
		this.ax1 = ax1;
		this.ay1 = ay1;
		this.bx0 = bx0;
		this.by0 = by0;
		this.bx1 = bx1;
		this.by1 = by1;
		this.cx0 = cx0;
		this.cy0 = cy0;
		this.cx1 = cx1;
		this.cy1 = cy1;
		this.dx0 = dx0;
		this.dy0 = dy0;
		this.dx1 = dx1;
		this.dy1 = dy1;
		this.position0 = position0;
		this.position1 = position1;
		this.position2 = position2;
		this.position3 = position3;

		final int eventKey = (position0 - 1) & EVENT_POSITION_MASK
				| (((position1 - 1) & EVENT_POSITION_MASK) << 2)
				| (((position2 - 1) & EVENT_POSITION_MASK) << 4)
				| (((position3 - 1) & EVENT_POSITION_MASK) << 6);

		EVENT_FILLERS[eventKey].apply();

		return BOUNDS_IN;
	}

	private int prepareBounds0011(int v0, int v1, int ext2, int ext3) {
		final int[] vertexData = this.vertexData;
		int ax0, ay0, ax1, ay1;
		int bx0, by0, bx1, by1;
		int cx0, cy0, cx1, cy1;
		int dx0, dy0, dx1, dy1;

		int minY = 0, maxY = 0, minX = 0, maxX = 0;

		ax0 = vertexData[v0 + PV_PX];
		ay0 = vertexData[v0 + PV_PY];
		ax1 = vertexData[v1 + PV_PX];
		ay1 = vertexData[v1 + PV_PY];

		bx0 = ax1;
		by0 = ay1;
		clipNear(v1, ext2);
		bx1 = clipX0;
		by1 = clipY0;

		// force line c to be a single, existing point - entire line is clipped and should not influence anything
		cx0 = ax0;
		cy0 = ay0;
		cx1 = ax0;
		cy1 = ay0;

		clipNear(v0, ext3);
		dx0 = clipX0;
		dy0 = clipY0;
		dx1 = ax0;
		dy1 = ay0;

		minX = ax0;
		maxX = ax0;

		if (bx0 < minX) minX = bx0; else if (bx0 > maxX) maxX = bx0;
		if (bx1 < minX) minX = bx1; else if (bx1 > maxX) maxX = bx1;
		if (dx0 < minX) minX = dx0; else if (dx0 > maxX) maxX = dx0;

		minY = ay0;
		maxY = ay0;

		if (by0 < minY) minY = by0; else if (by0 > maxY) maxY = by0;
		if (by1 < minY) minY = by1; else if (by1 > maxY) maxY = by1;
		if (dy0 < minY) minY = dy0; else if (dy0 > maxY) maxY = dy0;


		if (maxY <= 0 || minY >= PRECISE_HEIGHT) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		if (maxX <= 0 || minX >= PRECISE_WIDTH) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
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

		final int minPixelX = ((minX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int minPixelY = ((minY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelX = ((maxX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelY = ((maxY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);

		minTileOriginX = minPixelX & TILE_AXIS_MASK;
		maxTileOriginX = maxPixelX & TILE_AXIS_MASK;
		maxTileOriginY = maxPixelY & TILE_AXIS_MASK;

		tileOriginX = minPixelX & TILE_AXIS_MASK;
		tileOriginY = minPixelY & TILE_AXIS_MASK;
		tileIndex = tileIndex(minPixelX >> TILE_AXIS_SHIFT, minPixelY >> TILE_AXIS_SHIFT);

		final int position0 = edgePosition(ax0, ay0, ax1, ay1);
		final int position1 = edgePosition(bx0, by0, bx1, by1);
		final int position2 = edgePosition(cx0, cy0, cx1, cy1);
		final int position3 = edgePosition(dx0, dy0, dx1, dy1);

		this.minPixelX = minPixelX;
		this.minPixelY = minPixelY;
		this.maxPixelX = maxPixelX;
		this.maxPixelY = maxPixelY;
		this.ax0 = ax0;
		this.ay0 = ay0;
		this.ax1 = ax1;
		this.ay1 = ay1;
		this.bx0 = bx0;
		this.by0 = by0;
		this.bx1 = bx1;
		this.by1 = by1;
		this.cx0 = cx0;
		this.cy0 = cy0;
		this.cx1 = cx1;
		this.cy1 = cy1;
		this.dx0 = dx0;
		this.dy0 = dy0;
		this.dx1 = dx1;
		this.dy1 = dy1;
		this.position0 = position0;
		this.position1 = position1;
		this.position2 = position2;
		this.position3 = position3;

		final int eventKey = (position0 - 1) & EVENT_POSITION_MASK
				| (((position1 - 1) & EVENT_POSITION_MASK) << 2)
				| (((position2 - 1) & EVENT_POSITION_MASK) << 4)
				| (((position3 - 1) & EVENT_POSITION_MASK) << 6);

		EVENT_FILLERS[eventKey].apply();

		return BOUNDS_IN;
	}

	private int prepareBounds0111(int v0, int ext1, int ext2, int ext3) {
		final int[] vertexData = this.vertexData;
		int ax0, ay0, ax1, ay1;
		int bx0, by0, bx1, by1;
		int cx0, cy0, cx1, cy1;
		int dx0, dy0, dx1, dy1;
		int minY = 0, maxY = 0, minX = 0, maxX = 0;

		ax0 = vertexData[v0 + PV_PX];
		ay0 = vertexData[v0 + PV_PY];
		clipNear(v0, ext1);
		ax1 = clipX0;
		ay1 = clipY0;

		// force lines b & c to be a single, existing point - entire line is clipped and should not influence anything
		bx0 = ax0;
		by0 = ay0;
		bx1 = ax0;
		by1 = ay0;
		cx0 = ax0;
		cy0 = ay0;
		cx1 = ax0;
		cy1 = ay0;

		clipNear(v0, ext3);
		dx0 = clipX0;
		dy0 = clipY0;
		dx1 = ax0;
		dy1 = ay0;

		minX = ax0;
		maxX = ax0;

		if (ax1 < minX) minX = ax1; else if (ax1 > maxX) maxX = ax1;
		if (dx0 < minX) minX = dx0; else if (dx0 > maxX) maxX = dx0;

		minY = ay0;
		maxY = ay0;

		if (ay1 < minY) minY = ay1; else if (ay1 > maxY) maxY = ay1;
		if (dy0 < minY) minY = dy0; else if (dy0 > maxY) maxY = dy0;

		if (maxY <= 0 || minY >= PRECISE_HEIGHT) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}


		if (maxX <= 0 || minX >= PRECISE_WIDTH) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
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

		final int minPixelX = ((minX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int minPixelY = ((minY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelX = ((maxX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelY = ((maxY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);

		minTileOriginX = minPixelX & TILE_AXIS_MASK;
		maxTileOriginX = maxPixelX & TILE_AXIS_MASK;
		maxTileOriginY = maxPixelY & TILE_AXIS_MASK;

		tileOriginX = minPixelX & TILE_AXIS_MASK;
		tileOriginY = minPixelY & TILE_AXIS_MASK;
		tileIndex = tileIndex(minPixelX >> TILE_AXIS_SHIFT, minPixelY >> TILE_AXIS_SHIFT);

		final int position0 = edgePosition(ax0, ay0, ax1, ay1);
		final int position1 = edgePosition(bx0, by0, bx1, by1);
		final int position2 = edgePosition(cx0, cy0, cx1, cy1);
		final int position3 = edgePosition(dx0, dy0, dx1, dy1);

		this.minPixelX = minPixelX;
		this.minPixelY = minPixelY;
		this.maxPixelX = maxPixelX;
		this.maxPixelY = maxPixelY;
		this.ax0 = ax0;
		this.ay0 = ay0;
		this.ax1 = ax1;
		this.ay1 = ay1;
		this.bx0 = bx0;
		this.by0 = by0;
		this.bx1 = bx1;
		this.by1 = by1;
		this.cx0 = cx0;
		this.cy0 = cy0;
		this.cx1 = cx1;
		this.cy1 = cy1;
		this.dx0 = dx0;
		this.dy0 = dy0;
		this.dx1 = dx1;
		this.dy1 = dy1;
		this.position0 = position0;
		this.position1 = position1;
		this.position2 = position2;
		this.position3 = position3;

		final int eventKey = (position0 - 1) & EVENT_POSITION_MASK
				| (((position1 - 1) & EVENT_POSITION_MASK) << 2)
				| (((position2 - 1) & EVENT_POSITION_MASK) << 4)
				| (((position3 - 1) & EVENT_POSITION_MASK) << 6);

		EVENT_FILLERS[eventKey].apply();

		return BOUNDS_IN;
	}

	@FunctionalInterface interface EventFiller {
		void apply();
	}

	private final EventFiller[] EVENT_FILLERS = new EventFiller[0x1000];

	{
		EVENT_FILLERS[EVENT_0123_RRRR] = () -> {
			populateLeftEvents();
			populateRightEvents4(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1, cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
		};
		EVENT_FILLERS[EVENT_0123_LRRR] = () -> {
			populateLeftEvents(ax0, ay0, ax1, ay1);
			populateRightEvents3(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
		};
		EVENT_FILLERS[EVENT_0123_FRRR] = () -> {
			populateLeftEvents();
			populateRightEvents3(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
			populateFlatEvents(position0, ay0);
		};
		EVENT_FILLERS[EVENT_0123_RLRR] = () -> {
			populateLeftEvents(bx0, by0, bx1, by1);
			populateRightEvents3(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
		};
		EVENT_FILLERS[EVENT_0123_LLRR] = () -> {
			populateLeftEvents2(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1);
			populateRightEvents2(cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
		};
		EVENT_FILLERS[EVENT_0123_FLRR] = () -> {
			populateLeftEvents(bx0, by0, bx1, by1);
			populateRightEvents2(cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
			populateFlatEvents(position0, ay0);
		};
		EVENT_FILLERS[EVENT_0123_RFRR] = () -> {
			populateLeftEvents();
			populateRightEvents3(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
			populateFlatEvents(position1, by0);
		};
		EVENT_FILLERS[EVENT_0123_LFRR] = () -> {
			populateLeftEvents(ax0, ay0, ax1, ay1);
			populateRightEvents2(cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
			populateFlatEvents(position1, by0);
		};
		EVENT_FILLERS[EVENT_0123_FFRR] = () -> {
			populateLeftEvents();
			populateRightEvents2(cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position1, by0);
		};

		EVENT_FILLERS[EVENT_0123_RRLR] = () -> {
			populateLeftEvents(cx0, cy0, cx1, cy1);
			populateRightEvents3(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1, dx0, dy0, dx1, dy1);
		};
		EVENT_FILLERS[EVENT_0123_LRLR] = () -> {
			populateLeftEvents2(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1);
			populateRightEvents2(bx0, by0, bx1, by1, dx0, dy0, dx1, dy1);
		};
		EVENT_FILLERS[EVENT_0123_FRLR] = () -> {
			populateLeftEvents(cx0, cy0, cx1, cy1);
			populateRightEvents2(bx0, by0, bx1, by1, dx0, dy0, dx1, dy1);
			populateFlatEvents(position0, ay0);
		};
		EVENT_FILLERS[EVENT_0123_RLLR] = () -> {
			populateLeftEvents2(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
			populateRightEvents2(ax0, ay0, ax1, ay1, dx0, dy0, dx1, dy1);
		};
		EVENT_FILLERS[EVENT_0123_LLLR] = () -> {
			populateLeftEvents3(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
			populateRightEvents(dx0, dy0, dx1, dy1);
		};
		EVENT_FILLERS[EVENT_0123_FLLR] = () -> {
			populateLeftEvents2(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
			populateRightEvents(dx0, dy0, dx1, dy1);
			populateFlatEvents(position0, ay0);
		};
		EVENT_FILLERS[EVENT_0123_RFLR] = () -> {
			populateLeftEvents(cx0, cy0, cx1, cy1);
			populateRightEvents2(ax0, ay0, ax1, ay1, dx0, dy0, dx1, dy1);
			populateFlatEvents(position1, by0);
		};
		EVENT_FILLERS[EVENT_0123_LFLR] = () -> {
			populateLeftEvents2(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1);
			populateRightEvents(dx0, dy0, dx1, dy1);
			populateFlatEvents(position1, by0);
		};
		EVENT_FILLERS[EVENT_0123_FFLR] = () -> {
			populateLeftEvents(cx0, cy0, cx1, cy1);
			populateRightEvents(dx0, dy0, dx1, dy1);
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position1, by0);
		};

		EVENT_FILLERS[EVENT_0123_LRFR] = () -> {
			populateLeftEvents(ax0, ay0, ax1, ay1);
			populateRightEvents2(bx0, by0, bx1, by1, dx0, dy0, dx1, dy1);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_RRFR] = () -> {
			populateLeftEvents();
			populateRightEvents3(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1, dx0, dy0, dx1, dy1);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_FRFR] = () -> {
			populateLeftEvents();
			populateRightEvents2(bx0, by0, bx1, by1, dx0, dy0, dx1, dy1);
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_RLFR] = () -> {
			populateLeftEvents(bx0, by0, bx1, by1);
			populateRightEvents2(ax0, ay0, ax1, ay1, dx0, dy0, dx1, dy1);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_LLFR] = () -> {
			populateLeftEvents2(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1);
			populateRightEvents(dx0, dy0, dx1, dy1);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_FLFR] = () -> {
			populateLeftEvents(bx0, by0, bx1, by1);
			populateRightEvents(dx0, dy0, dx1, dy1);
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_RFFR] = () -> {
			populateLeftEvents();
			populateRightEvents2(ax0, ay0, ax1, ay1, dx0, dy0, dx1, dy1);
			populateFlatEvents(position1, by0);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_LFFR] = () -> {
			populateLeftEvents(ax0, ay0, ax1, ay1);
			populateRightEvents(dx0, dy0, dx1, dy1);
			populateFlatEvents(position1, by0);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_FFFR] = () -> {
			populateLeftEvents();
			populateRightEvents(dx0, dy0, dx1, dy1);
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position1, by0);
			populateFlatEvents(position2, cy0);
		};


		EVENT_FILLERS[EVENT_0123_RRRL] = () -> {
			populateLeftEvents(dx0, dy0, dx1, dy1);
			populateRightEvents3(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
		};
		EVENT_FILLERS[EVENT_0123_LRRL] = () -> {
			populateLeftEvents2(ax0, ay0, ax1, ay1, dx0, dy0, dx1, dy1);
			populateRightEvents2(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
		};
		EVENT_FILLERS[EVENT_0123_FRRL] = () -> {
			populateLeftEvents(dx0, dy0, dx1, dy1);
			populateRightEvents2(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
			populateFlatEvents(position0, ay0);
		};
		EVENT_FILLERS[EVENT_0123_RLRL] = () -> {
			populateLeftEvents2(bx0, by0, bx1, by1, dx0, dy0, dx1, dy1);
			populateRightEvents2(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1);
		};
		EVENT_FILLERS[EVENT_0123_LLRL] = () -> {
			populateLeftEvents3(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1, dx0, dy0, dx1, dy1);
			populateRightEvents(cx0, cy0, cx1, cy1);
		};
		EVENT_FILLERS[EVENT_0123_FLRL] = () -> {
			populateLeftEvents2(bx0, by0, bx1, by1, dx0, dy0, dx1, dy1);
			populateRightEvents(cx0, cy0, cx1, cy1);
			populateFlatEvents(position0, ay0);
		};
		EVENT_FILLERS[EVENT_0123_RFRL] = () -> {
			populateLeftEvents(dx0, dy0, dx1, dy1);
			populateRightEvents2(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1);
			populateFlatEvents(position1, by0);
		};
		EVENT_FILLERS[EVENT_0123_LFRL] = () -> {
			populateLeftEvents2(ax0, ay0, ax1, ay1, dx0, dy0, dx1, dy1);
			populateRightEvents(cx0, cy0, cx1, cy1);
			populateFlatEvents(position1, by0);
		};
		EVENT_FILLERS[EVENT_0123_FFRL] = () -> {
			populateLeftEvents(dx0, dy0, dx1, dy1);
			populateRightEvents(cx0, cy0, cx1, cy1);
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position1, by0);
		};
		EVENT_FILLERS[EVENT_0123_RRLL] = () -> {
			populateLeftEvents2(cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
			populateRightEvents2(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1);
		};
		EVENT_FILLERS[EVENT_0123_LRLL] = () -> {
			populateLeftEvents3(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
			populateRightEvents(bx0, by0, bx1, by1);
		};
		EVENT_FILLERS[EVENT_0123_FRLL] = () -> {
			populateLeftEvents2(cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
			populateRightEvents(bx0, by0, bx1, by1);
			populateFlatEvents(position0, ay0);
		};
		EVENT_FILLERS[EVENT_0123_RLLL] = () -> {
			populateLeftEvents3(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
			populateRightEvents(ax0, ay0, ax1, ay1);
		};
		EVENT_FILLERS[EVENT_0123_LLLL] = () -> {
			populateLeftEvents4(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1, cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
			populateRightEvents();
		};
		EVENT_FILLERS[EVENT_0123_FLLL] = () -> {
			populateLeftEvents3(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
			populateRightEvents();
			populateFlatEvents(position0, ay0);
		};
		EVENT_FILLERS[EVENT_0123_RFLL] = () -> {
			populateLeftEvents2(cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
			populateRightEvents(ax0, ay0, ax1, ay1);
			populateFlatEvents(position1, by0);
		};
		EVENT_FILLERS[EVENT_0123_LFLL] = () -> {
			populateLeftEvents3(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
			populateRightEvents();
			populateFlatEvents(position1, by0);
		};
		EVENT_FILLERS[EVENT_0123_FFLL] = () -> {
			populateLeftEvents2(cx0, cy0, cx1, cy1, dx0, dy0, dx1, dy1);
			populateRightEvents();
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position1, by0);
		};
		EVENT_FILLERS[EVENT_0123_LRFL] = () -> {
			populateLeftEvents2(ax0, ay0, ax1, ay1, dx0, dy0, dx1, dy1);
			populateRightEvents(bx0, by0, bx1, by1);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_RRFL] = () -> {
			populateLeftEvents(dx0, dy0, dx1, dy1);
			populateRightEvents2(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_FRFL] = () -> {
			populateLeftEvents(dx0, dy0, dx1, dy1);
			populateRightEvents(bx0, by0, bx1, by1);
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_RLFL] = () -> {
			populateLeftEvents2(bx0, by0, bx1, by1, dx0, dy0, dx1, dy1);
			populateRightEvents(ax0, ay0, ax1, ay1);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_LLFL] = () -> {
			populateLeftEvents3(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1, dx0, dy0, dx1, dy1);
			populateRightEvents();
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_FLFL] = () -> {
			populateLeftEvents2(bx0, by0, bx1, by1, dx0, dy0, dx1, dy1);
			populateRightEvents();
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_RFFL] = () -> {
			populateLeftEvents(dx0, dy0, dx1, dy1);
			populateRightEvents(ax0, ay0, ax1, ay1);
			populateFlatEvents(position1, by0);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_LFFL] = () -> {
			populateLeftEvents2(ax0, ay0, ax1, ay1, dx0, dy0, dx1, dy1);
			populateRightEvents();
			populateFlatEvents(position1, by0);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_FFFL] = () -> {
			populateLeftEvents(dx0, dy0, dx1, dy1);
			populateRightEvents();
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position1, by0);
			populateFlatEvents(position2, cy0);
		};
		EVENT_FILLERS[EVENT_0123_RRRF] = () -> {
			populateLeftEvents();
			populateRightEvents3(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_LRRF] = () -> {
			populateLeftEvents(ax0, ay0, ax1, ay1);
			populateRightEvents2(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_FRRF] = () -> {
			populateLeftEvents();
			populateRightEvents2(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_RLRF] = () -> {
			populateLeftEvents(bx0, by0, bx1, by1);
			populateRightEvents2(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_LLRF] = () -> {
			populateLeftEvents2(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1);
			populateRightEvents(cx0, cy0, cx1, cy1);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_FLRF] = () -> {
			populateLeftEvents(bx0, by0, bx1, by1);
			populateRightEvents(cx0, cy0, cx1, cy1);
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_RFRF] = () -> {
			populateLeftEvents();
			populateRightEvents2(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1);
			populateFlatEvents(position1, by0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_LFRF] = () -> {
			populateLeftEvents(ax0, ay0, ax1, ay1);
			populateRightEvents(cx0, cy0, cx1, cy1);
			populateFlatEvents(position1, by0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_FFRF] = () -> {
			populateLeftEvents();
			populateRightEvents(cx0, cy0, cx1, cy1);
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position1, by0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_RRLF] = () -> {
			populateLeftEvents(cx0, cy0, cx1, cy1);
			populateRightEvents2(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_LRLF] = () -> {
			populateLeftEvents2(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1);
			populateRightEvents(bx0, by0, bx1, by1);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_FRLF] = () -> {
			populateLeftEvents(cx0, cy0, cx1, cy1);
			populateRightEvents(bx0, by0, bx1, by1);
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_RLLF] = () -> {
			populateLeftEvents2(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
			populateRightEvents(ax0, ay0, ax1, ay1);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_LLLF] = () -> {
			populateLeftEvents3(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
			populateRightEvents();
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_FLLF] = () -> {
			populateLeftEvents2(bx0, by0, bx1, by1, cx0, cy0, cx1, cy1);
			populateRightEvents();
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_RFLF] = () -> {
			populateLeftEvents(cx0, cy0, cx1, cy1);
			populateRightEvents(ax0, ay0, ax1, ay1);
			populateFlatEvents(position1, by0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_LFLF] = () -> {
			populateLeftEvents2(ax0, ay0, ax1, ay1, cx0, cy0, cx1, cy1);
			populateRightEvents();
			populateFlatEvents(position1, by0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_FFLF] = () -> {
			populateLeftEvents(cx0, cy0, cx1, cy1);
			populateRightEvents();
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position1, by0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_LRFF] = () -> {
			populateLeftEvents(ax0, ay0, ax1, ay1);
			populateRightEvents(bx0, by0, bx1, by1);
			populateFlatEvents(position2, cy0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_RRFF] = () -> {
			populateLeftEvents();
			populateRightEvents2(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1);
			populateFlatEvents(position2, cy0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_FRFF] = () -> {
			populateLeftEvents();
			populateRightEvents(bx0, by0, bx1, by1);
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position2, cy0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_RLFF] = () -> {
			populateLeftEvents(bx0, by0, bx1, by1);
			populateRightEvents(ax0, ay0, ax1, ay1);
			populateFlatEvents(position2, cy0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_LLFF] = () -> {
			populateLeftEvents2(ax0, ay0, ax1, ay1, bx0, by0, bx1, by1);
			populateRightEvents();
			populateFlatEvents(position2, cy0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_FLFF] = () -> {
			populateLeftEvents(bx0, by0, bx1, by1);
			populateRightEvents();
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position2, cy0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_RFFF] = () -> {
			populateLeftEvents();
			populateRightEvents(ax0, ay0, ax1, ay1);
			populateFlatEvents(position1, by0);
			populateFlatEvents(position2, cy0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_LFFF] = () -> {
			populateLeftEvents(ax0, ay0, ax1, ay1);
			populateRightEvents();
			populateFlatEvents(position1, by0);
			populateFlatEvents(position2, cy0);
			populateFlatEvents(position3, dy0);
		};
		EVENT_FILLERS[EVENT_0123_FFFF] = () -> {
			// fill it
			populateLeftEvents();
			populateRightEvents();
			populateFlatEvents(position0, ay0);
			populateFlatEvents(position1, by0);
			populateFlatEvents(position2, cy0);
			populateFlatEvents(position3, dy0);
		};

	}

	private int edgePosition(int x0In, int y0In, int x1In, int y1In) {
		final int dy = y1In - y0In;
		final int dx = x1In - x0In;
		// signum of dx and dy, with shifted masks to derive the edge constant directly
		// the edge constants are specifically formulated to allow this, inline, avoids any pointer chases
		// sign of dy is inverted for historical reasons
		return (1 << (((-dy >> 31) | (dy >>> 31)) + 1)) | (1 << (((dx >> 31) | (-dx >>> 31)) + 4));
	}

	private void populateFlatEvents(int position, int y0In) {
		if (position == EDGE_TOP) {
			final int py = ((y0In + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) + 1;
			final int[] data = this.data;

			if (py == MAX_PIXEL_Y) return;

			final int y1 = maxTileOriginY + 7;
			final int start = IDX_EVENTS + (py < 0 ? 0 : (py << 1));
			final int limit = IDX_EVENTS + (y1 << 1);

			assert limit < EVENTS_LENGTH + IDX_EVENTS;

			for (int y = start; y <= limit; ) {
				data[y++] = PIXEL_WIDTH;
				data[y++] = -1;
			}
		}  else if (position == EDGE_BOTTOM) {
			final int py = (y0In >> PRECISION_BITS);

			if (py == 0) return;

			final int y0 = minPixelY & TILE_AXIS_MASK;
			final int start = IDX_EVENTS + (y0 << 1);
			final int limit = IDX_EVENTS + (py > MAX_PIXEL_Y ? (MAX_PIXEL_Y << 1) : (py << 1));

			assert limit < EVENTS_LENGTH + IDX_EVENTS;

			for (int y = start; y < limit; ) {
				data[y++] = PIXEL_WIDTH;
				data[y++] = -1;
			}
		} else {
			assert position == EDGE_POINT;
		}
	}

	/** Puts left edge at screen boundary */
	private void populateLeftEvents() {
		final int[] data = this.data;
		final int y0 = minPixelY & TILE_AXIS_MASK;
		final int y1 = maxTileOriginY + 7;
		final int limit = IDX_EVENTS + (y1 << 1);

		for (int y = IDX_EVENTS + (y0 << 1); y <= limit; y += 2) {
			data[y] = 0;
		}
	}

	private void populateLeftEvents(int x0In, int y0In, int x1In, int y1In) {
		final int y0 = minPixelY & TILE_AXIS_MASK;
		final int y1 = maxTileOriginY + 7;
		final int limit = (y1 << 1);
		final int dx = x1In - x0In;
		final int[] data = this.data;

		final long nStep;
		long x;

		if (dx == 0) {
			x = ((x0In + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			nStep = 0;
		} else {
			final int dy = y1In - y0In;
			final long n = (((long)dx) << 16) / dy;
			nStep = n << PRECISION_BITS;
			x = ((long) x0In << 16) - n * y0In + nStep * y0 + 0x100000L;
		}

		for (int y = (y0 << 1); y <= limit; y += 2) {
			data[IDX_EVENTS + y] = (int) (x > 0 ? (x >> 20) : 0);
			x += nStep;
		}
	}

	private void populateRightEvents() {
		final int[] data = this.data;
		final int y0 = minPixelY & TILE_AXIS_MASK;
		final int y1 = maxTileOriginY + 7;
		// difference from left: is high index in pairs
		final int limit = (y1 << 1) + 1;

		// difference from left: is high index in pairs
		for (int y = (y0 << 1) + 1; y <= limit; y += 2) {
			data[IDX_EVENTS + y] = PIXEL_WIDTH;
		}
	}

	private void populateRightEvents(int x0In, int y0In, int x1In, int y1In) {
		final int y0 = minPixelY & TILE_AXIS_MASK;
		final int y1 = maxTileOriginY + 7;
		// difference from left: is high index in pairs
		final int limit = (y1 << 1) + 1;
		final int dx = x1In - x0In;
		final int[] data = this.data;

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
			x = ((long) x0In << 16) - n * y0In + nStep * y0 + 0x7FFFFL;
		}

		// difference from left: is high index in pairs
		for (int y = (y0 << 1) + 1; y <= limit; y += 2) {
			data[IDX_EVENTS + y] = (int) (x >= 0 ? (x >> 20) : -1);
			x += nStep;
		}
	}

	private void populateLeftEvents2(int ax0, int ay0, int ax1, int ay1, int bx0, int by0, int bx1, int by1) {
		final int y0 = minPixelY & TILE_AXIS_MASK;
		final int y1 = maxTileOriginY + 7;
		final int limit = (y1 << 1);
		final int[] data = this.data;

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
			ax = ((long) ax0 << 16) - an * ay0 + aStep * y0 + 0x100000L;
		}

		if (bx0 == bx1) {
			bx = ((bx0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			bStep = 0;
		} else {
			final int bdx = bx1 - bx0;
			final int bdy = by1 - by0;
			final long bn = (((long)bdx) << 16) / bdy;
			bStep = bn << PRECISION_BITS;
			bx = ((long) bx0 << 16) - bn * by0 + bStep * y0 + 0x100000L;
		}

		for (int y = (y0 << 1); y <= limit; y += 2) {
			final long x = ax > bx ? ax : bx;

			data[IDX_EVENTS + y] = (int) (x > 0 ? (x >> 20) : 0);

			ax += aStep;
			bx += bStep;
		}
	}

	private void populateLeftEvents3(int ax0, int ay0, int ax1, int ay1, int bx0, int by0, int bx1, int by1, int cx0, int cy0, int cx1, int cy1) {
		final int y0 = minPixelY & TILE_AXIS_MASK;
		final int y1 = maxTileOriginY + 7;
		final int limit = (y1 << 1);
		final int[] data = this.data;

		final long aStep;
		long ax;
		final long bStep;
		long bx;
		final long cStep;
		long cx;

		if (ax0 == ax1) {
			ax = ((ax0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			aStep = 0;
		} else {
			final int ady = ay1 - ay0;
			final int adx = ax1 - ax0;
			final long an = (((long)adx) << 16) / ady;
			aStep = an << PRECISION_BITS;
			ax = ((long) ax0 << 16) - an * ay0 + aStep * y0 + 0x100000L;
		}

		if (bx0 == bx1) {
			bx = ((bx0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			bStep = 0;
		} else {
			final int bdx = bx1 - bx0;
			final int bdy = by1 - by0;
			final long bn = (((long)bdx) << 16) / bdy;
			bStep = bn << PRECISION_BITS;
			bx = ((long) bx0 << 16) - bn * by0 + bStep * y0 + 0x100000L;
		}

		if (cx0 == cx1) {
			cx = ((cx0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			cStep = 0;
		} else {
			final int cdx = cx1 - cx0;
			final int cdy = cy1 - cy0;
			final long cn = (((long)cdx) << 16) / cdy;
			cStep = cn << PRECISION_BITS;
			cx = ((long) cx0 << 16) - cn * cy0 + cStep * y0 + 0x100000L;
		}

		for (int y = (y0 << 1); y <= limit; y += 2) {
			long x = ax > bx ? ax : bx;
			if (cx > x) x = cx;

			data[IDX_EVENTS + y] = (int) (x > 0 ? (x >> 20) : 0);

			ax += aStep;
			bx += bStep;
			cx += cStep;
		}
	}

	private void populateLeftEvents4(int ax0, int ay0, int ax1, int ay1, int bx0, int by0, int bx1, int by1, int cx0, int cy0, int cx1, int cy1, int dx0, int dy0, int dx1, int dy1) {
		final int y0 = minPixelY & TILE_AXIS_MASK;
		final int y1 = maxTileOriginY + 7;
		final int limit = (y1 << 1);
		final int[] data = this.data;

		final long aStep;
		long ax;
		final long bStep;
		long bx;
		final long cStep;
		long cx;
		final long dStep;
		long dx;

		if (ax0 == ax1) {
			ax = ((ax0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			aStep = 0;
		} else {
			final int ady = ay1 - ay0;
			final int adx = ax1 - ax0;
			final long an = (((long)adx) << 16) / ady;
			aStep = an << PRECISION_BITS;
			ax = ((long) ax0 << 16) - an * ay0 + aStep * y0 + 0x100000L;
		}

		if (bx0 == bx1) {
			bx = ((bx0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			bStep = 0;
		} else {
			final int bdx = bx1 - bx0;
			final int bdy = by1 - by0;
			final long bn = (((long)bdx) << 16) / bdy;
			bStep = bn << PRECISION_BITS;
			bx = ((long) bx0 << 16) - bn * by0 + bStep * y0 + 0x100000L;
		}

		if (cx0 == cx1) {
			cx = ((cx0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			cStep = 0;
		} else {
			final int cdx = cx1 - cx0;
			final int cdy = cy1 - cy0;
			final long cn = (((long)cdx) << 16) / cdy;
			cStep = cn << PRECISION_BITS;
			cx = ((long) cx0 << 16) - cn * cy0 + cStep * y0 + 0x100000L;
		}

		if (dx0 == dx1) {
			dx = ((dx0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			dStep = 0;
		} else {
			final int ddx = dx1 - dx0;
			final int ddy = dy1 - dy0;
			final long dn = (((long)ddx) << 16) / ddy;
			dStep = dn << PRECISION_BITS;
			dx = ((long) dx0 << 16) - dn * dy0 + dStep * y0 + 0x100000L;
		}

		for (int y = (y0 << 1); y <= limit; y += 2) {
			long x = ax > bx ? ax : bx;
			if (cx > x) x = cx;
			if (dx > x) x = dx;

			data[IDX_EVENTS + y] = (int) (x > 0 ? (x >> 20) : 0);

			ax += aStep;
			bx += bStep;
			cx += cStep;
			dx += dStep;
		}
	}

	private void populateRightEvents2(int ax0, int ay0, int ax1, int ay1, int bx0, int by0, int bx1, int by1) {
		final int y0 = minPixelY & TILE_AXIS_MASK;
		final int y1 = maxTileOriginY + 7;
		// difference from left: is high index in pairs
		final int limit = (y1 << 1) + 1;
		final int[] data = this.data;

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
			ax = ((long) ax0 << 16) - an * ay0 + aStep * y0 + 0x7FFFFL;
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
			bx = ((long) bx0 << 16) - bn * by0 + bStep * y0 + 0x7FFFFL;
		}

		// difference from left: is high index in pairs
		for (int y = (y0 << 1) + 1; y <= limit; y += 2) {
			// difference from left: lower value wins
			final long x = ax < bx ? ax : bx;

			data[IDX_EVENTS + y] = (int) (x >= 0 ? (x >> 20) : -1);

			ax += aStep;
			bx += bStep;
		}
	}

	private void populateRightEvents3(int ax0, int ay0, int ax1, int ay1, int bx0, int by0, int bx1, int by1, int cx0, int cy0, int cx1, int cy1) {
		final int y0 = minPixelY & TILE_AXIS_MASK;
		final int y1 = maxTileOriginY + 7;
		// difference from left: is high index in pairs
		final int limit = (y1 << 1) + 1;
		final int[] data = this.data;

		final long aStep;
		long ax;
		final long bStep;
		long bx;
		final long cStep;
		long cx;

		if (ax0 == ax1) {
			ax = ((ax0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			aStep = 0;
		} else {
			final int ady = ay1 - ay0;
			final int adx = ax1 - ax0;
			final long an = (((long)adx) << 16) / ady;
			aStep = an << PRECISION_BITS;
			// difference from left: rounding looses tie
			ax = ((long) ax0 << 16) - an * ay0 + aStep * y0 + 0x7FFFFL;
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
			bx = ((long) bx0 << 16) - bn * by0 + bStep * y0 + 0x7FFFFL;
		}

		if (cx0 == cx1) {
			cx = ((cx0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			cStep = 0;
		} else {
			final int cdx = cx1 - cx0;
			final int cdy = cy1 - cy0;
			final long cn = (((long)cdx) << 16) / cdy;
			cStep = cn << PRECISION_BITS;
			// difference from left: rounding looses tie
			cx = ((long) cx0 << 16) - cn * cy0 + cStep * y0 + 0x7FFFFL;
		}

		// difference from left: is high index in pairs
		for (int y = (y0 << 1) + 1; y <= limit; y += 2) {
			// difference from left: lower value wins
			long x = ax < bx ? ax : bx;

			if (cx < x) x = cx;

			data[IDX_EVENTS + y] = (int) (x >= 0 ? (x >> 20) : -1);

			ax += aStep;
			bx += bStep;
			cx += cStep;
		}
	}

	private void populateRightEvents4(int ax0, int ay0, int ax1, int ay1, int bx0, int by0, int bx1, int by1, int cx0, int cy0, int cx1, int cy1, int dx0, int dy0, int dx1, int dy1) {
		final int y0 = minPixelY & TILE_AXIS_MASK;
		final int y1 = maxTileOriginY + 7;
		// difference from left: is high index in pairs
		final int limit = (y1 << 1) + 1;
		final int[] data = this.data;

		final long aStep;
		long ax;
		final long bStep;
		long bx;
		final long cStep;
		long cx;
		final long dStep;
		long dx;

		if (ax0 == ax1) {
			ax = ((ax0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			aStep = 0;
		} else {
			final int ady = ay1 - ay0;
			final int adx = ax1 - ax0;
			final long an = (((long)adx) << 16) / ady;
			aStep = an << PRECISION_BITS;
			// difference from left: rounding looses tie
			ax = ((long) ax0 << 16) - an * ay0 + aStep * y0 + 0x7FFFFL;
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
			bx = ((long) bx0 << 16) - bn * by0 + bStep * y0 + 0x7FFFFL;
		}

		if (cx0 == cx1) {
			cx = ((cx0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			cStep = 0;
		} else {
			final int cdx = cx1 - cx0;
			final int cdy = cy1 - cy0;
			final long cn = (((long)cdx) << 16) / cdy;
			cStep = cn << PRECISION_BITS;
			// difference from left: rounding looses tie
			cx = ((long) cx0 << 16) - cn * cy0 + cStep * y0 + 0x7FFFFL;
		}

		if (dx0 == dx1) {
			dx = ((dx0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			dStep = 0;
		} else {
			final int ddx = dx1 - dx0;
			final int ddy = dy1 - dy0;
			final long dn = (((long)ddx) << 16) / ddy;
			dStep = dn << PRECISION_BITS;
			// difference from left: rounding looses tie
			dx = ((long) dx0 << 16) - dn * dy0 + dStep * y0 + 0x7FFFFL;
		}

		// difference from left: is high index in pairs
		for (int y = (y0 << 1) + 1; y <= limit; y += 2) {
			// difference from left: lower value wins
			long x = ax < bx ? ax : bx;

			if (cx < x) x = cx;
			if (dx < x) x = dx;

			data[IDX_EVENTS + y] = (int) (x >= 0 ? (x >> 20) : -1);

			ax += aStep;
			bx += bStep;
			cx += cStep;
			dx += dStep;
		}
	}

	// For abandoned traversal scheme
	//	void populateTileEvents() {
	//		final int[] events = this.events;
	//		final int[] tileEvents = this.tileEvents;
	//
	//		int y = (minPixelY & TILE_AXIS_MASK) << 1;
	//		final int ty0 = (y >> TILE_AXIS_SHIFT); //  NB: no left shift here because y already includes
	//		final int ty1 = (maxTileOriginY >> TILE_AXIS_SHIFT) << 1;
	//
	//		for (int ty = ty0; ty <= ty1;) {
	//			int l = Integer.MAX_VALUE;
	//			int r = Integer.MIN_VALUE;
	//
	//			int tl = (events[y++] >> TILE_AXIS_SHIFT);
	//			int tr = (events[y++] >> TILE_AXIS_SHIFT);
	//			if (tr >= tl) {
	//				if (tl < l) l = tl;
	//				if (tr > r) r = tr;
	//			}
	//
	//			tl = (events[y++] >> TILE_AXIS_SHIFT);
	//			tr = (events[y++] >> TILE_AXIS_SHIFT);
	//			if (tr >= tl) {
	//				if (tl < l) l = tl;
	//				if (tr > r) r = tr;
	//			}
	//
	//			tl = (events[y++] >> TILE_AXIS_SHIFT);
	//			tr = (events[y++] >> TILE_AXIS_SHIFT);
	//			if (tr >= tl) {
	//				if (tl < l) l = tl;
	//				if (tr > r) r = tr;
	//			}
	//
	//			tl = (events[y++] >> TILE_AXIS_SHIFT);
	//			tr = (events[y++] >> TILE_AXIS_SHIFT);
	//			if (tr >= tl) {
	//				if (tl < l) l = tl;
	//				if (tr > r) r = tr;
	//			}
	//
	//			tl = (events[y++] >> TILE_AXIS_SHIFT);
	//			tr = (events[y++] >> TILE_AXIS_SHIFT);
	//			if (tr >= tl) {
	//				if (tl < l) l = tl;
	//				if (tr > r) r = tr;
	//			}
	//
	//			tl = (events[y++] >> TILE_AXIS_SHIFT);
	//			tr = (events[y++] >> TILE_AXIS_SHIFT);
	//			if (tr >= tl) {
	//				if (tl < l) l = tl;
	//				if (tr > r) r = tr;
	//			}
	//
	//			tl = (events[y++] >> TILE_AXIS_SHIFT);
	//			tr = (events[y++] >> TILE_AXIS_SHIFT);
	//			if (tr >= tl) {
	//				if (tl < l) l = tl;
	//				if (tr > r) r = tr;
	//			}
	//
	//			tl = (events[y++] >> TILE_AXIS_SHIFT);
	//			tr = (events[y++] >> TILE_AXIS_SHIFT);
	//			if (tr >= tl) {
	//				if (tl < l) l = tl;
	//				if (tr > r) r = tr;
	//			}
	//
	//			if (l < 0) l = 0;
	//
	//			if (r > MAX_TILE_X) r = MAX_TILE_X;
	//
	//			//assert l <= r;
	//			//			assert l <= MAX_TILE_X;
	//			//			assert r >= 0;
	//
	//			tileEvents[ty++] = l;
	//			tileEvents[ty++] = r;
	//		}
	//	}

	void setupVertex(final int baseIndex, final int x, final int y, final int z) {
		final int[] data = vertexData;
		final Matrix4L mvpMatrix = this.mvpMatrix;

		final float tx = mvpMatrix.transformVec4X(x, y, z) * Matrix4L.FLOAT_CONVERSION;
		final float ty = mvpMatrix.transformVec4Y(x, y, z) * Matrix4L.FLOAT_CONVERSION;
		final float w = mvpMatrix.transformVec4W(x, y, z) * Matrix4L.FLOAT_CONVERSION;

		data[baseIndex + PV_X] = Float.floatToRawIntBits(tx);
		data[baseIndex + PV_Y] = Float.floatToRawIntBits(ty);
		data[baseIndex + PV_Z] = Float.floatToRawIntBits(mvpMatrix.transformVec4Z(x, y, z) * Matrix4L.FLOAT_CONVERSION);
		data[baseIndex + PV_W] = Float.floatToRawIntBits(w);

		if (w != 0)  {
			final float iw = 1f / w;
			final int px = Math.round(tx * iw * HALF_PRECISE_WIDTH) + HALF_PRECISE_WIDTH;
			final int py = Math.round(ty * iw * HALF_PRECISE_HEIGHT) + HALF_PRECISE_HEIGHT;

			data[baseIndex + PV_PX] = px;
			data[baseIndex + PV_PY] = py;
		}
	}

	int needsNearClip(final int baseIndex) {
		final int[] data = vertexData;
		final float w = Float.intBitsToFloat(data[baseIndex + PV_W]);
		final float z = Float.intBitsToFloat(data[baseIndex + PV_Z]);

		if (w == 0) {
			return 1;
		} else if (w > 0) {
			return (z > 0 && z <= w ) ? 0 : 1;
		} else {
			// w < 0
			return (z < 0 && z >= w) ? 0 : 1;
		}
	}

	void moveTileRight() {
		tileOriginX += 8;

		if ((tileIndex & TILE_INDEX_LOW_X_MASK) == TILE_INDEX_LOW_X_MASK) {
			tileIndex = (tileIndex & ~TILE_INDEX_LOW_X_MASK) + TILE_INDEX_HIGH_X;
		} else {
			tileIndex += 1;
		}

		assert tileIndex == tileIndex(tileOriginX >> TILE_AXIS_SHIFT, tileOriginY >> TILE_AXIS_SHIFT);
		assert tileOriginX < PIXEL_WIDTH;
	}

	void moveTileLeft() {
		tileOriginX -= 8;

		if ((tileIndex & TILE_INDEX_LOW_X_MASK) == 0) {
			tileIndex |= TILE_INDEX_LOW_X_MASK;
			tileIndex -= TILE_INDEX_HIGH_X;
		} else {
			tileIndex -= 1;
		}

		assert tileIndex == tileIndex(tileOriginX >> TILE_AXIS_SHIFT, tileOriginY >> TILE_AXIS_SHIFT);
		assert tileOriginX >= 0;
	}

	void moveTileUp() {
		tileOriginY += 8;

		if ((tileIndex & TILE_INDEX_LOW_Y_MASK) == TILE_INDEX_LOW_Y_MASK) {
			tileIndex = (tileIndex & ~TILE_INDEX_LOW_Y_MASK) + TILE_INDEX_HIGH_Y;
		} else {
			tileIndex += TILE_INDEX_LOW_Y;
		}

		assert tileIndex == tileIndex(tileOriginX >> TILE_AXIS_SHIFT, tileOriginY >> TILE_AXIS_SHIFT);
		assert tileOriginY < PIXEL_HEIGHT;
	}

	void pushTile() {
		save_tileOriginX = tileOriginX;
		save_tileOriginY = tileOriginY;
		save_tileIndex = tileIndex;
	}

	void popTile() {
		tileOriginX = save_tileOriginX;
		tileOriginY = save_tileOriginY;
		tileIndex = save_tileIndex;
	}

	long computeTileCoverage() {
		final int[] data = this.data;

		int y = tileOriginY << 1;
		final int tx = tileOriginX;

		final int baseX = tileOriginX + 7;

		long mask = 0;

		int leftX = data[y + IDX_EVENTS] - tx;
		int rightX = baseX - data[++y + IDX_EVENTS];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask = m;
		}


		leftX = data[++y + IDX_EVENTS] - tx;
		rightX = baseX - data[++y + IDX_EVENTS];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 8;
		}


		leftX = data[++y + IDX_EVENTS] - tx;
		rightX = baseX - data[++y + IDX_EVENTS];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 16;
		}


		leftX = data[++y + IDX_EVENTS] - tx;
		rightX = baseX - data[++y + IDX_EVENTS];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 24;
		}


		leftX = data[++y + IDX_EVENTS] - tx;
		rightX = baseX - data[++y + IDX_EVENTS];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 32;
		}


		leftX = data[++y + IDX_EVENTS] - tx;
		rightX = baseX - data[++y + IDX_EVENTS];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 40;
		}


		leftX = data[++y + IDX_EVENTS] - tx;
		rightX = baseX - data[++y + IDX_EVENTS];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 48;
		}


		leftX = data[++y + IDX_EVENTS] - tx;
		rightX = baseX - data[++y + IDX_EVENTS];

		if (leftX < 8 && rightX < 8) {
			long m = leftX <= 0 ? 0xFF : ((0xFF << leftX) & 0xFF);

			if (rightX > 0) {
				m &= (0xFF >> rightX);
			}

			mask |= m << 56;
		}


		return mask;
	}

	/**
	 * For early exit testing
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	boolean isPointVisible(int x, int y, int z) {
		final Matrix4L mvpMatrix = this.mvpMatrix;

		final long w = mvpMatrix.transformVec4W(x, y, z);
		final long tz = mvpMatrix.transformVec4Z(x, y, z);

		if (w <= 0 || tz < 0 || tz > w) {
			return false;
		}

		final int px = (int) (HALF_PIXEL_WIDTH + (MATRIX_PRECISION_HALF + HALF_PIXEL_WIDTH  * mvpMatrix.transformVec4X(x, y, z)) / w);
		final int py = (int) (HALF_PIXEL_HEIGHT + (MATRIX_PRECISION_HALF + HALF_PIXEL_HEIGHT * mvpMatrix.transformVec4Y(x, y, z)) / w);

		if (px >= 0 && py >= 0 && px < PIXEL_WIDTH && py < PIXEL_HEIGHT && testPixel(px, py)) {
			return true;
		}

		return false;
	}

	boolean testPixel(int x, int y) {
		return (tiles[Indexer.lowIndexFromPixelXY(x, y)] & (1L << (Indexer.pixelIndex(x, y)))) == 0;
	}

	void drawPixel(int x, int y) {
		tiles[Indexer.lowIndexFromPixelXY(x, y)] |= (1L << (Indexer.pixelIndex(x, y)));
	}

	long nextRasterOutputTime;
}
