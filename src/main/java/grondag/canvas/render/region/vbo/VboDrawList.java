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

package grondag.canvas.render.region.vbo;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.region.DrawableRegion;
import grondag.canvas.render.region.RegionDrawList;
import grondag.canvas.render.region.base.AbstractDrawList;
import grondag.canvas.varia.GFX;

public class VboDrawList extends AbstractDrawList {
	private VboDrawList(final ObjectArrayList<DrawableRegion> regions) {
		super(regions);
	}

	public static RegionDrawList build(final ObjectArrayList<DrawableRegion> regions) {
		if (regions.isEmpty()) {
			return RegionDrawList.EMPTY;
		}

		return new VboDrawList(regions);
	}

	@Override
	public void draw() {
		final int limit = regions.size();
		GFX.bindVertexArray(0);

		for (int regionIndex = 0; regionIndex < limit; ++regionIndex) {
			final VboDrawableRegion vboDrawable = (VboDrawableRegion) regions.get(regionIndex);
			final VboDrawableState drawState = vboDrawable.drawState();

			if (drawState != null) {
				final long modelOrigin = vboDrawable.packedOriginBlockPos();
				drawState.renderState().enable(BlockPos.unpackLongX(modelOrigin), BlockPos.unpackLongY(modelOrigin), BlockPos.unpackLongZ(modelOrigin), 0, 0);
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
		// NOOP
	}
}
