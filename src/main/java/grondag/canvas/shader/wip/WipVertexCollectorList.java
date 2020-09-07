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

package grondag.canvas.shader.wip;

import grondag.canvas.material.MaterialState;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;

/**
 * MUST ALWAYS BE USED WITHIN SAME MATERIAL CONTEXT
 */
public class WipVertexCollectorList {
	private final ObjectArrayList<WipVertexCollectorImpl> pool = new ObjectArrayList<>();
	private int size = 0;
	private WipVertexCollectorImpl[] collectors = new WipVertexCollectorImpl[WipRenderState.MAX_INDEX];

	/**
	 * Releases any held vertex collectors and resets state
	 */
	public void clear() {

		for (int i = 0; i < size; i++) {
			pool.get(i).clear();
		}

		Arrays.fill(collectors, 0, collectors.length, null);
	}

	public final WipVertexCollectorImpl getIfExists(WipRenderState materialState) {
		return collectors[materialState.collectorIndex];
	}

	public final WipVertexCollectorImpl get(WipRenderState materialState) {
		final int index = materialState.collectorIndex;
		WipVertexCollectorImpl[] collectors = this.collectors;

		WipVertexCollectorImpl result;

		if (index < collectors.length) {
			result = collectors[index];
		} else {
			result = null;
			final WipVertexCollectorImpl[] newCollectors = new WipVertexCollectorImpl[MathHelper.smallestEncompassingPowerOfTwo(index)];
			System.arraycopy(collectors, 0, newCollectors, 0, collectors.length);
			collectors = newCollectors;
			this.collectors = collectors;
		}

		if (result == null) {
			assert materialState.collectorIndex != MaterialState.TRANSLUCENT_INDEX;
			result = emptyCollector().prepare(materialState);
			collectors[index] = result;
		}

		return result;
	}

	private WipVertexCollectorImpl emptyCollector() {
		WipVertexCollectorImpl result;

		if (size == pool.size()) {
			result = new WipVertexCollectorImpl();
			pool.add(result);
		} else {
			result = pool.get(size);
		}

		++size;
		return result;
	}

	public boolean contains(MaterialState materialState) {
		final int index = materialState.collectorIndex;
		return index < collectors.length && collectors[index] != null;
	}

	public int totalBytes() {
		final int size = this.size;
		final ObjectArrayList<WipVertexCollectorImpl> pool = this.pool;

		int intSize = 0;

		for (int i = 0; i < size; i++) {
			intSize += pool.get(i).integerSize();
		}

		return intSize * 4;
	}

	//	public UploadableChunk toUploadableChunk(EncodingContext context, boolean isTranslucent) {
	//		final int bytes = totalBytes(isTranslucent);
	//		return bytes == 0 ? UploadableChunk.EMPTY_UPLOADABLE : new UploadableChunk(this, MaterialVertexFormats.get(context, isTranslucent), isTranslucent, bytes);
	//	}

	public int size() {
		return size;
	}

	public WipVertexCollectorImpl get(int index) {
		return pool.get(index);
	}
}
