package grondag.canvas.chunk;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.fermion.bits.BitPacker32;

@Environment(EnvType.CLIENT)
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class RegionOcclusionData {

	public static final BitPacker32 PACKER = new BitPacker32(null, null);
	public static final BitPacker32.BooleanElement SAME_AS_VISIBLE_FLAG = PACKER.createBooleanElement();
	public static final BitPacker32.BooleanElement FULL_CHUNK_VISIBLE_FLAG = PACKER.createBooleanElement();
	public static final BitPacker32.BooleanElement EMPTY_CHUNK_FLAG = PACKER.createBooleanElement();
	public static final BitPacker32.IntElement OCCLUDER_COUNT = PACKER.createIntElement(64);

	public static int[] EMPTY_DATA = new int[7];

	static {
		isEmptyChunk(EMPTY_DATA, true);
	}

	public static final int X0 = 0;
	public static final int Y0 = 1;
	public static final int Z0 = 2;
	public static final int X1 = 3;
	public static final int Y1 = 4;
	public static final int Z1 = 5;
	public static final int BITS = 6;

	public static boolean sameAsVisible(int[] data) {
		return SAME_AS_VISIBLE_FLAG.getValue(data[BITS]);
	}

	public static boolean isFullChunkVisible(int[] data) {
		return FULL_CHUNK_VISIBLE_FLAG.getValue(data[BITS]);
	}

	public static void sameAsVisible(int[] data, boolean val) {
		data[BITS] = SAME_AS_VISIBLE_FLAG.setValue(val, data[BITS]);
	}

	public static void isFullChunkVisible(int[] data, boolean val) {
		data[BITS] = FULL_CHUNK_VISIBLE_FLAG.setValue(val, data[BITS]);
	}

	/**
	 * Layout
	 * 0-5: visible bounds
	 * 6: bits, use static functions
	 * 7+ at 7-int stride:
	 * 	n + 0: volume of occluder (largest will be first)
	 *  n + 1-6: bounds of occluder
	 */
	//	private static final int DIRECTION_COUNT = Direction.values().length;
	//	private final BitSet visibility;

	private RegionOcclusionData() {
	}

	public static boolean isEmptyChunk(int[] data) {
		return EMPTY_CHUNK_FLAG.getValue(data[BITS]);
	}

	public static void isEmptyChunk(int[] data, boolean val) {
		data[BITS] = EMPTY_CHUNK_FLAG.setValue(val, data[BITS]);
	}
}
