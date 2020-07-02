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
	private static GlProgram bloom;
	private static GlProgram bloomSample;
	private static GlProgram copyLod;
	private static GlProgram blurLod;

	private static Uniform2iImpl copySize;
	private static Uniform2iImpl copyLodSize;
	private static Uniform1iImpl copyLodLod;
	private static Uniform2iImpl blurSize;
	private static Uniform2fImpl blurDist;
	private static Uniform2iImpl bloomSize;
	private static Uniform2iImpl bloomSampleSize;
	private static Uniform2iImpl blurLodSize;
	private static Uniform2fImpl blurLodDist;
	private static Uniform1iImpl blurLodLod;

	static {
		reload();
	}

	public static void reload() {
		if (copy != null) {
			copy.unload();
			copy = null;
		}

		GlShader vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(ShaderData.COPY_VERTEX, ShaderContext.PROCESS);
		GlShader fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(ShaderData.COPY_FRAGMENT, ShaderContext.PROCESS);
		copy = new GlProgram(vs, fs, MaterialVertexFormats.PROCESS_VERTEX_UV, ShaderContext.PROCESS);
		copy.uniform1i("_cvu_input", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		copySize = (Uniform2iImpl) copy.uniform2i("_cvu_size", UniformRefreshFrequency.ON_LOAD, u -> {});
		copy.load();

		if (blur != null) {
			blur.unload();
			blur = null;
		}

		vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(ShaderData.BLUR_VERTEX, ShaderContext.PROCESS);
		fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(ShaderData.BLUR_FRAGMENT, ShaderContext.PROCESS);
		blur = new GlProgram(vs, fs, MaterialVertexFormats.PROCESS_VERTEX_UV, ShaderContext.PROCESS);
		blur.uniform1i("_cvu_input", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		blurSize = (Uniform2iImpl) blur.uniform2i("_cvu_size", UniformRefreshFrequency.ON_LOAD, u -> {});
		blurDist = (Uniform2fImpl) blur.uniform2f("_cvu_distance", UniformRefreshFrequency.ON_LOAD, u -> {});
		blur.load();

		if (bloom != null) {
			bloom.unload();
			bloom = null;
		}

		vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(ShaderData.BLOOM_VERTEX, ShaderContext.PROCESS);
		fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(ShaderData.BLOOM_FRAGMENT, ShaderContext.PROCESS);
		bloom = new GlProgram(vs, fs, MaterialVertexFormats.PROCESS_VERTEX_UV, ShaderContext.PROCESS);
		bloom.uniform1i("_cvu_base", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		bloom.uniform1i("_cvu_bloom0", UniformRefreshFrequency.ON_LOAD, u -> u.set(1));
		bloom.uniform1i("_cvu_bloom1", UniformRefreshFrequency.ON_LOAD, u -> u.set(2));
		bloom.uniform1i("_cvu_bloom2", UniformRefreshFrequency.ON_LOAD, u -> u.set(3));
		bloomSize = (Uniform2iImpl) bloom.uniform2i("_cvu_size", UniformRefreshFrequency.ON_LOAD, u -> {});
		bloom.load();

		if (bloomSample != null) {
			bloomSample.unload();
			bloomSample = null;
		}

		vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(ShaderData.COPY_VERTEX, ShaderContext.PROCESS);
		fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(ShaderData.BLOOM_SAMPLE_FRAGMENT, ShaderContext.PROCESS);
		bloomSample = new GlProgram(vs, fs, MaterialVertexFormats.PROCESS_VERTEX_UV, ShaderContext.PROCESS);
		bloomSample.uniform1i("_cvu_input", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		bloomSampleSize = (Uniform2iImpl) bloomSample.uniform2i("_cvu_size", UniformRefreshFrequency.ON_LOAD, u -> {});
		bloomSample.load();


		if (copyLod != null) {
			copyLod.unload();
			copyLod = null;
		}

		vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(ShaderData.COPY_LOD_VERTEX, ShaderContext.PROCESS);
		fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(ShaderData.COPY_LOD_FRAGMENT, ShaderContext.PROCESS);
		copyLod = new GlProgram(vs, fs, MaterialVertexFormats.PROCESS_VERTEX_UV, ShaderContext.PROCESS);
		copyLod.uniform1i("_cvu_input", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		copyLodSize = (Uniform2iImpl) copyLod.uniform2i("_cvu_size", UniformRefreshFrequency.ON_LOAD, u -> {});
		copyLodLod = (Uniform1iImpl) copyLod.uniform1i("_cvu_lod", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		copyLod.load();

		if (blurLod != null) {
			blurLod.unload();
			blurLod = null;
		}

		vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(ShaderData.BLUR_LOD_VERTEX, ShaderContext.PROCESS);
		fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(ShaderData.BLUR_LOD_FRAGMENT, ShaderContext.PROCESS);
		blurLod = new GlProgram(vs, fs, MaterialVertexFormats.PROCESS_VERTEX_UV, ShaderContext.PROCESS);
		blurLod.uniform1i("_cvu_input", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		blurLodSize = (Uniform2iImpl) blurLod.uniform2i("_cvu_size", UniformRefreshFrequency.ON_LOAD, u -> {});
		blurLodDist = (Uniform2fImpl) blurLod.uniform2f("_cvu_distance", UniformRefreshFrequency.ON_LOAD, u -> {});
		blurLodLod = (Uniform1iImpl) blurLod.uniform1i("_cvu_lod", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		blurLod.load();

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
		//assert GlProgram.activeProgram() == blur.programId();
		blurDist.set(dx, dy);
		blurDist.upload();
		blurSize.set(width, height);
		blurSize.upload();
	}

	public static GlProgram bloom(int width, int height) {
		assert width > 0;
		assert height > 0;

		bloomSize.set(width, height);
		return bloom;
	}

	public static GlProgram bloomSample(int width, int height) {
		assert width > 0;
		assert height > 0;

		bloomSampleSize.set(width, height);
		return bloomSample;
	}

	/**
	 * Call after program is active
	 */
	public static void bloomSampleResize(int width, int height) {
		assert GlProgram.activeProgram() == bloomSample.programId();
		bloomSampleSize.set(width, height);
		bloomSampleSize.upload();
	}

	public static GlProgram copyLod(int width, int height, int lod) {
		assert width > 0;
		assert height > 0;

		copyLodSize.set(width, height);
		copyLodLod.set(lod);
		return copyLod;
	}

	public static void copyLodResize(int width, int height, int lod) {
		assert GlProgram.activeProgram() == copyLod.programId();
		copyLodSize.set(width, height);
		copyLodSize.upload();
		copyLodLod.set(lod);
		copyLodLod.upload();
	}

	public static GlProgram blurLod(float dx, float dy, int width, int height, int lod) {
		assert width >= 0;
		assert height >= 0;
		blurLodDist.set(dx, dy);
		blurLodSize.set(width, height);
		blurLodLod.set(lod);
		return blurLod;
	}

	/**
	 * Call after program is active
	 */
	public static void blurLodResize(float dx, float dy, int width, int height, int lod) {
		//assert GlProgram.activeProgram() == blur.programId();
		blurLodDist.set(dx, dy);
		blurLodDist.upload();
		blurLodSize.set(width, height);
		blurLodSize.upload();
		blurLodLod.set(lod);
		blurLodLod.upload();
	}
}
