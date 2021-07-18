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

package grondag.canvas.render.terrain.clump;

import org.lwjgl.PointerBuffer;

import net.minecraft.client.render.VertexFormat.DrawMode;

import grondag.canvas.varia.GFX;

public class MultiClumpedDrawListClump extends ClumpedDrawListClump {
	private int[] vertexCounts;
	private int[] baseIndices;
	private PointerBuffer indexPointers;

	@Override
	public void draw(int elementType) {
		if (vertexCounts == null) {
			prepareForDraw();
		}

		GFX.glMultiDrawElementsBaseVertex(DrawMode.QUADS.mode, vertexCounts, elementType, indexPointers, baseIndices);
	}

	private void prepareForDraw() {
		final int limit = stores.size();
		vertexCounts = new int[limit];
		baseIndices = new int[limit];
		indexPointers = PointerBuffer.allocateDirect(limit);

		for (int regionIndex = 0; regionIndex < limit; ++regionIndex) {
			var storage = stores.get(regionIndex);
			baseIndices[regionIndex] = storage.baseVertex();
			vertexCounts[regionIndex] = storage.triVertexCount;
			indexPointers.put(regionIndex, 0L);
		}
	}

	@Override
	public void add(ClumpedDrawableStorage storage) {
		stores.add(storage);
	}

	@Override
	public void bind() {
		stores.get(0).getClump().bind();
	}
}
