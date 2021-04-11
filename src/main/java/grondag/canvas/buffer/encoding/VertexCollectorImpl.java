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

package grondag.canvas.buffer.encoding;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.material.state.RenderMaterialImpl;

public class VertexCollectorImpl extends AbstractVertexCollector {
	public VertexCollectorImpl prepare(RenderMaterialImpl materialState) {
		clear();
		this.materialState = materialState;
		material(materialState);
		return this;
	}

	public void clear() {
		currentVertexIndex = 0;
		vertexArray.clear();
		didPopulateNormal = false;
	}

	public RenderMaterialImpl materialState() {
		return materialState;
	}

	@Override
	public VertexCollectorImpl clone() {
		throw new UnsupportedOperationException();
	}

	public int[] saveState(int[] priorState) {
		final int integerSize = vertexArray.integerSize();

		if (integerSize == 0) {
			return null;
		}

		int[] result = priorState;

		if (result == null || result.length != integerSize) {
			result = new int[integerSize];
		}

		if (integerSize > 0) {
			System.arraycopy(vertexArray.data(), 0, result, 0, integerSize);
		}

		return result;
	}

	public VertexCollectorImpl loadState(RenderMaterialImpl state, int[] stateData) {
		clear();
		materialState = state;

		if (stateData != null) {
			final int size = stateData.length;
			vertexArray.allocate(size);
			System.arraycopy(stateData, 0, vertexArray.data(), 0, size);
		}

		return this;
	}

	public void draw(boolean clear) {
		if (!vertexArray.isEmpty()) {
			drawSingle();

			if (clear) {
				clear();
			}
		}
	}

	void sortIfNeeded() {
		if (materialState.sorted) {
			vertexArray.sortQuads(0, 0, 0);
		}
	}

	/** Avoid: slow. */
	public void drawSingle() {
		// PERF: allocation - or eliminate this
		final ObjectArrayList<VertexCollectorImpl> drawList = new ObjectArrayList<>();
		drawList.add(this);
		draw(drawList);
	}

	/**
	 * Single-buffer draw, minimizes state changes.
	 * Assumes all collectors are non-empty.
	 */
	public static void draw(ObjectArrayList<VertexCollectorImpl> drawList) {
		final DrawableBuffer buffer = new DrawableBuffer(drawList);
		buffer.draw(false);
		buffer.close();
	}

	@Override
	protected void emitQuad() {
		if (conditionActive) {
			final int idx = vertexArray.allocate(CanvasVertexFormats.MATERIAL_QUAD_STRIDE);
			System.arraycopy(vertexData, 0, vertexArray.data(), idx, CanvasVertexFormats.MATERIAL_QUAD_STRIDE);
		}

		currentVertexIndex = 0;
	}
}
