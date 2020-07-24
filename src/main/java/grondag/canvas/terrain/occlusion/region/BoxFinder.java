package grondag.canvas.terrain.occlusion.region;

import static grondag.canvas.terrain.RenderRegionAddressHelper.INTERIOR_CACHE_WORDS;
import static grondag.canvas.terrain.RenderRegionAddressHelper.SLICE_WORD_COUNT;

import java.util.function.Consumer;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import grondag.canvas.terrain.occlusion.region.area.Area;
import grondag.canvas.terrain.occlusion.region.area.AreaFinder;
import grondag.canvas.terrain.occlusion.region.area.AreaUtil;

public class BoxFinder {
	final long[] source = new long[INTERIOR_CACHE_WORDS];
	final long[] filled = new long[INTERIOR_CACHE_WORDS];

	/** bits 0-15 indicate which slices contain the area with the same index of the value */
	final int[] areaSlices = new int[AreaFinder.AREA_COUNT];

	public final IntArrayList boxes = new IntArrayList();
	private final LongArrayList sortedBoxes = new LongArrayList();

	public final AreaFinder areaFinder;

	private int voxelCount;

	public BoxFinder(AreaFinder areaFinder) {
		this.areaFinder = areaFinder;
	}

	public void findBoxes(long[] sourceBits, int sourceIndex) {

		System.arraycopy(OcclusionRegion.EMPTY_BITS, 0, filled, 0, INTERIOR_CACHE_WORDS);
		System.arraycopy(sourceBits, sourceIndex, source, 0, INTERIOR_CACHE_WORDS);
		boxes.clear();

		markSectionSlices();
		buildSortedSections();
		findSections();

		clearSectionBits();

		voxelCount = voxelCount(sourceBits, sourceIndex);
		markBoxSlices();
		markBoxNeighborSlices();
		buildSortedBoxes();
		findDisjointBoxes();

		if (voxelCount > 0) {
			clearSectionBits();
			markBoxSlices();
			markBoxNeighborSlices();
			buildSortedBoxes();
			findDisjointBoxes();
		}
	}

	private void buildSortedSections() {
		final LongArrayList sortedBoxes = this.sortedBoxes;
		sortedBoxes.clear();

		final AreaFinder areaFinder = this.areaFinder;
		final int[] areaSlices = this.areaSlices;

		if (areaSlices[0] != 0) {
			// handle special case of full Z-plane
			addBoxesFromSlice(areaFinder.get(0), areaSlices[0]);
		}

		for (int i = 1; i < AreaFinder.SECTION_COUNT; ++i) {
			final Area area  =  areaFinder.getSection(i);
			final int a = area.areaKey;
			final int slice = areaSlices[area.index];

			if (slice == 0xFFFF) {
				final int dy = (AreaUtil.y1(a) - AreaUtil.y0(a) + 1);
				final int dx = (AreaUtil.x1(a) - AreaUtil.x0(a) + 1);
				final long vol = (dx * dy * 16);
				sortedBoxes.add((vol << 34) | (area.index << 10) | (16 << 5) | 0);
			}
		}

		sortedBoxes.sort((a,b) -> Long.compare(b, a));
	}

