package grondag.canvas.terrain.occlusion.region.area;

import grondag.canvas.terrain.occlusion.region.OcclusionBitPrinter;

public class Area {
	public final int areaKey;
	public final int areaSize;
	public final int edgeCount;
	public final int index;

	private final long[] bits = new long[4];

	public boolean isIncludedBySample(long[] sample, int sampleStart) {
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

	public boolean intersectsWithSample(long[] sample, int sampleStart) {
		final long[] myBits = bits;
		return (myBits[0] & sample[sampleStart]) != 0
				|| (myBits[1] & sample[++sampleStart]) != 0
				|| (myBits[2] & sample[++sampleStart]) != 0
				|| (myBits[3] & sample[++sampleStart]) != 0;
	}

	public boolean isAdditive(long[] sample, int sampleStart) {
		final long[] myBits = bits;
		return (myBits[0] | sample[sampleStart]) != sample[sampleStart]
				|| (myBits[1] | sample[++sampleStart]) != sample[sampleStart]
						|| (myBits[2] | sample[++sampleStart]) != sample[sampleStart]
								|| (myBits[3] | sample[++sampleStart]) != sample[sampleStart];
	}

	public void setBits(long[] targetBits, int startIndex) {
		targetBits[startIndex] |= bits[0];
		targetBits[++startIndex] |= bits[1];
		targetBits[++startIndex] |= bits[2];
		targetBits[++startIndex] |= bits[3];
	}

	public void clearBits(long[] targetBits, int startIndex) {
		targetBits[startIndex] &= ~bits[0];
		targetBits[++startIndex] &= ~bits[1];
		targetBits[++startIndex] &= ~bits[2];
		targetBits[++startIndex] &= ~bits[3];
	}

	public Area(int rectKey, int index) {
		areaKey = rectKey;
		this.index = index;
		final int x0 = AreaUtil.x0(rectKey);
		final int y0 = AreaUtil.y0(rectKey);
		final int x1 = AreaUtil.x1(rectKey);
		final int y1 = AreaUtil.y1(rectKey);

		final int x = x1 - x0 + 1;
		final int y = y1 - y0 + 1;
		areaSize = x * y;
		edgeCount =  x + y;

		populateBits();
	}

	private void populateBits() {
		final int x0 = AreaUtil.x0(areaKey);
		final int y0 = AreaUtil.y0(areaKey);
		final int x1 = AreaUtil.x1(areaKey);
		final int y1 = AreaUtil.y1(areaKey);

		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				final int key = (y << 4) | x;
				bits[key >> 6] |= (1L << (key & 63));
			}
		}
	}

	public void printShape() {
		OcclusionBitPrinter.printShape(bits, 0);
	}
}