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

import static grondag.canvas.buffer.format.CanvasVertexFormatElement.BASE_RGBA_4UB;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.BASE_TEX_2F;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.BASE_TEX_2US;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.LIGHTMAPS_4UB;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.MATERIAL_2US;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.NORMAL_FLAGS_4UB;
import static grondag.canvas.buffer.format.CanvasVertexFormatElement.POSITION_3F;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;

public final class CanvasVertexFormats {
	static {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: MaterialVertexFormats static init");
		}
	}

	public static final CanvasVertexFormat PROCESS_VERTEX_UV = new CanvasVertexFormat(POSITION_3F, BASE_TEX_2F);
	public static final CanvasVertexFormat PROCESS_VERTEX = new CanvasVertexFormat(POSITION_3F);

	/**
	 * New common format for all world/game object rendering.
	 *
	 * <p>Texture is always normalized. For atlas textures, sprite ID is
	 * carried in most significant bytes of normal.
	 * Most significant byte of lightmap holds vertex state flags.
	 */
	public static final CanvasVertexFormat POSITION_COLOR_TEXTURE_MATERIAL_LIGHT_NORMAL = new CanvasVertexFormat(POSITION_3F, BASE_RGBA_4UB, BASE_TEX_2US, MATERIAL_2US, LIGHTMAPS_4UB, NORMAL_FLAGS_4UB);

	public static final int MATERIAL_COLOR_INDEX = 3;
	public static final int MATERIAL_TEXTURE_INDEX = 4;
	public static final int MATERIAL_MATERIAL_INDEX = 5;
	public static final int MATERIAL_LIGHT_INDEX = 6;
	public static final int MATERIAL_NORMAL_INDEX = 7;

	public static final int MATERIAL_VERTEX_STRIDE = POSITION_COLOR_TEXTURE_MATERIAL_LIGHT_NORMAL.vertexStrideInts;
	public static final int MATERIAL_QUAD_STRIDE = MATERIAL_VERTEX_STRIDE * 4;

	// WIP2: new compact format
	// Position 3F
	// Color    4UB
	// Texture	2US
	// Mat/Norm 2US -> material includes sprite atlas info if needed
	// Lightmap is optional addition

	// 6 words / 24 bytes w/o lightmap
}
