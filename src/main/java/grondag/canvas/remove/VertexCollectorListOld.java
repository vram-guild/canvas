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

package grondag.canvas.remove;

import java.util.Arrays;

import grondag.canvas.material.EncodingContext;
import grondag.canvas.terrain.render.UploadableChunk;
import grondag.canvas.wip.state.WipRenderMaterial;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.util.math.MathHelper;

/**
 * MUST ALWAYS BE USED WITHIN SAME MATERIAL CONTEXT
 */
public class VertexCollectorListOld {
	private final ObjectArrayList<VertexCollectorImplOld> solidCollectors = new ObjectArrayList<>();
	private VertexCollectorImplOld[] collectors = new VertexCollectorImplOld[4096];
	private int solidCount = 0;
	private EncodingContext context = null;

	public VertexCollectorListOld() {
		collectors[WAS_TRANSLUCENT_INDEX_NOW_DUMMY_INDEX_NOT_USED] = new VertexCollectorImplOld();
	}

	/**
	 * Used for assertions and to set material state for translucent collector .
	 */
	public void setContext(EncodingContext context) {
		this.context = context;
		collectors[WAS_TRANSLUCENT_INDEX_NOW_DUMMY_INDEX_NOT_USED].prepare(context, WipRenderMaterial.TRANSLUCENT_TERRAIN);
	}

	/**
	 * Releases any held vertex collectors and resets state
	 */
	public void clear() {
		collectors[WAS_TRANSLUCENT_INDEX_NOW_DUMMY_INDEX_NOT_USED].clear();

		for (int i = 0; i < solidCount; i++) {
			solidCollectors.get(i).clear();
		}

		solidCount = 0;

		Arrays.fill(collectors, 1, collectors.length, null);
	}

	public final VertexCollectorImplOld getIfExists(WipRenderMaterial materialState) {
		return collectors[materialState.collectorIndex];
	}

	public final VertexCollectorImplOld get(WipRenderMaterial materialState) {
		final int index = materialState.collectorIndex;
		VertexCollectorImplOld[] collectors = this.collectors;

		VertexCollectorImplOld result;

		if (index < collectors.length) {
			result = collectors[index];
		} else {
			result = null;
			final VertexCollectorImplOld[] newCollectors = new VertexCollectorImplOld[MathHelper.smallestEncompassingPowerOfTwo(index)];
			System.arraycopy(collectors, 0, newCollectors, 0, collectors.length);
			collectors = newCollectors;
			this.collectors = collectors;
		}

		if (result == null) {
			assert materialState.collectorIndex != WAS_TRANSLUCENT_INDEX_NOW_DUMMY_INDEX_NOT_USED;
			result = emptySolidCollector().prepare(context, materialState);
			collectors[index] = result;
		}

		return result;
	}

	private VertexCollectorImplOld emptySolidCollector() {
		VertexCollectorImplOld result;

		if (solidCount == solidCollectors.size()) {
			result = new VertexCollectorImplOld();
			solidCollectors.add(result);
		} else {
			result = solidCollectors.get(solidCount);
		}

		++solidCount;
		return result;
	}

	public boolean contains(WipRenderMaterial materialState) {
		final int index = materialState.collectorIndex;
		return index < collectors.length && collectors[index] != null;
	}

	private int totalBytes(boolean translucent) {
		if (translucent) {
			return collectors[WAS_TRANSLUCENT_INDEX_NOW_DUMMY_INDEX_NOT_USED].integerSize() * 4;
		} else {
			final int solidCount = this.solidCount;
			final ObjectArrayList<VertexCollectorImplOld> solidCollectors = this.solidCollectors;

			int intSize = 0;

			for (int i = 0; i < solidCount; i++) {
				intSize += solidCollectors.get(i).integerSize();
			}

			return intSize * 4;
		}
	}

	public UploadableChunk toUploadableChunk(EncodingContext context, boolean isTranslucent) {
		final int bytes = totalBytes(isTranslucent);
		return bytes == 0 ? UploadableChunk.EMPTY_UPLOADABLE : null; //new UploadableChunk(this, MaterialVertexFormats.POSITION_COLOR_TEXTURE_MATERIAL_LIGHT_NORMAL, isTranslucent, bytes);
	}

	public VertexCollectorImplOld getTranslucent() {
		return collectors[WAS_TRANSLUCENT_INDEX_NOW_DUMMY_INDEX_NOT_USED];
	}

	public int solidCount() {
		return solidCount;
	}

	public VertexCollectorImplOld getSolid(int index) {
		return solidCollectors.get(index);
	}

	public static final int WAS_TRANSLUCENT_INDEX_NOW_DUMMY_INDEX_NOT_USED = 0;
}
