package grondag.canvas.terrain.occlusion.region.area;

import java.util.Arrays;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AreaFinder {
	@Deprecated
	private static final Area[] AREA_BY_INDEX;

	@Deprecated
	private static final Area[] AREA_BY_KEY = new Area[0x10000];

	private static final int[] AREA_KEY_TO_INDEX = new int[0x10000];
	private static final int[] AREA_INDEX_TO_KEY;

	public static final int AREA_COUNT;

	private static final Area[] SECTION;

	public static final int SECTION_COUNT;

	//private static final ConcurrentMicroTimer timer = new ConcurrentMicroTimer("AreaFinder.find", 10000);

	public Area get(int index) {
		return AREA_BY_INDEX[index];
	}

	public Area getSection(int sectionIndex) {
		return SECTION[sectionIndex];
	}

	static {
		final IntOpenHashSet areas = new IntOpenHashSet();

		areas.add(Area.areaKey(0, 0, 15, 15));

		areas.add(Area.areaKey(1, 0, 15, 15));
		areas.add(Area.areaKey(0, 0, 14, 15));
		areas.add(Area.areaKey(0, 1, 15, 15));
		areas.add(Area.areaKey(0, 0, 15, 14));

		for (int x0 = 0; x0 <= 15; x0++) {
			for (int x1 = x0; x1 <= 15; x1++) {
				for (int y0 = 0; y0 <= 15; y0++) {
					for(int y1 = y0; y1 <= 15; y1++) {
						areas.add(Area.areaKey(x0, y0, x1, y1));
					}
				}
			}
		}

		AREA_COUNT = areas.size();
		AREA_INDEX_TO_KEY = new int[AREA_COUNT];

		AREA_BY_INDEX = new Area[AREA_COUNT];

		int i = 0;

		for(final int k : areas) {
			AREA_BY_INDEX[i++] = new Area(k, 0);
		}

		Arrays.sort(AREA_BY_INDEX, (a, b) -> {
			final int result = Integer.compare(Area.size(b.areaKey), Area.size(a.areaKey));

			// within same area size, prefer more compact rectangles
			return result == 0 ? Integer.compare(Area.edgeCount(a.areaKey), Area.edgeCount(b.areaKey)) : result;
		});

		// PERF: minor, but sort keys instead array to avoid extra alloc at startup
		for (int j = 0; j < AREA_COUNT; j++) {
			final Area a = new Area(AREA_BY_INDEX[j].areaKey, j);
			AREA_BY_INDEX[j] = a;
			AREA_BY_KEY[a.areaKey] = a;

			AREA_INDEX_TO_KEY[j] = a.areaKey;
			AREA_KEY_TO_INDEX[a.areaKey] = j;
		}

		final ObjectArrayList<Area> sections = new ObjectArrayList<>();

		for (int j = 0; j < AREA_COUNT; ++j) {
			final int a = AREA_INDEX_TO_KEY[j];

			if ((Area.x0(a) == 0  &&  Area.x1(a) == 15) || (Area.y0(a) == 0  &&  Area.y1(a) == 15)) {
				sections.add(AREA_BY_INDEX[j]);
			}
		}

		SECTION_COUNT = sections.size();
		SECTION = sections.toArray(new Area[SECTION_COUNT]);
	}

	final long[] bits = new long[4];

	public final ObjectArrayList<Area> areas =  new ObjectArrayList<>();

	//	[12:21:16] [Canvas Render Thread - 3/INFO] (Canvas) Avg AreaFinder.find duration = 124,536 ns, total duration = 1,245, total runs = 10,000
	//	[12:21:20] [Canvas Render Thread - 4/INFO] (Canvas) Avg AreaFinder.find duration = 128,970 ns, total duration = 1,289, total runs = 10,000
	//	[12:21:32] [Canvas Render Thread - 2/INFO] (Canvas) Avg AreaFinder.find duration = 117,787 ns, total duration = 1,177, total runs = 10,000

	//	[21:29:06] [Canvas Render Thread - 4/INFO] (Canvas) Avg AreaFinder.find duration = 2,459 ns, total duration = 24, total runs = 10,000
	//	[21:29:08] [Canvas Render Thread - 4/INFO] (Canvas) Avg AreaFinder.find duration = 3,512 ns, total duration = 35, total runs = 10,000
	//	[21:29:15] [Canvas Render Thread - 2/INFO] (Canvas) Avg AreaFinder.find duration = 2,721 ns, total duration = 27, total runs = 10,000
	//	[21:29:22] [Canvas Render Thread - 2/INFO] (Canvas) Avg AreaFinder.find duration = 3,087 ns, total duration = 30, total runs = 10,000
	public void find(long[] bitsIn, int sourceIndex, Consumer<Area> consumer) {
		//		timer.start();

		areas.clear();
		final long[] bits = this.bits;
		System.arraycopy(bitsIn, sourceIndex, bits, 0, 4);

		int bitCount = bitCount(bits[0]) +  bitCount(bits[1]) +  bitCount(bits[2]) +  bitCount(bits[3]);

		while(bitCount > 0) {
			final int key = findLargest(bits);
			final Area a = AREA_BY_KEY[key];
			consumer.accept(a);
			Area.clearBits(bits, 0, key);
			bitCount -= Area.size(a.areaKey);
		}

		//		timer.stop();
	}

	private static int bitCount(long bits) {
		return bits == 0 ? 0 : Long.bitCount(bits);
	}

	public void findSections(long[] bitsIn, int sourceIndex, Consumer<Area> consumer) {
		areas.clear();
		final long[] bits = this.bits;
		System.arraycopy(bitsIn, sourceIndex, bits, 0, 4);

		final int bitCount = Long.bitCount(bits[0]) + Long.bitCount(bits[1]) + Long.bitCount(bits[2]) + Long.bitCount(bits[3]);

		if (bitCount == 0) {
			return;
		}

		for(final Area r : SECTION) {
			if (Area.isIncludedBySample(bits, 0, r.areaKey)) {
				consumer.accept(r);
			}
		}
	}

	// based on approach described here:
	// 	https://stackoverflow.com/a/7497967
	// 	https://stackoverflow.com/a/7773870
	//  https://www.drdobbs.com/database/the-maximal-rectangle-problem/184410529
	public int findLargest(long[] bitsIn) {
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

			if  (rowBits == 0) {
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
				} else  {
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

	/** 1-16 values */
	private static int getVal16(long packed, int x) {
		return 1 + getVal15(packed, x);
	}

	/** 1-16 values */
	private static long setVal16(long packed, int x, int val) {
		return setVal15(packed, x, val -1);
	}

	/** 0-15 values */
	private static int getVal15(long packed, int x) {
		return (int) ((packed >>> (x << 2)) & 0xF);
	}

	/** 0-15 values */
	private static long setVal15(long packed, int x, int val) {
		final int shift = x << 2;
		final long mask = 0xFL << shift;
		return (packed & ~mask) | (((long) val) << shift);
	}
}