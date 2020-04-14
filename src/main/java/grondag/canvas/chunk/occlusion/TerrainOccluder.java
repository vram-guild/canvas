package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.AbstractTile.COVERAGE_FULL;
import static grondag.canvas.chunk.occlusion.AbstractTile.COVERAGE_NONE_OR_SOME;
import static grondag.canvas.chunk.occlusion.Triangle.SCALE_LOW;
import static grondag.canvas.chunk.occlusion.Triangle.SCALE_MID;
import static grondag.canvas.chunk.occlusion.Triangle.SCALE_POINT;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;

import grondag.canvas.render.CanvasWorldRenderer;

// Some elements are adapted from content found at
// https://fgiesen.wordpress.com/2013/02/17/optimizing-sw-occlusion-culling-index/
// by Fabian “ryg” Giesen. That content is in the public domain.

// PERF: try propagating edge function values up/down heirarchy
// PERF: remove partial coverage mask if can't make it pay
// PERF: temporal optimizations...
//		Limit rebuilds to n/second or when scene is reset or has big rotation
//		Track breaking changes to region occlusion data
//			new renderable that expands chunk bounding box
//			removal of occluder that has effect given region distance
//		Track last distance/region added to occluder
// 		Make occluder additive unless change nearer than last occluder happens (or movement)
//		Without movement, when breaking change happens, invalidate specific tiles, only rebuild those tiles
//		With movement, project from forecasted position and retain visible regions for n frames
//			Above is to allow limit to frequency of rebuilds without too many visibility errors

public class TerrainOccluder extends ClippingTerrainOccluder  {
	private final AbstractTile lowTile = new AbstractTile(triangle, LOW_BIN_PIXEL_DIAMETER) {
		@Override
		public int tileIndex() {
			return lowIndex(tileX,  tileY);
		}
	};

	private final AbstractTile midTile = new AbstractTile(triangle, MID_BIN_PIXEL_DIAMETER) {
		@Override
		public int tileIndex() {
			return midIndex(tileX,  tileY);
		}
	};

	private final AbstractTile topTile = new AbstractTile(triangle, TOP_BIN_PIXEL_DIAMETER) {
		@Override
		public int tileIndex() {
			return topIndex(tileX,  tileY);
		}
	};


	public void drawTriOld(int v0, int v1, int v2) {
		final Triangle tri = triangle;

		final int boundsResult  = triangle.prepareBounds(vertexData, v0, v1, v2);

		if (boundsResult == BoundsResult.OUT_OF_BOUNDS_OR_TOO_SMALL) {
			return;
		}

		if (boundsResult == BoundsResult.NEEDS_CLIP) {
			drawClippedLowX(v0, v1, v2);
			return;
		}

		tri.prepareScan(vertexData, v0, v1, v2);
		topTile.prepare();
		midTile.prepare();
		lowTile.prepare();

		if (tri.minPixelX >> TOP_AXIS_SHIFT == 0) {
			drawTriTop(0, 0);
		}

		if (tri.maxPixelX >> TOP_AXIS_SHIFT == 1) {
			drawTriTop(1, 0);
		}
	}

	@Override
	public void drawTri(int v0, int v1, int v2) {
		final Triangle tri = triangle;
		final int boundsResult  = tri.prepareBounds(vertexData, v0, v1, v2);

		if (boundsResult == BoundsResult.OUT_OF_BOUNDS_OR_TOO_SMALL) {
			return;
		}

		if (boundsResult == BoundsResult.NEEDS_CLIP) {
			drawClippedLowX(v0, v1, v2);
			return;
		}

		//		switch(tri.scale) {
		//
		//		case SCALE_LOW:{
		//			//CanvasWorldRenderer.innerTimer.start();
		//			tri.prepareScan(vertexData, v0, v1, v2);
		//			drawTriLow();
		//			//CanvasWorldRenderer.innerTimer.stop();
		//			return;
		//		}
		//
		//		case SCALE_MID: {
		//CanvasWorldRenderer.innerTimer.start();
		tri.prepareScan(vertexData, v0, v1, v2);
		drawTriMid();
		//CanvasWorldRenderer.innerTimer.stop();
		//
		//			return;
		//		}
		//
		//		// skip drawing single points - can't get accurate coverage
		//		case SCALE_POINT:
		//		default:
		//			assert false : "Bad triangle scale";
		//		return;
		//		}
	}

