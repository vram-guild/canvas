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

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;

public class QuadEncoders {
	public static final QuadEncoder STANDARD_ENCODER = (quad, buff) -> {
		final RenderMaterialImpl mat = quad.material();

		int packedNormal = 0;
		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			packedNormal = quad.packedFaceNormal();
		}

		final int material = mat.dongle().index(quad.spriteId()) << 16;

		int k = buff.allocate(CanvasVertexFormats.STANDARD_QUAD_STRIDE);
		final int[] target = buff.data();

		for (int i = 0; i < 4; i++) {
			quad.appendVertex(i, target, k);
			k += 3;

			target[k++] = quad.vertexColor(i);
			target[k++] = quad.spriteBufferU(i) | (quad.spriteBufferV(i) << 16);

			final int packedLight = quad.lightmap(i);
			final int blockLight = (packedLight & 0xFF);
			final int skyLight = ((packedLight >> 16) & 0xFF);
			target[k++] = blockLight | (skyLight << 8) | material;

			if (useNormals) {
				packedNormal = quad.packedNormal(i);
			}

			target[k++] = packedNormal | 0xFF000000;
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
