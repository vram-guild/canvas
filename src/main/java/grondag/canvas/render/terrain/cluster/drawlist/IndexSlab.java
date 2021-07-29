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

package grondag.canvas.render.terrain.cluster.drawlist;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayDeque;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.buffer.render.AbstractGlBuffer;
import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.buffer.render.TransferBuffers;
import grondag.canvas.buffer.util.BufferSynchronizer;
import grondag.canvas.buffer.util.BufferSynchronizer.SynchronizedBuffer;
import grondag.canvas.render.terrain.cluster.SlabAllocator;
import grondag.canvas.varia.GFX;

public class IndexSlab extends AbstractGlBuffer implements SynchronizedBuffer {
	public static final int PADDING_BYTES = 4096;
	private boolean isClaimed = false;
	private int headQuadVertexIndex = 0;
	private TransferBuffer transferBuffer;
	private ByteBuffer uploadBuffer;
	private final int quadVertexCapacity;

	private IndexSlab(int quadVertexCapacity) {
		// NB: STATIC makes a huge positive difference on AMD at least
		super(PADDING_BYTES + quadVertexCapacity * INDEX_QUAD_VERTEX_TO_TRIANGLE_BYTES_MULTIPLIER, GFX.GL_ELEMENT_ARRAY_BUFFER, GFX.GL_STATIC_DRAW);
		//super(BYTES_PER_INDEX_SLAB, GFX.GL_ELEMENT_ARRAY_BUFFER, GFX.GL_STATIC_DRAW);
		this.quadVertexCapacity = quadVertexCapacity;
		assert RenderSystem.isOnRenderThread();
	}

	private void prepareForClaim() {
		isClaimed = true;
		transferBuffer = TransferBuffers.claim(this.capacityBytes); //.BYTES_PER_INDEX_SLAB);
		uploadBuffer = transferBuffer.byteBuffer();
	}

	void release() {
		assert RenderSystem.isOnRenderThread();
		assert isClaimed;
		assert !isClosed;
		isClaimed = false;
		headQuadVertexIndex = 0;

		if (transferBuffer != null) {
			transferBuffer.release();
			uploadBuffer = null;
		}

		BufferSynchronizer.accept(this);
	}

	@Override
	public void onBufferSync() {
		shutdown();
		//POOL.offer(this);
	}

	/** How much vertex capacity is remaining. */
	int availableQuadVertexCount() {
		assert !isClosed;
		assert RenderSystem.isOnRenderThread();

		return quadVertexCapacity - headQuadVertexIndex;
	}

	/**
	 * The index buffer byte offset for the next allocation.
	 */
	int nextByteOffset() {
		assert RenderSystem.isOnRenderThread();
		assert !isClosed;

		return headQuadVertexIndex * INDEX_QUAD_VERTEX_TO_TRIANGLE_BYTES_MULTIPLIER;
	}

	/** Throws exception if insufficient capacity. Check {@link #availableQuadVertexCount()} prior to calling. */
	void allocateAndLoad(final int firstQuadVertexIndex, final int quadVertexCount) {
		assert RenderSystem.isOnRenderThread();
		assert !isClosed;

		if (quadVertexCount > availableQuadVertexCount()) {
			throw new IllegalStateException("Requested index allocation exceeds available capacity");
		}

		final var buff = uploadBuffer;
		final int newHead = headQuadVertexIndex + quadVertexCount;
		int triVertexIndex = PADDING_BYTES + headQuadVertexIndex * INDEX_QUAD_VERTEX_TO_TRIANGLE_BYTES_MULTIPLIER;
		int quadVertexIndex = firstQuadVertexIndex;
		final int limit = firstQuadVertexIndex + quadVertexCount;

		while (quadVertexIndex < limit) {
			buff.putShort(triVertexIndex, (short) quadVertexIndex);
			triVertexIndex += 2;
			buff.putShort(triVertexIndex, (short) (quadVertexIndex + 1));
			triVertexIndex += 2;
			buff.putShort(triVertexIndex, (short) (quadVertexIndex + 2));
			triVertexIndex += 2;
			buff.putShort(triVertexIndex, (short) (quadVertexIndex + 2));
			triVertexIndex += 2;
			buff.putShort(triVertexIndex, (short) (quadVertexIndex + 3));
			triVertexIndex += 2;
			buff.putShort(triVertexIndex, (short) quadVertexIndex);
			triVertexIndex += 2;
			quadVertexIndex += 4;
		}

		headQuadVertexIndex = newHead;
	}

	public void upload() {
		assert transferBuffer != null;

		if (transferBuffer != null) {
			GFX.bindBuffer(bindTarget, glBufferId());
			transferBuffer.transferToBoundBuffer(bindTarget, 0, 0, PADDING_BYTES + headQuadVertexIndex * INDEX_QUAD_VERTEX_TO_TRIANGLE_BYTES_MULTIPLIER);
			GFX.bindBuffer(bindTarget, 0);
			transferBuffer.release();
			transferBuffer = null;
			uploadBuffer = null;
		}
	}

	@Override
	protected void onShutdown() {
		assert RenderSystem.isOnRenderThread();

		if (transferBuffer != null) {
			transferBuffer.release();
			transferBuffer = null;
		}

		--totalSlabCount;
	}

	/** Ideally large enough to handle an entire draw list but not so large to push it out of VRAM. */
	public static final int BYTES_PER_INDEX_SLAB = 0x200000;

	/** Six tri vertices per four quad vertices at 2 bytes each gives 6 / 4 * 2 = 3. */
	public static final int INDEX_QUAD_VERTEX_TO_TRIANGLE_BYTES_MULTIPLIER = 6 * 2 / 4;

	/** Largest multiple of four vertices that, when expanded to triangles, will fit within the index buffer. */
	public static final int MAX_INDEX_SLAB_QUAD_VERTEX_COUNT = (BYTES_PER_INDEX_SLAB / INDEX_QUAD_VERTEX_TO_TRIANGLE_BYTES_MULTIPLIER) & 0xFFFFFFF8;

	private static final ArrayDeque<IndexSlab> POOL = new ArrayDeque<>();
	private static int totalSlabCount = 0;
	private static IndexSlab fullSlabIndex;

	static IndexSlab claim(int vertexCapacity) {
		assert RenderSystem.isOnRenderThread();

		IndexSlab result = POOL.poll();

		if (result == null) {
			result = new IndexSlab(vertexCapacity);
			++totalSlabCount;
		}

		result.prepareForClaim();
		return result;
	}

	public static IndexSlab fullSlabIndex() {
		IndexSlab result = fullSlabIndex;

		if (result == null) {
			result = IndexSlab.claim(SlabAllocator.MAX_SLAB_QUAD_VERTEX_COUNT);
			result.allocateAndLoad(0, SlabAllocator.MAX_SLAB_QUAD_VERTEX_COUNT);
			result.upload();
			result.unbind();
			fullSlabIndex = result;
		}

		return result;
	}

	public static String debugSummary() {
		return String.format("Idx slabs: %dMb / %dMb", (totalSlabCount - POOL.size()) * BYTES_PER_INDEX_SLAB / 0x100000, totalSlabCount * BYTES_PER_INDEX_SLAB / 0x100000);
	}
}
