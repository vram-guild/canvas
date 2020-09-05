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

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.shader.MaterialShaderManager;
import grondag.canvas.shader.ShaderPass;

/**
 * Describes a single layer of a mesh material, containing all of the information
 * needed to buffer and draw that layer.  Analogous to vanilla RenderLayer.
 */
public class MeshMaterialLayer {
	private final MeshMaterial meshMaterial;
	public final int shaderFlags;
	public final boolean isTranslucent;
	// WIP: remove
	@Deprecated
	public final ShaderPass shaderType;
	private final MaterialShaderImpl shader;

	public MeshMaterialLayer(MeshMaterial meshMaterial, int depth) {
		this.meshMaterial = meshMaterial;
		isTranslucent = meshMaterial.isTranslucent;
		// determine how to buffer
		if (depth == 0) {
			shaderType = isTranslucent ? ShaderPass.TRANSLUCENT : ShaderPass.SOLID;
		} else {
			// +1 layers with cutout are expected to not share pixels with lower layers! Otherwise Z-fighting over overwrite will happen
			// anything other than cutout handled as non-sorting, no-depth translucent decal

			// FIX: should be reading layer-specific blend mode, no?
			shaderType = this.meshMaterial.blendMode() == BlendMode.CUTOUT || this.meshMaterial.blendMode() == BlendMode.CUTOUT_MIPPED ? ShaderPass.SOLID : ShaderPass.DECAL;
		}

		shader = MaterialShaderManager.INSTANCE.get(MeshMaterialLocator.SHADERS[depth].getValue(this.meshMaterial.bits1));

		// WIP: flags get conveyed via MaterialVertexState instead
		int flags = this.meshMaterial.emissive(depth) ? 1 : 0;

		if (this.meshMaterial.disableDiffuse(depth)) {
			flags |= 2;
		}

		if (this.meshMaterial.disableAo(depth)) {
			flags |= 4;
		}

		switch(this.meshMaterial.blendMode()) {
		case CUTOUT:
			flags |= 16; // disable LOD
			//$FALL-THROUGH$
		case CUTOUT_MIPPED:
			flags |= 8; // cutout
			break;
		default:
			break;
		}

		shaderFlags = flags;
	}

	public MaterialShaderImpl shader() {
		return shader;
	}

	public MaterialConditionImpl condition() {
		return meshMaterial.condition;
	}
}
