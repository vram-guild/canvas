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

import grondag.canvas.render.terrain.TerrainFormat;
import grondag.canvas.render.terrain.cluster.Slab;
import grondag.canvas.varia.GFX;

public abstract class AbstractVaoBinding {
	public final Slab slab;
	public final int baseQuadVertexIndex;
	private int vaoBufferId = 0;
	boolean isClosed = false;

	protected AbstractVaoBinding(Slab slab, int baseQuadVertexIndex) {
		this.slab = slab;
		this.baseQuadVertexIndex = baseQuadVertexIndex;
	}

	public final void bind() {
		assert !isClosed;

		if (vaoBufferId == 0) {
			vaoBufferId = GFX.genVertexArray();
			GFX.bindVertexArray(vaoBufferId);
			final long offset = baseQuadVertexIndex * TerrainFormat.TERRAIN_MATERIAL.vertexStrideBytes;
			slab.bind();
			TerrainFormat.TERRAIN_MATERIAL.enableAttributes();
			TerrainFormat.TERRAIN_MATERIAL.bindAttributeLocations(offset);
			indexSlab().bind();
		} else {
			GFX.bindVertexArray(vaoBufferId);
		}
	}

	public final void release() {
		assert !isClosed;
		isClosed = true;

		if (vaoBufferId != 0) {
			GFX.deleteVertexArray(vaoBufferId);
			vaoBufferId = 0;
		}
		
		onRelease();
	}
	
	protected abstract void onRelease();
	
	protected abstract IndexSlab indexSlab();
}
