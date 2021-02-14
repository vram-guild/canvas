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

import net.minecraft.util.Identifier;

public enum ProgramType {
	MATERIAL_UNIFORM_LOGIC(false, false, ShaderData.MATERIAL_MAIN_VERTEX, ShaderData.MATERIAL_MAIN_FRAGMENT),
	MATERIAL_VERTEX_LOGIC(true, false, ShaderData.MATERIAL_MAIN_VERTEX, ShaderData.MATERIAL_MAIN_FRAGMENT),
	DEPTH_UNIFORM_LOGIC(false, true, ShaderData.DEPTH_MAIN_VERTEX, ShaderData.DEPTH_MAIN_FRAGMENT),
	DEPTH_VERTEX_LOGIC(true, true, ShaderData.DEPTH_MAIN_VERTEX, ShaderData.DEPTH_MAIN_FRAGMENT),
	PROCESS(false, false, null, null);

	public final String name;
	public final boolean isVertexLogic;
	public final boolean isDepth;
	public final Identifier vertexSource;
	public final Identifier fragmentSource;

	ProgramType(boolean isVertexLogic, boolean isShadow, Identifier vertexSource, Identifier fragmentSource) {
		name = name().toLowerCase();
		this.isVertexLogic = isVertexLogic;
		isDepth = isShadow;
		this.vertexSource = vertexSource;
		this.fragmentSource = fragmentSource;
	}

	public static ProgramType shadowType(ProgramType materialType) {
		if (materialType == MATERIAL_UNIFORM_LOGIC) {
			return DEPTH_UNIFORM_LOGIC;
		} else if (materialType == MATERIAL_VERTEX_LOGIC) {
			return DEPTH_VERTEX_LOGIC;
		} else {
			assert false : "Depth program type requested for unrecognized material program type.";
			return null;
		}
	}
}
