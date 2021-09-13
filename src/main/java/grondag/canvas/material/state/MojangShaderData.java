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

import static io.vram.frex.api.material.MaterialConstants.CUTOUT_ALPHA;
import static io.vram.frex.api.material.MaterialConstants.CUTOUT_HALF;
import static io.vram.frex.api.material.MaterialConstants.CUTOUT_NONE;
import static io.vram.frex.api.material.MaterialConstants.CUTOUT_TENTH;
import static io.vram.frex.api.material.MaterialConstants.CUTOUT_ZERO;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

// segregates render layer references from mod init
public final class MojangShaderData {
	public final boolean fog;
	public final int cutout;
	public final boolean diffuse;
	public final boolean glint;

	private MojangShaderData(boolean fog, boolean glint, int cutout, boolean diffuse) {
		this.fog = fog;
		this.glint = glint;
		this.cutout = cutout;
		this.diffuse = diffuse;
	}

	private static final Reference2ObjectOpenHashMap<String, MojangShaderData> MAP = new Reference2ObjectOpenHashMap<>(64, Hash.VERY_FAST_LOAD_FACTOR);

	public static final MojangShaderData MISSING = of(false, false, CUTOUT_NONE);

	static {
		MAP.put("block", of(false, CUTOUT_NONE));
		MAP.put("new_entity", of(false, CUTOUT_NONE));
		MAP.put("particle", of(true, CUTOUT_TENTH));
		MAP.put("position_color_lightmap", of(false, CUTOUT_NONE));
		MAP.put("position", of(true, CUTOUT_NONE));
		MAP.put("position_color", of(false, CUTOUT_ZERO));
		MAP.put("position_color_tex", of(false, CUTOUT_TENTH));
		MAP.put("position_tex", of(false, CUTOUT_ZERO));
		MAP.put("position_tex_color", of(false, CUTOUT_TENTH));
		MAP.put("position_color_tex_lightmap", of(false, CUTOUT_TENTH));
		MAP.put("position_tex_color_normal", of(true, CUTOUT_TENTH));
		MAP.put("position_tex_lightmap_color", of(false, CUTOUT_NONE));
		MAP.put("rendertype_solid", of(true, CUTOUT_NONE, true));
		MAP.put("rendertype_cutout_mipped", of(true, CUTOUT_HALF, true));
		MAP.put("rendertype_cutout", of(true, CUTOUT_TENTH, true));
		MAP.put("rendertype_translucent", of(true, CUTOUT_NONE, true));
		MAP.put("rendertype_translucent_moving_block", of(false, CUTOUT_NONE));
		MAP.put("rendertype_translucent_no_crumbling", of(false, CUTOUT_NONE));
		MAP.put("rendertype_armor_cutout_no_cull", of(true, CUTOUT_TENTH, true));
		MAP.put("rendertype_entity_solid", of(true, CUTOUT_NONE, true));
		MAP.put("rendertype_entity_cutout", of(true, CUTOUT_TENTH, true));
		MAP.put("rendertype_entity_cutout_no_cull", of(true, CUTOUT_TENTH, true));
		MAP.put("rendertype_entity_cutout_no_cull_z_offset", of(true, CUTOUT_TENTH, true));
		MAP.put("rendertype_item_entity_translucent_cull", of(true, CUTOUT_TENTH, true));
		MAP.put("rendertype_entity_translucent_cull", of(true, CUTOUT_TENTH, true));
		MAP.put("rendertype_entity_translucent", of(true, CUTOUT_TENTH, true));
		MAP.put("rendertype_entity_smooth_cutout", of(true, CUTOUT_TENTH, true));
		MAP.put("rendertype_beacon_beam", of(false, CUTOUT_NONE));
		MAP.put("rendertype_entity_decal", of(true, CUTOUT_TENTH, true));
		MAP.put("rendertype_entity_no_outline", of(true, CUTOUT_NONE, true));
		MAP.put("rendertype_entity_shadow", of(true, CUTOUT_NONE));
		MAP.put("rendertype_entity_alpha", of(false, CUTOUT_ALPHA));
		MAP.put("rendertype_eyes", of(false, CUTOUT_NONE));
		MAP.put("rendertype_energy_swirl", of(false, CUTOUT_TENTH));
		MAP.put("rendertype_leash", of(true, CUTOUT_NONE));
		MAP.put("rendertype_water_mask", of(false, CUTOUT_NONE));
		MAP.put("rendertype_outline", of(false, CUTOUT_ZERO));
		MAP.put("rendertype_armor_glint", of(false, false, CUTOUT_TENTH));
		MAP.put("rendertype_armor_entity_glint", of(false, false, CUTOUT_TENTH));
		MAP.put("rendertype_glint_translucent", of(false, false, CUTOUT_TENTH));
		MAP.put("rendertype_glint", of(false, false, CUTOUT_TENTH));
		MAP.put("rendertype_glint_direct", of(false, false, CUTOUT_TENTH));
		MAP.put("rendertype_entity_glint", of(false, false, CUTOUT_TENTH));
		MAP.put("rendertype_entity_glint_direct", of(false, false, CUTOUT_TENTH));
		MAP.put("rendertype_text", of(true, CUTOUT_TENTH));
		MAP.put("rendertype_text_see_through", of(false, CUTOUT_TENTH));
		MAP.put("rendertype_crumbling", of(false, CUTOUT_TENTH));
		MAP.put("rendertype_end_gateway", of(false, CUTOUT_NONE));
		MAP.put("rendertype_end_portal", of(true, CUTOUT_TENTH));
		MAP.put("rendertype_lightning", of(false, CUTOUT_NONE));
		MAP.put("rendertype_lines", of(true, CUTOUT_NONE));
		MAP.put("rendertype_tripwire", of(false, CUTOUT_TENTH));
	}

	public static MojangShaderData get(String shader) {
		return MAP.getOrDefault(shader, MISSING);
	}

	private static MojangShaderData of(boolean fog, int cutout) {
		return new MojangShaderData(fog, false, cutout, false);
	}

	private static MojangShaderData of(boolean fog, int cutout, boolean diffuse) {
		return new MojangShaderData(fog, false, cutout, diffuse);
	}

	private static MojangShaderData of(boolean fog, boolean glint, int cutout) {
		return new MojangShaderData(fog, glint, cutout, false);
	}
}
