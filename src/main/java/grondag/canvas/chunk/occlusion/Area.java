package grondag.canvas.chunk.occlusion;

import com.google.common.base.Strings;

public class Area {
	public final int areaKey;
	public final int x0;
	public final int y0;
	public final int x1;
	public final int y1;
	public final int areaSize;
	public final int edgeCount;
	public final long areaHash;

	private final long[] bits = new long[4];

	boolean isIncludedBySample(long[] sample, int sampleStart) {
		final long[] myBits = bits;

		return AreaUtil.sampleIncludes(myBits[0], sample[sampleStart])
				&& AreaUtil.sampleIncludes(myBits[1], sample[sampleStart + 1])
				&& AreaUtil.sampleIncludes(myBits[2], sample[sampleStart + 2])
				&& AreaUtil.sampleIncludes(myBits[3], sample[sampleStart + 3]);
	}

	public boolean intersects(Area other) {
		final long[] myBits = bits;
		final long[] otherBits = other.bits;
		return (myBits[0] & otherBits[0]) != 0
				|| (myBits[1] & otherBits[1]) != 0
				|| (myBits[2] & otherBits[2]) != 0
				|| (myBits[3] & otherBits[3]) != 0;
	}

	public boolean matchesHash(long hash) {
		return (areaHash & hash) == areaHash;
	}

	public Area(int rectKey) {
		areaKey = rectKey;
		x0 = rectKey & 31;
		y0 = (rectKey >> 5) & 31;
		x1 = (rectKey >> 10) & 31;
		y1 = (rectKey >> 15) & 31;

		final int x = x1 - x0 + 1;
		final int y = y1 - y0 + 1;
		areaSize = x * y;
		edgeCount =  x +  y;

		populateBits();

		//			printShape();

		areaHash = AreaUtil.areaHash(bits);

		//			printHash();
	}

	private void populateBits() {
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				final int key = (y << 4) | x;
				bits[key >> 6] |= (1L << (key & 63));
			}
		}
	}

	public void printHash() {
		final String s = Strings.padStart(Long.toBinaryString(areaHash), 64, '0');
		System.out.println(s.substring(0, 8));
		System.out.println(s.substring(8, 16));
		System.out.println(s.substring(16, 24));
		System.out.println(s.substring(24, 32));
		System.out.println(s.substring(32, 40));
		System.out.println(s.substring(40, 48));
		System.out.println(s.substring(48, 56));
		System.out.println(s.substring(56, 64));
		System.out.println();
	}

	public void printShape() {
		OcclusionBitPrinter.printShape(bits, 0);
	}
}