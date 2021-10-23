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

import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.base.renderer.mesh.BaseQuadView;

import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.terrain.TerrainFormat;

public class CompoundVertexCollector extends ArrayVertexCollector implements DrawableVertexCollector {
	public final boolean isTerrain;
	private final BucketSorter bucketSorter;

	public CompoundVertexCollector(RenderState renderState, boolean isTerrain, int[] target) {
		super(renderState, isTerrain ? TerrainFormat.TERRAIN_MATERIAL.quadStrideInts : CanvasVertexFormats.STANDARD_MATERIAL_FORMAT.quadStrideInts, target);
		this.isTerrain = isTerrain;
		bucketSorter = isTerrain ? new BucketSorter() : null;
	}

	@Override
	public void commit(BaseQuadView quad, RenderMaterial mat) {
		assert isTerrain;

		if (bucketSorter != null) {
			bucketSorter.add(quad.effectiveCullFaceId(), integerSize);
		}

		commit(quadStrideInts);
	}

	@Override
	public final void clear() {
		super.clear();

		if (bucketSorter != null) {
			bucketSorter.clear();
		}
	}

	@Override
	public VertexBucket[] vertexBuckets() {
		return bucketSorter == null ? null : bucketSorter.sort(vertexData, integerSize);
	}
}
