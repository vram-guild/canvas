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

package grondag.canvas.vf;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.buffer.CleanVAO;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.region.DrawableDelegate;
import grondag.canvas.render.region.DrawableRegion;
import grondag.canvas.terrain.occlusion.VisibleRegionList;
import grondag.canvas.terrain.region.RenderRegion;
import grondag.canvas.varia.GFX;
import grondag.canvas.vf.stream.VfStreamReference;

public class VfDrawSpec implements AutoCloseable {
	private final int[] vertexStarts;
	private final int[] vertexCounts;
	// stream holds 4 ints per region, x, y, z origin plus starting quad address
	// WIP: make private after moving draw here
	public VfStreamReference vfStream;
	private final RenderState renderState;

	private VfDrawSpec(RenderState renderState, int[] vertexStarts, int[] vertexCounts, VfStreamReference vfStream) {
		this.renderState = renderState;
		this.vertexStarts = vertexStarts;
		this.vertexCounts = vertexCounts;
		this.vfStream = vfStream;
	}

	public static VfDrawSpec build(final VisibleRegionList visibleRegions, boolean isTranslucent) {
		final int count = visibleRegions.size();

		if (count == 0) {
			return EMPTY;
		}

		RenderState renderState = null;

		final IntArrayList vertexStarts = new IntArrayList();
		final IntArrayList vertexCounts = new IntArrayList();
		final IntArrayList regionData = new IntArrayList();

		final int startIndex = isTranslucent ? count - 1 : 0;
		final int endIndex = isTranslucent ? -1 : count;
		final int step = isTranslucent ? -1 : 1;

		int vertex = 0;

		for (int regionIndex = startIndex; regionIndex != endIndex; regionIndex += step) {
			final RenderRegion builtRegion = visibleRegions.get(regionIndex);

			if (builtRegion == null) {
				continue;
			}

			final DrawableRegion drawable = isTranslucent ? builtRegion.translucentDrawable() : builtRegion.solidDrawable();

			if (!drawable.isClosed()) {
				final DrawableDelegate delegate = drawable.delegate();

				if (delegate != null) {
					if (renderState == null) {
						renderState = delegate.renderState();
					} else {
						assert renderState == delegate.renderState();
					}

					final BlockPos modelOrigin = builtRegion.origin;
					regionData.add(modelOrigin.getX());
					regionData.add(modelOrigin.getY());
					regionData.add(modelOrigin.getZ());
					regionData.add(delegate.vfbr().getByteAddress() / 16);
					// Theoretically could be anything because we aren't using a vertex array
					// but setting to offsets as if we were hoping that avoid driver complaints.
					vertexStarts.add(vertex);
					// convert to triangles
					final int vCount = delegate.vertexCount() / 4 * 6;
					vertexCounts.add(vCount);
					vertex += vCount;
				}
			}
		}

		if (vertexStarts.isEmpty()) {
			return EMPTY;
		}

		final int[] r = regionData.toIntArray();

		VfStreamReference ref = Vf.REGIONS.allocate(vertexStarts.size() * 16, (buff, index) -> {
			buff.put(index, r);
		});

		return new VfDrawSpec(renderState, vertexStarts.toIntArray(), vertexCounts.toIntArray(), ref);
	}

	@Override
	public void close() {
		if (vfStream != null) {
			vfStream.close();
			vfStream = null;
		}
	}

	public void draw() {
		CleanVAO.bind();
		renderState.enable();
		vfStream.bind();
		GFX.multiDrawArrays(GFX.GL_TRIANGLES, vertexStarts, vertexCounts);
		vfStream.unbind();
		RenderState.disable();
		CleanVAO.unbind();
	}

	public static final VfDrawSpec EMPTY = new VfDrawSpec(null, new int[0], new int[0], null) {
		@Override
		public void draw() {
			// NOOP
		}
	};
}
