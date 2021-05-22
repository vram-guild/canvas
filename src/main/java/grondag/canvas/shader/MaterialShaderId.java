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

package grondag.canvas.shader;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.util.Identifier;

import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;

public class MaterialShaderId {
	public final int index;
	public final Identifier vertexId;
	public final int vertexIndex;
	public final Identifier fragmentId;
	public final int fragmentIndex;
	public final Identifier depthVertexId;
	public final int depthVertexIndex;
	public final Identifier depthFragmentId;
	public final int depthFragmentIndex;

	private MaterialShaderId(int index, int vertexIndex, int fragmentIndex, int depthVertexIndex, int depthFragmentIndex) {
		this.index = index;
		this.vertexIndex = vertexIndex;
		this.fragmentIndex = fragmentIndex;
		this.depthVertexIndex = depthVertexIndex;
		this.depthFragmentIndex = depthFragmentIndex;
		vertexId = MaterialShaderManager.VERTEX_INDEXER.fromHandle(vertexIndex);
		fragmentId = MaterialShaderManager.FRAGMENT_INDEXER.fromHandle(fragmentIndex);
		depthVertexId = MaterialShaderManager.VERTEX_INDEXER.fromHandle(depthVertexIndex);
		depthFragmentId = MaterialShaderManager.FRAGMENT_INDEXER.fromHandle(depthFragmentIndex);
	}

	private static final Long2ObjectOpenHashMap<MaterialShaderId> MAP = new Long2ObjectOpenHashMap<>();
	private static final SimpleUnorderedArrayList<MaterialShaderId> LIST = new SimpleUnorderedArrayList<>();

	public static synchronized MaterialShaderId find(Identifier vertexShaderId, Identifier fragmentShaderId, Identifier depthVertexShaderId, Identifier depthFragmentShaderId) {
		return find(
			MaterialShaderManager.VERTEX_INDEXER.toHandle(vertexShaderId),
			MaterialShaderManager.FRAGMENT_INDEXER.toHandle(fragmentShaderId),
			MaterialShaderManager.VERTEX_INDEXER.toHandle(depthVertexShaderId),
			MaterialShaderManager.FRAGMENT_INDEXER.toHandle(depthFragmentShaderId)
		);
	}

	public static synchronized MaterialShaderId find(int vertexShaderIndex, int fragmentShaderIndex, int depthVertexIndex, int depthFragmentIndex) {
		final long key = vertexShaderIndex | (fragmentShaderIndex << 16) | (((long) depthVertexIndex) << 32) | (((long) depthFragmentIndex) << 48);
		MaterialShaderId result = MAP.get(key);

		if (result == null) {
			result = new MaterialShaderId(LIST.size(), vertexShaderIndex, fragmentShaderIndex, depthVertexIndex, depthFragmentIndex);
			LIST.add(result);
			MAP.put(key, result);
		}

		return result;
	}

	public static MaterialShaderId get(int index) {
		return LIST.get(index);
	}
}
