/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
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
import grondag.canvas.material.state.RenderMaterialImpl;

public abstract class MaterialIndexProvider {
	public abstract MaterialIndexer getIndexer(RenderMaterialImpl mat);

	public abstract void enable();

	protected abstract void clear();

	private static class SimpleIndexProvider extends MaterialIndexProvider {
		int nextIndex = 0;
		final Long2IntOpenHashMap map = new Long2IntOpenHashMap(64, Hash.VERY_FAST_LOAD_FACTOR);
		private final MaterialIndexTexture tex = new MaterialIndexTexture(false);

		@Override
		public MaterialIndexer getIndexer(RenderMaterialImpl mat) {
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
			private Indexer(RenderMaterialImpl mat) {
				this.mat = mat;
			}

			private final RenderMaterialImpl mat;
			private final Int2IntOpenHashMap spriteMap = new Int2IntOpenHashMap(64, Hash.VERY_FAST_LOAD_FACTOR);

			@Override
			public int index(int spriteId) {
				synchronized (sync) {
					return spriteMap.computeIfAbsent(spriteId, k -> {
						final int i = nextIndex++;
						final TextureAtlasSprite sprite = mat.texture.atlasInfo().fromId(k);
						tex.set(i, mat.vertexShaderIndex, mat.fragmentShaderIndex, mat.shaderFlags, mat.condition.index, sprite);
						return i;
					});
				}
			}
		}

		@Override
		public MaterialIndexer getIndexer(RenderMaterialImpl mat) {
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
