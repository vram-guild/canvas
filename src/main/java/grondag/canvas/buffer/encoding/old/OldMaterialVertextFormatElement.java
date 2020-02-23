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

package grondag.canvas.buffer.encoding.old;

import static grondag.canvas.buffer.encoding.old.OldVertexAttributeEncoder.HD_AO_SHADEMAP;
import static grondag.canvas.buffer.encoding.old.OldVertexAttributeEncoder.HD_BLOCK_LIGHTMAP;
import static grondag.canvas.buffer.encoding.old.OldVertexAttributeEncoder.HD_SKY_LIGHTMAP;
import static grondag.canvas.buffer.encoding.old.OldVertexAttributeEncoder.LIGHTMAP;
import static grondag.canvas.buffer.encoding.old.OldVertexAttributeEncoder.NORMAL_AO;
import static grondag.canvas.buffer.encoding.old.OldVertexAttributeEncoder.POS;
import static grondag.canvas.buffer.encoding.old.OldVertexAttributeEncoder.SPRITE_COLOR_0;
import static grondag.canvas.buffer.encoding.old.OldVertexAttributeEncoder.SPRITE_COLOR_1;
import static grondag.canvas.buffer.encoding.old.OldVertexAttributeEncoder.SPRITE_COLOR_2;
import static grondag.canvas.buffer.encoding.old.OldVertexAttributeEncoder.SPRITE_UV_0;
import static grondag.canvas.buffer.encoding.old.OldVertexAttributeEncoder.SPRITE_UV_1;
import static grondag.canvas.buffer.encoding.old.OldVertexAttributeEncoder.SPRITE_UV_2;

import net.minecraft.client.render.VertexFormatElement;

public class OldMaterialVertextFormatElement {
	// openGL implementation on my dev laptop *really* wants to get vertex positions
	// via standard (GL 2.1) binding
	// slows to a crawl otherwise
	public static final OldMaterialVertextFormatElement POSITION_3F = new OldMaterialVertextFormatElement(
			VertexFormatElement.Format.FLOAT, 3, null, POS);
	public static final OldMaterialVertextFormatElement BASE_RGBA_4UB = new OldMaterialVertextFormatElement(
			VertexFormatElement.Format.UBYTE, 4, "in_color_0", SPRITE_COLOR_0);
	public static final OldMaterialVertextFormatElement BASE_TEX_2F = new OldMaterialVertextFormatElement(
			VertexFormatElement.Format.FLOAT, 2, "in_uv_0", SPRITE_UV_0);

	/**
	 * Format varies by model.<p>
	 *
	 * In vanilla lighting model, Bytes 1-2 are sky and block lightmap
	 * coordinates. 3rd and 4th bytes are control flags. <p>
	 */
	public static final OldMaterialVertextFormatElement LIGHTMAPS_4UB = new OldMaterialVertextFormatElement(
			VertexFormatElement.Format.UBYTE, 4, "in_lightmap", LIGHTMAP, false);

	public static final OldMaterialVertextFormatElement HD_BLOCK_LIGHTMAP_2US = new OldMaterialVertextFormatElement(
			VertexFormatElement.Format.USHORT, 2, "in_hd_blocklight", HD_BLOCK_LIGHTMAP, false);

	public static final OldMaterialVertextFormatElement HD_SKY_LIGHTMAP_2US = new OldMaterialVertextFormatElement(
			VertexFormatElement.Format.USHORT, 2, "in_hd_skylight", HD_SKY_LIGHTMAP, false);

	public static final OldMaterialVertextFormatElement HD_AO_SHADEMAP_2US = new OldMaterialVertextFormatElement(
			VertexFormatElement.Format.USHORT, 2, "in_hd_ao", HD_AO_SHADEMAP, false);

	public static final OldMaterialVertextFormatElement NORMAL_AO_4UB = new OldMaterialVertextFormatElement(
			VertexFormatElement.Format.BYTE, 4, "in_normal_ao", NORMAL_AO, true);

	public static final OldMaterialVertextFormatElement SECONDARY_RGBA_4UB = new OldMaterialVertextFormatElement(
			VertexFormatElement.Format.UBYTE, 4, "in_color_1", SPRITE_COLOR_1);

	public static final OldMaterialVertextFormatElement SECONDARY_TEX_2F = new OldMaterialVertextFormatElement(
			VertexFormatElement.Format.FLOAT, 2, "in_uv_1", SPRITE_UV_1);

	public static final OldMaterialVertextFormatElement TERTIARY_RGBA_4UB = new OldMaterialVertextFormatElement(
			VertexFormatElement.Format.UBYTE, 4, "in_color_2", SPRITE_COLOR_2);

	public static final OldMaterialVertextFormatElement TERTIARY_TEX_2F = new OldMaterialVertextFormatElement(
			VertexFormatElement.Format.FLOAT, 2, "in_uv_2", SPRITE_UV_2);

	public final String attributeName;
	public final int elementCount;
	public final int glConstant;
	public final boolean isNormalized;
	public final int byteSize;
	public final OldVertexAttributeEncoder encoder;

	private OldMaterialVertextFormatElement(VertexFormatElement.Format formatIn, int count, String attributeName, OldVertexAttributeEncoder encoder) {
		this(formatIn, count, attributeName, encoder, true);
	}

	private OldMaterialVertextFormatElement(VertexFormatElement.Format formatIn, int count, String attributeName, OldVertexAttributeEncoder encoder, boolean isNormalized) {
		this.attributeName = attributeName;
		elementCount = count;
		glConstant = formatIn.getGlId();
		byteSize = formatIn.getSize() * count;
		this.encoder = encoder;
		this.isNormalized = isNormalized;
	}
}