	private void markBoxNeighborSlices() {
		final AreaFinder areaFinder = this.areaFinder;
		final int[] areaSlices = this.areaSlices;

		for (int i = 0; i < AreaFinder.AREA_COUNT; ++i) {
			int slice = areaSlices[i];

			if (slice != 0) {
				final Area area  = areaFinder.get(i);

				if ((slice & 1) == 1 && (slice & 2) == 0) {
					// special case first slice - can only transfer up
					if (area.isIncludedBySample(source, SLICE_WORD_COUNT)) {
						slice |= 2;
					}
				}

				int mask = 2;

				for (int z = 1; z < 15; z++) {
					if((slice & mask) != 0) {
						// transfer to lower slice if not already present
						final int lowMask = (mask >> 1);

						if ((slice & lowMask) == 0) {
							if (area.isIncludedBySample(source, (z - 1) * SLICE_WORD_COUNT)) {
								slice |= lowMask;
							}
						}

						// transfer to upper slice if not already present
						final int highMask = (mask << 1);

						if ((slice & highMask) == 0) {
							if (area.isIncludedBySample(source, (z + 1) * SLICE_WORD_COUNT)) {
								slice |= highMask;
							}
						}
					}

					mask <<= 1;
				}

				if ((slice & 0b1000000000000000) == 0b1000000000000000  && (slice & 0b0100000000000000) == 0) {
					if (area.isIncludedBySample(source, SLICE_WORD_COUNT * 14)) {
						slice |= 0b0100000000000000;
					}
				}

				areaSlices[i] = slice;
			}
		}
	}

	private void buildSortedBoxes() {
		final LongArrayList sortedBoxes = this.sortedBoxes;
		sortedBoxes.clear();

		final AreaFinder areaFinder = this.areaFinder;
		final int[] areaSlices = this.areaSlices;

		for (int i = 0; i < AreaFinder.AREA_COUNT; ++i) {
			final int slice = areaSlices[i];

			if (slice != 0) {
				addBoxesFromSlice(areaFinder.get(i), slice);
			}
		}

		sortedBoxes.sort((a,b) -> Long.compare(b, a));
	}

	private void addBoxesFromSlice(Area area, int slice) {
		int z0 = -1;
		int mask = 1;
		final int a = area.areaKey;
		final int x0 = AreaUtil.x0(a);
		final int y0 = AreaUtil.y0(a);
		final int x1 = AreaUtil.x1(a);
		final int y1 = AreaUtil.y1(a);

		for (int z = 0; z < 16; z++) {
			if((slice & mask) == 0) {
				// no bit, end run if started
				if(z0 != -1) {
					final int dz = (z - z0);
					final int dy = (y1 - y0 + 1);
					final int dx = (x1 - x0 + 1);
					final long vol = dx * dy * dz;
					sortedBoxes.add((vol << 34) | (area.index << 10) | (z << 5) | z0);
					z0 = -1;
				}
			} else {
				// bit set, start run if not started
				if (z0 == -1) {
					z0 = z;
				}
			}

			mask <<= 1;
		}

		// handle case when run extends to last bit
		if (z0 != -1)  {
			final int dz = (16 - z0);
			final int dy = (y1 - y0 + 1);
			final int dx = (x1 - x0 + 1);
			final long vol = dx * dy * dz;

			sortedBoxes.add((vol << 34) | (area.index << 10) | (16 << 5) | z0);
		}
	}

	int mask;

	private final Consumer<Area> markSliceConsumer = a -> areaSlices[a.index] |= mask;

	// PERF: still slow on relative basis to rest of chunk baking
	private void markBoxSlices() {
		final long[] sourceBits = source;
		final AreaFinder areaFinder = this.areaFinder;
		final int[] areaSlices = this.areaSlices;
		System.arraycopy(EMPTY_AREA_SLICES, 0, areaSlices, 0, AreaFinder.AREA_COUNT);
		mask = 1;
		int sourceIndex = 0;

		for (int i = 0; i < 16; ++i) {
			areaFinder.find(sourceBits, sourceIndex, markSliceConsumer);
			sourceIndex += SLICE_WORD_COUNT;
			mask <<= 1;
		}
	}

	private void markSectionSlices() {
		final long[] sourceBits = source;
		final AreaFinder areaFinder = this.areaFinder;
		final int[] areaSlices = this.areaSlices;
		System.arraycopy(EMPTY_AREA_SLICES, 0, areaSlices, 0, AreaFinder.AREA_COUNT);
		mask = 1;
		int sourceIndex = 0;

		for (int i = 0; i < 16; ++i) {
			areaFinder.findSections(sourceBits, sourceIndex, markSliceConsumer);
			sourceIndex += SLICE_WORD_COUNT;
			mask <<= 1;
		}
	}

