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

import grondag.canvas.render.terrain.TerrainFormat;
import grondag.canvas.render.terrain.cluster.Slab;
import grondag.canvas.varia.GFX;

class DrawSpec {
	final Slab slab;
	final IndexSlab indexSlab;
	private IntBuffer triVertexCount;
	private PointerBuffer indexBaseByteAddress;
	private int vaoBufferId = 0;
	boolean isClosed = false;

	DrawSpec(Slab slab, IndexSlab indexSlab, int triVertexCount, int indexBaseByteAddress) {
		this.slab = slab;
		this.indexSlab = indexSlab;

		this.triVertexCount = MemoryUtil.memAllocInt(4);
		this.triVertexCount.put(0, triVertexCount);
		this.triVertexCount.limit(1);

		this.indexBaseByteAddress = MemoryUtil.memAllocPointer(4);
		this.indexBaseByteAddress.put(0, indexBaseByteAddress);
		this.indexBaseByteAddress.limit(1);
	}

	IntBuffer triVertexCount() {
		return triVertexCount;
	}

	PointerBuffer indexBaseByteAddress() {
		return indexBaseByteAddress;
	}

	public void bind() {
		assert !isClosed;

		if (vaoBufferId == 0) {
			vaoBufferId = GFX.genVertexArray();
			GFX.bindVertexArray(vaoBufferId);

			slab.bind();
			indexSlab.bind();
			TerrainFormat.TERRAIN_MATERIAL.enableAttributes();
			TerrainFormat.TERRAIN_MATERIAL.bindAttributeLocations(0);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
		} else {
			GFX.bindVertexArray(vaoBufferId);
		}
	}

	void release() {
		assert !isClosed;
		isClosed = true;

		if (vaoBufferId != 0) {
			GFX.deleteVertexArray(vaoBufferId);
			vaoBufferId = 0;
		}

		MemoryUtil.memFree(triVertexCount);
		triVertexCount = null;

		MemoryUtil.memFree(indexBaseByteAddress);
		indexBaseByteAddress = null;
	}
}
