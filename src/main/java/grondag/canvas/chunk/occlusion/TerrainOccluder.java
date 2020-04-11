package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Edge.EDGE_BOTTOM;
import static grondag.canvas.chunk.occlusion.Edge.EDGE_BOTTOM_LEFT;
import static grondag.canvas.chunk.occlusion.Edge.EDGE_BOTTOM_RIGHT;
import static grondag.canvas.chunk.occlusion.Edge.EDGE_LEFT;
import static grondag.canvas.chunk.occlusion.Edge.EDGE_RIGHT;
import static grondag.canvas.chunk.occlusion.Edge.EDGE_TOP;
import static grondag.canvas.chunk.occlusion.Edge.EDGE_TOP_LEFT;
import static grondag.canvas.chunk.occlusion.Edge.EDGE_TOP_RIGHT;
import static grondag.canvas.chunk.occlusion.TileEdge.INSIDE;
import static grondag.canvas.chunk.occlusion.TileEdge.INTERSECTING;
import static grondag.canvas.chunk.occlusion.TileEdge.OUTSIDE;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;

// Some elements are adapted from content found at
// https://fgiesen.wordpress.com/2013/02/17/optimizing-sw-occlusion-culling-index/
// by Fabian “ryg” Giesen. That content is in the public domain.

// PERF: try propagating edge function values up/down heirarchy
// PERF: remove partial coverage mask if can't make it pay
public class TerrainOccluder extends ClippingTerrainOccluder  {
	private final LowTile lowTile = new LowTile(triangle);
	private final MidTile midTile = new MidTile(triangle);
	private final TopTile topTile = new TopTile(triangle);

	@Override
	protected void prepareTriScan(int v0, int v1, int v2) {
		super.prepareTriScan(v0, v1, v2);

		// PERF:  make these lazy
		lowTile.computeSpan();
		midTile.computeSpan();
		topTile.computeSpan();
	}

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
		prepareTriScan(v0, v1, v2);

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

		prepareTriScan(v0, v1, v2);

		return (tri.minPixelX >> TOP_AXIS_SHIFT == 0 && testTriTop(0, 0))
				|| (tri.maxPixelX >> TOP_AXIS_SHIFT == 1 && testTriTop(1, 0));

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

