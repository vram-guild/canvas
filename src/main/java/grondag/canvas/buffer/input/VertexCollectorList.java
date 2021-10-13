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

package grondag.canvas.buffer.input;

import java.util.Comparator;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import io.vram.frex.api.buffer.FrexVertexConsumer;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.base.renderer.mesh.MeshEncodingHelper;

import grondag.canvas.apiimpl.mesh.QuadEditorImpl;
import grondag.canvas.apiimpl.rendercontext.AbsentEncodingContext;
import grondag.canvas.buffer.format.QuadEncoders;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.material.state.wip.CanvasRenderMaterial;
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
	public class Consumer extends QuadEditorImpl {
		{
			data = new int[MeshEncodingHelper.TOTAL_MESH_QUAD_STRIDE];
			material(RenderMaterial.defaultMaterial());
		}

		@Override
		public Consumer emit() {
			final CanvasRenderMaterial mat = material();

			if (mat.condition().compute()) {
				complete();
				QuadEncoders.STANDARD_ENCODER.encode(this, AbsentEncodingContext.INSTANCE, get(mat));

				if (Configurator.disableUnseenSpriteAnimation) {
					mat.trackPerFrameAnimation(this.spriteId());
				}
			}

			clear();
			return this;
		}

		public FrexVertexConsumer prepare(CanvasRenderMaterial mat) {
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

	public final ArrayVertexCollector getIfExists(CanvasRenderMaterial materialState) {
		return materialState.isMissing() ? null : collectors[materialState.collectorIndex()];
	}

	public final ArrayVertexCollector get(CanvasRenderMaterial materialState) {
		if (materialState.isMissing()) {
			return null;
		}

		final int index = materialState.collectorIndex();
		final ArrayVertexCollector[] collectors = this.collectors;

		ArrayVertexCollector result = null;

		if (index < collectors.length) {
			result = collectors[index];
		}

		if (result == null) {
			result = new ArrayVertexCollector(materialState.renderState(), isTerrain);
			collectors[index] = result;
			active.add(result);
		}

		return result;
	}

	public boolean contains(CanvasRenderMaterial materialState) {
		final int index = materialState.collectorIndex();
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
