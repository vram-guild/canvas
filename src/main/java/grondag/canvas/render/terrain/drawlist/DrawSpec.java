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

package grondag.canvas.render.terrain.drawlist;

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