	private boolean testTriTop(final int topX, final int topY) {
		topTile.moveTo(topX, topY);
		final int index = topIndex(topX, topY) << 1; // shift because two words per index
		final long notFull = ~topBins[index + OFFSET_FULL];

		long coverage = topTile.computeCoverage() & notFull;

		// if all covered tiles are full, then nothing visible
		if (coverage  == 0)  {
			return false;
		}

		final long fullCoverage = topTile.fullCoverage;

		// if any fully covered tile is not full then must be visible
		if  ((fullCoverage & notFull) != 0) {
			return true;
		}

		// don't check full tiles - they would have been caught earlier
		coverage &= ~fullCoverage;

		// PERF: is this always true? Significantly increases visible count
		// if any partially covered tiles are fully empty then must be visible
		//		if ((coverage & ~topBins[index + OFFSET_PARTIAL]) != 0) {
		//			return true;
		//		}

		if (coverage == 0) {
			// nothing left to check
			return false;
		}

		final int baseX = topX << BIN_AXIS_SHIFT;
		int baseY = topY << BIN_AXIS_SHIFT;

		for (int y = 0; y < 8; y++) {
			final int bits = (int) coverage & 0xFF;

			switch (bits & 0xF) {
			case 0b0000:
				break;

			case 0b0001:
				if (testTriMid(baseX + 0, baseY)) return true;
				break;

			case 0b0010:
				if (testTriMid(baseX + 1, baseY)) return true;
				break;

			case 0b0011:
				if (testTriMid(baseX + 0, baseY)) return true;
				if (testTriMid(baseX + 1, baseY)) return true;
				break;

			case 0b0100:
				if (testTriMid(baseX + 2, baseY)) return true;
				break;

			case 0b0101:
				if (testTriMid(baseX + 0, baseY)) return true;
				if (testTriMid(baseX + 2, baseY)) return true;
				break;

			case 0b0110:
				if (testTriMid(baseX + 1, baseY)) return true;
				if (testTriMid(baseX + 2, baseY)) return true;
				break;

			case 0b0111:
				if (testTriMid(baseX + 0, baseY)) return true;
				if (testTriMid(baseX + 1, baseY)) return true;
				if (testTriMid(baseX + 2, baseY)) return true;
				break;

			case 0b1000:
				if (testTriMid(baseX + 3, baseY)) return true;
				break;

			case 0b1001:
				if (testTriMid(baseX + 0, baseY)) return true;
				if (testTriMid(baseX + 3, baseY)) return true;
				break;

			case 0b1010:
				if (testTriMid(baseX + 1, baseY)) return true;
				if (testTriMid(baseX + 3, baseY)) return true;
				break;

			case 0b1011:
				if (testTriMid(baseX + 0, baseY)) return true;
				if (testTriMid(baseX + 1, baseY)) return true;
				if (testTriMid(baseX + 3, baseY)) return true;
				break;

			case 0b1100:
				if (testTriMid(baseX + 2, baseY)) return true;
				if (testTriMid(baseX + 3, baseY)) return true;
				break;

			case 0b1101:
				if (testTriMid(baseX + 0, baseY)) return true;
				if (testTriMid(baseX + 2, baseY)) return true;
				if (testTriMid(baseX + 3, baseY)) return true;
				break;

			case 0b1110:
				if (testTriMid(baseX + 1, baseY)) return true;
				if (testTriMid(baseX + 2, baseY)) return true;
				if (testTriMid(baseX + 3, baseY)) return true;
				break;

			case 0b1111:
				if (testTriMid(baseX + 0, baseY)) return true;
				if (testTriMid(baseX + 1, baseY)) return true;
				if (testTriMid(baseX + 2, baseY)) return true;
				if (testTriMid(baseX + 3, baseY)) return true;
				break;
			}

			switch (bits >> 4) {
			case 0b0000:
				break;

			case 0b0001:
				if (testTriMid(baseX + 4, baseY)) return true;
				break;

			case 0b0010:
				if (testTriMid(baseX + 5, baseY)) return true;
				break;

			case 0b0011:
				if (testTriMid(baseX + 4, baseY)) return true;
				if (testTriMid(baseX + 5, baseY)) return true;
				break;

			case 0b0100:
				if (testTriMid(baseX + 6, baseY)) return true;
				break;

			case 0b0101:
				if (testTriMid(baseX + 4, baseY)) return true;
				if (testTriMid(baseX + 6, baseY)) return true;
				break;

			case 0b0110:
				if (testTriMid(baseX + 5, baseY)) return true;
				if (testTriMid(baseX + 6, baseY)) return true;
				break;

			case 0b0111:
				if (testTriMid(baseX + 4, baseY)) return true;
				if (testTriMid(baseX + 5, baseY)) return true;
				if (testTriMid(baseX + 6, baseY)) return true;
				break;

			case 0b1000:
				if (testTriMid(baseX + 7, baseY)) return true;
				break;

			case 0b1001:
				if (testTriMid(baseX + 4, baseY)) return true;
				if (testTriMid(baseX + 7, baseY)) return true;
				break;

			case 0b1010:
				if (testTriMid(baseX + 5, baseY)) return true;
				if (testTriMid(baseX + 7, baseY)) return true;
				break;

			case 0b1011:
				if (testTriMid(baseX + 4, baseY)) return true;
				if (testTriMid(baseX + 5, baseY)) return true;
				if (testTriMid(baseX + 7, baseY)) return true;
				break;

			case 0b1100:
				if (testTriMid(baseX + 6, baseY)) return true;
				if (testTriMid(baseX + 7, baseY)) return true;
				break;

			case 0b1101:
				if (testTriMid(baseX + 4, baseY)) return true;
				if (testTriMid(baseX + 6, baseY)) return true;
				if (testTriMid(baseX + 7, baseY)) return true;
				break;

			case 0b1110:
				if (testTriMid(baseX + 5, baseY)) return true;
				if (testTriMid(baseX + 6, baseY)) return true;
				if (testTriMid(baseX + 7, baseY)) return true;
				break;

			case 0b1111:
				if (testTriMid(baseX + 4, baseY)) return true;
				if (testTriMid(baseX + 5, baseY)) return true;
				if (testTriMid(baseX + 6, baseY)) return true;
				if (testTriMid(baseX + 7, baseY)) return true;
				break;
			}

			++baseY;
			coverage >>= 8;
		}

		return false;
	}

