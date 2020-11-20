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

package grondag.canvas.terrain.occlusion.region;

import it.unimi.dsi.fastutil.ints.IntConsumer;

public class AreaFinder {
	final long[] bits = new long[4];

	private static int bitCount(long bits) {
		return bits == 0 ? 0 : Long.bitCount(bits);
	}

	// based on approach described here:
	// 	https://stackoverflow.com/a/7497967
	// 	https://stackoverflow.com/a/7773870
	//  https://www.drdobbs.com/database/the-maximal-rectangle-problem/184410529
	public static int findLargest(long[] bitsIn) {
		int bestX0 = 0;
		int bestY0 = 0;
		int bestX1 = -1;
		int bestY1 = -1;
		int bestArea = 0;

		// height of prior rows
		// four bits per position, values 0-15
		long heights = 0;

		for (int y = 0; y < 16; ++y) {
			final int rowBits = (int) ((bitsIn[y >> 2] >> ((y & 3) << 4)) & 0xFFFF);

			if (rowBits == 0) {
				heights = 0;
				continue;
			}

			//	OcclusionBitPrinter.printSpaced(Strings.padStart(Integer.toBinaryString(rowBits), 16, '0'));

			// track start of runs up to current height
			long stackX15 = 0; // 0-15
			long stackH16 = 0; // 1-16
			int stackSize = 0;

			// height of first column is zero if closed, otherwise 1 + previous row first column height
			int runHeight = (rowBits & 1) == 0 ? 0 : (1 + getVal15(heights, 0));
			int runStart = 0;

			// save height for use by next row, unless at top row
			if (y != 15) heights = setVal15(heights, 0, runHeight);

			// NB: inclusive of 16. The height @ 16 will always be zero, closing off last column
			for (int x = 1; x <= 16; ++x) {
				// height here is 0 if closed, otherwise 1 + height of row below
				final int h = (rowBits & (1 << x)) == 0 ? 0 : (1 + getVal15(heights, x));

				// if higher than last start new run
				if (h > runHeight) {
					// push current run onto stack
					if (runHeight != 0) {
						stackX15 = setVal15(stackX15, stackSize, runStart);
						stackH16 = setVal16(stackH16, stackSize, runHeight);
						++stackSize;
					}

					// new run starts here
					runStart = x;
					runHeight = h;
				} else {
					// if reduction in height, close out current run and
					// also runs on stack until revert to a sustainable run
					// or the stack is empty

					while (h < runHeight) {
						// check for largest area on current run
						final int a = (x - runStart) * runHeight;

						if (a > bestArea) {
							bestArea = a;
							bestX0 = runStart;
							bestX1 = x - 1;
							bestY0 = y - runHeight + 1;
							bestY1 = y;
						}

						if (stackSize == 0) {
							// if we have an empty stack but non-zero height,
							// then run at current height effectively starts
							// where the just-closed prior run  started
							runHeight = h;
							// NB: no change to run start - continue from prior
						} else { // stackSize > 0
							--stackSize;
							final int stackStart = getVal15(stackX15, stackSize);
							final int stackHeight = getVal16(stackH16, stackSize);

							if (stackHeight == h) {
								// if stack run height is same as current, resume run, leave stack popped
								runHeight = h;
								runStart = stackStart;
							} else if (stackHeight < h) {
								// if stack run height is less new height, leave on the stack
								++stackSize;
								// and new run starts from current position
								runHeight = h;
								// NB: no change to run start - continue from prior
							} else {
								// stack area is higher than new height
								// leave stack popped and loop to close out area on the stack
								runHeight = stackHeight;
								runStart = stackStart;
							}
						}
					}
				}

				// track height of this column but don't overflow on last row/column
				if (y != 15 && x < 16) heights = setVal15(heights, x, h);
			}
		}

		return Area.areaKey(bestX0, bestY0, bestX1, bestY1);
	}

	/**
	 * 1-16 values.
	 */
	private static int getVal16(long packed, int x) {
		return 1 + getVal15(packed, x);
	}

	/**
	 * 1-16 values.
	 */
	private static long setVal16(long packed, int x, int val) {
		return setVal15(packed, x, val - 1);
	}

	/**
	 * 0-15 values.
	 */
	private static int getVal15(long packed, int x) {
		return (int) ((packed >>> (x << 2)) & 0xF);
	}

	/**
	 * 0-15 values.
	 */
	private static long setVal15(long packed, int x, int val) {
		final int shift = x << 2;
		final long mask = 0xFL << shift;
		return (packed & ~mask) | (((long) val) << shift);
	}

	public long[] bitsFromIndex(int areaIndex) {
		final long[] result = bits;

		result[0] = Area.bitsFromIndex(areaIndex, 0);
		result[1] = Area.bitsFromIndex(areaIndex, 1);
		result[2] = Area.bitsFromIndex(areaIndex, 2);
		result[3] = Area.bitsFromIndex(areaIndex, 3);

		return result;
	}

	public void find(long[] bitsIn, int sourceIndex, IntConsumer areaIndexConsumer) {
		final long[] bits = this.bits;
		System.arraycopy(bitsIn, sourceIndex, bits, 0, 4);

		int bitCount = bitCount(bits[0]) + bitCount(bits[1]) + bitCount(bits[2]) + bitCount(bits[3]);

		while (bitCount > 0) {
			final int key = findLargest(bits);
			final int index = Area.keyToIndex(key);
			areaIndexConsumer.accept(index);
			Area.clearBits(bits, 0, index);
			bitCount -= Area.size(key);
		}
	}

	public void findSections(long[] bitsIn, int sourceIndex, IntConsumer areaIndexConsumer) {
		final long[] bits = this.bits;
		System.arraycopy(bitsIn, sourceIndex, bits, 0, 4);

		final int bitCount = Long.bitCount(bits[0]) + Long.bitCount(bits[1]) + Long.bitCount(bits[2]) + Long.bitCount(bits[3]);

		if (bitCount == 0) {
			return;
		}

		for (int i = 0; i < Area.SECTION_COUNT; ++i) {
			final int areaIndex = Area.sectionToAreaIndex(i);

			if (Area.isIncludedBySample(bits, 0, areaIndex)) {
				areaIndexConsumer.accept(areaIndex);
			}
		}
	}
}
