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

package grondag.canvas.shader.data;

import net.minecraft.resources.ResourceLocation;

public final class ShaderStrings {
	public static final ResourceLocation MATERIAL_MAIN_VERTEX = new ResourceLocation("canvas:shaders/internal/material_main.vert");
	public static final ResourceLocation MATERIAL_MAIN_FRAGMENT = new ResourceLocation("canvas:shaders/internal/material_main.frag");

	public static final ResourceLocation DEPTH_MAIN_VERTEX = new ResourceLocation("canvas:shaders/internal/shadow_main.vert");
	public static final ResourceLocation DEPTH_MAIN_FRAGMENT = new ResourceLocation("canvas:shaders/internal/shadow_main.frag");

	public static final String API_TARGET = "#include canvas:apitarget";
	public static final String FRAGMENT_START = "#include canvas:startfragment";
	public static final String VERTEX_START = "#include canvas:startvertex";

	private ShaderStrings() { }
}
