package grondag.canvas.collision.octree;


import java.util.Arrays;

public class OctreeCoordinates
{
	@FunctionalInterface
	public interface Int3Consumer {
		void accept(int x, int y, int z);
	}

	@FunctionalInterface
	public interface Float3Consumer {
		void accept(float x, float y, float z);
	}

	@FunctionalInterface
	public interface Float3Test {
		boolean apply(float x, float y, float z);
	}

	@FunctionalInterface
	public interface IBoxBoundsIntConsumer {
		void accept(int x0, int y0, int z0, int x1, int y1, int z1);
	}

	@FunctionalInterface
	public interface IAreaBoundsIntFunction {
		int apply(int x0, int y0, int x1, int y1);
	}

	@FunctionalInterface
	public interface IBoxBoundsIntFunction {
		int accept(int x0, int y0, int z0, int x1, int y1, int z1);
	}

	@FunctionalInterface
	public interface IBoxBoundsObjectFunction<V> {
		V accept(int x0, int y0, int z0, int x1, int y1, int z1);
	}

	public static final long FULL_BITS = 0xFFFFFFFFFFFFFFFFL;
	public static final long[] ALL_FULL = new long[64];
	public static final long[] ALL_EMPTY = new long[64];

	/**
	 * Indexes to face voxels in division level 4
	 */
	static final int[] EXTERIOR_INDEX_4 = new int[1352];

	/**
	 * Indexes to face voxels in division level 3
	 */
	static final int[] EXTERIOR_INDEX_3 = new int[1352];

	static
	{
		Arrays.fill(ALL_FULL, FULL_BITS);

		int exteriorIndex = 0;
		for(int i = 0; i < 4096; i++)
		{
			final int xyz = indexToXYZ4(i);
			final int x = xyz & 15;
			final int y = (xyz >> 4) & 15;
			final int z = (xyz >> 8) & 15;

			if(x == 0 || x == 15)
			{
				EXTERIOR_INDEX_4[exteriorIndex++] = xyzToIndex4(xyz);
				continue;
			}

			if(y == 0 || y == 15)
			{
				EXTERIOR_INDEX_4[exteriorIndex++] = xyzToIndex4(xyz);
				continue;
			}

			if(z == 0 || z == 15)
			{
				EXTERIOR_INDEX_4[exteriorIndex++] = xyzToIndex4(xyz);
				continue;
			}

		}
		assert exteriorIndex == 1352;

		int exteriorBottomIndex = 0;
		for(int i = 0; i < 512; i++)
		{
			final int xyz = indexToXYZ3(i);
			final int x = xyz & 7;
			final int y = (xyz >> 3) & 7;
			final int z = (xyz >> 6) & 7;

			if(x == 0 || x == 7)
			{
				EXTERIOR_INDEX_3[exteriorBottomIndex++] = xyzToIndex3(xyz);
				continue;
			}

			if(y == 0 || y == 7)
			{
				EXTERIOR_INDEX_3[exteriorBottomIndex++] = xyzToIndex3(xyz);
				continue;
			}

			if(z == 0 || z == 7)
			{
				EXTERIOR_INDEX_3[exteriorBottomIndex++] = xyzToIndex3(xyz);
				continue;
			}

		}
		assert exteriorBottomIndex == 296;
	}

	static int xyzToIndex(final int xyz, final int divisionLevel)
	{
		switch(divisionLevel)
		{
		case 0:
			return 0;

		case 1:
			return xyz;

		case 2:
			return xyzToIndex2(xyz);

		case 3:
			return xyzToIndex3(xyz);

		case 4:
			return xyzToIndex4(xyz);
		}
		return 0;
	}

	static int indexToXYZ(final int index, final int divisionLevel)
	{
		switch(divisionLevel)
		{
		case 0:
			return 0;

		case 1:
			return index;

		case 2:
			return indexToXYZ2(index);

		case 3:
			return indexToXYZ3(index);

		case 4:
			return indexToXYZ4(index);
		}
		return 0;
	}

	/**
	 * Gives octree index w/ division level 2 from packed 2-bit Cartesian coordinates
	 */
	static int xyzToIndex2(final int xyz2)
	{
		final int y = xyz2 >> 1;
		final int z = xyz2 >> 2;

		return (xyz2 & 1) | (y & 2) | (z & 4)
				| (((xyz2 & 2) | (y & 4) | (z & 8)) << 2);
	}

	/**
	 * Gives packed 2-bit Cartesian coordinates from octree index w/ division level 2
	 */
	static int indexToXYZ2(final int i2)
	{
		final int j = i2 >> 2;
				return ((i2 & 1) | (j & 2))
						| (((i2 & 2) | (j & 4)) << 1)
						| (((i2 & 4) | (j & 8)) << 2);
	}

	/**
	 * Packed 2-bit Cartesian coordinates
	 */
	static int packedXYZ2(int x, int y, int z)
	{
		return x | (y << 2) | (z << 4);
	}

	static int xyzToIndex2(int x, int y, int z)
	{
		return xyzToIndex2(packedXYZ2(x, y, z));
	}

	/**
	 * Gives octree index w/ division level 3 from packed 3-bit Cartesian coordinates
	 */
	static int xyzToIndex3(final int xyz3)
	{
		//coordinate values are 3 bits each: xxx, yyy, zzz
		//voxel coordinates are interleaved: zyx zyx zyx

		// shift all bits of y, z at once to avoid separate shift ops later

		final int y = xyz3 >> 2;
		final int z = xyz3 >> 4;

		return (xyz3 & 1) | (y & 2) | (z & 4)
				| (((xyz3 & 2) | (y & 4) | (z & 8)) << 2)
				| (((xyz3 & 4) | (y & 8) | (z & 16)) << 4);
	}

