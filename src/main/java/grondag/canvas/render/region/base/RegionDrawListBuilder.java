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

package grondag.canvas.render.region.base;

import java.util.function.Function;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.render.region.DrawableRegion;
import grondag.canvas.render.region.RegionDrawList;
import grondag.canvas.terrain.occlusion.VisibleRegionList;
import grondag.canvas.terrain.region.RenderRegion;

public final class RegionDrawListBuilder {
	private RegionDrawListBuilder() { }

	public static RegionDrawList build(
			final VisibleRegionList visibleRegions,
			Function<ObjectArrayList<DrawableRegion>, RegionDrawList> drawListFunc,
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

			if (drawable != null && drawable != DrawableRegion.EMPTY_DRAWABLE && !drawable.isReleasedFromRegion()) {
				drawables.add(drawable);
				drawable.retainFromDrawList();
			}
		}

		return drawables.isEmpty() ? RegionDrawList.EMPTY : drawListFunc.apply(drawables);
	}
}
