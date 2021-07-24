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

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;

import grondag.canvas.buffer.render.AbstractGlBuffer;
import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.render.terrain.TerrainFormat;
import grondag.canvas.render.terrain.cluster.VertexCluster.RegionAllocation.SlabAllocation;
import grondag.canvas.render.terrain.cluster.VertexCluster.SlabAllocationFactory;
import grondag.canvas.varia.GFX;

public class Slab extends AbstractGlBuffer {
	final SlabAllocator allocator;
	private final TransferSlab transferSlab = new TransferSlab();
	private int headVertexIndex = 0;
	private boolean isClaimed;
	private int vaoBufferId = 0;
	private int usedVertexCount;

	Slab(SlabAllocator allocator) {
		// NB: STATIC makes a huge positive difference on AMD at least
		super(allocator.bytesPerSlab, GFX.GL_ARRAY_BUFFER, GFX.GL_STATIC_DRAW);
		this.allocator = allocator;
		assert RenderSystem.isOnRenderThread();
	}

	TransferBuffer asTransferBuffer() {
		return transferSlab;
	}

	/** How much vertex capacity is remaining. */
	int availableVertexCount() {
		assert RenderSystem.isOnRenderThread();
		assert headVertexIndex <= allocator.maxSlabQuadVertexCount;
		return allocator.maxSlabQuadVertexCount - headVertexIndex;
	}

	int usedVertexCount() {
		return usedVertexCount;
	}

	int usedBytes() {
		return usedVertexCount * BYTES_PER_SLAB_VERTEX;
	}

	public int vacatedVertexCount() {
		return headVertexIndex - usedVertexCount;
	}

	public boolean isFull() {
		return availableVertexCount() == 0;
	}

	boolean isEmpty() {
		assert RenderSystem.isOnRenderThread();
		return usedVertexCount == 0;
	}

	boolean isVacated() {
		return vacatedVertexCount() > allocator.vacatedQuadVertexThreshold;
	}

	void prepareForClaim() {
		assert !isClaimed;
		assert usedVertexCount == 0;
		isClaimed = true;
	}

	void release() {
		assert usedVertexCount == 0;
		releaseInner();
	}

	private void addToVertexCounts(int vertexCount) {
		usedVertexCount += vertexCount;
		allocator.addToVertexCount(vertexCount);
	}

	/** Doesn't assume or require all regions to have closed. */
	void releaseTransferred() {
		releaseInner();
	}

	private void releaseInner() {
		assert RenderSystem.isOnRenderThread();
		assert isClaimed;
		headVertexIndex = 0;
		isClaimed = false;
		addToVertexCounts(-usedVertexCount);
		allocator.release(this);
	}

	/** Returns the number of vertices allocated. */
	SlabAllocation allocateAndLoad(SlabAllocationFactory factory, TransferBuffer buffer, int sourceStartVertexIndex) {
		final int quadVertexCount = buffer.sizeBytes() / BYTES_PER_SLAB_VERTEX;
		assert quadVertexCount * BYTES_PER_SLAB_VERTEX == buffer.sizeBytes();
		assert sourceStartVertexIndex < quadVertexCount;
		final int allocatedVertexCount = Math.min(availableVertexCount(), quadVertexCount - sourceStartVertexIndex);
		return allocateInner(factory, buffer, sourceStartVertexIndex, allocatedVertexCount);
	}

	/** Returns the number of quad vertices transfered. */
	SlabAllocation transferFromSlabAllocation(SlabAllocationFactory factory, SlabAllocation source, int sourceStartVertexIndex) {
		assert sourceStartVertexIndex < source.quadVertexCount;
		final int allocatedVertexCount = Math.min(availableVertexCount(), source.quadVertexCount - sourceStartVertexIndex);
		return allocateInner(factory, source.slab.asTransferBuffer(), source.baseQuadVertexIndex + sourceStartVertexIndex, allocatedVertexCount);
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
	public void bind() {
		assert !isClosed;

		if (vaoBufferId == 0) {
			vaoBufferId = GFX.genVertexArray();
			GFX.bindVertexArray(vaoBufferId);

			super.bind();
			TerrainFormat.TERRAIN_MATERIAL.enableAttributes();
			TerrainFormat.TERRAIN_MATERIAL.bindAttributeLocations(0);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
		} else {
			GFX.bindVertexArray(vaoBufferId);
		}
	}

	@Override
	protected void onShutdown() {
		assert RenderSystem.isOnRenderThread();

		if (vaoBufferId != 0) {
			GFX.deleteVertexArray(vaoBufferId);
			vaoBufferId = 0;
		}

		addToVertexCounts(-usedVertexCount);
		allocator.notifyShutdown(this);
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
	}
}
