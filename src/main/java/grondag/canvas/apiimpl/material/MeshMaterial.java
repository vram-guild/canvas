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

package grondag.canvas.apiimpl.material;

import javax.annotation.Nullable;

import grondag.canvas.apiimpl.MaterialConditionImpl;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

/**
 * Mesh material with a specific blend mode and one or more layers.
 * This class controls overall quad buffer prep and encoding, but individual
 * layers control which buffers are targeted and individual layers.
 * <p>
 * WIP2: make sure can handle "dual" render layers and similar vanilla constructs.
 */
public class MeshMaterial extends AbstractMeshMaterial {
	/**
	 * True if base layer is translucent.
	 */
	public final boolean isTranslucent;
	final MaterialConditionImpl condition;
	private final MeshMaterialLayer[] layers = new MeshMaterialLayer[MAX_SPRITE_DEPTH];

	protected MeshMaterial(MeshMaterialLocator locator) {
		bits0 = locator.bits0;
		bits1 = locator.bits1;
		condition = locator.condition();
		isTranslucent = (blendMode() == BlendMode.TRANSLUCENT);

		layers[0] = new MeshMaterialLayer(this, 0);
		final int depth = spriteDepth();

		if (depth > 1) {
			layers[1] = new MeshMaterialLayer(this, 1);

			if (depth > 2) {
				layers[2] = new MeshMaterialLayer(this, 2);
			}
		}
	}

	/**
	 * Returns a single-layer material appropriate for the base layer or overlay/decal layer given.
	 */
	public @Nullable
	MeshMaterialLayer getLayer(int layerIndex) {
		assert layerIndex < spriteDepth();
		return layers[layerIndex];
	}
}
