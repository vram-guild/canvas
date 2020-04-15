package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion._Clipper.drawQuad;
import static grondag.canvas.chunk.occlusion._Clipper.testQuad;
import static grondag.canvas.chunk.occlusion._Constants.CAMERA_PRECISION_BITS;
import static grondag.canvas.chunk.occlusion._Constants.CAMERA_PRECISION_CHUNK_MAX;
import static grondag.canvas.chunk.occlusion._Constants.CAMERA_PRECISION_UNITY;
import static grondag.canvas.chunk.occlusion._Constants.HALF_PIXEL_HEIGHT;
import static grondag.canvas.chunk.occlusion._Constants.HALF_PIXEL_WIDTH;
import static grondag.canvas.chunk.occlusion._Constants.LOW_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion._Constants.MID_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion._Constants.MID_INDEX_SHIFT;
import static grondag.canvas.chunk.occlusion._Constants.PIXEL_HEIGHT;
import static grondag.canvas.chunk.occlusion._Constants.PIXEL_WIDTH;
import static grondag.canvas.chunk.occlusion._Constants.TILE_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion._Constants.TILE_PIXEL_INDEX_MASK;
import static grondag.canvas.chunk.occlusion._Constants.TOP_INDEX_SHIFT;
import static grondag.canvas.chunk.occlusion._Constants.TOP_Y_SHIFT;
import static grondag.canvas.chunk.occlusion._Data.V000;
import static grondag.canvas.chunk.occlusion._Data.V001;
import static grondag.canvas.chunk.occlusion._Data.V010;
import static grondag.canvas.chunk.occlusion._Data.V011;
import static grondag.canvas.chunk.occlusion._Data.V100;
import static grondag.canvas.chunk.occlusion._Data.V101;
import static grondag.canvas.chunk.occlusion._Data.V110;
import static grondag.canvas.chunk.occlusion._Data.V111;
import static grondag.canvas.chunk.occlusion._Data.mvpMatrixExt;
import static grondag.canvas.chunk.occlusion._Data.offsetX;
import static grondag.canvas.chunk.occlusion._Data.offsetY;
import static grondag.canvas.chunk.occlusion._Data.offsetZ;
import static grondag.canvas.chunk.occlusion._Data.vertexData;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.setupVertex;

import com.google.common.base.Strings;

import grondag.canvas.mixinterface.Matrix4fExt;

abstract class _Indexer {
	private  _Indexer() {}

	// TODO: remove
	//	int totalCount;
	//	int extTrue;
	//	int extFalse;
	//	int earlyExit;

	static boolean testUp() { return testQuad(V110, V010, V011, V111); }
	static boolean testDown() { return testQuad(V000, V100, V101, V001); }
	static boolean testEast() { return testQuad(V101, V100, V110, V111); }
	static boolean testWest() { return testQuad(V000, V001, V011, V010); }
	static boolean testSouth() { return testQuad(V001, V101, V111, V011); }
	static boolean testNorth() { return testQuad(V100, V000, V010, V110); }

