/*******************************************************************************
 * Copyright 2020 grondag
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
 ******************************************************************************/

package grondag.canvas.pipeline;

import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.GlProgram.Uniform1iImpl;
import grondag.canvas.shader.GlShader;
import grondag.canvas.shader.GlShaderManager;
import grondag.canvas.shader.ShaderContext;
import grondag.canvas.shader.ShaderData;
import grondag.frex.api.material.UniformRefreshFrequency;

public class ProcessShaders {
	private static GlProgram copy;

	static Uniform1iImpl copyWidth;
	static Uniform1iImpl copyHeight;

	/**
	 * Call after program is active
	 */
	public static void copyResize(int width, int height) {
		assert GlProgram.activeProgram() == copy.programId();
		copyWidth.set(width);
		copyWidth.upload();
		copyHeight.set(height);
		copyHeight.upload();
	}

	public static GlProgram copy(int width, int height) {
		assert width > 0;
		assert height > 0;

		copyWidth.set(width);
		copyHeight.set(height);
		return copy;
	}

	static {
		reload();
	}

	public static void reload() {
		if (copy != null) {
			copy.unload();
			copy = null;
		}

		final GlShader vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(ShaderData.COPY_VERTEX, ShaderContext.PROCESS);
		final GlShader fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(ShaderData.COPY_FRAGMENT, ShaderContext.PROCESS);
		copy = new GlProgram(vs, fs, MaterialVertexFormats.PROCESS_VERTEX_UV, ShaderContext.PROCESS);
		copy.uniform1i("_cvu_input", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		copyWidth = (Uniform1iImpl) copy.uniform1i("_cvu_width", UniformRefreshFrequency.ON_LOAD, u -> {});
		copyHeight = (Uniform1iImpl) copy.uniform1i("_cvu_height", UniformRefreshFrequency.ON_LOAD, u -> {});
		copy.load();
	}
}