	//	private boolean testTriTopOld(final int topX, final int topY) {
	//		final int index = topIndex(topX, topY) << 1; // shift because two words per index
	//		final long wordFull = topBins[index + OFFSET_FULL];
	//
	//		if (wordFull == -1L) {
	//			// bin fully occluded
	//			return false;
	//		}
	//
	//		final long wordPartial = topBins[index + OFFSET_PARTIAL];
	//
	//		if (wordPartial == 0) {
	//			// bin has no occlusion
	//			return true;
	//		}
	//
	//		final int binOriginX = topX << TOP_AXIS_SHIFT;
	//		final int binOriginY = topY << TOP_AXIS_SHIFT;
	//		final int minPixelX = this.minPixelX;
	//		final int minPixelY = this.minPixelY;
	//
	//		final int minX = minPixelX < binOriginX ? binOriginX : minPixelX;
	//		final int minY = minPixelY < binOriginY ? binOriginY : minPixelY;
	//		final int maxX = Math.min(maxPixelX, binOriginX + TOP_BIN_PIXEL_DIAMETER - 1);
	//		final int maxY = Math.min(maxPixelY, binOriginY + TOP_BIN_PIXEL_DIAMETER - 1);
	//
	//		long coverage = coverageMask((minX >> MID_AXIS_SHIFT) & 7, (minY >> MID_AXIS_SHIFT) & 7,
	//				(maxX >> MID_AXIS_SHIFT) & 7, (maxY >> MID_AXIS_SHIFT) & 7);
	//
	//		coverage &= ~wordFull;
	//
	//		if (coverage == 0) {
	//			// all bins fully occluded
	//			return false;
	//		}
	//
	//		if ((coverage & wordPartial) == 0) {
	//			// no bins are occluded
	//			return true;
	//		}
	//
	//		final int baseX = topX << BIN_AXIS_SHIFT;
	//		final int baseY = topY << BIN_AXIS_SHIFT;
	//
	//		long mask = 1;
	//
	//
	//		for (int n = 0; n < 64; ++n) {
	//			if ((mask & coverage) != 0 && testTriMid(baseX + (n & 7), baseY + (n >> 3))) {
	//				return true;
	//			}
	//
	//			mask <<= 1;
	//		}
	//
	//		return false;
	//	}

	//	@SuppressWarnings("unused")
	//	private boolean testTriMidWithCompare(final int midX, final int midY) {
	//		final boolean oldResult = testTriMidOld(midX,  midY);
	//		final boolean newResult = testTriMid(midX,  midY);
	//
	//		if (newResult != oldResult) {
	//			final int index = midIndex(midX, midY) << 1; // shift because two words per index
	//			System.out.println("INPUT WORD - FULL: " + oldResult);
	//			printMask8x8(midBins[index + OFFSET_FULL]);
	//			System.out.println("INPUT WORD - PARTIAL: " + oldResult);
	//			printMask8x8(midBins[index + OFFSET_PARTIAL]);
	//			System.out.println();
	//
	//			debugMid = true;
	//			testTriMid(midX,  midY);
	//			debugMid = false;
	//
	//			testTriMid(midX,  midY);
	//			System.out.println();
	//		}
	//
	//		return newResult;
	//	}


