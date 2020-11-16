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

import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.shader.GlProgram.Uniform1fImpl;
import grondag.canvas.shader.GlProgram.Uniform1iImpl;
import grondag.canvas.shader.GlProgram.Uniform2fImpl;
import grondag.canvas.shader.GlProgram.Uniform2iImpl;
import grondag.canvas.shader.GlProgram.UniformMatrix4fImpl;
import grondag.frex.api.material.UniformRefreshFrequency;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;

public class ProcessShader {
	private final Identifier fragmentId;
	private final Identifier vertexId;
	private final String[] samplers;
	private GlProgram program;
	private Uniform2iImpl size;
	private Uniform2fImpl distance;
	private Uniform1iImpl lod;
	private Uniform1fImpl intensity;
	private UniformMatrix4fImpl projection;
	private UniformMatrix4fImpl invProjection;

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
			final Shader vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(vertexId, ProgramType.PROCESS);
			final Shader fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentId, ProgramType.PROCESS);
			program = new GlProgram(vs, fs, CanvasVertexFormats.PROCESS_VERTEX_UV, ProgramType.PROCESS);
			size = (Uniform2iImpl) program.uniform2i("_cvu_size", UniformRefreshFrequency.ON_LOAD, u -> u.set(1, 1));
			lod = (Uniform1iImpl) program.uniform1i("_cvu_lod", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
			distance = (Uniform2fImpl) program.uniform2f("_cvu_distance", UniformRefreshFrequency.ON_LOAD, u -> u.set(0, 0));
			intensity = (Uniform1fImpl) program.uniform1f("cvu_intensity", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
			projection = (UniformMatrix4fImpl) program.uniformMatrix4f("cvu_projection", UniformRefreshFrequency.ON_LOAD, u -> u.set(new Matrix4f()));
			invProjection = (UniformMatrix4fImpl) program.uniformMatrix4f("cvu_inv_projection", UniformRefreshFrequency.ON_LOAD, u -> u.set(new Matrix4f()));

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
		if (program != null && GlProgram.activeProgram() == program) {
			size.set(w, h);
			size.upload();
		}

		return this;
	}

	public ProcessShader distance(float x, float y) {
		if (program != null && GlProgram.activeProgram() == program) {
			distance.set(x, y);
			distance.upload();
		}

		return this;
	}

	public ProcessShader lod(int lod) {
		if (program != null && GlProgram.activeProgram() == program) {
			this.lod.set(lod);
			this.lod.upload();
		}

		return this;
	}

	public ProcessShader intensity(float intensity) {
		if (program != null && GlProgram.activeProgram() == program) {
			this.intensity.set(intensity);
			this.intensity.upload();
		}

		return this;
	}

	public ProcessShader projection(Matrix4f projection) {
		if (program != null && GlProgram.activeProgram() == program) {
			this.projection.set(projection);
			this.projection.upload();
		}

		return this;
	}

	public ProcessShader invProjection(Matrix4f invProjection) {
		if (program != null && GlProgram.activeProgram() == program) {
			this.invProjection.set(invProjection);
			this.invProjection.upload();
		}

		return this;
	}
}
