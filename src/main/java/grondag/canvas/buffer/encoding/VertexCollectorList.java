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

package grondag.canvas.buffer.encoding;

import java.util.Arrays;

import grondag.canvas.material.state.RenderContextState;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.terrain.render.UploadableChunk;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.util.math.MathHelper;

/**
 * MUST ALWAYS BE USED WITHIN SAME MATERIAL CONTEXT
 */
public class VertexCollectorList {
	private final ObjectArrayList<VertexCollectorImpl> pool = new ObjectArrayList<>();
	private int size = 0;
	private VertexCollectorImpl[] collectors = new VertexCollectorImpl[RenderState.MAX_COUNT];
	private final RenderContextState contextState;

	public VertexCollectorList(RenderContextState contextState) {
		this.contextState = contextState;
	}

	/**
	 * Releases any held vertex collectors and resets state
	 */
	public void clear() {
		for (int i = 0; i < size; i++) {
			pool.get(i).clear();
		}

		Arrays.fill(collectors, 0, collectors.length, null);

		size = 0;
	}

	public final VertexCollectorImpl getIfExists(RenderMaterialImpl materialState) {
		return materialState == RenderMaterialImpl.MISSING ? null : collectors[materialState.collectorIndex];
	}

	public final VertexCollectorImpl get(RenderMaterialImpl materialState) {
		if (materialState == RenderMaterialImpl.MISSING) {
			return null;
		}

		final int index = materialState.collectorIndex;
		VertexCollectorImpl[] collectors = this.collectors;

		VertexCollectorImpl result;

		if (index < collectors.length) {
			result = collectors[index];
		} else {
			result = null;
			final VertexCollectorImpl[] newCollectors = new VertexCollectorImpl[MathHelper.smallestEncompassingPowerOfTwo(index)];
			System.arraycopy(collectors, 0, newCollectors, 0, collectors.length);
			collectors = newCollectors;
			this.collectors = collectors;
		}

		if (result == null) {
			result = emptyCollector().prepare(materialState);
			collectors[index] = result;
		}

		return result;
	}

	private VertexCollectorImpl emptyCollector() {
		VertexCollectorImpl result;

		if (size == pool.size()) {
			result = new VertexCollectorImpl(contextState);
			pool.add(result);
		} else {
			result = pool.get(size);
		}

		++size;
		return result;
	}

	public boolean contains(RenderMaterialImpl materialState) {
		final int index = materialState.collectorIndex;
		return index < collectors.length && collectors[index] != null;
	}

	//	public int totalBytes() {
	//		final int size = this.size;
	//		final ObjectArrayList<WipVertexCollectorImpl> pool = this.pool;
	//
	//		int intSize = 0;
	//
	//		for (int i = 0; i < size; i++) {
	//			intSize += pool.get(i).integerSize();
	//		}
	//
	//		return intSize * 4;
	//	}

	//	public UploadableChunk toUploadableChunk(EncodingContext context, boolean isTranslucent) {
	//		final int bytes = totalBytes(isTranslucent);
	//		return bytes == 0 ? UploadableChunk.EMPTY_UPLOADABLE : new UploadableChunk(this, MaterialVertexFormats.get(context, isTranslucent), isTranslucent, bytes);
	//	}

	public int size() {
		return size;
	}

	public VertexCollectorImpl get(int index) {
		return pool.get(index);
	}

	public int totalBytes(boolean sorted) {
		final int limit = size;
		final ObjectArrayList<VertexCollectorImpl> pool = this.pool;
		int intSize = 0;

		for (int i = 0; i < limit; i++) {
			intSize += pool.get(i).integerSize();
		}

		return intSize * 4;
	}

	public UploadableChunk toUploadableChunk(boolean sorted) {
		final int bytes = totalBytes(sorted);
		return bytes == 0 ? UploadableChunk.EMPTY_UPLOADABLE : new UploadableChunk(this, sorted, bytes);
	}
}
