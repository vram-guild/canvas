package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PX;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PY;

// Some elements are adapted from content found at
// https://fgiesen.wordpress.com/2013/02/17/optimizing-sw-occlusion-culling-index/
// by Fabian “ryg” Giesen. That content is in the public domain.

// PERF: try propagating edge function values up/down heirarchy
// PERF: remove partial coverage mask if can't make it pay
public class TerrainOccluder extends ClippingTerrainOccluder  {
	private final int[] aMid = new int[3];
	private final int[] bMid = new int[3];
	private final int[] abMid = new int[3];

	private final LowTile lowTile = new LowTile();

	private void prepareTriMidA() {
		for (int i = 0; i < 3; ++i) {
			aMid[i] = a[0] * MID_BIN_PIXEL_DIAMETER_VECTOR[i];
		}
	}

	private void prepareTriMidB() {
		for (int i = 0; i < 3; ++i) {
			bMid[i] = b[0] * MID_BIN_PIXEL_DIAMETER_VECTOR[i];
		}
	}

	private void prepareTriMidAB() {
		for (int i = 0; i < 3; ++i) {
			abMid[i] = aMid[0] + bMid[0];
		}
	}

	@Override
	protected void prepareTriScan(int v0, int v1, int v2) {
		super.prepareTriScan(v0, v1, v2);
		prepareTriMidA();
		prepareTriMidB();
		prepareTriMidAB();

		lowTile.computeSpan();
	}

	@Override
	protected void drawTri(int v0, int v1, int v2) {
		final int boundsResult  = prepareTriBounds(v0, v1, v2);

		if (boundsResult == BoundsResult.OUT_OF_BOUNDS) {
			return;
		}

		if (boundsResult == BoundsResult.NEEDS_CLIP) {
			drawClippedLowX(v0, v1, v2);
			return;
		}

		//  PERF: consider skipping small tris at intermediate ranges (at extreme range only full sections are attempted)
		prepareTriScan(v0, v1, v2);

		final int bx0 = minPixelX >> TOP_AXIS_SHIFT;
		final int bx1 = maxPixelX >> TOP_AXIS_SHIFT;
		final int by0 = minPixelY >> TOP_AXIS_SHIFT;
		final int by1 = maxPixelY >> TOP_AXIS_SHIFT;

		if (bx0 == bx1 && by0 == by1) {
			drawTriTop(bx0, by0);
		} else {
			for (int by = by0; by <= by1; by++) {
				for (int bx = bx0; bx <= bx1; bx++) {
					drawTriTop(bx, by);
				}
			}
		}
	}

	private void drawTriTop(final int topX, final int topY) {
		final int index = topIndex(topX, topY) << 1; // shift because two words per index
		long wordFull = topBins[index + OFFSET_FULL];

		if (wordFull == -1L) {
			// if bin fully occluded nothing to do
			return;
		}

		final int binOriginX = topX << TOP_AXIS_SHIFT;
		final int binOriginY = topY << TOP_AXIS_SHIFT;
		final int minPixelX = this.minPixelX;
		final int minPixelY = this.minPixelY;

		final int minX = minPixelX < binOriginX ? binOriginX : minPixelX;
		final int minY = minPixelY < binOriginY ? binOriginY : minPixelY;
		final int maxX = Math.min(maxPixelX, binOriginX + TOP_BIN_PIXEL_DIAMETER - 1);
		final int maxY = Math.min(maxPixelY, binOriginY + TOP_BIN_PIXEL_DIAMETER - 1);

		long coverage = coverageMask((minX >> MID_AXIS_SHIFT) & 7, (minY >> MID_AXIS_SHIFT) & 7,
				(maxX >> MID_AXIS_SHIFT) & 7, (maxY >> MID_AXIS_SHIFT) & 7);

		coverage &= ~wordFull;

		if (coverage == 0) {
			return;
		}

		final int baseX = topX << BIN_AXIS_SHIFT;
		final int baseY = topY << BIN_AXIS_SHIFT;
		long wordPartial = topBins[index + OFFSET_PARTIAL];
		long mask = 1;

		for (int n = 0; n < 64; ++n) {
			if ((mask & coverage) != 0) {
				final int mid = drawTriMid(baseX + (n & 7), baseY + (n >> 3));

				if (mid != COVERAGE_NONE) {
					wordPartial |= mask;

					if (mid == COVERAGE_FULL) {
						wordFull |= mask;
					}
				}
			}

			mask <<= 1;
		}

		topBins[index + OFFSET_FULL] = wordFull;
		topBins[index + OFFSET_PARTIAL] = wordPartial;
	}

