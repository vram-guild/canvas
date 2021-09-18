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
