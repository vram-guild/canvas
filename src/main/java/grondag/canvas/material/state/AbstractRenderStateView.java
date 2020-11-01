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

package grondag.canvas.material.state;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.material.property.MaterialDecal;
import grondag.canvas.material.property.MaterialDepthTest;
import grondag.canvas.material.property.MaterialFog;
import grondag.canvas.material.property.MaterialTarget;
import grondag.canvas.material.property.MaterialTextureState;
import grondag.canvas.material.property.MaterialTransparency;
import grondag.canvas.material.property.MaterialWriteMask;
import grondag.canvas.shader.MaterialShaderImpl;
import grondag.canvas.shader.MaterialShaderManager;
import grondag.canvas.shader.ProgramType;
import grondag.canvas.shader.ShaderData;
import grondag.fermion.bits.BitPacker64;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.texture.SpriteAtlasTexture;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

@SuppressWarnings("rawtypes")
abstract class AbstractRenderStateView {
	protected long bits;

	protected AbstractRenderStateView(long bits) {
		this.bits = bits;
	}

	public long collectorKey() {
		return sorted() ? (bits & SORTED_COLLECTOR_KEY_MASK) : (bits & UNSORTED_COLLECTOR_KEY_MASK);
	}

	public MaterialShaderImpl shader() {
		return MaterialShaderManager.INSTANCE.get(SHADER.getValue(bits));
	}

	public MaterialConditionImpl condition() {
		return MaterialConditionImpl.fromIndex(CONDITION.getValue(bits));
	}

