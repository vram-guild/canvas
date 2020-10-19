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

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.frex.api.material.MaterialCondition;
import grondag.frex.api.material.MaterialFinder;
import grondag.frex.api.material.MaterialShader;
import grondag.frex.api.material.RenderMaterial;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

public class MeshMaterialFinder extends AbstractMeshMaterial implements MaterialFinder {
	public MeshMaterialFinder() {
		super(DEFAULT_BITS);
	}

	@Override
	public MeshMaterial find() {
		synchronized(MeshMaterial.MAP) {
			MeshMaterial result = MeshMaterial.MAP.get(bits);

			if (result == null) {
				result = new MeshMaterial(MeshMaterial.LIST.size(), bits);
				MeshMaterial.LIST.add(result);
				MeshMaterial.MAP.put(bits, result);
			}

			return result;
		}
	}

	@Override
	public MeshMaterialFinder clear() {
		bits = DEFAULT_BITS;
		return this;
	}

	@Override
	public MeshMaterialFinder blendMode(BlendMode blendMode) {
		if (blendMode == null) {
			blendMode = BlendMode.DEFAULT;
		}

		bits = BLEND_MODE.setValue(blendMode, bits);
		return this;
	}

	@Override
	public MeshMaterialFinder disableColorIndex(boolean disable) {
		bits = DISABLE_COLOR.setValue(disable, bits);
		return this;
	}

	@Override
	public MeshMaterialFinder emissive(boolean isEmissive) {
		bits = EMISSIVE.setValue(isEmissive, bits);
		return this;
	}

	@Override
	public MeshMaterialFinder disableDiffuse(boolean disable) {
		bits = DISABLE_DIFFUSE.setValue(disable, bits);
		return this;
	}

	@Override
	public MeshMaterialFinder disableAo(boolean disable) {
		bits = DISABLE_AO.setValue(disable, bits);
		return this;
	}

	@Override
	public MeshMaterialFinder shader( MaterialShader shader) {
		bits = SHADER.setValue(((MaterialShaderImpl) shader).getIndex(), bits);
		return this;
	}

	@Override
	public MeshMaterialFinder condition(MaterialCondition condition) {
		bits = CONDITION.setValue(((MaterialConditionImpl) condition).index, bits);
		return this;
	}

	@Override
	public MeshMaterialFinder copyFrom(RenderMaterial material) {
		bits =  ((MeshMaterial) material).bits;
		return this;
	}
}
