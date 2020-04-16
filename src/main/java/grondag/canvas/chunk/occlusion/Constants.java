package grondag.canvas.chunk.occlusion;

import grondag.canvas.Configurator;

public class Constants {

	static final boolean ENABLE_RASTER_OUTPUT = Configurator.debugOcclusionRaster;
	static final int TILE_AXIS_SHIFT = 3;
	static final int TILE_PIXEL_DIAMETER = 1 << TILE_AXIS_SHIFT;
	static final int TILE_PIXEL_INDEX_MASK = TILE_PIXEL_DIAMETER - 1;
	static final int TILE_PIXEL_INVERSE_MASK = ~TILE_PIXEL_INDEX_MASK;
	static final int LOW_AXIS_SHIFT = TILE_AXIS_SHIFT;
	static final int LOW_AXIS_MASK = ~((1 << LOW_AXIS_SHIFT) - 1);
	static final int MID_AXIS_SHIFT = TILE_AXIS_SHIFT * 2;
	static final int MID_AXIS_MASK = ~((1 << MID_AXIS_SHIFT) - 1);
	static final int MID_INDEX_SHIFT = LOW_AXIS_SHIFT * 2;
	static final int TOP_INDEX_SHIFT = MID_INDEX_SHIFT * 2;

	static final int MID_WIDTH = 16;
	static final int MID_Y_SHIFT = Integer.bitCount(MID_WIDTH - 1);
	static final int MIDDLE_HEIGHT = 8;

	static final int TOP_Y_SHIFT = Integer.bitCount(MID_WIDTH / 8 - 1);

	static final int PRECISION_BITS = 4;
	static final int PRECISE_FRACTION_MASK = (1 << PRECISION_BITS) - 1;
	static final int PRECISE_INTEGER_MASK = ~PRECISE_FRACTION_MASK;
	static final int PRECISE_PIXEL_SIZE = 1 << PRECISION_BITS;
	static final int PRECISE_PIXEL_CENTER = PRECISE_PIXEL_SIZE / 2;

	static final int LOW_WIDTH = MID_WIDTH * 8;
	//static final int LOW_Y_SHIFT = Integer.bitCount(LOW_WIDTH - 1);
	static final int PIXEL_WIDTH = LOW_WIDTH * TILE_PIXEL_DIAMETER;
	static final int MAX_PIXEL_X = PIXEL_WIDTH - 1;
	static final int HALF_PIXEL_WIDTH = PIXEL_WIDTH / 2;
	static final int PRECISE_WIDTH = PIXEL_WIDTH << PRECISION_BITS;
	static final int HALF_PRECISE_WIDTH = PRECISE_WIDTH / 2;
	/** clamp to this to ensure value + half pixel rounds down to last pixel */
	static final int PRECISE_WIDTH_CLAMP = PRECISE_WIDTH - PRECISE_PIXEL_CENTER;

	static final int LOW_HEIGHT = MIDDLE_HEIGHT * 8;
	static final int PIXEL_HEIGHT = LOW_HEIGHT * TILE_PIXEL_DIAMETER;
	static final int MAX_PIXEL_Y = PIXEL_HEIGHT - 1;
	static final int HALF_PIXEL_HEIGHT = PIXEL_HEIGHT / 2;
	//	static final int HEIGHT_WORD_RELATIVE_SHIFT = LOW_Y_SHIFT - BIN_AXIS_SHIFT;
	static final int PRECISE_HEIGHT = PIXEL_HEIGHT << PRECISION_BITS;
	static final int HALF_PRECISE_HEIGHT = PRECISE_HEIGHT / 2;
	/** clamp to this to ensure value + half pixel rounds down to last pixel */
	static final int PRECISE_HEIGHT_CLAMP = PRECISE_HEIGHT - PRECISE_PIXEL_CENTER;

	static final int GUARD_SIZE = 512 << PRECISION_BITS;
	static final int GUARD_WIDTH = PRECISE_WIDTH + GUARD_SIZE;
	static final int GUARD_HEIGHT = PRECISE_HEIGHT + GUARD_SIZE;

	static final int LOW_TILE_COUNT = LOW_WIDTH * LOW_HEIGHT;
	static final int MID_TILE_COUNT = MID_WIDTH * LOW_HEIGHT;

	static final int MID_TILE_PIXEL_DIAMETER = PIXEL_WIDTH / MID_WIDTH;
	static final int MID_TILE_SPAN = MID_TILE_PIXEL_DIAMETER - 1;

	static final int LOW_TILE_PIXEL_DIAMETER = PIXEL_WIDTH / LOW_WIDTH;
	static final int LOW_TILE_SPAN = LOW_TILE_PIXEL_DIAMETER - 1;

	static final long[] EMPTY_BITS = new long[LOW_TILE_COUNT];

	static final int CAMERA_PRECISION_BITS = 12;
	static final int CAMERA_PRECISION_UNITY = 1 << CAMERA_PRECISION_BITS;
	static final int CAMERA_PRECISION_CHUNK_MAX = 16 * CAMERA_PRECISION_UNITY;

	static final int SCALE_POINT = 0;
	static final int SCALE_VLINE = 1;
	static final int SCALE_HLINE = 2;
	static final int SCALE_LOW = 3;
	static final int SCALE_MID = 4;

	static final int BOUNDS_IN = 0;
	static final int BOUNDS_OUTSIDE_OR_TOO_SMALL = 1;
	static final int BOUNDS_NEEDS_CLIP = 2;

	static final int OUTSIDE = 1;
	static final int INTERSECTING = 2;
	static final int INSIDE = 4;

	static final int COVERAGE_NONE_OR_SOME = 0;
	static final int COVERAGE_FULL = 1;

	static final int OFFSET_NONE = 0;
	static final int OFFSET_A = 4;
	static final int OFFSET_B = 8;
	static final int OFFSET_AB = OFFSET_A | OFFSET_B;

	// upper two bits determine offset
	// lower two bits disambiguate within

	static final int EDGE_TOP = OFFSET_NONE | 0;
	static final int EDGE_RIGHT = OFFSET_NONE | 1;
	static final int EDGE_TOP_RIGHT = OFFSET_NONE | 2;
	static final int EDGE_LEFT = OFFSET_A | 0;
	static final int EDGE_TOP_LEFT = OFFSET_A | 1;
	static final int EDGE_BOTTOM = OFFSET_B | 0;
	static final int EDGE_BOTTOM_RIGHT = OFFSET_B | 1;
	static final int EDGE_BOTTOM_LEFT = OFFSET_AB | 0;


	static final int EDGE_FLAGS_TOP = (1 << EDGE_TOP) | (1 << EDGE_TOP_LEFT) | (1 << EDGE_TOP_RIGHT);
	static final int EDGE_FLAGS_BOTTOM = (1 << EDGE_BOTTOM) | (1 << EDGE_BOTTOM_LEFT) | (1 << EDGE_BOTTOM_RIGHT);
	static final int EDGE_FLAGS_LEFT = (1 << EDGE_LEFT) | (1 << EDGE_BOTTOM_LEFT) | (1 << EDGE_TOP_LEFT);
	static final int EDGE_FLAGS_RIGHT = (1 << EDGE_RIGHT) | (1 << EDGE_TOP_RIGHT) | (1 << EDGE_BOTTOM_RIGHT);

}