	public boolean sorted() {
		return SORTED.getValue(bits);
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

	public MaterialTextureState texture() {
		return MaterialTextureState.fromIndex(TEXTURE.getValue(bits));
	}

	public boolean bilinear() {
		return BILINEAR.getValue(bits);
	}

	public MaterialTransparency translucency() {
		return decal() == MaterialDecal.TRANSLUCENT ? MaterialTransparency.TRANSLUCENT : TRANSPARENCY.getValue(bits);
	}

	public MaterialDepthTest depthTest() {
		return DEPTH_TEST.getValue(bits);
	}

	public boolean cull() {
		return CULL.getValue(bits);
	}

	public MaterialWriteMask writeMask() {
		return WRITE_MASK.getValue(bits);
	}

	public boolean enableLightmap() {
		return ENABLE_LIGHTMAP.getValue(bits);
	}

	public MaterialDecal decal() {
		return DECAL.getValue(bits);
	}

	public MaterialTarget target() {
		return TARGET.getValue(bits);
	}

	public boolean lines() {
		return LINES.getValue(bits);
	}

	public MaterialFog fog() {
		return FOG.getValue(bits);
	}

	/** derived and may not match input for non-block layers */
	public BlendMode blendMode() {
		if (DEFAULT_BLEND_MODE.getValue(bits)) {
			return BlendMode.DEFAULT;
		}

		if (translucency() != MaterialTransparency.NONE) {
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

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("bits:         ").append(Long.toHexString(bits)).append("  ").append(Long.toBinaryString(bits)).append("\n");
		sb.append("collectorKey: ").append(Long.toHexString(collectorKey())).append("  ").append(Long.toBinaryString(collectorKey())).append("\n");
		sb.append("bilinear: ").append(bilinear()).append("\n");
		sb.append("blendMode: ").append(blendMode().name()).append("\n");
		sb.append("conditionIndex: ").append(conditionIndex()).append("\n");
		sb.append("cull: ").append(cull()).append("\n");
		sb.append("cutout: ").append(cutout()).append("\n");
		sb.append("decal: ").append(decal().name()).append("\n");
		sb.append("depthTest: ").append(depthTest().name()).append("\n");
		sb.append("disableAo: ").append(disableAo()).append("\n");
		sb.append("disableColorIndex: ").append(disableColorIndex()).append("\n");
		sb.append("disableDiffuse: ").append(disableDiffuse()).append("\n");
		sb.append("emissive: ").append(emissive()).append("\n");
		sb.append("enableLightmap: ").append(enableLightmap()).append("\n");
		sb.append("flashoverlay: ").append(flashOverlay()).append("\n");
		sb.append("fog: ").append(fog().name()).append("\n");
		sb.append("hurtOverlay: ").append(hurtOverlay()).append("\n");
		sb.append("lines: ").append(lines()).append("\n");
		sb.append("primitive: ").append(primitive()).append("\n");
		sb.append("shader: [").append(shader().toString()).append("]\n");
		sb.append("shaderFlags: ").append(Integer.toBinaryString(shaderFlags())).append("\n");
		sb.append("sorted: ").append(sorted()).append("\n");
		sb.append("target: ").append(target().name()).append("\n");
		sb.append("texture: ").append(texture().index).append("  ").append(texture().id.toString()).append("\n");
		sb.append("transparency: ").append(translucency().name()).append("\n");
		sb.append("transparentCutout: ").append(translucentCutout()).append("\n");
		sb.append("unmipped: ").append(unmipped()).append("\n");
		sb.append("writeMask: ").append(writeMask().name()).append("\n");
		return sb.toString();

	}

	static final BitPacker64<Void> PACKER = new BitPacker64<> (null, null);

	// GL State comes first for sorting
	static final BitPacker64.IntElement TEXTURE = PACKER.createIntElement(MaterialTextureState.MAX_TEXTURE_STATES);
	static final BitPacker64.BooleanElement BILINEAR = PACKER.createBooleanElement();

	static final BitPacker64<Void>.EnumElement<MaterialTransparency> TRANSPARENCY = PACKER.createEnumElement(MaterialTransparency.class);
	static final BitPacker64<Void>.EnumElement<MaterialDepthTest> DEPTH_TEST = PACKER.createEnumElement(MaterialDepthTest.class);
	static final BitPacker64.BooleanElement CULL = PACKER.createBooleanElement();
	static final BitPacker64<Void>.EnumElement<MaterialWriteMask> WRITE_MASK = PACKER.createEnumElement(MaterialWriteMask.class);
	static final BitPacker64.BooleanElement ENABLE_LIGHTMAP = PACKER.createBooleanElement();
	// note that translucent decal is never persisted because it isn't part of GL state - that is indicated by SORTED
	// WIP: move matrix-based decal out of render state
	static final BitPacker64<Void>.EnumElement<MaterialDecal> DECAL = PACKER.createEnumElement(MaterialDecal.class);
	static final BitPacker64<Void>.EnumElement<MaterialTarget> TARGET = PACKER.createEnumElement(MaterialTarget.class);
	static final BitPacker64.BooleanElement LINES = PACKER.createBooleanElement();
	static final BitPacker64<Void>.EnumElement<MaterialFog> FOG = PACKER.createEnumElement(MaterialFog.class);

	// These don't affect GL state but must be collected and drawn separately
	// They also generally won't change within a render state for any given context
	// so they don't cause fragmentation except for sorted transparency, which is intended.
	/** indicates sorted transparency - should be only one true value per render target */
	static final BitPacker64.BooleanElement SORTED = PACKER.createBooleanElement();
	static final BitPacker64.IntElement PRIMITIVE = PACKER.createIntElement(8);

	public static final long SORTED_RENDER_STATE_MASK = PACKER.bitMask();

	// WIP: simplify shaders - the actual programs really aren't variant except by sorting
	// WIP: make vertex/uniform configurable for non-sorted layers

	// Part of render state and collection key for non-sorted, not included in either for sorted
	static final BitPacker64.IntElement SHADER = PACKER.createIntElement(4096);

	public static final long UNSORTED_RENDER_STATE_MASK = PACKER.bitMask();
	public static final long SORTED_COLLECTOR_KEY_MASK = PACKER.bitMask();

	// Can't be part of translucent collector key
	static final BitPacker64.IntElement CONDITION = PACKER.createIntElement(MaterialConditionImpl.MAX_CONDITIONS);

	public static final long UNSORTED_COLLECTOR_KEY_MASK = PACKER.bitMask();

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

	static final long DEFAULT_BITS;
	static {
		assert PACKER.bitLength() <= 64;

		long defaultBits = PRIMITIVE.setValue(GL11.GL_QUADS, 0);

		final int vertexShaderIndex = MaterialShaderManager.VERTEX_INDEXER.toHandle(ShaderData.DEFAULT_VERTEX_SOURCE);
		final int fragmentShaderIndex = MaterialShaderManager.FRAGMENT_INDEXER.toHandle(ShaderData.DEFAULT_FRAGMENT_SOURCE);
		defaultBits = SHADER.setValue(MaterialShaderManager.INSTANCE.find(vertexShaderIndex,fragmentShaderIndex, ProgramType.MATERIAL_UNIFORM_LOGIC).index, defaultBits);

		defaultBits = DEFAULT_BLEND_MODE.setValue(true, defaultBits);
		defaultBits = CULL.setValue(true, defaultBits);
		defaultBits = DEPTH_TEST.setValue(MaterialDepthTest.LEQUAL, defaultBits);
		defaultBits = ENABLE_LIGHTMAP.setValue(true, defaultBits);
		defaultBits = TEXTURE.setValue(MaterialTextureState.fromId(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).index, defaultBits);
		defaultBits = TARGET.setValue(MaterialTarget.MAIN, defaultBits);
		defaultBits = WRITE_MASK.setValue(MaterialWriteMask.COLOR_DEPTH, defaultBits);
		defaultBits = UNMIPPED.setValue(false, defaultBits);
		defaultBits = FOG.setValue(MaterialFog.FOG, defaultBits);

		DEFAULT_BITS = defaultBits;
	}
}
