/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.material.state;

import net.minecraft.client.renderer.texture.TextureAtlas;

import io.vram.bitkit.BitPacker64;
import io.vram.frex.api.material.MaterialConstants;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.material.property.DecalRenderState;
import grondag.canvas.material.property.DepthTestRenderState;
import grondag.canvas.material.property.TargetRenderState;
import grondag.canvas.material.property.TextureMaterialState;
import grondag.canvas.material.property.TransparencyRenderState;
import grondag.canvas.material.property.WriteMaskRenderState;
import grondag.canvas.shader.MaterialShaderId;
import grondag.canvas.shader.MaterialShaderImpl;
import grondag.canvas.shader.data.ShaderStrings;

public abstract class MaterialStateEncoder {
	private MaterialStateEncoder() { }

	static boolean primaryTargetTransparency(long bits) {
		if (!SORTED.getValue(bits)) {
			return false;
		}

		final long masked = bits & COLLECTOR_AND_STATE_MASK;
		final var target = TARGET.getValue(bits);

		return (masked == TRANSLUCENT_TERRAIN_COLLECTOR_KEY && target == MaterialConstants.TARGET_TRANSLUCENT)
			|| (masked == TRANSLUCENT_ENTITY_COLLECTOR_KEY && target == MaterialConstants.TARGET_ENTITIES);
	}

	static final BitPacker64<Void> PACKER = new BitPacker64<> (null, null);

	// GL State comes first for sorting
	public static final BitPacker64<Void>.IntElement TARGET = PACKER.createIntElement(TargetRenderState.TARGET_COUNT);
	public static final BitPacker64<Void>.IntElement TEXTURE = PACKER.createIntElement(TextureMaterialState.MAX_TEXTURE_STATES);
	public static final BitPacker64<Void>.BooleanElement BLUR = PACKER.createBooleanElement();
	public static final BitPacker64<Void>.IntElement TRANSPARENCY = PACKER.createIntElement(TransparencyRenderState.TRANSPARENCY_COUNT);
	public static final BitPacker64<Void>.IntElement DEPTH_TEST = PACKER.createIntElement(DepthTestRenderState.DEPTH_TEST_COUNT);
	public static final BitPacker64<Void>.BooleanElement CULL = PACKER.createBooleanElement();
	public static final BitPacker64<Void>.IntElement WRITE_MASK = PACKER.createIntElement(WriteMaskRenderState.WRITE_MASK_COUNT);
	public static final BitPacker64<Void>.IntElement DECAL = PACKER.createIntElement(DecalRenderState.DECAL_COUNT);
	public static final BitPacker64<Void>.BooleanElement LINES = PACKER.createBooleanElement();

	// These don't affect GL state but must be collected and drawn separately
	// They also generally won't change within a render state for any given context
	// so they don't cause fragmentation except for sorted transparency, which is intended.
	public static final BitPacker64<Void>.BooleanElement SORTED = PACKER.createBooleanElement();
	//static final BitPacker64<Void>.IntElement PRIMITIVE = PACKER.createIntElement(8);

	// Identifies the collection key and state to be used for the primary sorted transparency buffer
	// for a given target. Also used to render mixed-material atlas quads as a performance optimization.
	// Quads outside of this buffer, if any, will be rendered after primary and may not sort correctly.
	// Must not be GUI render
	public static final long COLLECTOR_AND_STATE_MASK = PACKER.bitMask();

	// Part of render state and collection key for non-sorted, not included in either for sorted
	public static final BitPacker64<Void>.IntElement SHADER_ID = PACKER.createIntElement(MaterialShaderImpl.MAX_SHADERS);

	public static final long RENDER_STATE_MASK = PACKER.bitMask();

	// Can't be part of PTT collector key
	public static final BitPacker64<Void>.IntElement CONDITION = PACKER.createIntElement(MaterialConditionImpl.MAX_CONDITIONS);

	// here and below only used in material - holds vertex state - does not affect buffering or gl State
	public static final BitPacker64<Void>.BooleanElement DISABLE_COLOR_INDEX = PACKER.createBooleanElement();
	public static final BitPacker64<Void>.IntElement PRESET = PACKER.createIntElement(6);
	public static final BitPacker64<Void>.BooleanElement DISCARDS_TEXTURE = PACKER.createBooleanElement();

