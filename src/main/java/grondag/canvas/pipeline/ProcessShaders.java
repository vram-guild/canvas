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
import grondag.canvas.shader.GlProgram.Uniform2fImpl;
import grondag.canvas.shader.GlProgram.Uniform2iImpl;
import grondag.canvas.shader.GlShader;
import grondag.canvas.shader.GlShaderManager;
import grondag.canvas.shader.ShaderContext;
import grondag.canvas.shader.ShaderData;
import grondag.frex.api.material.UniformRefreshFrequency;

public class ProcessShaders {
	private static GlProgram copy;
	private static GlProgram blur;

	static Uniform2iImpl copySize;
	static Uniform2iImpl blurSize;
	static Uniform2fImpl blurDist;

	static {
		reload();
	}

	public static void reload() {
		if (copy != null) {
			copy.unload();
			copy = null;
		}

		if (blur != null) {
			blur.unload();
			blur = null;
		}

		GlShader vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(ShaderData.COPY_VERTEX, ShaderContext.PROCESS);
		GlShader fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(ShaderData.COPY_FRAGMENT, ShaderContext.PROCESS);
		copy = new GlProgram(vs, fs, MaterialVertexFormats.PROCESS_VERTEX_UV, ShaderContext.PROCESS);
		copy.uniform1i("_cvu_input", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		copySize = (Uniform2iImpl) copy.uniform2i("_cvu_size", UniformRefreshFrequency.ON_LOAD, u -> {});
		copy.load();

		vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(ShaderData.BLUR_VERTEX, ShaderContext.PROCESS);
		fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(ShaderData.BLUR_FRAGMENT, ShaderContext.PROCESS);
		blur = new GlProgram(vs, fs, MaterialVertexFormats.PROCESS_VERTEX_UV, ShaderContext.PROCESS);
		blur.uniform1i("_cvu_input", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		blurSize = (Uniform2iImpl) blur.uniform2i("_cvu_size", UniformRefreshFrequency.ON_LOAD, u -> {});
		blurDist = (Uniform2fImpl) blur.uniform2f("_cvu_distance", UniformRefreshFrequency.ON_LOAD, u -> {});
		blur.load();
	}

	public static GlProgram copy(int width, int height) {
		assert width > 0;
		assert height > 0;

		copySize.set(width, height);
		return copy;
	}

	/**
	 * Call after program is active
	 */
	public static void copyResize(int width, int height) {
		assert GlProgram.activeProgram() == copy.programId();
		copySize.set(width, height);
		copySize.upload();
	}

	public static GlProgram blur(float dx, float dy, int width, int height) {
		assert width >= 0;
		assert height >= 0;
		blurDist.set(dx, dy);
		blurSize.set(width, height);
		return blur;
	}

	/**
	 * Call after program is active
	 */
	public static void blurResize(float dx, float dy, int width, int height) {
		assert GlProgram.activeProgram() == blur.programId();
		blurDist.set(dx, dy);
		blurDist.upload();
		blurSize.set(width, height);
		blurSize.upload();
	}
}
