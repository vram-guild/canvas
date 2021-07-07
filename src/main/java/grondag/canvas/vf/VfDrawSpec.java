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

// WIP: use gl_DrawId instead of quad-to-region map when GL4.6 available

public class VfDrawSpec implements AutoCloseable {
	// stream holds 4 ints per region, x, y, z origin plus starting quad address
	private final int vertexCount;
	private VfStreamReference regionStream;
	private VfStreamReference quadMapStream;
	private final RenderState renderState;

	private VfDrawSpec(RenderState renderState, int vertexCount, VfStreamReference regionStream, VfStreamReference quadMapStream) {
		this.renderState = renderState;
		this.vertexCount = vertexCount;
		this.regionStream = regionStream;
		this.quadMapStream = quadMapStream;
	}

	public static VfDrawSpec build(final VisibleRegionList visibleRegions, boolean isTranslucent) {
		final int count = visibleRegions.size();

		if (count == 0) {
			return EMPTY;
		}

		RenderState renderState = null;

		final IntArrayList regionData = new IntArrayList();
		final IntArrayList quadMapData = new IntArrayList();

		final int startIndex = isTranslucent ? count - 1 : 0;
		final int endIndex = isTranslucent ? -1 : count;
		final int step = isTranslucent ? -1 : 1;

		int totalQuadCount = 0;
		int regionArrayIndex = 0;

		int quadMap = 0;
		boolean odd = true;

		for (int regionLoopIndex = startIndex; regionLoopIndex != endIndex; regionLoopIndex += step) {
			// max region address space in array buffer
			assert regionArrayIndex < 0x10000;

			final RenderRegion builtRegion = visibleRegions.get(regionLoopIndex);

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
					// Subtract the total quad count because gl_VertexID (and thus Quad ID)
					// will increment through the entire draw.
					final int regionQuadCount = delegate.quadVertexCount() / 4;
					final int rIndex = delegate.regionStorageReference().getByteAddress() / 16 - totalQuadCount;
					regionData.add(rIndex);

					for (int q = 0; q < regionQuadCount; ++q) {
						if (odd) {
							quadMap = regionArrayIndex;
							odd = false;
						} else {
							quadMap |= regionArrayIndex << 16;
							quadMapData.add(quadMap);
							odd = true;
						}
					}

					totalQuadCount += regionQuadCount;
					++regionArrayIndex;
				}
			}
		}

		if (!odd) {
			quadMapData.add(quadMap);
		}

		if (regionData.isEmpty()) {
			return EMPTY;
		}

		VfStreamReference regionStream = TerrainVertexFetch.REGIONS.allocate(regionData.size() * 4, (buff, index) -> {
			buff.put(index, regionData.toIntArray());
		});

		VfStreamReference quadMapStream = TerrainVertexFetch.QUAD_REGION_MAP.allocate(quadMapData.size() * 4, (buff, index) -> {
			buff.put(index, quadMapData.toIntArray());
		});

		assert regionStream != null;
		assert regionStream != VfStreamReference.EMPTY;

		assert quadMapStream != null;
		assert quadMapStream != VfStreamReference.EMPTY;

		return new VfDrawSpec(renderState, totalQuadCount * 6, regionStream, quadMapStream);
	}

	@Override
	public void close() {
		regionStream.close();
		regionStream = VfStreamReference.EMPTY;
		quadMapStream.close();
		quadMapStream = VfStreamReference.EMPTY;
	}

	public void draw() {
		CleanVAO.bind();
		regionStream.bind();
		quadMapStream.bind();
		// NB: divide by two for quad map stream because each component is a short
		renderState.enable(0, 0, 0, regionStream.byteAddress / 16, quadMapStream.byteAddress / 2);
		GFX.drawArrays(GFX.GL_TRIANGLES, 0, vertexCount);
		regionStream.unbind();
		quadMapStream.unbind();
		RenderState.disable();
		CleanVAO.unbind();
	}

	//public static final VfDrawSpec EMPTY = new VfDrawSpec(null, new int[0], new int[0], VfStreamReference.EMPTY, VfStreamReference.EMPTY) {
	public static final VfDrawSpec EMPTY = new VfDrawSpec(null, 0, VfStreamReference.EMPTY, VfStreamReference.EMPTY) {
		@Override
		public void draw() {
			// NOOP
		}

		@Override
		public void close() {
			// NOOP - avoids NPE due to EMPTY going missing
		}
	};
}
