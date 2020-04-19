package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Clipper.drawClippedLowX;
import static grondag.canvas.chunk.occlusion.Clipper.testClippedLowX;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_NEEDS_CLIP;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_OUTSIDE_OR_TOO_SMALL;
import static grondag.canvas.chunk.occlusion.Constants.COVERAGE_FULL;
import static grondag.canvas.chunk.occlusion.Constants.COVERAGE_NONE_OR_SOME;
import static grondag.canvas.chunk.occlusion.Constants.LOW_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Constants.MID_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.SCALE_LOW;
import static grondag.canvas.chunk.occlusion.Constants.SCALE_MID;
import static grondag.canvas.chunk.occlusion.Constants.SCALE_POINT;
import static grondag.canvas.chunk.occlusion.Data.lowTileX;
import static grondag.canvas.chunk.occlusion.Data.lowTileY;
import static grondag.canvas.chunk.occlusion.Data.lowTiles;
import static grondag.canvas.chunk.occlusion.Data.maxPixelX;
import static grondag.canvas.chunk.occlusion.Data.maxPixelY;
import static grondag.canvas.chunk.occlusion.Data.midTileX;
import static grondag.canvas.chunk.occlusion.Data.midTileY;
import static grondag.canvas.chunk.occlusion.Data.midTiles;
import static grondag.canvas.chunk.occlusion.Data.minPixelX;
import static grondag.canvas.chunk.occlusion.Data.minPixelY;
import static grondag.canvas.chunk.occlusion.Data.scale;
import static grondag.canvas.chunk.occlusion.Indexer.lowIndex;
import static grondag.canvas.chunk.occlusion.Indexer.midIndex;
import static grondag.canvas.chunk.occlusion.Indexer.testPixel;
import static grondag.canvas.chunk.occlusion.Tile.computeLowTileCoverage;
import static grondag.canvas.chunk.occlusion.Tile.computeMidTileCoverage;
import static grondag.canvas.chunk.occlusion.Tile.moveLowTileLeft;
import static grondag.canvas.chunk.occlusion.Tile.moveLowTileRight;
import static grondag.canvas.chunk.occlusion.Tile.moveLowTileToParentOrigin;
import static grondag.canvas.chunk.occlusion.Tile.moveLowTileUp;
import static grondag.canvas.chunk.occlusion.Tile.moveMidTileLeft;
import static grondag.canvas.chunk.occlusion.Tile.moveMidTileRight;
import static grondag.canvas.chunk.occlusion.Tile.moveMidTileUp;
import static grondag.canvas.chunk.occlusion.Tile.popLowTile;
import static grondag.canvas.chunk.occlusion.Tile.pushLowTile;
import static grondag.canvas.chunk.occlusion.Triangle.prepareBounds;
import static grondag.canvas.chunk.occlusion.Triangle.prepareScan;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;


// Some elements are adapted from content found at
// https://fgiesen.wordpress.com/2013/02/17/optimizing-sw-occlusion-culling-index/
// by Fabian “ryg” Giesen. That content is in the public domain.

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

abstract class Rasterizer  {
	private Rasterizer() { }

	@SuppressWarnings("fallthrough")
	static void drawTri(int v0, int v1, int v2) {
		final int boundsResult  = prepareBounds(v0, v1, v2);

		if (boundsResult == BOUNDS_OUTSIDE_OR_TOO_SMALL) {
			return;
		}

		if (boundsResult == BOUNDS_NEEDS_CLIP) {
			drawClippedLowX(v0, v1, v2);
			return;
		}

		switch(scale) {
		case SCALE_MID:
			scale = SCALE_LOW;

		case SCALE_LOW:{
			//CanvasWorldRenderer.innerTimer.start();
			prepareScan();
			Rasterizer.drawTriLow();
			//CanvasWorldRenderer.innerTimer.stop();
			return;
		}

		//		case SCALE_MID: {
		//			//CanvasWorldRenderer.innerTimer.start();
		//			prepareScan();
		//			Rasterizer.drawTriMid();
		//			//CanvasWorldRenderer.innerTimer.stop();
		//
		//			return;
		//		}

		// skip drawing single points - can't get accurate coverage
		case SCALE_POINT:
		default:
			return;
		}
	}

