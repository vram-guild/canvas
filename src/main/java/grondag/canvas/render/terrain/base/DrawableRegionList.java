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

package grondag.canvas.render.terrain.base;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.material.state.RenderState;
import grondag.canvas.material.state.TerrainRenderStates;
import grondag.canvas.render.terrain.drawlist.RealmDrawList;
import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.terrain.occlusion.VisibleRegionList;
import grondag.canvas.terrain.region.RenderRegion;

public interface DrawableRegionList {
	void close();

	boolean isClosed();

	void draw(WorldRenderState worldRenderState);

	int quadCount();

	DrawableRegionList EMPTY = new DrawableRegionList() {
		@Override
		public void close() {
			// NOOP
		}

		@Override
		public boolean isClosed() {
			return false;
		}

		@Override
		public void draw(WorldRenderState worldRenderState) {
			// NOOP
		}

		@Override
		public int quadCount() {
			return 0;
		}
	};

	static DrawableRegionList build(
			final VisibleRegionList visibleRegions,
			boolean isTranslucent,
			boolean isShadowMap
	) {
		final ObjectArrayList<DrawableRegion> drawables = new ObjectArrayList<>();

		final int count = visibleRegions.size();
		final int startIndex = isTranslucent ? count - 1 : 0;
		final int endIndex = isTranslucent ? -1 : count;
		final int step = isTranslucent ? -1 : 1;

		for (int regionLoopIndex = startIndex; regionLoopIndex != endIndex; regionLoopIndex += step) {
			final RenderRegion region = visibleRegions.get(regionLoopIndex);
			final DrawableRegion drawable = isTranslucent ? region.translucentDrawable() : region.solidDrawable();

			if (drawable != null && drawable != DrawableRegion.EMPTY_DRAWABLE) {
				drawables.add(drawable);
				drawable.retainFromDrawList();
			}
		}

		final var renderState = isTranslucent ? TerrainRenderStates.TRANSLUCENT : TerrainRenderStates.SOLID;
		return drawables.isEmpty() ? DrawableRegionList.EMPTY : RealmDrawList.build(drawables, renderState, isShadowMap);
	}

	@FunctionalInterface
	interface DrawableRegionListFunc {
		DrawableRegionList apply(ObjectArrayList<DrawableRegion> regions, RenderState renderState, boolean isShadowMap);
	}
}