	private void drawTriTop(final int topX, final int topY) {
		final int index = topIndex(topX, topY);
		long word = topBins[index];

		topTile.moveTo(topX, topY);

		long coverage = topTile.computeCoverage() & ~word;

		if (coverage == 0L) {
			return;
		}

		final int baseX = topX << BIN_AXIS_SHIFT;
		int baseY = topY << BIN_AXIS_SHIFT;

		for (int y = 0; y < 8; y++) {
			final int bits = (int) coverage & 0xFF;
			int setBits = 0;

			switch (bits & 0xF) {
			case 0b0000:
				break;

			case 0b0001:
				setBits |= drawTriMid(baseX + 0, baseY);
				break;

			case 0b0010:
				setBits |= drawTriMid(baseX + 1, baseY) << 1;
				break;

			case 0b0011:
				setBits |= drawTriMid(baseX + 0, baseY);
				setBits |= drawTriMid(baseX + 1, baseY) << 1;
				break;

			case 0b0100:
				setBits |= drawTriMid(baseX + 2, baseY) << 2;
				break;

			case 0b0101:
				setBits |= drawTriMid(baseX + 0, baseY);
				setBits |= drawTriMid(baseX + 2, baseY) << 2;
				break;

			case 0b0110:
				setBits |= drawTriMid(baseX + 1, baseY) << 1;
				setBits |= drawTriMid(baseX + 2, baseY) << 2;
				break;

			case 0b0111:
				setBits |= drawTriMid(baseX + 0, baseY);
				setBits |= drawTriMid(baseX + 1, baseY) << 1;
				setBits |= drawTriMid(baseX + 2, baseY) << 2;
				break;

			case 0b1000:
				setBits |= drawTriMid(baseX + 3, baseY) << 3;
				break;

			case 0b1001:
				setBits |= drawTriMid(baseX + 0, baseY);
				setBits |= drawTriMid(baseX + 3, baseY) << 3;
				break;

			case 0b1010:
				setBits |= drawTriMid(baseX + 1, baseY) << 1;
				setBits |= drawTriMid(baseX + 3, baseY) << 3;
				break;

			case 0b1011:
				setBits |= drawTriMid(baseX + 0, baseY);
				setBits |= drawTriMid(baseX + 1, baseY) << 1;
				setBits |= drawTriMid(baseX + 3, baseY) << 3;
				break;

			case 0b1100:
				setBits |= drawTriMid(baseX + 2, baseY) << 2;
				setBits |= drawTriMid(baseX + 3, baseY) << 3;
				break;

			case 0b1101:
				setBits |= drawTriMid(baseX + 0, baseY);
				setBits |= drawTriMid(baseX + 2, baseY) << 2;
				setBits |= drawTriMid(baseX + 3, baseY) << 3;
				break;

			case 0b1110:
				setBits |= drawTriMid(baseX + 1, baseY) << 1;
				setBits |= drawTriMid(baseX + 2, baseY) << 2;
				setBits |= drawTriMid(baseX + 3, baseY) << 3;
				break;

			case 0b1111:
				setBits |= drawTriMid(baseX + 0, baseY);
				setBits |= drawTriMid(baseX + 1, baseY) << 1;
				setBits |= drawTriMid(baseX + 2, baseY) << 2;
				setBits |= drawTriMid(baseX + 3, baseY) << 3;
				break;
			}

			switch (bits >> 4) {
			case 0b0000:
				break;

			case 0b0001:
				setBits |= drawTriMid(baseX + 4, baseY) << 4;
				break;

			case 0b0010:
				setBits |= drawTriMid(baseX + 5, baseY) << 5;
				break;

			case 0b0011:
				setBits |= drawTriMid(baseX + 4, baseY) << 4;
				setBits |= drawTriMid(baseX + 5, baseY) << 5;
				break;

			case 0b0100:
				setBits |= drawTriMid(baseX + 6, baseY) << 6;
				break;

			case 0b0101:
				setBits |= drawTriMid(baseX + 4, baseY) << 4;
				setBits |= drawTriMid(baseX + 6, baseY) << 6;
				break;

			case 0b0110:
				setBits |= drawTriMid(baseX + 5, baseY) << 5;
				setBits |= drawTriMid(baseX + 6, baseY) << 6;
				break;

			case 0b0111:
				setBits |= drawTriMid(baseX + 4, baseY) << 4;
				setBits |= drawTriMid(baseX + 5, baseY) << 5;
				setBits |= drawTriMid(baseX + 6, baseY) << 6;
				break;

			case 0b1000:
				setBits |= drawTriMid(baseX + 7, baseY) << 7;
				break;

			case 0b1001:
				setBits |= drawTriMid(baseX + 4, baseY) << 4;
				setBits |= drawTriMid(baseX + 7, baseY) << 7;
				break;

			case 0b1010:
				setBits |= drawTriMid(baseX + 5, baseY) << 5;
				setBits |= drawTriMid(baseX + 7, baseY) << 7;
				break;

			case 0b1011:
				setBits |= drawTriMid(baseX + 4, baseY) << 4;
				setBits |= drawTriMid(baseX + 5, baseY) << 5;
				setBits |= drawTriMid(baseX + 7, baseY) << 7;
				break;

			case 0b1100:
				setBits |= drawTriMid(baseX + 6, baseY) << 6;
				setBits |= drawTriMid(baseX + 7, baseY) << 7;
				break;

			case 0b1101:
				setBits |= drawTriMid(baseX + 4, baseY) << 4;
				setBits |= drawTriMid(baseX + 6, baseY) << 6;
				setBits |= drawTriMid(baseX + 7, baseY) << 7;
				break;

			case 0b1110:
				setBits |= drawTriMid(baseX + 5, baseY) << 5;
				setBits |= drawTriMid(baseX + 6, baseY) << 6;
				setBits |= drawTriMid(baseX + 7, baseY) << 7;
				break;

			case 0b1111:
				setBits |= drawTriMid(baseX + 4, baseY) << 4;
				setBits |= drawTriMid(baseX + 5, baseY) << 5;
				setBits |= drawTriMid(baseX + 6, baseY) << 6;
				setBits |= drawTriMid(baseX + 7, baseY) << 7;
				break;
			}

			if (setBits != 0) {
				word |= ((setBits & 0xFFL) << (y << 3));
			}

			++baseY;
			coverage >>= 8;
		}

		topBins[index] = word;
	}

