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
