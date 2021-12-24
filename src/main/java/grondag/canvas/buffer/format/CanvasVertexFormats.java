/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.buffer.format;

import com.mojang.blaze3d.vertex.VertexFormatElement;

public final class CanvasVertexFormats {
	public static final CanvasVertexFormatElement POSITION_3F = new CanvasVertexFormatElement(
		VertexFormatElement.Type.FLOAT, 3, "in_vertex", true, false);

	private static final CanvasVertexFormatElement BASE_TEX_2F = new CanvasVertexFormatElement(
			VertexFormatElement.Type.FLOAT, 2, "in_uv", true, false);

	public static final CanvasVertexFormat PROCESS_VERTEX_UV = new CanvasVertexFormat(POSITION_3F, BASE_TEX_2F);
	public static final CanvasVertexFormat PROCESS_VERTEX = new CanvasVertexFormat(POSITION_3F);

	public static final CanvasVertexFormatElement BASE_RGBA_4UB = new CanvasVertexFormatElement(
		VertexFormatElement.Type.UBYTE, 4, "in_color", true, false);

	public static final CanvasVertexFormatElement BASE_TEX_2US = new CanvasVertexFormatElement(
		VertexFormatElement.Type.USHORT, 2, "in_uv", true, false);

	/** Low bits hold signs for Z coordinate of normal and tangent vectors. */
	public static final CanvasVertexFormatElement LIGHTMAPS_2UB_WITH_SIGNS = new CanvasVertexFormatElement(
			VertexFormatElement.Type.UBYTE, 2, "in_lightmap_with_signs", false, true);

	public static final CanvasVertexFormatElement MATERIAL_1US = new CanvasVertexFormatElement(
			VertexFormatElement.Type.USHORT, 1, "in_material", false, true);

	public static final CanvasVertexFormatElement NORMAL_TANGENT_4B = new CanvasVertexFormatElement(
			VertexFormatElement.Type.BYTE, 4, "in_normal_tangent", true, false);

	/**
	 * Compact default format for all world/game object rendering unless otherwise configured.
	 *
	 * <p>Texture is always normalized.
	 *
	 * <p>Two-byte material ID conveys sprite, condition, program IDs
	 * and vertex state flags.  AO is carried in last octet of normal.
	 */
	public static final CanvasVertexFormat STANDARD_MATERIAL_FORMAT = new CanvasVertexFormat(POSITION_3F, BASE_RGBA_4UB, BASE_TEX_2US, LIGHTMAPS_2UB_WITH_SIGNS, MATERIAL_1US, NORMAL_TANGENT_4B);

	public static final int STANDARD_QUAD_STRIDE = STANDARD_MATERIAL_FORMAT.quadStrideInts;
	public static final int STANDARD_VERTEX_STRIDE = STANDARD_MATERIAL_FORMAT.vertexStrideInts;
}