	static boolean isChunkVisibleInner()  {
		if (isPointVisible(8, 8, 8)) {
			return true;
		}

		computeProjectedBoxBounds(0, 0, 0, 16, 16, 16);

		// rank tests by how directly they face - use distance from camera coordinates for this
		final int offsetX = _Data.offsetX;
		final int offsetY = _Data.offsetY;
		final int offsetZ = _Data.offsetZ;

		int testBits = 0;
		int nearBits  = 0;
		int xTest =  0;
		int yTest = 0;
		int zTest = 0;

		// if camera below top face can't be seen
		if (offsetY < -CAMERA_PRECISION_CHUNK_MAX) {
			// UP
			if (offsetY > -CAMERA_PRECISION_CHUNK_MAX - CAMERA_PRECISION_UNITY) {
				// if very close to plane then don't test - precision may give inconsistent results
				nearBits |= 2;
			}

			yTest = -offsetY + CAMERA_PRECISION_CHUNK_MAX;
			testBits |= 2;
		} else if (offsetY > 0) {
			// DOWN
			if (offsetY < CAMERA_PRECISION_UNITY) {
				nearBits |= 2;
			}

			yTest = offsetY;
			testBits |= 2;
		}

		if (offsetX < -CAMERA_PRECISION_CHUNK_MAX) {
			// EAST;
			if (offsetX > -CAMERA_PRECISION_CHUNK_MAX - CAMERA_PRECISION_UNITY) {
				nearBits |= 1;
			}

			xTest = -offsetX + CAMERA_PRECISION_CHUNK_MAX;
			testBits |= 1;
		} else if (offsetX > 0) {
			// WEST
			if (offsetX < CAMERA_PRECISION_UNITY) {
				nearBits |= 1;
			}

			xTest = offsetX;
			testBits |= 1;
		}

		if (offsetZ < -CAMERA_PRECISION_CHUNK_MAX) {
			// SOUTH
			if (offsetZ > -CAMERA_PRECISION_CHUNK_MAX - CAMERA_PRECISION_UNITY) {
				nearBits |= 4;
			}

			zTest = -offsetZ + CAMERA_PRECISION_CHUNK_MAX;
			testBits |= 4;
		} else if (offsetZ > 0) {
			// NORTH
			if (offsetZ < CAMERA_PRECISION_UNITY) {
				nearBits |= 4;
			}

			zTest = offsetZ;
			testBits |= 4;
		}

		// if only valid tests are very near, assume visible to avoid false negatives due to precision
		if (nearBits != 0 && (testBits & ~nearBits) == 0) {
			return true;
		} else {
			switch (testBits)  {
			default:
			case 0b000:
				return false;
			case 0b001:
				return offsetX > 0 ? testWest() : testEast();
			case 0b010:
				return offsetY > 0 ? testDown() : testUp();
			case 0b011:
				if (xTest > yTest) {
					return (offsetX > 0 ? testWest() : testEast()) || (offsetY > 0 ? testDown() : testUp());
				} else {
					return (offsetY > 0 ? testDown() : testUp()) || (offsetX > 0 ? testWest() : testEast());
				}
			case 0b100:
				return offsetZ > 0 ? testNorth() : testSouth();
			case 0b101:
				if (xTest > zTest) {
					return (offsetX > 0 ? testWest() : testEast()) || (offsetZ > 0 ? testNorth() : testSouth());
				} else {
					return (offsetZ > 0 ? testNorth() : testSouth()) || (offsetX > 0 ? testWest() : testEast());
				}
			case 0b110:
				if (yTest > zTest) {
					return (offsetY > 0 ? testDown() : testUp()) || (offsetZ > 0 ? testNorth() : testSouth());
				} else {
					return (offsetZ > 0 ? testNorth() : testSouth()) || (offsetY > 0 ? testDown() : testUp());
				}
			case 0b111:
				if (xTest > yTest) {
					if  (zTest > xTest) {
						// z first
						return (offsetZ > 0 ? testNorth() : testSouth())
								|| (offsetX > 0 ? testWest() : testEast())
								|| (offsetY > 0 ? testDown() : testUp());
					} else {
						// x first
						return (offsetX > 0 ? testWest() : testEast())
								|| (offsetZ > 0 ? testNorth() : testSouth())
								|| (offsetY > 0 ? testDown() : testUp());
					}
				} else if (zTest > yTest) {
					// z first
					return (offsetZ > 0 ? testNorth() : testSouth())
							|| (offsetY > 0 ? testDown() : testUp())
							|| (offsetX > 0 ? testWest() : testEast());
				} else {
					// y first
					return (offsetY > 0 ? testDown() : testUp())
							|| (offsetZ > 0 ? testNorth() : testSouth())
							|| (offsetX > 0 ? testWest() : testEast());
				}
			}
		}

		//		// TODO: remove
		//		if (occlusionRange == PackedBox.RANGE_EXTREME) {
		//			if (result) {
		//				++extTrue;
		//			} else {
		//				++extFalse;
		//			}
		//		}
		//
		//		if (++totalCount == 1000000) {
		//			System.out.println(String.format("extreme true: %f  extreme false: %f", extTrue / 10000f, extFalse / 10000f));
		//			System.out.println(String.format("Early exit: %f", earlyExit / 10000f));
		//			System.out.println();
		//			totalCount = 0;
		//			extTrue = 0;
		//			extFalse = 0;
		//			earlyExit = 0;
		//		}
	}

	static final void occlude(int x0, int y0, int z0, int x1, int y1, int z1) {
		computeProjectedBoxBounds(x0, y0, z0, x1, y1, z1);

		// PERF use same techniques as view test?
		if (offsetY < -(y1 << CAMERA_PRECISION_BITS)) drawQuad(V110, V010, V011, V111); // up
		if (offsetY > -(y0 << CAMERA_PRECISION_BITS)) drawQuad(V000, V100, V101, V001); // down
		if (offsetX < -(x1 << CAMERA_PRECISION_BITS)) drawQuad(V101, V100, V110, V111); // east
		if (offsetX > -(x0 << CAMERA_PRECISION_BITS)) drawQuad(V000, V001, V011, V010); // west
		if (offsetZ < -(z1 << CAMERA_PRECISION_BITS)) drawQuad(V001, V101, V111, V011); // south
		if (offsetZ > -(z0 << CAMERA_PRECISION_BITS)) drawQuad(V100, V000, V010, V110); // north
	}

