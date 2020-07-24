package grondag.canvas.terrain.occlusion.region;

import static grondag.canvas.terrain.RenderRegionAddressHelper.INTERIOR_CACHE_WORDS;
import static grondag.canvas.terrain.RenderRegionAddressHelper.SLICE_WORD_COUNT;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import grondag.canvas.terrain.occlusion.region.area.Area;
import grondag.canvas.terrain.occlusion.region.area.AreaFinder;
import grondag.canvas.terrain.occlusion.region.area.AreaUtil;

/**
 * Copies data from a render region and finds occluding rectangles for each axis.<p>
 *
 * Copied data is rotated for the X and Y axes to make bit-wise operations easier
 */
public class PlaneFinder {
	// We have an extra unit of words on the slicing dimension because we slice
	// on inter-block planes vs through block centers.
	static final int WORD_COUNT = INTERIOR_CACHE_WORDS + SLICE_WORD_COUNT;

	static final int MIDDLE_PLANE = SLICE_WORD_COUNT *  8;

	public final long[] outputBits = new long[WORD_COUNT];

	public final long[] fillBits = new long[SLICE_WORD_COUNT];

	private final long[] inputBits = new long[INTERIOR_CACHE_WORDS];

	public final AreaFinder areaFinder;

	public final IntArrayList planes  = new IntArrayList();

	public PlaneFinder(AreaFinder areaFinder) {
		this.areaFinder = areaFinder;
	}

	public void buildAxisZ(long[] source, int startIndex) {
		System.arraycopy(source, startIndex, inputBits, 0, INTERIOR_CACHE_WORDS);

		build();
	}

	private void build() {
		System.arraycopy(inputBits, 0, outputBits, 0, SLICE_WORD_COUNT);
		System.arraycopy(inputBits, INTERIOR_CACHE_WORDS - SLICE_WORD_COUNT, outputBits, INTERIOR_CACHE_WORDS, SLICE_WORD_COUNT);

		for (int i = 0; i < 15 * SLICE_WORD_COUNT; i++) {
			outputBits[i + SLICE_WORD_COUNT] = inputBits[i] | inputBits[i + SLICE_WORD_COUNT];
		}
	}

	private static final long X_MASK = 0x0001000100010001L;

	public void buildAxisX(long[] source, int startIndex) {

		final long[] input = inputBits;

		long mask = X_MASK;

		for (int i = 0; i < 16; i++) {
			long a = 0;
			long b = 0;
			long c = 0;
			long d = 0;

			for (int j = 0; j < 16; j++) {
				final int jIndex = j << 2;
				final int offset = j - i;

				a |= shift(offset, (source[jIndex] & mask));
				b |= shift(offset, (source[jIndex + 1] & mask));
				c |= shift(offset, (source[jIndex + 2] & mask));
				d |= shift(offset, (source[jIndex + 3] & mask));
			}

			final int iIndex = i << 2;

			input[iIndex]= a;
			input[iIndex + 1]= b;
			input[iIndex + 2]= c;
			input[iIndex + 3]= d;

			mask <<= 1;
		}

		build();
	}

	private static final long Y_MASK = 0xFFFFL;

	public void buildAxisY(long[] source, int startIndex) {
		final long[] input = inputBits;

		for (int y = 0; y < 16; y++) {
			final long ySrcShift =  (y & 3) << 4;
			final int yTargetOffset = y << 2;
			final int ySrcOffset = y >>> 2;

			for (int zWord = 0; zWord < 4; zWord++) {
				final int zSrcOffset = ySrcOffset + (zWord << 4);
				long zResult = (source[zSrcOffset] >>> ySrcShift) & Y_MASK;
				zResult |= ((source[zSrcOffset + 4] >>> ySrcShift) & Y_MASK) << 16;
				zResult |= ((source[zSrcOffset + 8] >>> ySrcShift) & Y_MASK) << 32;
				zResult |= ((source[zSrcOffset + 12] >>> ySrcShift) &  Y_MASK) << 48;
				input[yTargetOffset + zWord] = zResult;
			}
		}

		build();
	}

	private static long shift(int offset, long bits) {
		if (offset < 0) {
			return bits >>>= -offset;
		} else if (offset > 0) {
			return bits <<= offset;
		} else {
			return bits;
		}
	}

	public void findPlanes(long[] bits, int sourceIndex) {
		planes.clear();

		buildAxisZ(bits, sourceIndex);
		findPlanes(Z_FUNC);

		buildAxisX(bits, sourceIndex);
		findPlanes(X_FUNC);

		buildAxisY(bits, sourceIndex);
		findPlanes(Y_FUNC);
	}

	@FunctionalInterface
	private interface PlaneFunction {
		int apply(int areaKey, int d);
	}

	private static final PlaneFunction Z_FUNC = (a, d) -> PackedBox.pack(AreaUtil.x0(a), AreaUtil.y0(a), d, AreaUtil.x1(a) + 1, AreaUtil.y1(a) + 1, d, 0);
	private static final PlaneFunction X_FUNC = (a, d) -> PackedBox.pack(d, AreaUtil.y0(a), AreaUtil.x0(a), d, AreaUtil.y1(a) + 1, AreaUtil.x1(a) + 1, 0);
	private static final PlaneFunction Y_FUNC = (a, d) -> PackedBox.pack(AreaUtil.x0(a), d, AreaUtil.y0(a), AreaUtil.x1(a) + 1, d, AreaUtil.y1(a) + 1, 0);

	private void findPlanes(PlaneFunction planeFunc) {
		final long[] fillBits =  this.fillBits;
		fillBits[0] = 0;
		fillBits[1] = 0;
		fillBits[2] = 0;
		fillBits[3] = 0;
		final AreaFinder areas = areaFinder;
		int openCount = 256;

		for (int i = 0; i < AreaFinder.AREA_COUNT; ++i) {
			final Area a = areas.get(i);

			if (a.areaSize < 16) {
				return;
			}

			if (a.intersectsWithSample(fillBits, 0)) {
				continue;
			}

			int d = -1;

			if (a.isIncludedBySample(outputBits, MIDDLE_PLANE)) {
				d = 8;
			} else {
				int indexOffset = SLICE_WORD_COUNT;

				for (int offset = 1; offset <= 8; ++offset) {
					if (a.isIncludedBySample(outputBits, MIDDLE_PLANE + indexOffset)) {
						d = 8 + offset;
						break;
					} else if (a.isIncludedBySample(outputBits, MIDDLE_PLANE - indexOffset)) {
						d = 8 - offset;
						break;
					}

					indexOffset += SLICE_WORD_COUNT;
				}
			}

			if (d != -1) {
				planes.add(planeFunc.apply(a.areaKey, d));
				openCount -= a.areaSize;

				if (openCount < 16) {
					return;
				}

				a.setBits(fillBits, 0);
			}
		}
	}
}
