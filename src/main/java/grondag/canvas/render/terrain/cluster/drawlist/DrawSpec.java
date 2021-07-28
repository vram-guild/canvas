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

import java.nio.IntBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import grondag.canvas.render.terrain.cluster.Slab;

class DrawSpec {
	private IndexSlab indexSlab;
	private IntBuffer triVertexCount;
	private IntBuffer baseQuadVertexOffset;
	private PointerBuffer triIndexOffset;
	private final int size;
	private final TerrainVAO vao;
	private boolean isClosed = false;

	DrawSpec (Slab slab, int maxTriVertexCount, int[] triVertexCount, int[] baseQuadVertexOffset, long[] triIndexOffset) {
		size = triVertexCount.length;
		int allocationLen = maxTriVertexCount / 6 * 4 + 4095;
		allocationLen &= ~4095;
		indexSlab = IndexSlab.claim(allocationLen);
		indexSlab.allocateAndLoad(0, maxTriVertexCount / 6 * 4);
		indexSlab.upload();

		this.triVertexCount = MemoryUtil.memAllocInt(size);
		this.triVertexCount.put(0, triVertexCount);

		this.baseQuadVertexOffset = MemoryUtil.memAllocInt(size);
		this.baseQuadVertexOffset.put(0, baseQuadVertexOffset);

		this.triIndexOffset = MemoryUtil.memAllocPointer(size);
		this.triIndexOffset.put(triIndexOffset);
		this.triIndexOffset.position(0);

		vao = new TerrainVAO(() -> slab.glBufferId(), () -> indexSlab.glBufferId(), 0);
	}

	IntBuffer baseQuadVertexOffset() {
		assert baseQuadVertexOffset.position() == 0;
		assert baseQuadVertexOffset.limit() == size;
		return baseQuadVertexOffset;
	}

	IntBuffer triVertexCount() {
		assert triVertexCount.position() == 0;
		assert triVertexCount.limit() == size;
		return triVertexCount;
	}

	PointerBuffer triIndexOffset() {
		assert triIndexOffset.position() == 0;
		assert triIndexOffset.limit() == size;
		return triIndexOffset;
	}

	protected void release() {
		assert !isClosed;

		if (!isClosed) {
			isClosed = true;

			vao.shutdown();

			MemoryUtil.memFree(triVertexCount);
			triVertexCount = null;

			MemoryUtil.memFree(triIndexOffset);
			triIndexOffset = null;

			MemoryUtil.memFree(baseQuadVertexOffset);
			baseQuadVertexOffset = null;

			indexSlab.release();
			indexSlab = null;
		}
	}

	public void bind() {
		vao.bind();
	}
}
