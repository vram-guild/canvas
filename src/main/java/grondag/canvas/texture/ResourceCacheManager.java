package grondag.canvas.texture;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;

public class ResourceCacheManager {
	public static final ObjectArrayList<ResourceCache> CACHED = new ObjectArrayList<>(64);
	public static final SimpleSynchronousResourceReloadListener cacheReloader = new SimpleSynchronousResourceReloadListener() {
		private final Identifier ID = new Identifier("canvas:resource_cache_reloader");

		@Override
		public Identifier getFabricId() {
			return ID;
		}

		@Override
		public void reload(ResourceManager resourceManager) {
			MaterialIndexProvider.reload();
			CACHED.forEach(ResourceCache::invalidate);
		}
	};
}