	static boolean testTri(int v0, int v1, int v2) {
		final int boundsResult  = prepareBounds(v0, v1, v2);

		if (boundsResult == BOUNDS_OUTSIDE_OR_TOO_SMALL) {
			return false;
		}

		if (boundsResult == BOUNDS_NEEDS_CLIP) {
			return testClippedLowX(v0, v1, v2);
		}

		switch(scale) {
		case SCALE_POINT: {
			//			CanvasWorldRenderer.innerTimer.start();
			final int px = minPixelX;
			final int py = minPixelY;
			final boolean result = px >= 0 && py >= 0 && px < PIXEL_WIDTH && py < PIXEL_HEIGHT && testPixel(px, py);
			//			CanvasWorldRenderer.innerTimer.stop();

			return result;
		}

		case SCALE_MID:
			scale = SCALE_LOW;
			//		{
			//			//CanvasWorldRenderer.innerTimer.start();
			//			prepareScan();
			//			final boolean result = testTriMid();
			//			//CanvasWorldRenderer.innerTimer.stop();
			//
			//			return result;
			//		}

		case SCALE_LOW:{
			//CanvasWorldRenderer.innerTimer.start();
			prepareScan();
			final boolean result = testTriLow();
			//CanvasWorldRenderer.innerTimer.stop();
			return result;
		}



		default:
			assert false : "Bad triangle scale";
		return false;
		}
	}

	static boolean testTriMid() {
		final int x0 = (minPixelX >> MID_AXIS_SHIFT);
		final int x1 = (maxPixelX >> MID_AXIS_SHIFT);
		final int y1 = (maxPixelY >> MID_AXIS_SHIFT);

		boolean goRight = true;

		while(true) {
			if (testTriMidInner()) {
				return true;
			}

			if (goRight) {
				if (midTileX == x1) {
					if(midTileY == y1) {
						return false;
					} else {
						moveMidTileUp();
						goRight = !goRight;
					}
				} else {
					moveMidTileRight();
				}
			} else {
				if (midTileX == x0) {
					if(midTileY == y1) {
						return false;
					} else {
						moveMidTileUp();
						goRight = !goRight;
					}
				} else {
					moveMidTileLeft();
				}
			}
		}
	}

	static boolean testTriMidInner() {
		final long full = Data.midTiles[midIndex(midTileX,  midTileY)];

		if (full == -1L) {
			return false;
		}

		// don't check tiles known to be fully occluded
		long coverage = computeMidTileCoverage() & ~full;

		// nothing in this tile
		if (coverage  == 0)  {
			return false;
		}

		moveLowTileToParentOrigin();

		do {
			int row = (int) (coverage & 0xFFL);

			// PERF: more efficient traversal
			if (row != 0) {
				pushLowTile();

				do {
					if  ((row & 1) == 1 && testTriLowInner()) {
						return true;
					}

					row >>= 1;

					if (row == 0) break;

					moveLowTileRight();
				} while (true);

				popLowTile();
			}

			coverage >>>= 8;
			if (coverage == 0)  break;
			moveLowTileUp();
		} while (true);

		return false;
	}

	static boolean testTriLow() {
		final int x0 = (minPixelX >> LOW_AXIS_SHIFT);
		final int x1 = (maxPixelX >> LOW_AXIS_SHIFT);
		final int y1 = (maxPixelY >> LOW_AXIS_SHIFT);

		boolean goRight = true;

		while(true) {
			if(testTriLowInner()) {
				return true;
			}

			if (goRight) {
				if (lowTileX == x1) {
					if(lowTileY == y1) {
						return false;
					} else {
						moveLowTileUp();
						goRight = !goRight;
					}
				} else {
					moveLowTileRight();
				}
			} else {
				if (lowTileX == x0) {
					if(lowTileY == y1) {
						return false;
					} else {
						moveLowTileUp();
						goRight = !goRight;
					}
				} else {
					moveLowTileLeft();
				}
			}
		}
	}

	static boolean testTriLowOld() {
		final int x0 = (minPixelX >> LOW_AXIS_SHIFT);
		final int x1 = (maxPixelX >> LOW_AXIS_SHIFT);
		final int y0 = (minPixelY >> LOW_AXIS_SHIFT);
		final int y1 = (maxPixelY >> LOW_AXIS_SHIFT);

		if (testTriLowInner()) {
			return true;
		}

		if (x0 != x1) {
			moveLowTileRight();

			if (testTriLowInner()) {
				return true;
			}
		}

		if (y0 != y1) {
			moveLowTileUp();

			if (testTriLowInner()) {
				return true;
			}

			if (x0 != x1) {
				moveLowTileLeft();

				if (testTriLowInner()) {
					return true;
				}
			}
		}

		return false;
	}

