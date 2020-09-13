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

package grondag.canvas.shader.wip;

import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.GlShaderManager;
import grondag.canvas.shader.Shader;
import grondag.canvas.shader.ShaderContext;
import grondag.canvas.shader.ShaderData;
import grondag.canvas.shader.wip.encoding.WipVertexFormat;

import net.minecraft.util.Identifier;

public enum WipShader {
	DEFAULT_SOLID(ShaderData.DEFAULT_WIP_VERTEX, ShaderData.DEFAULT_WIP_FRAGMENT, ShaderContext.ENTITY_BLOCK_SOLID, WipVertexFormat.POSITION_COLOR_TEXTURE_MATERIAL_LIGHT_NORMAL);

	private final Identifier fragmentId;
	private final Identifier vertexId;
	private final ShaderContext context;
	private GlProgram program;
	private final WipVertexFormat format;

	WipShader(Identifier vertexId, Identifier fragmentId, ShaderContext context, WipVertexFormat format) {
		this.fragmentId = fragmentId;
		this.vertexId = vertexId;
		this.context = context;
		this.format = format;
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
			final Shader vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(vertexId, context);
			final Shader fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentId, context);
			program = new GlProgram(vs, fs, format, context);
			ShaderData.STANDARD_UNIFORM_SETUP.accept(program);
			program.load();
		}

		program.activate();

		return this;
	}
}
