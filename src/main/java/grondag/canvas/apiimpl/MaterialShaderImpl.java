/*******************************************************************************
 * Copyright 2019 grondag
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

package grondag.canvas.apiimpl;

import java.util.ArrayList;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.joml.Matrix4f;

import net.minecraft.util.Identifier;

import grondag.canvas.material.GlFragmentShader;
import grondag.canvas.material.GlProgram;
import grondag.canvas.material.GlShaderManager;
import grondag.canvas.material.GlVertexShader;
import grondag.canvas.material.ShaderContext;
import grondag.canvas.material.ShaderProps;
import grondag.frex.api.material.MaterialShader;
import grondag.frex.api.material.Uniform;
import grondag.frex.api.material.Uniform.Uniform1f;
import grondag.frex.api.material.Uniform.Uniform1i;
import grondag.frex.api.material.Uniform.Uniform2f;
import grondag.frex.api.material.Uniform.Uniform2i;
import grondag.frex.api.material.Uniform.Uniform3f;
import grondag.frex.api.material.Uniform.Uniform3i;
import grondag.frex.api.material.Uniform.Uniform4f;
import grondag.frex.api.material.Uniform.Uniform4i;
import grondag.frex.api.material.Uniform.UniformArrayf;
import grondag.frex.api.material.Uniform.UniformArrayi;
import grondag.frex.api.material.UniformRefreshFrequency;

public final class MaterialShaderImpl implements MaterialShader {
	private final int index;
	private final Identifier vertexShader;
	private final Identifier fragmentShader;
	private final ArrayList<Consumer<GlProgram>> uniforms = new ArrayList<>();

	private final Int2ObjectOpenHashMap<GlProgram> programMap = new Int2ObjectOpenHashMap<>();
	// list is for fast, no-alloc iteration
	private final ObjectArrayList<GlProgram> programList = new ObjectArrayList<>();

	public MaterialShaderImpl(int index, Identifier vertexShader, Identifier fragmentShader) {
		this.vertexShader = vertexShader;
		this.fragmentShader = fragmentShader;
		this.index = index;
	}

	private static int key(ShaderContext context, int shaderProps) {
		return context.ordinal() | (shaderProps << 3);
	}

	private GlProgram getOrCreate(ShaderContext context, int shaderProps) {
		final int key = key(context, shaderProps);
		final GlProgram result = programMap.get(key);
		if(result == null) {
			final int spriteDepth = ShaderProps.spriteDepth(shaderProps);
			assert spriteDepth > 0;
			final GlVertexShader vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, shaderProps, context);
			final GlFragmentShader fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader, shaderProps, context);
			final GlProgram newProgram = new GlProgram(vs, fs, shaderProps, true);
			uniforms.forEach(u -> u.accept(newProgram));
			newProgram.load();
			programMap.put(key, newProgram);
			programList.add(newProgram);
			return newProgram;
		} else {
			return result;
		}
	}

	public void activate(ShaderContext context, int shaderProps) {
		getOrCreate(context, shaderProps).activate();
	}

	public void forceReload() {
		programList.clear();
		programMap.clear();
	}

	@Override
	@Deprecated
	public int spriteDepth() {
		return 1;
	}

	public int getIndex() {
		return index;
	}

	public void uniformSampler2d(String name, UniformRefreshFrequency frequency, Consumer<Uniform1i> initializer) {
		uniforms.add(p -> {
			if (p.containsUniformSpec("sampler2D", name)) {
				p.uniform1i(name, frequency, initializer);
			}
		});
	}

	public void uniform1f(String name, UniformRefreshFrequency frequency, Consumer<Uniform1f> initializer) {
		uniforms.add(p -> {
			if (p.containsUniformSpec("float", name)) {
				p.uniform1f(name, frequency, initializer);
			}
		});
	}

	public void uniform2f(String name, UniformRefreshFrequency frequency, Consumer<Uniform2f> initializer) {
		uniforms.add(p -> {
			if (p.containsUniformSpec("vec2", name)) {
				p.uniform2f(name, frequency, initializer);
			}
		});
	}

	public void uniform3f(String name, UniformRefreshFrequency frequency, Consumer<Uniform3f> initializer) {
		uniforms.add(p -> {
			if (p.containsUniformSpec("vec3", name)) {
				p.uniform3f(name, frequency, initializer);
			}
		});
	}

	public void uniform4f(String name, UniformRefreshFrequency frequency, Consumer<Uniform4f> initializer) {
		uniforms.add(p -> {
			if (p.containsUniformSpec("vec4", name)) {
				p.uniform4f(name, frequency, initializer);
			}
		});
	}

	public void uniform1i(String name, UniformRefreshFrequency frequency, Consumer<Uniform1i> initializer) {
		uniforms.add(p -> {
			if (p.containsUniformSpec("int", name)) {
				p.uniform1i(name, frequency, initializer);
			}
		});
	}

	public void uniform2i(String name, UniformRefreshFrequency frequency, Consumer<Uniform2i> initializer) {
		uniforms.add(p -> {
			if (p.containsUniformSpec("ivec2", name)) {
				p.uniform2i(name, frequency, initializer);
			}
		});
	}

	public void uniform3i(String name, UniformRefreshFrequency frequency, Consumer<Uniform3i> initializer) {
		uniforms.add(p -> {
			if (p.containsUniformSpec("ivec3", name)) {
				p.uniform3i(name, frequency, initializer);
			}
		});
	}

	public void uniform4i(String name, UniformRefreshFrequency frequency, Consumer<Uniform4i> initializer) {
		uniforms.add(p -> {
			if (p.containsUniformSpec("ivec4", name)) {
				p.uniform4i(name, frequency, initializer);
			}
		});
	}

	public void uniformArrayf(String name, UniformRefreshFrequency frequency, Consumer<UniformArrayf> initializer, int size) {
		uniforms.add(p -> {
			if (p.containsUniformSpec("float\\s*\\[\\s*[0-9]+\\s*]", name)) {
				p.uniformArrayf(name, frequency, initializer, size);
			}
		});
	}

	public void uniformArrayi(String name, UniformRefreshFrequency frequency, Consumer<UniformArrayi> initializer, int size) {
		uniforms.add(p -> {
			if (p.containsUniformSpec("int\\s*\\[\\s*[0-9]+\\s*]", name)) {
				p.uniformArrayi(name, frequency, initializer, size);
			}
		});
	}

	@FunctionalInterface
	public interface UniformMatrix4f extends Uniform {
		void set(Matrix4f matrix);
	}

	public void uniformMatrix4f(String name, UniformRefreshFrequency frequency, Consumer<UniformMatrix4f> initializer) {
		uniforms.add(p -> {
			if (p.containsUniformSpec("mat4", name)) {
				p.uniformMatrix4f(name, frequency, initializer);
			}
		});
	}

	//PERF: hmmm....
	public void onRenderTick() {
		final int limit = programList.size();
		for(int i = 0; i < limit; i++) {
			programList.get(i).onRenderTick();
		}
	}

	//PERF: hmmm....
	public void onGameTick() {
		final int limit = programList.size();
		for(int i = 0; i < limit; i++) {
			programList.get(i).onGameTick();
		}
	}
}
