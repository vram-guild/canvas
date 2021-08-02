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
	private IntBuffer triVertexCount;
	private IntBuffer baseQuadVertexOffset;
	private PointerBuffer triIndexOffset;
	private final int size;
	private final TerrainVAO vao;
	private boolean isClosed = false;

	DrawSpec (Slab slab, int size, int[] triVertexCount, int[] baseQuadVertexOffset) {
		this.size = size;
		this.triVertexCount = MemoryUtil.memAllocInt(size);
		this.triVertexCount.put(0, triVertexCount, 0, size);

		this.baseQuadVertexOffset = MemoryUtil.memAllocInt(size);
		this.baseQuadVertexOffset.put(0, baseQuadVertexOffset, 0, size);

		triIndexOffset = MemoryUtil.memAllocPointer(size);

		for (int i = 0; i < size; ++i) {
			triIndexOffset.put(i, 0L);
		}

		triIndexOffset.position(0);

		vao = new TerrainVAO(() -> slab.glBufferId(), () -> SlabIndex.get().glBufferId(), 0);
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
		}
	}

	public void bind() {
		vao.bind();
	}
}
