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
