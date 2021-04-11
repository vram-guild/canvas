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

package grondag.canvas.buffer.encoding;

import java.util.Comparator;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.terrain.render.UploadableChunk;

/**
 * MUST ALWAYS BE USED WITHIN SAME MATERIAL CONTEXT.
 */
public class VertexCollectorList {
	private final ObjectArrayList<ArrayVertexCollector> active = new ObjectArrayList<>();
	private final ArrayVertexCollector[] collectors = new ArrayVertexCollector[RenderState.MAX_COUNT];
	private final ObjectArrayList<ArrayVertexCollector> drawList = new ObjectArrayList<>();

	public final FrexVertexConsumerImpl consumer = new FrexVertexConsumerImpl(this);

	/**
	 * Clears all storage arrays.
	 */
	public void clear() {
		final int limit = active.size();

		for (int i = 0; i < limit; i++) {
			active.get(i).clear();
		}
	}

	public final ArrayVertexCollector getIfExists(RenderMaterialImpl materialState) {
		return materialState == RenderMaterialImpl.MISSING ? null : collectors[materialState.collectorIndex];
	}

	public final ArrayVertexCollector get(RenderMaterialImpl materialState) {
		if (materialState == RenderMaterialImpl.MISSING) {
			return null;
		}

		final int index = materialState.collectorIndex;
		final ArrayVertexCollector[] collectors = this.collectors;

		ArrayVertexCollector result = null;

		if (index < collectors.length) {
			result = collectors[index];
		}

		if (result == null) {
			result = new ArrayVertexCollector(materialState.renderState);
			collectors[index] = result;
			active.add(result);
		}

		return result;
	}

	public boolean contains(RenderMaterialImpl materialState) {
		final int index = materialState.collectorIndex;
		return index < collectors.length && collectors[index] != null;
	}

	public int size() {
		return active.size();
	}

	public ArrayVertexCollector get(int index) {
		return active.get(index);
	}

	public int totalBytes(boolean sorted) {
		final int limit = active.size();
		final ObjectArrayList<ArrayVertexCollector> active = this.active;
		int intSize = 0;

		for (int i = 0; i < limit; i++) {
			final ArrayVertexCollector collector = active.get(i);

			if (!collector.isEmpty() && collector.renderState.sorted == sorted) {
				intSize += collector.integerSize();
			}
		}

		return intSize * 4;
	}

	public UploadableChunk toUploadableChunk(boolean sorted) {
		final int bytes = totalBytes(sorted);
		return bytes == 0 ? UploadableChunk.EMPTY_UPLOADABLE : new UploadableChunk(this, sorted, bytes);
	}

	/**
	 * Gives populated collectors in the order they should be drawn.
	 * DO NOT RETAIN A REFERENCE
	 */
	public ObjectArrayList<ArrayVertexCollector> sortedDrawList(Predicate<RenderState> predicate) {
		final ObjectArrayList<ArrayVertexCollector> drawList = this.drawList;
		drawList.clear();

		final int limit = size();

		if (limit != 0) {
			for (int i = 0; i < limit; ++i) {
				final ArrayVertexCollector collector = get(i);

				if (!collector.isEmpty() && predicate.test(collector.renderState)) {
					drawList.add(collector);
				}
			}
		}

		if (!drawList.isEmpty()) {
			drawList.sort(DRAW_SORT);
		}

		return drawList;
	}

	private static final Comparator<ArrayVertexCollector> DRAW_SORT = (a, b) -> {
		// note reverse argument order - higher priority wins
		return Long.compare(b.renderState.drawPriority, a.renderState.drawPriority);
	};
}