	/**
	 * Returns true when bin fully occluded
	 */
	private int drawTriMid(final int midX, final int midY) {
		final int index = midIndex(midX, midY);
		long word = midBins[index];

		midTile.moveTo(midX, midY);
		long coverage = midTile.computeCoverage() & ~word;

		if (coverage == 0) {
			return COVERAGE_NONE_OR_SOME;
		}

		final int baseX = midX << BIN_AXIS_SHIFT;
		int baseY = midY << BIN_AXIS_SHIFT;

		for (int y = 0; y < 8; y++) {
			final int bits = (int) coverage & 0xFF;
			int setBits = 0;

			switch (bits & 0xF) {
			case 0b0000:
				break;

			case 0b0001:
				setBits |= drawTriLow(baseX + 0, baseY);
				break;

			case 0b0010:
				setBits |= drawTriLow(baseX + 1, baseY) << 1;
				break;

			case 0b0011:
				setBits |= drawTriLow(baseX + 0, baseY);
				setBits |= drawTriLow(baseX + 1, baseY) << 1;
				break;

			case 0b0100:
				setBits |= drawTriLow(baseX + 2, baseY) << 2;
				break;

			case 0b0101:
				setBits |= drawTriLow(baseX + 0, baseY);
				setBits |= drawTriLow(baseX + 2, baseY) << 2;
				break;

			case 0b0110:
				setBits |= drawTriLow(baseX + 1, baseY) << 1;
				setBits |= drawTriLow(baseX + 2, baseY) << 2;
				break;

			case 0b0111:
				setBits |= drawTriLow(baseX + 0, baseY);
				setBits |= drawTriLow(baseX + 1, baseY) << 1;
				setBits |= drawTriLow(baseX + 2, baseY) << 2;
				break;

			case 0b1000:
				setBits |= drawTriLow(baseX + 3, baseY) << 3;
				break;

			case 0b1001:
				setBits |= drawTriLow(baseX + 0, baseY);
				setBits |= drawTriLow(baseX + 3, baseY) << 3;
				break;

			case 0b1010:
				setBits |= drawTriLow(baseX + 1, baseY) << 1;
				setBits |= drawTriLow(baseX + 3, baseY) << 3;
				break;

			case 0b1011:
				setBits |= drawTriLow(baseX + 0, baseY);
				setBits |= drawTriLow(baseX + 1, baseY) << 1;
				setBits |= drawTriLow(baseX + 3, baseY) << 3;
				break;

			case 0b1100:
				setBits |= drawTriLow(baseX + 2, baseY) << 2;
				setBits |= drawTriLow(baseX + 3, baseY) << 3;
				break;

			case 0b1101:
				setBits |= drawTriLow(baseX + 0, baseY);
				setBits |= drawTriLow(baseX + 2, baseY) << 2;
				setBits |= drawTriLow(baseX + 3, baseY) << 3;
				break;

			case 0b1110:
				setBits |= drawTriLow(baseX + 1, baseY) << 1;
				setBits |= drawTriLow(baseX + 2, baseY) << 2;
				setBits |= drawTriLow(baseX + 3, baseY) << 3;
				break;

			case 0b1111:
				setBits |= drawTriLow(baseX + 0, baseY);
				setBits |= drawTriLow(baseX + 1, baseY) << 1;
				setBits |= drawTriLow(baseX + 2, baseY) << 2;
				setBits |= drawTriLow(baseX + 3, baseY) << 3;
				break;
			}

			switch (bits >> 4) {
			case 0b0000:
				break;

			case 0b0001:
				setBits |= drawTriLow(baseX + 4, baseY) << 4;
				break;

			case 0b0010:
				setBits |= drawTriLow(baseX + 5, baseY) << 5;
				break;

			case 0b0011:
				setBits |= drawTriLow(baseX + 4, baseY) << 4;
				setBits |= drawTriLow(baseX + 5, baseY) << 5;
				break;

			case 0b0100:
				setBits |= drawTriLow(baseX + 6, baseY) << 6;
				break;

			case 0b0101:
				setBits |= drawTriLow(baseX + 4, baseY) << 4;
				setBits |= drawTriLow(baseX + 6, baseY) << 6;
				break;

			case 0b0110:
				setBits |= drawTriLow(baseX + 5, baseY) << 5;
				setBits |= drawTriLow(baseX + 6, baseY) << 6;
				break;

			case 0b0111:
				setBits |= drawTriLow(baseX + 4, baseY) << 4;
				setBits |= drawTriLow(baseX + 5, baseY) << 5;
				setBits |= drawTriLow(baseX + 6, baseY) << 6;
				break;

			case 0b1000:
				setBits |= drawTriLow(baseX + 7, baseY) << 7;
				break;

			case 0b1001:
				setBits |= drawTriLow(baseX + 4, baseY) << 4;
				setBits |= drawTriLow(baseX + 7, baseY) << 7;
				break;

			case 0b1010:
				setBits |= drawTriLow(baseX + 5, baseY) << 5;
				setBits |= drawTriLow(baseX + 7, baseY) << 7;
				break;

			case 0b1011:
				setBits |= drawTriLow(baseX + 4, baseY) << 4;
				setBits |= drawTriLow(baseX + 5, baseY) << 5;
				setBits |= drawTriLow(baseX + 7, baseY) << 7;
				break;

			case 0b1100:
				setBits |= drawTriLow(baseX + 6, baseY) << 6;
				setBits |= drawTriLow(baseX + 7, baseY) << 7;
				break;

			case 0b1101:
				setBits |= drawTriLow(baseX + 4, baseY) << 4;
				setBits |= drawTriLow(baseX + 6, baseY) << 6;
				setBits |= drawTriLow(baseX + 7, baseY) << 7;
				break;

			case 0b1110:
				setBits |= drawTriLow(baseX + 5, baseY) << 5;
				setBits |= drawTriLow(baseX + 6, baseY) << 6;
				setBits |= drawTriLow(baseX + 7, baseY) << 7;
				break;

			case 0b1111:
				setBits |= drawTriLow(baseX + 4, baseY) << 4;
				setBits |= drawTriLow(baseX + 5, baseY) << 5;
				setBits |= drawTriLow(baseX + 6, baseY) << 6;
				setBits |= drawTriLow(baseX + 7, baseY) << 7;
				break;
			}

			if (setBits != 0) {
				word |= ((setBits & 0xFFL) << (y << 3));
			}

			++baseY;
			coverage >>= 8;
		}

		midBins[index] = word;

		return word == -1L ? COVERAGE_FULL : COVERAGE_NONE_OR_SOME;
	}

