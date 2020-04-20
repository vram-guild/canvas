package grondag.canvas.chunk.occlusion;

import java.util.Arrays;

import grondag.canvas.Configurator;

public class Constants {

	static final boolean ENABLE_RASTER_OUTPUT = Configurator.debugOcclusionRaster;
	static final int TILE_AXIS_SHIFT = 3;
	static final int TILE_AXIS_MASK = ~((1 << TILE_AXIS_SHIFT) - 1);
	static final int TILE_PIXEL_DIAMETER = 1 << TILE_AXIS_SHIFT;
	static final int TILE_PIXEL_INDEX_MASK = TILE_PIXEL_DIAMETER - 1;
	static final int TILE_PIXEL_INVERSE_MASK = ~TILE_PIXEL_INDEX_MASK;

	static final int PRECISION_BITS = 4;
	static final int PRECISE_FRACTION_MASK = (1 << PRECISION_BITS) - 1;
	static final int PRECISE_INTEGER_MASK = ~PRECISE_FRACTION_MASK;
	static final int PRECISE_PIXEL_SIZE = 1 << PRECISION_BITS;
	static final int PRECISE_PIXEL_CENTER = PRECISE_PIXEL_SIZE / 2;
	static final int SCANT_PRECISE_PIXEL_CENTER = PRECISE_PIXEL_CENTER - 1;

	static final int TILE_WIDTH = 128;
	static final int TILE_WIDTH_BITS = Integer.bitCount(TILE_WIDTH - 1);
	static final int TILE_ADDRESS_SHIFT_X = TILE_AXIS_SHIFT; // starts at 6 bits, but bottom 3 are part of low 6 bits
	static final int PIXEL_WIDTH = TILE_WIDTH * TILE_PIXEL_DIAMETER;
	static final int MAX_PIXEL_X = PIXEL_WIDTH - 1;
	static final int HALF_PIXEL_WIDTH = PIXEL_WIDTH / 2;
	static final int PRECISE_WIDTH = PIXEL_WIDTH << PRECISION_BITS;
	static final int HALF_PRECISE_WIDTH = PRECISE_WIDTH / 2;
	/** clamp to this to ensure value + half pixel rounds down to last pixel */
	static final int PRECISE_WIDTH_CLAMP = PRECISE_WIDTH - PRECISE_PIXEL_CENTER;

	static final int TILE_HEIGHT = 64;
	static final int TILE_HEIGHT_BITS = Integer.bitCount(TILE_HEIGHT - 1);
	static final int TILE_ADDRESS_SHIFT_Y = TILE_ADDRESS_SHIFT_X + TILE_WIDTH_BITS - TILE_AXIS_SHIFT;
	static final int PIXEL_HEIGHT = TILE_HEIGHT * TILE_PIXEL_DIAMETER;
	static final int MAX_PIXEL_Y = PIXEL_HEIGHT - 1;
	static final int HALF_PIXEL_HEIGHT = PIXEL_HEIGHT / 2;
	static final int PRECISE_HEIGHT = PIXEL_HEIGHT << PRECISION_BITS;
	static final int HALF_PRECISE_HEIGHT = PRECISE_HEIGHT / 2;
	/** clamp to this to ensure value + half pixel rounds down to last pixel */
	static final int PRECISE_HEIGHT_CLAMP = PRECISE_HEIGHT - PRECISE_PIXEL_CENTER;

	static final int GUARD_SIZE = 512 << PRECISION_BITS;
	static final int GUARD_WIDTH = PRECISE_WIDTH + GUARD_SIZE;
	static final int GUARD_HEIGHT = PRECISE_HEIGHT + GUARD_SIZE;

	static final int TILE_COUNT = TILE_WIDTH * TILE_HEIGHT;

	static final long[] EMPTY_BITS = new long[TILE_COUNT];

	static final int CAMERA_PRECISION_BITS = 12;
	static final int CAMERA_PRECISION_UNITY = 1 << CAMERA_PRECISION_BITS;
	static final int CAMERA_PRECISION_CHUNK_MAX = 16 * CAMERA_PRECISION_UNITY;

	static final int BOUNDS_IN = 0;
	static final int BOUNDS_OUTSIDE_OR_TOO_SMALL = 1;
	static final int BOUNDS_NEEDS_CLIP = 2;

	static final int OUTSIDE_0 = 1;
	static final int INSIDE_0 = 2;
	static final int OUTSIDE_1 = 4;
	static final int INSIDE_1 = 8;
	static final int OUTSIDE_2 = 16;
	static final int INSIDE_2 = 32;
	static final int INTERSECT = 0;

