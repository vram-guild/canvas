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
import io.vram.frex.api.material.RenderMaterial;

import grondag.canvas.material.property.DecalRenderState;
import grondag.canvas.material.property.TextureMaterialState;

public abstract class MaterialStateEncoder {
	private MaterialStateEncoder() { }

	static boolean primaryTargetTransparency(long bits) {
		bits &= RENDER_STATE_MASK;
		return (bits == TRANSLUCENT_TERRAIN_KEY) || (bits == TRANSLUCENT_ENTITY_KEY);
	}

	private static final BitPacker64<Void> RENDER_PACKER = new BitPacker64<> (null, null);

	// GL State comes first for sorting
	public static final BitPacker64<Void>.IntElement R_TARGET = RENDER_PACKER.createIntElement(MaterialConstants.TARGET_COUNT);
	public static final BitPacker64<Void>.IntElement R_TEXTURE = RENDER_PACKER.createIntElement(MaterialConstants.MAX_TEXTURE_STATES);
	public static final BitPacker64<Void>.BooleanElement R_BLUR = RENDER_PACKER.createBooleanElement();
	public static final BitPacker64<Void>.IntElement R_TRANSPARENCY = RENDER_PACKER.createIntElement(MaterialConstants.TRANSPARENCY_COUNT);
	public static final BitPacker64<Void>.IntElement R_DEPTH_TEST = RENDER_PACKER.createIntElement(MaterialConstants.DEPTH_TEST_COUNT);
	public static final BitPacker64<Void>.BooleanElement R_CULL = RENDER_PACKER.createBooleanElement();
	public static final BitPacker64<Void>.IntElement R_WRITE_MASK = RENDER_PACKER.createIntElement(MaterialConstants.WRITE_MASK_COUNT);
	public static final BitPacker64<Void>.IntElement R_DECAL = RENDER_PACKER.createIntElement(MaterialConstants.DECAL_COUNT);
	public static final BitPacker64<Void>.BooleanElement R_LINES = RENDER_PACKER.createBooleanElement();

	public static final long RENDER_STATE_MASK = RENDER_PACKER.bitMask();

	// Doesn't affect GL state but must be collected separately.
	public static final BitPacker64<Void>.BooleanElement R_SORTED = RENDER_PACKER.createBooleanElement();

	private static final BitPacker64<Void> MATERIAL_PACKER = new BitPacker64<> (null, null);

	private static final BitPacker64<Void>.IntElement M_SHADER = MATERIAL_PACKER.createIntElement(MaterialConstants.MAX_SHADERS);
	private static final BitPacker64<Void>.IntElement M_CONDITION = MATERIAL_PACKER.createIntElement(MaterialConstants.MAX_CONDITIONS);

	// here and below only used in material - holds vertex state - does not affect buffering or gl State
	private static final BitPacker64<Void>.BooleanElement M_DISABLE_COLOR_INDEX = MATERIAL_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.IntElement M_PRESET = MATERIAL_PACKER.createIntElement(6);
	private static final BitPacker64<Void>.BooleanElement M_DISCARDS_TEXTURE = MATERIAL_PACKER.createBooleanElement();

	public static final int FLAG_SHIFT = MATERIAL_PACKER.bitLength();

	// remaining bits correspond to shader flag bits
	private static final BitPacker64<Void>.BooleanElement M_EMISSIVE = MATERIAL_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.BooleanElement M_DISABLE_DIFFUSE = MATERIAL_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.BooleanElement M_DISABLE_AO = MATERIAL_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.IntElement M_CUTOUT = MATERIAL_PACKER.createIntElement(MaterialConstants.CUTOUT_COUNT);
	private static final BitPacker64<Void>.BooleanElement M_UNMIPPED = MATERIAL_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.BooleanElement M_HURT_OVERLAY = MATERIAL_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.BooleanElement M_FLASH_OVERLAY = MATERIAL_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.BooleanElement M_FOG = MATERIAL_PACKER.createBooleanElement();
	private static final BitPacker64<Void>.BooleanElement M_ENABLE_GLINT = MATERIAL_PACKER.createBooleanElement();

	private static final long TRANSLUCENT_TERRAIN_KEY;
	private static final long TRANSLUCENT_ENTITY_KEY;

