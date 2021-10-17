/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.render.terrain;

import static grondag.canvas.buffer.format.CanvasVertexFormats.BASE_RGBA_4UB;
import static grondag.canvas.buffer.format.CanvasVertexFormats.BASE_TEX_2US;
import static grondag.canvas.buffer.format.CanvasVertexFormats.MATERIAL_1US;
import static grondag.canvas.buffer.format.CanvasVertexFormats.NORMAL_TANGENT_4B;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_FIRST_VERTEX_TANGENT;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.MESH_VERTEX_STRIDE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.UV_EXTRA_PRECISION;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.UV_ROUNDING_BIT;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_COLOR;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_LIGHTMAP;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_NORMAL;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_U;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_V;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_X;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_Y;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_Z;

import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.math.FastMatri4f;
import io.vram.frex.api.math.FastMatrix3f;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.format.CanvasVertexFormatElement;
import grondag.canvas.buffer.format.QuadEncoder;
import grondag.canvas.material.state.CanvasRenderMaterial;

public class TerrainFormat {
	private TerrainFormat() { }

	private static final CanvasVertexFormatElement REGION = new CanvasVertexFormatElement(VertexFormatElement.Type.USHORT, 4, "in_region", false, true);
	private static final CanvasVertexFormatElement BLOCK_POS_AO = new CanvasVertexFormatElement(VertexFormatElement.Type.UBYTE, 4, "in_blockpos_ao", false, true);
	private static final CanvasVertexFormatElement LIGHTMAPS_2UB = new CanvasVertexFormatElement(
			VertexFormatElement.Type.UBYTE, 2, "in_lightmap", false, true);

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
		final FastMatri4f matrix = (FastMatri4f) context.matrix();
		final FastMatrix3f normalMatrix = context.normalMatrix();
		final boolean isNormalMatrixUseful = !normalMatrix.f_isIdentity();

		final int overlay = context.overlay();

		quad.overlayCoords(overlay);

		final boolean aoDisabled = !Minecraft.useAmbientOcclusion();
		final float[] aoData = quad.ao;
		final CanvasRenderMaterial mat = (CanvasRenderMaterial) quad.material();

		assert mat.preset() != MaterialConstants.PRESET_DEFAULT;

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

		final int material = mat.materialIndexer().index(quad.spriteId()) << 16;

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
				transformedNormal = isNormalMatrixUseful ? normalMatrix.f_transformPacked3f(packedNormal) : packedNormal;
				normalFlagBits = (transformedNormal >>> 8) & 0x8000;
				transformedNormal = transformedNormal & 0xFFFF;
			}

			// We do this here because we need to pack the tangent Z sign bit with sector ID
			final int t = ((quadTangetFlags & vertexMask) == 0) ? faceTangent : source[baseSourceIndex + i + HEADER_FIRST_VERTEX_TANGENT];

			if (t != packedTangent) {
				packedTangent = p;
				transformedTangent = isNormalMatrixUseful ? normalMatrix.f_transformPacked3f(packedTangent) : packedTangent;
				tangentFlagBits = (transformedTangent >>> 9) & 0x4000;
				transformedTangent = transformedTangent << 16;
			}

			// PERF: Consider fixed precision integer math
			final float x = Float.intBitsToFloat(source[fromIndex + VERTEX_X]);
			final float y = Float.intBitsToFloat(source[fromIndex + VERTEX_Y]);
			final float z = Float.intBitsToFloat(source[fromIndex + VERTEX_Z]);

			final float xOut = matrix.f_m00() * x + matrix.f_m10() * y + matrix.f_m20() * z + matrix.f_m30();
			final float yOut = matrix.f_m01() * x + matrix.f_m11() * y + matrix.f_m21() * z + matrix.f_m31();
			final float zOut = matrix.f_m02() * x + matrix.f_m12() * y + matrix.f_m22() * z + matrix.f_m32();

			int xInt = Mth.floor(xOut);
			int yInt = Mth.floor(yOut);
			int zInt = Mth.floor(zOut);

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
