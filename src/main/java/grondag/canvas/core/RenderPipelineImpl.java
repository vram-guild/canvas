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

package grondag.canvas.core;

import java.util.ArrayList;
import java.util.function.Consumer;

import grondag.frex.api.extended.Pipeline;
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
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;

public final class RenderPipelineImpl implements Pipeline {
    private final int index;
    private Program solidProgram;
    private Program translucentProgram;
    private final Identifier vertexShader;
    private final Identifier fragmentShader;
    private final ArrayList<Consumer<Program>> uniforms = new ArrayList<>();
    
    public final int spriteDepth;
    private PipelineVertexFormat pipelineVertexFormat;
    private VertexFormat vertexFormat;

    RenderPipelineImpl(int index, Identifier vertexShader, Identifier fragmentShader, int spriteDepth) {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.index = index;
        this.spriteDepth = spriteDepth;
        pipelineVertexFormat = PipelineManager.FORMATS[spriteDepth - 1];
        vertexFormat = pipelineVertexFormat.vertexFormat;
    }

    public void activate(boolean isSolidLayer) {
        if (isSolidLayer)
            this.solidProgram.activate();
        else
            this.translucentProgram.activate();
    }

    public void forceReload() {
        PipelineVertexShader vs = PipelineShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, spriteDepth,
                true);
        PipelineFragmentShader fs = PipelineShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader,
                spriteDepth, true);
        solidProgram = new Program(vs, fs, spriteDepth, true);
        uniforms.forEach(u -> u.accept(solidProgram));
        solidProgram.load();
        
        vs = PipelineShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, spriteDepth, false);
        fs = PipelineShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader, spriteDepth, false);
        translucentProgram = new Program(vs, fs, spriteDepth, false);
        uniforms.forEach(u -> u.accept(translucentProgram));
        translucentProgram.load();
    }

    @Override
    public int spriteDepth() {
        return this.spriteDepth;
    }

    public PipelineVertexFormat piplineVertexFormat() {
        return this.pipelineVertexFormat;
    }

    /**
     * Avoids a pointer chase, more concise code.
     */
    public VertexFormat vertexFormat() {
        return this.vertexFormat;
    }

    public int getIndex() {
        return this.index;
    }

    public void uniformSampler2d(String name, UniformRefreshFrequency frequency, Consumer<Uniform1i> initializer) {
        uniforms.add(p -> {
            if (p.containsUniformSpec("sampler2D", name))
                p.uniform1i(name, frequency, initializer);
        });
    }

    public void uniform1f(String name, UniformRefreshFrequency frequency, Consumer<Uniform1f> initializer) {
        uniforms.add(p -> {
            if (p.containsUniformSpec("float", name))
                p.uniform1f(name, frequency, initializer);
        });
    }

    public void uniform2f(String name, UniformRefreshFrequency frequency, Consumer<Uniform2f> initializer) {
        uniforms.add(p -> {
            if (p.containsUniformSpec("vec2", name))
                p.uniform2f(name, frequency, initializer);
        });
    }

    public void uniform3f(String name, UniformRefreshFrequency frequency, Consumer<Uniform3f> initializer) {
        uniforms.add(p -> {
            if (p.containsUniformSpec("vec3", name))
                p.uniform3f(name, frequency, initializer);
        });
    }

    public void uniform4f(String name, UniformRefreshFrequency frequency, Consumer<Uniform4f> initializer) {
        uniforms.add(p -> {
            if (p.containsUniformSpec("vec4", name))
                p.uniform4f(name, frequency, initializer);
        });
    }

    public void uniform1i(String name, UniformRefreshFrequency frequency, Consumer<Uniform1i> initializer) {
        uniforms.add(p -> {
            if (p.containsUniformSpec("int", name))
                p.uniform1i(name, frequency, initializer);
        });
    }

    public void uniform2i(String name, UniformRefreshFrequency frequency, Consumer<Uniform2i> initializer) {
        uniforms.add(p -> {
            if (p.containsUniformSpec("ivec2", name))
                p.uniform2i(name, frequency, initializer);
        });
    }

    public void uniform3i(String name, UniformRefreshFrequency frequency, Consumer<Uniform3i> initializer) {
        uniforms.add(p -> {
            if (p.containsUniformSpec("ivec3", name))
                p.uniform3i(name, frequency, initializer);
        });
    }

    public void uniform4i(String name, UniformRefreshFrequency frequency, Consumer<Uniform4i> initializer) {
        uniforms.add(p -> {
            if (p.containsUniformSpec("ivec4", name))
                p.uniform4i(name, frequency, initializer);
        });
    }

    public void uniformMatrix4f(String name, UniformRefreshFrequency frequency, Consumer<UniformMatrix4f> initializer) {
        uniforms.add(p -> {
            if (p.containsUniformSpec("mat4", name))
                p.uniformMatrix4f(name, frequency, initializer);
        });
    }

    public void onRenderTick() {
        this.solidProgram.onRenderTick();
        this.translucentProgram.onRenderTick();
    }

    public void onGameTick() {
        // can be called before programs are initialized
        if(this.solidProgram != null) {
            this.solidProgram.onGameTick();
            this.translucentProgram.onGameTick();
        }
    }
}
