package grondag.canvas.collision;

import java.util.function.IntConsumer;

import net.minecraft.util.math.Direction;

import grondag.canvas.collision.octree.OctreeCoordinates.IBoxBoundsIntFunction;
import grondag.canvas.collision.octree.OctreeCoordinates.IBoxBoundsObjectFunction;

/**
 * Static methods for encoding and decoding 1/8 ^ 3 AABB within a unit
 * cube into/from primitive values.  These are used to reduce garbage and improve LOR in implementation.
 */
public class CollisionBoxEncoder
{
	@SuppressWarnings("unused")
	static final int MIN_X_SHIFT = 0;
	static final int MIN_Y_SHIFT = 4;
	static final int MIN_Z_SHIFT = 8;
	static final int MAX_X_SHIFT = 12;
	static final int MAX_Y_SHIFT = 16;
	static final int MAX_Z_SHIFT = 20;

	@SuppressWarnings("unused")
	private static final int AXIS_SHIFT = 0;
	private static final int DEPTH_SHIFT = 4;
	private static final int MIN_A_SHIFT = 8;
	private static final int MIN_B_SHIFT = 12;
	private static final int MAX_A_SHIFT = 16;
	private static final int MAX_B_SHIFT = 20;

	static final int X_AXIS = Direction.Axis.X.ordinal();
	static final int Y_AXIS = Direction.Axis.Y.ordinal();
	static final int Z_AXIS = Direction.Axis.Z.ordinal();

	/**
	 * Encodes an AABB within a unit cube sliced into eights on each axis.
	 * Values must be 0-8.  Values do not need to be sorted but cannot be equal.
	 */
	static int boxKey(int x0, int y0, int z0, int x1, int y1, int z1)
	{
		int swap;

		if(x1 < x0)
		{
			swap = x0;
			x0 = x1;
			x1 = swap;
		}

		if(y1 < y0)
		{
			swap = y0;
			y0 = y1;
			y1 = swap;
		}

		if(z1 < z0)
		{
			swap = z0;
			z0 = z1;
			z1 = swap;
		}

		return boxKeySorted(x0, y0, z0, x1, y1, z1);
	}

