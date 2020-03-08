package grondag.canvas.collision;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;

/**
 * Makes no attempt to combine boxes.<br>
 * Used when boxes are already known to be optimal.
 */
public class SimpleBoxListBuilder implements ICollisionBoxListBuilder
{
	final IntArrayList boxes = new IntArrayList();

	@Override
	public void clear()
	{
		boxes.clear();
	}

	@Override
	public IntCollection boxes()
	{
		return boxes;
	}

	@Override
	public void add(int boxKey)
	{
		boxes.add(boxKey);
	}
}
