package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.RenderRegionAddressHelper.INTERIOR_CACHE_WORDS;
import static grondag.canvas.chunk.RenderRegionAddressHelper.SLICE_WORD_COUNT;

import java.util.function.Consumer;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class BoxFinder {
	final long[] filled = new long[INTERIOR_CACHE_WORDS];
	final int[] areaSlices = new int[AreaFinder.AREA_COUNT];

	public final IntArrayList boxes = new IntArrayList();
	private final LongArrayList sortedBoxes = new LongArrayList();

	public final AreaFinder areaFinder;

	public BoxFinder(AreaFinder areaFinder) {
		this.areaFinder = areaFinder;
	}

	public void findBoxes(long[] sourceBits, int sourceIndex) {
		final int voxelCount = voxelCount(sourceBits, sourceIndex);
		markSliceAreas(sourceBits, sourceIndex);
		addAllBoxes();
		findDisjointBoxes(voxelCount);
	}

	private void addAllBoxes() {
		final LongArrayList sortedBoxes = this.sortedBoxes;
		sortedBoxes.clear();

		final AreaFinder areaFinder = this.areaFinder;
		final int[] areaSlices = this.areaSlices;

		for (int i = 0; i < AreaFinder.AREA_COUNT; ++i) {
			final int slice = areaSlices[i];

			if (slice != 0) {
				addSlices(areaFinder.get(i), slice);
			}
		}

		sortedBoxes.sort((a,b) -> Long.compare(b, a));
	}

	private void addSlices(Area area, int slice) {
		int z0 = -1;
		int mask = 1;

		for (int z = 0; z < 16; z++) {

			if((slice & mask) == 0) {
				// no bit, end run if started
				if(z0 != -1) {
					final long vol = area.areaSize * (z - z0);
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
			final long vol = area.areaSize * (16 - z0);
			sortedBoxes.add((vol << 34) | (area.index << 10) | (16 << 5) | z0);
		}
	}

	int mask;

	private final Consumer<Area> markSliceConsumer = a -> areaSlices[a.index] |= mask;

	// PERF: still slow on relative basis to rest of chunk baking
	// also exlcudes child areas, may lead to inefficient box selection
	// (see find routine in AreaFinder)
	private void markSliceAreas(long[] sourceBits, int sourceIndex) {
		final AreaFinder areaFinder = this.areaFinder;
		final int[] areaSlices = this.areaSlices;
		System.arraycopy(EMPTY_AREA_SLICES, 0, areaSlices, 0, AreaFinder.AREA_COUNT);
		mask = 1;

		for (int i = 0; i < 16; i++) {
			areaFinder.find(sourceBits, sourceIndex, markSliceConsumer);
			sourceIndex += SLICE_WORD_COUNT;
			mask <<= 1;
		}
	}

	private void findDisjointBoxes(int voxelCount) {
		final AreaFinder areaFinder = this.areaFinder;
		final LongArrayList sortedBoxes = this.sortedBoxes;
		final int limit = sortedBoxes.size();
		final IntArrayList boxes = this.boxes;
		boxes.clear();

		System.arraycopy(OcclusionRegion.EMPTY_BITS, 0, filled, 0, INTERIOR_CACHE_WORDS);

		for (int i = 0; i < limit; i++) {
			final long box = sortedBoxes.getLong(i);
			final Area area =  areaFinder.get((int) (box >> 10) & 0xFFFFFF);
			final int z0 = (int) box & 31;
			final int z1 = (int) (box >> 5) & 31;

			if (!intersects(area, z0, z1)) {
				fill(area, z0, z1);
				final int vol = (int) (box >>> 34);
				boxes.add(PackedBox.pack(area.x0, area.y0, z0, area.x1 + 1, area.y1 + 1, z1, range(vol)));
				voxelCount -= vol;

				if (voxelCount == 0) {
					break;
				}
			}
		}
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

	private  int range(int volume) {
		return volume < 16 ? PackedBox.RANGE_NEAR : volume < 64 ? PackedBox.RANGE_MID : PackedBox.RANGE_FAR;
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


	private final int[] EMPTY_AREA_SLICES = new int[AreaFinder.AREA_COUNT];

}
