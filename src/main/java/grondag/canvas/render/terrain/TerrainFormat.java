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

package grondag.canvas.render.terrain;

import static grondag.canvas.buffer.format.CanvasVertexFormats.AO_1UB;
import static grondag.canvas.buffer.format.CanvasVertexFormats.BASE_RGBA_4UB;
import static grondag.canvas.buffer.format.CanvasVertexFormats.BASE_TEX_2US;
import static grondag.canvas.buffer.format.CanvasVertexFormats.LIGHTMAPS_2UB;
import static grondag.canvas.buffer.format.CanvasVertexFormats.MATERIAL_1US;
import static grondag.canvas.buffer.format.CanvasVertexFormats.NORMAL_3B;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexFormatElement;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.format.CanvasVertexFormatElement;
import grondag.canvas.buffer.format.QuadTranscoder;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;

public class TerrainFormat {
	private TerrainFormat() { }

	private static final CanvasVertexFormatElement REGION = new CanvasVertexFormatElement(VertexFormatElement.DataType.USHORT, 4, "in_region", false, true);
	private static final CanvasVertexFormatElement BLOCK_POS = new CanvasVertexFormatElement(VertexFormatElement.DataType.UBYTE, 4, "in_blockpos", false, true);

	// Would be nice to make this smaller but with less precision in position we start
	// to see Z-fighting on iron bars, fire, etc. Iron bars require a resolution of 1/16000.
	// Reducing resolution of UV is problematic for multi-block textures.
	public static final CanvasVertexFormat TERRAIN_MATERIAL = new CanvasVertexFormat(
			REGION,
			BLOCK_POS,
			BASE_RGBA_4UB, BASE_TEX_2US, LIGHTMAPS_2UB, MATERIAL_1US, NORMAL_3B, AO_1UB);

	static final int TERRAIN_QUAD_STRIDE = TERRAIN_MATERIAL.quadStrideInts;

	public static final QuadTranscoder TERRAIN_TRANSCODER = (quad, context, buff) -> {
		final Matrix4fExt matrix = (Matrix4fExt) (Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final int overlay = context.overlay();

		quad.overlay(overlay);

		final boolean aoDisabled = !MinecraftClient.isAmbientOcclusionEnabled();
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

		int k = buff.allocate(TERRAIN_QUAD_STRIDE);
		final int[] target = buff.data();

		// This and pos vertex encoding are the only differences from standard format
		final int sectorId = context.sectorId;
		assert sectorId >= 0;
		final int sectorRelativeRegionOrigin = context.sectorRelativeRegionOrigin;

		for (int i = 0; i < 4; i++) {
			quad.transformAndAppendRegionVertex(i, matrix, target, k, sectorId, sectorRelativeRegionOrigin);
			k += 3;

			target[k++] = quad.vertexColor(i);
			target[k++] = quad.spriteBufferU(i) | (quad.spriteBufferV(i) << 16);

			final int packedLight = quad.lightmap(i);
			final int blockLight = (packedLight & 0xFF);
			final int skyLight = ((packedLight >> 16) & 0xFF);
			final int ao = aoDisabled ? 255 : (Math.round(aoData[i] * 255));
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
}
