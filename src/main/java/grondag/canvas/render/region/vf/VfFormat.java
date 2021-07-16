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

package grondag.canvas.render.region.vf;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexFormatElement;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.format.CanvasVertexFormatElement;
import grondag.canvas.buffer.format.QuadTranscoder;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.vf.TerrainVertexFetch;

public class VfFormat {
	private VfFormat() { }

	private static final CanvasVertexFormatElement HEADER_VF = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.UINT, 1, "in_header_vf", false, true);
	private static final CanvasVertexFormatElement VERTEX_VF = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.UINT, 1, "in_vertex_vf", false, true);

	private static final CanvasVertexFormatElement BASE_RGBA_VF = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.UINT, 1, "in_color_vf", false, true);

	private static final CanvasVertexFormatElement BASE_TEX_VF = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.UINT, 1, "in_uv_vf", false, true);

	public static final CanvasVertexFormat VF_MATERIAL = new CanvasVertexFormat(HEADER_VF, VERTEX_VF, BASE_RGBA_VF, BASE_TEX_VF);

	// Note the vertex stride is the effective quad stride because of indexing
	static final int VF_QUAD_STRIDE = VF_MATERIAL.vertexStrideInts;

	public static final QuadTranscoder VF_TRANSCODER = (quad, context, buff) -> {
		final Matrix4fExt matrix = (Matrix4fExt) (Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final int overlay = context.overlay();

		quad.overlay(overlay);

		final boolean aoDisabled = !MinecraftClient.isAmbientOcclusionEnabled();
		final float[] aoData = quad.ao;
		final RenderMaterialImpl mat = quad.material();

		assert mat.blendMode != BlendMode.DEFAULT;

		final int material = mat.dongle().index(quad.spriteId()) << 12;

		int n = buff.allocate(VF_QUAD_STRIDE);
		final int[] target = buff.data();

		final int vfColor = TerrainVertexFetch.COLOR.index(quad.vertexColor(0), quad.vertexColor(1), quad.vertexColor(2), quad.vertexColor(3));

		final int vfUv = TerrainVertexFetch.UV.index(
				quad.spriteBufferU(0) | (quad.spriteBufferV(0) << 16),
				quad.spriteBufferU(1) | (quad.spriteBufferV(1) << 16),
				quad.spriteBufferU(2) | (quad.spriteBufferV(2) << 16),
				quad.spriteBufferU(3) | (quad.spriteBufferV(3) << 16)
		);

		final int vfVertex = TerrainVertexFetch.VERTEX.index(matrix, normalMatrix, quad);

		final int packedLight0 = quad.lightmap(0);
		final int light0 = (packedLight0 & 0xFF) | ((packedLight0 >> 8) & 0xFF00) | ((aoDisabled ? 255 : (Math.round(aoData[0] * 255))) << 16);

		final int packedLight1 = quad.lightmap(1);
		final int light1 = (packedLight1 & 0xFF) | ((packedLight1 >> 8) & 0xFF00) | ((aoDisabled ? 255 : (Math.round(aoData[1] * 255))) << 16);

		final int packedLight2 = quad.lightmap(2);
		final int light2 = (packedLight2 & 0xFF) | ((packedLight2 >> 8) & 0xFF00) | ((aoDisabled ? 255 : (Math.round(aoData[2] * 255))) << 16);

		final int packedLight3 = quad.lightmap(3);
		final int light3 = (packedLight3 & 0xFF) | ((packedLight3 >> 8) & 0xFF00) | ((aoDisabled ? 255 : (Math.round(aoData[3] * 255))) << 16);

		final int vfLight = TerrainVertexFetch.LIGHT.index(light0, light1, light2, light3);

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
		target[n++] = material | packedRelPos;
		target[n++] = vfVertex | l0;
		target[n++] = vfColor | l1;
		target[n++] = vfUv | l2;
	};
}
