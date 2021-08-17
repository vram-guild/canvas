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

package grondag.canvas.buffer.input;

import java.util.Comparator;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.render.VertexConsumer;

import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.mesh.MeshEncodingHelper;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.buffer.format.QuadEncoders;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.terrain.base.UploadableRegion;
import grondag.canvas.render.terrain.cluster.ClusteredDrawableRegion;
import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.terrain.region.RegionPosition;

/**
 * MUST ALWAYS BE USED WITHIN SAME MATERIAL CONTEXT.
 */
public class VertexCollectorList {
	private final ObjectArrayList<ArrayVertexCollector> active = new ObjectArrayList<>();
	private final ArrayVertexCollector[] collectors = new ArrayVertexCollector[RenderState.MAX_COUNT];
	private final ObjectArrayList<ArrayVertexCollector> drawList = new ObjectArrayList<>();
	public final boolean isTerrain;

	public VertexCollectorList(boolean isTerrain) {
		this.isTerrain = isTerrain;
	}

	/**
	 * Where we handle all pre-buffer coloring, lighting, transformation, etc.
	 * Reused for all mesh quads. Fixed baking array sized to hold largest possible mesh quad.
	 */
	public class Consumer extends MutableQuadViewImpl {
		{
			data = new int[MeshEncodingHelper.TOTAL_MESH_QUAD_STRIDE];
			material(Canvas.MATERIAL_STANDARD);
		}

		@Override
		public Consumer emit() {
			final RenderMaterialImpl mat = material();

			if (mat.condition.compute()) {
				complete();
				QuadEncoders.STANDARD_ENCODER.encode(this, get(mat));
			}

			clear();
			return this;
		}

		public VertexConsumer prepare(RenderMaterialImpl mat) {
			defaultMaterial(mat);
			clear();
			return this;
		}
	}

	public final Consumer consumer = new Consumer();

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
			result = new ArrayVertexCollector(materialState.renderState, isTerrain);
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

	public UploadableRegion toUploadableChunk(boolean sorted, RegionPosition origin, WorldRenderState worldRenderState) {
		final int bytes = totalBytes(sorted);
		return bytes == 0 ? UploadableRegion.EMPTY_UPLOADABLE : ClusteredDrawableRegion.uploadable(this, sorted ? worldRenderState.translucentClusterRealm : worldRenderState.solidClusterRealm, bytes, origin);
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

		if (drawList.size() > 1) {
			drawList.sort(DRAW_SORT);
		}

		return drawList;
	}

	private static final Comparator<ArrayVertexCollector> DRAW_SORT = (a, b) -> {
		// note reverse argument order - higher priority wins
		return Long.compare(b.renderState.drawPriority, a.renderState.drawPriority);
	};
}
