package grondag.canvas.texture;

import java.util.function.Supplier;

public final class ResourceCache<T> {
	public ResourceCache(Supplier<T> loader) {
		ResourceCacheManager.CACHED.add(this);
		this.loader = loader;
	}

	private final Supplier<T> loader;
	private T value;

	public void invalidate() {
		value = null;
	}

	public T getOrLoad() {
		if (value == null) {
			value = loader.get();
		}

		return value;
	}
}