	private boolean testTriMid(final int midX, final int midY) {
		midTile.moveTo(midX, midY);
		final int index = midIndex(midX, midY) << 1; // shift because two words per index
		final long notFull = ~midBins[index + OFFSET_FULL];

		long coverage = midTile.computeCoverage() & notFull;

		// if all covered tiles are full, then nothing visible
		if (coverage  == 0)  {
			return false;
		}

		final long fullCoverage = midTile.fullCoverage;

		// if any fully covered tile is not full then must be visible
		if  ((fullCoverage & notFull) != 0) {
			return true;
		}

		// don't check full tiles - they would have been caught earlier
		coverage &= ~fullCoverage;

		// PERF: is this always true? Significantly increases visible count
		// if any partially covered tiles are fully empty then must be visible
		//		if ((coverage & ~midBins[index + OFFSET_PARTIAL]) != 0) {
		//			return true;
		//		}

		if (coverage == 0) {
			// nothing left to check
			return false;
		}

		final int baseX = midX << BIN_AXIS_SHIFT;
		int baseY = midY << BIN_AXIS_SHIFT;

		for (int y = 0; y < 8; y++) {
			final int bits = (int) coverage & 0xFF;

			switch (bits & 0xF) {
			case 0b0000:
				break;

			case 0b0001:
				if (testTriLow(baseX + 0, baseY)) return true;
				break;

			case 0b0010:
				if (testTriLow(baseX + 1, baseY)) return true;
				break;

			case 0b0011:
				if (testTriLow(baseX + 0, baseY)) return true;
				if (testTriLow(baseX + 1, baseY)) return true;
				break;

			case 0b0100:
				if (testTriLow(baseX + 2, baseY)) return true;
				break;

			case 0b0101:
				if (testTriLow(baseX + 0, baseY)) return true;
				if (testTriLow(baseX + 2, baseY)) return true;
				break;

			case 0b0110:
				if (testTriLow(baseX + 1, baseY)) return true;
				if (testTriLow(baseX + 2, baseY)) return true;
				break;

			case 0b0111:
				if (testTriLow(baseX + 0, baseY)) return true;
				if (testTriLow(baseX + 1, baseY)) return true;
				if (testTriLow(baseX + 2, baseY)) return true;
				break;

			case 0b1000:
				if (testTriLow(baseX + 3, baseY)) return true;
				break;

			case 0b1001:
				if (testTriLow(baseX + 0, baseY)) return true;
				if (testTriLow(baseX + 3, baseY)) return true;
				break;

			case 0b1010:
				if (testTriLow(baseX + 1, baseY)) return true;
				if (testTriLow(baseX + 3, baseY)) return true;
				break;

			case 0b1011:
				if (testTriLow(baseX + 0, baseY)) return true;
				if (testTriLow(baseX + 1, baseY)) return true;
				if (testTriLow(baseX + 3, baseY)) return true;
				break;

			case 0b1100:
				if (testTriLow(baseX + 2, baseY)) return true;
				if (testTriLow(baseX + 3, baseY)) return true;
				break;

			case 0b1101:
				if (testTriLow(baseX + 0, baseY)) return true;
				if (testTriLow(baseX + 2, baseY)) return true;
				if (testTriLow(baseX + 3, baseY)) return true;
				break;

			case 0b1110:
				if (testTriLow(baseX + 1, baseY)) return true;
				if (testTriLow(baseX + 2, baseY)) return true;
				if (testTriLow(baseX + 3, baseY)) return true;
				break;

			case 0b1111:
				if (testTriLow(baseX + 0, baseY)) return true;
				if (testTriLow(baseX + 1, baseY)) return true;
				if (testTriLow(baseX + 2, baseY)) return true;
				if (testTriLow(baseX + 3, baseY)) return true;
				break;
			}

			switch (bits >> 4) {
			case 0b0000:
				break;

			case 0b0001:
				if (testTriLow(baseX + 4, baseY)) return true;
				break;

			case 0b0010:
				if (testTriLow(baseX + 5, baseY)) return true;
				break;

			case 0b0011:
				if (testTriLow(baseX + 4, baseY)) return true;
				if (testTriLow(baseX + 5, baseY)) return true;
				break;

			case 0b0100:
				if (testTriLow(baseX + 6, baseY)) return true;
				break;

			case 0b0101:
				if (testTriLow(baseX + 4, baseY)) return true;
				if (testTriLow(baseX + 6, baseY)) return true;
				break;

			case 0b0110:
				if (testTriLow(baseX + 5, baseY)) return true;
				if (testTriLow(baseX + 6, baseY)) return true;
				break;

			case 0b0111:
				if (testTriLow(baseX + 4, baseY)) return true;
				if (testTriLow(baseX + 5, baseY)) return true;
				if (testTriLow(baseX + 6, baseY)) return true;
				break;

			case 0b1000:
				if (testTriLow(baseX + 7, baseY)) return true;
				break;

			case 0b1001:
				if (testTriLow(baseX + 4, baseY)) return true;
				if (testTriLow(baseX + 7, baseY)) return true;
				break;

			case 0b1010:
				if (testTriLow(baseX + 5, baseY)) return true;
				if (testTriLow(baseX + 7, baseY)) return true;
				break;

			case 0b1011:
				if (testTriLow(baseX + 4, baseY)) return true;
				if (testTriLow(baseX + 5, baseY)) return true;
				if (testTriLow(baseX + 7, baseY)) return true;
				break;

			case 0b1100:
				if (testTriLow(baseX + 6, baseY)) return true;
				if (testTriLow(baseX + 7, baseY)) return true;
				break;

			case 0b1101:
				if (testTriLow(baseX + 4, baseY)) return true;
				if (testTriLow(baseX + 6, baseY)) return true;
				if (testTriLow(baseX + 7, baseY)) return true;
				break;

			case 0b1110:
				if (testTriLow(baseX + 5, baseY)) return true;
				if (testTriLow(baseX + 6, baseY)) return true;
				if (testTriLow(baseX + 7, baseY)) return true;
				break;

			case 0b1111:
				if (testTriLow(baseX + 4, baseY)) return true;
				if (testTriLow(baseX + 5, baseY)) return true;
				if (testTriLow(baseX + 6, baseY)) return true;
				if (testTriLow(baseX + 7, baseY)) return true;
				break;
			}

			++baseY;
			coverage >>= 8;
		}

		return false;
	}

