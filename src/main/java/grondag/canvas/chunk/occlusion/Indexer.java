package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.CAMERA_PRECISION_BITS;
import static grondag.canvas.chunk.occlusion.Constants.HALF_PIXEL_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.HALF_PIXEL_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.TILE_ADDRESS_SHIFT_X;
import static grondag.canvas.chunk.occlusion.Constants.TILE_ADDRESS_SHIFT_Y;
import static grondag.canvas.chunk.occlusion.Constants.TILE_AXIS_MASK;
import static grondag.canvas.chunk.occlusion.Constants.TILE_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.Constants.TILE_PIXEL_INDEX_MASK;
import static grondag.canvas.chunk.occlusion.Constants.V000;
import static grondag.canvas.chunk.occlusion.Constants.V001;
import static grondag.canvas.chunk.occlusion.Constants.V010;
import static grondag.canvas.chunk.occlusion.Constants.V011;
import static grondag.canvas.chunk.occlusion.Constants.V100;
import static grondag.canvas.chunk.occlusion.Constants.V101;
import static grondag.canvas.chunk.occlusion.Constants.V110;
import static grondag.canvas.chunk.occlusion.Constants.V111;
import static grondag.canvas.chunk.occlusion.Data.offsetX;
import static grondag.canvas.chunk.occlusion.Data.offsetY;
import static grondag.canvas.chunk.occlusion.Data.offsetZ;
import static grondag.canvas.chunk.occlusion.Matrix4L.MATRIX_PRECISION_HALF;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.setupVertex;

import com.google.common.base.Strings;

import grondag.canvas.chunk.occlusion.region.OcclusionBitPrinter;

abstract class Indexer {
	private  Indexer() {}

	// TODO: remove
	//	int totalCount;
	//	int extTrue;
	//	int extFalse;
	//	int earlyExit;

	static boolean testUp() { return Rasterizer.testQuad(V110, V010, V011, V111); }
	static boolean testDown() { return Rasterizer.testQuad(V000, V100, V101, V001); }
	static boolean testEast() { return Rasterizer.testQuad(V101, V100, V110, V111); }
	static boolean testWest() { return Rasterizer.testQuad(V000, V001, V011, V010); }
	static boolean testSouth() { return Rasterizer.testQuad(V001, V101, V111, V011); }
	static boolean testNorth() { return Rasterizer.testQuad(V100, V000, V010, V110); }

	static final void occlude(int x0, int y0, int z0, int x1, int y1, int z1) {
		computeProjectedBoxBounds(x0, y0, z0, x1, y1, z1);

		// PERF use same techniques as view test?
		if (offsetY < -(y1 << CAMERA_PRECISION_BITS)) Rasterizer.drawQuad(V110, V010, V011, V111); // up
		if (offsetY > -(y0 << CAMERA_PRECISION_BITS)) Rasterizer.drawQuad(V000, V100, V101, V001); // down
		if (offsetX < -(x1 << CAMERA_PRECISION_BITS)) Rasterizer.drawQuad(V101, V100, V110, V111); // east
		if (offsetX > -(x0 << CAMERA_PRECISION_BITS)) Rasterizer.drawQuad(V000, V001, V011, V010); // west
		if (offsetZ < -(z1 << CAMERA_PRECISION_BITS)) Rasterizer.drawQuad(V001, V101, V111, V011); // south
		if (offsetZ > -(z0 << CAMERA_PRECISION_BITS)) Rasterizer.drawQuad(V100, V000, V010, V110); // north
	}

	/**
	 * For early exit testing
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	static boolean isPointVisible(int x, int y, int z) {
		final Matrix4L mvpMatrix = Data.mvpMatrix;

		final long w = mvpMatrix.transformVec4W(x, y, z);
		final long tz = mvpMatrix.transformVec4Z(x, y, z);

		if (w <= 0 || tz < 0 || tz > w) {
			return false;
		}

		final int px = (int) (HALF_PIXEL_WIDTH + (MATRIX_PRECISION_HALF + HALF_PIXEL_WIDTH  * mvpMatrix.transformVec4X(x, y, z)) / w);
		final int py = (int) (HALF_PIXEL_HEIGHT + (MATRIX_PRECISION_HALF + HALF_PIXEL_HEIGHT * mvpMatrix.transformVec4Y(x, y, z)) / w);

		if (px >= 0 && py >= 0 && px < PIXEL_WIDTH && py < PIXEL_HEIGHT && testPixel(px, py)) {
			return true;
		}

		return false;
	}

	static final void computeProjectedBoxBounds(int x0, int y0, int z0, int x1, int y1, int z1) {
		setupVertex(V000, x0, y0, z0);
		setupVertex(V001, x0, y0, z1);
		setupVertex(V010, x0, y1, z0);
		setupVertex(V011, x0, y1, z1);
		setupVertex(V100, x1, y0, z0);
		setupVertex(V101, x1, y0, z1);
		setupVertex(V110, x1, y1, z0);
		setupVertex(V111, x1, y1, z1);
	}

	static boolean testPixel(int x, int y) {
		return (Data.tiles[lowIndexFromPixelXY(x, y)] & (1L << (pixelIndex(x, y)))) == 0;
	}

	static void drawPixel(int x, int y) {
		Data.tiles[lowIndexFromPixelXY(x, y)] |= (1L << (pixelIndex(x, y)));
	}

	static long nextRasterOutputTime;

	// only handle 0-7  values
	static int mortonNumber(int x, int y) {
		int z = (x & 0b001) | ((y & 0b001) << 1);
		z |= ((x & 0b010) << 1) | ((y & 0b010) << 2);
		return z | ((x & 0b100) << 2) | ((y & 0b100) << 3);
	}

	static int tileIndex(int tileX, int tileY) {
		return ((tileY & TILE_AXIS_MASK) << TILE_ADDRESS_SHIFT_Y) | ((tileX & TILE_AXIS_MASK) << TILE_ADDRESS_SHIFT_X) | ((tileY & TILE_PIXEL_INDEX_MASK) << TILE_AXIS_SHIFT) | (tileX & TILE_PIXEL_INDEX_MASK);
	}

	static int lowIndexFromPixelXY(int x, int y)  {
		return tileIndex(x >>> TILE_AXIS_SHIFT, y >>> TILE_AXIS_SHIFT);
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

