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

package grondag.canvas.render.terrain.cluster;

import static grondag.canvas.render.terrain.cluster.SlabAllocator.BYTES_PER_SLAB_VERTEX;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.buffer.render.AbstractGlBuffer;
import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.buffer.util.BufferSynchronizer;
import grondag.canvas.buffer.util.BufferSynchronizer.SynchronizedBuffer;
import grondag.canvas.render.terrain.cluster.VertexCluster.RegionAllocation.SlabAllocation;
import grondag.canvas.render.terrain.cluster.VertexCluster.SlabAllocationFactory;
import grondag.canvas.varia.GFX;

public class Slab extends AbstractGlBuffer implements SynchronizedBuffer {
	private final TransferSlab transferSlab = new TransferSlab();
	private int headVertexIndex = 0;
	private int usedVertexCount;
	private final int maxVertexCount;

	Slab(int capacityBytes) {
		// NB: STATIC makes a huge positive difference on AMD at least
		super(capacityBytes, GFX.GL_ARRAY_BUFFER, GFX.GL_STATIC_DRAW);
		assert RenderSystem.isOnRenderThread();
		maxVertexCount = (capacityBytes / BYTES_PER_SLAB_VERTEX) & ~3;
	}

	TransferBuffer asTransferBuffer() {
		return transferSlab;
	}

	/** How much vertex capacity is remaining. */
	int availableVertexCount() {
		assert RenderSystem.isOnRenderThread();
		assert headVertexIndex <= maxVertexCount;
		return maxVertexCount - headVertexIndex;
	}

	int usedVertexCount() {
		return usedVertexCount;
	}

	/**
	 * Excludes bytes no longer used by their allocation.
	 * Thus, may not match {@link #capacityBytes()} - {@link #availableBytes()}.
	 */
	int usedBytes() {
		return usedVertexCount * BYTES_PER_SLAB_VERTEX;
	}

	int availableBytes() {
		return availableVertexCount() * BYTES_PER_SLAB_VERTEX;
	}

	public boolean isFull() {
		return availableVertexCount() == 0;
	}

	boolean isEmpty() {
		assert RenderSystem.isOnRenderThread();
		return usedVertexCount == 0;
	}

	void release() {
		assert RenderSystem.isOnRenderThread();
		assert usedVertexCount == 0;
		headVertexIndex = 0;
		BufferSynchronizer.accept(this);
	}

	private void addToVertexCounts(int vertexCount) {
		usedVertexCount += vertexCount;
		assert usedVertexCount >= 0;
		SlabAllocator.addToVertexCount(vertexCount);
	}

	@Override
	public void onBufferSync() {
		shutdown();
	}

	/** Returns the number of vertices allocated. */
	SlabAllocation allocateAndLoad(SlabAllocationFactory factory, TransferBuffer buffer) {
		final int quadVertexCount = buffer.sizeBytes() / BYTES_PER_SLAB_VERTEX;
		assert quadVertexCount * BYTES_PER_SLAB_VERTEX == buffer.sizeBytes();
		return allocateInner(factory, buffer, 0, quadVertexCount);
	}

	/** Returns the number of quad vertices transfered. */
	SlabAllocation transferFromSlabAllocation(SlabAllocationFactory factory, SlabAllocation source) {
		return allocateInner(factory, source.slab.asTransferBuffer(), source.baseQuadVertexIndex, source.quadVertexCount);
	}

	private SlabAllocation allocateInner(SlabAllocationFactory factory, TransferBuffer buffer, int sourceStartVertexIndex, int allocatedVertexCount) {
		if (allocatedVertexCount <= 0) {
			return null;
		}

		final int newHeadVertexIndex = headVertexIndex + allocatedVertexCount;
		final var allocation = factory.create(this, headVertexIndex, allocatedVertexCount);
		addToVertexCounts(allocatedVertexCount);

		GFX.bindBuffer(bindTarget, glBufferId());
		buffer.transferToBoundBuffer(bindTarget,
				headVertexIndex * BYTES_PER_SLAB_VERTEX,
				sourceStartVertexIndex * BYTES_PER_SLAB_VERTEX,
				allocatedVertexCount * BYTES_PER_SLAB_VERTEX);

		headVertexIndex = newHeadVertexIndex;
		return allocation;
	}

	void removeAllocation(SlabAllocation allocation) {
		assert RenderSystem.isOnRenderThread();
		assert !isClosed;
		addToVertexCounts(-allocation.quadVertexCount);
	}

	@Override
	protected void onShutdown() {
		assert RenderSystem.isOnRenderThread();
		assert usedVertexCount == 0;
		SlabAllocator.notifyShutdown(this);
	}

	private class TransferSlab implements TransferBuffer {
		@Override
		public void transferToBoundBuffer(int target, int targetStartBytes, int sourceStartBytes, int lengthBytes) {
			GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, glBufferId());

			GFX.copyBufferSubData(
					GFX.GL_COPY_READ_BUFFER,
					target,
					sourceStartBytes,
					targetStartBytes,
					lengthBytes);

			GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, 0);
		}

		@Override
		public int sizeBytes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void put(int[] source, int sourceStart, int targetStart, int length) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @Nullable TransferBuffer release() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ShortBuffer shortBuffer() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer byteBuffer() {
			throw new UnsupportedOperationException();
		}
	}
}
