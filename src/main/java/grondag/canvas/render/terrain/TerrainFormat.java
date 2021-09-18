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
import static grondag.canvas.buffer.format.CanvasVertexFormats.BASE_RGBA_4UB;
import static grondag.canvas.buffer.format.CanvasVertexFormats.BASE_TEX_2US;
import static grondag.canvas.buffer.format.CanvasVertexFormats.MATERIAL_1US;
import static grondag.canvas.buffer.format.CanvasVertexFormats.NORMAL_TANGENT_4B;

import io.vram.frex.api.material.MaterialConstants;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.util.math.MathHelper;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.format.CanvasVertexFormatElement;
import grondag.canvas.buffer.format.QuadEncoder;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;

public class TerrainFormat {
	private TerrainFormat() { }

	private static final CanvasVertexFormatElement REGION = new CanvasVertexFormatElement(VertexFormatElement.DataType.USHORT, 4, "in_region", false, true);
	private static final CanvasVertexFormatElement BLOCK_POS_AO = new CanvasVertexFormatElement(VertexFormatElement.DataType.UBYTE, 4, "in_blockpos_ao", false, true);
	private static final CanvasVertexFormatElement LIGHTMAPS_2UB = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.UBYTE, 2, "in_lightmap", false, true);

	// Would be nice to make this smaller but with less precision in position we start
	// to see Z-fighting on iron bars, fire, etc. Iron bars require a resolution of 1/16000.
	// Reducing resolution of UV is problematic for multi-block textures.
	public static final CanvasVertexFormat TERRAIN_MATERIAL = new CanvasVertexFormat(
			REGION,
			BLOCK_POS_AO,
			BASE_RGBA_4UB, BASE_TEX_2US, LIGHTMAPS_2UB, MATERIAL_1US, NORMAL_TANGENT_4B);

	static final int TERRAIN_QUAD_STRIDE = TERRAIN_MATERIAL.quadStrideInts;
	static final int TERRAIN_VERTEX_STRIDE = TERRAIN_MATERIAL.vertexStrideInts;

	public static final QuadEncoder TERRAIN_ENCODER = (quad, context, buff) -> {
		final Matrix4fExt matrix = (Matrix4fExt) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final boolean isNormalMatrixUseful = !normalMatrix.canvas_isIdentity();

		final int overlay = context.overlay();

		quad.overlay(overlay);

		final boolean aoDisabled = !MinecraftClient.isAmbientOcclusionEnabled();
		final float[] aoData = quad.ao;
		final RenderMaterialImpl mat = quad.material();

		assert mat.preset != MaterialConstants.PRESET_DEFAULT;

		final int quadNormalFlags = quad.normalFlags();
		// don't retrieve if won't be used
		final int faceNormal = quadNormalFlags == 0b1111 ? 0 : quad.packedFaceNormal();
		// bit 16 is set if normal Z component is negative
		int normalFlagBits = 0;
		int packedNormal = 0;
		int transformedNormal = 0;

		final int quadTangetFlags = quad.tangentFlags();
		final int faceTangent = quadTangetFlags == 0b1111 ? 0 : quad.packedFaceTanget();
		// bit 15 is set if tangent Z component is negative
		int tangentFlagBits = 0;
		int packedTangent = 0;
		int transformedTangent = 0;

		final int material = mat.dongle().index(quad.spriteId()) << 16;

		final int baseTargetIndex = buff.allocate(TERRAIN_QUAD_STRIDE, quad.effectiveCullFaceId());
		final int[] target = buff.data();
		final int baseSourceIndex = quad.vertexStart();
		final int[] source = quad.data();

		// This and pos vertex encoding are the only differences from standard format
		final int sectorId = context.sectorId();
		assert sectorId >= 0;
		final int sectorRelativeRegionOrigin = context.sectorRelativeRegionOrigin();

		for (int i = 0; i < 4; i++) {
			final int vertexMask = 1 << i;
			final int fromIndex = baseSourceIndex + i * MESH_VERTEX_STRIDE;
			final int toIndex = baseTargetIndex + i * TERRAIN_VERTEX_STRIDE;

			// We do this here because we need to pack the normal Z sign bit with sector ID
			final int p = ((quadNormalFlags & vertexMask) == 0) ? faceNormal : source[fromIndex + VERTEX_NORMAL];

			if (p != packedNormal) {
				packedNormal = p;
				transformedNormal = isNormalMatrixUseful ? normalMatrix.canvas_transform(packedNormal) : packedNormal;
				normalFlagBits = (transformedNormal >>> 8) & 0x8000;
				transformedNormal = transformedNormal & 0xFFFF;
			}

			// We do this here because we need to pack the tangent Z sign bit with sector ID
			final int t = ((quadTangetFlags & vertexMask) == 0) ? faceTangent : source[baseSourceIndex + i + HEADER_FIRST_VERTEX_TANGENT];

			if (t != packedTangent) {
				packedTangent = p;
				transformedTangent = isNormalMatrixUseful ? normalMatrix.canvas_transform(packedTangent) : packedTangent;
				tangentFlagBits = (transformedTangent >>> 9) & 0x4000;
				transformedTangent = transformedTangent << 16;
			}

			// PERF: Consider fixed precision integer math
			final float x = Float.intBitsToFloat(source[fromIndex + VERTEX_X]);
			final float y = Float.intBitsToFloat(source[fromIndex + VERTEX_Y]);
			final float z = Float.intBitsToFloat(source[fromIndex + VERTEX_Z]);

			final float xOut = matrix.a00() * x + matrix.a01() * y + matrix.a02() * z + matrix.a03();
			final float yOut = matrix.a10() * x + matrix.a11() * y + matrix.a12() * z + matrix.a13();
			final float zOut = matrix.a20() * x + matrix.a21() * y + matrix.a22() * z + matrix.a23();

			int xInt = MathHelper.floor(xOut);
			int yInt = MathHelper.floor(yOut);
			int zInt = MathHelper.floor(zOut);

			final int xFract = Math.round((xOut - xInt) * 0xFFFF);
			final int yFract = Math.round((yOut - yInt) * 0xFFFF);
			final int zFract = Math.round((zOut - zInt) * 0xFFFF);

			// because our integer component could be negative, we have to unpack and re-pack the sector components
			xInt += (sectorRelativeRegionOrigin & 0xFF);
			yInt += ((sectorRelativeRegionOrigin >> 8) & 0xFF);
			zInt += ((sectorRelativeRegionOrigin >> 16) & 0xFF);

			target[toIndex] = sectorId | normalFlagBits | tangentFlagBits | (xFract << 16);
			target[toIndex + 1] = yFract | (zFract << 16);

			final int ao = aoDisabled ? 0xFF000000 : (Math.round(aoData[i] * 255) << 24);
			target[toIndex + 2] = xInt | (yInt << 8) | (zInt << 16) | ao;

			target[toIndex + 3] = source[fromIndex + VERTEX_COLOR];

			target[toIndex + 4] = (source[fromIndex + VERTEX_U] + UV_ROUNDING_BIT) >> UV_EXTRA_PRECISION
					| ((source[fromIndex + VERTEX_V] + UV_ROUNDING_BIT) >> UV_EXTRA_PRECISION << 16);

			final int packedLight = source[fromIndex + VERTEX_LIGHTMAP];
			final int blockLight = packedLight & 0xFF;
			final int skyLight = (packedLight >> 16) & 0xFF;
			target[toIndex + 5] = blockLight | (skyLight << 8) | material;

			target[toIndex + 6] = transformedNormal | transformedTangent;
		}
	};
}