	private void findSections() {
		final AreaFinder areaFinder = this.areaFinder;
		final LongArrayList sortedBoxes = this.sortedBoxes;
		final int limit = sortedBoxes.size();
		final IntArrayList boxes = this.boxes;

		for (int i = 0; i < limit; i++) {
			final long box = sortedBoxes.getLong(i);
			final Area area = areaFinder.get((int) (box >> 10) & 0xFFFFFF);
			final int a = area.areaKey;

			final int z0 = (int) box & 31;
			final int z1 = (int) (box >> 5) & 31;

			if (isAdditive(area, z0, z1)) {
				fill(area, z0, z1);
				boxes.add(PackedBox.pack(AreaUtil.x0(a), AreaUtil.y0(a), z0, AreaUtil.x1(a) + 1, AreaUtil.y1(a) + 1, z1, PackedBox.RANGE_EXTREME));
			}
		}
	}

	private void findDisjointBoxes() {
		final AreaFinder areaFinder = this.areaFinder;
		final LongArrayList sortedBoxes = this.sortedBoxes;
		final int limit = sortedBoxes.size();
		final IntArrayList boxes = this.boxes;

		for (int i = 0; i < limit; i++) {
			final long box = sortedBoxes.getLong(i);
			final Area area = areaFinder.get((int) (box >> 10) & 0xFFFFFF);
			final int a = area.areaKey;
			final int z0 = (int) box & 31;
			final int z1 = (int) (box >> 5) & 31;

			if (!intersects(area, z0, z1)) {
				fill(area, z0, z1);
				final int vol = (int) (box >>> 34);
				boxes.add(PackedBox.pack(AreaUtil.x0(a), AreaUtil.y0(a), z0, AreaUtil.x1(a) + 1, AreaUtil.y1(a) + 1, z1, rangeFromVolume(vol)));
				voxelCount -= vol;

				if (voxelCount == 0) {
					break;
				}
			}
		}
	}

	private int rangeFromVolume(int maxArea) {
		return maxArea <= 64 ? PackedBox.RANGE_NEAR : maxArea > 512 ? PackedBox.RANGE_FAR : PackedBox.RANGE_MID;
	}

	private int voxelCount(long[] sourceBits, int sourceIndex) {
		int result = 0;
		final int limit = sourceIndex + INTERIOR_CACHE_WORDS;

		for (int i = sourceIndex; i < limit; ++i) {
			final long bits = sourceBits[i];
			result +=  bits == 0 ? 0 : bits == -1 ? 64 : Long.bitCount(bits);
		}

		return result;
	}

	private void fill(Area a, int z0, int z1) {
		final long[] filled = this.filled;
		int index = z0 * SLICE_WORD_COUNT;

		for  (int z = z0; z < z1; ++z) {
			a.setBits(filled, index);
			index += SLICE_WORD_COUNT;
		}
	}

	private boolean intersects(Area a, int z0, int z1) {
		final long[] filled = this.filled;
		int index = z0 * SLICE_WORD_COUNT;

		for  (int z = z0; z < z1; ++z) {
			if (a.intersectsWithSample(filled, index)) {
				return true;
			}

			index += SLICE_WORD_COUNT;
		}

		return false;
	}

	private boolean isAdditive(Area a, int z0, int z1) {
		final long[] filled = this.filled;
		int index = z0 * SLICE_WORD_COUNT;

		for  (int z = z0; z < z1; ++z) {
			if (a.isAdditive(filled, index)) {
				return true;
			}

			index += SLICE_WORD_COUNT;
		}

		return false;
	}

	private void clearSectionBits() {
		for (int i = 0; i < INTERIOR_CACHE_WORDS; ++i) {
			source[i] &= ~filled[i];
		}
	}

	private final int[] EMPTY_AREA_SLICES = new int[AreaFinder.AREA_COUNT];

}
