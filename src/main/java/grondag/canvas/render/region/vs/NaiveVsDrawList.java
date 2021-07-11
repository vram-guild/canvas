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
//
//package grondag.canvas.render.region.vs;
//
//import com.mojang.blaze3d.systems.RenderSystem;
//import it.unimi.dsi.fastutil.objects.ObjectArrayList;
//import org.lwjgl.PointerBuffer;
//
//import net.minecraft.client.render.VertexFormat.DrawMode;
//
//import grondag.canvas.material.state.RenderState;
//import grondag.canvas.render.region.DrawableRegion;
//import grondag.canvas.render.region.RegionDrawList;
//import grondag.canvas.render.region.base.AbstractDrawList;
//import grondag.canvas.varia.GFX;
//
//public class NaiveVsDrawList extends AbstractDrawList {
//	/**
//	 * Largest length in triangle vertices of any region in the list.
//	 * Used to size our index buffer 1X.
//	 */
//	private final int maxTriangleVertexCount;
//	private final int[] vertexCounts;
//	private final int[] baseIndices;
//	private final PointerBuffer indexPointers;
//
//	private NaiveVsDrawList(final ObjectArrayList<DrawableRegion> regions, int maxTriangleVertexCount) {
//		super(regions);
//		this.maxTriangleVertexCount = maxTriangleVertexCount;
//		final int limit = regions.size();
//
//		vertexCounts = new int[limit];
//		baseIndices = new int[limit];
//		indexPointers = PointerBuffer.allocateDirect(limit);
//
//		for (int regionIndex = 0; regionIndex < limit; ++regionIndex) {
//			vertexCounts[regionIndex] = ((NaiveVsDrawableRegion) regions.get(regionIndex)).drawState().drawVertexCount();
//			indexPointers.put(regionIndex, 0L);
//		}
//	}
//
//	public static RegionDrawList build(final ObjectArrayList<DrawableRegion> regions) {
//		if (regions.isEmpty()) {
//			return RegionDrawList.EMPTY;
//		}
//
//		final int limit = regions.size();
//		int maxQuads = 0;
//
//		for (int i = 0; i < limit; ++i) {
//			maxQuads = Math.max(maxQuads, regions.get(i).drawState().quadVertexCount());
//		}
//
//		return new NaiveVsDrawList(regions, maxQuads / 4 * 6);
//	}
//
//	@Override
//	public void draw() {
//		final int limit = regions.size();
//
//		if (limit == 0) {
//			return;
//		}
//
//		GFX.bindVertexArray(0);
//
//		final RenderSystem.IndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS, maxTriangleVertexCount);
//		final int indexBufferId = indexBuffer.getId();
//		final int elementType = indexBuffer.getElementFormat().count; // "count" appears to be a yarn bug
//		VsVertexStorage.INSTANCE.bind();
//		GFX.bindBuffer(GFX.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
//
//		// WIP: still need to handle multiple render states somehow
//		((NaiveVsDrawableRegion) regions.get(0)).drawState().renderState().enable(0, 0, 0, 0, 0);
//
//		for (int regionIndex = 0; regionIndex < limit; ++regionIndex) {
//			baseIndices[regionIndex] = ((NaiveVsDrawableRegion) regions.get(regionIndex)).drawState().storage().baseVertex();
//		}
//
//		GFX.glMultiDrawElementsBaseVertex(DrawMode.QUADS.mode, vertexCounts, elementType, indexPointers, baseIndices);
//
//		// Important this happens BEFORE anything that could affect vertex state
//		GFX.bindVertexArray(0);
//
//		RenderState.disable();
//
//		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
//	}
//
//	@Override
//	protected void closeInner() {
//		// WIP
//	}
//}
