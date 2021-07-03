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

public class CanvasVertexFormatElement {
	public static final CanvasVertexFormatElement POSITION_3F = new CanvasVertexFormatElement(
		VertexFormatElement.DataType.FLOAT, 3, "in_vertex", true, false);

	public static final CanvasVertexFormatElement BASE_RGBA_4UB = new CanvasVertexFormatElement(
		VertexFormatElement.DataType.UBYTE, 4, "in_color", true, false);

	// WIP: remove at end
	public static final CanvasVertexFormatElement BASE_RGBA_VF = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.UINT, 1, "in_color", false, true);

	public static final CanvasVertexFormatElement BASE_TEX_2F = new CanvasVertexFormatElement(
		VertexFormatElement.DataType.FLOAT, 2, "in_uv", true, false);

	public static final CanvasVertexFormatElement BASE_TEX_2US = new CanvasVertexFormatElement(
		VertexFormatElement.DataType.USHORT, 2, "in_uv", true, false);

	public static final CanvasVertexFormatElement LIGHTMAPS_2UB = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.UBYTE, 2, "in_lightmap", false, false);

	public static final CanvasVertexFormatElement NORMAL_3B = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.BYTE, 3, "in_normal", true, false);

	public static final CanvasVertexFormatElement AO_1UB = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.UBYTE, 1, "in_ao", true, false);

	public static final CanvasVertexFormatElement SPRITE_1US = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.USHORT, 1, "in_sprite", false, true);

	public static final CanvasVertexFormatElement MATERIAL_1US = new CanvasVertexFormatElement(
			VertexFormatElement.DataType.USHORT, 1, "in_material", false, true);

	public final String attributeName;
	public final int elementCount;
	public final int glConstant;
	public final boolean isNormalized;
	public final boolean isInteger;
	public final int byteSize;

	private CanvasVertexFormatElement(VertexFormatElement.DataType formatIn, int count, String attributeName, boolean isNormalized, boolean isInteger) {
		this.attributeName = attributeName;
		elementCount = count;
		glConstant = formatIn.getId();
		byteSize = formatIn.getByteLength() * count;
		this.isNormalized = isNormalized;
		this.isInteger = isInteger;
	}
}