	public static final int FLAG_SHIFT = PACKER.bitLength();

	// remaining bits correspond to shader flag bits
	public static final BitPacker64<Void>.BooleanElement EMISSIVE = PACKER.createBooleanElement();
	public static final BitPacker64<Void>.BooleanElement DISABLE_DIFFUSE = PACKER.createBooleanElement();
	public static final BitPacker64<Void>.BooleanElement DISABLE_AO = PACKER.createBooleanElement();
	// WIP: doesn't handle alpha type cutout - only used for ender dragon currently
	public static final BitPacker64<Void>.IntElement CUTOUT = PACKER.createIntElement(4);
	public static final BitPacker64<Void>.BooleanElement UNMIPPED = PACKER.createBooleanElement();
	public static final BitPacker64<Void>.BooleanElement HURT_OVERLAY = PACKER.createBooleanElement();
	public static final BitPacker64<Void>.BooleanElement FLASH_OVERLAY = PACKER.createBooleanElement();
	public static final BitPacker64<Void>.BooleanElement FOG = PACKER.createBooleanElement();
	public static final BitPacker64<Void>.BooleanElement DISABLE_SHADOWS = PACKER.createBooleanElement();
	public static final BitPacker64<Void>.BooleanElement ENABLE_GLINT = PACKER.createBooleanElement();

	public static final long DEFAULT_BITS;

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
		defaultBits = TEXTURE.setValue(TextureMaterialState.fromId(TextureAtlas.LOCATION_BLOCKS).index, defaultBits);
		defaultBits = TARGET.setValue(MaterialConstants.TARGET_MAIN, defaultBits);
		defaultBits = WRITE_MASK.setValue(MaterialConstants.WRITE_MASK_COLOR_DEPTH, defaultBits);
		defaultBits = UNMIPPED.setValue(false, defaultBits);
		defaultBits = FOG.setValue(true, defaultBits);
		defaultBits = DISABLE_SHADOWS.setValue(false, defaultBits);
		defaultBits = CUTOUT.setValue(MaterialConstants.CUTOUT_NONE, defaultBits);

		DEFAULT_BITS = defaultBits;

		long translucentBits = PRESET.setValue(MaterialConstants.PRESET_NONE, 0);
		translucentBits = TEXTURE.setValue(TextureMaterialState.fromId(TextureAtlas.LOCATION_BLOCKS).index, translucentBits);
		translucentBits = BLUR.setValue(false, translucentBits);
		translucentBits = TRANSPARENCY.setValue(MaterialConstants.TRANSPARENCY_TRANSLUCENT, translucentBits);
		translucentBits = DEPTH_TEST.setValue(MaterialConstants.DEPTH_TEST_LEQUAL, translucentBits);
		translucentBits = CULL.setValue(true, translucentBits);
		translucentBits = WRITE_MASK.setValue(MaterialConstants.WRITE_MASK_COLOR_DEPTH, translucentBits);
		translucentBits = ENABLE_GLINT.setValue(false, translucentBits);
		translucentBits = DECAL.setValue(DecalRenderState.NONE.index, translucentBits);
		translucentBits = TARGET.setValue(MaterialConstants.TARGET_TRANSLUCENT, translucentBits);
		translucentBits = LINES.setValue(false, translucentBits);
		translucentBits = FOG.setValue(true, translucentBits);
		translucentBits = DISABLE_SHADOWS.setValue(false, translucentBits);
		translucentBits = SORTED.setValue(true, translucentBits);
		translucentBits = CUTOUT.setValue(MaterialConstants.CUTOUT_NONE, translucentBits);
		//translucentBits = PRIMITIVE.setValue(GL11.GL_QUADS, translucentBits);

		TRANSLUCENT_TERRAIN_COLLECTOR_KEY = translucentBits & COLLECTOR_AND_STATE_MASK;

		translucentBits = TEXTURE.setValue(TextureMaterialState.fromId(TextureAtlas.LOCATION_BLOCKS).index, translucentBits);
		translucentBits = TARGET.setValue(MaterialConstants.TARGET_ENTITIES, translucentBits);

		//copyFromLayer(RenderLayer.getItemEntityTranslucentCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
		TRANSLUCENT_ENTITY_COLLECTOR_KEY = translucentBits & COLLECTOR_AND_STATE_MASK;
	}
}
