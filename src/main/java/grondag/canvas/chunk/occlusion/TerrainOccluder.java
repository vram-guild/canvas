package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.AbstractTile.COVERAGE_FULL;
import static grondag.canvas.chunk.occlusion.AbstractTile.COVERAGE_NONE;
import static grondag.canvas.chunk.occlusion.AbstractTile.COVERAGE_PARTIAL;
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
	private final LowTile lowTile = new LowTile(triangle);
	private final SummaryTile midTile = new SummaryTile(triangle, MID_BIN_PIXEL_DIAMETER) {
		@Override
		public int tileIndex() {
			// shift because two words per tile
			return midIndex(tileX,  tileY) << 1;
		}
	};

	private final SummaryTile topTile = new SummaryTile(triangle, TOP_BIN_PIXEL_DIAMETER) {
		@Override
		public int tileIndex() {
			// shift because two words per tile
			return topIndex(tileX,  tileY) << 1;
		}
	};

	@Override
	protected void drawTri(int v0, int v1, int v2) {
		final Triangle tri = triangle;

		final int boundsResult  = triangle.prepareBounds(vertexData, v0, v1, v2);

		if (boundsResult == BoundsResult.OUT_OF_BOUNDS) {
			return;
		}

		if (boundsResult == BoundsResult.NEEDS_CLIP) {
			drawClippedLowX(v0, v1, v2);
			return;
		}

		//  PERF: consider skipping small tris at intermediate ranges (at extreme range only full sections are attempted)
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

		//		final int bx0 = minPixelX >> TOP_AXIS_SHIFT;
		//		final int bx1 = maxPixelX >> TOP_AXIS_SHIFT;
		//
		//		if (minPixelX >> TOP_AXIS_SHIFT == 0) {
		//			drawTriTop(0, 0);
		//		}

		//		final int by0 = minPixelY >> TOP_AXIS_SHIFT;
		//		final int by1 = maxPixelY >> TOP_AXIS_SHIFT;

		//		if (bx0 == bx1 && by0 == by1) {
		//			drawTriTop(bx0, by0);
		//		} else {
		//			for (int by = by0; by <= by1; by++) {
		//				for (int bx = bx0; bx <= bx1; bx++) {
		//					drawTriTop(bx, by);
		//				}
		//			}
		//		}
	}

	//	private final boolean debugTop = false;

	private void drawTriTop(final int topX, final int topY) {
		topTile.moveTo(topX, topY);
		final long newWordPartial = topTile.computeCoverage();
		final int index = topIndex(topX, topY) << 1; // shift because two words per index
		final long oldWordPartial = topBins[index + OFFSET_PARTIAL];

		if (newWordPartial == 0) {
			return;
		}

		final long oldWordFull = topBins[index + OFFSET_FULL];
		final long newWordFull = topTile.fullCoverage;
		long wordFull = oldWordFull;
		long wordPartial = oldWordPartial;

		if (newWordFull != 0) {
			doMidFullTiles(topX, topY, newWordFull & ~oldWordFull);
			wordFull |= newWordFull;
			wordPartial |= newWordFull;

			if (wordFull == -1L) {
				topBins[index + OFFSET_FULL] = -1L;
				topBins[index + OFFSET_PARTIAL] = -1L;
				return;
			}
		}

		long coverage = newWordPartial & ~wordFull;

		if (coverage != 0) {

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
					final long fullMask = ((long) setBits >> 8) << (y << 3);
					wordFull |= fullMask;
					wordPartial |= fullMask | ((setBits & 0xFFL) << (y << 3));
				}

				++baseY;
				coverage >>= 8;
			}
		}

		wordPartial |= wordFull;
		topBins[index + OFFSET_FULL] = wordFull;
		topBins[index + OFFSET_PARTIAL] = wordPartial;
	}

	private void doMidFullTiles(final int topX, final int topY, long coverage) {
		if (coverage == 0) {
			return;
		}

		final int baseX = topX << BIN_AXIS_SHIFT;
		int baseY = topY << BIN_AXIS_SHIFT;

		for (int y = 0; y < 8; y++) {
			final int bits = (int) coverage & 0xFF;

			switch (bits & 0xF) {
			case 0b0000:
				break;

			case 0b0001:
				fillMidBin(baseX + 0, baseY);
				break;

			case 0b0010:
				fillMidBin(baseX + 1, baseY);
				break;

			case 0b0011:
				fillMidBin(baseX + 0, baseY);
				fillMidBin(baseX + 1, baseY);
				break;

			case 0b0100:
				fillMidBin(baseX + 2, baseY);
				break;

			case 0b0101:
				fillMidBin(baseX + 0, baseY);
				fillMidBin(baseX + 2, baseY);
				break;

			case 0b0110:
				fillMidBin(baseX + 1, baseY);
				fillMidBin(baseX + 2, baseY);
				break;

			case 0b0111:
				fillMidBin(baseX + 0, baseY);
				fillMidBin(baseX + 1, baseY);
				fillMidBin(baseX + 2, baseY);
				break;

			case 0b1000:
				fillMidBin(baseX + 3, baseY);
				break;

			case 0b1001:
				fillMidBin(baseX + 0, baseY);
				fillMidBin(baseX + 3, baseY);
				break;

			case 0b1010:
				fillMidBin(baseX + 1, baseY);
				fillMidBin(baseX + 3, baseY);
				break;

			case 0b1011:
				fillMidBin(baseX + 0, baseY);
				fillMidBin(baseX + 1, baseY);
				fillMidBin(baseX + 3, baseY);
				break;

			case 0b1100:
				fillMidBin(baseX + 2, baseY);
				fillMidBin(baseX + 3, baseY);
				break;

			case 0b1101:
				fillMidBin(baseX + 0, baseY);
				fillMidBin(baseX + 2, baseY);
				fillMidBin(baseX + 3, baseY);
				break;

			case 0b1110:
				fillMidBin(baseX + 1, baseY);
				fillMidBin(baseX + 2, baseY);
				fillMidBin(baseX + 3, baseY);
				break;

			case 0b1111:
				fillMidBin(baseX + 0, baseY);
				fillMidBin(baseX + 1, baseY);
				fillMidBin(baseX + 2, baseY);
				fillMidBin(baseX + 3, baseY);
				break;
			}

			switch (bits >> 4) {
			case 0b0000:
				break;

			case 0b0001:
				fillMidBin(baseX + 4, baseY);
				break;

			case 0b0010:
				fillMidBin(baseX + 5, baseY);
				break;

			case 0b0011:
				fillMidBin(baseX + 4, baseY);
				fillMidBin(baseX + 5, baseY);
				break;

			case 0b0100:
				fillMidBin(baseX + 6, baseY);
				break;

			case 0b0101:
				fillMidBin(baseX + 4, baseY);
				fillMidBin(baseX + 6, baseY);
				break;

			case 0b0110:
				fillMidBin(baseX + 5, baseY);
				fillMidBin(baseX + 6, baseY);
				break;

			case 0b0111:
				fillMidBin(baseX + 4, baseY);
				fillMidBin(baseX + 5, baseY);
				fillMidBin(baseX + 6, baseY);
				break;

			case 0b1000:
				fillMidBin(baseX + 7, baseY);
				break;

			case 0b1001:
				fillMidBin(baseX + 4, baseY);
				fillMidBin(baseX + 7, baseY);
				break;

			case 0b1010:
				fillMidBin(baseX + 5, baseY);
				fillMidBin(baseX + 7, baseY);
				break;

			case 0b1011:
				fillMidBin(baseX + 4, baseY);
				fillMidBin(baseX + 5, baseY);
				fillMidBin(baseX + 7, baseY);
				break;

			case 0b1100:
				fillMidBin(baseX + 6, baseY);
				fillMidBin(baseX + 7, baseY);
				break;

			case 0b1101:
				fillMidBin(baseX + 4, baseY);
				fillMidBin(baseX + 6, baseY);
				fillMidBin(baseX + 7, baseY);
				break;

			case 0b1110:
				fillMidBin(baseX + 5, baseY);
				fillMidBin(baseX + 6, baseY);
				fillMidBin(baseX + 7, baseY);
				break;

			case 0b1111:
				fillMidBin(baseX + 4, baseY);
				fillMidBin(baseX + 5, baseY);
				fillMidBin(baseX + 6, baseY);
				fillMidBin(baseX + 7, baseY);
				break;
			}

			++baseY;
			coverage >>= 8;
		}
	}


	// PERF: do this with arrayCopy
	private void fillMidBin(final int midX, final int midY) {
		final int index = midIndex(midX, midY) << 1;
		midBins[index + OFFSET_FULL] = -1L;
		midBins[index + OFFSET_PARTIAL] = -1L;

		final int lowX0 = midX << 3;
		final int lowY0 = midY << 3;
		final int lowX1 = lowX0 + 7;
		final int lowY1 = lowY0 + 7;
		final long[] lowBins = this.lowBins;

		for (int lowY = lowY0; lowY <= lowY1; lowY++) {
			for (int lowX = lowX0; lowX <= lowX1; lowX++) {
				lowBins[lowIndex(lowX, lowY)] = -1L;
			}
		}
	}

	/**
	 * Returns true when bin fully occluded
	 */
	private int drawTriMid(final int midX, final int midY) {
		midTile.moveTo(midX, midY);
		final long newWordPartial = midTile.computeCoverage();
		final int index = midIndex(midX, midY) << 1; // shift because two words per index
		final long oldWordPartial = midBins[index + OFFSET_PARTIAL];

		if (newWordPartial == 0) {
			return oldWordPartial == 0 ? COVERAGE_NONE : COVERAGE_PARTIAL;
		}

		final long oldWordFull = midBins[index + OFFSET_FULL];
		final long newWordFull = midTile.fullCoverage;
		long wordFull = oldWordFull;
		long wordPartial = oldWordPartial;

		if (newWordFull != 0) {
			doLowFullTiles(midX, midY, newWordFull & ~oldWordFull);
			wordFull |= newWordFull;
			wordPartial |= newWordFull;

			if (wordFull == -1L) {
				midBins[index + OFFSET_FULL] = -1L;
				midBins[index + OFFSET_PARTIAL] = -1L;
				return COVERAGE_FULL;
			}
		}

		long coverage = newWordPartial & ~wordFull;
		if (coverage != 0) {

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
					final long fullMask = ((long) setBits >> 8) << (y << 3);
					wordFull |= fullMask;
					wordPartial |= fullMask | ((setBits & 0xFFL) << (y << 3));
				}

				++baseY;
				coverage >>= 8;
			}
		}

		wordPartial |= wordFull;
		midBins[index + OFFSET_FULL] = wordFull;
		midBins[index + OFFSET_PARTIAL] = wordPartial;

		return wordFull == -1L ? COVERAGE_FULL : wordPartial != 0 ? COVERAGE_PARTIAL : COVERAGE_NONE;
	}

	private void doLowFullTiles(final int midX, final int midY, long coverage) {
		if (coverage == 0) {
			return;
		}

		final int baseX = midX << BIN_AXIS_SHIFT;
		int baseY = midY << BIN_AXIS_SHIFT;

		for (int y = 0; y < 8; y++) {
			final int bits = (int) coverage & 0xFF;

			switch (bits & 0xF) {
			case 0b0000:
				break;

			case 0b0001:
				lowBins[lowIndex(baseX + 0, baseY)] = -1L;
				break;

			case 0b0010:
				lowBins[lowIndex(baseX + 1, baseY)] = -1L;
				break;

			case 0b0011:
				lowBins[lowIndex(baseX + 0, baseY)] = -1L;
				lowBins[lowIndex(baseX + 1, baseY)] = -1L;
				break;

			case 0b0100:
				lowBins[lowIndex(baseX + 2, baseY)] = -1L;
				break;

			case 0b0101:
				lowBins[lowIndex(baseX + 0, baseY)] = -1L;
				lowBins[lowIndex(baseX + 2, baseY)] = -1L;
				break;

			case 0b0110:
				lowBins[lowIndex(baseX + 1, baseY)] = -1L;
				lowBins[lowIndex(baseX + 2, baseY)] = -1L;
				break;

			case 0b0111:
				lowBins[lowIndex(baseX + 0, baseY)] = -1L;
				lowBins[lowIndex(baseX + 1, baseY)] = -1L;
				lowBins[lowIndex(baseX + 2, baseY)] = -1L;
				break;

			case 0b1000:
				lowBins[lowIndex(baseX + 3, baseY)] = -1L;
				break;

			case 0b1001:
				lowBins[lowIndex(baseX + 0, baseY)] = -1L;
				lowBins[lowIndex(baseX + 3, baseY)] = -1L;
				break;

			case 0b1010:
				lowBins[lowIndex(baseX + 1, baseY)] = -1L;
				lowBins[lowIndex(baseX + 3, baseY)] = -1L;
				break;

			case 0b1011:
				lowBins[lowIndex(baseX + 0, baseY)] = -1L;
				lowBins[lowIndex(baseX + 1, baseY)] = -1L;
				lowBins[lowIndex(baseX + 3, baseY)] = -1L;
				break;

			case 0b1100:
				lowBins[lowIndex(baseX + 2, baseY)] = -1L;
				lowBins[lowIndex(baseX + 3, baseY)] = -1L;
				break;

			case 0b1101:
				lowBins[lowIndex(baseX + 0, baseY)] = -1L;
				lowBins[lowIndex(baseX + 2, baseY)] = -1L;
				lowBins[lowIndex(baseX + 3, baseY)] = -1L;
				break;

			case 0b1110:
				lowBins[lowIndex(baseX + 1, baseY)] = -1L;
				lowBins[lowIndex(baseX + 2, baseY)] = -1L;
				lowBins[lowIndex(baseX + 3, baseY)] = -1L;
				break;

			case 0b1111:
				lowBins[lowIndex(baseX + 0, baseY)] = -1L;
				lowBins[lowIndex(baseX + 1, baseY)] = -1L;
				lowBins[lowIndex(baseX + 2, baseY)] = -1L;
				lowBins[lowIndex(baseX + 3, baseY)] = -1L;
				break;
			}

			switch (bits >> 4) {
			case 0b0000:
				break;

			case 0b0001:
				lowBins[lowIndex(baseX + 4, baseY)] = -1L;
				break;

			case 0b0010:
				lowBins[lowIndex(baseX + 5, baseY)] = -1L;
				break;

			case 0b0011:
				lowBins[lowIndex(baseX + 4, baseY)] = -1L;
				lowBins[lowIndex(baseX + 5, baseY)] = -1L;
				break;

			case 0b0100:
				lowBins[lowIndex(baseX + 6, baseY)] = -1L;
				break;

			case 0b0101:
				lowBins[lowIndex(baseX + 4, baseY)] = -1L;
				lowBins[lowIndex(baseX + 6, baseY)] = -1L;
				break;

			case 0b0110:
				lowBins[lowIndex(baseX + 5, baseY)] = -1L;
				lowBins[lowIndex(baseX + 6, baseY)] = -1L;
				break;

			case 0b0111:
				lowBins[lowIndex(baseX + 4, baseY)] = -1L;
				lowBins[lowIndex(baseX + 5, baseY)] = -1L;
				lowBins[lowIndex(baseX + 6, baseY)] = -1L;
				break;

			case 0b1000:
				lowBins[lowIndex(baseX + 7, baseY)] = -1L;
				break;

			case 0b1001:
				lowBins[lowIndex(baseX + 4, baseY)] = -1L;
				lowBins[lowIndex(baseX + 7, baseY)] = -1L;
				break;

			case 0b1010:
				lowBins[lowIndex(baseX + 5, baseY)] = -1L;
				lowBins[lowIndex(baseX + 7, baseY)] = -1L;
				break;

			case 0b1011:
				lowBins[lowIndex(baseX + 4, baseY)] = -1L;
				lowBins[lowIndex(baseX + 5, baseY)] = -1L;
				lowBins[lowIndex(baseX + 7, baseY)] = -1L;
				break;

			case 0b1100:
				lowBins[lowIndex(baseX + 6, baseY)] = -1L;
				lowBins[lowIndex(baseX + 7, baseY)] = -1L;
				break;

			case 0b1101:
				lowBins[lowIndex(baseX + 4, baseY)] = -1L;
				lowBins[lowIndex(baseX + 6, baseY)] = -1L;
				lowBins[lowIndex(baseX + 7, baseY)] = -1L;
				break;

			case 0b1110:
				lowBins[lowIndex(baseX + 5, baseY)] = -1L;
				lowBins[lowIndex(baseX + 6, baseY)] = -1L;
				lowBins[lowIndex(baseX + 7, baseY)] = -1L;
				break;

			case 0b1111:
				lowBins[lowIndex(baseX + 4, baseY)] = -1L;
				lowBins[lowIndex(baseX + 5, baseY)] = -1L;
				lowBins[lowIndex(baseX + 6, baseY)] = -1L;
				lowBins[lowIndex(baseX + 7, baseY)] = -1L;
				break;
			}

			++baseY;
			coverage >>= 8;
		}
	}

	private int drawTriLow(int lowX, int lowY) {
		lowTile.moveTo(lowX, lowY);
		final long coverage = lowTile.computeCoverage();

		if (coverage == 0)  {
			return COVERAGE_NONE;
		}

		final int index = lowIndex(lowX, lowY);
		final long word =  lowBins[index] | coverage;
		lowBins[index] = word;
		return word == -1L ? COVERAGE_FULL : COVERAGE_PARTIAL;
	}


	@Override
	protected boolean testTri(int v0, int v1, int v2) {
		final Triangle tri = triangle;
		final int boundsResult  = tri.prepareBounds(vertexData, v0, v1, v2);

		if (boundsResult == BoundsResult.OUT_OF_BOUNDS) {
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
			final boolean result =  testTriLow();
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

		//		case SCALE_TOP: {
		//			//CanvasWorldRenderer.innerTimer.start();
		//			tri.prepareScan(vertexData, v0, v1, v2);
		//			final boolean result = testTriTop();
		//			//CanvasWorldRenderer.innerTimer.stop();
		//
		//			return result;
		//		}

		default:
			assert false : "Bad triangle scale";
		return false;
		}

		//		final int bx0 = (minPixelX >> TOP_AXIS_SHIFT);
		//		final int bx1 = (maxPixelX >> TOP_AXIS_SHIFT);
		//		final int by0 = (minPixelY >> TOP_AXIS_SHIFT);
		//		final int by1 = maxPixelY >> TOP_AXIS_SHIFT;
		//
		//					if (bx0 == bx1 && by0 == by1) {
		//						return testTriTop(bx0, by0);
		//					} else {
		//						for (int by = by0; by <= by1; by++) {
		//							for (int bx = bx0; bx <= bx1; bx++) {
		//								if (testTriTop(bx, by)) {
		//									return true;
		//								}
		//							}
		//						}
		//
		//						return false;
		//					}
	}

	private boolean testTriMid() {
		final Triangle tri = triangle;
		final SummaryTile midTile = this.midTile;
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
		final long full = midBins[midTile.tileIndex() + OFFSET_FULL];

		if (full == -1L) {
			return false;
		}

		final SummaryTile midTile = this.midTile;
		long coverage = midTile.computeCoverage();

		// nothing in this tile
		if (coverage  == 0)  {
			return false;
		}

		final long notFull = ~full;
		final long fullCoverage = midTile.fullCoverage;

		// if any fully covered tile is not full then must be visible
		if  ((fullCoverage & notFull) != 0) {
			return true;
		}

		// don't check tiles fully covered by triangle - they would have been caught in previous step
		coverage &= ~fullCoverage;

		// don't check tiles known to be fully occluded
		coverage &= notFull;

		// PERF: is this always true? Significantly increases visible count
		// if any partially covered tiles are fully empty then must be visible
		//		if ((coverage & ~midBins[index + OFFSET_PARTIAL]) != 0) {
		//			return true;
		//		}

		if (coverage == 0) {
			// nothing left to check
			return false;
		}

		final LowTile lowTile = this.lowTile;
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
		final LowTile lowTile = this.lowTile;
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

	protected static final int OFFSET_FULL = 0;
	protected static final int OFFSET_PARTIAL = 1;


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