	private void fillMidBinChildren(final int midX, final int midY) {
		final int lowX0 = midX << 3;
		final int lowY0 = midY << 3;
		final int lowX1 = lowX0 + 7;
		final int lowY1 = lowY0 + 7;

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
		final int index = midIndex(midX, midY) << 1; // shift because two words per index
		long wordFull = midBins[index + OFFSET_FULL];

		final int binOriginX = midX << MID_AXIS_SHIFT;
		final int binOriginY = midY << MID_AXIS_SHIFT;
		final int minPixelX = this.minPixelX;
		final int minPixelY = this.minPixelY;

		final int minX = minPixelX < binOriginX ? binOriginX : minPixelX;
		final int minY = minPixelY < binOriginY ? binOriginY : minPixelY;
		final int maxX = Math.min(maxPixelX, binOriginX + MID_BIN_PIXEL_DIAMETER - 1);
		final int maxY = Math.min(maxPixelY, binOriginY + MID_BIN_PIXEL_DIAMETER - 1);

		final int lowX0 = minX >> LOW_AXIS_SHIFT;
		final int lowX1 = maxX >> LOW_AXIS_SHIFT;
		final int lowY0 = minY >> LOW_AXIS_SHIFT;
		final int lowY1 = (maxY >> LOW_AXIS_SHIFT); // parens stop eclipse formatter from freaking

		if (lowX0 == lowX1)  {
			if (lowY0 == lowY1) {
				// single  bin
				final long mask = pixelMask(lowX0, lowY0);

				if ((wordFull & mask) == 0) {
					final int lowCoverage = drawTriLow(lowX0, lowY0);

					if (lowCoverage != COVERAGE_NONE) {
						midBins[index + OFFSET_PARTIAL] |= mask;

						if (lowCoverage == COVERAGE_FULL) {
							wordFull |= mask;
							midBins[index + OFFSET_FULL] = wordFull;
						}

						return wordFull == -1 ? COVERAGE_FULL : COVERAGE_PARTIAL;
					}
				}

				return COVERAGE_NONE;
			} else {
				// single column
				long mask = pixelMask(lowX0, lowY0);
				long wordPartial = midBins[index + OFFSET_PARTIAL];

				for (int y = lowY0; y <= lowY1; ++y) {
					if ((wordFull & mask) == 0) {
						final int lowCover = drawTriLow(lowX0, y);

						if(lowCover != COVERAGE_NONE) {
							wordPartial |= mask;

							if (lowCover == COVERAGE_FULL) {
								wordFull |= mask;
							}
						}
					}

					mask  <<= 8;
				}

				midBins[index + OFFSET_FULL] = wordFull;
				midBins[index + OFFSET_PARTIAL] = wordPartial;
				return wordFull == -1 ? COVERAGE_FULL : wordPartial == 0 ? COVERAGE_NONE : COVERAGE_PARTIAL;
			}

		} else if (lowY0 == lowY1) {
			// single row
			long mask = pixelMask(lowX0, lowY0);
			long wordPartial = midBins[index + OFFSET_PARTIAL];

			for (int x = lowX0; x <= lowX1; ++x) {
				if ((wordFull & mask) == 0) {
					final int lowCover = drawTriLow(x, lowY0);

					if(lowCover != COVERAGE_NONE) {
						wordPartial |= mask;

						if (lowCover == COVERAGE_FULL) {
							wordFull |= mask;
						}
					}
				}

				mask  <<= 1;
			}

			midBins[index + OFFSET_FULL] = wordFull;
			midBins[index + OFFSET_PARTIAL] = wordPartial;
			return wordFull == -1 ? COVERAGE_FULL : wordPartial == 0 ? COVERAGE_NONE : COVERAGE_PARTIAL;
		}

		long coverage = coverageMask(lowX0 & 7, lowY0 & 7, lowX1 & 7, lowY1 & 7);

		// optimize whole bin case
		if (coverage == -1L && isTriMidCovered(minX,  minY)) {
			midBins[index + OFFSET_FULL] =  -1;
			midBins[index + OFFSET_PARTIAL] =  -1;

			if (ENABLE_RASTER_OUTPUT) {
				fillMidBinChildren(midX, midY);
			}

			return COVERAGE_FULL;
		}

		coverage &= ~wordFull;

		if (coverage == 0) {
			return COVERAGE_NONE;
		}

		final int baseX = midX << BIN_AXIS_SHIFT;
		int baseY = midY << BIN_AXIS_SHIFT;
		long wordPartial = midBins[index + OFFSET_PARTIAL];

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

		midBins[index + OFFSET_FULL] = wordFull;
		midBins[index + OFFSET_PARTIAL] = wordPartial;

		return wordFull == -1L ? COVERAGE_FULL : wordPartial == 0 ? COVERAGE_NONE : COVERAGE_PARTIAL;
	}

