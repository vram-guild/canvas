/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.texture;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.MaterialImpl;

public abstract class MaterialIndexProvider {
	public abstract MaterialIndexer getIndexer(MaterialImpl mat);

	public abstract void enable();

	protected abstract void clear();

	private static class SimpleIndexProvider extends MaterialIndexProvider {
		int nextIndex = 0;
		final Long2IntOpenHashMap map = new Long2IntOpenHashMap(64, Hash.VERY_FAST_LOAD_FACTOR);
		private final MaterialIndexTexture tex = new MaterialIndexTexture(false);

		@Override
		public MaterialIndexer getIndexer(MaterialImpl mat) {
			final long key = mat.vertexShaderIndex | (mat.fragmentShaderIndex << 16) | (((long) mat.shaderFlags) << 32) | (((long) mat.condition.index) << 48);

			synchronized (this) {
				final int result = map.computeIfAbsent(key, k -> {
					final int i = nextIndex++;
					tex.set(i, mat.vertexShaderIndex, mat.fragmentShaderIndex, mat.shaderFlags, mat.condition.index);
					return i;
				});

				return i -> result;
			}
		}

		@Override
		public void enable() {
			tex.enable();
		}

		@Override
		protected void clear() {
			map.clear();
			tex.reset();
		}
	}

	private static class AtlasIndexProvider extends MaterialIndexProvider {
		@SuppressWarnings("unused")
		private final ResourceLocation atlasId;

		AtlasIndexProvider(ResourceLocation atlasId) {
			this.atlasId = atlasId;
		}

		private int nextIndex = 0;
		private final Long2ObjectOpenHashMap<Indexer> materialMap = new Long2ObjectOpenHashMap<>(64, Hash.VERY_FAST_LOAD_FACTOR);
		private final MaterialIndexTexture tex = new MaterialIndexTexture(true);

		// PERF: find ways to reduce/avoid locking here and in MaterialIndexImage
		private final Object sync = new Object();

		private class Indexer implements MaterialIndexer {
			private Indexer(MaterialImpl mat) {
				this.mat = mat;
			}

			private final MaterialImpl mat;
			private final Int2IntOpenHashMap spriteMap = new Int2IntOpenHashMap(64, Hash.VERY_FAST_LOAD_FACTOR);

			@Override
			public int index(int spriteId) {
				synchronized (sync) {
					return spriteMap.computeIfAbsent(spriteId, k -> {
						final int i = nextIndex++;
						final TextureAtlasSprite sprite = mat.texture.spriteIndex().fromIndex(k);
						tex.set(i, mat.vertexShaderIndex, mat.fragmentShaderIndex, mat.shaderFlags, mat.condition.index, sprite);
						return i;
					});
				}
			}
		}

		@Override
		public MaterialIndexer getIndexer(MaterialImpl mat) {
			final long key = mat.vertexShaderIndex | (mat.fragmentShaderIndex << 16) | (((long) mat.shaderFlags) << 32) | (((long) mat.condition.index) << 48);

			synchronized (sync) {
				return materialMap.computeIfAbsent(key, k -> {
					return new Indexer(mat);
				});
			}
		}

		@Override
		public void enable() {
			tex.enable();
		}

		@Override
		protected void clear() {
			materialMap.clear();
			tex.reset();
			nextIndex = 0;
		}
	}

	private static final Object2ObjectOpenHashMap<ResourceLocation, AtlasIndexProvider> ATLAS_PROVIDERS = new Object2ObjectOpenHashMap<>(64, Hash.VERY_FAST_LOAD_FACTOR);

	public static final synchronized MaterialIndexProvider getOrCreateForAtlas(ResourceLocation id) {
		return ATLAS_PROVIDERS.computeIfAbsent(id, AtlasIndexProvider::new);
	}

	public static void reload() {
		if (Configurator.traceTextureLoad) {
			CanvasMod.LOG.info("MaterialIndexProvider reloading");
		}

		for (final var p : ATLAS_PROVIDERS.values()) {
			p.clear();
		}
	}

	public static final MaterialIndexProvider GENERIC = new SimpleIndexProvider();
}
