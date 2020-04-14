package grondag.canvas.chunk.occlusion.region;

public class AreaSample {
	public final long[] bits = new long[4];

	public void fill(int x0, int y0, int x1, int y1) {
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				final int key = (y << 4) | x;
				bits[key >> 6] |= (1L << (key & 63));
			}
		}
	}

	public void remove(Area r) {
		r.clearBits(bits, 0);
	}

	public void fill(Area r) {
		r.setBits(bits, 0);
	}

	void clear() {
		bits[0] = 0;
		bits[1] = 0;
		bits[2] = 0;
		bits[3] = 0;
	}
}