	//	private boolean testTriMidOld(final int midX, final int midY) {
	//		final int index = midIndex(midX, midY) << 1; // shift because two words per index
	//		final long wordFull = midBins[index + OFFSET_FULL];
	//		final long wordPartial = midBins[index + OFFSET_PARTIAL];
	//
	//		final int binOriginX = midX << MID_AXIS_SHIFT;
	//		final int binOriginY = midY << MID_AXIS_SHIFT;
	//		final int minPixelX = this.minPixelX;
	//		final int minPixelY = this.minPixelY;
	//
	//		final int minX = minPixelX < binOriginX ? binOriginX : minPixelX;
	//		final int minY = minPixelY < binOriginY ? binOriginY : minPixelY;
	//		final int maxX = Math.min(maxPixelX, binOriginX + MID_BIN_PIXEL_DIAMETER - 1);
	//		final int maxY = Math.min(maxPixelY, binOriginY + MID_BIN_PIXEL_DIAMETER - 1);
	//
	//		final int lowX0 = minX >>> LOW_AXIS_SHIFT;
	//		final int lowX1 = maxX >>> LOW_AXIS_SHIFT;
	//		final int lowY0 = (minY >>> LOW_AXIS_SHIFT);
	//		final int lowY1 = maxY >>> LOW_AXIS_SHIFT;
	//
	//		if (lowX0 == lowX1)  {
	//			if (lowY0 == lowY1) {
	//				// single  bin
	//				final long mask = pixelMask(lowX0, lowY0);
	//				return (wordPartial & mask) == 0 || ((wordFull & mask) == 0 && testTriLow(lowX0, lowY0));
	//			} else {
	//				// single column
	//				long mask = pixelMask(lowX0, lowY0);
	//
	//				for (int y = lowY0; y <= lowY1; ++y) {
	//					if ((wordPartial & mask) == 0 || ((wordFull & mask) == 0 && testTriLow(lowX0, y))) {
	//						return true;
	//					}
	//
	//					mask  <<= 8;
	//				}
	//
	//				return false;
	//			}
	//
	//		} else if (lowY0 == lowY1) {
	//			// single row
	//			long mask = pixelMask(lowX0, lowY0);
	//
	//			for (int x = lowX0; x <= lowX1; ++x) {
	//				if ((wordPartial & mask) == 0 || ((wordFull & mask) == 0 && testTriLow(x, lowY0))) {
	//					return true;
	//				}
	//
	//				mask  <<= 1;
	//			}
	//
	//			return false;
	//		}
	//
	//		long coverage = coverageMask(lowX0 & 7, lowY0 & 7, lowX1 & 7, lowY1 & 7);
	//
	//		coverage &= ~wordFull;
	//
	//		if (coverage == 0) {
	//			return false;
	//		}
	//
	//		if ((coverage & wordPartial) == 0) {
	//			return true;
	//		}
	//
	//		final int baseX = midX << BIN_AXIS_SHIFT;
	//		int baseY = midY << BIN_AXIS_SHIFT;
	//
	//		for (int y = 0; y < 8; y++) {
	//			final int bits = (int) coverage & 0xFF;
	//
	//			switch (bits & 0xF) {
	//			case 0b0000:
	//				break;
	//
	//			case 0b0001:
	//				if (testTriLow(baseX + 0, baseY)) return true;
	//				break;
	//
	//			case 0b0010:
	//				if (testTriLow(baseX + 1, baseY)) return true;
	//				break;
	//
	//			case 0b0011:
	//				if (testTriLow(baseX + 0, baseY)) return true;
	//				if (testTriLow(baseX + 1, baseY)) return true;
	//				break;
	//
	//			case 0b0100:
	//				if (testTriLow(baseX + 2, baseY)) return true;
	//				break;
	//
	//			case 0b0101:
	//				if (testTriLow(baseX + 0, baseY)) return true;
	//				if (testTriLow(baseX + 2, baseY)) return true;
	//				break;
	//
	//			case 0b0110:
	//				if (testTriLow(baseX + 1, baseY)) return true;
	//				if (testTriLow(baseX + 2, baseY)) return true;
	//				break;
	//
	//			case 0b0111:
	//				if (testTriLow(baseX + 0, baseY)) return true;
	//				if (testTriLow(baseX + 1, baseY)) return true;
	//				if (testTriLow(baseX + 2, baseY)) return true;
	//				break;
	//
	//			case 0b1000:
	//				if (testTriLow(baseX + 3, baseY)) return true;
	//				break;
	//
	//			case 0b1001:
	//				if (testTriLow(baseX + 0, baseY)) return true;
	//				if (testTriLow(baseX + 3, baseY)) return true;
	//				break;
	//
	//			case 0b1010:
	//				if (testTriLow(baseX + 1, baseY)) return true;
	//				if (testTriLow(baseX + 3, baseY)) return true;
	//				break;
	//
	//			case 0b1011:
	//				if (testTriLow(baseX + 0, baseY)) return true;
	//				if (testTriLow(baseX + 1, baseY)) return true;
	//				if (testTriLow(baseX + 3, baseY)) return true;
	//				break;
	//
	//			case 0b1100:
	//				if (testTriLow(baseX + 2, baseY)) return true;
	//				if (testTriLow(baseX + 3, baseY)) return true;
	//				break;
	//
	//			case 0b1101:
	//				if (testTriLow(baseX + 0, baseY)) return true;
	//				if (testTriLow(baseX + 2, baseY)) return true;
	//				if (testTriLow(baseX + 3, baseY)) return true;
	//				break;
	//
	//			case 0b1110:
	//				if (testTriLow(baseX + 1, baseY)) return true;
	//				if (testTriLow(baseX + 2, baseY)) return true;
	//				if (testTriLow(baseX + 3, baseY)) return true;
	//				break;
	//
	//			case 0b1111:
	//				if (testTriLow(baseX + 0, baseY)) return true;
	//				if (testTriLow(baseX + 1, baseY)) return true;
	//				if (testTriLow(baseX + 2, baseY)) return true;
	//				if (testTriLow(baseX + 3, baseY)) return true;
	//				break;
	//			}
	//
	//			switch (bits >> 4) {
	//			case 0b0000:
	//				break;
	//
	//			case 0b0001:
	//				if (testTriLow(baseX + 4, baseY)) return true;
	//				break;
	//
	//			case 0b0010:
	//				if (testTriLow(baseX + 5, baseY)) return true;
	//				break;
	//
	//			case 0b0011:
	//				if (testTriLow(baseX + 4, baseY)) return true;
	//				if (testTriLow(baseX + 5, baseY)) return true;
	//				break;
	//
	//			case 0b0100:
	//				if (testTriLow(baseX + 6, baseY)) return true;
	//				break;
	//
	//			case 0b0101:
	//				if (testTriLow(baseX + 4, baseY)) return true;
	//				if (testTriLow(baseX + 6, baseY)) return true;
	//				break;
	//
	//			case 0b0110:
	//				if (testTriLow(baseX + 5, baseY)) return true;
	//				if (testTriLow(baseX + 6, baseY)) return true;
	//				break;
	//
	//			case 0b0111:
	//				if (testTriLow(baseX + 4, baseY)) return true;
	//				if (testTriLow(baseX + 5, baseY)) return true;
	//				if (testTriLow(baseX + 6, baseY)) return true;
	//				break;
	//
	//			case 0b1000:
	//				if (testTriLow(baseX + 7, baseY)) return true;
	//				break;
	//
	//			case 0b1001:
	//				if (testTriLow(baseX + 4, baseY)) return true;
	//				if (testTriLow(baseX + 7, baseY)) return true;
	//				break;
	//
	//			case 0b1010:
	//				if (testTriLow(baseX + 5, baseY)) return true;
	//				if (testTriLow(baseX + 7, baseY)) return true;
	//				break;
	//
	//			case 0b1011:
	//				if (testTriLow(baseX + 4, baseY)) return true;
	//				if (testTriLow(baseX + 5, baseY)) return true;
	//				if (testTriLow(baseX + 7, baseY)) return true;
	//				break;
	//
	//			case 0b1100:
	//				if (testTriLow(baseX + 6, baseY)) return true;
	//				if (testTriLow(baseX + 7, baseY)) return true;
	//				break;
	//
	//			case 0b1101:
	//				if (testTriLow(baseX + 4, baseY)) return true;
	//				if (testTriLow(baseX + 6, baseY)) return true;
	//				if (testTriLow(baseX + 7, baseY)) return true;
	//				break;
	//
	//			case 0b1110:
	//				if (testTriLow(baseX + 5, baseY)) return true;
	//				if (testTriLow(baseX + 6, baseY)) return true;
	//				if (testTriLow(baseX + 7, baseY)) return true;
	//				break;
	//
	//			case 0b1111:
	//				if (testTriLow(baseX + 4, baseY)) return true;
	//				if (testTriLow(baseX + 5, baseY)) return true;
	//				if (testTriLow(baseX + 6, baseY)) return true;
	//				if (testTriLow(baseX + 7, baseY)) return true;
	//				break;
	//			}
	//
	//			++baseY;
	//			coverage >>= 8;
	//		}
	//
	//		return false;
	//	}

