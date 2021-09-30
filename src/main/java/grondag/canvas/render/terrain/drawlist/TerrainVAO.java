/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
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
