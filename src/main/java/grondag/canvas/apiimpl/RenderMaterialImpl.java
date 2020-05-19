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

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

import grondag.canvas.shader.ShaderContext;
import grondag.canvas.shader.ShaderManager;
import grondag.fermion.bits.BitPacker64;
import grondag.fermion.bits.BitPacker64.BooleanElement;
import grondag.frex.api.material.MaterialCondition;
import grondag.frex.api.material.MaterialFinder;
import grondag.frex.api.material.MaterialShader;

public abstract class RenderMaterialImpl {
	private static final BitPacker64<RenderMaterialImpl> BITPACKER = new BitPacker64<>(RenderMaterialImpl::getBits, RenderMaterialImpl::setBits);
	public static final int MAX_SPRITE_DEPTH = 3;
	private static final BlendMode[] LAYERS = new BlendMode[4];

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

	private static final BitPacker64<RenderMaterialImpl>.IntElement SHADER;

	private static final BitPacker64<RenderMaterialImpl>.IntElement CONDITION;

	private static final long DEFAULT_BITS;

	private static final ObjectArrayList<CompositeMaterial> LIST = new ObjectArrayList<>();
	private static final Long2ObjectOpenHashMap<CompositeMaterial> MAP = new Long2ObjectOpenHashMap<>();

	public static final int SHADER_FLAGS_DISABLE_AO;

	static {
		FLAGS[EMISSIVE_INDEX_START + 0] = BITPACKER.createBooleanElement();
		FLAGS[DIFFUSE_INDEX_START + 0] = BITPACKER.createBooleanElement();
		FLAGS[AO_INDEX_START + 0] = BITPACKER.createBooleanElement();

		FLAGS[EMISSIVE_INDEX_START + 1] = BITPACKER.createBooleanElement();
		FLAGS[DIFFUSE_INDEX_START + 1] = BITPACKER.createBooleanElement();
		FLAGS[AO_INDEX_START + 1] = BITPACKER.createBooleanElement();

		FLAGS[EMISSIVE_INDEX_START + 2] = BITPACKER.createBooleanElement();
		FLAGS[DIFFUSE_INDEX_START + 2] = BITPACKER.createBooleanElement();
		FLAGS[AO_INDEX_START + 2] = BITPACKER.createBooleanElement();

		FLAGS[COLOR_DISABLE_INDEX_START + 0] = BITPACKER.createBooleanElement();
		FLAGS[COLOR_DISABLE_INDEX_START + 1] = BITPACKER.createBooleanElement();
		FLAGS[COLOR_DISABLE_INDEX_START + 2] = BITPACKER.createBooleanElement();

		BLEND_MODE = BITPACKER.createEnumElement(BlendMode.class);

		SPRITE_DEPTH = BITPACKER.createIntElement(1, MAX_SPRITE_DEPTH);
		SHADER = BITPACKER.createIntElement(ShaderManager.MAX_SHADERS);
		CONDITION = BITPACKER.createIntElement(MaterialConditionImpl.MAX_CONDITIONS);

		assert BITPACKER.bitLength() <= 64;

		long defaultBits = 0;
		defaultBits = BLEND_MODE.setValue(BlendMode.DEFAULT, defaultBits);
		DEFAULT_BITS = defaultBits;

		long aoDisableBits = 0;
		aoDisableBits = FLAGS[AO_INDEX_START + 0].setValue(true, aoDisableBits);
		aoDisableBits = FLAGS[AO_INDEX_START + 1].setValue(true, aoDisableBits);
		aoDisableBits = FLAGS[AO_INDEX_START + 2].setValue(true, aoDisableBits);
		SHADER_FLAGS_DISABLE_AO = (int)aoDisableBits;
	}

	public static RenderMaterialImpl.CompositeMaterial byIndex(int index) {
		assert index < LIST.size();
		assert index >= 0;

		return LIST.get(index);
	}

	protected long bits = DEFAULT_BITS;

	private long getBits() {
		return bits;
	}