	// TODO: remove
	//	@SuppressWarnings("unused")
	//	private boolean testTriLowWithCompare(int lowX, int lowY) {
	//		final int index = lowIndex(lowX, lowY);
	//		final long inputWord  =  lowBins[index];
	//
	//		final boolean oldResult = testTriLowOld(lowX, lowY);
	//		final boolean newResult = testTriLow(lowX, lowY);
	//
	//		if (newResult != oldResult) {
	//			System.out.println("INPUT WORD: " + oldResult);
	//			printMask8x8(inputWord);
	//			System.out.println();
	//
	//			debugLow = true;
	//			testTriLow(lowX, lowY);
	//			debugLow = false;
	//
	//			System.out.println();
	//		}
	//
	//		return newResult;
	//	}

	private boolean testTriLow(int lowX, int lowY) {
		lowTile.moveTo(lowX, lowY);
		final long coverage = lowTile.computeCoverage();
		final long word =  lowBins[lowIndex(lowX, lowY)];
		return (~word & coverage) != 0;
	}

	private static final int COVERAGE_NONE = 0;
	private static final int COVERAGE_PARTIAL = 1;
	// 8 bits away from partial coverage so partial and full results can be accumulated in one word and combined with their respective masks
	private static final int COVERAGE_FULL = 1 << 8;
	private static final int OFFSET_FULL = 0;
	private static final int OFFSET_PARTIAL = 1;

