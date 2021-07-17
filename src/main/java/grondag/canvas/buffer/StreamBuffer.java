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

package grondag.canvas.buffer;

import org.jetbrains.annotations.Nullable;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.util.BinIndex;
import grondag.canvas.render.region.UploadableVertexStorage;
import grondag.canvas.varia.GFX;

public class StreamBuffer extends AbstractMappedBuffer<StreamBuffer> implements AllocatableBuffer, UploadableVertexStorage {
	public final CanvasVertexFormat format;
	private final BufferVAO vao;

	protected StreamBuffer(BinIndex binIndex, CanvasVertexFormat format) {
		super(binIndex, GFX.GL_ARRAY_BUFFER, GFX.GL_STREAM_DRAW, StreamBufferAllocator::release);
		this.format = format;
		vao = new BufferVAO(format);
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

	public void bind() {
		vao.bind(GFX.GL_ARRAY_BUFFER, glBufferId());
	}
}
