/*******************************************************************************
 * Copyright 2019 grondag
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
 ******************************************************************************/

package grondag.canvas.apiimpl;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.shader.MaterialShaderManager;
import grondag.fermion.bits.BitPacker64;
import grondag.fermion.bits.BitPacker64.BooleanElement;
import grondag.fermion.bits.BitPacker64.IntElement;
import grondag.frex.api.material.MaterialCondition;
import grondag.frex.api.material.MaterialFinder;
import grondag.frex.api.material.MaterialShader;

/**

 WIP Needs to be parallel to RenderLayer and also have a simpler mapping to material state and shaders
 Ideally shaders should not be context sensitive - contexts should instead expose attributes that shader depends on

 */
public abstract class RenderMaterialImpl extends RenderMaterialKey {
	private static final BitPacker64<RenderMaterialImpl> BITPACKER_0 = new BitPacker64<>(m -> m.bits0, (m, b) -> m.bits0 = b);
	private static final BitPacker64<RenderMaterialImpl> BITPACKER_1 = new BitPacker64<>(m -> m.bits1, (m, b) -> m.bits1 = b);

	public static final int MAX_SPRITE_DEPTH = 3;
	static final BlendMode[] LAYERS = new BlendMode[4];

	static {
		final BlendMode[] layers = BlendMode.values();
		assert layers[0] == BlendMode.DEFAULT;
		assert layers.length == 5;

		for (int i = 0; i < 4; ++i) {
			LAYERS[i] = layers[i + 1];
		}
	}

	// Following are indexes into the array of boolean elements.
	// They are NOT the index of the bits themselves.  Used to
	// efficiently access flags based on sprite layer. "_START"
	// index is the flag for sprite layer 0, with additional layers
	// offset additively by sprite index.
	private static final int EMISSIVE_INDEX_START = 0;
	private static final int DIFFUSE_INDEX_START = EMISSIVE_INDEX_START + MAX_SPRITE_DEPTH;
	private static final int AO_INDEX_START = DIFFUSE_INDEX_START + MAX_SPRITE_DEPTH;
	private static final int COLOR_DISABLE_INDEX_START = AO_INDEX_START + MAX_SPRITE_DEPTH;

	@SuppressWarnings("unchecked")
	private static final BitPacker64<RenderMaterialImpl>.BooleanElement[] FLAGS = new BooleanElement[COLOR_DISABLE_INDEX_START + MAX_SPRITE_DEPTH];

	private static final BitPacker64<RenderMaterialImpl>.EnumElement<BlendMode> BLEND_MODE;

	private static final BitPacker64<RenderMaterialImpl>.IntElement SPRITE_DEPTH;

	@SuppressWarnings("unchecked")
	static final BitPacker64<RenderMaterialImpl>.IntElement [] SHADERS = new IntElement[MAX_SPRITE_DEPTH];

	static final BitPacker64<RenderMaterialImpl>.IntElement CONDITION;

	private static final long DEFAULT_BITS_0;
	private static final long DEFAULT_BITS_1;

	private static final ObjectArrayList<CompositeMaterial> LIST = new ObjectArrayList<>();
	private static final Object2ObjectOpenHashMap<RenderMaterialKey, CompositeMaterial> MAP = new Object2ObjectOpenHashMap<>();

	public static final int SHADER_FLAGS_DISABLE_AO;

	static {
		for (int i = 0; i < MAX_SPRITE_DEPTH; ++i) {
			FLAGS[EMISSIVE_INDEX_START + i] = BITPACKER_0.createBooleanElement();
			FLAGS[DIFFUSE_INDEX_START + i] = BITPACKER_0.createBooleanElement();
			FLAGS[AO_INDEX_START + i] = BITPACKER_0.createBooleanElement();
			FLAGS[COLOR_DISABLE_INDEX_START + i] = BITPACKER_0.createBooleanElement();
			SHADERS[i] = BITPACKER_1.createIntElement(MaterialShaderManager.MAX_SHADERS);
		}

		BLEND_MODE = BITPACKER_0.createEnumElement(BlendMode.class);

		SPRITE_DEPTH = BITPACKER_0.createIntElement(1, MAX_SPRITE_DEPTH);

		CONDITION = BITPACKER_0.createIntElement(MaterialConditionImpl.MAX_CONDITIONS);

		assert BITPACKER_0.bitLength() <= 64;
		assert BITPACKER_1.bitLength() <= 64;

		DEFAULT_BITS_0 = BLEND_MODE.setValue(BlendMode.DEFAULT, 0);

		long defaultBits = 0;
		final int defaultShaderIndex = MaterialShaderManager.INSTANCE.getDefault().getIndex();

		for (int i = 0; i < MAX_SPRITE_DEPTH; ++i) {
			defaultBits = SHADERS[i].setValue(defaultShaderIndex, defaultBits);
		}

		DEFAULT_BITS_1 = defaultBits;

		long aoDisableBits = 0;

		for (int i = 0; i < MAX_SPRITE_DEPTH; ++i) {
			aoDisableBits = FLAGS[AO_INDEX_START + i].setValue(true, aoDisableBits);
		}

		SHADER_FLAGS_DISABLE_AO = (int)aoDisableBits;
	}

