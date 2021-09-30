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

package grondag.canvas.buffer.render;

import org.jetbrains.annotations.Nullable;

import grondag.canvas.buffer.format.BufferVAO;
import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.util.BinIndex;
import grondag.canvas.varia.GFX;

public class StreamBuffer extends AbstractMappedBuffer<StreamBuffer> implements AllocatableBuffer, UploadableVertexStorage {
	public final CanvasVertexFormat format;
	private final BufferVAO vao;

	protected StreamBuffer(BinIndex binIndex, CanvasVertexFormat format) {
		super(binIndex, GFX.GL_ARRAY_BUFFER, GFX.GL_STREAM_DRAW, StreamBufferAllocator::release);
		this.format = format;
		vao = new BufferVAO(format, () -> glBufferId(), () -> 0);
	}

	public static @Nullable StreamBuffer claim(int claimedBytes, CanvasVertexFormat standardMaterialFormat) {
		return StreamBufferAllocator.claim(standardMaterialFormat, claimedBytes);
	}

	/** Un-map and bind for drawing. */
	@Override
	public void upload() {
		unmap();
	}

	@Override
	protected void onShutdown() {
		super.onShutdown();
		vao.shutdown();
	}

	@Override
	public void bind() {
		vao.bind();
	}
}
