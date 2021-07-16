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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.jetbrains.annotations.Nullable;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.util.BinIndex;
import grondag.canvas.varia.GFX;

public class StreamBuffer extends AbstractDrawBuffer {
	final BinIndex binIndex;
	private ByteBuffer mappedBuffer;
	private int claimedBytes;

	protected StreamBuffer(BinIndex binIndex, CanvasVertexFormat format) {
		super(binIndex.capacityBytes(), format);
		this.binIndex = binIndex;
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId());
		GFX.bufferData(GFX.GL_ARRAY_BUFFER, capacityBytes, GFX.GL_STREAM_DRAW);
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
	}

	void prepare(int claimedBytes) {
		this.claimedBytes = claimedBytes;
		// Invalidate and map buffer
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId());
		mappedBuffer = GFX.mapBufferRange(GFX.GL_ARRAY_BUFFER, 0, claimedBytes, GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_INVALIDATE_BUFFER_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT);
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
	}

	/** MUST be called if one of other release methods isn't. ALWAYS returns null. */
	@Override
	@Nullable
	public StreamBuffer release() {
		upload();
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
		StreamBufferAllocator.release(this);
		return null;
	}

	public static @Nullable StreamBuffer claim(int claimedBytes, CanvasVertexFormat standardMaterialFormat) {
		return StreamBufferAllocator.claim(standardMaterialFormat, claimedBytes);
	}

	@Override
	public IntBuffer intBuffer() {
		return mappedBuffer.asIntBuffer();
	}

	/** Un-map and bind for drawing. */
	@Override
	public void upload() {
		if (mappedBuffer != null) {
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId());
			GFX.flushMappedBufferRange(GFX.GL_ARRAY_BUFFER, 0, claimedBytes);
			GFX.unmapBuffer(GFX.GL_ARRAY_BUFFER);
			mappedBuffer = null;
		}
	}

	@Override
	protected void onShutdown() {
		upload();
	}
}
