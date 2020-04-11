package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PX;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PY;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;

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
	private final MidTile midTile = new MidTile();

	private void prepareTriMidA() {
		for (int i = 0; i < 3; ++i) {
			aMid[i] = a[i] * MID_BIN_PIXEL_DIAMETER_VECTOR[i];
		}
	}

	private void prepareTriMidB() {
		for (int i = 0; i < 3; ++i) {
			bMid[i] = b[i] * MID_BIN_PIXEL_DIAMETER_VECTOR[i];
		}
	}

	private void prepareTriMidAB() {
		for (int i = 0; i < 3; ++i) {
			abMid[i] = aMid[i] + bMid[i];
		}
	}

	@Override
	protected void prepareTriScan(int v0, int v1, int v2) {
		super.prepareTriScan(v0, v1, v2);
		prepareTriMidA();
		prepareTriMidB();
		prepareTriMidAB();

		lowTile.computeSpan();
		midTile.computeSpan();
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

	private boolean debugMid = false;

	@SuppressWarnings("unused")
	private int drawTriMidWithCompare(final int midX, final int midY) {
		final int index = midIndex(midX, midY) << 1; // shift because two words per index
		final long inputWordFull = midBins[index + OFFSET_FULL];
		final long inputWordPartial = midBins[index + OFFSET_PARTIAL];

		final int oldResult = drawTriMidOld(midX, midY);
		final long oldWordFull = midBins[index + OFFSET_FULL];
		final long oldWordPartial = midBins[index + OFFSET_PARTIAL];
		midBins[index + OFFSET_FULL] = inputWordFull;
		midBins[index + OFFSET_PARTIAL] = inputWordPartial;

		final int newResult = drawTriMid(midX, midY);
		final long newWordFull = midBins[index + OFFSET_FULL];
		final long newWordPartial = midBins[index + OFFSET_PARTIAL];

		if (oldWordFull != newWordFull || oldWordPartial != newWordPartial) { // || newResult != oldResult) {
			System.out.println("OLD FULL RESULT: " + oldResult);
			printMask8x8(oldWordFull);
			System.out.println("OLD PARTIAL RESULT");
			printMask8x8(oldWordPartial);
			System.out.println();

			System.out.println("NEW FULL RESULT: " + newResult);
			printMask8x8(newWordFull);
			System.out.println("NEW PARTIAL RESULT");
			printMask8x8(newWordPartial);
			System.out.println();

			debugMid = true;
			midBins[index + OFFSET_FULL] = inputWordFull;
			midBins[index + OFFSET_PARTIAL] = inputWordPartial;
			drawTriMid(midX, midY);
			debugMid = false;
			drawTriMid(midX, midY);

			System.out.println();
		}

		return newResult;
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

	/**
	 * Returns true when bin fully occluded
	 */
	private int drawTriMidOld(final int midX, final int midY) {
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
			return midBins[index + OFFSET_PARTIAL] == 0 ? COVERAGE_NONE : COVERAGE_PARTIAL;
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

	private boolean debugLow = false;

	// TODO: remove
	@SuppressWarnings("unused")
	private int drawTriLowWithCompare(int lowX, int lowY) {
		final int index = lowIndex(lowX, lowY);
		final long inputWord  =  lowBins[index];

		final int oldResult = drawTriLowOld(lowX, lowY);
		final long oldWord = lowBins[index];
		lowBins[index] = inputWord;

		final int newResult = drawTriLow(lowX, lowY);
		final long newWord = lowBins[index];

		if (newWord != oldWord) { // || newResult != oldResult) {
			System.out.println("OLD RESULT: " + oldResult);
			printMask8x8(oldWord);
			System.out.println();
			System.out.println("NEW RESULT: " + newResult);
			printMask8x8(newWord);
			System.out.println();

			debugLow = true;
			lowBins[index] = inputWord;
			drawTriLow(lowX, lowY);
			debugLow = false;

			System.out.println();
		}

		return newResult;
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

	private int drawTriLowOld(int lowX, int lowY) {
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
			return 1;
		}

		long word = lowBins[index];
		coverage &= ~word;

		if (coverage == 0L) {
			return 0;
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
		return word == -1L ? 1 : 0;
	}

	private boolean isTriLowCovered(int minX, int minY) {
		final int dx = minX - minPixelX;
		final int dy = minY - minPixelY;
		final int[] wLowX = this.wLowX;
		computeLowX(dx, dy);

		final int w0_row = wLowX[0];
		final int w1_row = wLowX[1];
		final int w2_row = wLowX[2];
		lowTile.moveTo(minX >> LOW_AXIS_SHIFT, minY >> LOW_AXIS_SHIFT);

		return ((w0_row | w1_row | w2_row
				| (w0_row + aLow[0]) | (w1_row + aLow[1]) | (w2_row + aLow[2])
				| (w0_row + bLow[0]) | (w1_row + bLow[1]) | (w2_row + bLow[2])
				| (w0_row + abLow[0]) | (w1_row + abLow[1]) | (w2_row + abLow[2])) >= 0);
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

	@SuppressWarnings("unused")
	private boolean testTriMidWithCompare(final int midX, final int midY) {
		final boolean oldResult = testTriMidOld(midX,  midY);
		final boolean newResult = testTriMid(midX,  midY);

		if (newResult != oldResult) {
			final int index = midIndex(midX, midY) << 1; // shift because two words per index
			System.out.println("INPUT WORD - FULL: " + oldResult);
			printMask8x8(midBins[index + OFFSET_FULL]);
			System.out.println("INPUT WORD - PARTIAL: " + oldResult);
			printMask8x8(midBins[index + OFFSET_PARTIAL]);
			System.out.println();

			debugMid = true;
			testTriMid(midX,  midY);
			debugMid = false;

			testTriMid(midX,  midY);
			System.out.println();
		}

		return newResult;
	}


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


		// don't check empty tiles
		coverage &= ~midBins[index + OFFSET_PARTIAL];

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

	private boolean testTriMidOld(final int midX, final int midY) {
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

	// TODO: remove
	@SuppressWarnings("unused")
	private boolean testTriLowWithCompare(int lowX, int lowY) {
		final int index = lowIndex(lowX, lowY);
		final long inputWord  =  lowBins[index];

		final boolean oldResult = testTriLowOld(lowX, lowY);
		final boolean newResult = testTriLow(lowX, lowY);

		if (newResult != oldResult) {
			System.out.println("INPUT WORD: " + oldResult);
			printMask8x8(inputWord);
			System.out.println();

			debugLow = true;
			testTriLow(lowX, lowY);
			debugLow = false;

			System.out.println();
		}

		return newResult;
	}

	private boolean testTriLow(int lowX, int lowY) {
		lowTile.moveTo(lowX, lowY);
		final long coverage = lowTile.computeCoverage();
		final long word =  lowBins[lowIndex(lowX, lowY)];
		return (~word & coverage) != 0;
	}

	private boolean testTriLowOld(int lowX, int lowY) {
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


	protected static final int TITLE_SIZE = 29;
	protected static final int TILE_LOW_START = 0;
	protected static final int TILE_MID_START = TILE_LOW_START + TITLE_SIZE;

	// all coordinates are full precision and corner-oriented unless otherwise noted
	protected final int[] tileData = new int[TITLE_SIZE * 2];

	private abstract class AbstractTile {
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

		protected static final int STEP_A0 = 12;
		protected static final int STEP_A1 = 13;
		protected static final int STEP_A2 = 14;

		protected static final int STEP_B0 = 15;
		protected static final int STEP_B1 = 16;
		protected static final int STEP_B2 = 17;

		protected static final int SPAN_A0 = 18;
		protected static final int SPAN_A1 = 19;
		protected static final int SPAN_A2 = 20;

		protected static final int SPAN_B0 = 21;
		protected static final int SPAN_B1 = 22;
		protected static final int SPAN_B2 = 23;

		protected static final int EXTENT_0 = 24;
		protected static final int EXTENT_1 = 25;
		protected static final int EXTENT_2 = 26;

		protected static final int BIN_ORIGIN_X = 27;
		protected static final int BIN_ORIGIN_Y = 28;

		protected abstract void computeSpan();

		protected static final int INSIDE = -1;
		protected static final int OUTSIDE = 0;
		protected static final int INTERSECTING = 1;

		protected final int classify(int edgeFlag, int edgeIndex) {
			final int w = chooseEdgeValue(edgeFlag, edgeIndex);
			//NB extent is always negative
			final int extent = tileData[EXTENT_0 + edgeIndex];

			if (w < extent) {
				// fully outside edge
				return OUTSIDE;
			} else if (w >= 0) {
				// fully inside or touching edge
				return INSIDE;
			} else {
				// intersecting - at least one pixel is set
				return INTERSECTING;
			}
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

		protected final long buildMask(int edgeFlag, int edgeIndex) {
			final int[] tileData = TerrainOccluder.this.tileData;
			final int a = tileData[STEP_A0 + edgeIndex];
			final int b = tileData[STEP_B0 + edgeIndex];

			switch  (edgeFlag) {
			case EDGE_TOP: {
				int wy = tileData[CORNER_X0_Y0_E0 + edgeIndex]; // bottom left will always be inside
				assert wy >= 0;
				assert b < 0;

				long yMask = 0xFFL;
				long mask = 0;

				while (wy >= 0 && yMask != 0L) {
					mask |= yMask;
					yMask <<= 8;
					wy += b; //NB: b will be negative
				}

				//				System.out.println("TOP");
				//				printMask8x8(mask);

				return mask;
			}

			case EDGE_BOTTOM: {
				int wy = tileData[CORNER_X0_Y1_E0 + edgeIndex]; // top left will always be inside
				assert wy >= 0;
				assert b > 0;

				long yMask = 0xFF00000000000000L;
				long mask = 0;

				while (wy >= 0 && yMask != 0L) {
					mask |= yMask;
					yMask = (yMask >>> 8); // parens are to help eclipse auto-formatting
					wy -= b;
				}

				//				System.out.println("BOTTOM");
				//				printMask8x8(mask);

				return mask;
			}

			case EDGE_RIGHT: {
				final int wy = tileData[CORNER_X0_Y0_E0 + edgeIndex]; // bottom left will always be inside
				assert wy >= 0;
				assert a < 0;

				final int x = 7 - Math.min(7, -wy / a);
				long mask = (0xFF >> x);

				mask |= mask << 8;
				mask |= mask << 16;
				mask |= mask << 32;

				//				System.out.println("RIGHT");
				//				printMask8x8(mask);

				return mask;
			}

			case EDGE_LEFT: {
				final int wy = tileData[CORNER_X1_Y0_E0 + edgeIndex]; // bottom right will always be inside
				assert wy >= 0;
				assert a > 0;

				final int x =  7 - Math.min(7, wy / a);
				long mask = (0xFF << x) & 0xFF;

				mask |= mask << 8;
				mask |= mask << 16;
				mask |= mask << 32;

				//				System.out.println("LEFT");
				//				printMask8x8(mask);

				return mask;
			}

			case EDGE_TOP_LEFT: {
				// PERF: optimize case when shallow slope and several bottom rows are full

				int wy = tileData[CORNER_X1_Y0_E0 + edgeIndex]; // bottom right will always be inside
				assert wy >= 0;
				assert b < 0;
				assert a > 0;

				// min y will occur at x = 0;

				long mask = 0;
				int yShift = 0;

				while (yShift < 64 && wy >= 0) {
					// x  here is first not last
					final int x =  7 - Math.min(7, wy / a);
					final int yMask = (0xFF << x) & 0xFF;
					mask |= ((long) yMask) << yShift;
					wy += b; //NB: b will be negative
					yShift += 8;
				}

				//				System.out.println("TOP LEFT");
				//				printMask8x8(mask);

				return mask;
			}

			case EDGE_BOTTOM_LEFT: {
				int wy = tileData[CORNER_X1_Y1_E0 + edgeIndex]; // top right will always be inside
				assert wy >= 0;
				assert b > 0;
				assert a > 0;

				// min y will occur at x = 7;

				int yShift = 8 * 7;
				long mask = 0;

				while (yShift >= 0 && wy >= 0) {
					// x  here is first not last
					final int x =  7 - Math.min(7, wy / a);
					final int yMask = (0xFF << x) & 0xFF;
					mask |= ((long) yMask) << yShift;
					wy -= b;
					yShift -= 8;
				}

				//				System.out.println("BOTTOM LEFT");
				//				printMask8x8(mask);

				return mask;
			}

			case EDGE_TOP_RIGHT: {
				// PERF: optimize case when shallow slope and several bottom rows are full

				// max y will occur at x = 0
				// Find highest y index of pixels filled at given x.
				// All pixels with lower y value will also be filled in given x.
				// ax + by + c = 0 so y at intersection will be y = -(ax + c) / b
				// Exploit step-wise nature of a/b here to avoid computing the first term
				// logic in other cases is similar
				int wy = tileData[CORNER_X0_Y0_E0 + edgeIndex]; // bottom left will always be inside
				assert wy >= 0;
				assert b < 0;
				assert a < 0;

				long mask = 0;
				int yShift = 0;

				while(yShift < 64 && wy >= 0) {
					final int x =  7  - Math.min(7, -wy / a);
					final int yMask = (0xFF >> x);
					mask |= ((long) yMask) << yShift;
					wy += b;
					yShift +=  8;
				}

				//				System.out.println("TOP RIGHT");
				//				printMask8x8(mask);

				return mask;
			}

			case EDGE_BOTTOM_RIGHT: {
				// PERF: optimize case when shallow slope and several top rows are full

				int wy = tileData[CORNER_X0_Y1_E0 + edgeIndex]; // top left will always be inside
				assert wy >= 0;
				assert b > 0;
				assert a < 0;

				int yShift = 8 * 7;
				long mask = 0;

				while (yShift >= 0 && wy >= 0) {
					final int x = 7 - Math.min(7, -wy / a);
					final int yMask = (0xFF >> x);
					mask |= ((long) yMask) << yShift;
					wy -= b;
					yShift -= 8;
				}

				//				System.out.println("BOTTOM RIGHT");
				//				printMask8x8(mask);

				return mask;
			}

			default:
				assert false : "Edge flag out of bounds.";
			return 0L;
			}
		}
		protected final int chooseEdgeValue(int edgeFlag, int edgeIndex) {
			switch  (edgeFlag) {
			case EDGE_TOP:
			case EDGE_TOP_LEFT:
			case EDGE_LEFT:
				return tileData[CORNER_X0_Y1_E0 + edgeIndex];

			case EDGE_BOTTOM_LEFT:
				return tileData[CORNER_X0_Y0_E0 + edgeIndex];

			case EDGE_TOP_RIGHT:
				return tileData[CORNER_X1_Y1_E0 + edgeIndex];

			case EDGE_BOTTOM:
			case EDGE_RIGHT:
			case EDGE_BOTTOM_RIGHT:
				return tileData[CORNER_X1_Y0_E0 + edgeIndex];

			default:
				assert false : "Edge flag out of bounds.";
			return -1;
			}
		}
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
			final int[] tileData = TerrainOccluder.this.tileData;

			this.lowX = lowX;
			this.lowY = lowY;

			final int dx = binOriginX - minPixelX;
			final int dy = binOriginY - minPixelY;

			tileData[CORNER_X0_Y0_E0 + TILE_LOW_START] = wOrigin[0] + a[0] * dx + b[0] * dy;
			tileData[CORNER_X0_Y0_E1 + TILE_LOW_START] = wOrigin[1] + a[1] * dx + b[1] * dy;
			tileData[CORNER_X0_Y0_E2 + TILE_LOW_START] = wOrigin[2] + a[2] * dx + b[2] * dy;

			tileData[CORNER_X1_Y0_E0 + TILE_LOW_START] = tileData[CORNER_X0_Y0_E0 + TILE_LOW_START] + tileData[SPAN_A0 + TILE_LOW_START];
			tileData[CORNER_X1_Y0_E1 + TILE_LOW_START] = tileData[CORNER_X0_Y0_E1 + TILE_LOW_START] + tileData[SPAN_A1 + TILE_LOW_START];
			tileData[CORNER_X1_Y0_E2 + TILE_LOW_START] = tileData[CORNER_X0_Y0_E2 + TILE_LOW_START] + tileData[SPAN_A2 + TILE_LOW_START];

			tileData[CORNER_X0_Y1_E0 + TILE_LOW_START] = tileData[CORNER_X0_Y0_E0 + TILE_LOW_START] + tileData[SPAN_B0 + TILE_LOW_START];
			tileData[CORNER_X0_Y1_E1 + TILE_LOW_START] = tileData[CORNER_X0_Y0_E1 + TILE_LOW_START] + tileData[SPAN_B1 + TILE_LOW_START];
			tileData[CORNER_X0_Y1_E2 + TILE_LOW_START] = tileData[CORNER_X0_Y0_E2 + TILE_LOW_START] + tileData[SPAN_B2 + TILE_LOW_START];

			tileData[CORNER_X1_Y1_E0 + TILE_LOW_START] = tileData[CORNER_X0_Y1_E0 + TILE_LOW_START] + tileData[SPAN_A0 + TILE_LOW_START];
			tileData[CORNER_X1_Y1_E1 + TILE_LOW_START] = tileData[CORNER_X0_Y1_E1 + TILE_LOW_START] + tileData[SPAN_A1 + TILE_LOW_START];
			tileData[CORNER_X1_Y1_E2 + TILE_LOW_START] = tileData[CORNER_X0_Y1_E2 + TILE_LOW_START] + tileData[SPAN_A2 + TILE_LOW_START];

			tileData[BIN_ORIGIN_X + TILE_LOW_START] = binOriginX;
			tileData[BIN_ORIGIN_Y + TILE_LOW_START] = binOriginY;
		}

		private long computeMask(int edgeFlag, int edgeIndex) {
			final int c = classify(edgeFlag, edgeIndex);
			return c == INTERSECTING ? buildMask(edgeFlag, edgeIndex) : c;
		}

		public long computeCoverage() {
			final int ef = edgeFlags;
			final int e0 = ef & EDGE_MASK;
			final int e1 = (ef >> EDGE_SHIFT_1) & EDGE_MASK;
			final int e2 = (ef >> EDGE_SHIFT_2);

			final long m0 = computeMask(e0, 0 + TILE_LOW_START);

			if (m0 == 0L) {
				return 0;
			}

			final long m1 = computeMask(e1, 1 + TILE_LOW_START);

			if (m1 == 0L) {
				return 0;
			}

			final long m2 = computeMask(e2, 2 + TILE_LOW_START);

			if  (debugLow) {
				System.out.println("E0 = " + e0);
				printMask8x8(m0);
				System.out.println();
				System.out.println("E1 = " + e1);
				printMask8x8(m1);
				System.out.println();
				System.out.println("E2 = " + e2);
				printMask8x8(m2);
				System.out.println();

				System.out.println("COMBINED");
				printMask8x8(m0 & m1 & m2);

				final float x0 = vertexData[v0 + PV_PX] / 16f;
				final float y0 = vertexData[v0 + PV_PY] / 16f;
				final float x1 = vertexData[v1 + PV_PX] / 16f;
				final float y1 = vertexData[v1 + PV_PY] / 16f;
				final float x2 = vertexData[v2 + PV_PX] / 16f;
				final float y2 = vertexData[v2 + PV_PY] / 16f;

				System.out.println();
				System.out.println(String.format("E00: %d, %d, %d", tileData[CORNER_X0_Y0_E0 + TILE_LOW_START], tileData[CORNER_X0_Y0_E1 + TILE_LOW_START], tileData[CORNER_X0_Y0_E2 + TILE_LOW_START]));
				System.out.println(String.format("E10: %d, %d, %d", tileData[CORNER_X1_Y0_E0 + TILE_LOW_START], tileData[CORNER_X1_Y0_E1 + TILE_LOW_START], tileData[CORNER_X1_Y0_E2 + TILE_LOW_START]));
				System.out.println(String.format("E01: %d, %d, %d", tileData[CORNER_X0_Y1_E0 + TILE_LOW_START], tileData[CORNER_X0_Y1_E1 + TILE_LOW_START], tileData[CORNER_X0_Y1_E2 + TILE_LOW_START]));
				System.out.println(String.format("E11: %d, %d, %d", tileData[CORNER_X1_Y1_E0 + TILE_LOW_START], tileData[CORNER_X1_Y1_E1 + TILE_LOW_START], tileData[CORNER_X1_Y1_E2 + TILE_LOW_START]));
				System.out.println();
				System.out.println(String.format("Points: %f\t%f\t%f\t%f\t%f\t%f", x0, y0, x1, y1, x2, y2));
				System.out.println(String.format("A,B: (%d, %d)  (%d, %d)  (%d, %d)", a[0], b[0], a[1], b[1], a[2], b[2]));
				System.out.println(String.format("Edges: %d, %d, %d", e0, e1, e2));
				System.out.println(String.format("origin: (%d, %d)", lowX << LOW_AXIS_SHIFT,  lowY << LOW_AXIS_SHIFT));
				System.out.println();
			}

			return m0 & m1 & m2;
		}

		/**
		 *
		 * Edge functions are line equations: ax + by + c = 0 where c is the origin value
		 * a and b are normal to the line/edge.
		 *
		 * Distance from point to line is given by (ax + by + c) / magnitude
		 * where magnitude is sqrt(a^2 + b^2).
		 *
		 * A tile is fully outside the edge if signed distance less than -extent, where
		 * extent is the 7x7 diagonal vector projected onto the edge normal.
		 *
		 * The length of the extent is given by  (|a| + |b|) * 7 / magnitude.
		 *
		 * Given that magnitude is a common denominator of both the signed distance and the extent
		 * we can avoid computing square root and compare the weight directly with the un-normalized  extent.
		 *
		 * In summary,  if extent e = (|a| + |b|) * 7 and w = ax + by + c then
		 *    when w < -e  tile is fully outside edge
		 *    when w >= 0 tile is fully inside edge (or touching)
		 *    else (-e <= w < 0) tile is intersection (at least one pixel is covered.
		 *
		 * For background, see Real Time Rendering, 4th Ed.  Sec 23.1 on Rasterization, esp. Figure 23.3
		 */
		@Override
		protected void computeSpan() {
			final int[] tileData = TerrainOccluder.this.tileData;
			int i = a[0];
			int j = b[0];
			tileData[STEP_A0 + TILE_LOW_START] = i;
			tileData[STEP_B0 + TILE_LOW_START] = j;
			tileData[SPAN_A0 + TILE_LOW_START] = i * (LOW_BIN_PIXEL_DIAMETER - 1);
			tileData[SPAN_B0 + TILE_LOW_START] = j * (LOW_BIN_PIXEL_DIAMETER - 1);
			tileData[EXTENT_0 + TILE_LOW_START] = -Math.abs(tileData[SPAN_A0 + TILE_LOW_START]) - Math.abs(tileData[SPAN_B0 + TILE_LOW_START]);
			//tileData[EXTENT_0 + TILE_LOW_START] = (Math.abs(i) + Math.abs(j)) * -7;

			i = a[1];
			j = b[1];
			tileData[STEP_A1 + TILE_LOW_START] = i;
			tileData[STEP_B1 + TILE_LOW_START] = j;
			tileData[SPAN_A1 + TILE_LOW_START] = i * (LOW_BIN_PIXEL_DIAMETER - 1);
			tileData[SPAN_B1 + TILE_LOW_START] = j * (LOW_BIN_PIXEL_DIAMETER - 1);
			tileData[EXTENT_1 + TILE_LOW_START] = -Math.abs(tileData[SPAN_A1 + TILE_LOW_START]) - Math.abs(tileData[SPAN_B1 + TILE_LOW_START]);
			//			tileData[EXTENT_1 + TILE_LOW_START] = (Math.abs(i) + Math.abs(j)) * -7;

			i = a[2];
			j = b[2];
			tileData[STEP_A2 + TILE_LOW_START] = i;
			tileData[STEP_B2 + TILE_LOW_START] = j;
			tileData[SPAN_A2 + TILE_LOW_START] = i * (LOW_BIN_PIXEL_DIAMETER - 1);
			tileData[SPAN_B2 + TILE_LOW_START] = j * (LOW_BIN_PIXEL_DIAMETER - 1);
			tileData[EXTENT_2 + TILE_LOW_START] = -Math.abs(tileData[SPAN_A2 + TILE_LOW_START]) - Math.abs(tileData[SPAN_B2 + TILE_LOW_START]);
			//tileData[EXTENT_2 + TILE_LOW_START] = (Math.abs(i) + Math.abs(j)) * -7;

		}
	}

	private class MidTile extends AbstractTile {
		protected int midX, midY;

		protected long fullCoverage;

		public void moveTo(int midX, int midY) {
			final int binOriginX = midX << MID_AXIS_SHIFT;
			final int binOriginY = midY << MID_AXIS_SHIFT;
			final int[] tileData = TerrainOccluder.this.tileData;
			this.midX = midX;
			this.midY = midY;

			final int dx = binOriginX - minPixelX;
			final int dy = binOriginY - minPixelY;

			tileData[CORNER_X0_Y0_E0 + TILE_MID_START] = wOrigin[0] + a[0] * dx + b[0] * dy;
			tileData[CORNER_X0_Y0_E1 + TILE_MID_START] = wOrigin[1] + a[1] * dx + b[1] * dy;
			tileData[CORNER_X0_Y0_E2 + TILE_MID_START] = wOrigin[2] + a[2] * dx + b[2] * dy;

			tileData[CORNER_X1_Y0_E0 + TILE_MID_START] = tileData[CORNER_X0_Y0_E0 + TILE_MID_START] + tileData[SPAN_A0 + TILE_MID_START];
			tileData[CORNER_X1_Y0_E1 + TILE_MID_START] = tileData[CORNER_X0_Y0_E1 + TILE_MID_START] + tileData[SPAN_A1 + TILE_MID_START];
			tileData[CORNER_X1_Y0_E2 + TILE_MID_START] = tileData[CORNER_X0_Y0_E2 + TILE_MID_START] + tileData[SPAN_A2 + TILE_MID_START];

			tileData[CORNER_X0_Y1_E0 + TILE_MID_START] = tileData[CORNER_X0_Y0_E0 + TILE_MID_START] + tileData[SPAN_B0 + TILE_MID_START];
			tileData[CORNER_X0_Y1_E1 + TILE_MID_START] = tileData[CORNER_X0_Y0_E1 + TILE_MID_START] + tileData[SPAN_B1 + TILE_MID_START];
			tileData[CORNER_X0_Y1_E2 + TILE_MID_START] = tileData[CORNER_X0_Y0_E2 + TILE_MID_START] + tileData[SPAN_B2 + TILE_MID_START];

			tileData[CORNER_X1_Y1_E0 + TILE_MID_START] = tileData[CORNER_X0_Y1_E0 + TILE_MID_START] + tileData[SPAN_A0 + TILE_MID_START];
			tileData[CORNER_X1_Y1_E1 + TILE_MID_START] = tileData[CORNER_X0_Y1_E1 + TILE_MID_START] + tileData[SPAN_A1 + TILE_MID_START];
			tileData[CORNER_X1_Y1_E2 + TILE_MID_START] = tileData[CORNER_X0_Y1_E2 + TILE_MID_START] + tileData[SPAN_A2 + TILE_MID_START];

			tileData[BIN_ORIGIN_X + TILE_MID_START] = binOriginX;
			tileData[BIN_ORIGIN_Y + TILE_MID_START] = binOriginY;
		}


		/**
		 *
		 * @return mask that inclueds edge coverage.
		 */
		public long computeCoverage() {
			final int ef = edgeFlags;
			final int e0 = ef & EDGE_MASK;
			final int e1 = (ef >> EDGE_SHIFT_1) & EDGE_MASK;
			final int e2 = (ef >> EDGE_SHIFT_2);

			final long m0;
			int c = classify(e0, 0 + TILE_MID_START);

			if (c == INTERSECTING) {
				m0 = buildMask(e0, 0 + TILE_MID_START);
				fullCoverage = shiftMask(e0, m0);
			} else if (c == OUTSIDE){
				m0 = 0;
				fullCoverage = 0;
				//	return 0;
			} else {
				m0 = -1L;
				fullCoverage = -1L;
			}


			final long m1;
			c = classify(e1, 1 + TILE_MID_START);

			if (c == INTERSECTING) {
				m1 = buildMask(e1, 1 + TILE_MID_START);
				fullCoverage &= shiftMask(e1, m1);
			} else if (c == OUTSIDE){
				m1 = 0;
				fullCoverage = 0;
				//return 0;
			} else {
				m1 = -1L;
			}


			final long m2;
			c = classify(e2, 2 + TILE_MID_START);

			if (c == INTERSECTING) {
				m2 = buildMask(e2, 2 + TILE_MID_START);
				fullCoverage &= shiftMask(e2, m2);
			} else if (c == OUTSIDE){
				m2 = 0;
				fullCoverage = 0;
				//return 0;
			} else {
				m2 = -1L;
			}

			final long result = m0 & m1 & m2;

			if  (debugMid) {
				System.out.println("E0 = " + e0);
				printMask8x8(m0);
				System.out.println();
				System.out.println("E1 = " + e1);
				printMask8x8(m1);
				System.out.println();
				System.out.println("E2 = " + e2);
				printMask8x8(m2);
				System.out.println();

				System.out.println("COMBINED");
				printMask8x8(m0 & m1 & m2);

				final float x0 = vertexData[v0 + PV_PX] / 16f;
				final float y0 = vertexData[v0 + PV_PY] / 16f;
				final float x1 = vertexData[v1 + PV_PX] / 16f;
				final float y1 = vertexData[v1 + PV_PY] / 16f;
				final float x2 = vertexData[v2 + PV_PX] / 16f;
				final float y2 = vertexData[v2 + PV_PY] / 16f;

				System.out.println();
				System.out.println(String.format("E00: %d, %d, %d", tileData[CORNER_X0_Y0_E0 + TILE_MID_START], tileData[CORNER_X0_Y0_E1 + TILE_MID_START], tileData[CORNER_X0_Y0_E2 + TILE_MID_START]));
				System.out.println(String.format("E10: %d, %d, %d", tileData[CORNER_X1_Y0_E0 + TILE_MID_START], tileData[CORNER_X1_Y0_E1 + TILE_MID_START], tileData[CORNER_X1_Y0_E2 + TILE_MID_START]));
				System.out.println(String.format("E01: %d, %d, %d", tileData[CORNER_X0_Y1_E0 + TILE_MID_START], tileData[CORNER_X0_Y1_E1 + TILE_MID_START], tileData[CORNER_X0_Y1_E2 + TILE_MID_START]));
				System.out.println(String.format("E11: %d, %d, %d", tileData[CORNER_X1_Y1_E0 + TILE_MID_START], tileData[CORNER_X1_Y1_E1 + TILE_MID_START], tileData[CORNER_X1_Y1_E2 + TILE_MID_START]));
				System.out.println();
				System.out.println(String.format("Points: %f\t%f\t%f\t%f\t%f\t%f", x0, y0, x1, y1, x2, y2));
				System.out.println(String.format("A,B: (%d, %d)  (%d, %d)  (%d, %d)", a[0], b[0], a[1], b[1], a[2], b[2]));
				System.out.println(String.format("Edges: %d, %d, %d", e0, e1, e2));
				System.out.println(String.format("origin: (%d, %d)", midX << MID_AXIS_SHIFT,  midY << MID_AXIS_SHIFT));
				System.out.println();
			}

			return result;
		}

		/**
		 *
		 * Edge functions are line equations: ax + by + c = 0 where c is the origin value
		 * a and b are normal to the line/edge.
		 *
		 * Distance from point to line is given by (ax + by + c) / magnitude
		 * where magnitude is sqrt(a^2 + b^2).
		 *
		 * A tile is fully outside the edge if signed distance less than -extent, where
		 * extent is the 7x7 diagonal vector projected onto the edge normal.
		 *
		 * The length of the extent is given by  (|a| + |b|) * 7 / magnitude.
		 *
		 * Given that magnitude is a common denominator of both the signed distance and the extent
		 * we can avoid computing square root and compare the weight directly with the un-normalized  extent.
		 *
		 * In summary,  if extent e = (|a| + |b|) * 7 and w = ax + by + c then
		 *    when w < -e  tile is fully outside edge
		 *    when w >= 0 tile is fully inside edge (or touching)
		 *    else (-e <= w < 0) tile is intersection (at least one pixel is covered.
		 *
		 * For background, see Real Time Rendering, 4th Ed.  Sec 23.1 on Rasterization, esp. Figure 23.3
		 */
		@Override
		protected void computeSpan() {
			final int[] tileData = TerrainOccluder.this.tileData;
			int i = a[0];
			int j = b[0];
			tileData[STEP_A0 + TILE_MID_START] = i * 8;
			tileData[STEP_B0 + TILE_MID_START] = j * 8;
			tileData[SPAN_A0 + TILE_MID_START] = i * (MID_BIN_PIXEL_DIAMETER - 1);
			tileData[SPAN_B0 + TILE_MID_START] = j * (MID_BIN_PIXEL_DIAMETER - 1);
			tileData[EXTENT_0 + TILE_MID_START] = -Math.abs(tileData[SPAN_A0 + TILE_MID_START]) - Math.abs(tileData[SPAN_B0 + TILE_MID_START]);
			//tileData[EXTENT_0 + TILE_MID_START] = (Math.abs(i) + Math.abs(j)) * -7;

			i = a[1];
			j = b[1];
			tileData[STEP_A1 + TILE_MID_START] = i * 8;
			tileData[STEP_B1 + TILE_MID_START] = j * 8;
			tileData[SPAN_A1 + TILE_MID_START] = i * (MID_BIN_PIXEL_DIAMETER - 1);
			tileData[SPAN_B1 + TILE_MID_START] = j * (MID_BIN_PIXEL_DIAMETER - 1);
			tileData[EXTENT_1 + TILE_MID_START] = -Math.abs(tileData[SPAN_A1 + TILE_MID_START]) - Math.abs(tileData[SPAN_B1 + TILE_MID_START]);
			//tileData[EXTENT_1 + TILE_MID_START] = (Math.abs(i) + Math.abs(j)) * -7;

			i = a[2];
			j = b[2];
			tileData[STEP_A2 + TILE_MID_START] = i * 8;
			tileData[STEP_B2 + TILE_MID_START] = j * 8;
			tileData[SPAN_A2 + TILE_MID_START] = i * (MID_BIN_PIXEL_DIAMETER - 1);
			tileData[SPAN_B2 + TILE_MID_START] = j * (MID_BIN_PIXEL_DIAMETER - 1);
			tileData[EXTENT_2 + TILE_MID_START] = -Math.abs(tileData[SPAN_A2 + TILE_MID_START]) - Math.abs(tileData[SPAN_B2 + TILE_MID_START]);
			//			tileData[EXTENT_2 + TILE_MID_START] = (Math.abs(i) + Math.abs(j)) * -7;
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
