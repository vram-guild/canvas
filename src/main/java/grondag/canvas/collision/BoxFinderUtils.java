package grondag.canvas.collision;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import grondag.canvas.collision.octree.OctreeCoordinates.IAreaBoundsIntFunction;
import grondag.fermion.bits.BitHelper;

/**
 * Static utility methods for BoxFinder
 */
public class BoxFinderUtils
{
	static final long[] AREAS;
	static final int[] VOLUME_KEYS;
	static final int VOLUME_COUNT;
	static final short[] BOUNDS;

	static final long[] EMPTY = new long[64];

	static final Slice[][] lookupMinMax = new Slice[8][8];

	/**
	 * How many volumes are at least 4 voxels in volume.
	 * Will come first in {@link #VOLUME_KEYS}
	 */
	static final int VOLUME_COUNT_4_PLUS;

	/**
	 * Max is inclusive and equal to max attribute of slice.
	 */
	public static Slice sliceByMinMax(int minZ, int maxZ)
	{
		return lookupMinMax[minZ][maxZ];
	}

	private static final Slice[] SLICES = Slice.values();
	public static final int SLICE_COUNT = SLICES.length;

	static
	{
		for(final Slice slice : SLICES)
		{
			lookupMinMax[slice.min][slice.max] = slice;
		}

		final LongOpenHashSet patterns = new LongOpenHashSet();

		for(int xSize = 1; xSize <= 8; xSize++)
		{
			for(int ySize = 1; ySize <= 8; ySize++)
			{
				addPatterns(xSize, ySize, patterns);
			}
		}

		AREAS = patterns.toLongArray();

		LongArrays.quickSort(AREAS, new LongComparator()
		{
			@Override
			public int compare(@SuppressWarnings("null") Long o1, @SuppressWarnings("null") Long o2)
			{
				return compare(o1.longValue(), o2.longValue());
			}

			@Override
			public int compare(long k1, long k2)
			{
				// note reverse order, want largest first
				return  Integer.compare(Long.bitCount(k2), Long.bitCount(k1));
			}
		});

		BOUNDS = new short[AREAS.length];
		for(int i = 0; i < AREAS.length; i++)
		{
			final long pattern = AREAS[i];
			long xBits = pattern | (pattern >>> 32);
			xBits |= xBits >>> 16;
				xBits |= xBits >>> 8;
				xBits &= 0xFFL;
				BOUNDS[i] = (short)(minX(xBits) | (maxX(xBits) << 3) | (minY(pattern) << 6) | (maxY(pattern) << 9));
		}

		final IntArrayList volumes = new IntArrayList();
		for(final Slice slice : Slice.values())
		{
			for(int i = 0; i < AREAS.length; i++)
			{
				if(slice.depth * Long.bitCount(AREAS[i]) > 1)
					volumes.add(volumeKey(slice, i));
			}
		}

		VOLUME_KEYS = volumes.toIntArray();
		IntArrays.quickSort(BoxFinderUtils.VOLUME_KEYS, new IntComparator()
		{
			@Override
			public int compare(@SuppressWarnings("null") Integer o1, @SuppressWarnings("null") Integer o2)
			{
				return compare(o1.intValue(), o2.intValue());
			}

			@Override
			public int compare(int k1, int k2)
			{
				// note reverse order, want largest first
				return  Integer.compare(k2, k1);
			}
		});

		VOLUME_COUNT = VOLUME_KEYS.length;

		int countFourPlus = 0;
		for(int i = 0; i < VOLUME_COUNT; i++)
		{
			if(volumeFromKey(VOLUME_KEYS[i]) < 4)
			{
				if(countFourPlus == 0)
					countFourPlus = i;
			}
			else
				assert countFourPlus == 0: "volumes not in volume descending order";
		}
		VOLUME_COUNT_4_PLUS = countFourPlus;
	}


