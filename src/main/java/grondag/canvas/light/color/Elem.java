package grondag.canvas.light.color;

public enum Elem {
	R(0xF000, 12, 0),
	G(0x0F00, 8, 1),
	B(0x00F0, 4, 2),
	A(0x000F, 0, 3);

	public final int mask;
	public final int shift;
	public final int pos;

	Elem(int mask, int shift, int pos) {
		this.mask = mask;
		this.shift = shift;
		this.pos = pos;
	}

	public int of(short light) {
		return (light >> shift) & 0xF;
	}

	public short replace(short source, short elemLight) {
		return (short) ((source & ~mask) | (elemLight << shift));
	}

	public static short encode(int r, int g, int b, int a) {
		return (short) ((r << R.shift) | (g << G.shift) | (b << B.shift) | (a << A.shift));
	}

	public static short maxRGB(short left, short right, int a) {
		return (short) (Math.max(left & R.mask, right & R.mask) | Math.max(left & G.mask, right & G.mask)
				| Math.max(left & B.mask, right & B.mask) | a);
	}

	public static String text(short light) {
		return "(" + R.of(light) + "," + G.of(light) + "," + B.of(light) + ")";
	}
}
