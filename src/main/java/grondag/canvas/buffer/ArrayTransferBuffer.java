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

import java.util.concurrent.ConcurrentLinkedQueue;

import org.jetbrains.annotations.Nullable;

import grondag.canvas.buffer.util.BinIndex;

class ArrayTransferBuffer implements TransferBuffer, AllocatableBuffer {
	final BinIndex bin;
	final int capacityBytes;
	final int[] data;
	volatile int claimedBytes;
	private final BufferTrace trace = BufferTrace.create();

	ArrayTransferBuffer(BinIndex bin) {
		this.bin = bin;
		capacityBytes = bin.capacityBytes();
		data = new int[capacityBytes];
	}

	@Override
	public void put(int[] source, int sourceStartInts, int targetStartInts, int lengthInts) {
		assert claimedBytes > 0 : "Buffer accessed while unclaimed";
		assert sourceStartInts + lengthInts <= claimedBytes * 4;
		System.arraycopy(source, sourceStartInts, data, targetStartInts, lengthInts);
	}

	@Override
	public int sizeBytes() {
		assert claimedBytes > 0 : "Buffer accessed while unclaimed";
		return claimedBytes;
	}

	@Override
	public @Nullable ArrayTransferBuffer releaseToBoundBuffer(int target, int targetStartBytes) {
		assert claimedBytes > 0 : "Buffer accessed while unclaimed";
		final MappedTransferBuffer mapped = MappedTransferBuffer.claim(claimedBytes);
		mapped.put(data, 0, 0, claimedBytes / 4);
		mapped.releaseToBoundBuffer(target, targetStartBytes);
		release();
		return null;
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
		// NOOP
	}

	static ArrayTransferBuffer claim(int claimedBytes) {
		return THREAD_SAFE_ALLOCATOR.claim(claimedBytes);
	}

	@Override
	public @Nullable ArrayTransferBuffer release() {
		assert claimedBytes > 0 : "Buffer released while unclaimed";
		claimedBytes = 0;
		THREAD_SAFE_ALLOCATOR.release(this);
		return null;
	}

	@Override
	public BufferTrace trace() {
		return trace;
	}

	private static final RenderThreadBufferAllocator<ArrayTransferBuffer> THREAD_SAFE_ALLOCATOR = new RenderThreadBufferAllocator<>(ArrayTransferBuffer::new, ConcurrentLinkedQueue::new);
}
