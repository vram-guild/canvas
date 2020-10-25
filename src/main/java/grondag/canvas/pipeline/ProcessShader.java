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

package grondag.canvas.pipeline;

import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.shader.GlShaderManager;
import grondag.canvas.shader.Shader;
import grondag.canvas.wip.shader.WipGlProgram;
import grondag.canvas.wip.shader.WipGlProgram.Uniform1fImpl;
import grondag.canvas.wip.shader.WipGlProgram.Uniform1iImpl;
import grondag.canvas.wip.shader.WipGlProgram.Uniform2fImpl;
import grondag.canvas.wip.shader.WipGlProgram.Uniform2iImpl;
import grondag.canvas.wip.state.WipProgramType;
import grondag.frex.api.material.UniformRefreshFrequency;

import net.minecraft.util.Identifier;

public class ProcessShader {
	private final Identifier fragmentId;
	private final Identifier vertexId;
	private final String[] samplers;
	private WipGlProgram program;
	private Uniform2iImpl size;
	private Uniform2fImpl distance;
	private Uniform1iImpl lod;
	private Uniform1fImpl intensity;

	ProcessShader(Identifier vertexId, Identifier fragmentId, String... samplers) {
		this.fragmentId = fragmentId;
		this.vertexId = vertexId;
		this.samplers = samplers;
	}

	void unload() {
		if (program != null) {
			program.unload();
			program = null;
		}
	}

	public ProcessShader activate() {
		if (program == null) {
			final Shader vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(vertexId, WipProgramType.PROCESS);
			final Shader fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentId, WipProgramType.PROCESS);
			program = new WipGlProgram(vs, fs, MaterialVertexFormats.PROCESS_VERTEX_UV, WipProgramType.PROCESS);
			size = (Uniform2iImpl) program.uniform2i("_cvu_size", UniformRefreshFrequency.ON_LOAD, u -> u.set(1, 1));
			lod = (Uniform1iImpl) program.uniform1i("_cvu_lod", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
			distance = (Uniform2fImpl) program.uniform2f("_cvu_distance", UniformRefreshFrequency.ON_LOAD, u -> u.set(0, 0));
			intensity = (Uniform1fImpl) program.uniform1f("cvu_intensity", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));

			int tex = 0;

			for (final String samplerName : samplers) {
				final int n = tex++;
				program.uniformSampler2d(samplerName, UniformRefreshFrequency.ON_LOAD, u -> u.set(n));
			}

			program.load();
		}

		program.activate();

		return this;
	}

	public ProcessShader size(int w, int h) {
		if (program != null && WipGlProgram.activeProgram() == program) {
			size.set(w, h);
			size.upload();
		}

		return this;
	}

	public ProcessShader distance(float x, float y) {
		if (program != null && WipGlProgram.activeProgram() == program) {
			distance.set(x, y);
			distance.upload();
		}

		return this;
	}

	public ProcessShader lod(int lod) {
		if (program != null && WipGlProgram.activeProgram() == program) {
			this.lod.set(lod);
			this.lod.upload();
		}

		return this;
	}

	public ProcessShader intensity(float intensity) {
		if (program != null && WipGlProgram.activeProgram() == program) {
			this.intensity.set(intensity);
			this.intensity.upload();
		}

		return this;
	}
}
