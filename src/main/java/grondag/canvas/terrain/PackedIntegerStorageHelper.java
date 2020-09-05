package grondag.canvas.terrain;

import java.util.concurrent.ArrayBlockingQueue;
import net.minecraft.util.collection.PackedIntegerArray;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import grondag.canvas.mixinterface.PackedIntegerArrayExt;

/**
 * Minimize new allocation for chunk storage copies by recycling the arrays.
 */
public class PackedIntegerStorageHelper {

	private static final ArrayBlockingQueue<IntArrayList> POOL = new ArrayBlockingQueue<>(1024);

	private static IntArrayList claimList() {
		final IntArrayList list = POOL.poll();
		return list == null ? new IntArrayList(512) : list;
	}

	public static void release(IntArrayList list) {
		list.clear();
		POOL.offer(list);
	}

	public static IntArrayList claim(PackedIntegerArray array) {
		final IntArrayList list = claimList();
		((PackedIntegerArrayExt) array).canvas_fastForEach(list);
		return list;
	}
}
