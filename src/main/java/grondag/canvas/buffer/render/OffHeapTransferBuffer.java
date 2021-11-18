/*
 * Copyright © Original Authors
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