	private boolean isTriMidCovered(int minX, int minY) {
		final int dx = minX - minPixelX;
		final int dy = minY - minPixelY;

		//		final int w0_row = wOrigin0 + dx * a0 + dy * b0;
		//		final int w1_row = wOrigin1 + dx * a1 + dy * b1;
		//		final int w2_row = wOrigin2 + dx * a2 + dy * b2;

		computeRow(dx, dy);
		final int[] wRow = this.wRow;
		final int w0_row = wRow[0];
		final int w1_row = wRow[1];
		final int w2_row = wRow[2];

		if ((w0_row | w1_row | w2_row
				| (w0_row + aMid[0]) | (w1_row + aMid[1]) | (w2_row + aMid[2])
				| (w0_row + bMid[0]) | (w1_row + bMid[1]) | (w2_row + bMid[2])
				| (w0_row + abMid[0]) | (w1_row + abMid[1]) | (w2_row + abMid[2])) >= 0) {
			return true;
		} else {
			return false;
		}
	}

	//	private final Int2IntOpenHashMap binCounts = new Int2IntOpenHashMap();
	//	private int counter;

	private final int[] wLowX = new int[3];
	private final int[] wLowY = new int[3];

	private static void copyVec3(int[] source, int[] target) {
		for (int i = 0; i < 3; ++i) {
			target[i] = source[i];
		}
	}

	private static void addVec3(int[] source, int[] target) {
		for (int i = 0; i < 3; ++i) {
			target[i] += source[i];
		}
	}

	protected void computeLowY(final int dx, final int dy) {
		for (int i = 0; i < 3; ++i)  {
			wLowY[i] = wOrigin[i] + a[i] * dx + b[i] * dy;
		}
	}

	protected void computeLowX(final int dx, final int dy) {
		for (int i = 0; i < 3; ++i)  {
			wLowX[i] = wOrigin[i] + a[i] * dx + b[i] * dy;
		}
	}

	private int drawTriLow(int lowX, int lowY) {
		final int index = lowIndex(lowX, lowY);

		final int binOriginX = lowX << LOW_AXIS_SHIFT;
		final int binOriginY = lowY << LOW_AXIS_SHIFT;
		final int minPixelX = this.minPixelX;
		final int minPixelY = this.minPixelY;

		final int minX = minPixelX < binOriginX ? binOriginX : minPixelX;
		final int minY = minPixelY < binOriginY ? binOriginY : minPixelY;
		final int maxX = Math.min(maxPixelX, binOriginX + LOW_BIN_PIXEL_DIAMETER - 1);
		final int maxY = Math.min(maxPixelY, binOriginY + LOW_BIN_PIXEL_DIAMETER - 1);

		long coverage = coverageMask(minX & 7, minY & 7, maxX & 7, maxY & 7);

		// if filling whole bin then do it quick
		if (coverage == -1L && isTriLowCovered(minX,  minY)) {
			lowBins[index] = -1;
			return COVERAGE_FULL;
		}

		long word = lowBins[index];
		coverage &= ~word;

		if (coverage == 0L) {
			return COVERAGE_NONE;
		}

		//		final boolean oneRow = minY == maxY;
		//		final boolean oneCol = minX == maxX;
		//
		//		binCounts.addTo(oneRow ? (oneCol ? 0 : 1) : (oneCol ? 2 : 3), 1);
		//
		//		if (++counter == 8000000) {
		//			binCounts.int2IntEntrySet().fastForEach(e -> System.out.println(e.toString()));
		//			System.out.println();
		//			counter = 0;
		//			binCounts.clear();
		//		}

		final int x0 = minX & LOW_BIN_PIXEL_INDEX_MASK;
		final int x1 = maxX & LOW_BIN_PIXEL_INDEX_MASK;
		final int y0 = minY & LOW_BIN_PIXEL_INDEX_MASK;
		final int y1 = maxY & LOW_BIN_PIXEL_INDEX_MASK;
		final int dx = minX - minPixelX;
		final int dy = minY - minPixelY;

		final int[] wLowX = this.wLowX;
		final int[] wLowY = this.wLowY;
		computeLowY(dx, dy);

		for (int y = y0; y <= y1; y++) {
			copyVec3(wLowY, wLowX);
			long mask = 1L << ((y << BIN_AXIS_SHIFT) | x0);

			for (int x = x0; x <= x1; x++) {
				// If p is on or inside all edges, render pixel.
				if ((word & mask) == 0 && (wLowX[0] | wLowX[1] | wLowX[2]) >= 0) {
					word |= mask;
				}

				// One step to the right
				addVec3(a, wLowX);
				mask <<= 1;
			}

			// One row step
			addVec3(b, wLowY);
		}

		lowBins[index] = word;
		return word == 0 ? COVERAGE_NONE : word == -1L ? COVERAGE_FULL : COVERAGE_PARTIAL;
	}