	/**
	 * Encodes an AABB within a unit cube sliced into eights on each axis.
	 * Values must be 0-8.  This version requires that min & max be pre-sorted on each axis.
	 * If you don't have pre-sorted values, use {@link #boxKey(int, int, int, int, int, int)}.
	 */
	static int boxKeySorted(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
	{
		return minX | (minY << MIN_Y_SHIFT) | (minZ << MIN_Z_SHIFT)
				| (maxX << MAX_X_SHIFT) | (maxY << MAX_Y_SHIFT) | (maxZ << MAX_Z_SHIFT);
	}

	//    static int minX(int boxKey)
	//    {
	//        return boxKey & 0xF;
	//    }
	//
	//    static int maxX(int boxKey)
	//    {
	//        return (boxKey >> MAX_X_SHIFT) & 0xF;
	//    }
	//
	//    static int minY(int boxKey)
	//    {
	//        return (boxKey >> MIN_Y_SHIFT) & 0xF;
	//    }
	//
	//    static int maxY(int boxKey)
	//    {
	//        return (boxKey >> MAX_Y_SHIFT) & 0xF;
	//    }
	//
	//    static int minZ(int boxKey)
	//    {
	//        return (boxKey >> MIN_Z_SHIFT) & 0xF;
	//    }
	//
	//    static int maxZ(int boxKey)
	//    {
	//        return (boxKey >> MAX_Z_SHIFT) & 0xF;
	//    }

	static int boxVolume(int boxKey)
	{
		return forBounds(boxKey, (minX, minY, minZ, maxX, maxY, maxZ) ->
		{
			return (maxX - minX) * (maxY - minY) * (maxZ - minZ);
		});
	}

	/**
	 * Returns box key representing combined AABB of both keys.
	 */
	static int combineBoxes(int boxKey0, int boxKey1)
	{
		return forBounds(boxKey0, (minX0, minY0, minZ0, maxX0, maxY0, maxZ0) ->
		{
			return forBounds(boxKey1, (minX1, minY1, minZ1, maxX1, maxY1, maxZ1) ->
			{
				return boxKeySorted(
						Math.min(minX0, minX1),
						Math.min(minY0, minY1),
						Math.min(minZ0, minZ1),
						Math.max(maxX0, maxX1),
						Math.max(maxY0, maxY1),
						Math.max(maxZ0, maxZ1));
			});
		});
	}

	/**
	 * Returns true if boxes share one or more voxels.
	 */
	static boolean areIntersecting(int boxKey0, int boxKey1)
	{
		return forBounds(boxKey0, (minX0, minY0, minZ0, maxX0, maxY0, maxZ0) ->
		{
			return forBounds(boxKey1, (minX1, minY1, minZ1, maxX1, maxY1, maxZ1) ->
			{
				return minX0 < maxX1
						&& maxX0 > minX1
						&& minY0 < maxY1
						&& maxY0 > minY1
						&& minZ0 <  maxZ1
						&& maxZ0 > minZ1
						? 1 : 0;
			});
		}) == 1;
	}

	/**
	 * Key components (LSB to MSB) are axis, plane depth (on-axis coordinate),
	 * planar min a, b and planar max a, b.<p>
	 *
	 * There is no "front-back" - coplanar bounds should be equal if 2d min/max equal.
	 */
	static int faceKey(int axisOrdinal, int depth, int minA, int minB, int maxA, int maxB)
	{
		return axisOrdinal | (depth << DEPTH_SHIFT)
				| (minA << MIN_A_SHIFT) | (minB << MIN_B_SHIFT)
				| (maxA << MAX_A_SHIFT) | (maxB << MAX_B_SHIFT);
	}

	static void forEachFaceKey(int boxKey, IntConsumer faceKeyConsumer)
	{
		forBounds(boxKey, (minX, minY, minZ, maxX, maxY, maxZ) ->
		{
			faceKeyConsumer.accept(faceKey(Y_AXIS, maxY, minX, minZ, maxX, maxZ));
			faceKeyConsumer.accept(faceKey(Y_AXIS, minY, minX, minZ, maxX, maxZ));
			faceKeyConsumer.accept(faceKey(X_AXIS, maxX, minY, minZ, maxY, maxZ));
			faceKeyConsumer.accept(faceKey(X_AXIS, minX, minY, minZ, maxY, maxZ));
			faceKeyConsumer.accept(faceKey(Z_AXIS, maxZ, minX, minY, maxX, maxY));
			faceKeyConsumer.accept(faceKey(Z_AXIS, minZ, minX, minY, maxX, maxY));
			return 0;
		});
	}

	static int forBounds(int boxKey, IBoxBoundsIntFunction consumer)
	{
		return consumer.accept(boxKey & 0xF,
				(boxKey >> MIN_Y_SHIFT) & 0xF,
				(boxKey >> MIN_Z_SHIFT) & 0xF,
				(boxKey >> MAX_X_SHIFT) & 0xF,
				(boxKey >> MAX_Y_SHIFT) & 0xF,
				(boxKey >> MAX_Z_SHIFT) & 0xF);
	}

	static <V> V forBoundsObject(int boxKey, IBoxBoundsObjectFunction<V> consumer)
	{
		return consumer.accept(boxKey & 0xF,
				(boxKey >> MIN_Y_SHIFT) & 0xF,
				(boxKey >> MIN_Z_SHIFT) & 0xF,
				(boxKey >> MAX_X_SHIFT) & 0xF,
				(boxKey >> MAX_Y_SHIFT) & 0xF,
				(boxKey >> MAX_Z_SHIFT) & 0xF);
	}
}
