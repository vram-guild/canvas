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

import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.BASE_VERTEX_STRIDE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_COLOR;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_LIGHTMAP;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_U;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_V;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_X;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_Y;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_Z;
import static grondag.canvas.apiimpl.mesh.QuadViewImpl.roundSpriteData;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;

public class QuadEncoders {
	public static final QuadEncoder STANDARD_ENCODER = (quad, buff) -> {
		final RenderMaterialImpl mat = quad.material();

		// bit 16 is set if normal Z component is negative
		final int normalFlagBits = 0;
		int packedNormal = 0;
		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			packedNormal = quad.packedFaceNormal();
		}

		final int material = mat.dongle().index(quad.spriteId()) << 16;

		final int baseTargetIndex = buff.allocate(CanvasVertexFormats.STANDARD_QUAD_STRIDE);
		final int[] target = buff.data();
		final int baseSourceIndex = quad.vertexStart();
		final int[] source = quad.data();

		for (int i = 0; i < 4; i++) {
			final int fromIndex = baseSourceIndex + i * BASE_VERTEX_STRIDE;
			final int toIndex = baseTargetIndex + i * CanvasVertexFormats.STANDARD_VERTEX_STRIDE;

			target[toIndex] = source[fromIndex + VERTEX_X];
			target[toIndex + 1] = source[fromIndex + VERTEX_Y];
			target[toIndex + 2] = source[fromIndex + VERTEX_Z];

			target[toIndex + 3] = source[fromIndex + VERTEX_COLOR];
			target[toIndex + 4] = roundSpriteData(source[fromIndex + VERTEX_U]) | (roundSpriteData(source[fromIndex + VERTEX_V]) << 16);

			final int packedLight = source[fromIndex + VERTEX_LIGHTMAP];
			final int blockLight = (packedLight & 0xFF);
			final int skyLight = ((packedLight >> 16) & 0xFF);
			target[toIndex + 5] = blockLight | (skyLight << 8) | material;

			if (useNormals) {
				packedNormal = quad.packedNormal(i);
			}

			target[toIndex + 6] = packedNormal;
		}
	};

	public static final QuadTranscoder STANDARD_TRANSCODER = (quad, context, buff) -> {
		final Matrix4fExt matrix = (Matrix4fExt) (Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final int overlay = context.overlay();

		quad.overlay(overlay);

		final RenderMaterialImpl mat = quad.material();

		assert mat.blendMode != BlendMode.DEFAULT;

		int packedNormal = 0;
		int transformedNormal = 0;
		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			packedNormal = quad.packedFaceNormal();
			transformedNormal = normalMatrix.canvas_transform(packedNormal);
		}

		final int material = mat.dongle().index(quad.spriteId()) << 16;

		int k = buff.allocate(CanvasVertexFormats.STANDARD_QUAD_STRIDE);
		final int[] target = buff.data();

		for (int i = 0; i < 4; i++) {
			quad.transformAndAppendVertex(i, matrix, target, k);
			k += 3;

			target[k++] = quad.vertexColor(i);
			target[k++] = quad.spriteBufferU(i) | (quad.spriteBufferV(i) << 16);

			final int packedLight = quad.lightmap(i);
			final int blockLight = (packedLight & 0xFF);
			final int skyLight = ((packedLight >> 16) & 0xFF);
			target[k++] = blockLight | (skyLight << 8) | material;

			if (useNormals) {
				final int p = quad.packedNormal(i);

				if (p != packedNormal) {
					packedNormal = p;
					transformedNormal = normalMatrix.canvas_transform(packedNormal);
				}
			}

			target[k++] = transformedNormal | 0xFF000000;
		}
	};
}
