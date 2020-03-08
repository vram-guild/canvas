package grondag.canvas.collision.octree;

import grondag.canvas.collision.octree.OctreeCoordinates.Int3Consumer;

/**
 * Operations on Cartesian representation of 8x8x8 voxels in unit cube
 * that can happen more efficiently that way. (Filling, mostly)
 *
 */
public class VoxelVolume8
{
	public static void forEachSimpleVoxel(long[] data, final int minVoxelCount, Int3Consumer consumer)
	{
		for(int x = 0; x < 8; x += 2)
		{
			for(int y = 0; y < 8; y += 2)
			{
				final long mask = 0b0000001100000011L << (x + y * 8);

				for(int z = 0; z < 8; z += 2)
				{
					final int count = Long.bitCount(data[z + 8] & mask) + Long.bitCount(data[z + 9] & mask);
					if(count >= minVoxelCount)
						consumer.accept(x, y, z);
				}
			}
		}
	}

	static final long INTERIOR_MASK_XY = 0x007E7E7E7E7E7E00L;

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
		data[8] = ~data[0];

		// any voxels accessible from edge of x,y slice are also open
		data[9] = ~(data[1] | INTERIOR_MASK_XY);
		data[10] = ~(data[2] | INTERIOR_MASK_XY);
		data[11] = ~(data[3] | INTERIOR_MASK_XY);
		data[12] = ~(data[4] | INTERIOR_MASK_XY);
		data[13] = ~(data[5] | INTERIOR_MASK_XY);
		data[14] = ~(data[6] | INTERIOR_MASK_XY);

		data[15] = ~data[7];

		while(fill(data)) {}

		// flip carved bits to represent filled voxels instead of open
		// needed by the output routine, which looks for set bits
		for(int i = 8; i < 16; i++)
			data[i] = ~data[i];
	}

	/**
	 * Exploits fact that coarse (8x8x8) voxels for a single
	 * Z-axis slice fit within a single long word.
	 */
	static boolean fill(long[] data)
	{
		boolean didFill = false;

		// note no need to do end slices - will always match initial input
		for(int z = 1; z <= 6; z++)
		{
			// get carved current state
			long opens = data[8 + z];

			// propagate voxels in previous and following layer
			opens |= (data[z + 7] | data[z + 9]);

			// propagate open voxels left, right, up and down
			opens |= ((opens << 1) | (opens >>> 1) | (opens << 8) | (opens >>> 8));

			// remove voxels that are already carved
			opens &= ~data[8 + z];

			// remove voxels that are solid in template
			opens &= ~data[z];

			// if nothing new to carve, move on
			if(opens == 0L) continue;

			// finally, propagate open voxels into carved results
			data[8 + z] |= opens;
			didFill = true;
		}
		return didFill;
	}
}