	private boolean isTriLowCovered(int minX, int minY) {
		final int dx = minX - minPixelX;
		final int dy = minY - minPixelY;
		final int[] wLowX = this.wLowX;
		computeLowX(dx, dy);

		final int w0_row = wLowX[0];
		final int w1_row = wLowX[1];
		final int w2_row = wLowX[2];

		if ((w0_row | w1_row | w2_row
				| (w0_row + aLow[0]) | (w1_row + aLow[1]) | (w2_row + aLow[2])
				| (w0_row + bLow[0]) | (w1_row + bLow[1]) | (w2_row + bLow[2])
				| (w0_row + abLow[0]) | (w1_row + abLow[1]) | (w2_row + abLow[2])) >= 0) {

			System.out.println(String.format("L00: %d, %d, %d", w0_row, w1_row, w2_row));
			System.out.println(String.format("L10: %d, %d, %d", (w0_row + aLow[0]), (w1_row + aLow[1]), (w2_row + aLow[2])));
			System.out.println(String.format("L01: %d, %d, %d", (w0_row + bLow[0]), (w1_row + bLow[1]), (w2_row + bLow[2])));
			System.out.println(String.format("L11: %d, %d, %d", (w0_row + abLow[0]), (w1_row + abLow[1]), (w2_row + abLow[2])));
			lowTile.moveTo(minX >> LOW_AXIS_SHIFT, minY >> LOW_AXIS_SHIFT);
			lowTile.assertCovered();
			System.out.println();

			return true;
		} else {
			return false;
		}
	}

	@Override
	protected boolean testTri(int v0, int v1, int v2) {
		final int boundsResult  = prepareTriBounds(v0, v1, v2);

		if (boundsResult == BoundsResult.OUT_OF_BOUNDS) {
			return false;
		}

		if (boundsResult == BoundsResult.NEEDS_CLIP) {
			return testClippedLowX(v0, v1, v2);
		}

		prepareTriScan(v0, v1, v2);

		final int bx0 = (minPixelX >> TOP_AXIS_SHIFT);
		final int bx1 = (maxPixelX >> TOP_AXIS_SHIFT);
		final int by0 = (minPixelY >> TOP_AXIS_SHIFT);
		final int by1 = maxPixelY >> TOP_AXIS_SHIFT;

		if (bx0 == bx1 && by0 == by1) {
			return testTriTop(bx0, by0);
		} else {
			for (int by = by0; by <= by1; by++) {
				for (int bx = bx0; bx <= bx1; bx++) {
					if (testTriTop(bx, by)) {
						return true;
					}
				}
			}

			return false;
		}
	}