	static final int POS_INVERSE_MASK_0 =  ~(OUTSIDE_0 | INSIDE_0);
	static final int POS_INVERSE_MASK_1 =  ~(OUTSIDE_1 | INSIDE_1);
	static final int POS_INVERSE_MASK_2 =  ~(OUTSIDE_2 | INSIDE_2);

	static final int POS_012_III = INSIDE_0 	| 	INSIDE_1 	| 	INSIDE_2;
	static final int POS_012_OII = OUTSIDE_0 	| 	INSIDE_1 	| 	INSIDE_2;
	static final int POS_012_XII = INTERSECT 	| 	INSIDE_1 	| 	INSIDE_2;
	static final int POS_012_IOI = INSIDE_0 	| 	OUTSIDE_1 	| 	INSIDE_2;
	static final int POS_012_OOI = OUTSIDE_0 	| 	OUTSIDE_1 	| 	INSIDE_2;
	static final int POS_012_XOI = INTERSECT 	| 	OUTSIDE_1 	| 	INSIDE_2;
	static final int POS_012_IXI = INSIDE_0 	| 	INTERSECT	|	INSIDE_2;
	static final int POS_012_OXI = OUTSIDE_0 	|	INTERSECT	|	INSIDE_2;
	static final int POS_012_XXI = INTERSECT	|	INTERSECT	|	INSIDE_2;

	static final int POS_012_IIO = INSIDE_0 	| 	INSIDE_1 	| 	OUTSIDE_2;
	static final int POS_012_OIO = OUTSIDE_0 	| 	INSIDE_1 	| 	OUTSIDE_2;
	static final int POS_012_XIO = INTERSECT	|	INSIDE_1 	| 	OUTSIDE_2;
	static final int POS_012_IOO = INSIDE_0 	| 	OUTSIDE_1 	| 	OUTSIDE_2;
	static final int POS_012_OOO = OUTSIDE_0 	| 	OUTSIDE_1 	| 	OUTSIDE_2;
	static final int POS_012_XOO = INTERSECT	|	OUTSIDE_1 	| 	OUTSIDE_2;
	static final int POS_012_IXO = INSIDE_0 	| 	INTERSECT	|	OUTSIDE_2;
	static final int POS_012_OXO = OUTSIDE_0 	|	INTERSECT	|	OUTSIDE_2;
	static final int POS_012_XXO = INTERSECT	|	INTERSECT	|	OUTSIDE_2;

	static final int POS_012_IIX = INSIDE_0 	| 	INSIDE_1	|	INTERSECT;
	static final int POS_012_OIX = OUTSIDE_0 	| 	INSIDE_1	|	INTERSECT;
	static final int POS_012_XIX = INTERSECT	|	INSIDE_1	|	INTERSECT;
	static final int POS_012_IOX = INSIDE_0 	| 	OUTSIDE_1	|	INTERSECT;
	static final int POS_012_OOX = OUTSIDE_0 	| 	OUTSIDE_1	|	INTERSECT;
	static final int POS_012_XOX = INTERSECT	|	OUTSIDE_1	|	INTERSECT;
	static final int POS_012_IXX = INSIDE_0		|	INTERSECT	|	INTERSECT;
	static final int POS_012_OXX = OUTSIDE_0	|	INTERSECT	| 	INTERSECT;
	static final int POS_012_XXX = INTERSECT	|	INTERSECT	|	INTERSECT;

	static final int COVERAGE_NONE_OR_SOME = 0;
	static final int COVERAGE_FULL = 1;

	static final int B_NEGATIVE = 8;
	static final int B_ZERO = 16;
	static final int B_POSITIVE = 32;

	static final int A_NEGATIVE = 1;
	static final int A_ZERO = 2;
	static final int A_POSITIVE = 4;

	static final int EDGE_TOP = B_NEGATIVE | A_ZERO;
	static final int EDGE_BOTTOM = B_POSITIVE | A_ZERO;
	static final int EDGE_RIGHT = B_ZERO | A_NEGATIVE;
	static final int EDGE_LEFT = B_ZERO | A_POSITIVE;
	static final int EDGE_TOP_RIGHT = B_NEGATIVE | A_NEGATIVE;
	static final int EDGE_TOP_LEFT = B_NEGATIVE | A_POSITIVE;
	static final int EDGE_BOTTOM_RIGHT = B_POSITIVE | A_NEGATIVE;
	static final int EDGE_BOTTOM_LEFT = B_POSITIVE | A_POSITIVE;

	static final int EVENT_EMPTY_ROW = -2;
	static final int EVENT_FULL_ROW = -1;

	static final short[] EMPTY_EVENT = new short[PIXEL_HEIGHT];

	static {
		Arrays.fill(EMPTY_EVENT, (short) EVENT_EMPTY_ROW);
	}
}
