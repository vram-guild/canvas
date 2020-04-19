package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Clipper.drawClippedLowX;
import static grondag.canvas.chunk.occlusion.Clipper.testClippedLowX;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_NEEDS_CLIP;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_OUTSIDE_OR_TOO_SMALL;
import static grondag.canvas.chunk.occlusion.Constants.COVERAGE_FULL;
import static grondag.canvas.chunk.occlusion.Constants.COVERAGE_NONE_OR_SOME;
import static grondag.canvas.chunk.occlusion.Constants.LOW_AXIS_SHIFT;
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
import static grondag.canvas.chunk.occlusion.Data.minPixelX;
import static grondag.canvas.chunk.occlusion.Data.minPixelY;
import static grondag.canvas.chunk.occlusion.Data.scale;
import static grondag.canvas.chunk.occlusion.Indexer.lowIndex;
import static grondag.canvas.chunk.occlusion.Indexer.testPixel;
import static grondag.canvas.chunk.occlusion.Tile.computeLowTileCoverage;
import static grondag.canvas.chunk.occlusion.Tile.moveLowTileLeft;
import static grondag.canvas.chunk.occlusion.Tile.moveLowTileRight;
import static grondag.canvas.chunk.occlusion.Tile.moveLowTileUp;
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
