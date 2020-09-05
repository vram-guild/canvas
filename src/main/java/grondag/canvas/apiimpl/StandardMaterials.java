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

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.apiimpl.material.MeshMaterialLocator;

import net.minecraft.client.render.RenderLayer;

// TODO: expose in API as alternate for render layer
public class StandardMaterials {
	public static final MeshMaterialLocator BLOCK_TRANSLUCENT = Canvas.INSTANCE.materialFinder().blendMode(BlendMode.TRANSLUCENT).find();
	public static final MeshMaterialLocator BLOCK_SOLID = Canvas.INSTANCE.materialFinder().blendMode(BlendMode.SOLID).find();
	public static final MeshMaterialLocator BLOCK_CUTOUT = Canvas.INSTANCE.materialFinder().blendMode(BlendMode.CUTOUT).find();
	public static final MeshMaterialLocator BLOCK_CUTOUT_MIPPED = Canvas.INSTANCE.materialFinder().blendMode(BlendMode.CUTOUT_MIPPED).find();

	private  static final IdentityHashMap<RenderLayer, MeshMaterialLocator> LAYER_MAP = new IdentityHashMap<>();

	static {
		LAYER_MAP.put(RenderLayer.getSolid(), BLOCK_SOLID);
		LAYER_MAP.put(RenderLayer.getCutout(), BLOCK_CUTOUT);
		LAYER_MAP.put(RenderLayer.getCutoutMipped(), BLOCK_CUTOUT_MIPPED);
		LAYER_MAP.put(RenderLayer.getTranslucent(), BLOCK_TRANSLUCENT);
	}

	public static MeshMaterialLocator get(RenderLayer layer) {
		return LAYER_MAP.get(layer);
	}
}
