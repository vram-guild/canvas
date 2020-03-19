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

	private final AreaFinder areaFinder = new AreaFinder();

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
		int priorOffset = 0;

		for (int depth = 2; depth <= 16; depth++) {
			final int thisOffset = SLICE_OFFSET[depth];

			final int sliceCount = sliceCount(depth);

			for (int slice = 0; slice < sliceCount; slice++) {
				final int wordOffset = slice << 2;
				final int targetOffset = thisOffset + wordOffset;
				final int srcOffset = priorOffset + wordOffset;
				combined[targetOffset] = combined[srcOffset] & combined[srcOffset + SLICE_WORD_COUNT];
				combined[targetOffset + 1] = combined[srcOffset + 1] & combined[srcOffset + SLICE_WORD_COUNT + 1];
				combined[targetOffset + 2] = combined[srcOffset + 2] & combined[srcOffset + SLICE_WORD_COUNT + 2];
				combined[targetOffset + 3] = combined[srcOffset + 3] & combined[srcOffset + SLICE_WORD_COUNT + 3];
			}

			priorOffset = thisOffset;
		}
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
}
