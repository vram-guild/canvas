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

package grondag.canvas.wip.shader;

import grondag.canvas.wip.encoding.WipVertexFormat;
import grondag.canvas.wip.state.WipProgramType;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL21;

import net.minecraft.util.Identifier;

public class WipGlMaterialShader extends WipGlShader{
	private final ObjectOpenHashSet<WipMaterialShaderImpl> materials;

	WipGlMaterialShader(Identifier shaderSource, int shaderType, WipProgramType programType, WipVertexFormat format, ObjectOpenHashSet<WipMaterialShaderImpl> materials) {
		super(shaderSource, shaderType, programType, format);
		this.materials = materials;
	}

	// all material shaders use the same source so only append extension to keep debug source file names of reasonable length
	@Override
	protected String debugSourceString() {
		return shaderType == GL21.GL_FRAGMENT_SHADER ? ".frag" : ".vert";
	}

	@Override
	protected String preprocessSource(String baseSource) {
		baseSource = StringUtils.replace(baseSource, WipShaderData.API_TARGET, implementationSources());

		if (shaderType == GL21.GL_FRAGMENT_SHADER) {
			baseSource = StringUtils.replace(baseSource, WipShaderData.FRAGMENT_START, fragmentStartSource());
		} else {
			baseSource = StringUtils.replace(baseSource, WipShaderData.VERTEX_START, vertexStartSource());
			baseSource = StringUtils.replace(baseSource, WipShaderData.VEREX_END,  vertexEndSource());
		}

		return baseSource;
	}

	private String fragmentStartSource() {
		// WIP generate switch/if-else from materials
		// current program ID uniform/varying holding material id is insufficient for
		// efficient selection because materials can share the same source
		return "";
	}

	private String vertexStartSource() {
		// WIP generate switch/if-else from materials

		/* example

		if (cv_programId == 0) { frx_startVertex0(data) }
		else if (cv_programId == 1) { frx_startVertex1(data)}
		else if (cv_programId == 2) { frx_startVertex2(data)};

		 */

		return "";
	}

	private String vertexEndSource() {
		// WIP generate switch/if-else from materials
		return "";
	}

	private String implementationSources() {
		// WIP generate relabeled methods from materials

		/* example

		void frx_startVertex0(inout frx_VertexData data) {

		}

		 */

		return "";
	}
}