	static {
		long translucentBits = R_TARGET.setValue(MaterialConstants.TARGET_TRANSLUCENT, 0);
		translucentBits = R_TEXTURE.setValue(TextureMaterialState.fromId(TextureAtlas.LOCATION_BLOCKS).index, translucentBits);
		translucentBits = R_BLUR.setValue(false, translucentBits);
		translucentBits = R_TRANSPARENCY.setValue(MaterialConstants.TRANSPARENCY_TRANSLUCENT, translucentBits);
		translucentBits = R_DEPTH_TEST.setValue(MaterialConstants.DEPTH_TEST_LEQUAL, translucentBits);
		translucentBits = R_CULL.setValue(true, translucentBits);
		translucentBits = R_WRITE_MASK.setValue(MaterialConstants.WRITE_MASK_COLOR_DEPTH, translucentBits);
		translucentBits = R_DECAL.setValue(DecalRenderState.NONE.index, translucentBits);
		translucentBits = R_LINES.setValue(false, translucentBits);

		TRANSLUCENT_TERRAIN_KEY = translucentBits;

		translucentBits = R_TEXTURE.setValue(TextureMaterialState.fromId(TextureAtlas.LOCATION_BLOCKS).index, translucentBits);
		translucentBits = R_TARGET.setValue(MaterialConstants.TARGET_ENTITIES, translucentBits);

		TRANSLUCENT_ENTITY_KEY = translucentBits;
	}

	// Identifies the collection key ato be used for the primary sorted transparency buffer
	// for a given target. Also used to render mixed-material atlas quads as a performance optimization.
	// Quads outside of this buffer, if any, will be rendered after primary and may not sort correctly.
	public static long encodeCollectorKey(RenderMaterial mat, TextureMaterialState texture) {
		long result = 0;
		result = MaterialStateEncoder.R_TARGET.setValue(mat.target(), result);
		result = MaterialStateEncoder.R_TEXTURE.setValue(texture.index, result);
		result = MaterialStateEncoder.R_BLUR.setValue(mat.blur(), result);
		result = MaterialStateEncoder.R_TRANSPARENCY.setValue(mat.transparency(), result);
		result = MaterialStateEncoder.R_DEPTH_TEST.setValue(mat.depthTest(), result);
		result = MaterialStateEncoder.R_CULL.setValue(mat.cull(), result);
		result = MaterialStateEncoder.R_WRITE_MASK.setValue(mat.writeMask(), result);
		result = MaterialStateEncoder.R_DECAL.setValue(mat.decal(), result);
		result = MaterialStateEncoder.R_LINES.setValue(mat.lines(), result);
		result = MaterialStateEncoder.R_SORTED.setValue(mat.sorted(), result);
		return result;
	}

	public static long encodeMaterialKey(RenderMaterial mat) {
		long result = 0;
		result = MaterialStateEncoder.M_CONDITION.setValue(mat.conditionIndex(), result);
		result = MaterialStateEncoder.M_CUTOUT.setValue(mat.cutout(), result);
		result = MaterialStateEncoder.M_DISABLE_AO.setValue(mat.disableAo(), result);
		result = MaterialStateEncoder.M_DISABLE_COLOR_INDEX.setValue(mat.disableColorIndex(), result);
		result = MaterialStateEncoder.M_DISABLE_DIFFUSE.setValue(mat.disableDiffuse(), result);
		result = MaterialStateEncoder.M_DISCARDS_TEXTURE.setValue(mat.discardsTexture(), result);
		result = MaterialStateEncoder.M_EMISSIVE.setValue(mat.emissive(), result);
		result = MaterialStateEncoder.M_FLASH_OVERLAY.setValue(mat.flashOverlay(), result);
		result = MaterialStateEncoder.M_FOG.setValue(mat.fog(), result);
		result = MaterialStateEncoder.M_ENABLE_GLINT.setValue(mat.foilOverlay(), result);
		result = MaterialStateEncoder.M_HURT_OVERLAY.setValue(mat.hurtOverlay(), result);
		result = MaterialStateEncoder.M_PRESET.setValue(mat.preset(), result);
		result = MaterialStateEncoder.M_SHADER.setValue(mat.shaderIndex(), result);
		result = MaterialStateEncoder.M_UNMIPPED.setValue(mat.unmipped(), result);
		return result;
	}
}
