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

package grondag.canvas.apiimpl;

import java.util.IdentityHashMap;
import net.minecraft.client.renderer.RenderType;
import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.material.RenderMaterial;
import grondag.canvas.material.state.RenderMaterialImpl;

public class StandardMaterials {
	public static final RenderMaterialImpl BLOCK_TRANSLUCENT = Canvas.instance().materialFinder().preset(MaterialConstants.PRESET_TRANSLUCENT).find();
	public static final RenderMaterialImpl BLOCK_SOLID = Canvas.instance().materialFinder().preset(MaterialConstants.PRESET_SOLID).find();
	public static final RenderMaterialImpl BLOCK_CUTOUT = Canvas.instance().materialFinder().preset(MaterialConstants.PRESET_CUTOUT).find();
	public static final RenderMaterialImpl BLOCK_CUTOUT_MIPPED = Canvas.instance().materialFinder().preset(MaterialConstants.PRESET_CUTOUT_MIPPED).find();
	public static final RenderMaterialImpl MATERIAL_STANDARD = Canvas.instance().materialFinder().preset(MaterialConstants.PRESET_DEFAULT).find();

	private static final IdentityHashMap<RenderType, RenderMaterialImpl> LAYER_MAP = new IdentityHashMap<>();

	static {
		LAYER_MAP.put(RenderType.solid(), BLOCK_SOLID);
		LAYER_MAP.put(RenderType.cutout(), BLOCK_CUTOUT);
		LAYER_MAP.put(RenderType.cutoutMipped(), BLOCK_CUTOUT_MIPPED);
		LAYER_MAP.put(RenderType.translucent(), BLOCK_TRANSLUCENT);
		Canvas.instance().registerMaterial(RenderMaterial.MATERIAL_STANDARD, StandardMaterials.MATERIAL_STANDARD);
	}

	public static RenderMaterialImpl get(RenderType layer) {
		return LAYER_MAP.get(layer);
	}
}