	private int drawTriLow(int lowX, int lowY) {
		lowTile.moveTo(lowX, lowY);
		final long coverage = lowTile.computeCoverage();

		if (coverage == 0)  {
			return COVERAGE_NONE_OR_SOME;
		}

		final int index = lowIndex(lowX, lowY);
		final long word = lowBins[index] | coverage;
		lowBins[index] = word;
		return word == -1L ? COVERAGE_FULL : COVERAGE_NONE_OR_SOME;
	}


	@Override
	protected boolean testTri(int v0, int v1, int v2) {
		final Triangle tri = triangle;
		final int boundsResult  = tri.prepareBounds(vertexData, v0, v1, v2);

		if (boundsResult == BoundsResult.OUT_OF_BOUNDS_OR_TOO_SMALL) {
			return false;
		}

		if (boundsResult == BoundsResult.NEEDS_CLIP) {
			return testClippedLowX(v0, v1, v2);
		}

		switch(tri.scale) {
		case SCALE_POINT: {
			//			CanvasWorldRenderer.innerTimer.start();
			final int px = tri.minPixelX;
			final int py = tri.minPixelY;
			final boolean result = px >= 0 && py >= 0 && px < PIXEL_WIDTH && py < PIXEL_HEIGHT && testPixel(px, py);
			//			CanvasWorldRenderer.innerTimer.stop();

			return result;
		}

		case SCALE_LOW:{
			//CanvasWorldRenderer.innerTimer.start();
			tri.prepareScan(vertexData, v0, v1, v2);
			final boolean result = testTriLow();
			//CanvasWorldRenderer.innerTimer.stop();
			return result;
		}

		case SCALE_MID: {
			CanvasWorldRenderer.innerTimer.start();
			tri.prepareScan(vertexData, v0, v1, v2);
			final boolean result = testTriMid();
			CanvasWorldRenderer.innerTimer.stop();

			return result;
		}

		default:
			assert false : "Bad triangle scale";
		return false;
		}
	}

