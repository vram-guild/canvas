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
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import grondag.canvas.buffer.util.GlBufferAllocator;
import grondag.canvas.varia.GFX;

public class MappedTransferBuffer implements TransferBuffer {
	private final int capacityBytes;
	private final Consumer<MappedTransferBuffer> releaseQueue;
	private int glBufferId;
	private ByteBuffer mappedBuffer;
	private int claimedBytes;

	public MappedTransferBuffer(int capacityBytes, Consumer<MappedTransferBuffer> releaseQueue) {
		this.capacityBytes = capacityBytes;
		this.releaseQueue = releaseQueue;
		glBufferId = GlBufferAllocator.claimBuffer(capacityBytes);
		GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, glBufferId);
		GFX.bufferData(GFX.GL_COPY_READ_BUFFER, capacityBytes, GFX.GL_STREAM_READ);
		GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, 0);
	}

	// WIP: remove - should always be doing copy to buffer
	@Override
	public @Nullable TransferBuffer releaseToMappedBuffer(ByteBuffer targetBuffer, int targetOffset, int sourceOffset, int byteCount) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IntBuffer asIntBuffer() {
		return mappedBuffer.asIntBuffer();
	}

	@Override
	public @Nullable TransferBuffer release() {
		// We don't do anything here because we may not be on render thread.
		// Will prepare for next frame when allocation manager requests it.
		claimedBytes = 0;
		releaseQueue.accept(this);
		return null;
	}

	// WIP: remove - should always be doing copy to buffer
	@Override
	public @Nullable TransferBuffer releaseToBuffer(int target, int usage) {
		throw new UnsupportedOperationException();
	}

	// WIP: remove - should always be doing copy to buffer
	@Override
	public @Nullable TransferBuffer releaseToSubBuffer(int glArrayBuffer, int unpackVacancyAddress, int vacantBytes) {
		throw new UnsupportedOperationException();
	}

	/** Called by allocation manager on render thread before made available to requesters. */
	void reset() {
		if (mappedBuffer != null) {
			GFX.unmapBuffer(glBufferId);
			mappedBuffer = null;
		}

		// Invalidate and map buffer
		GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, glBufferId);
		mappedBuffer = GFX.mapBufferRange(GFX.GL_COPY_READ_BUFFER, 0, capacityBytes, GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_INVALIDATE_BUFFER_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT);
	}

	/** Called by allocation manager before given to requester. */
	void setClaimed(int claimedBytes) {
		assert claimedBytes > 0;
		assert claimedBytes <= capacityBytes;
		this.claimedBytes = claimedBytes;
	}

	/** Used only by allocation manager to prune / shut down. */
	void close() {
		if (mappedBuffer != null) {
			GFX.unmapBuffer(glBufferId);
			mappedBuffer = null;
		}

		if (glBufferId != 0) {
			GlBufferAllocator.releaseBuffer(glBufferId, capacityBytes);
			glBufferId = 0;
		}
	}
}
