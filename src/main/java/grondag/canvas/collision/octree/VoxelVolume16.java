package grondag.canvas.collision.octree;

import grondag.canvas.collision.octree.OctreeCoordinates.Int3Consumer;

/**
 * Operations on Cartesian representation of 16x16x16 voxels in unit cube
 * that can happen more efficiently that way. (Filling, mostly)
 */
public class VoxelVolume16
{
	public static void forEachSimpleVoxel(long[] data, final int minVoxelCount, Int3Consumer consumer)
	{
		for(int x = 0; x < 16; x += 2)
		{
			for(int y = 0; y < 16; y += 2)
			{
				final long mask = 0b00000000000000110000000000000011L << (x + (y & 3) * 16);

				for(int z = 0; z < 16; z += 2)
				{
					final int i = ((z << 2) | (y >> 2));
					final int count = Long.bitCount(data[i + 64] & mask) + Long.bitCount(data[i + 68] & mask);
					if(count >= minVoxelCount)
						consumer.accept(x >> 1, y >> 1, z >> 1);
				}
			}
		}
	}

	private static final long[] INTERIOR_MASK_XY = { 0x7FFE7FFE7FFE0000L, 0x7FFE7FFE7FFE7FFEL, 0x7FFE7FFE7FFE7FFEL, 0x00007FFE7FFE7FFEL};

	/**
	 * Puts result of ~ operator on input into result.
	 * Operates on 256-bit word stored as 4 long values.
	 * Index is of the compound word, NOT the array index. (Array index would be x4).
	 */
	private static void compoundNot(final long[] input, int inputIndex, final long[] result, int resultIndex)
	{
		inputIndex *= 4;
		resultIndex *= 4;
		result[resultIndex] = ~input[inputIndex];
		result[resultIndex + 1] = ~input[inputIndex + 1];
		result[resultIndex + 2] = ~input[inputIndex + 2];
		result[resultIndex + 3] = ~input[inputIndex + 3];
	}

	/**
	 * Clears bits in target if they are set in the mask.
	 * Equivalent of <target> &= ~<mask>
	 * Operates on 256-bit word stored as 4 long values.
	 * Index is of the compound word, NOT the array index. (Array index would be x4).
	 */
	private static void compoundClear(final long[] target, int targetIndex, final long[] mask, int maskIndex)
	{
		targetIndex *= 4;
		maskIndex *= 4;
		target[targetIndex] &= ~mask[maskIndex];
		target[targetIndex + 1] &= ~mask[maskIndex + 1];
		target[targetIndex + 2] &= ~mask[maskIndex + 2];
		target[targetIndex + 3] &= ~mask[maskIndex + 3];
	}

	/**
	 * Sets bits in target if they are set in the mask.
	 * Equivalent of <target> |= <mask>
	 * Operates on 256-bit word stored as 4 long values.
	 * Index is of the compound word, NOT the array index. (Array index would be x4).
	 */
	private static void compoundSet(final long[] target, int targetIndex, final long[] mask, int maskIndex)
	{
		targetIndex *= 4;
		maskIndex *= 4;
		target[targetIndex] |= mask[maskIndex];
		target[targetIndex + 1] |= mask[maskIndex + 1];
		target[targetIndex + 2] |= mask[maskIndex + 2];
		target[targetIndex + 3] |= mask[maskIndex + 3];
	}

	/**
	 * Puts ~(input | mask) into result.
	 * Operates on 256-bit word stored as 4 long values.
	 * Index is of the compound word, NOT the array index. (Array index would be x4).
	 */
	private static void compoundNotOrMask(final long[] input, int inputIndex, final long[] result, int resultIndex, final long[] mask, int maskIndex)
	{
		inputIndex *= 4;
		resultIndex *= 4;
		maskIndex *= 4;
		result[resultIndex] = ~(input[inputIndex] | mask[maskIndex]);
		result[resultIndex + 1] = ~(input[inputIndex + 1] | mask[maskIndex + 1]);
		result[resultIndex + 2] = ~(input[inputIndex + 2] | mask[maskIndex + 2]);
		result[resultIndex + 3] = ~(input[inputIndex + 3] | mask[maskIndex + 3]);
	}

	private static boolean isZero(final long[] input, int inputIndex)
	{
		inputIndex *= 4;
		return input[inputIndex] == 0
				&& input[inputIndex + 1] == 0
				&& input[inputIndex + 2] == 0
				&& input[inputIndex + 3] == 0;
	}

	private static final ThreadLocal<long[]> utilityWord = new ThreadLocal<long[]>()
	{
		@Override
		protected long[] initialValue()
		{
			return new long[4];
		}
	};


	/**
	 * Fills all interior voxels not reachable from an exterior voxel that is already clear.
	 * Works by starting with a full volume and "etching" from the outside via simple
	 * flood fill until it is stopped by the shell data provided.<p>
	 *
	 * To ensure low garbage, requires array be sized to hold two sets of results.
	 * Expects source data in lower half.  Output is in upper half.
	 */
	public static void fillVolume(long[] data)
	{
		// during processing, 1 bits in high words represent open voxels

		// open voxels in Z end slices are definitely open
		compoundNot(data, 0, data, 16);
		compoundNot(data, 15, data, 31);

		// any voxels accessible from edges of x,y slices are also open
		for(int z = 1; z < 15; z++)
		{
			compoundNotOrMask(data, z, data, z + 16, INTERIOR_MASK_XY, 0);
		}

		final long[] temp = utilityWord.get();

		while(fill(data, temp)) {}

		// flip carved bits to represent filled voxels instead of open
		// needed by the output routine, which looks for set bits
		for(int i = 64; i < 128; i++)
			data[i] = ~data[i];
	}

	private static void propagateXY(long[] word)
	{
		final long d = word[3];
		final long c = word[2];
		final long b = word[1];
		final long a = word[0];

		word[3] |= ((d << 1) | (d >>> 1) | (d << 16) | (d >>> 16) | (c >>> 48));
		word[2] |= ((c << 1) | (c >>> 1) | (c << 16) | (c >>> 16) | (b >>> 48) | (d << 48));
		word[1] |= ((b << 1) | (b >>> 1) | (b << 16) | (b >>> 16) | (a >>> 48) | (c << 48));
		word[0] |= ((a << 1) | (a >>> 1) | (a << 16) | (a >>> 16) | (b << 48));
	}

	/**
	 * temp is long[4] working space provided by caller
	 * to prevent garbage and allocation overhead
	 */
	static boolean fill(long[] data, long[] temp)
	{
		boolean didFill = false;

		for(int z = 1; z <= 14; z++)
		{
			// get carved current state
			System.arraycopy(data, 64 + z * 4, temp, 0, 4);

			// propagate voxels in previous and following layer
			compoundSet(temp, 0, data, z + 15);
			compoundSet(temp, 0, data, z + 17);

			// propagate open voxels left, right, up and down, ignoring voxels already open in current state
			propagateXY(temp);

			// remove voxels that are already carved
			compoundClear(temp, 0, data, z + 16);

			// remove voxels that are solid in template
			compoundClear(temp, 0, data, z);

			// if nothing new to carve, move on
			if(isZero(temp, 0)) continue;

			// finally, propagate open voxels into carved results
			compoundSet(data, 16 + z, temp, 0);
			didFill = true;
		}
		return didFill;
	}
}