	static boolean testTriLowInner() {
		final long word = lowTiles[lowIndex(lowTileX, lowTileY)];

		// nothing to test if fully occluded
		if  (word == -1L) {
			return false;
		}

		return (~word & computeLowTileCoverage()) != 0;
	}

	static void drawTriLow() {
		final int x0 = (minPixelX >> LOW_AXIS_SHIFT);
		final int x1 = (maxPixelX >> LOW_AXIS_SHIFT);
		final int y1 = (maxPixelY >> LOW_AXIS_SHIFT);

		boolean goRight = true;

		while(true) {
			drawTriLowInner();

			if (goRight) {
				if (lowTileX == x1) {
					if(lowTileY == y1) {
						return;
					} else {
						moveLowTileUp();
						goRight = !goRight;
					}
				} else {
					moveLowTileRight();
				}
			} else {
				if (lowTileX == x0) {
					if(lowTileY == y1) {
						return;
					} else {
						moveLowTileUp();
						goRight = !goRight;
					}
				} else {
					moveLowTileLeft();
				}
			}
		}
	}

	static void drawTriLowOld() {

		final int x0 = (minPixelX >> LOW_AXIS_SHIFT);
		final int x1 = (maxPixelX >> LOW_AXIS_SHIFT);
		final int y0 = (minPixelY >> LOW_AXIS_SHIFT);
		final int y1 = (maxPixelY >> LOW_AXIS_SHIFT);

		drawTriLowInner();

		if (x0 != x1) {
			moveLowTileRight();
			drawTriLowInner();
		}

		if (y0 != y1) {
			moveLowTileUp();
			drawTriLowInner();

			if (x0 != x1) {
				moveLowTileLeft();
				drawTriLowInner();
			}
		}
	}

	static int drawTriLowInner() {
		final int index = lowIndex(lowTileX, lowTileY);
		long word = Data.lowTiles[index];

		// nothing to test if fully occluded
		if  (word == -1L) {
			return COVERAGE_FULL;
		}  else {
			word |= computeLowTileCoverage();
			Data.lowTiles[index] = word;
			return word == -1L ? COVERAGE_FULL : COVERAGE_NONE_OR_SOME;
		}
	}

	static void drawTriMid() {
		final int x0 = (minPixelX >> MID_AXIS_SHIFT);
		final int x1 = (maxPixelX >> MID_AXIS_SHIFT);
		final int y1 = (maxPixelY >> MID_AXIS_SHIFT);

		boolean goRight = true;

		while(true) {
			drawTriMidInner();

			if (goRight) {
				if (midTileX == x1) {
					if(midTileY == y1) {
						return;
					} else {
						moveMidTileUp();
						goRight = !goRight;
					}
				} else {
					moveMidTileRight();
				}
			} else {
				if (midTileX == x0) {
					if(midTileY == y1) {
						return;
					} else {
						moveMidTileUp();
						goRight = !goRight;
					}
				} else {
					moveMidTileLeft();
				}
			}
		}
	}

	static void drawTriMidInner() {
		final int index = midIndex(midTileX,  midTileY);
		long word = midTiles[index];

		if (word == -1L) {
			return;
		}

		// TODO: remove
		final long c = computeMidTileCoverage() & ~word;

		// don't draw tiles known to be fully occluded
		long coverage = c; // computeMidTileCoverage() & ~word;

		// nothing to do
		if (coverage == 0)  {
			return;
		}

		moveLowTileToParentOrigin();

		long yMask = 1L;

		do {
			int row = (int) (coverage & 0xFFL);

			// PERF: more efficient traversal
			if (row != 0) {
				pushLowTile();
				long mask = yMask;

				do {
					//  TODO: FIX and remove check - shuld  not be testing tiles out of range
					//					if ((row & 1) == 1 && (lowTileY << 3) > maxPixelY) {
					//						System.out.println();
					//						printMask8x8(c);
					//						computeMidTileCoverage();
					//					}

					if ((row & 1) == 1 && drawTriLowInner() == COVERAGE_FULL) {
						word |= mask;
					}

					mask <<= 1;
					row >>= 1;

					if (row == 0) {
						break;
					}

					moveLowTileRight();
				} while (true);

				popLowTile();
			}

			yMask <<= 8;
			coverage >>>= 8;

			if (coverage == 0) {
				break;
			}

			moveLowTileUp();
		} while (true);

		Data.midTiles[index] = word;
	}

	static void printMask8x8(long mask) {
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
