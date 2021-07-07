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

package grondag.canvas.vf;

import grondag.canvas.config.Configurator;
import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.GFX;
import grondag.canvas.vf.index.VfInt;
import grondag.canvas.vf.index.VfVertex;
import grondag.canvas.vf.storage.VfStorageReference;
import grondag.canvas.vf.storage.VfStorageTexture;
import grondag.canvas.vf.stream.VfStreamTexture;

public class TerrainVertexFetch {
	public static final VfInt UV = new VfInt(TextureData.VF_UV, GFX.GL_RG16);
	public static final VfInt COLOR = new VfInt(TextureData.VF_COLOR, GFX.GL_RGBA8);
	public static final VfInt LIGHT = new VfInt(TextureData.VF_LIGHT, GFX.GL_RGBA8);
	public static final VfVertex VERTEX = new VfVertex(TextureData.VF_VERTEX, GFX.GL_RGBA32I);
	public static final VfStorageTexture<VfStorageReference> QUADS = new VfStorageTexture<>(TextureData.VF_QUADS, GFX.GL_RGBA32I, 0x8000000);
	public static final VfStreamTexture REGIONS = new VfStreamTexture(TextureData.VF_REGIONS, GFX.GL_RGBA32I, 0x100000);
	public static final VfStreamTexture QUAD_REGION_MAP = new VfStreamTexture(TextureData.VF_QUAD_REGIONS, GFX.GL_R16UI, 0x1000000);

	public static void upload() {
		if (Configurator.vf) {
			COLOR.upload();
			UV.upload();
			LIGHT.upload();
			VERTEX.upload();
			QUADS.upload();
		}
	}

	public static void enable() {
		COLOR.enable();
		UV.enable();
		LIGHT.enable();
		VERTEX.enable();
		QUADS.enable();
	}

	public static void disable() {
		COLOR.disable();
		UV.disable();
		LIGHT.disable();
		VERTEX.disable();
		QUADS.disable();
	}

	public static void clear() {
		UV.clear();
		COLOR.clear();
		LIGHT.clear();
		VERTEX.clear();
		QUADS.clear();
	}
}
