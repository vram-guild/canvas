/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.shader;

import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.minecraft.util.Identifier;

public class MaterialShaderId {
	public final int index;
	public final Identifier vertexId;
	public final int vertexIndex;
	public final Identifier fragmentId;
	public final int fragmentIndex;

	private MaterialShaderId(int index, int vertexIndex, int fragmentIndex) {
		this.index = index;
		this.vertexIndex = vertexIndex;
		this.fragmentIndex = fragmentIndex;
		vertexId = MaterialShaderManager.VERTEX_INDEXER.fromHandle(vertexIndex);
		fragmentId = MaterialShaderManager.FRAGMENT_INDEXER.fromHandle(fragmentIndex);
	}

	private static final Int2ObjectOpenHashMap<MaterialShaderId> MAP = new Int2ObjectOpenHashMap<>();
	private static final SimpleUnorderedArrayList<MaterialShaderId> LIST = new SimpleUnorderedArrayList<>();

	public static synchronized MaterialShaderId find(Identifier vertexShaderId, Identifier fragmentShaderId) {
		return find(MaterialShaderManager.VERTEX_INDEXER.toHandle(vertexShaderId), MaterialShaderManager.FRAGMENT_INDEXER.toHandle(fragmentShaderId));
	}

	public static synchronized MaterialShaderId find(int vertexShaderIndex, int fragmentShaderIndex) {
		final int key = (fragmentShaderIndex << 16) | vertexShaderIndex;
		MaterialShaderId result = MAP.get(key);

		if (result == null) {
			result = new MaterialShaderId(LIST.size(), vertexShaderIndex, fragmentShaderIndex);
			LIST.add(result);
			MAP.put(key, result);
		}

		return result;
	}

	public static MaterialShaderId get(int index) {
		return LIST.get(index);
	}
}
