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

package grondag.canvas.render.region.vs;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.region.DrawableRegion;
import grondag.canvas.render.region.RegionDrawList;
import grondag.canvas.render.region.base.AbstractDrawList;
import grondag.canvas.varia.GFX;

public class VsDrawList extends AbstractDrawList {
	private VsDrawList(final ObjectArrayList<DrawableRegion> regions) {
		super(regions);
	}

	public static RegionDrawList build(final ObjectArrayList<DrawableRegion> regions) {
		if (regions.isEmpty()) {
			return RegionDrawList.EMPTY;
		}

		return new VsDrawList(regions);
	}

	@Override
	public void draw() {
		final int limit = regions.size();

		if (limit == 0) {
			return;
		}

		GFX.bindVertexArray(0);

		// WIP: still need to handle multiple render states somehow
		((VsDrawableRegion) regions.get(0)).drawState().renderState().enable(0, 0, 0, 0, 0);

		for (int regionIndex = 0; regionIndex < limit; ++regionIndex) {
			final VsDrawableRegion vsDrawable = (VsDrawableRegion) regions.get(regionIndex);
			final VsDrawableState drawState = vsDrawable.drawState();

			if (drawState != null) {
				vsDrawable.bindIfNeeded();
				drawState.draw();
			}
		}

		// Important this happens BEFORE anything that could affect vertex state
		GFX.bindVertexArray(0);

		RenderState.disable();

		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
	}

	@Override
	protected void closeInner() {
		// WIP
	}
}
