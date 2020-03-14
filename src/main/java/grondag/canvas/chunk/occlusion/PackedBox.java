package grondag.canvas.chunk.occlusion;

public abstract class PackedBox {
	private PackedBox() { }

	private static final int SHIFT_X0 = 0;
	private static final int SHIFT_Y0 = SHIFT_X0 + 4;
	private static final int SHIFT_Z0 = SHIFT_Y0 + 4;
	private static final int SHIFT_X1 = SHIFT_Z0 + 4;
	private static final int SHIFT_Y1 = SHIFT_X1 + 4;
	private static final int SHIFT_Z1 = SHIFT_Y1 + 4;


	public static int pack(int x0, int y0, int z0, int x1, int y1, int z1) {
		assert x0 >= 0 && x0 < 16;
		assert y0 >= 0 && y0 < 16;
		assert z0 >= 0 && z0 < 16;
		assert x1 > 0 && x1 <= 16;
		assert y1 > 0 && y1 <= 16;
		assert z1 > 0 && z1 <= 16;

		return x0 | (y0 << SHIFT_Y0) | (z0 << SHIFT_Z0)
				| ((x1 - 1) << SHIFT_X1) | ((y1 - 1) << SHIFT_Y1) | ((z1 - 1) << SHIFT_Z1);
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
		return ((packed >>> SHIFT_X1) & 15) + 1;
	}

	public static int y1(int packed) {
		return ((packed >>> SHIFT_Y1) & 15) + 1;
	}

	public static int z1(int packed) {
		return ((packed >>> SHIFT_Z1) & 15) + 1;
	}

	public static final int FULL_BOX = pack(0, 0, 0, 16, 16, 16);

	/** CANNOT BE DECODED */
	public static final int EMPTY_BOX = -1;
}