	/**
	 * For early exit testing
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	static boolean isPointVisible(int x, int y, int z) {
		final Matrix4fExt mvpMatrixExt = _Data.mvpMatrixExt;

		final float w = mvpMatrixExt.a30() * x + mvpMatrixExt.a31() * y + mvpMatrixExt.a32() * z + mvpMatrixExt.a33();
		final float tz = mvpMatrixExt.a20() * x + mvpMatrixExt.a21() * y + mvpMatrixExt.a22() * z + mvpMatrixExt.a23();

		if (w <= 0 || tz < 0 || tz > w) {
			return false;
		}

		final float iw = 1f / w;
		final float tx = HALF_PIXEL_WIDTH  * iw * (mvpMatrixExt.a00() * x + mvpMatrixExt.a01() * y + mvpMatrixExt.a02() * z + mvpMatrixExt.a03());
		final float ty = HALF_PIXEL_HEIGHT * iw * (mvpMatrixExt.a10() * x + mvpMatrixExt.a11() * y + mvpMatrixExt.a12() * z + mvpMatrixExt.a13());
		final int px = (int) tx + HALF_PIXEL_WIDTH;
		final int py = (int) ty + HALF_PIXEL_HEIGHT;

		if (px >= 0 && py >= 0 && px < PIXEL_WIDTH && py < PIXEL_HEIGHT && testPixel(px, py)) {
			return true;
		}

		return false;
	}

	static final void computeProjectedBoxBounds(int x0, int y0, int z0, int x1, int y1, int z1) {
		setupVertex(vertexData, V000, x0, y0, z0, mvpMatrixExt);
		setupVertex(vertexData, V001, x0, y0, z1, mvpMatrixExt);
		setupVertex(vertexData, V010, x0, y1, z0, mvpMatrixExt);
		setupVertex(vertexData, V011, x0, y1, z1, mvpMatrixExt);
		setupVertex(vertexData, V100, x1, y0, z0, mvpMatrixExt);
		setupVertex(vertexData, V101, x1, y0, z1, mvpMatrixExt);
		setupVertex(vertexData, V110, x1, y1, z0, mvpMatrixExt);
		setupVertex(vertexData, V111, x1, y1, z1, mvpMatrixExt);
	}

	static boolean testPixel(int x, int y) {
		return (_Data.lowTiles[lowIndexFromPixelXY(x, y)] & (1L << (pixelIndex(x, y)))) == 0;
	}

	static void drawPixel(int x, int y) {
		_Data.lowTiles[lowIndexFromPixelXY(x, y)] |= (1L << (pixelIndex(x, y)));
	}

	static long nextTime;

	static int mortonNumber(int x, int y) {
		int z = (x & 0b001) | ((y & 0b001) << 1);
		z |= ((x & 0b010) << 1) | ((y & 0b010) << 2);
		return z | ((x & 0b100) << 2) | ((y & 0b100) << 3);
	}

	static int midIndex(int midX, int midY) {
		final int topX = (midX >> LOW_AXIS_SHIFT);
		final int topY = (midY >> LOW_AXIS_SHIFT);
		return (topIndex(topX, topY) << MID_AXIS_SHIFT) | (mortonNumber(midX, midY));
	}

	static int topIndex(int topX, int topY) {
		return (topY << TOP_Y_SHIFT) | topX;
	}

	static int lowIndex(int lowX, int lowY) {
		final int midX = (lowX >> LOW_AXIS_SHIFT) & TILE_PIXEL_INDEX_MASK;
		final int midY = (lowY >> LOW_AXIS_SHIFT) & TILE_PIXEL_INDEX_MASK;

		final int topX = (lowX >> MID_AXIS_SHIFT);
		final int topY = (lowY >> MID_AXIS_SHIFT);

		return (topIndex(topX, topY) << TOP_INDEX_SHIFT) | (mortonNumber(midX, midY) << MID_INDEX_SHIFT) | mortonNumber(lowX & TILE_PIXEL_INDEX_MASK, lowY & TILE_PIXEL_INDEX_MASK);
	}

	static int lowIndexFromPixelXY(int x, int y)  {
		return lowIndex(x >>> LOW_AXIS_SHIFT, y >>> LOW_AXIS_SHIFT);
	}

	static int pixelIndex(int x, int y)  {
		return  ((y & TILE_PIXEL_INDEX_MASK) << TILE_AXIS_SHIFT) | (x & TILE_PIXEL_INDEX_MASK);
	}

	static boolean isPixelClear(long word, int x, int y)  {
		return (word & (1L << pixelIndex(x, y))) == 0;
	}

	static long pixelMask(int x, int y) {
		return 1L << pixelIndex(x, y);
	}

	/** REQUIRES 0-7 inputs! */
	static boolean testPixelInWordPreMasked(long word, int x, int y) {
		return (word & (1L << ((y << TILE_AXIS_SHIFT) | x))) == 0;
	}

	static long setPixelInWordPreMasked(long word, int x, int y) {
		return word | (1L << ((y << TILE_AXIS_SHIFT) | x));
	}

	static void printCoverageMask(long mask) {
		final String s = Strings.padStart(Long.toBinaryString(mask), 64, '0');
		OcclusionBitPrinter.printSpaced(s.substring(0, 8));
		OcclusionBitPrinter.printSpaced(s.substring(8, 16));
		OcclusionBitPrinter.printSpaced(s.substring(16, 24));
		OcclusionBitPrinter.printSpaced(s.substring(24, 32));
		OcclusionBitPrinter.printSpaced(s.substring(32, 40));
		OcclusionBitPrinter.printSpaced(s.substring(40, 48));
		OcclusionBitPrinter.printSpaced(s.substring(48, 56));
		OcclusionBitPrinter.printSpaced(s.substring(56, 64));
		System.out.println();
	}


}

