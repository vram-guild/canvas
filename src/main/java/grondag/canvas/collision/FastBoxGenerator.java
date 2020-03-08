package grondag.canvas.collision;

import static grondag.canvas.collision.octree.OctreeCoordinates.ALL_EMPTY;

import com.google.common.collect.ImmutableList;

import net.minecraft.util.math.Box;

import grondag.canvas.collision.octree.OctreeCoordinates;
import grondag.canvas.collision.octree.VoxelVolume8;

/**
 * Generates non-intersecting collision boxes for a model within a single block
 * at 1/4 block distance (per axis).<p>
 *
 * Identifies which voxels intersects with polys in the block mesh to build
 * a shell at 1/8 resolution, then fills the shell interior and outputs 1/4 voxels
 * that are at least half full. <p>
 *
 * Output voxels sharing a face joined together by {@link JoiningBoxListBuilder}.
 * No other attempt is made to reduce box count - instead relying on the low
 * resolution to keep box counts reasonable. <p>
 *
 * During the shell identification, voxels are addressed using Octree coordinates
 * but those coordinates are never saved to state (exist only in the call stack.)
 * When leaf nodes are identified, voxel bits are set using Cartesian coordinates
 * converted from octree coordinates because Cartesian representation is better
 * (or at least as good) for the subsequent simplification, fill and output operations.
 */
public class FastBoxGenerator extends AbstractBoxGenerator
{

	static void setVoxelBit(int voxelIndex3, long[] voxelBits)
	{
		final int xyz = OctreeCoordinates.indexToXYZ3(voxelIndex3);
		voxelBits[xyz >> 6] |= (1L << (xyz & 63));
	}

	private final long[] voxelBits = new long[16];
	private final JoiningBoxListBuilder builder = new JoiningBoxListBuilder();


	public final ImmutableList<Box> build()
	{
		builder.clear();
		final long[] data = voxelBits;
		VoxelVolume8.fillVolume(data);
		VoxelVolume8.forEachSimpleVoxel(data, 4, (x, y, z) -> builder.addSorted(x, y, z, x + 2, y + 2, z + 2));

		// handle very small meshes that don't half-fill any simple voxels; avoid having no collision boxes
		if(builder.isEmpty())
			VoxelVolume8.forEachSimpleVoxel(data, 1, (x, y, z) -> builder.addSorted(x, y, z, x + 2, y + 2, z + 2));

		// prep for next use
		System.arraycopy(ALL_EMPTY, 0, data, 0, 16);

		return builder.build();
	}
}
