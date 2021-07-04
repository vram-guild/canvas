package grondag.canvas.texture;

import java.util.function.Supplier;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;

public final class ResourceCache<T> {
	public static final ObjectArrayList<ResourceCache> CACHED = new ObjectArrayList<>(64);
	public static final SimpleSynchronousResourceReloadListener cacheReloader = new SimpleSynchronousResourceReloadListener() {
		private final Identifier ID = new Identifier("canvas:resource_cache_reloader");

		@Override
		public Identifier getFabricId() {
			return ID;
		}

		@Override
		public void reload(ResourceManager resourceManager) {
			CACHED.forEach(ResourceCache::invalidate);
		}
	};

	public ResourceCache(Supplier<T> loader) {
		CACHED.add(this);
		this.loader = loader;
	}

	private final Supplier<T> loader;
	private T value;

	private void invalidate() {
		value = null;
	}

	public T getOrLoad() {
		if (value == null) {
			value = loader.get();
		}

		return value;
	}
}