	private abstract class AbstractTile {
		protected final Edge e0;
		protected final Edge e1;
		protected final Edge e2;

		protected final TileEdge te0;
		protected final TileEdge te1;
		protected final TileEdge te2;

		protected final void computeSpan() {
			te0.prepare();
			te1.prepare();
			te2.prepare();
		}

		protected  AbstractTile(Triangle triangle, int tileSize) {
			e0 = triangle.e0;
			e1 = triangle.e1;
			e2 = triangle.e2;
			te0 = new TileEdge(e0, tileSize);
			te1 = new TileEdge(e1, tileSize);
			te2 = new TileEdge(e2, tileSize);
		}

		/**
		 * Shifts mask 1 pixel towards positive half plane
		 * Use to construct full coverage masks
		 * @param mask
		 * @param edgeFlag
		 * @return
		 */
		protected final long shiftMask(int edgeFlag, long mask) {
			switch  (edgeFlag) {
			case EDGE_TOP:
				return (mask >>> 8);

			case EDGE_RIGHT:
				return (mask >>> 1) & 0x7F7F7F7F7F7F7F7FL;

			case EDGE_LEFT:
				return (mask << 1) & 0xFEFEFEFEFEFEFEFEL;

			case EDGE_TOP_LEFT:
				return (((mask << 1) & 0xFEFEFEFEFEFEFEFEL) >>> 8);

			case EDGE_BOTTOM:
				return mask << 8;

			case EDGE_BOTTOM_LEFT:
				return ((mask << 1) & 0xFEFEFEFEFEFEFEFEL) << 8;

			case EDGE_TOP_RIGHT:
				return ((mask >>> 1) & 0x7F7F7F7F7F7F7F7FL) >>> 8;

			case EDGE_BOTTOM_RIGHT:
				return ((mask >>> 1) & 0x7F7F7F7F7F7F7F7FL) << 8;

			default:
				assert false : "Edge flag out of bounds.";
			return mask;
			}
		}

		public final void moveTo(int tileX, int tileY) {
			te0.moveTo(tileX, tileY);
			te1.moveTo(tileX, tileY);
			te2.moveTo(tileX, tileY);
		}
	}

	private class LowTile extends AbstractTile {
		// PERF: accept/preserve one or more corners pre-computed
		// PERF: add moveDown/Left/Right/Up
		// PERF: make corner compute lazy
		// PERF: compute base a/b based on origin or snapped corner to avoid dx/dy computation

		protected LowTile(Triangle triangle) {
			super(triangle, LOW_BIN_PIXEL_DIAMETER);
		}

		public long computeCoverage() {
			final int c0 = te0.classify();
			final int c1 = te1.classify();
			final int c2 = te2.classify();

			final int t = c0 | c1 | c2;

			if (t == INSIDE)  {
				return -1L;
			} else if((t & OUTSIDE) ==  OUTSIDE) {
				return 0;
			}

			long result = -1L;

			if (c0 == INTERSECTING) {
				final long mask = te0.buildMask();
				result &= mask;
			}

			if (c1 == INTERSECTING) {
				final long mask = te1.buildMask();
				result &= mask;
			}

			if (c2 == INTERSECTING) {
				final long mask = te2.buildMask();
				result &= mask;
			}

			return result;
		}
	}

	private class MidTile extends AbstractTile {
		protected MidTile(Triangle triangle) {
			super(triangle, MID_BIN_PIXEL_DIAMETER);
		}

		//		protected int midX, midY;

		protected long fullCoverage;

