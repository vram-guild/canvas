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

package grondag.canvas.wip.state;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.wip.shader.WipMaterialShaderImpl;
import grondag.canvas.wip.shader.WipMaterialShaderManager;
import grondag.canvas.wip.state.property.WipDecal;
import grondag.canvas.wip.state.property.WipDepthTest;
import grondag.canvas.wip.state.property.WipFog;
import grondag.canvas.wip.state.property.WipTarget;
import grondag.canvas.wip.state.property.WipTextureState;
import grondag.canvas.wip.state.property.WipTransparency;
import grondag.canvas.wip.state.property.WipWriteMask;
import grondag.fermion.bits.BitPacker64;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

@SuppressWarnings("rawtypes")
abstract class AbstractRenderStateView {
	protected long bits;

	protected AbstractRenderStateView(long bits) {
		this.bits = bits;
	}

	public long collectorKey() {
		return bits & COLLECTOR_KEY_MASK;
	}

	public WipMaterialShaderImpl shader() {
		return WipMaterialShaderManager.INSTANCE.get(SHADER.getValue(bits));
	}

	public MaterialConditionImpl condition() {
		return MaterialConditionImpl.fromIndex(CONDITION.getValue(bits));
	}

	public int conditionIndex() {
		return CONDITION.getValue(bits);
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

	public int primitive() {
		return PRIMITIVE.getValue(bits);
	}

	public WipTextureState texture() {
		return WipTextureState.fromIndex(TEXTURE.getValue(bits));
	}

	public boolean bilinear() {
		return BILINEAR.getValue(bits);
	}

	public WipTransparency translucency() {
		return TRANSPARENCY.getValue(bits);
	}

	public WipDepthTest depthTest() {
		return DEPTH_TEST.getValue(bits);
	}

	public boolean cull() {
		return CULL.getValue(bits);
	}

	public WipWriteMask writeMask() {
		return WRITE_MASK.getValue(bits);
	}

	public boolean enableLightmap() {
		return ENABLE_LIGHTMAP.getValue(bits);
	}

	public WipDecal decal() {
		if (translucency() != WipTransparency.TRANSLUCENT && DECAL_TRANSLUCENCY.getValue(bits)) {
			assert DECAL.getValue(bits) == WipDecal.NONE;
			return WipDecal.TRANSLUCENT;
		} else {
			return DECAL.getValue(bits);
		}
	}

	public WipTarget target() {
		return TARGET.getValue(bits);
	}

	public boolean lines() {
		return LINES.getValue(bits);
	}

	public WipFog fog() {
		return FOG.getValue(bits);
	}

	/** derived and may not match input for non-block layers */
	@Deprecated
	public BlendMode blendMode() {
		if (DEFAULT_BLEND_MODE.getValue(bits)) {
			return BlendMode.DEFAULT;
		}

		if (translucency() != WipTransparency.NONE) {
			return BlendMode.TRANSLUCENT;
		} else if (cutout()) {
			return unmipped() ? BlendMode.CUTOUT : BlendMode.CUTOUT_MIPPED;
		} else {
			return BlendMode.SOLID;
		}
	}

	public boolean disableColorIndex() {
		return DISABLE_COLOR_INDEX.getValue(bits);
	}

	public boolean cutout() {
		return CUTOUT.getValue(bits);
	}

	public boolean unmipped() {
		return UNMIPPED.getValue(bits);
	}

	public boolean translucentCutout() {
		return TRANSLUCENT_CUTOUT.getValue(bits);
	}

	public boolean hurtOverlay() {
		return HURT_OVERLAY.getValue(bits);
	}

	public boolean flashOverlay() {
		return FLASH_OVERLAY.getValue(bits);
	}

	public int shaderFlags() {
		return (int) (bits >>> FLAG_SHIFT) & 0xFF;
	}

	static final BitPacker64<Void> PACKER = new BitPacker64<> (null, null);

	// GL State comes first for sorting
	static final BitPacker64.IntElement TEXTURE = PACKER.createIntElement(WipTextureState.MAX_TEXTURE_STATES);
	static final BitPacker64.BooleanElement BILINEAR = PACKER.createBooleanElement();

	static final BitPacker64<Void>.EnumElement<WipTransparency> TRANSPARENCY = PACKER.createEnumElement(WipTransparency.class);
	static final BitPacker64<Void>.EnumElement<WipDepthTest> DEPTH_TEST = PACKER.createEnumElement(WipDepthTest.class);
	static final BitPacker64.BooleanElement CULL = PACKER.createBooleanElement();
	static final BitPacker64<Void>.EnumElement<WipWriteMask> WRITE_MASK = PACKER.createEnumElement(WipWriteMask.class);
	static final BitPacker64.BooleanElement ENABLE_LIGHTMAP = PACKER.createBooleanElement();
	// note that translucent decal is never persisted because it isn't part of GL state - that is indicated by SORTED
	static final BitPacker64<Void>.EnumElement<WipDecal> DECAL = PACKER.createEnumElement(WipDecal.class);
	static final BitPacker64<Void>.EnumElement<WipTarget> TARGET = PACKER.createEnumElement(WipTarget.class);
	static final BitPacker64.BooleanElement LINES = PACKER.createBooleanElement();
	static final BitPacker64<Void>.EnumElement<WipFog> FOG = PACKER.createEnumElement(WipFog.class);
	static final BitPacker64.IntElement SHADER = PACKER.createIntElement(4096);

	public static final long RENDER_STATE_MASK = PACKER.bitMask();

	// These don't affect GL state but must be buffered separately
	// Should always be zero in render state, only used in buffer key and material
	static final BitPacker64.IntElement PRIMITIVE = PACKER.createIntElement(8);
	static final BitPacker64.IntElement CONDITION = PACKER.createIntElement(MaterialConditionImpl.MAX_CONDITIONS);
	// true when translucent and not sorted
	static final BitPacker64.BooleanElement DECAL_TRANSLUCENCY = PACKER.createBooleanElement();

	public static final long COLLECTOR_KEY_MASK = PACKER.bitMask();

	// here and below only used in material - holds vertex state - does not affect buffering or gl State
	static final BitPacker64.BooleanElement DISABLE_COLOR_INDEX = PACKER.createBooleanElement();
	static final BitPacker64.BooleanElement DEFAULT_BLEND_MODE = PACKER.createBooleanElement();

	static final int FLAG_SHIFT = PACKER.bitLength();

	// last 8 bits correspond to shader flag bits
	static final BitPacker64.BooleanElement EMISSIVE = PACKER.createBooleanElement();
	static final BitPacker64.BooleanElement DISABLE_DIFFUSE = PACKER.createBooleanElement();
	static final BitPacker64.BooleanElement DISABLE_AO = PACKER.createBooleanElement();
	static final BitPacker64.BooleanElement CUTOUT = PACKER.createBooleanElement();
	static final BitPacker64.BooleanElement UNMIPPED = PACKER.createBooleanElement();
	// true = 10%, false = 50%
	static final BitPacker64.BooleanElement TRANSLUCENT_CUTOUT = PACKER.createBooleanElement();
	static final BitPacker64.BooleanElement HURT_OVERLAY = PACKER.createBooleanElement();
	static final BitPacker64.BooleanElement FLASH_OVERLAY = PACKER.createBooleanElement();

	public static final long HURT_OVERLAY_FLAG = HURT_OVERLAY.comparisonMask() >>> FLAG_SHIFT;
	public static final long FLASH_OVERLAY_FLAG = FLASH_OVERLAY.comparisonMask() >>> FLAG_SHIFT;

	static {
		assert PACKER.bitLength() <= 64;
	}
}
