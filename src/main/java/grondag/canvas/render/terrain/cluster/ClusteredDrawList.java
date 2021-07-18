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

package grondag.canvas.render.terrain.cluster;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.render.VertexFormat.DrawMode;

import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.terrain.base.AbstractDrawableRegionList;
import grondag.canvas.render.terrain.base.DrawableRegion;
import grondag.canvas.render.terrain.base.DrawableRegionList;
import grondag.canvas.varia.GFX;

public class ClusteredDrawList extends AbstractDrawableRegionList {
	final ObjectArrayList<ClusteredDrawListClump> drawClumps = new ObjectArrayList<>();
	final ClusteredDrawableState drawState;
	final int maxTriangleVertexCount;

	private ClusteredDrawList(final ObjectArrayList<DrawableRegion> regions, int maxTriangleVertexCount) {
		super(regions);
		this.maxTriangleVertexCount = maxTriangleVertexCount;
		drawState = ((ClusteredDrawableRegion) regions.get(0)).drawState();

		final Long2ObjectOpenHashMap<ClusteredDrawListClump> map = new Long2ObjectOpenHashMap<>();
		final int limit = regions.size();

		for (int regionIndex = 0; regionIndex < limit; ++regionIndex) {
			final ClusteredDrawableStorage storage = ((ClusteredDrawableRegion) regions.get(regionIndex)).drawState().storage();

			ClusteredDrawListClump clump = map.get(storage.clumpPos);

			if (clump == null) {
				clump = new ClusteredDrawListClump();
				drawClumps.add(clump);
			}

			clump.add(storage);
		}
	}

	public static DrawableRegionList build(final ObjectArrayList<DrawableRegion> regions) {
		if (regions.isEmpty()) {
			return DrawableRegionList.EMPTY;
		}

		final int limit = regions.size();
		int maxQuads = 0;

		for (int i = 0; i < limit; ++i) {
			maxQuads = Math.max(maxQuads, regions.get(i).drawState().quadVertexCount());
		}

		return new ClusteredDrawList(regions, maxQuads / 4 * 6);
	}

	@Override
	public void draw() {
		final RenderSystem.IndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS, maxTriangleVertexCount);
		final int indexBufferId = indexBuffer.getId();
		final int elementType = indexBuffer.getElementFormat().count; // "count" appears to be a yarn bug
		final int limit = drawClumps.size();

		GFX.bindVertexArray(0);
		drawState.renderState().enable(0, 0, 0, 0, 0);

		for (int clumpIndex = 0; clumpIndex < limit; ++clumpIndex) {
			ClusteredDrawListClump drawClump = drawClumps.get(clumpIndex);
			drawClump.bind();
			GFX.bindBuffer(GFX.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
			drawClump.draw(elementType);
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
