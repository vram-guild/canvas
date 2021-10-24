/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.buffer.util;

import java.nio.IntBuffer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.buffer.input.DrawableVertexCollector;
import grondag.canvas.buffer.input.FaceBucket;
import grondag.canvas.buffer.render.StreamBuffer;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.varia.GFX;

public class DrawableStream implements AutoCloseable {
	@Nullable private StreamBuffer buffer;
	private final int limit;
	private final FaceBucket[] buckets;
	private final RenderState[] states;

	public DrawableStream(ObjectArrayList<? extends DrawableVertexCollector> drawList) {
		limit = drawList.size();

		int bytes = 0;

		for (int i = 0; i < limit; ++i) {
			final DrawableVertexCollector collector = drawList.get(i);
			collector.sortIfNeeded();
			bytes += collector.byteSize();
		}

		buffer = StreamBuffer.claim(bytes, CanvasVertexFormats.STANDARD_MATERIAL_FORMAT);
		final IntBuffer intBuffer = buffer.intBuffer();
		buckets = new FaceBucket[limit];
		states = new RenderState[limit];

		intBuffer.position(0);
		int startIndex = 0;

		for (int i = 0; i < limit; ++i) {
			final DrawableVertexCollector collector = drawList.get(i);
			collector.toBuffer(intBuffer, 0);
			buckets[i] = collector.faceBucket(startIndex);
			states[i] = collector.renderState();
			startIndex += collector.vertexCount();
			collector.clear();
		}

		drawList.clear();
		buffer.upload();
	}

	private DrawableStream() {
		buffer = null;
		limit = 0;
		buckets = null;
		states = null;
	}

	public void draw(boolean isShadow) {
		if (buffer != null) {
			buffer.bind();

			for (int i = 0; i < limit; ++i) {
				final RenderState state = states[i];
				final FaceBucket bucket = buckets[i];

				final int startIndex = isShadow ? bucket.shadowVertexIndex() : bucket.colorVertexIndex();
				final int vertexCount = isShadow ? bucket.shadowVertexCount() : bucket.colorVertexCount();

				state.enable();
				final int elementCount = vertexCount / 4 * 6;
				final RenderSystem.AutoStorageIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(Mode.QUADS, elementCount);
				GFX.bindBuffer(GFX.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.name());
				final int elementType = indexBuffer.type().asGLType;
				GFX.drawElementsBaseVertex(Mode.QUADS.asGLMode, elementCount, elementType, 0L, startIndex);
			}

			RenderState.disable();
			GFX.bindVertexArray(0);
		}
	}

	@Override
	public void close() {
		if (buffer != null) {
			buffer.release();
			buffer = null;
		}
	}

	public static final DrawableStream EMPTY = new DrawableStream();
}
