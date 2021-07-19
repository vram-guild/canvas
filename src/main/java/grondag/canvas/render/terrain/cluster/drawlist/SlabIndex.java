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
import grondag.canvas.render.terrain.cluster.Slab;
import grondag.canvas.varia.GFX;

class SlabIndex extends AbstractGlBuffer {
	private boolean isClaimed = false;
	private int headQuadVertexIndex = 0;
	private ShortBuffer mappedBuffer = null;

	private SlabIndex() {
		super(Slab.BYTES_PER_SLAB_INDEX, GFX.GL_ELEMENT_ARRAY_BUFFER, GFX.GL_STATIC_DRAW);
	}

	void release() {
		assert RenderSystem.isOnRenderThread();
		assert isClaimed;
		assert mappedBuffer == null;
		isClaimed = false;
		headQuadVertexIndex = 0;
		POOL.offer(this);
	}

	/** How much vertex capacity is remaining. */
	int availableQuadVertexCount() {
		return Slab.MAX_QUAD_VERTEX_COUNT - headQuadVertexIndex;
	}

	/** Returns baseoffset in bytes if successful, -1 if insufficient capacity. */
	int allocateAndLoad(final int firstQuadVertexIndex, final int quadVertexCount) {
		if (quadVertexCount > availableQuadVertexCount()) {
			return -1;
		}

		map();
		final var buff = mappedBuffer;
		final int newHead = headQuadVertexIndex + quadVertexCount;
		int triVertexIndex = headQuadVertexIndex / 4 * 6;
		int quadVertexIndex = firstQuadVertexIndex;

		while (quadVertexIndex < newHead) {
			buff.put(triVertexIndex++, (short) quadVertexIndex);
			buff.put(triVertexIndex++, (short) (quadVertexIndex + 1));
			buff.put(triVertexIndex++, (short) (quadVertexIndex + 2));
			buff.put(triVertexIndex++, (short) (quadVertexIndex + 2));
			buff.put(triVertexIndex++, (short) (quadVertexIndex + 3));
			buff.put(triVertexIndex++, (short) quadVertexIndex);
			quadVertexIndex += 4;
		}

		final int result = headQuadVertexIndex * Slab.QUAD_VERTEX_TO_TRIANGLE_BYTES_MULTIPLIER;
		headQuadVertexIndex = newHead;
		return result;
	}

	@Override
	public void bind() {
		if (mappedBuffer != null) {
			unmap();
		} else {
			super.bind();
		}
	}

	private void map() {
		if (mappedBuffer == null) {
			super.bind();
			final ByteBuffer buff = GFX.mapBufferRange(bindTarget, 0, Slab.BYTES_PER_SLAB_INDEX, GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_INVALIDATE_BUFFER_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT);
			mappedBuffer = buff.asShortBuffer();
		}
	}

	private void unmap() {
		if (mappedBuffer != null) {
			GFX.bindBuffer(bindTarget, glBufferId());
			GFX.flushMappedBufferRange(bindTarget, 0, headQuadVertexIndex * Slab.QUAD_VERTEX_TO_TRIANGLE_BYTES_MULTIPLIER);
			GFX.unmapBuffer(bindTarget);
			mappedBuffer = null;
		}
	}

	@Override
	protected void onShutdown() {
		// NOOP
	}

	private static final ArrayDeque<SlabIndex> POOL = new ArrayDeque<>();
	private static SlabIndex fullSlabIndex;

	static SlabIndex claim() {
		assert RenderSystem.isOnRenderThread();

		SlabIndex result = POOL.poll();

		if (result == null) {
			result = new SlabIndex();
		} else {
			result.orphan();
		}

		result.isClaimed = true;
		return result;
	}

	static SlabIndex fullSlabIndex() {
		SlabIndex result = fullSlabIndex;

		if (result == null) {
			result = new SlabIndex();
			final int check = result.allocateAndLoad(0, Slab.MAX_QUAD_VERTEX_COUNT);
			assert check == 0;
			result.unmap();
			result.unbind();
			fullSlabIndex = result;
		}

		return result;
	}
}
