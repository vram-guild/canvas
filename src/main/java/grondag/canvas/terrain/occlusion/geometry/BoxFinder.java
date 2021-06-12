/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.terrain.occlusion.geometry;

import static grondag.canvas.terrain.util.RenderRegionAddressHelper.INTERIOR_CACHE_WORDS;
import static grondag.canvas.terrain.util.RenderRegionAddressHelper.SLICE_WORD_COUNT;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import grondag.bitraster.PackedBox;

public class BoxFinder {
	public final IntArrayList boxes = new IntArrayList();
	public final AreaFinder areaFinder;
	final long[] source = new long[INTERIOR_CACHE_WORDS];
	final long[] filled = new long[INTERIOR_CACHE_WORDS];
	/**
	 * Bits 0-15 indicate which slices contain the area with the same index of the value.
	 */
	final int[] areaSlices = new int[Area.AREA_COUNT];
	private final LongArrayList sortedBoxes = new LongArrayList();
	private final int[] EMPTY_AREA_SLICES = new int[Area.AREA_COUNT];
	int mask;
	private final IntConsumer markSliceConsumer = areaIndex -> areaSlices[areaIndex] |= mask;
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

		final int[] areaSlices = this.areaSlices;

		if (areaSlices[0] != 0) {
			// handle special case of full Z-plane
			addBoxesFromSlice(Area.indexToKey(0), areaSlices[0]);
		}

		for (int i = 1; i < Area.SECTION_COUNT; ++i) {
			final int areaIndex = Area.sectionToAreaIndex(i);
			final int slice = areaSlices[areaIndex];

			if (slice == 0xFFFF) {
				final int areaKey = Area.indexToKey(areaIndex);
				final int dy = (Area.y1(areaKey) - Area.y0(areaKey) + 1);
				final int dx = (Area.x1(areaKey) - Area.x0(areaKey) + 1);
				final long vol = (dx * dy * 16);
				sortedBoxes.add((vol << 34) | (areaIndex << 10) | (16 << 5) | 0);
			}
		}

