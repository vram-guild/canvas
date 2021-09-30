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

package grondag.canvas.shader;

import java.util.Locale;

import net.minecraft.resources.ResourceLocation;

import grondag.canvas.shader.data.ShaderStrings;

public enum ProgramType {
	MATERIAL_COLOR(false, true, ShaderStrings.MATERIAL_MAIN_VERTEX, ShaderStrings.MATERIAL_MAIN_FRAGMENT, false),
	MATERIAL_COLOR_TERRAIN(false, true, ShaderStrings.MATERIAL_MAIN_VERTEX, ShaderStrings.MATERIAL_MAIN_FRAGMENT, true),
	MATERIAL_DEPTH(true, true, ShaderStrings.DEPTH_MAIN_VERTEX, ShaderStrings.DEPTH_MAIN_FRAGMENT, false),
	MATERIAL_DEPTH_TERRAIN(true, true, ShaderStrings.DEPTH_MAIN_VERTEX, ShaderStrings.DEPTH_MAIN_FRAGMENT, true),
	PROCESS(false, false, null, null, false);

	public final String name;
	public final boolean isDepth;
	public final boolean hasVertexProgramControl;
	public final ResourceLocation vertexSource;
	public final ResourceLocation fragmentSource;
	public final boolean isTerrain;

	ProgramType(boolean isDepth, boolean hasVertexProgramControl, ResourceLocation vertexSource, ResourceLocation fragmentSource, boolean isTerrain) {
		name = name().toLowerCase(Locale.ROOT);
		this.isDepth = isDepth;
		this.hasVertexProgramControl = hasVertexProgramControl;
		this.vertexSource = vertexSource;
		this.fragmentSource = fragmentSource;
		this.isTerrain = isTerrain;
	}
}
