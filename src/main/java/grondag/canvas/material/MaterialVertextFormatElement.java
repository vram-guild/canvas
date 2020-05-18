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

import static grondag.canvas.salvage.VertexAttributeEncoder.HD_AO_SHADEMAP;
import static grondag.canvas.salvage.VertexAttributeEncoder.HD_BLOCK_LIGHTMAP;
import static grondag.canvas.salvage.VertexAttributeEncoder.HD_SKY_LIGHTMAP;
import static grondag.canvas.salvage.VertexAttributeEncoder.LIGHTMAP;
import static grondag.canvas.salvage.VertexAttributeEncoder.NORMAL_AO;
import static grondag.canvas.salvage.VertexAttributeEncoder.OVERLAY;
import static grondag.canvas.salvage.VertexAttributeEncoder.POS;
import static grondag.canvas.salvage.VertexAttributeEncoder.SPRITE_COLOR_0;
import static grondag.canvas.salvage.VertexAttributeEncoder.SPRITE_UV_0;

import net.minecraft.client.render.VertexFormatElement;

import grondag.canvas.salvage.VertexAttributeEncoder;

public class MaterialVertextFormatElement {
	// openGL implementation on my dev laptop *really* wants to get vertex positions
	// via standard (GL 2.1) binding
	// slows to a crawl otherwise
	public static final MaterialVertextFormatElement POSITION_3F = new MaterialVertextFormatElement(
			VertexFormatElement.Format.FLOAT, 3, null, POS);
	public static final MaterialVertextFormatElement BASE_RGBA_4UB = new MaterialVertextFormatElement(
			VertexFormatElement.Format.UBYTE, 4, "in_color", SPRITE_COLOR_0);
	public static final MaterialVertextFormatElement BASE_TEX_2F = new MaterialVertextFormatElement(
			VertexFormatElement.Format.FLOAT, 2, "in_uv", SPRITE_UV_0);

	/**
	 * Format varies by model.<p>
	 *
	 * In vanilla lighting model, Bytes 1-2 are sky and block lightmap
	 * coordinates. 3rd and 4th bytes are control flags. <p>
	 */
	public static final MaterialVertextFormatElement LIGHTMAPS_4UB = new MaterialVertextFormatElement(
			VertexFormatElement.Format.UBYTE, 4, "in_lightmap", LIGHTMAP, false);

	public static final MaterialVertextFormatElement OVERLAY_4UB = new MaterialVertextFormatElement(
			VertexFormatElement.Format.UBYTE, 4, "in_overlay", OVERLAY, false);

	public static final MaterialVertextFormatElement HD_BLOCK_LIGHTMAP_2US = new MaterialVertextFormatElement(
			VertexFormatElement.Format.USHORT, 2, "in_hd_blocklight", HD_BLOCK_LIGHTMAP, false);

	public static final MaterialVertextFormatElement HD_SKY_LIGHTMAP_2US = new MaterialVertextFormatElement(
			VertexFormatElement.Format.USHORT, 2, "in_hd_skylight", HD_SKY_LIGHTMAP, false);

	public static final MaterialVertextFormatElement HD_AO_SHADEMAP_2US = new MaterialVertextFormatElement(
			VertexFormatElement.Format.USHORT, 2, "in_hd_ao", HD_AO_SHADEMAP, false);

	public static final MaterialVertextFormatElement NORMAL_AO_4UB = new MaterialVertextFormatElement(
			VertexFormatElement.Format.BYTE, 4, "in_normal_ao", NORMAL_AO, true);

	public final String attributeName;
	public final int elementCount;
	public final int glConstant;
	public final boolean isNormalized;
	public final int byteSize;
	public final VertexAttributeEncoder encoder;

	private MaterialVertextFormatElement(VertexFormatElement.Format formatIn, int count, String attributeName, VertexAttributeEncoder encoder) {
		this(formatIn, count, attributeName, encoder, true);
	}

	private MaterialVertextFormatElement(VertexFormatElement.Format formatIn, int count, String attributeName, VertexAttributeEncoder encoder, boolean isNormalized) {
		this.attributeName = attributeName;
		elementCount = count;
		glConstant = formatIn.getGlId();
		byteSize = formatIn.getSize() * count;
		this.encoder = encoder;
		this.isNormalized = isNormalized;
	}
}
