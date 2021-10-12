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

package grondag.canvas.shader;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

import io.vram.frex.api.material.MaterialShader;
import io.vram.sc.unordered.SimpleUnorderedArrayList;

import grondag.canvas.shader.data.ShaderStrings;

public class MaterialShaderId implements MaterialShader {
	public final int index;
	public final ResourceLocation vertexId;
	public final String vertexIdString;
	public final int vertexIndex;
	public final ResourceLocation fragmentId;
	public final String fragmentIdString;
	public final int fragmentIndex;
	public final ResourceLocation depthVertexId;
	public final int depthVertexIndex;
	public final ResourceLocation depthFragmentId;
	public final int depthFragmentIndex;

	private MaterialShaderId(int index, int vertexIndex, int fragmentIndex, int depthVertexIndex, int depthFragmentIndex) {
		this.index = index;
		this.vertexIndex = vertexIndex;
		this.fragmentIndex = fragmentIndex;
		this.depthVertexIndex = depthVertexIndex;
		this.depthFragmentIndex = depthFragmentIndex;
		vertexId = MaterialShaderManager.VERTEX_INDEXER.fromHandle(vertexIndex);
		vertexIdString = vertexId.toString();
		fragmentId = MaterialShaderManager.FRAGMENT_INDEXER.fromHandle(fragmentIndex);
		fragmentIdString = fragmentId.toString();
		depthVertexId = MaterialShaderManager.VERTEX_INDEXER.fromHandle(depthVertexIndex);
		depthFragmentId = MaterialShaderManager.FRAGMENT_INDEXER.fromHandle(depthFragmentIndex);
	}

	@Override
	public int index() {
		return index;
	}

	@Override
	public ResourceLocation vertexShaderId() {
		return vertexId;
	}

	@Override
	public String vertexShader() {
		return vertexIdString;
	}

	@Override
	public ResourceLocation fragmentShaderId() {
		return fragmentId;
	}

	@Override
	public String fragmentShader() {
		return fragmentIdString;
	}

	private static final Long2ObjectOpenHashMap<MaterialShaderId> MAP = new Long2ObjectOpenHashMap<>();
	private static final SimpleUnorderedArrayList<MaterialShaderId> LIST = new SimpleUnorderedArrayList<>();

	public static synchronized MaterialShaderId find(ResourceLocation vertexShaderId, ResourceLocation fragmentShaderId, ResourceLocation depthVertexShaderId, ResourceLocation depthFragmentShaderId) {
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

	public static final io.vram.frex.api.renderer.MaterialShaderManager MANAGER = new io.vram.frex.api.renderer.MaterialShaderManager() {
		@Override
		public MaterialShader shaderFromIndex(int index) {
			return get(index);
		}

		@Override
		public MaterialShader getOrCreate(@Nullable ResourceLocation vertexSourceId, @Nullable ResourceLocation fragmentSourceId) {
			return getOrCreate(vertexSourceId, fragmentSourceId, ShaderStrings.DEFAULT_VERTEX_SOURCE, ShaderStrings.DEFAULT_FRAGMENT_SOURCE);
		}

		@Override
		public MaterialShader getOrCreate(@Nullable ResourceLocation vertexSourceId, @Nullable ResourceLocation fragmentSourceId, @Nullable ResourceLocation depthVertexSourceId, @Nullable ResourceLocation depthFragmentSourceId) {
			if (vertexSourceId == null) {
				vertexSourceId = ShaderStrings.DEFAULT_VERTEX_SOURCE;
			}

			if (fragmentSourceId == null) {
				fragmentSourceId = ShaderStrings.DEFAULT_FRAGMENT_SOURCE;
			}

			if (depthVertexSourceId == null) {
				depthVertexSourceId = ShaderStrings.DEFAULT_VERTEX_SOURCE;
			}

			if (depthFragmentSourceId == null) {
				depthFragmentSourceId = ShaderStrings.DEFAULT_FRAGMENT_SOURCE;
			}

			return find(vertexSourceId, fragmentSourceId, depthVertexSourceId, depthFragmentSourceId);
		}
	};
}
