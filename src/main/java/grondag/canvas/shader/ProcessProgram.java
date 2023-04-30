/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.shader;

import org.joml.Matrix4f;

import net.minecraft.resources.ResourceLocation;

import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.shader.data.UniformRefreshFrequency;

public class ProcessProgram extends GlProgram {

	private final Uniform2i size;
	private final Uniform1i lod;
	private final Uniform1i layer;
	private final UniformMatrix4f projMatrix;

	public ProcessProgram(String name, ResourceLocation vertexId, ResourceLocation fragmentId, String... samplers) {
		super(
			name,
			GlShaderManager.INSTANCE.getOrCreateVertexShader(vertexId, ProgramType.PROCESS),
			GlShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentId, ProgramType.PROCESS),
			CanvasVertexFormats.PROCESS_VERTEX_UV,
			ProgramType.PROCESS
		);

		this.size = uniform2i("frxu_size", UniformRefreshFrequency.ON_LOAD, u -> u.set(1, 1));
		this.lod = uniform1i("frxu_lod", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		this.layer = uniform1i("frxu_layer", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		this.projMatrix = uniformMatrix4f("frxu_frameProjectionMatrix", UniformRefreshFrequency.ON_LOAD, u -> { });

		int tex = 0;
		for (final String samplerName : samplers) {
			final int n = tex++;
			final String samplerType = SamplerTypeHelper.getSamplerType(this, samplerName);
			uniformSampler(samplerType, samplerName, UniformRefreshFrequency.ON_LOAD, u -> u.set(n));
		}
	}

	/** Unloads if non-null and always returns null. */
	public static ProcessProgram unload(ProcessProgram shader) {
		if (shader != null) {
			shader.unload();
		}

		return null;
	}

	public ProcessProgram size(int w, int h) {
		if (GlProgram.activeProgram() == this) {
			size.set(w, h);
			size.upload();
		}

		return this;
	}

	public ProcessProgram lod(int lod) {
		if (GlProgram.activeProgram() == this) {
			this.lod.set(lod);
			this.lod.upload();
		}

		return this;
	}

	public ProcessProgram layer(int layer) {
		if (GlProgram.activeProgram() == this) {
			this.layer.set(layer);
			this.layer.upload();
		}

		return this;
	}

	public ProcessProgram projection(Matrix4f matrix) {
		if (GlProgram.activeProgram() == this) {
			projMatrix.set(matrix);
			projMatrix.upload();
		}

		return this;
	}
}
