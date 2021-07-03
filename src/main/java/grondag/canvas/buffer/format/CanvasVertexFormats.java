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

import static grondag.canvas.buffer.format.CanvasVertexFormatElement.AO_1UB;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.BASE_RGBA_4UB;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.BASE_RGBA_VF;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.BASE_TEX_2F;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.BASE_TEX_2US;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.LIGHTMAPS_2UB;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.MATERIAL_1US;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.NORMAL_3B;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.POSITION_3F;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.encoding.QuadEncoder;
import grondag.canvas.buffer.encoding.QuadTranscoder;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.vf.VfColor;

public final class CanvasVertexFormats {
	static {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: MaterialVertexFormats static init");
		}
	}

	public static final CanvasVertexFormat PROCESS_VERTEX_UV = new CanvasVertexFormat(POSITION_3F, BASE_TEX_2F);
	public static final CanvasVertexFormat PROCESS_VERTEX = new CanvasVertexFormat(POSITION_3F);

	/**
	 * Compact format for all world/game object rendering.
	 *
	 * <p>Texture is always normalized.
	 *
	 * <p>Two-byte material ID conveys sprite, condition, program IDs
	 * and vertex state flags.  AO is carried in last octet of normal.
	 */
	private static final CanvasVertexFormat COMPACT_MATERIAL = new CanvasVertexFormat(POSITION_3F, BASE_RGBA_4UB, BASE_TEX_2US, LIGHTMAPS_2UB, MATERIAL_1US, NORMAL_3B, AO_1UB);

	private static final CanvasVertexFormat COMPACT_MATERIAL_VF = new CanvasVertexFormat(POSITION_3F, BASE_RGBA_VF, BASE_TEX_2US, LIGHTMAPS_2UB, MATERIAL_1US, NORMAL_3B, AO_1UB);

	private static final int COMPACT_QUAD_STRIDE = COMPACT_MATERIAL.quadStrideInts;

	private static final QuadEncoder COMPACT_ENCODER = (quad, buff) -> {
		final RenderMaterialImpl mat = quad.material();

		int packedNormal = 0;
		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			packedNormal = quad.packedFaceNormal();
		}

		final int material = mat.dongle().index(quad.spriteId()) << 16;

		int k = buff.allocate(COMPACT_QUAD_STRIDE);
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

	private static final QuadTranscoder COMPACT_TRANSCODER = (quad, context, buff) -> {
		final Matrix4fExt matrix = (Matrix4fExt) (Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final int overlay = context.overlay();

		quad.overlay(overlay);

		final float[] aoData = quad.ao;
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

		int k = buff.allocate(COMPACT_QUAD_STRIDE);
		final int[] target = buff.data();

		for (int i = 0; i < 4; i++) {
			quad.transformAndAppendVertex(i, matrix, target, k);
			k += 3;

			target[k++] = quad.vertexColor(i);
			target[k++] = quad.spriteBufferU(i) | (quad.spriteBufferV(i) << 16);

			final int packedLight = quad.lightmap(i);
			final int blockLight = (packedLight & 0xFF);
			final int skyLight = ((packedLight >> 16) & 0xFF);
			final int ao = aoData == null ? 255 : (Math.round(aoData[i] * 255));
			target[k++] = blockLight | (skyLight << 8) | material;

			if (useNormals) {
				final int p = quad.packedNormal(i);

				if (p != packedNormal) {
					packedNormal = p;
					transformedNormal = normalMatrix.canvas_transform(packedNormal);
				}
			}

			target[k++] = transformedNormal | (ao << 24);
		}
	};

	private static final QuadTranscoder VF_TRANSCODER = (quad, context, buff) -> {
		final Matrix4fExt matrix = (Matrix4fExt) (Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final int overlay = context.overlay();

		quad.overlay(overlay);

		final float[] aoData = quad.ao;
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

		int k = buff.allocate(COMPACT_QUAD_STRIDE);
		final int[] target = buff.data();

		final int vfColor = VfColor.INSTANCE.index(quad.vertexColor(0), quad.vertexColor(1), quad.vertexColor(2), quad.vertexColor(3)) << 2;

		for (int i = 0; i < 4; i++) {
			quad.transformAndAppendVertex(i, matrix, target, k);
			k += 3;

			target[k++] = vfColor | i;
			target[k++] = quad.spriteBufferU(i) | (quad.spriteBufferV(i) << 16);

			final int packedLight = quad.lightmap(i);
			final int blockLight = (packedLight & 0xFF);
			final int skyLight = ((packedLight >> 16) & 0xFF);
			final int ao = aoData == null ? 255 : (Math.round(aoData[i] * 255));
			target[k++] = blockLight | (skyLight << 8) | material;

			if (useNormals) {
				final int p = quad.packedNormal(i);

				if (p != packedNormal) {
					packedNormal = p;
					transformedNormal = normalMatrix.canvas_transform(packedNormal);
				}
			}

			target[k++] = transformedNormal | (ao << 24);
		}
	};

	public static CanvasVertexFormat MATERIAL_FORMAT = COMPACT_MATERIAL;
	public static CanvasVertexFormat MATERIAL_FORMAT_VF = COMPACT_MATERIAL_VF;
	public static final int MATERIAL_INT_VERTEX_STRIDE = MATERIAL_FORMAT.vertexStrideInts;
	public static final int MATERIAL_INT_QUAD_STRIDE = MATERIAL_FORMAT.quadStrideInts;
	public static QuadTranscoder MATERIAL_TRANSCODER = COMPACT_TRANSCODER;
	public static QuadEncoder MATERIAL_ENCODER = COMPACT_ENCODER;
	public static QuadTranscoder TERRAIN_TRANSCODER = Configurator.vf ? VF_TRANSCODER : COMPACT_TRANSCODER;
}