		/**
		 *
		 * @return mask that inclueds edge coverage.
		 */
		public long computeCoverage() {
			final int c0 = te0.classify();
			final int c1 = te1.classify();
			final int c2 = te2.classify();

			final int t = c0 | c1 | c2;

			if (t == INSIDE)  {
				fullCoverage = -1L;
				return -1L;
			} else if((t & OUTSIDE) ==  OUTSIDE) {
				fullCoverage = 0;
				return 0;
			}

			long result = -1L;
			long full = -1L;

			if (c0 == INTERSECTING) {
				final long mask = te0.buildMask();
				result &= mask;
				full &= shiftMask(e0.shape, mask);
			}

			if (c1 == INTERSECTING) {
				final long mask = te1.buildMask();
				result &= mask;
				full &= shiftMask(e1.shape, mask);
			}

			if (c2 == INTERSECTING) {
				final long mask = te2.buildMask();
				result &= mask;
				full &= shiftMask(e2.shape, mask);
			}

			fullCoverage = full;
			return result;
		}
	}

	private class TopTile extends AbstractTile {
		protected TopTile(Triangle triangle) {
			super(triangle, TOP_BIN_PIXEL_DIAMETER);
		}

		protected long fullCoverage;

		/**
		 *
		 * @return mask that includes edge coverage.
		 */
		public long computeCoverage() {
			final int c0 = te0.classify();
			final int c1 = te1.classify();
			final int c2 = te2.classify();

			final int t = c0 | c1 | c2;

			if (t == INSIDE)  {
				fullCoverage = -1L;
				return -1L;
			} else if((t & OUTSIDE) ==  OUTSIDE) {
				fullCoverage = 0;
				return 0;
			}

			long result = -1L;
			long full = -1L;

			if (c0 == INTERSECTING) {
				final long mask = te0.buildMask();
				result &= mask;
				full &= shiftMask(e0.shape, mask);
			}

			if (c1 == INTERSECTING) {
				final long mask = te1.buildMask();
				result &= mask;
				full &= shiftMask(e1.shape, mask);
			}

			if (c2 == INTERSECTING) {
				final long mask = te2.buildMask();
				result &= mask;
				full &= shiftMask(e2.shape, mask);
			}

			fullCoverage = full;
			return result;

			//			final long result = m0 & m1 & m2;
			//
			//			if  (debugTop) {
			//				System.out.println("E0 = " + e0);
			//				printMask8x8(m0);
			//				System.out.println();
			//				System.out.println("E1 = " + e1);
			//				printMask8x8(m1);
			//				System.out.println();
			//				System.out.println("E2 = " + e2);
			//				printMask8x8(m2);
			//				System.out.println();
			//
			//				System.out.println("COMBINED");
			//				printMask8x8(m0 & m1 & m2);
			//
			//				final float x0 = vertexData[v0 + PV_PX] / 16f;
			//				final float y0 = vertexData[v0 + PV_PY] / 16f;
			//				final float x1 = vertexData[v1 + PV_PX] / 16f;
			//				final float y1 = vertexData[v1 + PV_PY] / 16f;
			//				final float x2 = vertexData[v2 + PV_PX] / 16f;
			//				final float y2 = vertexData[v2 + PV_PY] / 16f;
			//
			//				System.out.println();
			//				System.out.println(String.format("E00: %d, %d, %d", tileData[CORNER_X0_Y0_E0 + TILE_TOP_START], tileData[CORNER_X0_Y0_E1 + TILE_TOP_START], tileData[CORNER_X0_Y0_E2 + TILE_TOP_START]));
			//				System.out.println(String.format("E10: %d, %d, %d", tileData[CORNER_X1_Y0_E0 + TILE_TOP_START], tileData[CORNER_X1_Y0_E1 + TILE_TOP_START], tileData[CORNER_X1_Y0_E2 + TILE_TOP_START]));
			//				System.out.println(String.format("E01: %d, %d, %d", tileData[CORNER_X0_Y1_E0 + TILE_TOP_START], tileData[CORNER_X0_Y1_E1 + TILE_TOP_START], tileData[CORNER_X0_Y1_E2 + TILE_TOP_START]));
			//				System.out.println(String.format("E11: %d, %d, %d", tileData[CORNER_X1_Y1_E0 + TILE_TOP_START], tileData[CORNER_X1_Y1_E1 + TILE_TOP_START], tileData[CORNER_X1_Y1_E2 + TILE_TOP_START]));
			//				System.out.println();
			//				System.out.println(String.format("Points: %f\t%f\t%f\t%f\t%f\t%f", x0, y0, x1, y1, x2, y2));
			//				System.out.println(String.format("A,B: (%d, %d)  (%d, %d)  (%d, %d)", a[0], b[0], a[1], b[1], a[2], b[2]));
			//				System.out.println(String.format("Edges: %d, %d, %d", e0, e1, e2));
			//				//				System.out.println(String.format("origin: (%d, %d)", topX << MID_AXIS_SHIFT,  topY << MID_AXIS_SHIFT));
			//				System.out.println();
			//			}
			//
			//			return result;
		}
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