	private boolean testTriTop(final int topX, final int topY) {
		final int index = topIndex(topX, topY) << 1; // shift because two words per index
		final long wordFull = topBins[index + OFFSET_FULL];

		if (wordFull == -1L) {
			// bin fully occluded
			return false;
		}

		final long wordPartial = topBins[index + OFFSET_PARTIAL];

		if (wordPartial == 0) {
			// bin has no occlusion
			return true;
		}

		final int binOriginX = topX << TOP_AXIS_SHIFT;
		final int binOriginY = topY << TOP_AXIS_SHIFT;
		final int minPixelX = this.minPixelX;
		final int minPixelY = this.minPixelY;

		final int minX = minPixelX < binOriginX ? binOriginX : minPixelX;
		final int minY = minPixelY < binOriginY ? binOriginY : minPixelY;
		final int maxX = Math.min(maxPixelX, binOriginX + TOP_BIN_PIXEL_DIAMETER - 1);
		final int maxY = Math.min(maxPixelY, binOriginY + TOP_BIN_PIXEL_DIAMETER - 1);

		long coverage = coverageMask((minX >> MID_AXIS_SHIFT) & 7, (minY >> MID_AXIS_SHIFT) & 7,
				(maxX >> MID_AXIS_SHIFT) & 7, (maxY >> MID_AXIS_SHIFT) & 7);

		coverage &= ~wordFull;

		if (coverage == 0) {
			// all bins fully occluded
			return false;
		}

		if ((coverage & wordPartial) == 0) {
			// no bins are occluded
			return true;
		}

		final int baseX = topX << BIN_AXIS_SHIFT;
		final int baseY = topY << BIN_AXIS_SHIFT;

		long mask = 1;


		for (int n = 0; n < 64; ++n) {
			if ((mask & coverage) != 0 && testTriMid(baseX + (n & 7), baseY + (n >> 3))) {
				return true;
			}

			mask <<= 1;
		}

		return false;
	}