	static void findBestExclusionBits()
	{
		final IntArrayList[] VOLUMES_BY_BIT = new IntArrayList[512];
		final int[] COUNTS_BY_BIT = new int[512];
		int coverageCount = 0;

		for(int i = 0; i < 512; i++)
		{
			VOLUMES_BY_BIT[i] = new IntArrayList();
		}

		for(int v = 0; v <VOLUME_KEYS.length; v++)
		{
			final int k = VOLUME_KEYS[v];
			final Slice slice = sliceFromKey(k);
			final long pattern = patternFromKey(k);
			final int vFinal = v;

			BitHelper.forEachBit(pattern, i ->
			{
				final int x = xFromAreaBitIndex(i);
				final int y = yFromAreaBitIndex(i);
				for(int z = slice.min; z <= slice.max; z++)
				{
					final int n = x | (y << 3) | (z << 6);
					VOLUMES_BY_BIT[n].add(vFinal);
					COUNTS_BY_BIT[n]++;
				}
			});
		}

		final boolean coverage[] = new boolean[VOLUME_KEYS.length];

		int firstIndex = -1;
		int bestCount = -1;
		for(int i =  0; i < 512; i++)
		{
			if(COUNTS_BY_BIT[i] > bestCount)
			{
				bestCount = COUNTS_BY_BIT[i];
				firstIndex = i;
			}
		}

		coverageCount += bestCount;
		System.out.println("First bit coverage  = " + bestCount);

		for(final int v : VOLUMES_BY_BIT[firstIndex])
			coverage[v] = true;

		int secondIndex = -1;
		bestCount = -1;
		for(int i =  0; i < 512; i++)
		{
			if(i == firstIndex)  continue;

			if(COUNTS_BY_BIT[i] > bestCount)
			{
				int c = 0;
				for(final int j : VOLUMES_BY_BIT[i])
					if(!coverage[j]) c++;

				if(c  > bestCount)
				{
					bestCount = c;
					secondIndex = i;
				}
			}

			for(final int v : VOLUMES_BY_BIT[secondIndex])
				coverage[v] = true;
		}

		coverageCount += bestCount;
		System.out.println("Second bit coverage  = " + bestCount);

		int thirdIndex = -1;
		bestCount = -1;
		for(int i =  0; i < 512; i++)
		{
			if(i == firstIndex || i == secondIndex)  continue;

			if(COUNTS_BY_BIT[i] > bestCount)
			{
				int c = 0;
				for(final int j : VOLUMES_BY_BIT[i])
					if(!coverage[j]) c++;

				if(c  > bestCount)
				{
					bestCount = c;
					thirdIndex = i;
				}
			}

			for(final int v : VOLUMES_BY_BIT[thirdIndex])
				coverage[v] = true;
		}

		coverageCount += bestCount;
		System.out.println("Third bit coverage  = " + bestCount);

		int fourthIndex = -1;
		bestCount = -1;
		for(int i =  0; i < 512; i++)
		{
			if(i == firstIndex || i == secondIndex)  continue;

			if(COUNTS_BY_BIT[i] > bestCount)
			{
				int c = 0;
				for(final int j : VOLUMES_BY_BIT[i])
					if(!coverage[j]) c++;

				if(c  > bestCount)
				{
					bestCount = c;
					fourthIndex = i;
				}
			}

			for(final int v : VOLUMES_BY_BIT[fourthIndex])
				coverage[v] = true;
		}

		coverageCount += bestCount;
		System.out.println("Fourth bit coverage  = " + bestCount);

		System.out.println("Coverge % = " + 100 * coverageCount / VOLUME_KEYS.length);
	}


	/**
	 * Assumes values are pre-sorted.
	 */
	static int intersectIndexUnsafe(int high, int low)
	{
		return high * (high - 1) / 2;
	}

