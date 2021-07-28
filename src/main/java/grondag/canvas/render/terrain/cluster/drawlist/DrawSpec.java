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

class DrawSpec extends AbstractVaoBinding {
	final IndexSlab indexSlab;
	private IntBuffer triVertexCount;
	private IntBuffer baseQuadVertexOffset;
	private PointerBuffer triIndexOffset;
	private final int size;

	DrawSpec(Slab slab, IndexSlab indexSlab, int[] triVertexCount, int baseQuadVertexOffset[], long triIndexOffset[]) {
		super(slab, 0);
		size = triVertexCount.length;
		this.indexSlab = indexSlab;
		
		this.triVertexCount = MemoryUtil.memAllocInt(size);
		this.triVertexCount.put(0, triVertexCount);
		
		this.baseQuadVertexOffset = MemoryUtil.memAllocInt(size);
		this.baseQuadVertexOffset.put(0, baseQuadVertexOffset);

		this.triIndexOffset = MemoryUtil.memAllocPointer(size);
		this.triIndexOffset.put(triIndexOffset);
		this.triIndexOffset.position(0);
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

	@Override
	protected void onRelease() {
		MemoryUtil.memFree(triVertexCount);
		triVertexCount = null;

		MemoryUtil.memFree(triIndexOffset);
		triIndexOffset = null;
		
		MemoryUtil.memFree(baseQuadVertexOffset);
		baseQuadVertexOffset = null;
	}

	@Override
	protected IndexSlab indexSlab() {
		return IndexSlab.fullSlabIndex();
		//return indexSlab;
	}
}
