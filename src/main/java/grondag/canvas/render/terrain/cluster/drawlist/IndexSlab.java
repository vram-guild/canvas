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
import grondag.canvas.render.terrain.cluster.SlabAllocator;
import grondag.canvas.varia.GFX;

public class IndexSlab extends AbstractGlBuffer {
	private boolean isClaimed = false;
	private int headQuadVertexIndex = 0;
	private ShortBuffer mappedBuffer = null;

	private IndexSlab() {
		// NB: STATIC makes a huge positive difference on AMD at least
		super(BYTES_PER_INDEX_SLAB, GFX.GL_ELEMENT_ARRAY_BUFFER, GFX.GL_STATIC_DRAW);
		assert RenderSystem.isOnRenderThread();
	}

	void release() {
		assert RenderSystem.isOnRenderThread();
		assert isClaimed;
		assert mappedBuffer == null;
		assert !isClosed;

		isClaimed = false;
		headQuadVertexIndex = 0;
		POOL.offer(this);
	}

	/** How much vertex capacity is remaining. */
	int availableQuadVertexCount() {
		assert !isClosed;
		assert RenderSystem.isOnRenderThread();

		return MAX_INDEX_SLAB_QUAD_VERTEX_COUNT - headQuadVertexIndex;
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

		map();
		final var buff = mappedBuffer;
		final int newHead = headQuadVertexIndex + quadVertexCount;
		int triVertexIndex = headQuadVertexIndex / 4 * 6;
		int quadVertexIndex = firstQuadVertexIndex;
		final int limit = firstQuadVertexIndex + quadVertexCount;

		while (quadVertexIndex < limit) {
			buff.put(triVertexIndex++, (short) quadVertexIndex);
			buff.put(triVertexIndex++, (short) (quadVertexIndex + 1));
			buff.put(triVertexIndex++, (short) (quadVertexIndex + 2));
			buff.put(triVertexIndex++, (short) (quadVertexIndex + 2));
			buff.put(triVertexIndex++, (short) (quadVertexIndex + 3));
			buff.put(triVertexIndex++, (short) quadVertexIndex);
			quadVertexIndex += 4;
		}

		headQuadVertexIndex = newHead;
	}

	@Override
	public void bind() {
		assert !isClosed;

		if (mappedBuffer != null) {
			unmap();
		} else {
			super.bind();
		}
	}

	private void map() {
		assert !isClosed;

		if (mappedBuffer == null) {
			super.bind();
			// NB: not using GFX.GL_MAP_INVALIDATE_BUFFER_BIT because we orphan the buffer on claim
			final ByteBuffer buff = GFX.mapBufferRange(bindTarget, 0, BYTES_PER_INDEX_SLAB, GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT);
			mappedBuffer = buff.asShortBuffer();
		}
	}

	private void unmap() {
		assert !isClosed;

		if (mappedBuffer != null) {
			GFX.bindBuffer(bindTarget, glBufferId());
			GFX.flushMappedBufferRange(bindTarget, 0, headQuadVertexIndex * INDEX_QUAD_VERTEX_TO_TRIANGLE_BYTES_MULTIPLIER);
			GFX.unmapBuffer(bindTarget);
			mappedBuffer = null;
		}
	}

	@Override
	protected void onShutdown() {
		assert RenderSystem.isOnRenderThread();
		--totalSlabCount;
	}

	/** Ideally large enough to handle an entire draw list but not so large to push it out of VRAM. */
	public static final int BYTES_PER_INDEX_SLAB = 0x200000;

	/** Six tri vertices per four quad vertices at 2 bytes each gives 6 / 4 * 2 = 3. */
	public static final int INDEX_QUAD_VERTEX_TO_TRIANGLE_BYTES_MULTIPLIER = 3;

	/** Largest multiple of four vertices that, when expanded to triangles, will fit within the index buffer. */
	public static final int MAX_INDEX_SLAB_QUAD_VERTEX_COUNT = (BYTES_PER_INDEX_SLAB / INDEX_QUAD_VERTEX_TO_TRIANGLE_BYTES_MULTIPLIER) & 0xFFFFFFF8;

	private static final ArrayDeque<IndexSlab> POOL = new ArrayDeque<>();
	private static int totalSlabCount = 0;
	private static IndexSlab fullSlabIndex;

	static IndexSlab claim() {
		assert RenderSystem.isOnRenderThread();

		IndexSlab result = POOL.poll();

		if (result == null) {
			result = new IndexSlab();
			++totalSlabCount;
		} else {
			result.orphan();
		}

		result.isClaimed = true;
		return result;
	}

	static IndexSlab fullSlabIndex() {
		IndexSlab result = fullSlabIndex;

		if (result == null) {
			result = new IndexSlab();
			result.allocateAndLoad(0, SlabAllocator.MAX_SLAB_QUAD_VERTEX_COUNT);
			result.unmap();
			result.unbind();
			fullSlabIndex = result;
		}

		return result;
	}

	public static String debugSummary() {
		return String.format("Idx slabs: %dMb / %dMb", (totalSlabCount - POOL.size()) * BYTES_PER_INDEX_SLAB / 0x100000, totalSlabCount * BYTES_PER_INDEX_SLAB / 0x100000);
	}
}
