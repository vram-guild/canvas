package grondag.canvas.terrain.occlusion;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

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

	static final int TILE_INDEX_LOW_Y_MASK = TILE_PIXEL_INDEX_MASK << TILE_AXIS_SHIFT;
	static final int TILE_INDEX_LOW_X_MASK = TILE_PIXEL_INDEX_MASK;
	static final int TILE_INDEX_LOW_Y = 1 << TILE_AXIS_SHIFT;
	static final int TILE_INDEX_HIGH_Y = TILE_INDEX_LOW_Y << TILE_ADDRESS_SHIFT_Y;
	static final int TILE_INDEX_HIGH_X = TILE_INDEX_LOW_Y << TILE_ADDRESS_SHIFT_X;

	static final int GUARD_SIZE = 512 << PRECISION_BITS;
	static final int GUARD_MAX = PRECISE_WIDTH + GUARD_SIZE;
	static final int GUARD_MIN = -GUARD_SIZE;

	static final int CLIP_RANGE = PRECISE_WIDTH + GUARD_SIZE * 2;
	static final int CLIP_MASK = ~(MathHelper.smallestEncompassingPowerOfTwo(CLIP_RANGE) - 1);

	static final int TILE_COUNT = TILE_WIDTH * TILE_HEIGHT;

	static final long[] EMPTY_BITS = new long[TILE_COUNT];

	static final int CAMERA_PRECISION_BITS = 12;
	static final int CAMERA_PRECISION_UNITY = 1 << CAMERA_PRECISION_BITS;
	static final int CAMERA_PRECISION_CHUNK_MAX = 18 * CAMERA_PRECISION_UNITY;
	static final int CAMERA_PRECISION_HALF = CAMERA_PRECISION_UNITY / 2;

	static final int BOUNDS_IN = 0;
	static final int BOUNDS_OUTSIDE_OR_TOO_SMALL = 1;

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
	static final int EDGE_POINT = A_ZERO | B_ZERO;

	// subtract 1 to fit in 2 bits
	static final int EVENT_0_LEFT = A_POSITIVE - 1;
	static final int EVENT_0_FLAT = A_ZERO - 1;
	static final int EVENT_0_RIGHT = A_NEGATIVE - 1;
	static final int EVENT_POSITION_MASK = 3;

	static final int EVENT_1_LEFT = EVENT_0_LEFT << 2;
	static final int EVENT_1_FLAT = EVENT_0_FLAT << 2;
	static final int EVENT_1_RIGHT = EVENT_0_RIGHT << 2;

	static final int EVENT_2_LEFT = EVENT_1_LEFT << 2;
	static final int EVENT_2_FLAT = EVENT_1_FLAT << 2;
	static final int EVENT_2_RIGHT = EVENT_1_RIGHT << 2;

	static final int EVENT_3_LEFT = EVENT_2_LEFT << 2;
	static final int EVENT_3_FLAT = EVENT_2_FLAT << 2;
	static final int EVENT_3_RIGHT = EVENT_2_RIGHT << 2;


	static final int EVENT_012_RRR = EVENT_0_RIGHT 		| EVENT_1_RIGHT  	| EVENT_2_RIGHT;
	static final int EVENT_012_LRR = EVENT_0_LEFT 		| EVENT_1_RIGHT  	| EVENT_2_RIGHT;
	static final int EVENT_012_FRR = EVENT_0_FLAT 		| EVENT_1_RIGHT  	| EVENT_2_RIGHT;
	static final int EVENT_012_RLR = EVENT_0_RIGHT 		| EVENT_1_LEFT  	| EVENT_2_RIGHT;
	static final int EVENT_012_LLR = EVENT_0_LEFT 		| EVENT_1_LEFT  	| EVENT_2_RIGHT;
	static final int EVENT_012_FLR = EVENT_0_FLAT 		| EVENT_1_LEFT  	| EVENT_2_RIGHT;
	static final int EVENT_012_RFR = EVENT_0_RIGHT 		| EVENT_1_FLAT  	| EVENT_2_RIGHT;
	static final int EVENT_012_LFR = EVENT_0_LEFT 		| EVENT_1_FLAT  	| EVENT_2_RIGHT;
	static final int EVENT_012_FFR = EVENT_0_FLAT 		| EVENT_1_FLAT  	| EVENT_2_RIGHT;

	static final int EVENT_012_RRL = EVENT_0_RIGHT 		| EVENT_1_RIGHT  	| EVENT_2_LEFT;
	static final int EVENT_012_LRL = EVENT_0_LEFT 		| EVENT_1_RIGHT 	| EVENT_2_LEFT;
	static final int EVENT_012_FRL = EVENT_0_FLAT 		| EVENT_1_RIGHT  	| EVENT_2_LEFT;
	static final int EVENT_012_RLL = EVENT_0_RIGHT 		| EVENT_1_LEFT  	| EVENT_2_LEFT;
	static final int EVENT_012_LLL = EVENT_0_LEFT 		| EVENT_1_LEFT  	| EVENT_2_LEFT;
	static final int EVENT_012_FLL = EVENT_0_FLAT 		| EVENT_1_LEFT  	| EVENT_2_LEFT;
	static final int EVENT_012_RFL = EVENT_0_RIGHT 		| EVENT_1_FLAT  	| EVENT_2_LEFT;
	static final int EVENT_012_LFL = EVENT_0_LEFT 		| EVENT_1_FLAT  	| EVENT_2_LEFT;
	static final int EVENT_012_FFL = EVENT_0_FLAT 		| EVENT_1_FLAT  	| EVENT_2_LEFT;

	static final int EVENT_012_LRF = EVENT_0_LEFT 		| EVENT_1_RIGHT  	| EVENT_2_FLAT;
	static final int EVENT_012_RRF = EVENT_0_RIGHT 		| EVENT_1_RIGHT  	| EVENT_2_FLAT;
	static final int EVENT_012_FRF = EVENT_0_FLAT 		| EVENT_1_RIGHT  	| EVENT_2_FLAT;
	static final int EVENT_012_RLF = EVENT_0_RIGHT 		| EVENT_1_LEFT  	| EVENT_2_FLAT;
	static final int EVENT_012_LLF = EVENT_0_LEFT 		| EVENT_1_LEFT  	| EVENT_2_FLAT;
	static final int EVENT_012_FLF = EVENT_0_FLAT 		| EVENT_1_LEFT  	| EVENT_2_FLAT;
	static final int EVENT_012_RFF = EVENT_0_RIGHT 		| EVENT_1_FLAT  	| EVENT_2_FLAT;
	static final int EVENT_012_LFF = EVENT_0_LEFT 		| EVENT_1_FLAT  	| EVENT_2_FLAT;
	static final int EVENT_012_FFF = EVENT_0_FLAT 		| EVENT_1_FLAT  	| EVENT_2_FLAT;


	////

	static final int EVENT_0123_RRRR = EVENT_0_RIGHT 	| EVENT_1_RIGHT  	| EVENT_2_RIGHT		| EVENT_3_RIGHT;
	static final int EVENT_0123_LRRR = EVENT_0_LEFT 	| EVENT_1_RIGHT  	| EVENT_2_RIGHT		| EVENT_3_RIGHT;
	static final int EVENT_0123_FRRR = EVENT_0_FLAT 	| EVENT_1_RIGHT  	| EVENT_2_RIGHT		| EVENT_3_RIGHT;
	static final int EVENT_0123_RLRR = EVENT_0_RIGHT 	| EVENT_1_LEFT  	| EVENT_2_RIGHT		| EVENT_3_RIGHT;
	static final int EVENT_0123_LLRR = EVENT_0_LEFT 	| EVENT_1_LEFT  	| EVENT_2_RIGHT		| EVENT_3_RIGHT;
	static final int EVENT_0123_FLRR = EVENT_0_FLAT 	| EVENT_1_LEFT  	| EVENT_2_RIGHT		| EVENT_3_RIGHT;
	static final int EVENT_0123_RFRR = EVENT_0_RIGHT 	| EVENT_1_FLAT  	| EVENT_2_RIGHT		| EVENT_3_RIGHT;
	static final int EVENT_0123_LFRR = EVENT_0_LEFT 	| EVENT_1_FLAT  	| EVENT_2_RIGHT		| EVENT_3_RIGHT;
	static final int EVENT_0123_FFRR = EVENT_0_FLAT 	| EVENT_1_FLAT  	| EVENT_2_RIGHT		| EVENT_3_RIGHT;

	static final int EVENT_0123_RRLR = EVENT_0_RIGHT 	| EVENT_1_RIGHT  	| EVENT_2_LEFT		| EVENT_3_RIGHT;
	static final int EVENT_0123_LRLR = EVENT_0_LEFT 	| EVENT_1_RIGHT 	| EVENT_2_LEFT		| EVENT_3_RIGHT;
	static final int EVENT_0123_FRLR = EVENT_0_FLAT 	| EVENT_1_RIGHT  	| EVENT_2_LEFT		| EVENT_3_RIGHT;
	static final int EVENT_0123_RLLR = EVENT_0_RIGHT 	| EVENT_1_LEFT  	| EVENT_2_LEFT		| EVENT_3_RIGHT;
	static final int EVENT_0123_LLLR = EVENT_0_LEFT 	| EVENT_1_LEFT  	| EVENT_2_LEFT		| EVENT_3_RIGHT;
	static final int EVENT_0123_FLLR = EVENT_0_FLAT 	| EVENT_1_LEFT  	| EVENT_2_LEFT		| EVENT_3_RIGHT;
	static final int EVENT_0123_RFLR = EVENT_0_RIGHT 	| EVENT_1_FLAT  	| EVENT_2_LEFT		| EVENT_3_RIGHT;
	static final int EVENT_0123_LFLR = EVENT_0_LEFT 	| EVENT_1_FLAT  	| EVENT_2_LEFT		| EVENT_3_RIGHT;
	static final int EVENT_0123_FFLR = EVENT_0_FLAT 	| EVENT_1_FLAT  	| EVENT_2_LEFT		| EVENT_3_RIGHT;

	static final int EVENT_0123_LRFR = EVENT_0_LEFT 	| EVENT_1_RIGHT  	| EVENT_2_FLAT		| EVENT_3_RIGHT;
	static final int EVENT_0123_RRFR = EVENT_0_RIGHT 	| EVENT_1_RIGHT  	| EVENT_2_FLAT		| EVENT_3_RIGHT;
	static final int EVENT_0123_FRFR = EVENT_0_FLAT 	| EVENT_1_RIGHT  	| EVENT_2_FLAT		| EVENT_3_RIGHT;
	static final int EVENT_0123_RLFR = EVENT_0_RIGHT 	| EVENT_1_LEFT  	| EVENT_2_FLAT		| EVENT_3_RIGHT;
	static final int EVENT_0123_LLFR = EVENT_0_LEFT 	| EVENT_1_LEFT  	| EVENT_2_FLAT		| EVENT_3_RIGHT;
	static final int EVENT_0123_FLFR = EVENT_0_FLAT 	| EVENT_1_LEFT  	| EVENT_2_FLAT		| EVENT_3_RIGHT;
	static final int EVENT_0123_RFFR = EVENT_0_RIGHT 	| EVENT_1_FLAT  	| EVENT_2_FLAT		| EVENT_3_RIGHT;
	static final int EVENT_0123_LFFR = EVENT_0_LEFT 	| EVENT_1_FLAT  	| EVENT_2_FLAT		| EVENT_3_RIGHT;
	static final int EVENT_0123_FFFR = EVENT_0_FLAT 	| EVENT_1_FLAT  	| EVENT_2_FLAT		| EVENT_3_RIGHT;

	static final int EVENT_0123_RRRL = EVENT_0_RIGHT 	| EVENT_1_RIGHT  	| EVENT_2_RIGHT		| EVENT_3_LEFT;
	static final int EVENT_0123_LRRL = EVENT_0_LEFT 	| EVENT_1_RIGHT  	| EVENT_2_RIGHT		| EVENT_3_LEFT;
	static final int EVENT_0123_FRRL = EVENT_0_FLAT 	| EVENT_1_RIGHT  	| EVENT_2_RIGHT		| EVENT_3_LEFT;
	static final int EVENT_0123_RLRL = EVENT_0_RIGHT 	| EVENT_1_LEFT  	| EVENT_2_RIGHT		| EVENT_3_LEFT;
	static final int EVENT_0123_LLRL = EVENT_0_LEFT 	| EVENT_1_LEFT  	| EVENT_2_RIGHT		| EVENT_3_LEFT;
	static final int EVENT_0123_FLRL = EVENT_0_FLAT 	| EVENT_1_LEFT  	| EVENT_2_RIGHT		| EVENT_3_LEFT;
	static final int EVENT_0123_RFRL = EVENT_0_RIGHT 	| EVENT_1_FLAT  	| EVENT_2_RIGHT		| EVENT_3_LEFT;
	static final int EVENT_0123_LFRL = EVENT_0_LEFT 	| EVENT_1_FLAT  	| EVENT_2_RIGHT		| EVENT_3_LEFT;
	static final int EVENT_0123_FFRL = EVENT_0_FLAT 	| EVENT_1_FLAT  	| EVENT_2_RIGHT		| EVENT_3_LEFT;

	static final int EVENT_0123_RRLL = EVENT_0_RIGHT 	| EVENT_1_RIGHT  	| EVENT_2_LEFT		| EVENT_3_LEFT;
	static final int EVENT_0123_LRLL = EVENT_0_LEFT 	| EVENT_1_RIGHT 	| EVENT_2_LEFT		| EVENT_3_LEFT;
	static final int EVENT_0123_FRLL = EVENT_0_FLAT 	| EVENT_1_RIGHT  	| EVENT_2_LEFT		| EVENT_3_LEFT;
	static final int EVENT_0123_RLLL = EVENT_0_RIGHT 	| EVENT_1_LEFT  	| EVENT_2_LEFT		| EVENT_3_LEFT;
	static final int EVENT_0123_LLLL = EVENT_0_LEFT 	| EVENT_1_LEFT  	| EVENT_2_LEFT		| EVENT_3_LEFT;
	static final int EVENT_0123_FLLL = EVENT_0_FLAT 	| EVENT_1_LEFT  	| EVENT_2_LEFT		| EVENT_3_LEFT;
	static final int EVENT_0123_RFLL = EVENT_0_RIGHT 	| EVENT_1_FLAT  	| EVENT_2_LEFT		| EVENT_3_LEFT;
	static final int EVENT_0123_LFLL = EVENT_0_LEFT 	| EVENT_1_FLAT  	| EVENT_2_LEFT		| EVENT_3_LEFT;
	static final int EVENT_0123_FFLL = EVENT_0_FLAT 	| EVENT_1_FLAT  	| EVENT_2_LEFT		| EVENT_3_LEFT;

	static final int EVENT_0123_LRFL = EVENT_0_LEFT 	| EVENT_1_RIGHT  	| EVENT_2_FLAT		| EVENT_3_LEFT;
	static final int EVENT_0123_RRFL = EVENT_0_RIGHT 	| EVENT_1_RIGHT  	| EVENT_2_FLAT		| EVENT_3_LEFT;
	static final int EVENT_0123_FRFL = EVENT_0_FLAT 	| EVENT_1_RIGHT  	| EVENT_2_FLAT		| EVENT_3_LEFT;
	static final int EVENT_0123_RLFL = EVENT_0_RIGHT 	| EVENT_1_LEFT  	| EVENT_2_FLAT		| EVENT_3_LEFT;
	static final int EVENT_0123_LLFL = EVENT_0_LEFT 	| EVENT_1_LEFT  	| EVENT_2_FLAT		| EVENT_3_LEFT;
	static final int EVENT_0123_FLFL = EVENT_0_FLAT 	| EVENT_1_LEFT  	| EVENT_2_FLAT		| EVENT_3_LEFT;
	static final int EVENT_0123_RFFL = EVENT_0_RIGHT 	| EVENT_1_FLAT  	| EVENT_2_FLAT		| EVENT_3_LEFT;
	static final int EVENT_0123_LFFL = EVENT_0_LEFT 	| EVENT_1_FLAT  	| EVENT_2_FLAT		| EVENT_3_LEFT;
	static final int EVENT_0123_FFFL = EVENT_0_FLAT 	| EVENT_1_FLAT  	| EVENT_2_FLAT		| EVENT_3_LEFT;

	static final int EVENT_0123_RRRF = EVENT_0_RIGHT 	| EVENT_1_RIGHT  	| EVENT_2_RIGHT		| EVENT_3_FLAT;
	static final int EVENT_0123_LRRF = EVENT_0_LEFT 	| EVENT_1_RIGHT  	| EVENT_2_RIGHT		| EVENT_3_FLAT;
	static final int EVENT_0123_FRRF = EVENT_0_FLAT 	| EVENT_1_RIGHT  	| EVENT_2_RIGHT		| EVENT_3_FLAT;
	static final int EVENT_0123_RLRF = EVENT_0_RIGHT 	| EVENT_1_LEFT  	| EVENT_2_RIGHT		| EVENT_3_FLAT;
	static final int EVENT_0123_LLRF = EVENT_0_LEFT 	| EVENT_1_LEFT  	| EVENT_2_RIGHT		| EVENT_3_FLAT;
	static final int EVENT_0123_FLRF = EVENT_0_FLAT 	| EVENT_1_LEFT  	| EVENT_2_RIGHT		| EVENT_3_FLAT;
	static final int EVENT_0123_RFRF = EVENT_0_RIGHT 	| EVENT_1_FLAT  	| EVENT_2_RIGHT		| EVENT_3_FLAT;
	static final int EVENT_0123_LFRF = EVENT_0_LEFT 	| EVENT_1_FLAT  	| EVENT_2_RIGHT		| EVENT_3_FLAT;
	static final int EVENT_0123_FFRF = EVENT_0_FLAT 	| EVENT_1_FLAT  	| EVENT_2_RIGHT		| EVENT_3_FLAT;

	static final int EVENT_0123_RRLF = EVENT_0_RIGHT 	| EVENT_1_RIGHT  	| EVENT_2_LEFT		| EVENT_3_FLAT;
	static final int EVENT_0123_LRLF = EVENT_0_LEFT 	| EVENT_1_RIGHT 	| EVENT_2_LEFT		| EVENT_3_FLAT;
	static final int EVENT_0123_FRLF = EVENT_0_FLAT 	| EVENT_1_RIGHT  	| EVENT_2_LEFT		| EVENT_3_FLAT;
	static final int EVENT_0123_RLLF = EVENT_0_RIGHT 	| EVENT_1_LEFT  	| EVENT_2_LEFT		| EVENT_3_FLAT;
	static final int EVENT_0123_LLLF = EVENT_0_LEFT 	| EVENT_1_LEFT  	| EVENT_2_LEFT		| EVENT_3_FLAT;
	static final int EVENT_0123_FLLF = EVENT_0_FLAT 	| EVENT_1_LEFT  	| EVENT_2_LEFT		| EVENT_3_FLAT;
	static final int EVENT_0123_RFLF = EVENT_0_RIGHT 	| EVENT_1_FLAT  	| EVENT_2_LEFT		| EVENT_3_FLAT;
	static final int EVENT_0123_LFLF = EVENT_0_LEFT 	| EVENT_1_FLAT  	| EVENT_2_LEFT		| EVENT_3_FLAT;
	static final int EVENT_0123_FFLF = EVENT_0_FLAT 	| EVENT_1_FLAT  	| EVENT_2_LEFT		| EVENT_3_FLAT;

	static final int EVENT_0123_LRFF = EVENT_0_LEFT 	| EVENT_1_RIGHT  	| EVENT_2_FLAT		| EVENT_3_FLAT;
	static final int EVENT_0123_RRFF = EVENT_0_RIGHT 	| EVENT_1_RIGHT  	| EVENT_2_FLAT		| EVENT_3_FLAT;
	static final int EVENT_0123_FRFF = EVENT_0_FLAT 	| EVENT_1_RIGHT  	| EVENT_2_FLAT		| EVENT_3_FLAT;
	static final int EVENT_0123_RLFF = EVENT_0_RIGHT 	| EVENT_1_LEFT  	| EVENT_2_FLAT		| EVENT_3_FLAT;
	static final int EVENT_0123_LLFF = EVENT_0_LEFT 	| EVENT_1_LEFT  	| EVENT_2_FLAT		| EVENT_3_FLAT;
	static final int EVENT_0123_FLFF = EVENT_0_FLAT 	| EVENT_1_LEFT  	| EVENT_2_FLAT		| EVENT_3_FLAT;
	static final int EVENT_0123_RFFF = EVENT_0_RIGHT 	| EVENT_1_FLAT  	| EVENT_2_FLAT		| EVENT_3_FLAT;
	static final int EVENT_0123_LFFF = EVENT_0_LEFT 	| EVENT_1_FLAT  	| EVENT_2_FLAT		| EVENT_3_FLAT;
	static final int EVENT_0123_FFFF = EVENT_0_FLAT 	| EVENT_1_FLAT  	| EVENT_2_FLAT		| EVENT_3_FLAT;

	static final int PV_PX = 0;
	static final int PV_PY = 1;
	static final int PV_X = 2;
	static final int PV_Y = 3;
	static final int PV_Z = 4;
	static final int PV_W = 5;

	static final int PROJECTED_VERTEX_STRIDE = 6;

	static final int V000 = 0;
	static final int V001 = V000 + PROJECTED_VERTEX_STRIDE;
	static final int V010 = V001 + PROJECTED_VERTEX_STRIDE;
	static final int V011 = V010 + PROJECTED_VERTEX_STRIDE;
	static final int V100 = V011 + PROJECTED_VERTEX_STRIDE;
	static final int V101 = V100 + PROJECTED_VERTEX_STRIDE;
	static final int V110 = V101 + PROJECTED_VERTEX_STRIDE;
	static final int V111 = V110 + PROJECTED_VERTEX_STRIDE;

	static final int X0 = 0;
	static final int Y0 = 1;
	static final int X1 = 2;
	static final int Y1 = 3;

	static final int AX0 = V111 + 1;
	static final int AY0 = AX0 + 1;
	static final int AX1 = AY0 + 1;
	static final int AY1 = AX1 + 1;

	static final int BX0 = AY1 + 1;
	static final int BY0 = BX0 + 1;
	static final int BX1 = BY0 + 1;
	static final int BY1 = BX1 + 1;

	static final int CX0 = BY1 + 1;
	static final int CY0 = CX0 + 1;
	static final int CX1 = CY0 + 1;
	static final int CY1 = CX1 + 1;

	static final int DX0 = CY1 + 1;
	static final int DY0 = DX0 + 1;
	static final int DX1 = DY0 + 1;
	static final int DY1 = DX1 + 1;

	static final int VERTEX_DATA_LENGTH = DY1 + 1;

	public static final int UP = 1 << Direction.UP.getId();
	public static final int DOWN = 1 << Direction.DOWN.getId();
	public static final int EAST = 1 << Direction.EAST.getId();
	public static final int WEST = 1 << Direction.WEST.getId();
	public static final int NORTH = 1 << Direction.NORTH.getId();
	public static final int SOUTH = 1 << Direction.SOUTH.getId();


	static final int IDX_EVENTS = 0;
	static final int EVENTS_LENGTH = PIXEL_HEIGHT * 2;
	static final int IDX_VERTEX_DATA = IDX_EVENTS + EVENTS_LENGTH;

	// Boumds of current triangle - pixel coordinates
	static final int IDX_MIN_PIX_X = IDX_VERTEX_DATA + VERTEX_DATA_LENGTH;
	static final int IDX_MIN_PIX_Y = IDX_MIN_PIX_X + 1;
	static final int IDX_MAX_PIX_X = IDX_MIN_PIX_Y + 1;
	static final int IDX_MAX_PIX_Y = IDX_MAX_PIX_X + 1;

	static final int IDX_CLIP_X = IDX_MAX_PIX_Y + 1;
	static final int IDX_CLIP_Y = IDX_CLIP_X + 1;

	static final int IDX_POS0 = IDX_CLIP_Y + 1;
	static final int IDX_POS1 = IDX_POS0 + 1;
	static final int IDX_POS2 = IDX_POS1 + 1;
	static final int IDX_POS3 = IDX_POS2 + 1;

	static final int IDX_AX0 = IDX_POS3 + 1;
	static final int IDX_AY0 = IDX_AX0 + 1;
	static final int IDX_AX1 = IDX_AY0 + 1;
	static final int IDX_AY1 = IDX_AX1 + 1;

	static final int IDX_BX0 = IDX_AY1 + 1;
	static final int IDX_BY0 = IDX_BX0 + 1;
	static final int IDX_BX1 = IDX_BY0 + 1;
	static final int IDX_BY1 = IDX_BX1 + 1;

	static final int IDX_CX0 = IDX_BY1 + 1;
	static final int IDX_CY0 = IDX_CX0 + 1;
	static final int IDX_CX1 = IDX_CY0 + 1;
	static final int IDX_CY1 = IDX_CX1 + 1;

	static final int IDX_DX0 = IDX_CY1 + 1;
	static final int IDX_DY0 = IDX_DX0 + 1;
	static final int IDX_DX1 = IDX_DY0 + 1;
	static final int IDX_DY1 = IDX_DX1 + 1;

	static final int IDX_MIN_TILE_ORIGIN_X = IDX_DY1 + 1;
	static final int IDX_MAX_TILE_ORIGIN_X = IDX_MIN_TILE_ORIGIN_X + 1;
	static final int IDX_MAX_TILE_ORIGIN_Y = IDX_MAX_TILE_ORIGIN_X + 1;

	static final int IDX_TILE_INDEX = IDX_MAX_TILE_ORIGIN_Y + 1;
	static final int IDX_TILE_ORIGIN_X = IDX_TILE_INDEX + 1;
	static final int IDX_TILE_ORIGIN_Y = IDX_TILE_ORIGIN_X + 1;

	static final int IDX_SAVE_TILE_INDEX = IDX_TILE_ORIGIN_Y + 1;
	static final int IDX_SAVE_TILE_ORIGIN_X = IDX_SAVE_TILE_INDEX + 1;
	static final int IDX_SAVE_TILE_ORIGIN_Y = IDX_SAVE_TILE_ORIGIN_X + 1;

	static final int DATA_LENGTH = IDX_SAVE_TILE_ORIGIN_Y + 1;

	// For abandoned traversal scheme
	//	static final int MAX_TILE_X = TILE_WIDTH - 1;
	//	static final int MAX_TILE_ORIGIN_Y = PIXEL_HEIGHT - TILE_PIXEL_DIAMETER;
	//	static final int MAX_TILE_ORIGIN_X = PIXEL_WIDTH - TILE_PIXEL_DIAMETER;
	//	static final long TILE_MASK_UP = 0xFF00000000000000L;
	//	static final long TILE_MASK_LEFT = 0x0101010101010101L;
	//	static final long TILE_MASK_RIGHT = 0x8080808080808080L;
}
