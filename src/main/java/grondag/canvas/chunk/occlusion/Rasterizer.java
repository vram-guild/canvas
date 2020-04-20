package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Clipper.drawClippedLowX;
import static grondag.canvas.chunk.occlusion.Clipper.testClippedLowX;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_NEEDS_CLIP;
import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_OUTSIDE_OR_TOO_SMALL;
import static grondag.canvas.chunk.occlusion.Constants.COVERAGE_FULL;
import static grondag.canvas.chunk.occlusion.Constants.COVERAGE_NONE_OR_SOME;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.TILE_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Data.maxPixelX;
import static grondag.canvas.chunk.occlusion.Data.maxPixelY;
import static grondag.canvas.chunk.occlusion.Data.minPixelX;
import static grondag.canvas.chunk.occlusion.Data.minPixelY;
import static grondag.canvas.chunk.occlusion.Data.tileIndex;
import static grondag.canvas.chunk.occlusion.Data.tileX;
import static grondag.canvas.chunk.occlusion.Data.tileY;
import static grondag.canvas.chunk.occlusion.Data.tiles;
import static grondag.canvas.chunk.occlusion.Indexer.testPixel;
import static grondag.canvas.chunk.occlusion.Tile.computeTileCoverage;
import static grondag.canvas.chunk.occlusion.Tile.moveTileLeft;
import static grondag.canvas.chunk.occlusion.Tile.moveTileRight;
import static grondag.canvas.chunk.occlusion.Tile.moveTileUp;
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

	static void drawTri(int v0, int v1, int v2) {
		final int boundsResult  = prepareBounds(v0, v1, v2);

		if (boundsResult == BOUNDS_OUTSIDE_OR_TOO_SMALL) {
			return;
		}

		if (boundsResult == BOUNDS_NEEDS_CLIP) {
			drawClippedLowX(v0, v1, v2);
			return;
		}

		// Don't draw single points
		if((minPixelX == maxPixelX && minPixelY == maxPixelY)) {
			return;
		}

		prepareScan();
		Rasterizer.drawTri();
	}

	static boolean testTri(int v0, int v1, int v2) {
		final int boundsResult  = prepareBounds(v0, v1, v2);

		if (boundsResult == BOUNDS_OUTSIDE_OR_TOO_SMALL) {
			return false;
		}

		if (boundsResult == BOUNDS_NEEDS_CLIP) {
			return testClippedLowX(v0, v1, v2);
		}

		if((minPixelX == maxPixelX && minPixelY == maxPixelY)) {
			final int px = minPixelX;
			final int py = minPixelY;
			return px >= 0 && py >= 0 && px < PIXEL_WIDTH && py < PIXEL_HEIGHT && testPixel(px, py);
		} else {
			prepareScan();
			return testTri();
		}
	}

	static boolean testTri() {
		final int x0 = (minPixelX >> TILE_AXIS_SHIFT);
		final int x1 = (maxPixelX >> TILE_AXIS_SHIFT);
		final int y1 = (maxPixelY >> TILE_AXIS_SHIFT);

		boolean goRight = true;

		while(true) {
			if(testTriInner()) {
				return true;
			}

			if (goRight) {
				if (tileX == x1) {
					if(tileY == y1) {
						return false;
					} else {
						moveTileUp();
						goRight = !goRight;
					}
				} else {
					moveTileRight();
				}
			} else {
				if (tileX == x0) {
					if(tileY == y1) {
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

	static boolean testTriInner() {
		final long word = tiles[tileIndex];

		// nothing to test if fully occluded
		if  (word == -1L) {
			return false;
		}

		return (~word & computeTileCoverage()) != 0;
	}

	static void drawTri() {
		final int x0 = (minPixelX >> TILE_AXIS_SHIFT);
		final int x1 = (maxPixelX >> TILE_AXIS_SHIFT);
		final int y1 = (maxPixelY >> TILE_AXIS_SHIFT);

		boolean goRight = true;

		while(true) {
			drawTriInner();

			if (goRight) {
				if (tileX == x1) {
					if(tileY == y1) {
						return;
					} else {
						moveTileUp();
						goRight = !goRight;
					}
				} else {
					moveTileRight();
				}
			} else {
				if (tileX == x0) {
					if(tileY == y1) {
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

	static int drawTriInner() {
		long word = tiles[tileIndex];

		// nothing to test if fully occluded
		if  (word == -1L) {
			return COVERAGE_FULL;
		}  else {
			word |= computeTileCoverage();
			tiles[tileIndex] = word;
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
