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

package grondag.canvas.remove;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.apiimpl.material.MaterialShaderImpl;
import grondag.frex.api.material.MaterialCondition;
import grondag.frex.api.material.MaterialFinder;
import grondag.frex.api.material.MaterialShader;
import grondag.frex.api.material.RenderMaterial;

import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

public class MeshMaterialFinderOld extends AbstractMeshMaterialOld implements MaterialFinder {
	public MeshMaterialFinderOld() {
		super(DEFAULT_BITS);
	}

	@Override
	public MeshMaterialOld find() {
		synchronized(MeshMaterialOld.MAP) {
			MeshMaterialOld result = MeshMaterialOld.MAP.get(bits);

			if (result == null) {
				result = new MeshMaterialOld(MeshMaterialOld.LIST.size(), bits);
				MeshMaterialOld.LIST.add(result);
				MeshMaterialOld.MAP.put(bits, result);
			}

			return result;
		}
	}

	@Override
	public MeshMaterialFinderOld clear() {
		bits = DEFAULT_BITS;
		return this;
	}

	@Override
	public MeshMaterialFinderOld blendMode(BlendMode blendMode) {
		if (blendMode == null) {
			blendMode = BlendMode.DEFAULT;
		}

		bits = BLEND_MODE.setValue(blendMode, bits);
		return this;
	}

	@Override
	public MeshMaterialFinderOld disableColorIndex(boolean disable) {
		bits = DISABLE_COLOR.setValue(disable, bits);
		return this;
	}

	@Override
	public MeshMaterialFinderOld emissive(boolean isEmissive) {
		bits = EMISSIVE.setValue(isEmissive, bits);
		return this;
	}

	@Override
	public MeshMaterialFinderOld disableDiffuse(boolean disable) {
		bits = DISABLE_DIFFUSE.setValue(disable, bits);
		return this;
	}

	@Override
	public MeshMaterialFinderOld disableAo(boolean disable) {
		bits = DISABLE_AO.setValue(disable, bits);
		return this;
	}

	@Override
	public MeshMaterialFinderOld shader( MaterialShader shader) {
		bits = SHADER.setValue(((MaterialShaderImpl) shader).getIndex(), bits);
		return this;
	}

	@Override
	public MeshMaterialFinderOld condition(MaterialCondition condition) {
		bits = CONDITION.setValue(((MaterialConditionImpl) condition).index, bits);
		return this;
	}

	@Override
	public MeshMaterialFinderOld copyFrom(RenderMaterial material) {
		bits =  ((MeshMaterialOld) material).bits;
		return this;
	}

	@Override
	public MaterialFinder vertexShader(Identifier identifier) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MaterialFinder fragmentShader(Identifier identifier) {
		// TODO Auto-generated method stub
		return null;
	}
}
