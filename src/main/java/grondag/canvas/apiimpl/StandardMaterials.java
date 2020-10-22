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

package grondag.canvas.apiimpl;

import java.util.IdentityHashMap;

import grondag.canvas.wip.state.WipRenderMaterial;

import net.minecraft.client.render.RenderLayer;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

// TODO: expose in API as alternate for render layer
public class StandardMaterials {
	public static final WipRenderMaterial BLOCK_TRANSLUCENT = Canvas.INSTANCE.materialFinder().blendMode(BlendMode.TRANSLUCENT).find();
	public static final WipRenderMaterial BLOCK_SOLID = Canvas.INSTANCE.materialFinder().blendMode(BlendMode.SOLID).find();
	public static final WipRenderMaterial BLOCK_CUTOUT = Canvas.INSTANCE.materialFinder().blendMode(BlendMode.CUTOUT).find();
	public static final WipRenderMaterial BLOCK_CUTOUT_MIPPED = Canvas.INSTANCE.materialFinder().blendMode(BlendMode.CUTOUT_MIPPED).find();

	private static final IdentityHashMap<RenderLayer, WipRenderMaterial> LAYER_MAP = new IdentityHashMap<>();

	static {
		LAYER_MAP.put(RenderLayer.getSolid(), BLOCK_SOLID);
		LAYER_MAP.put(RenderLayer.getCutout(), BLOCK_CUTOUT);
		LAYER_MAP.put(RenderLayer.getCutoutMipped(), BLOCK_CUTOUT_MIPPED);
		LAYER_MAP.put(RenderLayer.getTranslucent(), BLOCK_TRANSLUCENT);
	}

	public static WipRenderMaterial get(RenderLayer layer) {
		return LAYER_MAP.get(layer);
	}
}
