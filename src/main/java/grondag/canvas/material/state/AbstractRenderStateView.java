/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.material.state;

import io.vram.frex.api.material.MaterialConstants;
import net.minecraft.client.renderer.texture.TextureAtlas;
import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.material.property.MaterialDecal;
import grondag.canvas.material.property.MaterialDepthTest;
import grondag.canvas.material.property.MaterialTarget;
import grondag.canvas.material.property.MaterialTextureState;
import grondag.canvas.material.property.MaterialTransparency;
import grondag.canvas.material.property.MaterialWriteMask;
import grondag.canvas.shader.MaterialShaderId;
import grondag.canvas.shader.MaterialShaderImpl;
import grondag.canvas.shader.data.ShaderStrings;
import grondag.fermion.bits.BitPacker64;

abstract class AbstractRenderStateView {
	protected long bits;

	protected AbstractRenderStateView(long bits) {
		this.bits = bits;
	}

	public long collectorKey() {
		return bits & COLLECTOR_AND_STATE_MASK;
	}

	public MaterialShaderId shaderId() {
		return MaterialShaderId.get(SHADER_ID.getValue(bits));
	}

	/**
	 * Will be always visible condition in vertex-controlled render state.
	 * This is ensured by the state mask.
	 */
	public MaterialConditionImpl condition() {
		return MaterialConditionImpl.fromIndex(CONDITION.getValue(bits));
	}

	public boolean sorted() {
		return SORTED.getValue(bits);
	}

