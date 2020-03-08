package grondag.canvas.collision;

import static grondag.canvas.collision.CollisionBoxEncoder.MAX_X_SHIFT;
import static grondag.canvas.collision.CollisionBoxEncoder.MAX_Y_SHIFT;
import static grondag.canvas.collision.CollisionBoxEncoder.MAX_Z_SHIFT;
import static grondag.canvas.collision.CollisionBoxEncoder.MIN_X_SHIFT;
import static grondag.canvas.collision.CollisionBoxEncoder.MIN_Y_SHIFT;
import static grondag.canvas.collision.CollisionBoxEncoder.MIN_Z_SHIFT;
import static grondag.canvas.collision.CollisionBoxEncoder.X_AXIS;
import static grondag.canvas.collision.CollisionBoxEncoder.Y_AXIS;
import static grondag.canvas.collision.CollisionBoxEncoder.Z_AXIS;

import java.util.Arrays;
import java.util.function.IntConsumer;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

/**
 * Accumulates immutable, low-garbage (via cache) lists of collision boxes
 * and automatically combines boxes that share a surface. <p>
 */
public class JoiningBoxListBuilder implements ICollisionBoxListBuilder
{
	@SuppressWarnings("unused")
	private static final int MIN_A_SHIFT = 0;
	private static final int MIN_B_SHIFT = 2;
	private static final int MAX_A_SHIFT = 4;
	private static final int MAX_B_SHIFT = 6;
	private static final int AXIS_DEPTH_SHIFT = 8;
	// x9 because 3 axis and 3 depths
	private static final int MAX_FACE_KEYS = 4 * 4 * 4 * 4 * 9;

	private static final int[] EMPTY_FACE_BOX_MAP = new int[MAX_FACE_KEYS];

	static
	{
		Arrays.fill(EMPTY_FACE_BOX_MAP, -1);
	}

	/**
	 * Similar to method of same name in CollisionBoxEncorder but for 1/4 unit voxels
	 * and does not include exterior faces.<p>
	 *
	 * depth is 0-2, where 0 is the first interior divison on the given axis
	 * and 2 is the last. (Again, not counting exterior faces.)
	 */
	static int faceKey(int axisOrdinal, int depth, int minA, int minB, int maxA, int maxB)
	{
		return minA
				| (minB << MIN_B_SHIFT)
				| ((maxA - 1) << MAX_A_SHIFT)
				| ((maxB - 1) << MAX_B_SHIFT)
				| ((axisOrdinal * 3 + depth) << AXIS_DEPTH_SHIFT);
	}

	static void forEachFaceKey(int boxKey, IntConsumer faceKeyConsumer)
	{
		CollisionBoxEncoder.forBounds(boxKey, (minX, minY, minZ, maxX, maxY, maxZ) ->
		{
			final int x0 = minX >> 1;
				final int y0 = minY >> 1;
				final int z0 = minZ >> 1;
				final int x1 = maxX >> 1;
				final int y1 = maxY >> 1;
				final int z1 = maxZ >> 1;

				if(y1 != 4) faceKeyConsumer.accept(faceKey(Y_AXIS, y1 - 1, x0, z0, x1, z1));
				if(y0 != 0) faceKeyConsumer.accept(faceKey(Y_AXIS, y0 - 1, x0, z0, x1, z1));

				if(x1 != 4) faceKeyConsumer.accept(faceKey(X_AXIS, x1 - 1, y0, z0, y1, z1));
				if(x0 != 0) faceKeyConsumer.accept(faceKey(X_AXIS, x0 - 1, y0, z0, y1, z1));

				if(z1 != 4) faceKeyConsumer.accept(faceKey(Z_AXIS, z1 - 1, x0, y0, x1, y1));
				if(z0 != 0) faceKeyConsumer.accept(faceKey(Z_AXIS, z0 - 1, x0, y0, x1, y1));
				return 0;
		});
	}

