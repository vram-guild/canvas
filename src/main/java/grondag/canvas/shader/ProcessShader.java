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

import com.mojang.math.Matrix4f;

import net.minecraft.resources.ResourceLocation;

import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.shader.GlProgram.Uniform1i;
import grondag.canvas.shader.GlProgram.Uniform2i;
import grondag.canvas.shader.GlProgram.UniformMatrix4f;
import grondag.canvas.shader.data.UniformRefreshFrequency;

public class ProcessShader {
	private final String name;
	private final ResourceLocation fragmentId;
	private final ResourceLocation vertexId;
	private final String[] samplers;
	private GlProgram program;
	private Uniform2i size;
	private Uniform1i lod;
	private Uniform1i layer;
	private UniformMatrix4f projMatrix;

	public ProcessShader(String name, ResourceLocation vertexId, ResourceLocation fragmentId, String... samplers) {
		this.name = name;
		this.fragmentId = fragmentId;
		this.vertexId = vertexId;
		this.samplers = samplers;
	}

	public int samplerCount() {
		return samplers.length;
	}

	public void unload() {
		if (program != null) {
			program.unload();
			program = null;
		}
	}

	/** Unloads if non-null and always returns null. */
	public static ProcessShader unload(ProcessShader shader) {
		if (shader != null) {
			shader.unload();
		}

		return null;
	}

	public ProcessShader activate() {
		if (program == null) {
			final Shader vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(vertexId, ProgramType.PROCESS);
			final Shader fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentId, ProgramType.PROCESS);
			program = new GlProgram(name, vs, fs, CanvasVertexFormats.PROCESS_VERTEX_UV, ProgramType.PROCESS);
			size = program.uniform2i("frxu_size", UniformRefreshFrequency.ON_LOAD, u -> u.set(1, 1));
			lod = program.uniform1i("frxu_lod", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
			layer = program.uniform1i("frxu_layer", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
			projMatrix = program.uniformMatrix4f("frxu_frameProjectionMatrix", UniformRefreshFrequency.ON_LOAD, u -> { });
			int tex = 0;

			for (final String samplerName : samplers) {
				final int n = tex++;
				final String samplerType = SamplerTypeHelper.getSamplerType(program, samplerName);
				program.uniformSampler(samplerType, samplerName, UniformRefreshFrequency.ON_LOAD, u -> u.set(n));
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

	public ProcessShader lod(int lod) {
		if (program != null && GlProgram.activeProgram() == program) {
			this.lod.set(lod);
			this.lod.upload();
		}

		return this;
	}

	public ProcessShader layer(int layer) {
		if (program != null && GlProgram.activeProgram() == program) {
			this.layer.set(layer);
			this.layer.upload();
		}

		return this;
	}

	public ProcessShader projection(Matrix4f matrix) {
		if (program != null && GlProgram.activeProgram() == program) {
			projMatrix.set(matrix);
			projMatrix.upload();
		}

		return this;
	}
}