	boolean primaryTargetTransparency() {
		if (!sorted()) {
			return false;
		}

		final long masked = bits & AbstractRenderState.COLLECTOR_AND_STATE_MASK;

		return (masked == TRANSLUCENT_TERRAIN_COLLECTOR_KEY && target() == MaterialConstants.TARGET_TRANSLUCENT)
			|| (masked == TRANSLUCENT_ENTITY_COLLECTOR_KEY && target() == MaterialConstants.TARGET_ENTITIES);
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

	public MaterialTextureState textureState() {
		return MaterialTextureState.fromIndex(TEXTURE.getValue(bits));
	}

	public boolean blur() {
		return BLUR.getValue(bits);
	}

	public int transparency() {
		return TRANSPARENCY.getValue(bits);
	}

	public int depthTest() {
		return DEPTH_TEST.getValue(bits);
	}

	public boolean cull() {
		return CULL.getValue(bits);
	}

	public int writeMask() {
		return WRITE_MASK.getValue(bits);
	}

	public boolean enableGlint() {
		return ENABLE_GLINT.getValue(bits);
	}

	public boolean discardsTexture() {
		return DISCARDS_TEXTURE.getValue(bits);
	}

	public int decal() {
		return DECAL.getValue(bits);
	}

	public int target() {
		return TARGET.getValue(bits);
	}

	public boolean lines() {
		return LINES.getValue(bits);
	}

	public boolean fog() {
		return FOG.getValue(bits);
	}

	public boolean castShadows() {
		return !DISABLE_SHADOWS.getValue(bits);
	}

	public int preset() {
		return PRESET.getValue(bits);
	}

	public boolean disableColorIndex() {
		return DISABLE_COLOR_INDEX.getValue(bits);
	}

	public int cutout() {
		return CUTOUT.getValue(bits);
	}

	public boolean unmipped() {
		return UNMIPPED.getValue(bits);
	}

	public boolean hurtOverlay() {
		return HURT_OVERLAY.getValue(bits);
	}

	public boolean flashOverlay() {
		return FLASH_OVERLAY.getValue(bits);
	}

	public int shaderFlags() {
		return (int) (bits >>> FLAG_SHIFT) & 0xFFFF;
	}

	static final BitPacker64<Void> PACKER = new BitPacker64<> (null, null);

	// GL State comes first for sorting
	static final BitPacker64<Void>.IntElement TARGET = PACKER.createIntElement(MaterialTarget.TARGET_COUNT);
	static final BitPacker64<Void>.IntElement TEXTURE = PACKER.createIntElement(MaterialTextureState.MAX_TEXTURE_STATES);
	static final BitPacker64<Void>.BooleanElement BLUR = PACKER.createBooleanElement();
	static final BitPacker64<Void>.IntElement TRANSPARENCY = PACKER.createIntElement(MaterialTransparency.TRANSPARENCY_COUNT);
	static final BitPacker64<Void>.IntElement DEPTH_TEST = PACKER.createIntElement(MaterialDepthTest.DEPTH_TEST_COUNT);
	static final BitPacker64<Void>.BooleanElement CULL = PACKER.createBooleanElement();
	static final BitPacker64<Void>.IntElement WRITE_MASK = PACKER.createIntElement(MaterialWriteMask.WRITE_MASK_COUNT);
	static final BitPacker64<Void>.IntElement DECAL = PACKER.createIntElement(MaterialDecal.DECAL_COUNT);
	static final BitPacker64<Void>.BooleanElement LINES = PACKER.createBooleanElement();

	// These don't affect GL state but must be collected and drawn separately
	// They also generally won't change within a render state for any given context
	// so they don't cause fragmentation except for sorted transparency, which is intended.
	static final BitPacker64<Void>.BooleanElement SORTED = PACKER.createBooleanElement();
	//static final BitPacker64<Void>.IntElement PRIMITIVE = PACKER.createIntElement(8);

	// Identifies the collection key and state to be used for the primary sorted transparency buffer
	// for a given target. Also used to render mixed-material atlas quads as a performance optimization.
	// Quads outside of this buffer, if any, will be rendered after primary and may not sort correctly.
	// Must not be GUI render
	public static final long COLLECTOR_AND_STATE_MASK = PACKER.bitMask();

	// Part of render state and collection key for non-sorted, not included in either for sorted
	static final BitPacker64<Void>.IntElement SHADER_ID = PACKER.createIntElement(MaterialShaderImpl.MAX_SHADERS);

	public static final long RENDER_STATE_MASK = PACKER.bitMask();

	// Can't be part of PTT collector key
	static final BitPacker64<Void>.IntElement CONDITION = PACKER.createIntElement(MaterialConditionImpl.MAX_CONDITIONS);

	// here and below only used in material - holds vertex state - does not affect buffering or gl State
	static final BitPacker64<Void>.BooleanElement DISABLE_COLOR_INDEX = PACKER.createBooleanElement();
	static final BitPacker64<Void>.IntElement PRESET = PACKER.createIntElement(6);
	static final BitPacker64<Void>.BooleanElement DISCARDS_TEXTURE = PACKER.createBooleanElement();

	static final int FLAG_SHIFT = PACKER.bitLength();

	// remaining bits correspond to shader flag bits
	static final BitPacker64<Void>.BooleanElement EMISSIVE = PACKER.createBooleanElement();
	static final BitPacker64<Void>.BooleanElement DISABLE_DIFFUSE = PACKER.createBooleanElement();
	static final BitPacker64<Void>.BooleanElement DISABLE_AO = PACKER.createBooleanElement();
	// WIP: doesn't handle alpha type cutout - only used for ender dragon currently
	static final BitPacker64<Void>.IntElement CUTOUT = PACKER.createIntElement(4);
	static final BitPacker64<Void>.BooleanElement UNMIPPED = PACKER.createBooleanElement();
	static final BitPacker64<Void>.BooleanElement HURT_OVERLAY = PACKER.createBooleanElement();
	static final BitPacker64<Void>.BooleanElement FLASH_OVERLAY = PACKER.createBooleanElement();
	static final BitPacker64<Void>.BooleanElement FOG = PACKER.createBooleanElement();
	static final BitPacker64<Void>.BooleanElement DISABLE_SHADOWS = PACKER.createBooleanElement();
	static final BitPacker64<Void>.BooleanElement ENABLE_GLINT = PACKER.createBooleanElement();

	static final long DEFAULT_BITS;

	public static final long TRANSLUCENT_TERRAIN_COLLECTOR_KEY;
	public static final long TRANSLUCENT_ENTITY_COLLECTOR_KEY;

	static {
		assert PACKER.bitLength() <= 64;

		long defaultBits = 0; //PRIMITIVE.setValue(GL11.GL_QUADS, 0);

		defaultBits = SHADER_ID.setValue(MaterialShaderId.find(ShaderStrings.DEFAULT_VERTEX_SOURCE, ShaderStrings.DEFAULT_FRAGMENT_SOURCE, ShaderStrings.DEFAULT_VERTEX_SOURCE, ShaderStrings.DEFAULT_FRAGMENT_SOURCE).index, defaultBits);
		defaultBits = PRESET.setValue(MaterialConstants.PRESET_DEFAULT, defaultBits);
		defaultBits = CULL.setValue(true, defaultBits);
		defaultBits = DEPTH_TEST.setValue(MaterialConstants.DEPTH_TEST_LEQUAL, defaultBits);
		defaultBits = ENABLE_GLINT.setValue(false, defaultBits);
		defaultBits = TEXTURE.setValue(MaterialTextureState.fromId(TextureAtlas.LOCATION_BLOCKS).index, defaultBits);
		defaultBits = TARGET.setValue(MaterialConstants.TARGET_MAIN, defaultBits);
		defaultBits = WRITE_MASK.setValue(MaterialConstants.WRITE_MASK_COLOR_DEPTH, defaultBits);
		defaultBits = UNMIPPED.setValue(false, defaultBits);
		defaultBits = FOG.setValue(true, defaultBits);
		defaultBits = DISABLE_SHADOWS.setValue(false, defaultBits);
		defaultBits = CUTOUT.setValue(MaterialConstants.CUTOUT_NONE, defaultBits);

		DEFAULT_BITS = defaultBits;

		long translucentBits = PRESET.setValue(MaterialConstants.PRESET_NONE, 0);
		translucentBits = TEXTURE.setValue(MaterialTextureState.fromId(TextureAtlas.LOCATION_BLOCKS).index, translucentBits);
		translucentBits = BLUR.setValue(false, translucentBits);
		translucentBits = TRANSPARENCY.setValue(MaterialConstants.TRANSPARENCY_TRANSLUCENT, translucentBits);
		translucentBits = DEPTH_TEST.setValue(MaterialConstants.DEPTH_TEST_LEQUAL, translucentBits);
		translucentBits = CULL.setValue(true, translucentBits);
		translucentBits = WRITE_MASK.setValue(MaterialConstants.WRITE_MASK_COLOR_DEPTH, translucentBits);
		translucentBits = ENABLE_GLINT.setValue(false, translucentBits);
		translucentBits = DECAL.setValue(MaterialDecal.NONE.index, translucentBits);
		translucentBits = TARGET.setValue(MaterialConstants.TARGET_TRANSLUCENT, translucentBits);
		translucentBits = LINES.setValue(false, translucentBits);
		translucentBits = FOG.setValue(true, translucentBits);
		translucentBits = DISABLE_SHADOWS.setValue(false, translucentBits);
		translucentBits = SORTED.setValue(true, translucentBits);
		translucentBits = CUTOUT.setValue(MaterialConstants.CUTOUT_NONE, translucentBits);
		//translucentBits = PRIMITIVE.setValue(GL11.GL_QUADS, translucentBits);

		TRANSLUCENT_TERRAIN_COLLECTOR_KEY = translucentBits & COLLECTOR_AND_STATE_MASK;

		translucentBits = TEXTURE.setValue(MaterialTextureState.fromId(TextureAtlas.LOCATION_BLOCKS).index, translucentBits);
		translucentBits = TARGET.setValue(MaterialConstants.TARGET_ENTITIES, translucentBits);

		//copyFromLayer(RenderLayer.getItemEntityTranslucentCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
		TRANSLUCENT_ENTITY_COLLECTOR_KEY = translucentBits & COLLECTOR_AND_STATE_MASK;
	}
}
