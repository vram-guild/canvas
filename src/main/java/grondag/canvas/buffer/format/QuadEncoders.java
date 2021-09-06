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

package grondag.canvas.buffer.format;

import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_FIRST_VERTEX_TANGENT;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.MESH_VERTEX_STRIDE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.UV_EXTRA_PRECISION;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.UV_ROUNDING_BIT;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_COLOR;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_LIGHTMAP;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_NORMAL;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_U;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_V;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_X;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_Y;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_Z;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbsentEncodingContext;
import grondag.canvas.buffer.input.VertexCollector;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.perf.MicroTimer;

public class QuadEncoders {
	private static void encodeQuad(MutableQuadViewImpl quad, EncodingContext context, VertexCollector buff) {
		final Matrix4fExt matrix = (Matrix4fExt) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final boolean isContextPresent = context != AbsentEncodingContext.INSTANCE;

		if (isContextPresent) {
			quad.overlay(context.overlay());
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

			final float xOut = matrix.a00() * x + matrix.a01() * y + matrix.a02() * z + matrix.a03();
			final float yOut = matrix.a10() * x + matrix.a11() * y + matrix.a12() * z + matrix.a13();
			final float zOut = matrix.a20() * x + matrix.a21() * y + matrix.a22() * z + matrix.a23();

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

	//public static final QuadEncoder STANDARD_ENCODER = QuadEncoders::encodeQuad;

	//WIP: remove
	private static final MicroTimer TIMER = new MicroTimer("quad encoding", 2000000);

	public static final QuadEncoder STANDARD_ENCODER = (quad, context, buff) -> {
		if (RenderSystem.isOnRenderThread()) {
			TIMER.start();
			encodeQuad(quad, context, buff);
			TIMER.stop();
		} else {
			encodeQuad(quad, context, buff);
		}
	};
}
