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
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.util.Identifier;

import grondag.canvas.material.state.RenderMaterialImpl;

public class MaterialIndexProvider {
	MaterialIndexProvider() { }

	private int nextIndex = 0;
	private final Long2IntOpenHashMap map = new Long2IntOpenHashMap();
	public final MaterialIndexTexture tex = new MaterialIndexTexture();

	public MaterialIndexer getIndexer(RenderMaterialImpl mat) {
		final long key = mat.vertexShaderIndex | (mat.fragmentShaderIndex << 16) | (((long) mat.shaderFlags) << 32) | (((long) mat.condition.index) << 48);

		final int result = map.computeIfAbsent(key, k -> {
			final int i = nextIndex++;
			tex.set(i, mat.vertexShaderIndex, mat.fragmentShaderIndex, mat.shaderFlags, mat.condition.index);
			return i;
		});

		return i -> result;
	}

	public static class AtlasMaterialIndexProvider extends MaterialIndexProvider {
		//WIP2: implement

		AtlasMaterialIndexProvider(Identifier id) {
			super();
		}
	}

	private static final Object2ObjectOpenHashMap<Identifier, AtlasMaterialIndexProvider> MAP = new Object2ObjectOpenHashMap<>(64, Hash.VERY_FAST_LOAD_FACTOR);

	public static final AtlasMaterialIndexProvider getOrCreateForAtlas(Identifier id) {
		return MAP.computeIfAbsent(id, AtlasMaterialIndexProvider::new);
	}

	public static final MaterialIndexProvider GENERIC = new MaterialIndexProvider();
}
