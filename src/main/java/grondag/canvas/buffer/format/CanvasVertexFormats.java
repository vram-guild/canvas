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

import static grondag.canvas.buffer.format.CanvasVertexFormatElement.BASE_RGBA_4UB;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.BASE_TEX_2F;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.BASE_TEX_2US;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.LIGHTMAPS_2UB;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.LIGHTMAPS_4UB;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.MATERIAL_1US;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.NORMAL_PLUS_4UB;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.POSITION_3F;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.SPRITE_1US;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.encoding.QuadEncoder;
import grondag.canvas.buffer.encoding.QuadTranscoder;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;

@SuppressWarnings("unused")
public final class CanvasVertexFormats {
	static {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: MaterialVertexFormats static init");
		}
	}

	public static final CanvasVertexFormat PROCESS_VERTEX_UV = new CanvasVertexFormat(POSITION_3F, BASE_TEX_2F);
	public static final CanvasVertexFormat PROCESS_VERTEX = new CanvasVertexFormat(POSITION_3F);

	/**
	 * New common format for all world/game object rendering.
	 *
	 * <p>Texture is always normalized. For atlas textures, sprite ID is
	 * carried in most significant bytes of normal.
	 * Most significant byte of lightmap holds vertex state flags.
	 */
	private static final CanvasVertexFormat FAT_MATERIAL = new CanvasVertexFormat(POSITION_3F, BASE_RGBA_4UB, BASE_TEX_2US, SPRITE_1US, MATERIAL_1US, LIGHTMAPS_4UB, NORMAL_PLUS_4UB);

	private static final int FAT_QUAD_STRIDE = FAT_MATERIAL.quadStrideInts;

	private static final QuadEncoder FAT_ENCODER = (quad, buff) -> {
		final RenderMaterialImpl mat = quad.material();

		assert mat.blendMode != BlendMode.DEFAULT;

		final int shaderFlags = mat.shaderFlags << 24;

		int packedNormal = 0;
		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			packedNormal = quad.packedFaceNormal();
		}

		final int spriteIdCoord = quad.spriteId() | (mat.index << 16);

		int k = buff.allocate(FAT_QUAD_STRIDE);
		final int[] target = buff.data();

		for (int i = 0; i < 4; i++) {
			quad.appendVertex(i, target, k);
			k += 3;

			target[k++] = quad.vertexColor(i);
			target[k++] = quad.spriteBufferU(i) | (quad.spriteBufferV(i) << 16);
			target[k++] = spriteIdCoord;

			final int packedLight = quad.lightmap(i);
			final int blockLight = (packedLight & 0xFF);
			final int skyLight = ((packedLight >> 16) & 0xFF);
			target[k++] = blockLight | (skyLight << 8) | 0xFF0000;

			if (useNormals) {
				packedNormal = quad.packedNormal(i);
			}

			target[k++] = packedNormal | shaderFlags;
		}
	};

	private static final QuadTranscoder FAT_TRANSCODER = (quad, context, buff) -> {
		final Matrix4fExt matrix = (Matrix4fExt) (Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final float[] aoData = quad.ao;
		final RenderMaterialImpl mat = quad.material();

		assert mat.blendMode != BlendMode.DEFAULT;

		final int shaderFlags = mat.shaderFlags << 24;

		int packedNormal = 0;
		int transformedNormal = 0;
		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			packedNormal = quad.packedFaceNormal();
			transformedNormal = normalMatrix.canvas_transform(packedNormal);
		}

		final int spriteIdCoord = quad.spriteId() | (mat.index << 16);

		int k = buff.allocate(FAT_QUAD_STRIDE);
		final int[] target = buff.data();

		for (int i = 0; i < 4; i++) {
			quad.transformAndAppendVertex(i, matrix, target, k);
			k += 3;

			target[k++] = quad.vertexColor(i);
			target[k++] = quad.spriteBufferU(i) | (quad.spriteBufferV(i) << 16);
			target[k++] = spriteIdCoord;

			final int packedLight = quad.lightmap(i);
			final int blockLight = (packedLight & 0xFF);
			final int skyLight = ((packedLight >> 16) & 0xFF);
			final int ao = aoData == null ? 255 : (Math.round(aoData[i] * 255));
			target[k++] = blockLight | (skyLight << 8) | (ao << 16);

			if (useNormals) {
				final int p = quad.packedNormal(i);

				if (p != packedNormal) {
					packedNormal = p;
					transformedNormal = normalMatrix.canvas_transform(packedNormal);
				}
			}

			target[k++] = transformedNormal | shaderFlags;
		}
	};

	/**
	 * WIP2: Compact format for all world/game object rendering.
	 *
	 * <p>Texture is always normalized.
	 *
	 * <p>Two-byte material ID conveys sprite, condition, program IDs
	 * and vertex state flags.  AO is carried in last octet of normal.
	 */
	private static final CanvasVertexFormat COMPACT_MATERIAL = new CanvasVertexFormat(POSITION_3F, BASE_RGBA_4UB, BASE_TEX_2US, MATERIAL_1US, LIGHTMAPS_2UB, NORMAL_PLUS_4UB);

	private static final int COMPACT_QUAD_STRIDE = COMPACT_MATERIAL.quadStrideInts;

	private static final QuadEncoder COMPACT_ENCODER = (quad, buff) -> {
		final RenderMaterialImpl mat = quad.material();

		assert mat.blendMode != BlendMode.DEFAULT;

		final int shaderFlags = mat.shaderFlags << 24;

		int packedNormal = 0;
		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			packedNormal = quad.packedFaceNormal();
		}

		final int spriteIdCoord = quad.spriteId() | (mat.index << 16);

		int k = buff.allocate(COMPACT_QUAD_STRIDE);
		final int[] target = buff.data();

		for (int i = 0; i < 4; i++) {
			quad.appendVertex(i, target, k);
			k += 3;

			target[k++] = quad.vertexColor(i);
			target[k++] = quad.spriteBufferU(i) | (quad.spriteBufferV(i) << 16);
			target[k++] = spriteIdCoord;

			final int packedLight = quad.lightmap(i);
			final int blockLight = (packedLight & 0xFF);
			final int skyLight = ((packedLight >> 16) & 0xFF);
			target[k++] = blockLight | (skyLight << 8) | 0xFF0000;

			if (useNormals) {
				packedNormal = quad.packedNormal(i);
			}

			target[k++] = packedNormal | shaderFlags;
		}
	};

	private static final QuadTranscoder COMPACT_TRANSCODER = (quad, context, buff) -> {
		final Matrix4fExt matrix = (Matrix4fExt) (Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final float[] aoData = quad.ao;
		final RenderMaterialImpl mat = quad.material();

		assert mat.blendMode != BlendMode.DEFAULT;

		final int shaderFlags = mat.shaderFlags << 24;

		int packedNormal = 0;
		int transformedNormal = 0;
		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			packedNormal = quad.packedFaceNormal();
			transformedNormal = normalMatrix.canvas_transform(packedNormal);
		}

		final int spriteIdCoord = quad.spriteId() | (mat.index << 16);

		int k = buff.allocate(COMPACT_QUAD_STRIDE);
		final int[] target = buff.data();

		for (int i = 0; i < 4; i++) {
			quad.transformAndAppendVertex(i, matrix, target, k);
			k += 3;

			target[k++] = quad.vertexColor(i);
			target[k++] = quad.spriteBufferU(i) | (quad.spriteBufferV(i) << 16);
			target[k++] = spriteIdCoord;

			final int packedLight = quad.lightmap(i);
			final int blockLight = (packedLight & 0xFF);
			final int skyLight = ((packedLight >> 16) & 0xFF);
			final int ao = aoData == null ? 255 : (Math.round(aoData[i] * 255));
			target[k++] = blockLight | (skyLight << 8) | (ao << 16);

			if (useNormals) {
				final int p = quad.packedNormal(i);

				if (p != packedNormal) {
					packedNormal = p;
					transformedNormal = normalMatrix.canvas_transform(packedNormal);
				}
			}

			target[k++] = transformedNormal | shaderFlags;
		}
	};

	// WIP2: new UNLIT format - no lightmaps on vertex
	// Position 3F
	// Color    4UB
	// Texture	2US
	// Mat/normal 2US
	// 6 words / 24 bytes

	public static CanvasVertexFormat MATERIAL_FORMAT = FAT_MATERIAL;
	public static final int MATERIAL_INT_VERTEX_STRIDE = MATERIAL_FORMAT.vertexStrideInts;
	public static final int MATERIAL_INT_QUAD_STRIDE = MATERIAL_FORMAT.quadStrideInts;
	public static final QuadTranscoder MATERIAL_TRANSCODER = FAT_TRANSCODER;
	public static final QuadEncoder MATERIAL_ENCODER = FAT_ENCODER;
}