	/**
	 * Returns the number of maximal volume that target volume
	 * must be split into if the actorVolume is chosen for output.
	 * Note this total does not count boxes that would be included in the
	 * output volume.<p>
	 *
	 * Returns 0 if the boxes do not intersect.<p>
	 *
	 * Computed as the sum of actor bounds (any axis or side)
	 * that are within (not on the edge) of the target volume.
	 * This works because each face within bounds will force
	 * a split of the target box along the plane of the face.
	 *
	 * We subtract one from this total because we aren't counting
	 * the boxes that would be absorbed by the actor volume.
	 */
	static int splitScore(int actorVolIndex, int targetVolIndex)
	{
		final Slice actorSlice = sliceFromKey(actorVolIndex);
		final Slice targetSlice = sliceFromKey(targetVolIndex);

		int result = 0;

		// Must be >= or <= on one side of comparison because indexes are voxels and actual face depends on usage (min/max)

		if(actorSlice.min > targetSlice.min && actorSlice.min <= targetSlice.max)
			result++;

		if(actorSlice.max >= targetSlice.min && actorSlice.max < targetSlice.max)
			result++;

		result += testAreaBounds(patternIndexFromKey(targetVolIndex), (targetMinX, targetMinY, targetMaxX, targetMaxY) ->
		{
			return testAreaBounds(patternIndexFromKey(actorVolIndex), (actorMinX, actorMinY, actorMaxX, actorMaxY) ->
			{
				int n = 0;
				if(actorMinX > targetMinX && actorMinX <= targetMaxX)
					n++;

				if(actorMaxX >= targetMinX && actorMaxX < targetMaxX)
					n++;

				if(actorMinY > targetMinY && actorMinY <= targetMaxY)
					n++;

				if(actorMaxY >= targetMinY && actorMaxY < targetMaxY)
					n++;

				return n;
			});
		});

		return result == 0 ? 0 : result - 1;
	}


	/**
	 * Validates ordering and sorts if needed.
	 */
	static int intersectIndex(int a, int b)
	{
		return a > b ? intersectIndexUnsafe(a, b) : intersectIndexUnsafe(b, a);
	}

	private static void addPatterns(int xSize, int ySize, LongOpenHashSet patterns)
	{
		for(int xOrigin = 0; xOrigin <= 8 - xSize; xOrigin++)
		{
			for(int yOrigin = 0; yOrigin <= 8 - ySize; yOrigin++)
			{
				final long pattern = makePattern(xOrigin, yOrigin, xSize, ySize);

				//                if(yOrigin + ySize < 8)
				//                    assert ((pattern << 8) | pattern) == makePattern(xOrigin, yOrigin, xSize, ySize + 1);
				//
				//                if(yOrigin > 0)
				//                    assert ((pattern >>> 8) | pattern) == makePattern(xOrigin, yOrigin - 1, xSize, ySize + 1);
				//
				//                final int x0 = xOrigin;
				//                final int y0 = yOrigin;
				//                final int x1 = xOrigin + xSize - 1;
				//                final int y1 = yOrigin + ySize - 1;
				//                testAreaBounds(pattern, (minX, minY, maxX, maxY) ->
				//                {
				//                    assert minX == x0;
				//                    assert minY == y0;
				//                    assert maxX == x1;
				//                    assert maxY == y1;
				//                    return 0;
				//                });

				patterns.add(pattern);
			}
		}
	}

	static long makePattern(int xOrigin, int yOrigin, int xSize, int ySize)
	{
		long pattern = 0;
		for(int x = 0; x < xSize; x++)
		{
			for(int y = 0; y < ySize; y++)
			{
				pattern |= (1L << areaBitIndex(xOrigin + x, yOrigin + y));
			}
		}
		return pattern;
	}

	static int areaBitIndex(int x, int y)
	{
		return x | (y << 3);
	}

	static int xFromAreaBitIndex(int bitIndex)
	{
		return bitIndex & 7;
	}

