/*
 * Copyright 2019, 2020 grondag
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
 */

package grondag.canvas.material;

import static grondag.canvas.material.MaterialVertextFormatElement.BASE_RGBA_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.BASE_TEX_2F;
import static grondag.canvas.material.MaterialVertextFormatElement.BASE_TEX_2US;
import static grondag.canvas.material.MaterialVertextFormatElement.HD_LIGHTMAP_2US;
import static grondag.canvas.material.MaterialVertextFormatElement.LIGHTMAPS_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.MATERIAL_2US;
import static grondag.canvas.material.MaterialVertextFormatElement.NORMAL_AO_4B;
import static grondag.canvas.material.MaterialVertextFormatElement.POSITION_3F;
import static grondag.canvas.material.MaterialVertextFormatElement.TEMP_LIGHTMAP;
import static grondag.canvas.material.MaterialVertextFormatElement.TEMP_OVERLAY;

import grondag.canvas.Configurator;

// WIP: make these fully parallel to vanilla formats
// encoding may be different and may depend on feature configuration

public final class MaterialVertexFormats {
	// PERF: try quantized vertex format
	public static final MaterialVertexFormat VANILLA_BLOCKS_AND_ITEMS = new MaterialVertexFormat(
			POSITION_3F,
			BASE_RGBA_4UB,
			BASE_TEX_2US,
			LIGHTMAPS_4UB,
			NORMAL_AO_4B,
			MATERIAL_2US);

	public static final MaterialVertexFormat TEMPORARY_ENTITY_FORMAT = new MaterialVertexFormat(
			POSITION_3F,
			BASE_RGBA_4UB,
			BASE_TEX_2F,
			TEMP_OVERLAY,
			TEMP_LIGHTMAP,
			NORMAL_AO_4B);

	public static final MaterialVertexFormat HD_TERRAIN = new MaterialVertexFormat(
			POSITION_3F,
			BASE_RGBA_4UB,
			BASE_TEX_2F,
			LIGHTMAPS_4UB, // PERF: remove and bundle flags with normal
			HD_LIGHTMAP_2US,
			NORMAL_AO_4B);

	public static final MaterialVertexFormat PROCESS_VERTEX_UV = new MaterialVertexFormat(
			POSITION_3F,
			BASE_TEX_2F);

	public static final MaterialVertexFormat PROCESS_VERTEX = new MaterialVertexFormat(
			POSITION_3F);

	// UGLY: derive this from formats
	public static final int MAX_QUAD_INT_STRIDE = 128;

	public static MaterialVertexFormat get(EncodingContext context, boolean translucent) {
		return context == EncodingContext.TERRAIN && Configurator.hdLightmaps() ? HD_TERRAIN : VANILLA_BLOCKS_AND_ITEMS;
	}
}
