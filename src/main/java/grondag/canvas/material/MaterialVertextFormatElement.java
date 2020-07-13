/*******************************************************************************
 * Copyright 2019 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.material;

import net.minecraft.client.render.VertexFormatElement;

public class MaterialVertextFormatElement {
	// openGL implementation on my dev laptop *really* wants to get vertex positions
	// via standard (GL 2.1) binding
	// slows to a crawl otherwise
	public static final MaterialVertextFormatElement POSITION_3F = new MaterialVertextFormatElement(
			VertexFormatElement.Format.FLOAT, 3, null);
	public static final MaterialVertextFormatElement BASE_RGBA_4UB = new MaterialVertextFormatElement(
			VertexFormatElement.Format.UBYTE, 4, "in_color");
	public static final MaterialVertextFormatElement BASE_TEX_2F = new MaterialVertextFormatElement(
			VertexFormatElement.Format.FLOAT, 2, "in_uv");

	/**
	 * In vanilla lighting model, Bytes 1-2 are sky and block lightmap
	 * coordinates. 3rd and 4th bytes are control flags. <p>
	 *
	 */
	public static final MaterialVertextFormatElement LIGHTMAPS_4UB = new MaterialVertextFormatElement(
			VertexFormatElement.Format.UBYTE, 4, "in_lightmap", false);

	public static final MaterialVertextFormatElement OVERLAY_4UB = new MaterialVertextFormatElement(
			VertexFormatElement.Format.UBYTE, 4, "in_overlay", false);

	public static final MaterialVertextFormatElement HD_LIGHTMAP_2US = new MaterialVertextFormatElement(
			VertexFormatElement.Format.USHORT, 2, "in_hd_lightmap", false);

	public static final MaterialVertextFormatElement NORMAL_AO_4UB = new MaterialVertextFormatElement(
			VertexFormatElement.Format.BYTE, 4, "in_normal_ao", true);

	public static final MaterialVertextFormatElement MATERIAL_2US = new MaterialVertextFormatElement(
			VertexFormatElement.Format.USHORT, 2, "in_material", false);

	public final String attributeName;
	public final int elementCount;
	public final int glConstant;
	public final boolean isNormalized;
	public final int byteSize;

	private MaterialVertextFormatElement(VertexFormatElement.Format formatIn, int count, String attributeName) {
		this(formatIn, count, attributeName, true);
	}

	private MaterialVertextFormatElement(VertexFormatElement.Format formatIn, int count, String attributeName, boolean isNormalized) {
		this.attributeName = attributeName;
		elementCount = count;
		glConstant = formatIn.getGlId();
		byteSize = formatIn.getSize() * count;
		this.isNormalized = isNormalized;
	}
}
