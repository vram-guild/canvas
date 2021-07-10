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

import net.minecraft.client.render.VertexFormat.DrawMode;

import grondag.canvas.buffer.VboBuffer;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.region.base.AbstractDrawableState;
import grondag.canvas.varia.GFX;

public class VsDrawableState extends AbstractDrawableState<VboBuffer> {
	private final int vertexOffset;
	private final int triVertexCount;

	public VsDrawableState(RenderState renderState, int quadVertexCount, int vertexOffset, VboBuffer vboBuffer) {
		super(renderState, quadVertexCount, vboBuffer);
		this.vertexOffset = vertexOffset;
		triVertexCount = quadVertexCount() / 4 * 6;
	}

	/**
	 * Assumes pipeline has already been activated and buffer has already been bound
	 * via {@link #bind()}.
	 * @param indexBufferId
	 */
	public void draw(int elementType, int indexBufferId) {
		assert !isClosed();

		if (storage != null) {
			storage.bind();
			GFX.bindBuffer(GFX.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
			GFX.drawElementsBaseVertex(DrawMode.QUADS.mode, triVertexCount, elementType, 0L, vertexOffset);
		}
	}
}