	/**
	 * Gives packed 3-bit Cartesian coordinates from octree index w/ division level 3
	 */
	public static int indexToXYZ3(final int i3)
	{
		//coordinate values are 3 bits each: xxx, yyy, zzz
		//voxel coordinates are interleaved: zyx zyx zyx

		final int j = i3 >> 2;
				final int k = i3 >> 4;
				return ((i3 & 1) | (j & 2) | (k & 4))
						| (((i3 & 2) | (j & 4) | (k & 8)) << 2)
						| (((i3 & 4) | (j & 8) | (k & 16)) << 4);
	}

	public static void forXYZ3(final int i3, Int3Consumer consumer)
	{
		final int j = i3 >> 2;
						final int k = i3 >> 4;
						consumer.accept(
								(i3 & 1) | (j & 2) | (k & 4),
								((i3 & 2) | (j & 4) | (k & 8)) >> 1,
								((i3 & 4) | (j & 8) | (k & 16)) >> 2);
	}

	/**
	 * Packed 3-bit Cartesian coordinates
	 */
	static int packedXYZ3(int x, int y, int z)
	{
		return x | (y << 3) | (z << 6);
	}

	static int xyzToIndex3(int x, int y, int z)
	{
		return xyzToIndex3(packedXYZ3(x, y, z));
	}

	/**
	 * Gives octree index w/ division level 4 from packed 4-bit Cartesian coordinates
	 */
	static int xyzToIndex4(final int xyz4)
	{
		//coordinate values are 4 bits each: xxxx, yyyy, zzzz
		//voxel coordinates are interleaved: zyx zyx zyx zyx

		// shift all bits of y, z at once to avoid separate shift ops later
		// like so:
		//   xxxx
		//  yyyy
		// zzzz

		final int y = xyz4 >> 3;
		final int z = xyz4 >> 6;

		return (xyz4 & 1) | (y & 2) | (z & 4)
				| (((xyz4 & 2) | (y & 4) | (z & 8)) << 2)
				| (((xyz4 & 4) | (y & 8) | (z & 16)) << 4)
				| (((xyz4 & 8) | (y & 16) | (z & 32)) << 6);
	}

	static int xyzToIndex4(int x, int y, int z)
	{
		// PERF: avoid packing/unpacking
		return xyzToIndex4(packedXYZ4(x, y, z));
	}

	/**
	 * Gives packed 4-bit Cartesian coordinates from octree index w/ division level 4
	 */
	public static int indexToXYZ4(final int i4)
	{
		//coordinate values are 4 bits each: xxxx, yyyy, zzzz
		//voxel coordinates are interleaved: zyx zyx zyx zyx

		final int j = i4 >> 2;
				final int k = i4 >> 4;
				final int l = i4 >> 6;

				return ((i4 & 1) | (j & 2) | (k & 4) | (l & 8))
						| (((i4 & 2) | (j & 4) | (k & 8) | (l & 16)) << 3)
						| (((i4 & 4) | (j & 8) | (k & 16) | (l & 32)) << 6);
	}

	/**
	 * Packed 4-bit Cartesian coordinates
	 */
	static int packedXYZ4(int x, int y, int z)
	{
		return x | (y << 4) | (z << 8);
	}

	public static float voxelSize(int divisionLevel)
	{
		return 1f / (1 << divisionLevel);
	}

	public static float voxelRadius(int divisionLevel)
	{
		return 0.5f / (1 << divisionLevel);
	}

	public static void withCenter(final int index, final int divisionLevel, Float3Consumer consumer)
	{
		final int xyz = indexToXYZ(index, divisionLevel);
		final float d = OctreeCoordinates.voxelSize(divisionLevel);
		final int mask = (1 << divisionLevel) - 1;
		consumer.accept(
				((xyz & mask) + 0.5f) * d,
				(((xyz >> divisionLevel) & mask) + 0.5f) * d,
				(((xyz >> (divisionLevel * 2)) & mask) + 0.5f) * d);
	}

	public static boolean testCenter(final int index, final int divisionLevel, Float3Test test)
	{
		final int xyz = indexToXYZ(index, divisionLevel);
		final float d = OctreeCoordinates.voxelSize(divisionLevel);
		final int mask = (1 << divisionLevel) - 1;
		return test.apply(
				((xyz & mask) + 0.5f) * d,
				(((xyz >> divisionLevel) & mask) + 0.5f) * d,
				(((xyz >> (divisionLevel * 2)) & mask) + 0.5f) * d);
	}

	public static void withXYZ(final int index, final int divisionLevel, Int3Consumer consumer)
	{
		final int xyz = indexToXYZ(index, divisionLevel);
		final int mask = (1 << divisionLevel) - 1;
		consumer.accept(
				xyz & mask,
				(xyz >> divisionLevel) & mask,
				(xyz >> (divisionLevel * 2)) & mask);
	}

	/**
	 * Gives numerators of AABB coordinates aligned to 1/8 divisions.
	 * Meant only for division levels 0-3. (Level 4 is 1/16)
	 */
	public static void withBounds8(final int index, final int divisionLevel, IBoxBoundsIntConsumer consumer)
	{
		final int xyz = indexToXYZ(index, divisionLevel);
		final int mask = (1 << divisionLevel) - 1;
		final int x = xyz & mask;
		final int y = (xyz >> divisionLevel) & mask;
		final int z = (xyz >> (divisionLevel * 2)) & mask;
		final int f = 1 << (3 - divisionLevel);

		consumer.accept(
				x * f, y * f, z * f,
				(x + 1) * f, (y + 1) * f, (z + 1) * f);
	}
}
