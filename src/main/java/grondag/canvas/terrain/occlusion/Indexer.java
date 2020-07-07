package grondag.canvas.terrain.occlusion;

import static grondag.canvas.terrain.occlusion.Constants.TILE_ADDRESS_SHIFT_X;
import static grondag.canvas.terrain.occlusion.Constants.TILE_ADDRESS_SHIFT_Y;
import static grondag.canvas.terrain.occlusion.Constants.TILE_AXIS_MASK;
import static grondag.canvas.terrain.occlusion.Constants.TILE_AXIS_SHIFT;
import static grondag.canvas.terrain.occlusion.Constants.TILE_PIXEL_INDEX_MASK;

import com.google.common.base.Strings;

import grondag.canvas.terrain.occlusion.region.OcclusionBitPrinter;

abstract class Indexer {
	private  Indexer() {}

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

