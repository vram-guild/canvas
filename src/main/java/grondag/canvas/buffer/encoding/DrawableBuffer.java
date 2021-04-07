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

import java.nio.IntBuffer;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import grondag.canvas.buffer.VboBuffer;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.varia.GFX;

public class DrawableBuffer implements AutoCloseable {
	@Nullable private VboBuffer buffer;
	private final int limit;
	private final int[] counts;
	private final RenderState[] states;

	public DrawableBuffer(ObjectArrayList<VertexCollectorImpl> drawList) {
		limit = drawList.size();

		int bytes = 0;

		for (int i = 0; i < limit; ++i) {
			final VertexCollectorImpl collector = drawList.get(i);
			collector.sortIfNeeded();
			bytes += collector.byteSize();
		}

		// PERF: trial memory mapped here
		buffer = new VboBuffer(bytes, CanvasVertexFormats.POSITION_COLOR_TEXTURE_MATERIAL_LIGHT_NORMAL);
		final IntBuffer intBuffer = buffer.intBuffer();
		counts = new int[limit];
		states = new RenderState[limit];

		intBuffer.position(0);

		for (int i = 0; i < limit; ++i) {
			final VertexCollectorImpl collector = drawList.get(i);
			collector.toBuffer(intBuffer);
			counts[i] = collector.vertexCount();
			states[i] = collector.materialState.renderState;
			collector.clear();
		}

		drawList.clear();
		buffer.upload();
	}

	private DrawableBuffer() {
		buffer = null;
		limit = 0;
		counts = null;
		states = null;
	}

	public void draw(boolean isShadow) {
		if (buffer != null) {
			buffer.bind();
			int startIndex = 0;

			for (int i = 0; i < limit; ++i) {
				final RenderState state = states[i];
				final int vertexCount = counts[i];

				if (state.castShadows || !isShadow) {
					state.enable();
					final int elementCount = vertexCount / 4 * 6;
					final RenderSystem.IndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(state.primitive, elementCount);
					GFX.bindBuffer(GFX.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.getId());
					final int elementType = indexBuffer.getVertexFormat().field_27374;
					GFX.drawElementsBaseVertex(state.primitive.mode, elementCount, elementType, 0L, startIndex);
				}

				startIndex += vertexCount;
			}

			RenderState.disable();
			GFX.bindVertexArray(0);
		}
	}

	@Override
	public void close() {
		if (buffer != null) {
			buffer.close();
			buffer = null;
		}
	}

	public static final DrawableBuffer EMPTY = new DrawableBuffer();
}