	static int yFromAreaBitIndex(int bitIndex)
	{
		return (bitIndex >>> 3) & 7;
	}


	/**
	 * Single-pass lookup of x, y bounds for given area index.
	 */
	static int testAreaBounds(int areaIndex, IAreaBoundsIntFunction test)
	{
		final int bounds = BOUNDS[areaIndex];

		return test.apply(bounds & 7, (bounds >> 6) & 7, (bounds >> 3) & 7, (bounds >> 9) & 7);
	}

	/**
	 * For testing.
	 */
	static boolean doAreaBoundsMatch(int areaIndex, int minX, int minY, int maxX, int maxY)
	{
		return testAreaBounds(areaIndex, (x0, y0, x1, y1) ->
		{
			return (x0 == minX && y0 == minY && x1 == maxX && y1 == maxY) ? 1 : 0;
		}) == 1;
	}

	private static int minX(long xBits)
	{
		if((xBits & 0b1111) == 0)
		{
			if((xBits & 0b110000) == 0)
				return (xBits & 0b1000000) == 0 ? 7 : 6;
			else
				return (xBits & 0b10000) == 0 ? 5 : 4;
		}
		else
		{
			if((xBits & 0b11) == 0)
				return (xBits & 0b0100) == 0 ? 3 : 2;
			else
				return (xBits & 0b1) == 0 ? 1 : 0;
		}
	}

	private static int minY(long yBits)
	{
		if((yBits & 0xFFFFFFFFL) == 0L)
		{
			if((yBits & 0xFFFFFFFFFFFFL) == 0L)
				return (yBits & 0xFFFFFFFFFFFFFFL) == 0L ? 7 : 6;
			else
				return (yBits & 0xFFFFFFFFFFL) == 0L ? 5 : 4;
		}
		else
		{
			if((yBits & 0xFFFFL) == 0L)
				return (yBits & 0xFF0000L) == 0L ? 3 : 2;
			else
				return (yBits & 0xFFL) == 0L ? 1 : 0;
		}
	}

	private static int maxX(long xBits)
	{
		if((xBits & 0b11110000) == 0)
		{
			if((xBits & 0b1100) == 0)
				return (xBits & 0b10) == 0 ? 0 : 1;
			else
				return (xBits & 0b1000) == 0 ? 2 : 3;
		}
		else
		{
			if((xBits & 0b11000000) == 0)
				return (xBits & 0b100000) == 0 ? 4 : 5;
			else
				return (xBits & 0b10000000) == 0 ? 6 : 7;
		}
	}

	private static int maxY(long yBits)
	{
		if((yBits & 0xFFFFFFFF00000000L) == 0L)
		{
			if((yBits & 0xFFFF0000L) == 0L)
				return (yBits & 0xFF00L) == 0L ? 0 : 1;
			else
				return (yBits & 0xFF000000) == 0L ? 2 : 3;
		}
		else
		{
			if((yBits & 0xFFFF000000000000L) == 0L)
				return (yBits & 0xFF0000000000L) == 0L ? 4 : 5;
			else
				return (yBits & 0xFF00000000000000L) == 0L ? 6 : 7;
		}
	}

	/**
	 * Encodes a volume key that is naturally sortable by volume. (Larger values imply larger volume).
	 */
	static int volumeKey(Slice slice, int patternIndex)
	{
		final int volume = volume(slice, patternIndex);
		final int result = (volume << 17) | (patternIndex << 6) | slice.ordinal();
		assert result > 0;
		return result;
	}

	static int volume(Slice slice, int patternIndex)
	{
		return slice.depth * Long.bitCount(AREAS[patternIndex]);
	}

	static int volumeFromKey(int volumeKey)
	{
		return (volumeKey >> 17);
	}

	static int patternIndexFromKey(int volumeKey)
	{
		return (volumeKey >> 6) & 2047;
	}

	static long patternFromKey(int volumeKey)
	{
		return AREAS[patternIndexFromKey(volumeKey)];
	}

