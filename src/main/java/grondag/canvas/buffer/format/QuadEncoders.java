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

package grondag.canvas.buffer.format;

import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_FIRST_VERTEX_TANGENT;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.MESH_VERTEX_STRIDE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.UV_EXTRA_PRECISION;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.UV_ROUNDING_BIT;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_COLOR;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_LIGHTMAP;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_NORMAL;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_U;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_V;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_X;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_Y;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_Z;

import net.minecraft.client.renderer.texture.OverlayTexture;

import grondag.canvas.apiimpl.mesh.QuadEditorImpl;
import grondag.canvas.apiimpl.rendercontext.AbsentEncodingContext;
import grondag.canvas.buffer.input.VertexCollector;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;

public class QuadEncoders {
	private static void encodeQuad(QuadEditorImpl quad, EncodingContext context, VertexCollector buff) {
		final Matrix4fExt matrix = (Matrix4fExt) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final boolean isContextPresent = context != AbsentEncodingContext.INSTANCE;
		final int overlay = context.overlay();

		if (overlay != OverlayTexture.NO_OVERLAY) {
			quad.overlayCoords(overlay);
		}

		final RenderMaterialImpl mat = quad.material();

		final int quadNormalFlags = quad.normalFlags();
		// don't retrieve if won't be used
		final int faceNormal = quadNormalFlags == 0b1111 ? 0 : quad.packedFaceNormal();
		// bit 1 is set if normal Z component is negative
		int normalFlagBits = 0;
		int packedNormal = 0;
		int transformedNormal = 0;

		final int quadTangetFlags = quad.tangentFlags();
		final int faceTangent = quadTangetFlags == 0b1111 ? 0 : quad.packedFaceTanget();
		// bit 1 is set if tangent Z component is negative
		int tangentFlagBits = 0;
		int packedTangent = 0;
		int transformedTangent = 0;

		final int material = mat.dongle().index(quad.spriteId()) << 16;

		final int baseTargetIndex = buff.allocate(CanvasVertexFormats.STANDARD_QUAD_STRIDE);
		final int[] target = buff.data();
		final int baseSourceIndex = quad.vertexStart();
		final int[] source = quad.data();

		for (int i = 0; i < 4; i++) {
			final int vertexMask = 1 << i;
			final int fromIndex = baseSourceIndex + i * MESH_VERTEX_STRIDE;
			final int toIndex = baseTargetIndex + i * CanvasVertexFormats.STANDARD_VERTEX_STRIDE;

			final int p = ((quadNormalFlags & vertexMask) == 0) ? faceNormal : source[fromIndex + VERTEX_NORMAL];

			if (p != packedNormal) {
				packedNormal = p;
				transformedNormal = (isContextPresent ? normalMatrix.canvas_transform(packedNormal) : packedNormal) & 0xFFFF;
				normalFlagBits = (transformedNormal >>> 23) & 1;
			}

			final int t = ((quadTangetFlags & vertexMask) == 0) ? faceTangent : source[baseSourceIndex + i + HEADER_FIRST_VERTEX_TANGENT];

			if (t != packedTangent) {
				packedTangent = t;
				transformedTangent = ((isContextPresent ? normalMatrix.canvas_transform(packedTangent) : packedTangent) & 0xFFFF) << 16;
				tangentFlagBits = (transformedTangent >>> 23) & 1;
			}

			final float x = Float.intBitsToFloat(source[fromIndex + VERTEX_X]);
			final float y = Float.intBitsToFloat(source[fromIndex + VERTEX_Y]);
			final float z = Float.intBitsToFloat(source[fromIndex + VERTEX_Z]);

			final float xOut = matrix.m00() * x + matrix.m01() * y + matrix.m02() * z + matrix.m03();
			final float yOut = matrix.m10() * x + matrix.m11() * y + matrix.m12() * z + matrix.m13();
			final float zOut = matrix.m20() * x + matrix.m21() * y + matrix.m22() * z + matrix.m23();

			target[toIndex] = Float.floatToRawIntBits(xOut);
			target[toIndex + 1] = Float.floatToRawIntBits(yOut);
			target[toIndex + 2] = Float.floatToRawIntBits(zOut);

			target[toIndex + 3] = source[fromIndex + VERTEX_COLOR];

			target[toIndex + 4] = (source[fromIndex + VERTEX_U] + UV_ROUNDING_BIT) >> UV_EXTRA_PRECISION
				| ((source[fromIndex + VERTEX_V] + UV_ROUNDING_BIT) >> UV_EXTRA_PRECISION << 16);

			final int packedLight = source[fromIndex + VERTEX_LIGHTMAP];
			final int blockLight = (packedLight & 0xFE) | normalFlagBits;
			final int skyLight = ((packedLight >> 16) & 0xFE) | tangentFlagBits;
			target[toIndex + 5] = blockLight | (skyLight << 8) | material;

			target[toIndex + 6] = transformedNormal | transformedTangent;
		}
	}

	public static final QuadEncoder STANDARD_ENCODER = QuadEncoders::encodeQuad;
}
