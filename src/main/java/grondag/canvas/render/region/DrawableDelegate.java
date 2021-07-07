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

package grondag.canvas.render.region;

import java.util.concurrent.ArrayBlockingQueue;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.VertexFormat.DrawMode;

import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.varia.GFX;
import grondag.canvas.vf.storage.VfStorageReference;

public class DrawableDelegate {
	private static final ArrayBlockingQueue<DrawableDelegate> store = new ArrayBlockingQueue<>(4096);
	private RenderState renderState;
	private VfStorageReference regionStorageReference;

	private int vertexOffset;
	private int quadVertexCount;
	private boolean isReleased = false;

	private DrawableDelegate() {
		super();
	}

	public static DrawableDelegate claim(RenderState renderState, int vertexOffset, int quadVertexCount, VfStorageReference regionStorageReference) {
		DrawableDelegate result = store.poll();

		if (result == null) {
			result = new DrawableDelegate();
		}

		result.renderState = renderState;
		result.vertexOffset = vertexOffset;
		result.quadVertexCount = quadVertexCount;
		result.isReleased = false;
		result.regionStorageReference = regionStorageReference;
		return result;
	}

	public VfStorageReference regionStorageReference() {
		return regionStorageReference;
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

		if (Configurator.vf) {
			GFX.drawArrays(GFX.GL_TRIANGLES, 0, quadVertexCount / 4 * 6);
		} else {
			final int triVertexCount = quadVertexCount / 4 * 6;
			final RenderSystem.IndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS, triVertexCount);
			final int elementType = indexBuffer.getElementFormat().count; // "count" appears to be a yarn defect
			GFX.bindBuffer(GFX.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.getId());
			GFX.drawElementsBaseVertex(DrawMode.QUADS.mode, triVertexCount, elementType, 0L, vertexOffset);
		}
	}

	public void release() {
		assert RenderSystem.isOnRenderThread();

		if (!isReleased) {
			isReleased = true;
			renderState = null;

			if (regionStorageReference != null) {
				regionStorageReference.close();
				regionStorageReference = null;
			}

			store.offer(this);
		}
	}

	public int quadVertexCount() {
		return quadVertexCount;
	}
}
