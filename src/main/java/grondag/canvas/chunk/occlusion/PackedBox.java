package grondag.canvas.chunk.occlusion;

import grondag.fermion.varia.Useful;

public abstract class PackedBox {
	private PackedBox() { }

	private static final int SHIFT_X0 = 0;
	private static final int SHIFT_Y0 = SHIFT_X0 + 4;
	private static final int SHIFT_Z0 = SHIFT_Y0 + 4;
	private static final int SHIFT_X1 = SHIFT_Z0 + 4;
	private static final int SHIFT_Y1 = SHIFT_X1 + 5;
	private static final int SHIFT_Z1 = SHIFT_Y1 + 5;
	private static final int SHIFT_RANGE = SHIFT_Z1 + 5;

	public static final int OCCLUSION_RANGE_NEAR = 0;
	public static final int OCCLUSION_RANGE_MEDIUM = 1;
	public static final int OCCLUSION_RANGE_FAR = 2;

	public static int pack(int x0, int y0, int z0, int x1, int y1, int z1) {
		assert x0 >= 0 && x0 < 16;
		assert y0 >= 0 && y0 < 16;
		assert z0 >= 0 && z0 < 16;
		assert x1 >= 0 && x1 <= 16;
		assert y1 >= 0 && y1 <= 16;
		assert z1 >= 0 && z1 <= 16;

		return x0 | (y0 << SHIFT_Y0) | (z0 << SHIFT_Z0)
				| ((x1) << SHIFT_X1) | ((y1) << SHIFT_Y1) | ((z1) << SHIFT_Z1)
				| (computeOcclusionRange(x0, y0, z0, x1, y1, z1) << SHIFT_RANGE);
	}

	public static long packSortable(int x0, int y0, int z0, int x1, int y1, int z1) {
		long result  = computeOccludingPower(x0, y0, z0, x1, y1, z1);
		result <<= 32;
		result |= pack(x0, y0, z0, x1, y1, z1);

		return result;
	}

	public static int computeOccludingPower(int x0, int y0, int z0, int x1, int y1, int z1) {
		final int dx = x1 - x0;
		final int dy = y1 - y0;
		final int dz = z1 - z0;

		return dx * dy + dz * dy + dx * dz;
	}

	public static int computeOcclusionRange(int x0, int y0, int z0, int x1, int y1, int z1) {
		final int dx = x1 - x0;
		final int dy = y1 - y0;
		final int dz = z1 - z0;

		final int area = Useful.max(dx * dy, dz * dy, dx * dz);

		return area <= 4 ? OCCLUSION_RANGE_NEAR : area <= 9 ? OCCLUSION_RANGE_MEDIUM :  OCCLUSION_RANGE_FAR;
	}

	public static int x0(int packed) {
		return (packed & 15);
	}

	public static int y0(int packed) {
		return (packed >>> SHIFT_Y0) & 15;
	}

	public static int z0(int packed) {
		return (packed >>> SHIFT_Z0) & 15;
	}

	public static int x1(int packed) {
		return ((packed >>> SHIFT_X1) & 31);
	}

	public static int y1(int packed) {
		return ((packed >>> SHIFT_Y1) & 31);
	}

	public static int z1(int packed) {
		return ((packed >>> SHIFT_Z1) & 31);
	}

	public static int range(int packed) {
		return ((packed >>> SHIFT_RANGE) & 3);
	}

	public static String toString(int packed) {
		return "(" + x0(packed) + ", " + y0(packed) + ", " + z0(packed) + "), ("
				+ x1(packed) + ", " + y1(packed) + ", " + z1(packed) + ")";
	}

	public static final int FULL_BOX = pack(0, 0, 0, 16, 16, 16);

	public static final int EMPTY_BOX = pack(0, 0, 0, 0, 0, 0);
}
