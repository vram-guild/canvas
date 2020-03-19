package grondag.canvas.collision;

import static grondag.canvas.collision.BoxFinderUtils.EMPTY;

import java.util.function.IntConsumer;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import grondag.canvas.CanvasMod;
import grondag.canvas.collision.octree.OctreeCoordinates.IAreaBoundsIntFunction;
import grondag.fermion.bits.BitHelper;

/**
 * Quickly identifies largest filled AABBs within an 8x8x8 voxel volume
 * and outputs those boxes plus any unclaimed voxels. <p>
 *
 * Exploits representation of each 8x8x1 layer as a single long value to
 * enable fast bit-wise comparisons. <p>
 */
public class BoxFinder
{
	private final long[] voxels = new long[8];

	private final long[] combined = new long[BoxFinderUtils.SLICE_COUNT];

	final int[] maximalVolumes = new int[64];

	int volumeCount = 0;
	/**
	 * Bitmask for the number of volumes we have.  Set when volumeCount is set.
	 */
	long volumeMask = 0;

	final long[] intersects = new long[64];

	final LongOpenHashSet disjointSets = new LongOpenHashSet();

	/**
	 * Tuples that have already been recursed and therefore don't need to be checked again
	 */
	final LongOpenHashSet visitedSets = new LongOpenHashSet();

	final int[] volumeScores = new int[64];

	void clear() {
		System.arraycopy(EMPTY, 0, voxels, 0, 8);
	}

	public boolean isEmpty() {
		return voxels[0] == 0
				&& voxels[1] == 0
				&& voxels[2] == 0
				&& voxels[3] == 0
				&& voxels[4] == 0
				&& voxels[5] == 0
				&& voxels[6] == 0
				&& voxels[7] == 0;
	}

	/**
	 * Saves voxel data to given array.   Array must be >= 8 in length.
	 * Load with {@link #restoreFrom(long[])}.
	 */
	public void saveTo(long[] data) {
		System.arraycopy(voxels, 0, data, 0, 8);
	}

	/**
	 * Loads data from an earlier call to {@link #saveTo(long[])}
	 */
	public void restoreFrom(long[] data) {
		System.arraycopy(data, 0, voxels, 0, 8);
	}


	/**
	 * Coordinates must be 0 - 8
	 */
	public void setFilled(int x, int y, int z)
	{
		voxels[z] |= (1L << BoxFinderUtils.areaBitIndex(x, y));
	}

	/**
	 * Coordinates must be 0 - 8
	 */
	public boolean isFilled(int x, int y, int z)
	{
		return (voxels[z] & (1L << BoxFinderUtils.areaBitIndex(x, y))) != 0;
	}

