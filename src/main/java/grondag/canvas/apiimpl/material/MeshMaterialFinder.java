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
import grondag.frex.api.material.MaterialCondition;
import grondag.frex.api.material.MaterialFinder;
import grondag.frex.api.material.MaterialShader;

public class MeshMaterialFinder extends AbstractMeshMaterial implements MaterialFinder {
	@Override
	public MeshMaterialLocator find() {
		return findInternal(true);
	}

	synchronized MeshMaterialLocator findInternal(boolean setupVariants) {
		MeshMaterialLocator result = MAP.get(this);

		if (result == null) {
			result = new MeshMaterialLocator(LIST.size(), bits0, bits1);
			LIST.add(result);
			MAP.put(new MeshMaterialKey(bits0, bits1), result);

			if (setupVariants) {
				result.setupVariants();
			}
		}

		return result;
	}

	@Override
	public MeshMaterialFinder clear() {
		bits0 = DEFAULT_BITS_0;
		bits1 = DEFAULT_BITS_0;
		return this;
	}

	@Deprecated
	@Override
	public MeshMaterialFinder blendMode(int spriteIndex, BlendMode blendMode) {
		if (spriteIndex == 0) {
			if (blendMode == null)  {
				blendMode = BlendMode.DEFAULT;
			}

			blendMode(blendMode);
		}

		return this;
	}

	@Override
	public MeshMaterialFinder disableColorIndex(int spriteIndex, boolean disable) {
		FLAGS[COLOR_DISABLE_INDEX_START + spriteIndex].setValue(disable, this);
		return this;
	}

	@Override
	public MeshMaterialFinder spriteDepth(int depth) {
		if (depth < 1 || depth > MAX_SPRITE_DEPTH) {
			throw new IndexOutOfBoundsException("Invalid sprite depth: " + depth);
		}

		SPRITE_DEPTH.setValue(depth, this);
		return this;
	}

	@Override
	public MeshMaterialFinder emissive(int spriteIndex, boolean isEmissive) {
		FLAGS[EMISSIVE_INDEX_START + spriteIndex].setValue(isEmissive, this);
		return this;
	}

	@Override
	public MeshMaterialFinder disableDiffuse(int spriteIndex, boolean disable) {
		FLAGS[DIFFUSE_INDEX_START + spriteIndex].setValue(disable, this);
		return this;
	}

	@Override
	public MeshMaterialFinder disableAo(int spriteIndex, boolean disable) {
		FLAGS[AO_INDEX_START + spriteIndex].setValue(disable, this);
		return this;
	}

	@Override
	public MeshMaterialFinder shader(int spriteIndex, MaterialShader shader) {
		SHADERS[spriteIndex].setValue(((MaterialShaderImpl) shader).getIndex(), this);
		return this;
	}

	@Override
	public MeshMaterialFinder condition(MaterialCondition condition) {
		CONDITION.setValue(((MaterialConditionImpl)condition).index, this);
		return this;
	}

	@Override
	public MeshMaterialFinder blendMode(BlendMode blendMode) {
		BLEND_MODE.setValue(blendMode, this);
		return this;
	}
}