	private boolean testTriMid() {
		final Triangle tri = triangle;
		final AbstractTile midTile = this.midTile;
		midTile.prepare();
		lowTile.prepare();

		final int x0 = (tri.minPixelX >> MID_AXIS_SHIFT);
		final int x1 = (tri.maxPixelX >> MID_AXIS_SHIFT);
		final int y0 = (tri.minPixelY >> MID_AXIS_SHIFT);
		final int y1 = (tri.maxPixelY >> MID_AXIS_SHIFT);

		midTile.moveTo(x0, y0);
		boolean goRight = true;

		while(true) {
			if (testTriMidInner()) {
				return true;
			}

			if (goRight) {
				if (midTile.tileX == x1) {
					if(midTile.tileY == y1) {
						return false;
					} else {
						midTile.moveUp();
						goRight = !goRight;
					}
				} else {
					midTile.moveRight();
				}
			} else {
				if (midTile.tileX == x0) {
					if(midTile.tileY == y1) {
						return false;
					} else {
						midTile.moveUp();
						goRight = !goRight;
					}
				} else {
					midTile.moveLeft();
				}
			}
		}
	}

	private boolean testTriMidInner() {
		final long full = midBins[midTile.tileIndex()];

		if (full == -1L) {
			return false;
		}

		final AbstractTile midTile = this.midTile;
		// don't check tiles known to be fully occluded
		long coverage = midTile.computeCoverage() & ~full;

		// nothing in this tile
		if (coverage  == 0)  {
			return false;
		}

		final AbstractTile lowTile = this.lowTile;
		lowTile.moveToParentOrigin(midTile);

		do {
			int row = (int) (coverage & 0xFFL);

			// PERF: more efficient traversal
			if (row != 0) {
				lowTile.push();

				do {
					if  ((row & 1) == 1 && testTriLowInner()) {
						return true;
					}

					lowTile.moveRight();
					row >>= 1;
				} while (row != 0);

				lowTile.pop();
			}

			lowTile.moveUp();
			coverage >>>= 8;
		} while (coverage != 0L);

		return false;
	}

