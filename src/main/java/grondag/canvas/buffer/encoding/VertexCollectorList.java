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

import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.terrain.render.UploadableChunk;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * MUST ALWAYS BE USED WITHIN SAME MATERIAL CONTEXT
 */
public class VertexCollectorList {
	private final ObjectArrayList<VertexCollectorImpl> pool = new ObjectArrayList<>();
	private final VertexCollectorImpl[] collectors = new VertexCollectorImpl[RenderState.MAX_COUNT];

	/**
	 * Clears all vertex collectors
	 */
	public void clear() {
		final int limit = pool.size();

		for (int i = 0; i < limit; i++) {
			pool.get(i).clear();
		}
	}

	public final VertexCollectorImpl getIfExists(RenderMaterialImpl materialState) {
		return materialState == RenderMaterialImpl.MISSING ? null : collectors[materialState.collectorIndex];
	}

	public final VertexCollectorImpl get(RenderMaterialImpl materialState) {
		if (materialState == RenderMaterialImpl.MISSING) {
			return null;
		}

		final int index = materialState.collectorIndex;
		final VertexCollectorImpl[] collectors = this.collectors;

		VertexCollectorImpl result = null;

		if (index < collectors.length) {
			result = collectors[index];
		}

		if (result == null) {
			result = new VertexCollectorImpl().prepare(materialState);
			collectors[index] = result;
			pool.add(result);
		}

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
		return pool.size();
	}

	public VertexCollectorImpl get(int index) {
		return pool.get(index);
	}

	public int totalBytes(boolean sorted) {
		final int limit =  pool.size();
		final ObjectArrayList<VertexCollectorImpl> pool = this.pool;
		int intSize = 0;

		for (int i = 0; i < limit; i++) {
			final VertexCollectorImpl collector = pool.get(i);

			if (!collector.isEmpty() && collector.materialState.sorted == sorted) {
				intSize += collector.integerSize();
			}
		}

		return intSize * 4;
	}

	public UploadableChunk toUploadableChunk(boolean sorted) {
		final int bytes = totalBytes(sorted);
		return bytes == 0 ? UploadableChunk.EMPTY_UPLOADABLE : new UploadableChunk(this, sorted, bytes);
	}
}
