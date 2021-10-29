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

import io.vram.frex.api.buffer.VertexEmitter;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.base.renderer.context.input.AbsentInputContext;
import io.vram.frex.base.renderer.mesh.MeshEncodingHelper;
import io.vram.frex.base.renderer.mesh.RootQuadEmitter;

import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.buffer.format.StandardEncoder;
import grondag.canvas.buffer.format.TerrainEncoder;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.CanvasRenderMaterial;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.render.terrain.base.UploadableRegion;
import grondag.canvas.render.terrain.cluster.ClusteredDrawableRegion;
import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.terrain.region.RegionPosition;

/**
 * MUST ALWAYS BE USED WITHIN SAME MATERIAL CONTEXT.
 * MUST OBTAIN NEW INSTANCE whenever parameters that affect collector construction change.
 */
public class VertexCollectorList {
	private final ObjectArrayList<DrawableVertexCollector> active = new ObjectArrayList<>();
	private final DrawableVertexCollector[] collectors = new DrawableVertexCollector[RenderState.MAX_COUNT];
	private final ObjectArrayList<DrawableVertexCollector> drawList = new ObjectArrayList<>();
	/** If true, will segregate quads by face. */
	public final boolean trackFaces;
	/** If true, will segregate quads by shadow casting ability. */
	private final int[] target;
	protected final boolean isTerrain;

	public VertexCollectorList(boolean trackFaces, boolean isTerrain) {
		this.trackFaces = trackFaces;
		this.isTerrain = isTerrain;
		target = new int[isTerrain ? TerrainEncoder.TERRAIN_MATERIAL.quadStrideInts : CanvasVertexFormats.STANDARD_MATERIAL_FORMAT.quadStrideInts];
	}

	/**
	 * Where we handle all pre-buffer coloring, lighting, transformation, etc.
	 * Reused for all mesh quads. Fixed baking array sized to hold largest possible mesh quad.
	 */
	public class Emitter extends RootQuadEmitter {
		{
			data = new int[MeshEncodingHelper.TOTAL_MESH_QUAD_STRIDE];
			material(RenderMaterial.defaultMaterial());
		}

		@Override
		public Emitter emit() {
			final CanvasRenderMaterial mat = (CanvasRenderMaterial) material();

			if (mat.condition().compute()) {
				complete();
				StandardEncoder.encodeQuad(this, AbsentInputContext.INSTANCE, get(mat));

				if (Configurator.disableUnseenSpriteAnimation) {
					mat.trackPerFrameAnimation(this.spriteId());
				}
			}

			clear();
			return this;
		}

		public VertexEmitter prepare(CanvasRenderMaterial mat) {
			defaultMaterial(mat);
			clear();
			return this;
		}
	}

	public final Emitter emitter = new Emitter();

	/**
	 * Clears all storage arrays.
	 */
	public void clear() {
		final int limit = active.size();

		for (int i = 0; i < limit; i++) {
			active.get(i).clear();
		}
	}

	public final DrawableVertexCollector getIfExists(CanvasRenderMaterial materialState) {
		return materialState.isMissing() ? null : collectors[materialState.collectorIndex()];
	}

	public final DrawableVertexCollector get(CanvasRenderMaterial materialState) {
		if (materialState.isMissing()) {
			return null;
		}

		final int index = materialState.collectorIndex();
		final DrawableVertexCollector[] collectors = this.collectors;

		DrawableVertexCollector result = null;

		if (index < collectors.length) {
			result = collectors[index];
		}

		if (result == null) {
			if (materialState.sorted()) {
				result = new SortingVertexCollector(materialState.renderState(), isTerrain, target);
			} else if (Pipeline.shadowsEnabled()) {
				result = trackFaces
						? new TerrainShadowVertexCollector(materialState.renderState(), target)
						: new ShadowVertexCollector(materialState.renderState(), target);
			} else {
				result = trackFaces
						? new TerrainVertexCollector(materialState.renderState(), target)
						: new SimpleVertexCollector(materialState.renderState(), target);
			}

			collectors[index] = result;
			active.add(result);
		}

		return result;
	}

	public DrawableVertexCollector get(int index) {
		return active.get(index);
	}

	public int totalBytes(boolean sorted) {
		final int limit = active.size();
		final ObjectArrayList<DrawableVertexCollector> active = this.active;
		int intSize = 0;

		for (int i = 0; i < limit; i++) {
			final DrawableVertexCollector collector = active.get(i);

			if (!collector.isEmpty() && collector.sorted() == sorted) {
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
	public ObjectArrayList<DrawableVertexCollector> sortedDrawList(Predicate<RenderState> predicate) {
		final ObjectArrayList<DrawableVertexCollector> drawList = this.drawList;
		drawList.clear();

		final int limit = active.size();

		if (limit != 0) {
			for (int i = 0; i < limit; ++i) {
				final DrawableVertexCollector collector = get(i);

				if (!collector.isEmpty() && predicate.test(collector.renderState())) {
					drawList.add(collector);
				}
			}
		}

		if (drawList.size() > 1) {
			drawList.sort(DRAW_SORT);
		}

		return drawList;
	}

	private static final Comparator<DrawableVertexCollector> DRAW_SORT = (a, b) -> {
		// note reverse argument order - higher priority wins
		return Long.compare(b.renderState().drawPriority, a.renderState().drawPriority);
	};
}