	//    private final Int2IntOpenHashMap faceToBoxMap = new Int2IntOpenHashMap();

	private final int[] faceBoxMap = new int[MAX_FACE_KEYS];

	private final IntOpenHashSet boxSet = new IntOpenHashSet();

	public JoiningBoxListBuilder()
	{
		System.arraycopy(EMPTY_FACE_BOX_MAP, 0, faceBoxMap, 0, MAX_FACE_KEYS);
	}

	@Override
	public void clear()
	{
		System.arraycopy(EMPTY_FACE_BOX_MAP, 0, faceBoxMap, 0, MAX_FACE_KEYS);
		boxSet.clear();
	}

	/**
	 * Removes all faces keys in addition to box itself.
	 */
	private void removeBox(int boxKey)
	{
		forEachFaceKey(boxKey, k -> faceBoxMap[k] = -1);
		boxSet.rem(boxKey);
	}

	private void addBox(int boxKey)
	{
		boxSet.add(boxKey);
		forEachFaceKey(boxKey, k -> faceBoxMap[k] = boxKey);
	}

	//TODO: remove
	//    private class FaceAccumulator implements IntConsumer
	//    {
	//        int bestKey = NOT_FOUND;
	//        int bestVolume = NOT_FOUND;
	//
	//        void prepare()
	//        {
	//            bestKey = NOT_FOUND;
	//            bestVolume = NOT_FOUND;
	//        }
	//
	//        @Override
	//        public void accept(int k)
	//        {
	//            int testKey = faceToBoxMap.get(k);
	//            if(testKey != NOT_FOUND )
	//            {
	//                int v = CollisionBoxEncoder.boxVolume(testKey);
	//                if(v > bestVolume)
	//                {
	//                    bestKey = testKey;
	//                    bestVolume = v;
	//                }
	//            }
	//        }
	//    }
	//
	//    private final FaceAccumulator faceAccumulator = new FaceAccumulator();

	@Override
	public void add(final int boxKey)
	{
		final int x0 = (boxKey >> (MIN_X_SHIFT + 1)) & 0x7;
		final int y0 = (boxKey >> (MIN_Y_SHIFT + 1)) & 0x7;
		final int z0 = (boxKey >> (MIN_Z_SHIFT + 1)) & 0x7;
		final int x1 = (boxKey >> (MAX_X_SHIFT + 1)) & 0x7;
		final int y1 = (boxKey >> (MAX_Y_SHIFT + 1)) & 0x7;
		final int z1 = (boxKey >> (MAX_Z_SHIFT + 1)) & 0x7;

		if(y1 != 4 && tryCombine(boxKey, faceKey(Y_AXIS, y1 - 1, x0, z0, x1, z1)))
			return;

		if(y0 != 0 && tryCombine(boxKey, faceKey(Y_AXIS, y0 - 1, x0, z0, x1, z1)))
			return;

		if(x1 != 4 && tryCombine(boxKey, faceKey(X_AXIS, x1 - 1, y0, z0, y1, z1)))
			return;

		if(x0 != 0 && tryCombine(boxKey, faceKey(X_AXIS, x0 - 1, y0, z0, y1, z1)))
			return;

		if(z1 != 4 && tryCombine(boxKey, faceKey(Z_AXIS, z1 - 1, x0, y0, x1, y1)))
			return;

		if(z0 != 0 && tryCombine(boxKey, faceKey(Z_AXIS, z0 - 1, x0, y0, x1, y1)))
			return;

		addBox(boxKey);
	}

	boolean tryCombine(int boxKey, int faceKey)
	{
		final int k = faceBoxMap[faceKey];
		if(k == -1 )
			return false;

		removeBox(k);
		add(CollisionBoxEncoder.combineBoxes(boxKey, k));
		return true;
	}

	@Override
	public IntCollection boxes()
	{
		return boxSet;
	}
}