		sortedBoxes.sort((a, b) -> Long.compare(b, a));
	}

	private void markBoxNeighborSlices() {
		final int[] areaSlices = this.areaSlices;

		for (int areaIndex = 0; areaIndex < Area.AREA_COUNT; ++areaIndex) {
			int slice = areaSlices[areaIndex];

			if (slice != 0) {
				if ((slice & 1) == 1 && (slice & 2) == 0) {
					// special case first slice - can only transfer up
					if (Area.isIncludedBySample(source, SLICE_WORD_COUNT, areaIndex)) {
						slice |= 2;
					}
				}

				int mask = 2;

				for (int z = 1; z < 15; z++) {
					if ((slice & mask) != 0) {
						// transfer to lower slice if not already present
						final int lowMask = (mask >> 1);

						if ((slice & lowMask) == 0) {
							if (Area.isIncludedBySample(source, (z - 1) * SLICE_WORD_COUNT, areaIndex)) {
								slice |= lowMask;
							}
						}

						// transfer to upper slice if not already present
						final int highMask = (mask << 1);

						if ((slice & highMask) == 0) {
							if (Area.isIncludedBySample(source, (z + 1) * SLICE_WORD_COUNT, areaIndex)) {
								slice |= highMask;
							}
						}
					}

					mask <<= 1;
				}

				if ((slice & 0b1000000000000000) == 0b1000000000000000 && (slice & 0b0100000000000000) == 0) {
					if (Area.isIncludedBySample(source, SLICE_WORD_COUNT * 14, areaIndex)) {
						slice |= 0b0100000000000000;
					}
				}

				areaSlices[areaIndex] = slice;
			}
		}
	}

	private void buildSortedBoxes() {
		final LongArrayList sortedBoxes = this.sortedBoxes;
		sortedBoxes.clear();

		final int[] areaSlices = this.areaSlices;

		for (int areaIndex = 0; areaIndex < Area.AREA_COUNT; ++areaIndex) {
			final int slice = areaSlices[areaIndex];

			if (slice != 0) {
				addBoxesFromSlice(Area.indexToKey(areaIndex), slice);
			}
		}

		sortedBoxes.sort((a, b) -> Long.compare(b, a));
	}

	private void addBoxesFromSlice(int areaKey, int slice) {
		int z0 = -1;
		int mask = 1;
		final int x0 = Area.x0(areaKey);
		final int y0 = Area.y0(areaKey);
		final int x1 = Area.x1(areaKey);
		final int y1 = Area.y1(areaKey);
		final int areaIndex = Area.keyToIndex(areaKey);

		for (int z = 0; z < 16; z++) {
			if ((slice & mask) == 0) {
				// no bit, end run if started
				if (z0 != -1) {
					final int dz = (z - z0);
					final int dy = (y1 - y0 + 1);
					final int dx = (x1 - x0 + 1);
					final long vol = dx * dy * dz;
					sortedBoxes.add((vol << 34) | (areaIndex << 10) | (z << 5) | z0);
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
		if (z0 != -1) {
			final int dz = (16 - z0);
			final int dy = (y1 - y0 + 1);
			final int dx = (x1 - x0 + 1);
			final long vol = dx * dy * dz;

			sortedBoxes.add((vol << 34) | (areaIndex << 10) | (16 << 5) | z0);
		}
	}

	// PERF: still slow on relative basis to rest of chunk baking
	private void markBoxSlices() {
		final long[] sourceBits = source;
		final AreaFinder areaFinder = this.areaFinder;
		final int[] areaSlices = this.areaSlices;
		System.arraycopy(EMPTY_AREA_SLICES, 0, areaSlices, 0, Area.AREA_COUNT);
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
		System.arraycopy(EMPTY_AREA_SLICES, 0, areaSlices, 0, Area.AREA_COUNT);
		mask = 1;
		int sourceIndex = 0;

		for (int i = 0; i < 16; ++i) {
			areaFinder.findSections(sourceBits, sourceIndex, markSliceConsumer);
			sourceIndex += SLICE_WORD_COUNT;
			mask <<= 1;
		}
	}

	private void findSections() {
		final LongArrayList sortedBoxes = this.sortedBoxes;
		final int limit = sortedBoxes.size();
		final IntArrayList boxes = this.boxes;

		for (int i = 0; i < limit; i++) {
			final long box = sortedBoxes.getLong(i);
			final int areaIndex = (int) (box >> 10) & 0xFFFFFF;

			final int z0 = (int) box & 31;
			final int z1 = (int) (box >> 5) & 31;

			if (isAdditive(areaIndex, z0, z1)) {
				fill(areaIndex, z0, z1);
				final int areaKey = Area.indexToKey(areaIndex);
				boxes.add(PackedBox.pack(Area.x0(areaKey), Area.y0(areaKey), z0, Area.x1(areaKey) + 1, Area.y1(areaKey) + 1, z1, PackedBox.RANGE_EXTREME));
			}
		}
	}

	private void findDisjointBoxes() {
		final LongArrayList sortedBoxes = this.sortedBoxes;
		final int limit = sortedBoxes.size();
		final IntArrayList boxes = this.boxes;

		for (int i = 0; i < limit; i++) {
			final long box = sortedBoxes.getLong(i);
			final int areaIndex = (int) (box >> 10) & 0xFFFFFF;
			final int z0 = (int) box & 31;
			final int z1 = (int) (box >> 5) & 31;

			if (!intersects(areaIndex, z0, z1)) {
				fill(areaIndex, z0, z1);
				final int vol = (int) (box >>> 34);
				final int areaKey = Area.indexToKey(areaIndex);
				boxes.add(PackedBox.pack(Area.x0(areaKey), Area.y0(areaKey), z0, Area.x1(areaKey) + 1, Area.y1(areaKey) + 1, z1, rangeFromVolume(vol)));
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
			result += bits == 0 ? 0 : bits == -1 ? 64 : Long.bitCount(bits);
		}

		return result;
	}

	private void fill(int areaIndex, int z0, int z1) {
		final long[] filled = this.filled;
		int index = z0 * SLICE_WORD_COUNT;
		final long[] bits = areaFinder.bitsFromIndex(areaIndex);

		for (int z = z0; z < z1; ++z) {
			filled[index] |= bits[0];
			filled[index + 1] |= bits[1];
			filled[index + 2] |= bits[2];
			filled[index + 3] |= bits[3];
			index += SLICE_WORD_COUNT;
		}
	}

	private boolean intersects(int areaIndex, int z0, int z1) {
		final long[] filled = this.filled;
		int index = z0 * SLICE_WORD_COUNT;

		for (int z = z0; z < z1; ++z) {
			if (Area.intersectsWithSample(filled, index, areaIndex)) {
				return true;
			}

			index += SLICE_WORD_COUNT;
		}

		return false;
	}

	private boolean isAdditive(int areaIndex, int z0, int z1) {
		final long[] filled = this.filled;
		int index = z0 * SLICE_WORD_COUNT;

		final long a0 = Area.bitsFromIndex(areaIndex, 0);
		final long a1 = Area.bitsFromIndex(areaIndex, 1);
		final long a2 = Area.bitsFromIndex(areaIndex, 2);
		final long a3 = Area.bitsFromIndex(areaIndex, 3);

		for (int z = z0; z < z1; ++z) {
			if (((~filled[index++] & a0) | (~filled[index++] & a1) | (~filled[index++] & a2) | (~filled[index++] & a3)) != 0) {
				return true;
			}
		}

		return false;
	}

	private void clearSectionBits() {
		for (int i = 0; i < INTERIOR_CACHE_WORDS; ++i) {
			source[i] &= ~filled[i];
		}
	}
}
