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

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

/**
 * Pointer to a specific mesh material that allows for an ambiguous "default"
 * blend mode.  If blend mode is ambiguous, then {@link #withDefaultBlendMode(int)}
 * MUST be called before the mesh material is retrieved with {@link #get()}.
 */
public class MeshMaterialLocator extends AbstractMeshMaterial implements RenderMaterial {
	final int index;

	private final MeshMaterialLocator[] blendModeVariants = new MeshMaterialLocator[4];
	private final MeshMaterial material;

	protected MeshMaterialLocator(int index, long bits0, long bits1) {
		this.index = index;
		this.bits0 = bits0;
		this.bits1 = bits1;

		final BlendMode baseLayer = blendMode();
		material = baseLayer == BlendMode.DEFAULT ? null : new MeshMaterial(this);
	}

	/**
	 * Get mesh material with specific blend mode. If this locator has a default blend mode
	 * then will return null (and code will fail). Call {@link #withDefaultBlendMode(int)} and
	 * call this on the result.
	 */
	public MeshMaterial get() {
		assert material != null : "Attempt to get mesh material for default blend mode.";
		return material;
	}

	private static final ThreadLocal<MeshMaterialFinder> variantFinder = ThreadLocal.withInitial(MeshMaterialFinder::new);

	void setupVariants() {
		final boolean needsBlendModeVariant = blendMode() == BlendMode.DEFAULT;

		final MeshMaterialFinder finder = variantFinder.get();

		if(needsBlendModeVariant) {
			for(int i = 0; i < 4; i++) {
				final BlendMode layer = LAYERS[i];

				assert layer != BlendMode.DEFAULT;

				finder.bits0 = bits0;
				finder.bits1 = bits1;

				if(finder.blendMode() == BlendMode.DEFAULT) {
					finder.blendMode(layer);
				}

				blendModeVariants[i] = finder.findInternal(true);

				assert blendModeVariants[i].blendMode() !=  BlendMode.DEFAULT;
			}
		} else {
			for(int i = 0; i < 4; i++) {
				blendModeVariants[i] = this;
			}
		}
	}

	/**
	 * If this material has one or more null blend modes, this returns
	 * a material with any such blend modes set to the given input. Typically
	 * this is only used for vanilla default materials that derive their
	 * blend mode from the block render layer, but it is also possible to
	 * specify materials with null blend modes to achieve the same behavior.<p>
	 *
	 * If a non-null blend mode is specified for every sprite layer, this
	 * will always return the current instance.<p>
	 *
	 * We need shader flags to accurately reflect the effective blend mode
	 * and we need that to be fast, and we also want the buffering logic to
	 * remain simple.  This solves all those problems.<p>
	 */
	public MeshMaterialLocator withDefaultBlendMode(int modeIndex) {
		assert blendModeVariants[modeIndex - 1].blendMode() != BlendMode.DEFAULT;
		return blendModeVariants[modeIndex - 1];
	}

	public int index() {
		return index;
	}
}
