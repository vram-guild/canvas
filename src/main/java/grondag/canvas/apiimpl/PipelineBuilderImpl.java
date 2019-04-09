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

import grondag.canvas.pipeline.PipelineManager;
import grondag.frex.api.extended.Pipeline;
import grondag.frex.api.extended.PipelineBuilder;
import grondag.frex.api.extended.Uniform.Uniform1f;
import grondag.frex.api.extended.Uniform.Uniform1i;
import grondag.frex.api.extended.Uniform.Uniform2f;
import grondag.frex.api.extended.Uniform.Uniform2i;
import grondag.frex.api.extended.Uniform.Uniform3f;
import grondag.frex.api.extended.Uniform.Uniform3i;
import grondag.frex.api.extended.Uniform.Uniform4f;
import grondag.frex.api.extended.Uniform.Uniform4i;
import grondag.frex.api.extended.Uniform.UniformMatrix4f;
import grondag.frex.api.extended.UniformRefreshFrequency;
import net.minecraft.util.Identifier;

public class PipelineBuilderImpl implements PipelineBuilder {
    private Identifier vertexSource;
    private Identifier fragmentSource;
    private final ArrayList<Consumer<RenderPipelineImpl>> uniforms = new ArrayList<>();
    
    private int spriteDepth = 1;
    
    @Override
    public Pipeline build() {
        RenderPipelineImpl result = PipelineManager.INSTANCE.createPipeline(spriteDepth, vertexSource, fragmentSource);
        uniforms.forEach(u -> u.accept(result));
        vertexSource = null;
        fragmentSource = null;
        uniforms.clear();
        return result;
    }

    @Override
    public PipelineBuilderImpl fragmentSource(Identifier fragmentSource) {
        this.fragmentSource = fragmentSource;
        return this;
    }

    @Override
    public PipelineBuilderImpl vertexSource(Identifier vertexSource) {
        this.vertexSource = vertexSource;
        return this;
    }

    @Override
    public PipelineBuilderImpl spriteDepth(int depth) {
        this.spriteDepth = depth;
        return this;
    }
    
    @Override
    public PipelineBuilderImpl uniform1f(String name, UniformRefreshFrequency frequency, Consumer<Uniform1f> initializer) {
        uniforms.add(p -> p.uniform1f(name, frequency, initializer));
        return this;
    }

    @Override
    public PipelineBuilderImpl uniform1i(String name, UniformRefreshFrequency frequency, Consumer<Uniform1i> initializer) {
        uniforms.add(p -> p.uniform1i(name, frequency, initializer));
        return this;
    }

    @Override
    public PipelineBuilderImpl uniform2f(String name, UniformRefreshFrequency frequency, Consumer<Uniform2f> initializer) {
        uniforms.add(p -> p.uniform2f(name, frequency, initializer));
        return this;
    }

    @Override
    public PipelineBuilderImpl uniform2i(String name, UniformRefreshFrequency frequency, Consumer<Uniform2i> initializer) {
        uniforms.add(p -> p.uniform2i(name, frequency, initializer));
        return this;
    }

    @Override
    public PipelineBuilderImpl uniform3f(String name, UniformRefreshFrequency frequency, Consumer<Uniform3f> initializer) {
        uniforms.add(p -> p.uniform3f(name, frequency, initializer));
        return this;
    }

    @Override
    public PipelineBuilderImpl uniform3i(String name, UniformRefreshFrequency frequency, Consumer<Uniform3i> initializer) {
        uniforms.add(p -> p.uniform3i(name, frequency, initializer));
        return this;
    }

    @Override
    public PipelineBuilderImpl uniform4f(String name, UniformRefreshFrequency frequency, Consumer<Uniform4f> initializer) {
        uniforms.add(p -> p.uniform4f(name, frequency, initializer));
        return this;
    }

    @Override
    public PipelineBuilderImpl uniform4i(String name, UniformRefreshFrequency frequency, Consumer<Uniform4i> initializer) {
        uniforms.add(p -> p.uniform4i(name, frequency, initializer));
        return this;
    }

    @Override
    public PipelineBuilderImpl uniformMatrix4f(String name, UniformRefreshFrequency frequency, Consumer<UniformMatrix4f> initializer) {
        uniforms.add(p -> p.uniformMatrix4f(name, frequency, initializer));
        return this;
    }
}
