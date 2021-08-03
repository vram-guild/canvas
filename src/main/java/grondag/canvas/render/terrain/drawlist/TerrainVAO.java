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

package grondag.canvas.render.terrain.drawlist;

import java.util.function.IntSupplier;

import grondag.canvas.buffer.format.BufferVAO;
import grondag.canvas.render.terrain.TerrainFormat;

public class TerrainVAO extends BufferVAO {
	public final int baseQuadVertexIndex;

	public TerrainVAO(IntSupplier arrayIdSupplier, IntSupplier elementIdSupplier, int baseQuadVertexIndex) {
		super(TerrainFormat.TERRAIN_MATERIAL, arrayIdSupplier, elementIdSupplier);
		this.baseQuadVertexIndex = baseQuadVertexIndex;
	}

	@Override
	public void bind() {
		super.bind(baseQuadVertexIndex * TerrainFormat.TERRAIN_MATERIAL.vertexStrideBytes);
	}
}
