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
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.BASE_TEX_VF;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.HEADER_VF;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.LIGHTMAPS_2UB;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.MATERIAL_1US;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.NORMAL_3B;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.POSITION_3F;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.VERTEX_VF;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.buffer.encoding.QuadEncoder;
import grondag.canvas.buffer.encoding.QuadTranscoder;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.vf.Vf;

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

	// WIP: remove at end
	private static final CanvasVertexFormat VF_MATERIAL = new CanvasVertexFormat(HEADER_VF, VERTEX_VF, BASE_RGBA_VF, BASE_TEX_VF);

	private static final int COMPACT_QUAD_STRIDE = COMPACT_MATERIAL.quadStrideInts;
	private static final int VF_QUAD_STRIDE = VF_MATERIAL.quadStrideInts;

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

		final int material = mat.dongle().index(quad.spriteId()) << 12;

		int k = buff.allocate(VF_QUAD_STRIDE);
		final int[] target = buff.data();

		final int[] quadTarget = ((ArrayVertexCollector) buff).vfTestHackData;
		int n = k / 4;

		final int vfColor = Vf.COLOR.index(quad.vertexColor(0), quad.vertexColor(1), quad.vertexColor(2), quad.vertexColor(3));

		final int vfUv = Vf.UV.index(
				quad.spriteBufferU(0) | (quad.spriteBufferV(0) << 16),
				quad.spriteBufferU(1) | (quad.spriteBufferV(1) << 16),
				quad.spriteBufferU(2) | (quad.spriteBufferV(2) << 16),
				quad.spriteBufferU(3) | (quad.spriteBufferV(3) << 16)
		);

		final int vfVertex = Vf.VERTEX.index(matrix, normalMatrix, quad);

		final int packedLight0 = quad.lightmap(0);
		final int light0 = (packedLight0 & 0xFF) | ((packedLight0 >> 8) & 0xFF00) | ((aoData == null ? 255 : (Math.round(aoData[0] * 255))) << 16);

		final int packedLight1 = quad.lightmap(1);
		final int light1 = (packedLight1 & 0xFF) | ((packedLight1 >> 8) & 0xFF00) | ((aoData == null ? 255 : (Math.round(aoData[1] * 255))) << 16);

		final int packedLight2 = quad.lightmap(2);
		final int light2 = (packedLight2 & 0xFF) | ((packedLight2 >> 8) & 0xFF00) | ((aoData == null ? 255 : (Math.round(aoData[2] * 255))) << 16);

		final int packedLight3 = quad.lightmap(3);
		final int light3 = (packedLight3 & 0xFF) | ((packedLight3 >> 8) & 0xFF00) | ((aoData == null ? 255 : (Math.round(aoData[3] * 255))) << 16);

		final int vfLight = Vf.LIGHT.index(light0, light1, light2, light3);

		final int l0 = (vfLight & 0x0000FF) << 24;
		final int l1 = (vfLight & 0x00FF00) << 16;
		final int l2 = (vfLight & 0xFF0000) << 8;

		assert vfVertex < 0x1000000;
		assert vfColor < 0x1000000;
		assert vfUv < 0x1000000;
		assert vfLight < 0x1000000;

		final int packedRelPos = context.packedRelativeBlockPos();

		// Light is striped across the last three components so we can stay within four int components.
		// This is also convenient if we ever want to skip sending light.
		quadTarget[n++] = material | packedRelPos;
		quadTarget[n++] = vfVertex | l0;
		quadTarget[n++] = vfColor | l1;
		quadTarget[n++] = vfUv | l2;

		for (int i = 0; i < 4; i++) {
			target[k++] = material | packedRelPos | (i << 28);
			target[k++] = vfVertex | l0;
			target[k++] = vfColor | l1;
			target[k++] = vfUv | l2;
		}
	};

	public static CanvasVertexFormat MATERIAL_FORMAT = COMPACT_MATERIAL;
	public static CanvasVertexFormat MATERIAL_FORMAT_VF = VF_MATERIAL;
	public static QuadTranscoder MATERIAL_TRANSCODER = COMPACT_TRANSCODER;
	public static QuadEncoder MATERIAL_ENCODER = COMPACT_ENCODER;
	public static QuadTranscoder TERRAIN_TRANSCODER = Configurator.vf ? VF_TRANSCODER : COMPACT_TRANSCODER;
}
