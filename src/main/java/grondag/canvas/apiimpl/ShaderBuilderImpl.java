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

import org.joml.Matrix4f;

import net.minecraft.util.Identifier;

import grondag.canvas.apiimpl.material.MaterialShaderImpl;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.MaterialShaderManager;
import grondag.frex.api.material.MaterialShader;
import grondag.frex.api.material.ShaderBuilder;
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

public class ShaderBuilderImpl implements ShaderBuilder {
	private Identifier vertexSource;
	private Identifier fragmentSource;
	private final ArrayList<Consumer<GlProgram>> uniforms = new ArrayList<>();

	@Override
	public MaterialShader build() {
		final MaterialShaderImpl result = MaterialShaderManager.INSTANCE.create(vertexSource, fragmentSource);
		uniforms.forEach(u -> result.addProgramSetup(u));
		vertexSource = null;
		fragmentSource = null;
		uniforms.clear();
		return result;
	}

	@Override
	public ShaderBuilderImpl fragmentSource(Identifier fragmentSource) {
		this.fragmentSource = fragmentSource;
		return this;
	}

	@Override
	public ShaderBuilderImpl vertexSource(Identifier vertexSource) {
		this.vertexSource = vertexSource;
		return this;
	}

	@Override
	public ShaderBuilderImpl uniform1f(String name, UniformRefreshFrequency frequency, Consumer<Uniform1f> initializer) {
		uniforms.add(p -> p.uniform1f(name, frequency, initializer));
		return this;
	}

	@Override
	public ShaderBuilderImpl uniform1i(String name, UniformRefreshFrequency frequency, Consumer<Uniform1i> initializer) {
		uniforms.add(p -> p.uniform1i(name, frequency, initializer));
		return this;
	}

	@Override
	public ShaderBuilderImpl uniform2f(String name, UniformRefreshFrequency frequency, Consumer<Uniform2f> initializer) {
		uniforms.add(p -> p.uniform2f(name, frequency, initializer));
		return this;
	}

	@Override
	public ShaderBuilderImpl uniform2i(String name, UniformRefreshFrequency frequency, Consumer<Uniform2i> initializer) {
		uniforms.add(p -> p.uniform2i(name, frequency, initializer));
		return this;
	}

	@Override
	public ShaderBuilderImpl uniform3f(String name, UniformRefreshFrequency frequency, Consumer<Uniform3f> initializer) {
		uniforms.add(p -> p.uniform3f(name, frequency, initializer));
		return this;
	}

	@Override
	public ShaderBuilderImpl uniform3i(String name, UniformRefreshFrequency frequency, Consumer<Uniform3i> initializer) {
		uniforms.add(p -> p.uniform3i(name, frequency, initializer));
		return this;
	}

	@Override
	public ShaderBuilderImpl uniform4f(String name, UniformRefreshFrequency frequency, Consumer<Uniform4f> initializer) {
		uniforms.add(p -> p.uniform4f(name, frequency, initializer));
		return this;
	}

	@Override
	public ShaderBuilderImpl uniform4i(String name, UniformRefreshFrequency frequency, Consumer<Uniform4i> initializer) {
		uniforms.add(p -> p.uniform4i(name, frequency, initializer));
		return this;
	}


	@Override
	public ShaderBuilderImpl uniformArrayf(String name, UniformRefreshFrequency frequency, Consumer<UniformArrayf> initializer, int size) {
		uniforms.add(p -> p.uniformArrayf(name, frequency, initializer, size));
		return this;
	}

	@Override
	public ShaderBuilderImpl uniformArrayi(String name, UniformRefreshFrequency frequency, Consumer<UniformArrayi> initializer, int size) {
		uniforms.add(p -> p.uniformArrayi(name, frequency, initializer, size));
		return this;
	}

	public ShaderBuilderImpl uniformMatrix4f(String name, UniformRefreshFrequency frequency, Consumer<UniformMatrix4f> initializer) {
		uniforms.add(p -> p.uniformMatrix4f(name, frequency, initializer));
		return this;
	}

	@FunctionalInterface
	public interface UniformMatrix4f extends Uniform {
		void set(Matrix4f matrix);
	}
}