	/**
	 * Returns true when bin fully occluded
	 */
	private boolean testTriMid(final int midX, final int midY) {
		final int index = midIndex(midX, midY) << 1; // shift because two words per index
		final long wordFull = midBins[index + OFFSET_FULL];
		final long wordPartial = midBins[index + OFFSET_PARTIAL];

		final int binOriginX = midX << MID_AXIS_SHIFT;
		final int binOriginY = midY << MID_AXIS_SHIFT;
		final int minPixelX = this.minPixelX;
		final int minPixelY = this.minPixelY;

		final int minX = minPixelX < binOriginX ? binOriginX : minPixelX;
		final int minY = minPixelY < binOriginY ? binOriginY : minPixelY;
		final int maxX = Math.min(maxPixelX, binOriginX + MID_BIN_PIXEL_DIAMETER - 1);
		final int maxY = Math.min(maxPixelY, binOriginY + MID_BIN_PIXEL_DIAMETER - 1);

		final int lowX0 = minX >>> LOW_AXIS_SHIFT;
		final int lowX1 = maxX >>> LOW_AXIS_SHIFT;
		final int lowY0 = (minY >>> LOW_AXIS_SHIFT);
		final int lowY1 = maxY >>> LOW_AXIS_SHIFT;

		if (lowX0 == lowX1)  {
			if (lowY0 == lowY1) {
				// single  bin
				final long mask = pixelMask(lowX0, lowY0);
				return (wordPartial & mask) == 0 || ((wordFull & mask) == 0 && testTriLow(lowX0, lowY0));
			} else {
				// single column
				long mask = pixelMask(lowX0, lowY0);

				for (int y = lowY0; y <= lowY1; ++y) {
					if ((wordPartial & mask) == 0 || ((wordFull & mask) == 0 && testTriLow(lowX0, y))) {
						return true;
					}

					mask  <<= 8;
				}

				return false;
			}

		} else if (lowY0 == lowY1) {
			// single row
			long mask = pixelMask(lowX0, lowY0);

			for (int x = lowX0; x <= lowX1; ++x) {
				if ((wordPartial & mask) == 0 || ((wordFull & mask) == 0 && testTriLow(x, lowY0))) {
					return true;
				}

				mask  <<= 1;
			}

			return false;
		}

		long coverage = coverageMask(lowX0 & 7, lowY0 & 7, lowX1 & 7, lowY1 & 7);

		coverage &= ~wordFull;

		if (coverage == 0) {
			return false;
		}

		if ((coverage & wordPartial) == 0) {
			return true;
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

	private boolean testTriLow(int lowX, int lowY) {
		final int index = lowIndex(lowX, lowY);

		final int binOriginX = lowX << LOW_AXIS_SHIFT;
		final int binOriginY = lowY << LOW_AXIS_SHIFT;
		final int minPixelX = this.minPixelX;
		final int minPixelY = this.minPixelY;

		final int minX = minPixelX < binOriginX ? binOriginX : minPixelX;
		final int minY = minPixelY < binOriginY ? binOriginY : minPixelY;
		final int maxX = Math.min(maxPixelX, binOriginX + LOW_BIN_PIXEL_DIAMETER - 1);
		final int maxY = Math.min(maxPixelY, binOriginY + LOW_BIN_PIXEL_DIAMETER - 1);

		long coverage = coverageMask(minX & 7, minY & 7, maxX & 7, maxY & 7);

		final long word = lowBins[index];
		coverage &= ~word;

		if (coverage == 0L) {
			// TODO: remove
			//earlyExit++;
			return false;
		}

		final int a0 = a[0];
		final int b0 = b[0];
		final int a1 = a[1];
		final int b1 = b[1];
		final int a2 = a[2];
		final int b2 = b[2];
		final int x0 = minX & LOW_BIN_PIXEL_INDEX_MASK;
		final int x1 = maxX & LOW_BIN_PIXEL_INDEX_MASK;
		final int y0 = minY & LOW_BIN_PIXEL_INDEX_MASK;
		final int y1 = maxY & LOW_BIN_PIXEL_INDEX_MASK;
		final int dx = minX - minPixelX;
		final int dy = minY - minPixelY;

		computeRow(dx, dy);
		int w0_row = wRow[0];
		int w1_row = wRow[1];
		int w2_row = wRow[2];
		//		int w0_row = wOrigin0 + dx * a0 + dy * b0;
		//		int w1_row = wOrigin1 + dx * a1 + dy * b1;
		//		int w2_row = wOrigin2 + dx * a2 + dy * b2;

		// handle single pixel case
		if (x0 == x1 && y0 == y1) {
			return  testPixelInWordPreMasked(word, x0, y0)  && (w0_row | w1_row | w2_row) >= 0;
		}

		for (int y = y0; y <= y1; y++) {
			int w0 = w0_row;
			int w1 = w1_row;
			int w2 = w2_row;
			long mask = 1L << ((y << BIN_AXIS_SHIFT) | x0);

			for (int x = x0; x <= x1; x++) {
				if ((word & mask) == 0 && (w0 | w1 | w2) >= 0) {
					return true;
				}

				// One step to the right
				w0 += a0;
				w1 += a1;
				w2 += a2;
				mask <<= 1;
			}

			// One row step
			w0_row += b0;
			w1_row += b1;
			w2_row += b2;
		}

		return false;
	}

	private static final int COVERAGE_NONE = 0;
	private static final int COVERAGE_PARTIAL = 1;
	// 8 bits away from partial coverage so partial and full results can be accumulated in one word and combined with their respective masks
	private static final int COVERAGE_FULL = 1 << 8;
	private static final int OFFSET_FULL = 0;
	private static final int OFFSET_PARTIAL = 1;


	private abstract class AbstractTile {
		// all coordinates are full precision and corner-oriented unless otherwise noted
		protected final int[] tileData = new int[18];

		protected static final int CORNER_X0_Y0_E0 = 0;
		protected static final int CORNER_X0_Y0_E1 = 1;
		protected static final int CORNER_X0_Y0_E2 = 2;

		protected static final int CORNER_X1_Y0_E0 = 3;
		protected static final int CORNER_X1_Y0_E1 = 4;
		protected static final int CORNER_X1_Y0_E2 = 5;

		protected static final int CORNER_X0_Y1_E0 = 6;
		protected static final int CORNER_X0_Y1_E1 = 7;
		protected static final int CORNER_X0_Y1_E2 = 8;

		protected static final int CORNER_X1_Y1_E0 = 9;
		protected static final int CORNER_X1_Y1_E1 = 10;
		protected static final int CORNER_X1_Y1_E2 = 11;

		protected static final int SPAN_A0 = 12;
		protected static final int SPAN_A1 = 13;
		protected static final int SPAN_A2 = 14;

		protected static final int SPAN_B0 = 15;
		protected static final int SPAN_B1 = 16;
		protected static final int SPAN_B2 = 17;

		protected abstract void computeSpan();
	}

	private class LowTile extends AbstractTile {
		// PERF: accept/preserve one or more corners pre-computed
		// PERF: add moveDown/Left/Right/Up
		// PERF: make corner compute lazy
		// PERF: compute base a/b based on origin or snapped corner to avoid dx/dy computation

		protected int lowX, lowY;

		public void moveTo(int lowX, int lowY) {
			final int binOriginX = lowX << LOW_AXIS_SHIFT;
			final int binOriginY = lowY << LOW_AXIS_SHIFT;
			final int[] tileData = this.tileData;
			this.lowX = lowX;
			this.lowY = lowY;

			final int dx = binOriginX - minPixelX;
			final int dy = binOriginY - minPixelY;

			tileData[CORNER_X0_Y0_E0] = cornerOrigin[0] + a[0] * dx + b[0] * dy;
			tileData[CORNER_X0_Y0_E1] = cornerOrigin[1] + a[1] * dx + b[1] * dy;
			tileData[CORNER_X0_Y0_E2] = cornerOrigin[2] + a[2] * dx + b[2] * dy;

			tileData[CORNER_X1_Y0_E0] = tileData[CORNER_X0_Y0_E0] + tileData[SPAN_A0];
			tileData[CORNER_X1_Y0_E1] = tileData[CORNER_X0_Y0_E1] + tileData[SPAN_A1];
			tileData[CORNER_X1_Y0_E2] = tileData[CORNER_X0_Y0_E2] + tileData[SPAN_A2];

			tileData[CORNER_X0_Y1_E0] = tileData[CORNER_X0_Y0_E0] + tileData[SPAN_B0];
			tileData[CORNER_X0_Y1_E1] = tileData[CORNER_X0_Y0_E1] + tileData[SPAN_B1];
			tileData[CORNER_X0_Y1_E2] = tileData[CORNER_X0_Y0_E2] + tileData[SPAN_B2];

			tileData[CORNER_X1_Y1_E0] = tileData[CORNER_X0_Y1_E0] + tileData[SPAN_A0];
			tileData[CORNER_X1_Y1_E1] = tileData[CORNER_X0_Y1_E1] + tileData[SPAN_A1];
			tileData[CORNER_X1_Y1_E2] = tileData[CORNER_X0_Y1_E2] + tileData[SPAN_A2];
		}

		public void assertCovered() {
			final int[] tileData = this.tileData;

			final int e00a = tileData[CORNER_X0_Y0_E0];
			final int e00b = tileData[CORNER_X0_Y0_E1];
			final int e00c = tileData[CORNER_X0_Y0_E2];

			final int e01a = tileData[CORNER_X0_Y1_E0];
			final int e01b = tileData[CORNER_X0_Y1_E1];
			final int e01c = tileData[CORNER_X0_Y1_E2];

			final int e10a = tileData[CORNER_X1_Y0_E0];
			final int e10b = tileData[CORNER_X1_Y0_E1];
			final int e10c = tileData[CORNER_X1_Y0_E2];

			final int e11a = tileData[CORNER_X1_Y1_E0];
			final int e11b = tileData[CORNER_X1_Y1_E1];
			final int e11c = tileData[CORNER_X1_Y1_E2];

			System.out.println();
			System.out.println(String.format("E00: %d, %d, %d", e00a, e00b, e00c));
			System.out.println(String.format("E10: %d, %d, %d", e10a, e10b, e10c));
			System.out.println(String.format("E01: %d, %d, %d", e01a, e01b, e01c));
			System.out.println(String.format("E11: %d, %d, %d", e11a, e11b, e11c));

			final int ef = edgeFlags;
			final int e0 = ef & EDGE_MASK;
			final int e1 = (ef >> EDGE_SHIFT_1) & EDGE_MASK;
			final int e2 = (ef >> EDGE_SHIFT_2);

			final int a0 = a[0] / 2;
			final int a1 = a[1] / 2;
			final int a2 = a[2] / 2;
			final int b0 = b[0] / 2;
			final int b1 = b[1] / 2;
			final int b2 = b[2] / 2;

			final int w00a = (tileData[CORNER_X0_Y0_E0] + a0 + b0);
			final int w00b = (tileData[CORNER_X0_Y0_E1] + a1 + b1);
			final int w00c = (tileData[CORNER_X0_Y0_E2] + a2 + b2);

			final int w01a = (tileData[CORNER_X0_Y1_E0] + a0 - b0);
			final int w01b = (tileData[CORNER_X0_Y1_E1] + a1 - b1);
			final int w01c = (tileData[CORNER_X0_Y1_E2] + a2 - b2);

			final int w10a = (tileData[CORNER_X1_Y0_E0] - a0 + b0);
			final int w10b = (tileData[CORNER_X1_Y0_E1] - a1 + b1);
			final int w10c = (tileData[CORNER_X1_Y0_E2] - a2 + b2);

			final int w11a = (tileData[CORNER_X1_Y1_E0] - a0 - b0);
			final int w11b = (tileData[CORNER_X1_Y1_E1] - a1 - b1);
			final int w11c = (tileData[CORNER_X1_Y1_E2] - a2 - b2);

			System.out.println();
			System.out.println(String.format("P00: %d, %d, %d", w00a, w00b, w00c));
			System.out.println(String.format("P10: %d, %d, %d", w10a, w10b, w10c));
			System.out.println(String.format("P01: %d, %d, %d", w01a, w01b, w01c));
			System.out.println(String.format("P11: %d, %d, %d", w11a, w11b, w11c));
			System.out.println();

			int d0 = Integer.MIN_VALUE, d1 = Integer.MIN_VALUE, d2 = Integer.MIN_VALUE;


			switch  (e0) {
			case EDGE_TOP:
			case EDGE_TOP_LEFT:
			case EDGE_LEFT:
				d0 = e01a;
				break;
			case EDGE_BOTTOM_LEFT:
				d0 = e00a;
				break;
			case EDGE_TOP_RIGHT:
				d0 = e11a;
				break;
			case EDGE_BOTTOM:
			case EDGE_RIGHT:
			case EDGE_BOTTOM_RIGHT:
				d0 = e10a;
			}

			switch  (e1) {
			case EDGE_TOP:
			case EDGE_TOP_LEFT:
			case EDGE_LEFT:
				d1 = e01b;
				break;
			case EDGE_BOTTOM_LEFT:
				d1 = e00b;
				break;
			case EDGE_TOP_RIGHT:
				d1 = e11b;
				break;
			case EDGE_BOTTOM:
			case EDGE_RIGHT:
			case EDGE_BOTTOM_RIGHT:
				d1 = e10b;
			}

			switch  (e2) {
			case EDGE_TOP:
			case EDGE_TOP_LEFT:
			case EDGE_LEFT:
				d2 = e01c;
				break;
			case EDGE_BOTTOM_LEFT:
				d2 = e00c;
				break;
			case EDGE_TOP_RIGHT:
				d2 = e11c;
				break;
			case EDGE_BOTTOM:
			case EDGE_RIGHT:
			case EDGE_BOTTOM_RIGHT:
				d2 = e10c;
			}

			final float x0 = vertexData[v0 + PV_PX] / 16f;
			final float y0 = vertexData[v0 + PV_PY] / 16f;
			final float x1 = vertexData[v1 + PV_PX] / 16f;
			final float y1 = vertexData[v1 + PV_PY] / 16f;
			final float x2 = vertexData[v2 + PV_PX] / 16f;
			final float y2 = vertexData[v2 + PV_PY] / 16f;

			System.out.println(String.format("Points: %f\t%f\t%f\t%f\t%f\t%f", x0, y0, x1, y1, x2, y2));
			System.out.println(String.format("A,B: (%d, %d)  (%d, %d)  (%d, %d)", a[0], b[0], a[1], b[1], a[2], b[2]));
			System.out.println(String.format("Edges: %d, %d, %d", e0, e1, e2));
			System.out.println(String.format("D: %d, %d, %d", d0, d1, d2));
			System.out.println(String.format("origin: (%d, %d)", lowX << LOW_AXIS_SHIFT,  lowY << LOW_AXIS_SHIFT));

			if ((w00a | w00b | w00c | w01a | w01b | w01c | w10a | w10b | w10c | w11a | w11b | w11c) < 0) {
				System.out.println("boop");
			}
		}

		@Override
		protected void computeSpan() {
			final int[] tileData = this.tileData;
			tileData[SPAN_A0] = a[0] * LOW_BIN_PIXEL_DIAMETER;
			tileData[SPAN_A1] = a[1] * LOW_BIN_PIXEL_DIAMETER;
			tileData[SPAN_A2] = a[2] * LOW_BIN_PIXEL_DIAMETER;
			tileData[SPAN_B0] = b[0] * LOW_BIN_PIXEL_DIAMETER;
			tileData[SPAN_B1] = b[1] * LOW_BIN_PIXEL_DIAMETER;
			tileData[SPAN_B2] = b[2] * LOW_BIN_PIXEL_DIAMETER;
		}

		/**
		 *
		 * edge functions are line equations: ax + by + c = 0 where c is the origin value
		 * a and b are normal to the line/edge
		 */
	}
}
