package grondag.canvas.terrain.render;

import java.util.concurrent.ArrayBlockingQueue;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DelegateLists {
	private static final ArrayBlockingQueue<ObjectArrayList<DrawableDelegate>> delegateLists = new ArrayBlockingQueue<>(
			4096);

	public static ObjectArrayList<DrawableDelegate> getReadyDelegateList() {
		ObjectArrayList<DrawableDelegate> result = delegateLists.poll();
		if (result == null) {
			result = new ObjectArrayList<>();
		}
		return result;
	}

	public static void releaseDelegateList(ObjectArrayList<DrawableDelegate> list) {
		if (!list.isEmpty()) {
			list.clear();
		}

		delegateLists.offer(list);
	}
}
