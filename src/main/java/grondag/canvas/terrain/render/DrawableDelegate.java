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

package grondag.canvas.terrain.render;

import java.util.concurrent.ArrayBlockingQueue;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.material.state.RenderState;
import grondag.canvas.varia.GFX;

public class DrawableDelegate {
	private static final ArrayBlockingQueue<DrawableDelegate> store = new ArrayBlockingQueue<>(4096);
	private RenderState renderState;
	private int vertexOffset;
	private int vertexCount;
	private boolean isReleased = false;

	private DrawableDelegate() {
		super();
	}

	public static DrawableDelegate claim(RenderState renderState, int vertexOffset, int vertexCount) {
		DrawableDelegate result = store.poll();

		if (result == null) {
			result = new DrawableDelegate();
		}

		result.renderState = renderState;
		result.vertexOffset = vertexOffset;
		result.vertexCount = vertexCount;
		result.isReleased = false;
		return result;
	}

	/**
	 * The pipeline (and vertex format) associated with this delegate.
	 */
	public RenderState renderState() {
		return renderState;
	}

	/**
	 * Assumes pipeline has already been activated and buffer has already been bound
	 * via {@link #bind()}.
	 */
	public void draw() {
		assert !isReleased;

		final int triVertexCount = vertexCount / 4 * 6;
		final RenderSystem.IndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(renderState.primitive, triVertexCount);
		final int elementType = indexBuffer.getVertexFormat().field_27374;
		GFX.bindBuffer(GFX.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.getId());
		//GFX.drawElements(materialState.primitive.mode, triVertexCount, elementType, 0);
		GFX.drawElementsBaseVertex(renderState.primitive.mode, triVertexCount, elementType, 0L, vertexOffset);
		//GlStateManager.drawArrays(GL11.GL_QUADS, vertexOffset, vertexCount);
	}

	public void release() {
		assert RenderSystem.isOnRenderThread();

		if (!isReleased) {
			isReleased = true;
			renderState = null;
			store.offer(this);
		}
	}

	public int vertexCount() {
		return vertexCount;
	}
}