	public void setFilled(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
	{
		for(int x = minX; x <= maxX; x++)
		{
			for(int y = minY; y <= maxY; y++)
			{
				for(int z = minZ; z <= maxZ; z++)
				{
					setFilled(x, y, z);
				}
			}
		}
	}

	public void setEmpty(int x, int y, int z)
	{
		voxels[z] &= ~(1L << BoxFinderUtils.areaBitIndex(x, y));
	}

	public void setEmpty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
	{
		for(int x = minX; x <= maxX; x++)
		{
			for(int y = minY; y <= maxY; y++)
			{
				for(int z = minZ; z <= maxZ; z++)
				{
					setEmpty(x, y, z);
				}
			}
		}
	}

	boolean simplify(int iterations)
	{
		if(iterations <= 0)
		{
			assert false : "Bad simplification input";
		return false;
		}
		while(simplifyOnce() && --iterations > 0) {}
		return iterations == 0;
	}

	/**
	 * Combines the two volume volumes that can be combined with the smallest number of error voxels.
	 */
	boolean simplifyOnce()
	{
		calcCombined();
		populateMaximalVolumes();
		final int limit = volumeCount;
		final int limitMinus = limit - 1;
		if(limit < 2)
			return false;

		int bestError = Integer.MAX_VALUE;
		int bestPairL = 0;
		int bestPairR = 0;

		for(int i = 0; i < limitMinus; i++)
		{
			final int outerVol = maximalVolumes[i];
			final int outerVoxels = BoxFinderUtils.volumeFromKey(outerVol);

			for(int j = i + 1; j < limit; j++)
			{
				final int innerVol = maximalVolumes[j];

				final int separateVoxels = BoxFinderUtils.volumeFromKey(innerVol) + outerVoxels;
				final int commonVoxels = BoxFinderUtils.intersectingVoxelCount(innerVol, outerVol);
				final int unionVoxels = BoxFinderUtils.unionVoxelCount(innerVol, outerVol);
				final int error = unionVoxels - separateVoxels + commonVoxels;

				if(error < bestError)
				{
					bestError = error;
					bestPairL = innerVol;
					bestPairR = outerVol;
				}
			}
		}

		if(bestError == Integer.MAX_VALUE)
		{
			assert false : "Unable to simplify with two or more volumes";
		return false;
		}
		else
		{
			fillUnion(bestPairL, bestPairR);
			return true;
		}
	}

	/**
	 * How many voxels currently populated
	 */
	int filledVoxelCount()
	{
		return Long.bitCount(voxels[0])
				+ Long.bitCount(voxels[1])
				+ Long.bitCount(voxels[2])
				+ Long.bitCount(voxels[3])
				+ Long.bitCount(voxels[4])
				+ Long.bitCount(voxels[5])
				+ Long.bitCount(voxels[6])
				+ Long.bitCount(voxels[7]);
	}

	int simplificationScore(int volumeKey)
	{

		final int[] volumes = maximalVolumes;
		final int limit = volumeCount;

		// To score higher than zero, must fully enclose at least two maximal volumes.
		// If this volume would cause other maximal volume to split into two or more volumes
		// that are not fully enclosed in this volume, then those count against the score.
		int boxScore = -1;

		for(int i = 0; i < limit; i++)
		{
			final int v = volumes[i];
			if(BoxFinderUtils.isVolumeIncluded(volumeKey, v))
				boxScore++;

			boxScore -= BoxFinderUtils.splitScore(volumeKey, v);
		}

		if(boxScore == 0)
			return 0;

		final int filled = countFilledVoxels(volumeKey);
		final int vol = BoxFinderUtils.volumeFromKey(volumeKey);

		// must fill in some voxels or isn't a simplification
		if(filled == vol)
			return 0;

		// Preference is always given to lower error rates.
		// Because we have chosen high scores to mean "better" we
		// represent this inversely, as total possible voxels
		// less the number of error voxels.
		final int voxelScore = 512 - vol + filled;

		return (voxelScore << 16) | boxScore;
	}

	public void outputBoxes(ICollisionBoxListBuilder builder)
	{
		while(outputBest(builder)) {}
		outputRemainders(builder);
	}

	void outputRemainders(ICollisionBoxListBuilder builder)
	{
		outputRemaindersInner(0, builder);
		outputRemaindersInner(1, builder);
		outputRemaindersInner(2, builder);
		outputRemaindersInner(3, builder);
		outputRemaindersInner(4, builder);
		outputRemaindersInner(5, builder);
		outputRemaindersInner(6, builder);
		outputRemaindersInner(7, builder);
	}

	private class RemaindersConsumer implements IntConsumer
	{
		ICollisionBoxListBuilder builder;
		int z;

		RemaindersConsumer prepare(int z, ICollisionBoxListBuilder builder)
		{
			this.z = z;
			this.builder = builder;
			return this;
		}

		@Override
		public void accept(int i)
		{
			final int x = BoxFinderUtils.xFromAreaBitIndex(i);
			final int y = BoxFinderUtils.yFromAreaBitIndex(i);
			builder.addSorted(x, y, z, x + 1, y + 1, z + 1);
		}
	}

	private final RemaindersConsumer remaindersConsumer = new RemaindersConsumer();

	void outputRemaindersInner(int z, ICollisionBoxListBuilder builder)
	{
		final long bits = voxels[z];

		if(bits == 0L)
			return;

		BitHelper.forEachBit(bits, remaindersConsumer.prepare(z, builder));
	}

	void findDisjointSets()
	{
		disjointSets.clear();

		final int limit = volumeCount;

		for(int i = 0; i < limit; i++)
		{
			tryDisjoint(i, 0L, 0L);
		}
		visitedSets.clear();
	}

	/**
	 * If maximal volume at index does not intersect with any
	 * existing member, then is added to the set and recursively
	 * attempt to add more members.
	 *
	 * If the volume cannot be added to the set because it
	 * intersects, then this is a leaf node - adds the volume
	 * as a disjoint set and returns false.
	 *
	 */
	private boolean tryDisjoint(int volIndex, long membersIn, long combinedIntersectsIn)
	{
		final long volMask = (1L << volIndex);

		// no need to check volumes that don't intersect
		if((volMask & noIntersectMask) != 0)
			return false;

		if((volMask & (combinedIntersectsIn | membersIn)) == 0)
		{
			final long members = membersIn | volMask;

			// don't explore if been here already
			// returning true here lets caller know this isn't a leaf
			if(!visitedSets.add(members))
				return true;

			final long combinedIntersects = combinedIntersectsIn | intersects[volIndex] | volMask;
			final long candidates = (~combinedIntersects) & volumeMask & (~noIntersectMask);
			boolean isLeaf = true;
			if(candidates != 0)
			{
				boolean didRecurse = false;
				// not using lambda here to avoid mem alloc
				for(int i = 0; i < volumeCount; i++)
				{
					if(i != volIndex
							&& ((candidates & (1L << i)) != 0)
							&& tryDisjoint(i, members, combinedIntersects))
						didRecurse = true;
				}
				if(didRecurse)
					isLeaf = false;
			}
			if(isLeaf)
			{
				// add non-intersecting volumes to every disjoint set
				disjointSets.add(members | noIntersectMask);
			}
			return true;
		}
		else return false;
	}

	private class OutputConsumer implements IntConsumer
	{
		ICollisionBoxListBuilder builder;

		OutputConsumer prepare(ICollisionBoxListBuilder builder)
		{
			this.builder = builder;
			return this;
		}

		@Override
		public void accept(int i)
		{
			final int k = maximalVolumes[i];
			addBox(k, builder);
		}
	}

	final OutputConsumer outputConsumer = new OutputConsumer();

	private void outputDisjointSet(long disjointSet, ICollisionBoxListBuilder builder)
	{
		BitHelper.forEachBit(disjointSet, outputConsumer.prepare(builder));
	}

	void explainDisjointSets()
	{
		for(final Long l : disjointSets)
		{
			explainDisjointSet(l);
		}
	}

	void explainDisjointSet(long disjointSet)
	{
		CanvasMod.LOG.info("Disjoint Set Info: Box Count = %d, Score = %d", Long.bitCount(disjointSet), scoreOfDisjointSet(disjointSet));
		BitHelper.forEachBit(disjointSet, i ->
		{
			explainVolume(i);
		});
		CanvasMod.LOG.info("");
	}

	void explainMaximalVolumes()
	{
		CanvasMod.LOG.info("Maximal Volumes");
		for(int i = 0;  i < volumeCount; i++)
		{
			explainVolume(i);
		}
		CanvasMod.LOG.info("");
	}

	void explainVolume(int volIndex)
	{
		final int volKey = maximalVolumes[volIndex];
		final StringBuilder b = new StringBuilder();
		BitHelper.forEachBit(intersects[volIndex], i ->
		{
			if(b.length() != 0)
				b.append(", ");
			b.append(i);
		});
		final Slice slice = BoxFinderUtils.sliceFromKey(volKey);
		BoxFinderUtils.testAreaBounds(BoxFinderUtils.patternIndexFromKey(volKey), (minX, minY, maxX, maxY) ->
		{
			CanvasMod.LOG.info("Box w/ %d volume @ %d, %d,%d to %d, %d, %d  score = %d  intersects = %s  volKey = %d", BoxFinderUtils.volumeFromKey(volKey),
					minX, minY, slice.min, maxX, maxY, slice.max, volumeScores[volIndex], b.toString(), volKey);
			return 0;
		});
	}

	private boolean outputBest(ICollisionBoxListBuilder builder)
	{
		calcCombined();
		populateMaximalVolumes();
		return outputBestInner(builder);
	}

	private boolean outputBestInner(ICollisionBoxListBuilder builder)
	{
		if(volumeCount <= 1)
		{
			if(volumeCount == 1)
				addBox(maximalVolumes[0], builder);
			return false;
		}

		populateIntersects();
		classifyVolumeIntersections();
		findDisjointSets();

		final LongIterator it = disjointSets.iterator();
		long bestSet = it.nextLong();

		if(disjointSets.size() == 1)
		{
			outputDisjointSet(bestSet, builder);
			return false;
		}

		scoreMaximalVolumes();

		int bestScore = scoreOfDisjointSet(bestSet);

		while(it.hasNext())
		{
			final long set = it.nextLong();
			final int score = scoreOfDisjointSet(set);
			if(score < bestScore)
			{
				bestScore = score;
				bestSet = set;
			}
		}

		outputDisjointSet(bestSet, builder);

		return true;
	}


	class SetScoreAccumulator implements IntConsumer
	{
		int total;

		@Override
		public void accept(int value)
		{
			total += volumeScores[value];
		}

		void prepare()
		{
			total = 0;
		}
	}

	private final SetScoreAccumulator setScoreCounter = new SetScoreAccumulator();

	private int scoreOfDisjointSet(long set)
	{
		final SetScoreAccumulator counter = setScoreCounter;
		counter.prepare();
		BitHelper.forEachBit(set, setScoreCounter);
		return counter.total;
	}

	private class AddBoxConsumer implements IAreaBoundsIntFunction
	{
		Slice slice;
		ICollisionBoxListBuilder builder;

		AddBoxConsumer prepare(int volumeKey, ICollisionBoxListBuilder builder)
		{
			slice = BoxFinderUtils.sliceFromKey(volumeKey);
			this.builder = builder;
			return this;
		}

		@Override
		public int apply(int minX, int minY, int maxX, int maxY)
		{
			setEmpty(minX, minY, slice.min, maxX, maxY, slice.max);
			builder.add(minX, minY, slice.min, maxX + 1, maxY + 1, slice.max + 1);
			return 0;        }
	}

	private final AddBoxConsumer addBoxConsumer = new AddBoxConsumer();

	void addBox(int volumeKey, ICollisionBoxListBuilder builder)
	{
		BoxFinderUtils.testAreaBounds(BoxFinderUtils.patternIndexFromKey(volumeKey), addBoxConsumer.prepare(volumeKey, builder));
	}

	void calcCombined()
	{
		combined[Slice.D1_0.ordinal()] = voxels[0];
		combined[Slice.D1_1.ordinal()] = voxels[1];
		combined[Slice.D1_2.ordinal()] = voxels[2];
		combined[Slice.D1_3.ordinal()] = voxels[3];
		combined[Slice.D1_4.ordinal()] = voxels[4];
		combined[Slice.D1_5.ordinal()] = voxels[5];
		combined[Slice.D1_6.ordinal()] = voxels[6];
		combined[Slice.D1_7.ordinal()] = voxels[7];

		combined[Slice.D2_0.ordinal()] = voxels[0] & voxels[1];
		combined[Slice.D2_1.ordinal()] = voxels[1] & voxels[2];
		combined[Slice.D2_2.ordinal()] = voxels[2] & voxels[3];
		combined[Slice.D2_3.ordinal()] = voxels[3] & voxels[4];
		combined[Slice.D2_4.ordinal()] = voxels[4] & voxels[5];
		combined[Slice.D2_5.ordinal()] = voxels[5] & voxels[6];
		combined[Slice.D2_6.ordinal()] = voxels[6] & voxels[7];

		combined[Slice.D3_0.ordinal()] = combined[Slice.D2_0.ordinal()] & voxels[2];
		combined[Slice.D3_1.ordinal()] = combined[Slice.D2_1.ordinal()] & voxels[3];
		combined[Slice.D3_2.ordinal()] = combined[Slice.D2_2.ordinal()] & voxels[4];
		combined[Slice.D3_3.ordinal()] = combined[Slice.D2_3.ordinal()] & voxels[5];
		combined[Slice.D3_4.ordinal()] = combined[Slice.D2_4.ordinal()] & voxels[6];
		combined[Slice.D3_5.ordinal()] = combined[Slice.D2_5.ordinal()] & voxels[7];

		combined[Slice.D4_0.ordinal()] = combined[Slice.D3_0.ordinal()] & voxels[3];
		combined[Slice.D4_1.ordinal()] = combined[Slice.D3_1.ordinal()] & voxels[4];
		combined[Slice.D4_2.ordinal()] = combined[Slice.D3_2.ordinal()] & voxels[5];
		combined[Slice.D4_3.ordinal()] = combined[Slice.D3_3.ordinal()] & voxels[6];
		combined[Slice.D4_4.ordinal()] = combined[Slice.D3_4.ordinal()] & voxels[7];

		combined[Slice.D5_0.ordinal()] = combined[Slice.D4_0.ordinal()] & voxels[4];
		combined[Slice.D5_1.ordinal()] = combined[Slice.D4_1.ordinal()] & voxels[5];
		combined[Slice.D5_2.ordinal()] = combined[Slice.D4_2.ordinal()] & voxels[6];
		combined[Slice.D5_3.ordinal()] = combined[Slice.D4_3.ordinal()] & voxels[7];

		combined[Slice.D6_0.ordinal()] = combined[Slice.D5_0.ordinal()] & voxels[5];
		combined[Slice.D6_1.ordinal()] = combined[Slice.D5_1.ordinal()] & voxels[6];
		combined[Slice.D6_2.ordinal()] = combined[Slice.D5_2.ordinal()] & voxels[7];

		combined[Slice.D7_0.ordinal()] = combined[Slice.D6_0.ordinal()] & voxels[6];
		combined[Slice.D7_1.ordinal()] = combined[Slice.D6_1.ordinal()] & voxels[7];

		combined[Slice.D8_0.ordinal()] = combined[Slice.D7_0.ordinal()] & voxels[7];
	}

	private class VolumeMaximalConsumer implements IAreaBoundsIntFunction
	{
		Slice slice;
		long pattern;

		VolumeMaximalConsumer prepare(int volKey)
		{
			slice = BoxFinderUtils.sliceFromKey(volKey);
			pattern = BoxFinderUtils.patternFromKey(volKey);
			return this;
		}

		@Override
		public int apply(int minX, int minY, int maxX, int maxY)
		{
			if(slice.min > 0 && isVolumePresent(pattern, BoxFinderUtils.sliceByMinMax(slice.min - 1, slice.max)))
				return 1;

			if(slice.max < 7 && isVolumePresent(pattern, BoxFinderUtils.sliceByMinMax(slice.min, slice.max + 1)))
				return 1;

			if(minX > 0 && isVolumePresent((pattern >>> 1) | pattern, slice))
				return 1;

			if(maxX < 7 && isVolumePresent((pattern << 1) | pattern, slice))
				return 1;

			if(minY > 0 && isVolumePresent((pattern >>> 8) | pattern, slice))
				return 1;

			if(maxY < 7 && isVolumePresent((pattern << 8) | pattern, slice))
				return 1;

			return 0;       }
	}

	private final VolumeMaximalConsumer volumeMaximalConsumer = new VolumeMaximalConsumer();

	/**
	 * True if volume cannot be expanded along any axis.
	 */
	boolean isVolumeMaximal(int volKey)
	{
		return BoxFinderUtils.testAreaBounds(
				BoxFinderUtils.patternIndexFromKey(volKey),
				volumeMaximalConsumer.prepare(volKey)) == 0;
	}

	@FunctionalInterface
	interface IPresenceTest
	{
		boolean apply(int volumeKey);
	}

	void populateMaximalVolumes()
	{
		volumeCount = 0;
		final int[] volumes = maximalVolumes;

		// disabled for release, not strictly needed but prevents confusion during debug
		//        Arrays.fill(volumes, 0, 64, 0);

		// PERF: use an exclusion test voxel hash to reduce search universe
		for(int i = 0; i < BoxFinderUtils.VOLUME_COUNT; i++)
		{
			final int v = BoxFinderUtils.VOLUME_KEYS[i];

			if(isVolumePresent(v) && isVolumeMaximal(v))
			{
				final int c = volumeCount++;
				volumes[c] = v;
				if(c == 63)
					break;
			}
		}
		volumeMask = (1L << volumeCount) - 1;
		//        for(int i = 0; i < volumeCount; i++)
		//        {
		//            for(int j = 0; j < volumeCount; j++)
		//            {
		//                if(i != j && BoxFinderUtils.isVolumeIncluded(volumes[i], volumes[j]))
		//                {
		//                    CanvasMod.LOG.info("REDUNDANT MAXIMAL VOLUMES");
		//                    explainVolume(i);
		//                    explainVolume(j);
		//                }
		//
		//            }
		//        }
	}

	void populateIntersects()
	{
		final long[] intersects = this.intersects;
		final int[] volumes = maximalVolumes;
		final int limit = volumeCount;

		System.arraycopy(EMPTY, 0, intersects, 0, 64);

		if(limit < 2)
			return;

		for(int i = 1; i < limit; i++)
		{
			final int a = volumes[i];

			for(int j = 0; j < i; j++)
			{
				if(BoxFinderUtils.doVolumesIntersect(a, volumes[j]))
				{
					// could store only half of values, but
					// makes lookups and some tests easier later on
					// to simply update both halves while we're here.
					intersects[i] |= (1L << j);
					intersects[j] |= (1L << i);
					//                    intersectCount++;
				}
			}
		}
	}

	long noIntersectMask = 0;

	/**
	 * Finds volumes that do not intersect with any other volume.
	 */
	void classifyVolumeIntersections()
	{
		final long[] intersects = this.intersects;
		final int limit = volumeCount;
		noIntersectMask = 0;
		for(int i = 1; i < limit; i++)
		{
			if(intersects[i] == 0)
				noIntersectMask |= (1L << i);
		}
		//        CanvasMod.LOG.info("noIntersectCount = %d", Long.bitCount(noIntersectMask));
	}

	boolean isVolumePresent(int volKey)
	{
		return isVolumePresent(BoxFinderUtils.patternFromKey(volKey), BoxFinderUtils.sliceFromKey(volKey));
	}

	boolean isVolumePresent(long pattern, Slice slice)
	{
		return (pattern & combined[slice.ordinal()]) == pattern;
	}

	void fillVolume(int volKey)
	{
		final long pattern = BoxFinderUtils.patternFromKey(volKey);
		final Slice slice = BoxFinderUtils.sliceFromKey(volKey);

		for(int i = slice.min; i <= slice.max; i++)
		{
			voxels[i] |= pattern;
		}
	}

	private class FillUnionConsumer implements IAreaBoundsIntFunction
	{
		Slice s0;
		Slice s1
		;
		// +1 because max is inclusive
		int minZ;
		int maxZ;
		int innerPattern;

		int minX0, minY0, maxX0, maxY0;

		FillUnionConsumer prepare(int vol0, int vol1)
		{
			s0 = BoxFinderUtils.sliceFromKey(vol0);
			s1 = BoxFinderUtils.sliceFromKey(vol1);
			// +1 because max is inclusive
			minZ = Math.min(s0.min, s1.min);
			maxZ = Math.max(s0.max, s1.max);

			innerPattern = BoxFinderUtils.patternIndexFromKey(vol1);

			return this;
		}

		private class FillUnionConsumerInner implements IAreaBoundsIntFunction
		{
			@Override
			public int apply(int minX1, int minY1, int maxX1, int maxY1)
			{
				final int minX =  Math.min(minX0, minX1);
				final int maxX =  Math.max(maxX0, maxX1);
				final int minY =  Math.min(minY0, minY1);
				final int maxY =  Math.max(maxY0, maxY1);
				for(int x = minX; x <= maxX; x++)
				{
					for(int y = minY; y <= maxY; y++)
					{
						for(int z = minZ; z <= maxZ; z++)
						{
							setFilled(x, y, z);
						}
					}
				}
				return 0;
			}

		}

		private final FillUnionConsumerInner inner = new FillUnionConsumerInner();

		@Override
		public int apply(int minX0, int minY0, int maxX0, int maxY0)
		{
			this.minX0 = minX0;
			this.minY0 = minY0;
			this.maxX0 = maxX0;
			this.maxY0 = maxY0;

			return BoxFinderUtils.testAreaBounds(innerPattern, inner);
		}
	}

	private final FillUnionConsumer fillUnionConsumer = new FillUnionConsumer();

	/**
	 * Fills voxels the exist in minimum volume encompassing both given volumes.
	 */
	void fillUnion(int vol0, int vol1)
	{
		BoxFinderUtils.testAreaBounds(BoxFinderUtils.patternIndexFromKey(vol0), fillUnionConsumer.prepare(vol0, vol1));
	}

	int countFilledVoxels(int volKey)
	{
		final long pattern = BoxFinderUtils.patternFromKey(volKey);
		final Slice slice = BoxFinderUtils.sliceFromKey(volKey);

		int result = 0;

		for(int i = slice.min; i <= slice.max; i++)
		{
			result += Long.bitCount(voxels[i] & pattern);
		}

		return result;
	}

	class VolumeScoreAccumulator implements IntConsumer
	{
		int actorVolume;
		int total;

		@Override
		public void accept(int value)
		{
			total += BoxFinderUtils.splitScore(actorVolume, maximalVolumes[value]);
		}

		void prepare(int actorVolume)
		{
			this.actorVolume = actorVolume;
			total = 0;
		}
	}

	private final VolumeScoreAccumulator volumeScorecounter = new VolumeScoreAccumulator();

	void scoreMaximalVolumes()
	{
		final int[] scores = volumeScores;
		final long[] intersects = this.intersects;
		final int limit = volumeCount;
		final VolumeScoreAccumulator counter = volumeScorecounter;

		for(int i = 0; i < limit; i++)
		{
			counter.prepare(maximalVolumes[i]);
			BitHelper.forEachBit(intersects[i], counter);
			scores[i] = counter.total;
		}

	}
}
