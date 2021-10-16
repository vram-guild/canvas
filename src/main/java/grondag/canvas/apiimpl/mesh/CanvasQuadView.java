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

package grondag.canvas.apiimpl.mesh;

import io.vram.frex.api.buffer.QuadEmitter;
import io.vram.frex.base.renderer.mesh.MeshEncodingHelper;

import grondag.canvas.material.state.CanvasRenderMaterial;

public class CanvasQuadView extends BaseQuadView<CanvasRenderMaterial> {
	@Override
	public void copyTo(QuadEmitter target) {
		// force geometry compute
		computeGeometry();
		// force tangent compute
		this.packedFaceTanget();

		final QuadEditorImpl quad = (QuadEditorImpl) target;

		// copy everything except the material
		System.arraycopy(data, baseIndex, quad.data, quad.baseIndex, MeshEncodingHelper.TOTAL_MESH_QUAD_STRIDE);
		quad.isSpriteInterpolated = isSpriteInterpolated;
		quad.nominalFaceId = nominalFaceId;
		quad.isGeometryInvalid = false;
		quad.isTangentInvalid = false;
	}
}