	private boolean testTriLow() {
		final Triangle tri = triangle;
		final AbstractTile lowTile = this.lowTile;
		lowTile.prepare();

		final int x0 = (tri.minPixelX >> LOW_AXIS_SHIFT);
		final int x1 = (tri.maxPixelX >> LOW_AXIS_SHIFT);
		final int y0 = (tri.minPixelY >> LOW_AXIS_SHIFT);
		final int y1 = (tri.maxPixelY >> LOW_AXIS_SHIFT);

		lowTile.moveTo(x0, y0);

		if (testTriLowInner()) {
			return true;
		}

		if (x0 != x1) {
			lowTile.moveRight();

			if (testTriLowInner()) {
				return true;
			}
		}

		if (y0 != y1) {
			lowTile.moveUp();

			if (testTriLowInner()) {
				return true;
			}

			if (x0 != x1) {
				lowTile.moveLeft();

				if (testTriLowInner()) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean testTriLowInner() {
		final long word = lowBins[lowTile.tileIndex()];

		// nothing to test if fully occluded
		if  (word == -1L) {
			return false;
		}

		return (~word & lowTile.computeCoverage()) != 0;
	}

	private void drawTriLow() {
		final Triangle tri = triangle;
		final AbstractTile lowTile = this.lowTile;
		lowTile.prepare();

		final int x0 = (tri.minPixelX >> LOW_AXIS_SHIFT);
		final int x1 = (tri.maxPixelX >> LOW_AXIS_SHIFT);
		final int y0 = (tri.minPixelY >> LOW_AXIS_SHIFT);
		final int y1 = (tri.maxPixelY >> LOW_AXIS_SHIFT);

		lowTile.moveTo(x0, y0);

		drawTriLowInner();

		if (x0 != x1) {
			lowTile.moveRight();
			drawTriLowInner();
		}

		if (y0 != y1) {
			lowTile.moveUp();
			drawTriLowInner();

			if (x0 != x1) {
				lowTile.moveLeft();
				drawTriLowInner();
			}
		}
	}

	private int drawTriLowInner() {
		final int index = lowTile.tileIndex();
		long word = lowBins[index];

		// nothing to test if fully occluded
		if  (word == -1L) {
			return COVERAGE_FULL;
		}  else {
			word |= lowTile.computeCoverage();
			lowBins[index] = word;
			return word == -1L ? COVERAGE_FULL : COVERAGE_NONE_OR_SOME;
		}
	}

	private void drawTriMid() {
		final Triangle tri = triangle;
		final AbstractTile midTile = this.midTile;
		midTile.prepare();
		lowTile.prepare();

		final int x0 = (tri.minPixelX >> MID_AXIS_SHIFT);
		final int x1 = (tri.maxPixelX >> MID_AXIS_SHIFT);
		final int y0 = (tri.minPixelY >> MID_AXIS_SHIFT);
		final int y1 = (tri.maxPixelY >> MID_AXIS_SHIFT);

		midTile.moveTo(x0, y0);
		boolean goRight = true;

		while(true) {
			drawTriMidInner();

			if (goRight) {
				if (midTile.tileX == x1) {
					if(midTile.tileY == y1) {
						return;
					} else {
						midTile.moveUp();
						goRight = !goRight;
					}
				} else {
					midTile.moveRight();
				}
			} else {
				if (midTile.tileX == x0) {
					if(midTile.tileY == y1) {
						return;
					} else {
						midTile.moveUp();
						goRight = !goRight;
					}
				} else {
					midTile.moveLeft();
				}
			}
		}
	}

	private void drawTriMidInner() {
		final int index = midTile.tileIndex();
		long word = midBins[index];

		if (word == -1L) {
			return;
		}

		final AbstractTile midTile = this.midTile;
		// don't draw tiles known to be fully occluded
		long coverage = midTile.computeCoverage() & ~word;

		// nothing to do
		if (coverage == 0)  {
			return;
		}

		final AbstractTile lowTile = this.lowTile;
		lowTile.moveToParentOrigin(midTile);

		long yMask = 1L;

		do {
			int row = (int) (coverage & 0xFFL);

			// PERF: more efficient traversal
			if (row != 0) {
				lowTile.push();
				long mask = yMask;

				do {
					if  ((row & 1) == 1 && drawTriLowInner() == COVERAGE_FULL) {
						word |= mask;
					}

					mask <<= 1;
					lowTile.moveRight();
					row >>= 1;
				} while (row != 0);

				lowTile.pop();
			}

			lowTile.moveUp();
			yMask <<= 8;
			coverage >>>= 8;
		} while (coverage != 0L);

		midBins[index] = word;
	}

	public static void printMask8x8(long mask) {
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
}
