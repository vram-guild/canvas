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

package grondag.canvas.buffer.render;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jetbrains.annotations.Nullable;

import grondag.canvas.buffer.util.BinIndex;
import grondag.canvas.buffer.util.BufferTrace;
import grondag.canvas.buffer.util.DirectBufferAllocator;
import grondag.canvas.buffer.util.DirectBufferAllocator.DirectBufferReference;
import grondag.canvas.varia.GFX;

class OffHeapTransferBuffer implements TransferBuffer, AllocatableBuffer {
	final BinIndex bin;
	final int capacityBytes;
	DirectBufferReference data;
	volatile int claimedBytes;
	private final BufferTrace trace = BufferTrace.create();

	OffHeapTransferBuffer(BinIndex bin) {
		this.bin = bin;
		capacityBytes = bin.capacityBytes();
		data = DirectBufferAllocator.claim(capacityBytes);
	}

	@Override
	public ShortBuffer shortBuffer() {
		return data.buffer().asShortBuffer();
	}

	@Override
	public ByteBuffer byteBuffer() {
		return data.buffer();
	}

	@Override
	public void put(int[] source, int sourceStartInts, int targetStartInts, int lengthInts) {
		assert claimedBytes > 0 : "Buffer accessed while unclaimed";
		assert sourceStartInts + lengthInts <= claimedBytes * 4;
		data.buffer().asIntBuffer().put(targetStartInts, source, sourceStartInts, lengthInts);
	}

	@Override
	public int sizeBytes() {
		assert claimedBytes > 0 : "Buffer accessed while unclaimed";
		return claimedBytes;
	}

	@Override
	public void transferToBoundBuffer(int target, int targetStartBytes, int sourceStartBytes, int lengthBytes) {
		assert claimedBytes > 0 : "Buffer accessed while unclaimed";
		assert sourceStartBytes + lengthBytes <= claimedBytes;

		final var buffer = data.buffer();
		buffer.position(sourceStartBytes);
		GFX.bufferSubData(target, targetStartBytes, lengthBytes, buffer);
		buffer.position(0);
	}

	@Override
	public BinIndex binIndex() {
		return bin;
	}

	@Override
	public void prepare(int claimedBytes) {
		assert this.claimedBytes == 0 : "Buffer claimed more than once";
		assert claimedBytes > 0 : "Buffer claimed with zero bytes";
		this.claimedBytes = claimedBytes;
	}

	@Override
	public void shutdown() {
		data.release();
		data = null;
	}

	@Override
	public @Nullable OffHeapTransferBuffer release() {
		assert claimedBytes > 0 : "Buffer released while unclaimed";
		claimedBytes = 0;
		THREAD_SAFE_ALLOCATOR.release(this);
		return null;
	}

	@Override
	public BufferTrace trace() {
		return trace;
	}

	static final BufferAllocator<OffHeapTransferBuffer> THREAD_SAFE_ALLOCATOR = new BufferAllocator<>("OFF HEAP", OffHeapTransferBuffer::new, ConcurrentLinkedQueue::new);
}
