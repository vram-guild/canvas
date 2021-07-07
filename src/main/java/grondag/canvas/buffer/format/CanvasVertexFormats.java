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

import net.minecraft.client.render.VertexFormatElement;

public final class CanvasVertexFormats {
	private static final CanvasVertexFormatElement POSITION_3F = new CanvasVertexFormatElement(
		VertexFormatElement.DataType.FLOAT, 3, "in_vertex", true, false);

	private static final CanvasVertexFormatElement BASE_RGBA_4UB = new CanvasVertexFormatElement(
		VertexFormatElement.DataType.UBYTE, 4, "in_color", true, false);

	private static final CanvasVertexFormatElement BASE_TEX_2F = new CanvasVertexFormatElement(
		VertexFormatElement.DataType.FLOAT, 2, "in_uv", true, false);

	private static final CanvasVertexFormatElement BASE_TEX_2US = new CanvasVertexFormatElement(
		VertexFormatElement.DataType.USHORT, 2, "in_uv", true, false);

	private static final CanvasVertexFormatElement LIGHTMAPS_2UB = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.UBYTE, 2, "in_lightmap", false, false);

	private static final CanvasVertexFormatElement NORMAL_3B = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.BYTE, 3, "in_normal", true, false);

	private static final CanvasVertexFormatElement AO_1UB = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.UBYTE, 1, "in_ao", true, false);

	private static final CanvasVertexFormatElement MATERIAL_1US = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.USHORT, 1, "in_material", false, true);

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
	public static final CanvasVertexFormat COMPACT_MATERIAL = new CanvasVertexFormat(POSITION_3F, BASE_RGBA_4UB, BASE_TEX_2US, LIGHTMAPS_2UB, MATERIAL_1US, NORMAL_3B, AO_1UB);

	private static final CanvasVertexFormatElement POSITION_3UI = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.UINT, 3, "in_vertex", false, true);

	/**
	 * Vertex is packed to allow for region ID with same space requirement.
	 */
	public static final CanvasVertexFormat REGION_MATERIAL = new CanvasVertexFormat(POSITION_3UI, BASE_RGBA_4UB, BASE_TEX_2US, LIGHTMAPS_2UB, MATERIAL_1US, NORMAL_3B, AO_1UB);

	// WIP: remove or clean up
	private static final CanvasVertexFormatElement HEADER_VF = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.UINT, 1, "in_header_vf", false, true);
	private static final CanvasVertexFormatElement VERTEX_VF = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.UINT, 1, "in_vertex_vf", false, true);

	private static final CanvasVertexFormatElement BASE_RGBA_VF = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.UINT, 1, "in_color_vf", false, true);

	private static final CanvasVertexFormatElement BASE_TEX_VF = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.UINT, 1, "in_uv_vf", false, true);

	public static final CanvasVertexFormat VF_MATERIAL = new CanvasVertexFormat(HEADER_VF, VERTEX_VF, BASE_RGBA_VF, BASE_TEX_VF);

	public static final int COMPACT_QUAD_STRIDE = COMPACT_MATERIAL.quadStrideInts;

	// Note the vertex stride is the effective quad stride because of indexing
	public static final int VF_QUAD_STRIDE = VF_MATERIAL.vertexStrideInts;

	public static CanvasVertexFormat MATERIAL_FORMAT = COMPACT_MATERIAL;
}
