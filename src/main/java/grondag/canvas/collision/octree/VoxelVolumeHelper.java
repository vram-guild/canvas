package grondag.canvas.collision.octree;

public class VoxelVolumeHelper
{

	static void setBit(int index, long[] target)
	{
		target[index >> 6] |= (1L << (index & 63));
	}

	static void clearBit(int index, long[] target)
	{
		target[index >> 6] &= ~(1L << (index & 63));
	}

	static boolean isClear(int index, long[] src)
	{
		return (src[index >> 6] & (1L << (index & 63))) == 0;
	}

	static boolean isSet(int index, long[] src)
	{
		return !isClear(index, src);
	}

}
