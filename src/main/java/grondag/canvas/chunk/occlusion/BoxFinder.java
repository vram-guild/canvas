package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.RenderRegionAddressHelper.INTERIOR_CACHE_WORDS;
import static grondag.canvas.chunk.RenderRegionAddressHelper.SLICE_WORD_COUNT;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BoxFinder {
	final long[] combined = new long[COMBINED_WORD_COUNT];
	final long[] filled = new long[INTERIOR_CACHE_WORDS];

	private final LongArrayList sortedBoxes = new LongArrayList();
	public final IntArrayList boxes = new IntArrayList();

	public final AreaFinder areaFinder = new AreaFinder();

	public void findBoxes(long[] sourceBits, int sourceIndex) {
		final AreaFinder areaFinder = this.areaFinder;
		final ObjectArrayList<Area> areas = areaFinder.areas;
		final long[] combined = this.combined;
		final LongArrayList sortedBoxes = this.sortedBoxes;
		sortedBoxes.clear();

		loadCombined(sourceBits, sourceIndex);
		int priorOffset = SLICE_OFFSET[16];

		areaFinder.find(combined, priorOffset);

		if (!areas.isEmpty()) {
			for (final Area area : areas) {
				sortedBoxes.add(PackedBox.packSortable(area.x0, area.y0, 0, area.x1 + 1, area.y1 + 1, 16));
			}
		}

		for (int depth = 15; depth >= 1; depth--) {
			final int thisOffset = SLICE_OFFSET[depth];
			final int sliceCount = sliceCount(depth);

			for (int slice = 0; slice < sliceCount; slice++) {
				areaFinder.find(combined, thisOffset + (slice << 2));

				if (!areas.isEmpty()) {
					for (final Area area : areas) {
						if (!includedInAnyParent(area, depth, slice)) {
							sortedBoxes.add(PackedBox.packSortable(area.x0, area.y0, slice, area.x1 + 1, area.y1 + 1, slice + depth));
						}
					}
				}
			}

			priorOffset = thisOffset;
		}

		sortedBoxes.sort((a, b) -> Long.compare(b, a));

		final IntArrayList boxes = this.boxes;
		boxes.clear();

		final int limit = sortedBoxes.size();

		if (limit > 0) {
			System.arraycopy(EMPTY_WORDS, 0, filled, 0, INTERIOR_CACHE_WORDS);

			for (int i = 0; i < limit; i++) {
				final int box =  (int) sortedBoxes.getLong(i);

				if (fill(box)) {
					boxes.add(box);
				}
			}
		}
	}

	private final long[] fillSlice = new long[SLICE_WORD_COUNT];

	private boolean fill(int packedBox) {
		final long[] fillSlice = this.fillSlice;
		System.arraycopy(EMPTY_WORDS, 0, fillSlice, 0, SLICE_WORD_COUNT);
		final int x0 = PackedBox.x0(packedBox);
		final int y0 = PackedBox.y0(packedBox);
		final int z0 = PackedBox.z0(packedBox);
		final int x1 = PackedBox.x1(packedBox);
		final int y1 = PackedBox.y1(packedBox);
		final int z1 = PackedBox.z1(packedBox);

		// PERF: optimize XY fill
		for (int x = x0; x < x1; x++) {
			for (int y = y0; y < y1; y++) {
				final int index = x | (y << 4);
				fillSlice[index >> 6] |= (1L << (index & 63));
			}
		}

		boolean didFill = false;
		final long word0 = fillSlice[0];
		final long word1 = fillSlice[1];
		final long word2 = fillSlice[2];
		final long word3 = fillSlice[3];

		for (int z  = z0; z < z1; z++) {
			final int baseIndex = z << 2;
			didFill |= fillWord(word0, baseIndex);
			didFill |= fillWord(word1, baseIndex + 1);
			didFill |= fillWord(word2, baseIndex + 2);
			didFill |= fillWord(word3, baseIndex + 3);
		}

		return didFill;
	}

	private boolean fillWord(long word, int fillIndex) {
		if (word != 0) {
			final long prior = filled[fillIndex];
			final long updated = prior | word;

			if (updated != prior) {
				filled[fillIndex] = updated;
				return true;
			}
		}

		return false;
	}

	private boolean includedInAnyParent(Area area, int childSlideDepth, int childSliceIndex) {
		if (childSliceIndex == 0) {
			return area.isIncludedBySample(combined, SLICE_OFFSET[childSlideDepth + 1]);
		} else if (childSliceIndex == 16 - childSlideDepth) {
			return area.isIncludedBySample(combined, SLICE_OFFSET[childSlideDepth + 1] + ((childSliceIndex - 1) << 2));
		} else {
			final int parentOffset = SLICE_OFFSET[childSlideDepth + 1] + (childSliceIndex << 2);
			return area.isIncludedBySample(combined, parentOffset) || area.isIncludedBySample(combined, parentOffset - 4);
		}
	}

	public void loadCombined(long[] voxels, int sourceIndex) {
		final long[] combined = this.combined;
		System.arraycopy(voxels, 0, combined, 0, INTERIOR_CACHE_WORDS);

		combineTo(combined, D1_0, D1_1, D2_0);
		combineTo(combined, D1_1, D1_2, D2_1);
		combineTo(combined, D1_2, D1_3, D2_2);
		combineTo(combined, D1_3, D1_4, D2_3);
		combineTo(combined, D1_4, D1_5, D2_4);
		combineTo(combined, D1_5, D1_6, D2_5);
		combineTo(combined, D1_6, D1_7, D2_6);
		combineTo(combined, D1_7, D1_8, D2_7);
		combineTo(combined, D1_8, D1_9, D2_8);
		combineTo(combined, D1_9, D1_A, D2_9);
		combineTo(combined, D1_A, D1_B, D2_A);
		combineTo(combined, D1_B, D1_C, D2_B);
		combineTo(combined, D1_C, D1_D, D2_C);
		combineTo(combined, D1_D, D1_E, D2_D);
		combineTo(combined, D1_E, D1_F, D2_E);

		combineTo(combined, D2_0, D1_2, D3_0);
		combineTo(combined, D2_1, D1_3, D3_1);
		combineTo(combined, D2_2, D1_4, D3_2);
		combineTo(combined, D2_3, D1_5, D3_3);
		combineTo(combined, D2_4, D1_6, D3_4);
		combineTo(combined, D2_5, D1_7, D3_5);
		combineTo(combined, D2_6, D1_8, D3_6);
		combineTo(combined, D2_7, D1_9, D3_7);
		combineTo(combined, D2_8, D1_A, D3_8);
		combineTo(combined, D2_9, D1_B, D3_9);
		combineTo(combined, D2_A, D1_C, D3_A);
		combineTo(combined, D2_B, D1_D, D3_B);
		combineTo(combined, D2_C, D1_E, D3_C);
		combineTo(combined, D2_D, D1_F, D3_D);

		combineTo(combined, D3_0, D1_3, D4_0);
		combineTo(combined, D3_1, D1_4, D4_1);
		combineTo(combined, D3_2, D1_5, D4_2);
		combineTo(combined, D3_3, D1_6, D4_3);
		combineTo(combined, D3_4, D1_7, D4_4);
		combineTo(combined, D3_5, D1_8, D4_5);
		combineTo(combined, D3_6, D1_9, D4_6);
		combineTo(combined, D3_7, D1_A, D4_7);
		combineTo(combined, D3_8, D1_B, D4_8);
		combineTo(combined, D3_9, D1_C, D4_9);
		combineTo(combined, D3_A, D1_D, D4_A);
		combineTo(combined, D3_B, D1_E, D4_B);
		combineTo(combined, D3_C, D1_F, D4_C);

		combineTo(combined, D4_0, D1_4, D5_0);
		combineTo(combined, D4_1, D1_5, D5_1);
		combineTo(combined, D4_2, D1_6, D5_2);
		combineTo(combined, D4_3, D1_7, D5_3);
		combineTo(combined, D4_4, D1_8, D5_4);
		combineTo(combined, D4_5, D1_9, D5_5);
		combineTo(combined, D4_6, D1_A, D5_6);
		combineTo(combined, D4_7, D1_B, D5_7);
		combineTo(combined, D4_8, D1_C, D5_8);
		combineTo(combined, D4_9, D1_D, D5_9);
		combineTo(combined, D4_A, D1_E, D5_A);
		combineTo(combined, D4_B, D1_F, D5_B);

		combineTo(combined, D5_0, D1_5, D6_0);
		combineTo(combined, D5_1, D1_6, D6_1);
		combineTo(combined, D5_2, D1_7, D6_2);
		combineTo(combined, D5_3, D1_8, D6_3);
		combineTo(combined, D5_4, D1_9, D6_4);
		combineTo(combined, D5_5, D1_A, D6_5);
		combineTo(combined, D5_6, D1_B, D6_6);
		combineTo(combined, D5_7, D1_C, D6_7);
		combineTo(combined, D5_8, D1_D, D6_8);
		combineTo(combined, D5_9, D1_E, D6_9);
		combineTo(combined, D5_A, D1_F, D6_A);

		combineTo(combined, D6_0, D1_6, D7_0);
		combineTo(combined, D6_1, D1_7, D7_1);
		combineTo(combined, D6_2, D1_8, D7_2);
		combineTo(combined, D6_3, D1_9, D7_3);
		combineTo(combined, D6_4, D1_A, D7_4);
		combineTo(combined, D6_5, D1_B, D7_5);
		combineTo(combined, D6_6, D1_C, D7_6);
		combineTo(combined, D6_7, D1_D, D7_7);
		combineTo(combined, D6_8, D1_E, D7_8);
		combineTo(combined, D6_9, D1_F, D7_9);

		combineTo(combined, D7_0, D1_7, D8_0);
		combineTo(combined, D7_1, D1_8, D8_1);
		combineTo(combined, D7_2, D1_9, D8_2);
		combineTo(combined, D7_3, D1_A, D8_3);
		combineTo(combined, D7_4, D1_B, D8_4);
		combineTo(combined, D7_5, D1_C, D8_5);
		combineTo(combined, D7_6, D1_D, D8_6);
		combineTo(combined, D7_7, D1_E, D8_7);
		combineTo(combined, D7_8, D1_F, D8_8);

		combineTo(combined, D8_0, D1_8, D9_0);
		combineTo(combined, D8_1, D1_9, D9_1);
		combineTo(combined, D8_2, D1_A, D9_2);
		combineTo(combined, D8_3, D1_B, D9_3);
		combineTo(combined, D8_4, D1_C, D9_4);
		combineTo(combined, D8_5, D1_D, D9_5);
		combineTo(combined, D8_6, D1_E, D9_6);
		combineTo(combined, D8_7, D1_F, D9_7);

		combineTo(combined, D9_0, D1_9, DA_0);
		combineTo(combined, D9_1, D1_A, DA_1);
		combineTo(combined, D9_2, D1_B, DA_2);
		combineTo(combined, D9_3, D1_C, DA_3);
		combineTo(combined, D9_4, D1_D, DA_4);
		combineTo(combined, D9_5, D1_E, DA_5);
		combineTo(combined, D9_6, D1_F, DA_6);

		combineTo(combined, DA_0, D1_A, DB_0);
		combineTo(combined, DA_1, D1_B, DB_1);
		combineTo(combined, DA_2, D1_C, DB_2);
		combineTo(combined, DA_3, D1_D, DB_3);
		combineTo(combined, DA_4, D1_E, DB_4);
		combineTo(combined, DA_5, D1_F, DB_5);

		combineTo(combined, DB_0, D1_B, DC_0);
		combineTo(combined, DB_1, D1_C, DC_1);
		combineTo(combined, DB_2, D1_D, DC_2);
		combineTo(combined, DB_3, D1_E, DC_3);
		combineTo(combined, DB_4, D1_F, DC_4);

		combineTo(combined, DC_0, D1_C, DD_0);
		combineTo(combined, DC_1, D1_D, DD_1);
		combineTo(combined, DC_2, D1_E, DD_2);
		combineTo(combined, DC_3, D1_F, DD_3);

		combineTo(combined, DD_0, D1_D, DE_0);
		combineTo(combined, DD_1, D1_E, DE_1);
		combineTo(combined, DD_2, D1_F, DE_2);

		combineTo(combined, DE_0, D1_E, DF_0);
		combineTo(combined, DE_1, D1_F, DF_1);

		combineTo(combined, DF_0, D1_F, D_FULL);

	}

	private static void combineTo(long[] combined, int offsetA, int offsetB, int targetOffset) {
		combined[targetOffset] = combined[offsetA] & combined[offsetB];
		combined[++targetOffset] = combined[++offsetA] & combined[++offsetB];
		combined[++targetOffset] = combined[++offsetA] & combined[++offsetB];
		combined[++targetOffset] = combined[++offsetA] & combined[++offsetB];
	}

	/** indexed by depth, zero-element not used for ease of addressing */
	private static final int[] SLICE_OFFSET = new int[17];
	private static final int COMBINED_WORD_COUNT;
	private static final long[] EMPTY_WORDS = new long[INTERIOR_CACHE_WORDS];

	static {
		int wordCount = 0;

		for (int depth = 1; depth <= 16; depth++) {
			SLICE_OFFSET[depth] = wordCount;
			wordCount += SLICE_WORD_COUNT * sliceCount(depth);
		}

		COMBINED_WORD_COUNT = wordCount;
	}

	private static final int sliceCount(int sliceDepth) {
		assert sliceDepth >= 1;
		assert sliceDepth < 17;
		// 16 -> 1
		// 15 -> 2
		// ...
		// 2 -> 15
		// 1 -> 16
		return 17 - sliceDepth;
	}

	private static int sliceOffset(int depth, int slice) {
		return SLICE_OFFSET[depth] + SLICE_WORD_COUNT * slice;
	}

	private static final int D1_0 = sliceOffset(1, 0x0);
	private static final int D1_1 = sliceOffset(1, 0x1);
	private static final int D1_2 = sliceOffset(1, 0x2);
	private static final int D1_3 = sliceOffset(1, 0x3);
	private static final int D1_4 = sliceOffset(1, 0x4);
	private static final int D1_5 = sliceOffset(1, 0x5);
	private static final int D1_6 = sliceOffset(1, 0x6);
	private static final int D1_7 = sliceOffset(1, 0x7);
	private static final int D1_8 = sliceOffset(1, 0x8);
	private static final int D1_9 = sliceOffset(1, 0x9);
	private static final int D1_A = sliceOffset(1, 0xA);
	private static final int D1_B = sliceOffset(1, 0xB);
	private static final int D1_C = sliceOffset(1, 0xC);
	private static final int D1_D = sliceOffset(1, 0xD);
	private static final int D1_E = sliceOffset(1, 0xE);
	private static final int D1_F = sliceOffset(1, 0xF);

	private static final int D2_0 = sliceOffset(2, 0x0);
	private static final int D2_1 = sliceOffset(2, 0x1);
	private static final int D2_2 = sliceOffset(2, 0x2);
	private static final int D2_3 = sliceOffset(2, 0x3);
	private static final int D2_4 = sliceOffset(2, 0x4);
	private static final int D2_5 = sliceOffset(2, 0x5);
	private static final int D2_6 = sliceOffset(2, 0x6);
	private static final int D2_7 = sliceOffset(2, 0x7);
	private static final int D2_8 = sliceOffset(2, 0x8);
	private static final int D2_9 = sliceOffset(2, 0x9);
	private static final int D2_A = sliceOffset(2, 0xA);
	private static final int D2_B = sliceOffset(2, 0xB);
	private static final int D2_C = sliceOffset(2, 0xC);
	private static final int D2_D = sliceOffset(2, 0xD);
	private static final int D2_E = sliceOffset(2, 0xE);

	private static final int D3_0 = sliceOffset(3, 0x0);
	private static final int D3_1 = sliceOffset(3, 0x1);
	private static final int D3_2 = sliceOffset(3, 0x2);
	private static final int D3_3 = sliceOffset(3, 0x3);
	private static final int D3_4 = sliceOffset(3, 0x4);
	private static final int D3_5 = sliceOffset(3, 0x5);
	private static final int D3_6 = sliceOffset(3, 0x6);
	private static final int D3_7 = sliceOffset(3, 0x7);
	private static final int D3_8 = sliceOffset(3, 0x8);
	private static final int D3_9 = sliceOffset(3, 0x9);
	private static final int D3_A = sliceOffset(3, 0xA);
	private static final int D3_B = sliceOffset(3, 0xB);
	private static final int D3_C = sliceOffset(3, 0xC);
	private static final int D3_D = sliceOffset(3, 0xD);

	private static final int D4_0 = sliceOffset(4, 0x0);
	private static final int D4_1 = sliceOffset(4, 0x1);
	private static final int D4_2 = sliceOffset(4, 0x2);
	private static final int D4_3 = sliceOffset(4, 0x3);
	private static final int D4_4 = sliceOffset(4, 0x4);
	private static final int D4_5 = sliceOffset(4, 0x5);
	private static final int D4_6 = sliceOffset(4, 0x6);
	private static final int D4_7 = sliceOffset(4, 0x7);
	private static final int D4_8 = sliceOffset(4, 0x8);
	private static final int D4_9 = sliceOffset(4, 0x9);
	private static final int D4_A = sliceOffset(4, 0xA);
	private static final int D4_B = sliceOffset(4, 0xB);
	private static final int D4_C = sliceOffset(4, 0xC);

	private static final int D5_0 = sliceOffset(5, 0x0);
	private static final int D5_1 = sliceOffset(5, 0x1);
	private static final int D5_2 = sliceOffset(5, 0x2);
	private static final int D5_3 = sliceOffset(5, 0x3);
	private static final int D5_4 = sliceOffset(5, 0x4);
	private static final int D5_5 = sliceOffset(5, 0x5);
	private static final int D5_6 = sliceOffset(5, 0x6);
	private static final int D5_7 = sliceOffset(5, 0x7);
	private static final int D5_8 = sliceOffset(5, 0x8);
	private static final int D5_9 = sliceOffset(5, 0x9);
	private static final int D5_A = sliceOffset(5, 0xA);
	private static final int D5_B = sliceOffset(5, 0xB);

	private static final int D6_0 = sliceOffset(6, 0x0);
	private static final int D6_1 = sliceOffset(6, 0x1);
	private static final int D6_2 = sliceOffset(6, 0x2);
	private static final int D6_3 = sliceOffset(6, 0x3);
	private static final int D6_4 = sliceOffset(6, 0x4);
	private static final int D6_5 = sliceOffset(6, 0x5);
	private static final int D6_6 = sliceOffset(6, 0x6);
	private static final int D6_7 = sliceOffset(6, 0x7);
	private static final int D6_8 = sliceOffset(6, 0x8);
	private static final int D6_9 = sliceOffset(6, 0x9);
	private static final int D6_A = sliceOffset(6, 0xA);

	private static final int D7_0 = sliceOffset(7, 0x0);
	private static final int D7_1 = sliceOffset(7, 0x1);
	private static final int D7_2 = sliceOffset(7, 0x2);
	private static final int D7_3 = sliceOffset(7, 0x3);
	private static final int D7_4 = sliceOffset(7, 0x4);
	private static final int D7_5 = sliceOffset(7, 0x5);
	private static final int D7_6 = sliceOffset(7, 0x6);
	private static final int D7_7 = sliceOffset(7, 0x7);
	private static final int D7_8 = sliceOffset(7, 0x8);
	private static final int D7_9 = sliceOffset(7, 0x9);

	private static final int D8_0 = sliceOffset(8, 0x0);
	private static final int D8_1 = sliceOffset(8, 0x1);
	private static final int D8_2 = sliceOffset(8, 0x2);
	private static final int D8_3 = sliceOffset(8, 0x3);
	private static final int D8_4 = sliceOffset(8, 0x4);
	private static final int D8_5 = sliceOffset(8, 0x5);
	private static final int D8_6 = sliceOffset(8, 0x6);
	private static final int D8_7 = sliceOffset(8, 0x7);
	private static final int D8_8 = sliceOffset(8, 0x8);

	private static final int D9_0 = sliceOffset(9, 0x0);
	private static final int D9_1 = sliceOffset(9, 0x1);
	private static final int D9_2 = sliceOffset(9, 0x2);
	private static final int D9_3 = sliceOffset(9, 0x3);
	private static final int D9_4 = sliceOffset(9, 0x4);
	private static final int D9_5 = sliceOffset(9, 0x5);
	private static final int D9_6 = sliceOffset(9, 0x6);
	private static final int D9_7 = sliceOffset(9, 0x7);

	private static final int DA_0 = sliceOffset(0xA, 0x0);
	private static final int DA_1 = sliceOffset(0xA, 0x1);
	private static final int DA_2 = sliceOffset(0xA, 0x2);
	private static final int DA_3 = sliceOffset(0xA, 0x3);
	private static final int DA_4 = sliceOffset(0xA, 0x4);
	private static final int DA_5 = sliceOffset(0xA, 0x5);
	private static final int DA_6 = sliceOffset(0xA, 0x6);

	private static final int DB_0 = sliceOffset(0xB, 0x0);
	private static final int DB_1 = sliceOffset(0xB, 0x1);
	private static final int DB_2 = sliceOffset(0xB, 0x2);
	private static final int DB_3 = sliceOffset(0xB, 0x3);
	private static final int DB_4 = sliceOffset(0xB, 0x4);
	private static final int DB_5 = sliceOffset(0xB, 0x5);

	private static final int DC_0 = sliceOffset(0xC, 0x0);
	private static final int DC_1 = sliceOffset(0xC, 0x1);
	private static final int DC_2 = sliceOffset(0xC, 0x2);
	private static final int DC_3 = sliceOffset(0xC, 0x3);
	private static final int DC_4 = sliceOffset(0xC, 0x4);

	private static final int DD_0 = sliceOffset(0xD, 0x0);
	private static final int DD_1 = sliceOffset(0xD, 0x1);
	private static final int DD_2 = sliceOffset(0xD, 0x2);
	private static final int DD_3 = sliceOffset(0xD, 0x3);

	private static final int DE_0 = sliceOffset(0xE, 0x0);
	private static final int DE_1 = sliceOffset(0xE, 0x1);
	private static final int DE_2 = sliceOffset(0xE, 0x2);

	private static final int DF_0 = sliceOffset(0xF, 0x0);
	private static final int DF_1 = sliceOffset(0xF, 0x1);

	private static final int D_FULL = sliceOffset(0x10, 0x0);

}
