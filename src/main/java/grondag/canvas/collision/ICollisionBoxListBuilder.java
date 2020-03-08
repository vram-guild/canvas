package grondag.canvas.collision;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;

import net.minecraft.util.math.Box;

public interface ICollisionBoxListBuilder
{
	IntCollection boxes();

	void clear();

	default ImmutableList<Box> build()
	{
		final IntCollection boxes = boxes();
		if(boxes.isEmpty())
			return ImmutableList.of();
		else
		{
			final ImmutableList.Builder<Box> builder = ImmutableList.builder();

			final IntIterator it = boxes.iterator();
			while(it.hasNext())
			{
				builder.add(CollisionBoxStore.getBox(it.nextInt()));
			}

			return builder.build();
		}
	}

	/**
	 * Adds an AABB within a unit cube sliced into eighths on each axis.
	 * Values must be 0-8.  Values do not need to be sorted but cannot be equal.
	 */
	default void add(int x0, int y0, int z0, int x1, int y1, int z1)
	{
		add(CollisionBoxEncoder.boxKey(x0, y0, z0, x1, y1, z1));
	}

	/**
	 * Adds an AABB within a unit cube sliced into eighths on each axis.
	 * Values must be 0-8.  This version requires that min & max be pre-sorted on each axis.
	 * If you don't have pre-sorted values, use {@link #add(int, int, int, int, int, int)}.
	 */
	default void addSorted(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
	{
		add(CollisionBoxEncoder.boxKeySorted(minX, minY, minZ, maxX, maxY, maxZ));
	}

	default int size()
	{
		return boxes().size();
	}

	default boolean isEmpty()
	{
		return boxes().isEmpty();
	}

	void add(int boxKey);

	default long checkSum()
	{
		final IntCollection boxes = boxes();
		if(boxes.isEmpty())
			return 0;
		else
		{
			long result = 0;
			final IntIterator it = boxes.iterator();
			while(it.hasNext())
				result += it.nextInt();
			return result;
		}
	}
}