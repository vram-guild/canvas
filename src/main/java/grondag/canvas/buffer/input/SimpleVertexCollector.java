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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.world.phys.Vec3;

import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.base.renderer.mesh.BaseQuadView;

import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.terrain.TerrainFormat;
import grondag.canvas.render.terrain.TerrainSectorMap.RegionRenderSector;

public class SimpleVertexCollector extends ArrayVertexCollector implements DrawableVertexCollector {
	public final boolean isTerrain;
	public final RenderState renderState;
	private final VertexBucket.Sorter bucketSorter;

	public SimpleVertexCollector(RenderState renderState, boolean sorted, boolean isTerrain) {
		super(isTerrain ? TerrainFormat.TERRAIN_MATERIAL.quadStrideInts : CanvasVertexFormats.STANDARD_MATERIAL_FORMAT.quadStrideInts);
		this.renderState = renderState;
		this.isTerrain = isTerrain;
		bucketSorter = isTerrain && !sorted ? new VertexBucket.Sorter() : null;
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
	public final RenderState renderState() {
		return renderState;
	}

	@Override
	public final void draw(boolean clear) {
		if (!isEmpty()) {
			drawSingle();

			if (clear) {
				clear();
			}
		}
	}

	@Override
	public void sortIfNeeded() { }

	/** Avoid: slow. */
	public final void drawSingle() {
		// PERF: allocation - or eliminate this
		final ObjectArrayList<SimpleVertexCollector> drawList = new ObjectArrayList<>();
		drawList.add(this);
		DrawableVertexCollector.draw(drawList);
	}

	@Override
	public boolean sorted() {
		return false;
	}

	@Override
	public boolean sortTerrainQuads(Vec3 sortPos, RegionRenderSector sector) {
		return false;
	}

	@Override
	public VertexBucket[] sortVertexBuckets() {
		return null;
	}
}
