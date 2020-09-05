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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.util.math.MathHelper;

import grondag.canvas.apiimpl.material.MeshMaterialLayer;
import grondag.canvas.material.EncodingContext;
import grondag.canvas.material.MaterialState;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.shader.ShaderPass;
import grondag.canvas.terrain.render.UploadableChunk;

/**
 * MUST ALWAYS BE USED WITHIN SAME MATERIAL CONTEXT
 */
public class VertexCollectorList {
	private VertexCollectorImpl[] collectors = new VertexCollectorImpl[4096];

	private int solidCount = 0;

	private final ObjectArrayList<VertexCollectorImpl> solidCollectors = new ObjectArrayList<>();

	private EncodingContext context = null;

	public VertexCollectorList() {
		collectors[MaterialState.TRANSLUCENT_INDEX] = new VertexCollectorImpl();
	}

	/**
	 * Used for assertions and to set material state for translucent collector .
	 */
	public void setContext(EncodingContext context) {
		this.context = context;
		collectors[MaterialState.TRANSLUCENT_INDEX].prepare(context, MaterialState.getDefault(ShaderPass.TRANSLUCENT));
	}

	/**
	 * Releases any held vertex collectors and resets state
	 */
	public void clear() {
		collectors[MaterialState.TRANSLUCENT_INDEX].clear();

		for (int i = 0; i < solidCount; i++) {
			solidCollectors.get(i).clear();
		}

		solidCount = 0;

		Arrays.fill(collectors, 1, collectors.length, null);
	}

	public final VertexCollectorImpl getIfExists(MaterialState materialState) {
		return collectors[materialState.collectorIndex];
	}

	public final VertexCollectorImpl get(MaterialState materialState) {
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

		if(result == null) {
			assert materialState.collectorIndex != MaterialState.TRANSLUCENT_INDEX;
			result = emptySolidCollector().prepare(context, materialState);
			collectors[index] = result;
		}

		return result;
	}

	private VertexCollectorImpl emptySolidCollector() {
		VertexCollectorImpl result;

		if(solidCount == solidCollectors.size()) {
			result = new VertexCollectorImpl();
			solidCollectors.add(result);
		} else {
			result = solidCollectors.get(solidCount);
		}

		++solidCount;
		return result;
	}

	public boolean contains(MaterialState materialState) {
		final int index = materialState.collectorIndex;
		return index < collectors.length && collectors[index] != null;
	}

	private int totalBytes(boolean translucent) {
		if (translucent) {
			return collectors[MaterialState.TRANSLUCENT_INDEX].integerSize() * 4;
		} else {
			final int solidCount = this.solidCount;
			final ObjectArrayList<VertexCollectorImpl> solidCollectors = this.solidCollectors;

			int intSize = 0;

			for(int i = 0; i < solidCount; i++) {
				intSize += solidCollectors.get(i).integerSize();
			}

			return intSize * 4;
		}
	}

	public VertexCollectorImpl get(MeshMaterialLayer mat) {
		return get(MaterialState.get(mat));
	}

	public UploadableChunk toUploadableChunk(EncodingContext context, boolean isTranslucent) {
		final int bytes = totalBytes(isTranslucent);
		return bytes == 0 ? UploadableChunk.EMPTY_UPLOADABLE : new UploadableChunk(this, MaterialVertexFormats.get(context, isTranslucent), isTranslucent, bytes);
	}

	public VertexCollectorImpl getTranslucent() {
		return collectors[MaterialState.TRANSLUCENT_INDEX];
	}

	public int solidCount() {
		return solidCount;
	}

	public VertexCollectorImpl getSolid(int index) {
		return solidCollectors.get(index);
	}
}
