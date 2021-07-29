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

import java.util.function.BiFunction;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.render.RenderLayer;

import grondag.canvas.material.state.RenderLayerHelper;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.terrain.occlusion.VisibleRegionList;
import grondag.canvas.terrain.region.RenderRegion;

public final class DrawableRegionListBuilder {
	private static final RenderState TRANSLUCENT_TERRAIN = RenderLayerHelper.TRANSLUCENT_TERRAIN.renderState;
	private static final RenderState SOLID_TERRAIN = RenderLayerHelper.copyFromLayer(RenderLayer.getSolid()).renderState;

	private DrawableRegionListBuilder() { }

	public static DrawableRegionList build(
			final VisibleRegionList visibleRegions,
			BiFunction<ObjectArrayList<DrawableRegion>, RenderState, DrawableRegionList> drawListFunc,
			boolean isTranslucent
	) {
		final ObjectArrayList<DrawableRegion> drawables = new ObjectArrayList<>();

		final int count = visibleRegions.size();
		final int startIndex = isTranslucent ? count - 1 : 0;
		final int endIndex = isTranslucent ? -1 : count;
		final int step = isTranslucent ? -1 : 1;

		for (int regionLoopIndex = startIndex; regionLoopIndex != endIndex; regionLoopIndex += step) {
			RenderRegion region = visibleRegions.get(regionLoopIndex);
			final DrawableRegion drawable = isTranslucent ? region.translucentDrawable() : region.solidDrawable();

			if (drawable != null && drawable != DrawableRegion.EMPTY_DRAWABLE) {
				drawables.add(drawable);
				drawable.retainFromDrawList();
			}
		}

		final var renderState = isTranslucent ? TRANSLUCENT_TERRAIN : SOLID_TERRAIN;
		return drawables.isEmpty() ? DrawableRegionList.EMPTY : drawListFunc.apply(drawables, renderState);
	}
}
