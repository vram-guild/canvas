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

import grondag.canvas.shader.Shader;
import grondag.canvas.wip.encoding.WipVertexFormat;
import grondag.canvas.wip.state.WipProgramType;

import net.minecraft.util.Identifier;

public enum WipShader {
	;

	private final Identifier fragmentId;
	private final Identifier vertexId;
	private final WipProgramType programType;
	private WipGlProgram program;
	private final WipVertexFormat format;

	WipShader(Identifier vertexId, Identifier fragmentId, WipVertexFormat format, WipProgramType programType) {
		this.fragmentId = fragmentId;
		this.vertexId = vertexId;
		this.format = format;
		this.programType = programType;
	}

	public static void reload() {
		for (final WipShader s : values()) {
			s.unload();
		}
	}

	void unload() {
		if (program != null) {
			program.unload();
			program = null;
		}
	}

	public WipShader activate() {
		if (program == null) {
			final Shader vs = WipGlShaderManager.INSTANCE.getOrCreateVertexShader(vertexId, programType, format);
			final Shader fs = WipGlShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentId, programType, format);
			program = new WipGlProgram(vs, fs, format, programType);
			WipShaderData.STANDARD_UNIFORM_SETUP.accept(program);
			program.load();
		}

		program.activate();

		return this;
	}
}
