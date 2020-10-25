/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.buffer.encoding;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.light.LightmapHd;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;

import static grondag.canvas.buffer.encoding.EncoderUtils.applyBlockLighting;
import static grondag.canvas.buffer.encoding.EncoderUtils.colorizeQuad;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

public abstract class HdEncoders {
	private static final int QUAD_STRIDE = MaterialVertexFormats.HD_TERRAIN.vertexStrideInts * 4;

	public static final VertexEncoder HD_TERRAIN_1 = new HdTerrainEncoder() {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			// needs to happen before offsets are applied
			applyBlockLighting(quad, context);
			colorizeQuad(quad, context);
			bufferQuadHd1(quad, context);
		}
	};

	static void bufferQuadHd1(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4fExt matrix = (Matrix4fExt) (Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final float[] aoData = quad.ao;
		final RenderMaterialImpl mat = quad.material();
		final VertexCollectorImpl buff0 = context.collectors.get(mat);
		final int[] appendData = context.appendData;

		final LightmapHd hdLight = quad.hdLight;

		assert mat.blendMode() != BlendMode.DEFAULT;

		final int shaderFlags = mat.shaderFlags << 16;

		int packedNormal = 0;
		int transformedNormal = 0;
		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			packedNormal = quad.packedFaceNormal();
			transformedNormal = normalMatrix.canvas_transform(packedNormal);
		}

		int k = 0;

		for (int i = 0; i < 4; i++) {
			quad.transformAndAppend(i, matrix, appendData, k);
			k += 3;

			appendData[k++] = quad.spriteColor(i, 0);

			appendData[k++] = Float.floatToRawIntBits(quad.spriteU(i, 0));
			appendData[k++] = Float.floatToRawIntBits(quad.spriteV(i, 0));

			final int packedLight = quad.lightmap(i);
			final int blockLight = (packedLight & 0xFF);
			final int skyLight = ((packedLight >> 16) & 0xFF);
			appendData[k++] = blockLight | (skyLight << 8) | shaderFlags;
			appendData[k++] = hdLight == null ? -1 : hdLight.coord(quad, i);

			if (useNormals) {
				final int p = quad.packedNormal(i);

				if (p != packedNormal) {
					packedNormal = p;
					transformedNormal = normalMatrix.canvas_transform(packedNormal);
				}
			}

			final int ao = aoData == null ? 0xFF000000 : ((Math.round(aoData[i] * 254) - 127) << 24);
			appendData[k++] = transformedNormal | ao;
		}

		assert k == QUAD_STRIDE;

		buff0.add(appendData, k);
	}

	abstract static class HdTerrainEncoder extends VertexEncoder {
		HdTerrainEncoder() {
			super();
		}
	}
}
