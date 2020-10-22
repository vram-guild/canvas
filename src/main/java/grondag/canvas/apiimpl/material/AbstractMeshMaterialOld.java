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
import grondag.canvas.shader.MaterialShaderManager;
import grondag.fermion.bits.BitPacker64;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

public abstract class AbstractMeshMaterialOld {
	protected long bits;

	AbstractMeshMaterialOld(long bits) {
		this.bits = bits;
	}

	public BlendMode blendMode() {
		return BLEND_MODE.getValue(bits);
	}

	public boolean disableColorIndex() {
		return DISABLE_COLOR.getValue(bits);
	}

	public boolean emissive() {
		return EMISSIVE.getValue(bits);
	}

	public boolean disableDiffuse() {
		return DISABLE_DIFFUSE.getValue(bits);
	}

	public boolean disableAo() {
		return DISABLE_AO.getValue(bits);
	}

	public MaterialShaderImpl shader() {
		return MaterialShaderManager.INSTANCE.get(SHADER.getValue(bits));
	}

	public MaterialConditionImpl condition() {
		return MaterialConditionImpl.fromIndex(CONDITION.getValue(bits));
	}

	public static final int SHADER_FLAGS_DISABLE_AO;
	static final BlendMode[] LAYERS = new BlendMode[4];
	private static final BitPacker64<AbstractMeshMaterialOld> BITPACKER_0 = new BitPacker64<>(null, null);

	static final BitPacker64<AbstractMeshMaterialOld>.BooleanElement EMISSIVE = BITPACKER_0.createBooleanElement();
	static final BitPacker64<AbstractMeshMaterialOld>.BooleanElement DISABLE_AO = BITPACKER_0.createBooleanElement();
	static final BitPacker64<AbstractMeshMaterialOld>.BooleanElement DISABLE_DIFFUSE = BITPACKER_0.createBooleanElement();
	static final BitPacker64<AbstractMeshMaterialOld>.BooleanElement DISABLE_COLOR = BITPACKER_0.createBooleanElement();
	static final BitPacker64<AbstractMeshMaterialOld>.EnumElement<BlendMode> BLEND_MODE = BITPACKER_0.createEnumElement(BlendMode.class);
	static final BitPacker64<AbstractMeshMaterialOld>.IntElement SHADER = BITPACKER_0.createIntElement(MaterialShaderManager.MAX_SHADERS);
	static final BitPacker64<AbstractMeshMaterialOld>.IntElement CONDITION = BITPACKER_0.createIntElement(MaterialConditionImpl.MAX_CONDITIONS);

	protected static final long DEFAULT_BITS;

	static {
		final BlendMode[] layers = BlendMode.values();
		assert layers[0] == BlendMode.DEFAULT;
		assert layers.length == 5;

		for (int i = 0; i < 4; ++i) {
			LAYERS[i] = layers[i + 1];
		}
	}

	static {
		assert BITPACKER_0.bitLength() <= 64;

		long defaultBits = BLEND_MODE.setValue(BlendMode.DEFAULT, 0);

		final int defaultShaderIndex = MaterialShaderManager.INSTANCE.getDefault().getIndex();
		defaultBits = SHADER.setValue(defaultShaderIndex, defaultBits);

		DEFAULT_BITS = defaultBits;
		SHADER_FLAGS_DISABLE_AO = (int) DISABLE_AO.setValue(true, 0);
	}
}
