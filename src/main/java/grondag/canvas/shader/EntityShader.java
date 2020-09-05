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

package grondag.canvas.shader;

import net.minecraft.util.Identifier;

import grondag.canvas.material.MaterialVertexFormats;

public enum EntityShader {
	DEFAULT_SOLID(ShaderData.DEFAULT_ENTITY_VERTEX, ShaderData.DEFAULT_ENTITY_FRAGMENT, ShaderContext.ENTITY_BLOCK_SOLID);

	private GlProgram program;

	private final Identifier fragmentId;
	private final Identifier vertexId;
	private final ShaderContext context;

	EntityShader(Identifier vertexId, Identifier fragmentId, ShaderContext context) {
		this.fragmentId = fragmentId;
		this.vertexId = vertexId;
		this.context = context;
	}

	void unload() {
		if (program != null) {
			program.unload();
			program = null;
		}
	}

	public EntityShader activate() {
		if (program == null) {
			final GlShader vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(vertexId, context);
			final GlShader fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentId, context);
			program = new GlProgram(vs, fs, MaterialVertexFormats.TEMPORARY_ENTITY_FORMAT, context);
			ShaderData.STANDARD_UNIFORM_SETUP.accept(program);
			program.load();
		}

		program.activate();

		return this;
	}

	public static void reload() {
		for  (final EntityShader s : values()) {
			s.unload();
		}
	}
}
