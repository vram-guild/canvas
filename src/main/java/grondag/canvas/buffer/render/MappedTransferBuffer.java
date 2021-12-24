/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.buffer.render;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import grondag.canvas.buffer.util.BinIndex;
import grondag.canvas.varia.GFX;

public class MappedTransferBuffer extends AbstractMappedBuffer<MappedTransferBuffer> implements TransferBuffer {
	public MappedTransferBuffer(BinIndex binIndex) {
		super(binIndex, GFX.GL_COPY_READ_BUFFER, GFX.GL_STREAM_READ, RENDER_THREAD_ALLOCATOR::release);
	}

	@Override
	public void put(int[] source, int sourceStartInts, int targetStartInts, int lengthInts) {
		intBuffer().put(targetStartInts, source, sourceStartInts, lengthInts);
	}

	@Override
	public void prepareForOffThreadUse() {
		super.prepareForOffThreadUse();
	}

	@Override
	public void transferToBoundBuffer(int target, int targetStartBytes, int sourceStartBytes, int lengthBytes) {
		assert sourceStartBytes + lengthBytes <= sizeBytes();

		if (!unmap()) {
			bind();
		}

		GFX.copyBufferSubData(GFX.GL_COPY_READ_BUFFER, target, sourceStartBytes, targetStartBytes, lengthBytes);
		GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, 0);
	}

	static final BufferAllocator<MappedTransferBuffer> RENDER_THREAD_ALLOCATOR = new BufferAllocator<>("RENDER THREAD MAPPED", MappedTransferBuffer::new, ArrayDeque::new);
	static final TrackedBufferAllocator<MappedTransferBuffer> THREAD_SAFE_ALLOCATOR = new TrackedBufferAllocator<>("OFF THREAD MAPPED", b -> null, ConcurrentLinkedQueue::new);
}
