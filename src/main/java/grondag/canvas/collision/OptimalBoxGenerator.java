package grondag.canvas.collision;

import static grondag.canvas.collision.octree.OctreeCoordinates.ALL_EMPTY;

import com.google.common.collect.ImmutableList;

import net.minecraft.util.math.Box;

import grondag.canvas.collision.octree.OctreeCoordinates;
import grondag.canvas.collision.octree.VoxelVolume16;

public class OptimalBoxGenerator extends AbstractBoxGenerator
{
	// TODO: make config or remove
	static final int BOX_BUDGET = 8;

	static void setVoxelBit(int voxelIndex4, long[] voxelBits)
	{
		final int xyz = OctreeCoordinates.indexToXYZ4(voxelIndex4);
		voxelBits[xyz >> 6] |= (1L << (xyz & 63));
	}

	public static final double VOXEL_VOLUME = 1.0 / 8 / 8 / 8;

	private final long[] voxelBits = new long[128];
	private final SimpleBoxListBuilder builder = new SimpleBoxListBuilder();
	final long[] snapshot = new long[8];
	final BoxFinder bf = new BoxFinder();

	/**
	 * Returns voxel volume of loaded mesh after simplification. Simplification level is estimated
	 * based on the count of maximal bounding volumes vs the budget per mesh.
	 * Call after inputing mesh via {@link #accept(grondag.exotic_matter.model.primitives.IPolygon)}
	 * and before calling {@link #build()} <p>.
	 *
	 * Returns -1 if mesh isn't appropriate for optimization.
	 */
	public final double prepare()
	{
		final long[] data = voxelBits;
		VoxelVolume16.fillVolume(data);
		bf.clear();
		VoxelVolume16.forEachSimpleVoxel(data, 4, (x, y, z) -> bf.setFilled(x, y, z));

		// handle very small meshes that don't half-fill any simple voxels; avoid having no collision boxes
		if(bf.isEmpty()) {
			VoxelVolume16.forEachSimpleVoxel(data, 1, (x, y, z) -> bf.setFilled(x, y, z));
		}

		// prep for next use
		System.arraycopy(ALL_EMPTY, 0, data, 0, 64);

		bf.calcCombined();
		bf.populateMaximalVolumes();

		// find maximal volumes to enable estimate of simplification level

		final int overage = bf.volumeCount - BOX_BUDGET;

		if(overage > 0) {
			bf.simplify(overage);
			bf.calcCombined();
			bf.populateMaximalVolumes();
			if(bf.volumeCount > 16)
				return -1;
		}

		bf.saveTo(snapshot);
		int voxelCount = 0;

		for(final long bits : snapshot) {
			voxelCount += Long.bitCount(bits);
		}

		return voxelCount * VOXEL_VOLUME;
	}

	public final ImmutableList<Box> build()
	{
		builder.clear();
		bf.outputBoxes(builder);
		return builder.build();
	}
}
