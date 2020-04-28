package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.BOUNDS_OUTSIDE_OR_TOO_SMALL;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_WIDTH;
import static grondag.canvas.chunk.occlusion.Data.maxPixelX;
import static grondag.canvas.chunk.occlusion.Data.maxPixelY;
import static grondag.canvas.chunk.occlusion.Data.maxTileOriginX;
import static grondag.canvas.chunk.occlusion.Data.maxTileOriginY;
import static grondag.canvas.chunk.occlusion.Data.minPixelX;
import static grondag.canvas.chunk.occlusion.Data.minPixelY;
import static grondag.canvas.chunk.occlusion.Data.minTileOriginX;
import static grondag.canvas.chunk.occlusion.Data.tileIndex;
import static grondag.canvas.chunk.occlusion.Data.tileOriginX;
import static grondag.canvas.chunk.occlusion.Data.tileOriginY;
import static grondag.canvas.chunk.occlusion.Data.tiles;
import static grondag.canvas.chunk.occlusion.Indexer.testPixel;
import static grondag.canvas.chunk.occlusion.Quad.prepareBounds;
import static grondag.canvas.chunk.occlusion.Tile.computeTileCoverage;
import static grondag.canvas.chunk.occlusion.Tile.moveTileLeft;
import static grondag.canvas.chunk.occlusion.Tile.moveTileRight;
import static grondag.canvas.chunk.occlusion.Tile.moveTileUp;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;


// Some elements are adapted from content found at
// https://fgiesen.wordpress.com/2013/02/17/optimizing-sw-occlusion-culling-index/
// by Fabian “ryg” Giesen. That content is in the public domain.
abstract class Rasterizer  {
	private Rasterizer() { }

	static final void drawQuad(int v0, int v1, int v2, int v3) {
		final int boundsResult  = prepareBounds(v0, v1, v2, v3);

		if (boundsResult == BOUNDS_OUTSIDE_OR_TOO_SMALL) {
			return;
		}

		// Don't draw single points
		if((minPixelX == maxPixelX && minPixelY == maxPixelY)) {
			return;
		}

		Rasterizer.drawQuad();
	}

	static boolean testQuad(int v0, int v1, int v2, int v3) {
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

	static boolean testQuad() {
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

	static boolean testQuadInner() {
		final long word = tiles[tileIndex];

		// nothing to test if fully occluded
		if  (word == -1L) {
			return false;
		}

		return (~word & computeTileCoverage()) != 0;
	}

	static void drawQuad() {
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

	static void drawQuadInner() {
		long word = tiles[tileIndex];

		// nothing to do if fully occluded
		if  (word != -1L) {
			word |= computeTileCoverage();
			tiles[tileIndex] = word;
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
