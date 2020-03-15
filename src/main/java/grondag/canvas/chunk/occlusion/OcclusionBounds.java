package grondag.canvas.chunk.occlusion;

public abstract class OcclusionBounds {
	private OcclusionBounds() {}

	public static final int FACE_DOWN = 0;
	public static final int FACE_UP = 1;
	public static final int FACE_NORTH = 2;
	public static final int FACE_SOUTH = 3;
	public static final int FACE_WEST = 4;
	public static final int FACE_EAST = 5;

	private static final int SHIFT_U0 = 0;
	private static final int SHIFT_V0 = SHIFT_U0 + 4;
	private static final int SHIFT_U1 = SHIFT_V0 + 4;
	private static final int SHIFT_V1 = SHIFT_U1 + 4;
	private static final int SHIFT_DEPTH = SHIFT_V1 + 4;
	private static final int SHIFT_SIZE = SHIFT_DEPTH + 5;
	private static final int SHIFT_FACE = SHIFT_SIZE + 8;

	/**
	 *
	 * @param face 0 to 5
	 * @param depth 0 to 15
	 * @param u0 0 to 15
	 * @param v0 0 to 15
	 * @param u1 1 to 16
	 * @param v1 1 to 16
	 * @return packed occlusion face
	 */
	public static int pack(int face, int depth, int u0, int v0, int u1, int v1) {
		assert face >= 0 && face < 6;
		assert depth >= 0 && depth < 16;
		assert u0 >= 0 && u0 < 16;
		assert v0 >= 0 && v0 < 16;
		assert u1 > 0 && u1 <= 16;
		assert v1 > 0 && v1 <= 16;

		return u0 | (v0 << SHIFT_V0)
				| ((u1 - 1) << SHIFT_U1) | ((v1 - 1) << SHIFT_V1)
				| (face << SHIFT_FACE)
				| (depth << SHIFT_DEPTH)
				| (((u1 - u0) * (v1 - v0) - 1) << SHIFT_SIZE);
	}

	public static int face(int packed) {
		return (packed >>> SHIFT_FACE) & 7;
	}

	public static int depth(int packed) {
		return (packed >>> SHIFT_DEPTH) & 15;
	}

	public static int u0(int packed) {
		return (packed & 15);
	}

	public static int v0(int packed) {
		return (packed >>> SHIFT_V0) & 15;
	}

	public static int u1(int packed) {
		return ((packed >>> SHIFT_U1) & 15) + 1;
	}

	public static int v1(int packed) {
		return ((packed >>> SHIFT_V1) & 15) + 1;
	}

	public static int size(int packed) {
		return ((packed >>> SHIFT_SIZE) & 255) + 1;
	}
}