	private void setBits(long bits) {
		this.bits = bits;
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

	public static class CompositeMaterial extends RenderMaterialImpl implements RenderMaterial {
		private final int index;

		/**
		 * True if any texture wants AO shading. Simplifies check made by renderer at
		 * buffer-time.
		 */
		public final boolean hasAo;

		/**
		 * Determine which buffer we use - derived from base layer.
		 * If base layer is solid, then any additional sprites are handled
		 * as decals and render in solid pass.  If base layer is translucent
		 * then all sprite layers render as translucent.
		 */
		//public final BlendMode renderLayer;

		/**
		 * True if base layer is translucent.
		 */
		public final boolean isTranslucent;

		private final MaterialShaderImpl shader;

		private final MaterialConditionImpl condition;

		private final CompositeMaterial[] blendModeVariants = new CompositeMaterial[4];

		private final DrawableMaterial[] drawables = new DrawableMaterial[MAX_SPRITE_DEPTH];

		protected CompositeMaterial(int index, long bits, MaterialShaderImpl shader) {
			this.index = index;
			this.bits = bits;
			this.shader = shader;
			condition = MaterialConditionImpl.fromIndex(CONDITION.getValue(bits));
			final int depth = spriteDepth();
			hasAo = !disableAo(0) || (depth > 1 && !disableAo(1)) || (depth == 3 && !disableAo(2));
			final BlendMode baseLayer = blendMode();

			if(baseLayer == BlendMode.SOLID) {
				isTranslucent = false;
			} else {
				isTranslucent = (baseLayer == BlendMode.TRANSLUCENT);
			}
		}

		private static final ThreadLocal<Finder> variantFinder = ThreadLocal.withInitial(Finder::new);

		private void setupVariants() {
			final boolean needsBlendModeVariant = blendMode() == BlendMode.DEFAULT;

			final int depth = spriteDepth();

			final Finder finder = variantFinder.get();

			if(needsBlendModeVariant) {
				for(int i = 0; i < 4; i++) {
					final BlendMode layer = LAYERS[i];

					assert layer != BlendMode.DEFAULT;

					finder.bits = bits;
					finder.shader = shader;

					if(finder.blendMode() == BlendMode.DEFAULT) {
						finder.blendMode(layer);
					}

					blendModeVariants[i] = finder.findInternal(true);

					assert blendModeVariants[i].blendMode() !=  BlendMode.DEFAULT;
				}
			} else {
				// we are a renderable material, so set up control flags needed by shader
				for(int i = 0; i < 4; i++) {
					blendModeVariants[i] = this;
				}

				drawables[0] = new DrawableMaterial(0);

				if (depth > 1) {
					drawables[1] = new DrawableMaterial(1);

					if (depth > 2) {
						drawables[2] = new DrawableMaterial(2);
					}
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
		public CompositeMaterial forBlendMode(int modeIndex) {
			assert blendModeVariants[modeIndex - 1].blendMode() != BlendMode.DEFAULT;
			return blendModeVariants[modeIndex - 1];
		}

		/**
		 * Returns a single-layer material appropriate for the base layer or overlay/decal layer given.
		 * @param spriteIndex
		 * @return
		 */
		public @Nullable DrawableMaterial forDepth(int spriteIndex) {
			assert spriteIndex < spriteDepth();
			return drawables[spriteIndex];
		}

		public int index() {
			return index;
		}

		public class DrawableMaterial {
			public final int shaderFlags;
			public final ShaderContext.Type shaderType;

			public DrawableMaterial(int depth) {
				shaderType = depth == 0 ? (blendMode() == BlendMode.TRANSLUCENT ? ShaderContext.Type.TRANSLUCENT : ShaderContext.Type.SOLID) : ShaderContext.Type.DECAL;
				int flags = emissive(depth) ? 1 : 0;

				if (disableDiffuse(depth)) {
					flags |= 2;
				}

				if (disableAo(depth)) {
					flags |= 4;
				}

				if (depth == 0) {
					switch(blendMode()) {
					case CUTOUT:
						flags |= 16; // disable LOD
						//$FALL-THROUGH$
					case CUTOUT_MIPPED:
						flags |= 8; // cutout
						break;
					default:
						break;
					}
				}

				shaderFlags = flags;
			}

			public MaterialShaderImpl shader() {
				return shader;
			}

			public MaterialConditionImpl condition() {
				return condition;
			}
		}
	}

	public static class Finder extends RenderMaterialImpl implements MaterialFinder {
		private MaterialShaderImpl shader = null;

		@Override
		public CompositeMaterial find() {
			return findInternal(true);
		}

		private synchronized CompositeMaterial findInternal(boolean setupVariants) {
			final MaterialShaderImpl s = shader == null ? ShaderManager.INSTANCE.getDefault() : shader;
			SHADER.setValue(s.getIndex(), this);
			CompositeMaterial result = MAP.get(bits);

			if (result == null) {
				result = new CompositeMaterial(LIST.size(), bits, s);
				LIST.add(result);
				MAP.put(result.bits, result);

				if (setupVariants) {
					result.setupVariants();
				}
			}

			return result;
		}

		@Override
		public Finder clear() {
			bits = DEFAULT_BITS;
			shader = null;
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

		@Deprecated
		@Override
		public Finder shader(MaterialShader shader) {
			this.shader = (MaterialShaderImpl) shader;
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

		@Override
		public MaterialFinder shader(int spriteIndex, MaterialShader pipeline) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