	static Slice sliceFromKey(int volumeKey)
	{
		return SLICES[(volumeKey & 63)];
	}

	/**
	 * True if volumes share any voxels, including case where one volume fully includes the other.
	 */
	static boolean doVolumesIntersect(int volumeKey0, int volumeKey1)
	{
		return (sliceFromKey(volumeKey0).layerBits & sliceFromKey(volumeKey1).layerBits) != 0
				&&  (patternFromKey(volumeKey0) & patternFromKey(volumeKey1)) != 0L;
	}

	static boolean doesVolumeIncludeBit(int volumeKey, int x, int y, int z)
	{
		return (sliceFromKey(volumeKey).layerBits & (1 << z)) != 0
				&&  (patternFromKey(volumeKey) & (1L << areaBitIndex(x, y))) != 0L;
	}

	/**
	 * True if volume matches the given bounds.<br>
	 * Second point coordinates are inclusive.
	 */
	static boolean areVolumesSame(int volumeKey, int x0, int y0, int z0, int x1, int y1, int z1)
	{
		return sliceFromKey(volumeKey).min == z0 & sliceFromKey(volumeKey).max == z1
				&&  doAreaBoundsMatch(patternIndexFromKey(volumeKey), x0, y0, x1, y1);
	}

	/**
	 * True if volumes share no voxels.
	 */
	static boolean areVolumesDisjoint(int volumeKey0, int volumeKey1)
	{
		return (sliceFromKey(volumeKey0).layerBits & sliceFromKey(volumeKey1).layerBits) == 0
				||  (patternFromKey(volumeKey0) & patternFromKey(volumeKey1)) == 0L;
	}

	/**
	 * True if the "big" volume fully includes the "small" volume.
	 * False is the volumes are the same volume, if "small" volume
	 * is actually larger, or if the small volume contains any voxels
	 * not part of the big volume.
	 */
	static boolean isVolumeIncluded(int bigKey, int smallKey)
	{
		// big volume must be larger than and distinct from the small volume
		if(volumeFromKey(bigKey) <= volumeFromKey(smallKey))
			return false;

		final int smallSliceBits  = sliceFromKey(smallKey).layerBits;
		if((sliceFromKey(bigKey).layerBits & smallSliceBits) != smallSliceBits)
			return false;

		final long smallPattern =  patternFromKey(smallKey);
		return ((patternFromKey(bigKey) & smallPattern) == smallPattern);
	}

	/**
	 * Returns number of voxels the exist in both of the given volumes, if any.
	 */
	static int intersectingVoxelCount(int vol0, int vol1)
	{
		final int sliceBits = sliceFromKey(vol0).layerBits & sliceFromKey(vol1).layerBits;
		if(sliceBits == 0)
			return 0;

		return Long.bitCount(sliceBits) * Long.bitCount(patternFromKey(vol0) & patternFromKey(vol1));
	}

	/**
	 * Returns number of voxels the exist in minimum volume encompassing both given volumes.
	 */
	static int unionVoxelCount(int vol0, int vol1)
	{
		final Slice s0 = sliceFromKey(vol0);
		final Slice s1 = sliceFromKey(vol1);
		// +1 because max is inclusive
		final int sliceBits = Math.max(s0.max, s1.max) - Math.min(s0.min, s1.min) + 1;

		final int areaBits = testAreaBounds(patternIndexFromKey(vol0), (minX0, minY0, maxX0, maxY0) ->
		{
			return testAreaBounds(patternIndexFromKey(vol1), (minX1, minY1, maxX1, maxY1) ->
			{
				final int x =  Math.max(maxX0, maxX1) - Math.min(minX0, minX1) + 1;
				final int y =  Math.max(maxY0, maxY1) - Math.min(minY0, minY1) + 1;
				return x * y;
			});
		});

		return areaBits * sliceBits;
	}
}
