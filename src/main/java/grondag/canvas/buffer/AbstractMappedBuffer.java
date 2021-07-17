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

import grondag.canvas.buffer.util.BinIndex;
import grondag.canvas.varia.GFX;

public class AbstractMappedBuffer extends AbstractGlBuffer implements AllocatableBuffer {
	final BinIndex binIndex;
	final int bindTarget;
	final int usageHint;
	private ByteBuffer mappedBuffer;
	private IntBuffer mappedIntBuffer;
	private int claimedBytes;

	protected AbstractMappedBuffer(BinIndex binIndex, int bindTarget, int usageHint) {
		super(binIndex.capacityBytes());
		this.binIndex = binIndex;
		this.bindTarget = bindTarget;
		this.usageHint = usageHint;
		GFX.bindBuffer(bindTarget, glBufferId());
		GFX.bufferData(bindTarget, capacityBytes, usageHint);
		GFX.bindBuffer(bindTarget, 0);
	}

	@Override
	public final void prepare(int claimedBytes) {
		this.claimedBytes = claimedBytes;
		// Invalidate and map buffer
		GFX.bindBuffer(bindTarget, glBufferId());
		mappedBuffer = GFX.mapBufferRange(bindTarget, 0, claimedBytes, GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_INVALIDATE_BUFFER_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT);
		GFX.bindBuffer(bindTarget, 0);
	}

	public final int sizeBytes() {
		return claimedBytes;
	}

	public final IntBuffer intBuffer() {
		IntBuffer result = mappedIntBuffer;

		if (result == null) {
			result = mappedBuffer.asIntBuffer();
			mappedIntBuffer = result;
		}

		return result;
	}

	/** Un-map and bind. */
	protected final void unmap() {
		if (mappedBuffer != null) {
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId());
			GFX.flushMappedBufferRange(GFX.GL_ARRAY_BUFFER, 0, claimedBytes);
			GFX.unmapBuffer(GFX.GL_ARRAY_BUFFER);
			mappedBuffer = null;
			mappedIntBuffer = null;
		}
	}

	@Override
	protected void onShutdown() {
		unmap();
	}

	@Override
	public final BinIndex binIndex() {
		return binIndex;
	}
}