	public static CompositeMaterial byIndex(int index) {
		assert index < LIST.size();
		assert index >= 0;

		return LIST.get(index);
	}

	RenderMaterialImpl() {
		super(DEFAULT_BITS_0, DEFAULT_BITS_1);
	}

	public BlendMode blendMode() {
		return BLEND_MODE.getValue(this);
	}

	public boolean disableColorIndex(int spriteIndex) {
		return FLAGS[COLOR_DISABLE_INDEX_START + spriteIndex].getValue(this);
	}

	public int spriteDepth() {
		return SPRITE_DEPTH.getValue(this);
	}

	public boolean emissive(int spriteIndex) {
		return FLAGS[EMISSIVE_INDEX_START + spriteIndex].getValue(this);
	}

	public boolean disableDiffuse(int spriteIndex) {
		return FLAGS[DIFFUSE_INDEX_START + spriteIndex].getValue(this);
	}

	public boolean disableAo(int spriteIndex) {
		return FLAGS[AO_INDEX_START + spriteIndex].getValue(this);
	}

	public static class Finder extends RenderMaterialImpl implements MaterialFinder {
		@Override
		public CompositeMaterial find() {
			return findInternal(true);
		}

		synchronized CompositeMaterial findInternal(boolean setupVariants) {
			CompositeMaterial result = MAP.get(this);

			if (result == null) {
				result = new CompositeMaterial(LIST.size(), bits0, bits1);
				LIST.add(result);
				MAP.put(new RenderMaterialKey(bits0, bits1), result);

				if (setupVariants) {
					result.setupVariants();
				}
			}

			return result;
		}

		@Override
		public Finder clear() {
			bits0 = DEFAULT_BITS_0;
			bits1 = DEFAULT_BITS_0;
			return this;
		}

		@Deprecated
		@Override
		public Finder blendMode(int spriteIndex, BlendMode blendMode) {
			if (spriteIndex == 0) {
				if (blendMode == null)  {
					blendMode = BlendMode.DEFAULT;
				}

				blendMode(blendMode);
			}

			return this;
		}

		@Override
		public Finder disableColorIndex(int spriteIndex, boolean disable) {
			FLAGS[COLOR_DISABLE_INDEX_START + spriteIndex].setValue(disable, this);
			return this;
		}

		@Override
		public Finder spriteDepth(int depth) {
			if (depth < 1 || depth > MAX_SPRITE_DEPTH) {
				throw new IndexOutOfBoundsException("Invalid sprite depth: " + depth);
			}

			SPRITE_DEPTH.setValue(depth, this);
			return this;
		}

		@Override
		public Finder emissive(int spriteIndex, boolean isEmissive) {
			FLAGS[EMISSIVE_INDEX_START + spriteIndex].setValue(isEmissive, this);
			return this;
		}

		@Override
		public Finder disableDiffuse(int spriteIndex, boolean disable) {
			FLAGS[DIFFUSE_INDEX_START + spriteIndex].setValue(disable, this);
			return this;
		}

		@Override
		public Finder disableAo(int spriteIndex, boolean disable) {
			FLAGS[AO_INDEX_START + spriteIndex].setValue(disable, this);
			return this;
		}

		@Override
		public Finder shader(int spriteIndex, MaterialShader shader) {
			SHADERS[spriteIndex].setValue(((MaterialShaderImpl) shader).getIndex(), this);
			return this;
		}

		@Override
		public Finder condition(MaterialCondition condition) {
			CONDITION.setValue(((MaterialConditionImpl)condition).index, this);
			return this;
		}

		@Override
		public Finder blendMode(BlendMode blendMode) {
			BLEND_MODE.setValue(blendMode, this);
			return this;
		}
	}
}
