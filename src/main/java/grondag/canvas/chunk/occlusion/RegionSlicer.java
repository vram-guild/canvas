package grondag.canvas.chunk.occlusion;

import grondag.canvas.chunk.RenderRegionAddressHelper;

/**
 * Copies data from a render region and finds occluding rectangles for each axis.<p>
 *
 * Copied data is rotated for the X and Y axes to make bit-wise operations easier
 */
public class RegionSlicer {
	// We have an extra unit of words on the slicing dimension because we slice
	// on inter-block planes vs through block centers.
	static final int SLICE_WORD_COUNT = 4;
	static final int WORD_COUNT = RenderRegionAddressHelper.INTERIOR_CACHE_WORDS + SLICE_WORD_COUNT;

	public final long[] outputBits = new long[WORD_COUNT];

	long[] inputBits = new long[RenderRegionAddressHelper.INTERIOR_CACHE_WORDS];

	public void copyAxisZ(long[] source, int startIndex) {
		System.arraycopy(source, startIndex, inputBits, 0, RenderRegionAddressHelper.INTERIOR_CACHE_WORDS);
	}

	public void buildAxisZ() {
		System.arraycopy(inputBits, 0, outputBits, 0, SLICE_WORD_COUNT);
		System.arraycopy(inputBits, RenderRegionAddressHelper.INTERIOR_CACHE_WORDS - SLICE_WORD_COUNT, outputBits, RenderRegionAddressHelper.INTERIOR_CACHE_WORDS, SLICE_WORD_COUNT);

		for (int i = 0; i < 15 * SLICE_WORD_COUNT; i++) {
			outputBits[i + SLICE_WORD_COUNT] = inputBits[i] | inputBits[i + SLICE_WORD_COUNT];
		}
	}

	public void copyAxisX(long[] source, int startIndex) {
		/**
		 *
		 * AAA  BBB  CCC
		 * AAA  BBB  CCC
		 * AAA  BBB  CCC
		 *
		 * CCC  CCC  CCC
		 * BBB  BBB  BBB
		 * AAA  AAA  AAA
		 */
		System.arraycopy(source, startIndex, inputBits, 0, RenderRegionAddressHelper.INTERIOR_CACHE_WORDS);
	}

	public void buildAxisX() {
		System.arraycopy(inputBits, 0, outputBits, 0, SLICE_WORD_COUNT);
		System.arraycopy(inputBits, RenderRegionAddressHelper.INTERIOR_CACHE_WORDS - SLICE_WORD_COUNT, outputBits, RenderRegionAddressHelper.INTERIOR_CACHE_WORDS, SLICE_WORD_COUNT);

		for (int i = 0; i < 15 * SLICE_WORD_COUNT; i++) {
			outputBits[i + SLICE_WORD_COUNT] = inputBits[i] | inputBits[i + SLICE_WORD_COUNT];
		}
	}
}
