package grondag.canvas.terrain.occlusion;

import static grondag.canvas.terrain.occlusion.Constants.BOUNDS_OUTSIDE_OR_TOO_SMALL;
import static grondag.canvas.terrain.occlusion.Constants.PIXEL_HEIGHT;
import static grondag.canvas.terrain.occlusion.Constants.PIXEL_WIDTH;
import static grondag.canvas.terrain.occlusion.Data.maxPixelX;
import static grondag.canvas.terrain.occlusion.Data.maxPixelY;
import static grondag.canvas.terrain.occlusion.Data.maxTileOriginX;
import static grondag.canvas.terrain.occlusion.Data.maxTileOriginY;
import static grondag.canvas.terrain.occlusion.Data.minPixelX;
import static grondag.canvas.terrain.occlusion.Data.minPixelY;
import static grondag.canvas.terrain.occlusion.Data.minTileOriginX;
import static grondag.canvas.terrain.occlusion.Data.tileIndex;
import static grondag.canvas.terrain.occlusion.Data.tileOriginX;
import static grondag.canvas.terrain.occlusion.Data.tileOriginY;
import static grondag.canvas.terrain.occlusion.Data.tiles;
import static grondag.canvas.terrain.occlusion.Indexer.testPixel;
import static grondag.canvas.terrain.occlusion.Quad.prepareBounds;
import static grondag.canvas.terrain.occlusion.Tile.computeTileCoverage;
import static grondag.canvas.terrain.occlusion.Tile.moveTileLeft;
import static grondag.canvas.terrain.occlusion.Tile.moveTileRight;
import static grondag.canvas.terrain.occlusion.Tile.moveTileUp;

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


	// attempt to improve traversal performance - not faster
	//	private static void drawQuadNew() {
	//		Quad.populateTileEvents();
	//		final int[] tileEvents = Data.tileEvents;
	//		int y = (tileOriginY >> TILE_AXIS_SHIFT) << 1;
	//		int x = (tileOriginX >> TILE_AXIS_SHIFT);
	//		int saveX;
	//
	//		while(true) {
	//			final int l = tileEvents[y++];
	//			final int r = tileEvents[y++];
	//
	//			if (l <= r) {
	//				if (l < x) {
	//					// right of the left edge
	//					if (x < r) {
	//						// inside span
	//						pushTile();
	//						saveX = x;
	//
	//						while (x > l) {
	//							drawQuadInner();
	//							moveTileLeft();
	//							--x;
	//						}
	//
	//						drawQuadInner();
	//						popTile();
	//						x = saveX;
	//
	//						while (x < r) {
	//							drawQuadInner();
	//							moveTileRight();
	//							++x;
	//						}
	//
	//						drawQuadInner();
	//					} else {
	//						// right of or at left edge
	//						while (x > r) {
	//							moveTileLeft();
	//							--x;
	//						}
	//
	//						while (x > l) {
	//							drawQuadInner();
	//							moveTileLeft();
	//							--x;
	//						}
	//
	//						drawQuadInner();
	//					}
	//				} else {
	//					// left of or at left edge
	//					while (x < l) {
	//						moveTileRight();
	//						++x;
	//					}
	//
	//					while (x < r) {
	//						drawQuadInner();
	//						moveTileRight();
	//						++x;
	//					}
	//
	//					drawQuadInner();
	//				}
	//			}
	//
	//			if (tileOriginY < maxTileOriginY) {
	//				moveTileUp();
	//			} else {
	//				return;
	//			}
	//		}
	//	}

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

	//	static long drawQuadInnerNew() {
	//		assert tileOriginY < PIXEL_HEIGHT;
	//		assert tileOriginX < PIXEL_WIDTH;
	//		assert tileOriginX >= 0;
	//
	//		final long coverage = computeTileCoverage();
	//
	//		// nothing to do if fully occluded
	//		tiles[tileIndex] |= coverage;
	//
	//		if ((coverage & TILE_MASK_UP) != 0)  {
	//			pushUp();
	//		}
	//
	//		return coverage;
	//	}

	static void drawQuadInner() {
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
