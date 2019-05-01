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

import grondag.canvas.material.GlFragmentShader;
import grondag.canvas.material.GlProgram;
import grondag.canvas.material.GlShaderManager;
import grondag.canvas.material.GlVertexShader;
import grondag.canvas.material.MaterialShaderManager;
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.material.ShaderContext;
import grondag.frex.api.material.MaterialShader;
import grondag.frex.api.material.Uniform.Uniform1f;
import grondag.frex.api.material.Uniform.Uniform1i;
import grondag.frex.api.material.Uniform.Uniform2f;
import grondag.frex.api.material.Uniform.Uniform2i;
import grondag.frex.api.material.Uniform.Uniform3f;
import grondag.frex.api.material.Uniform.Uniform3i;
import grondag.frex.api.material.Uniform.Uniform4f;
import grondag.frex.api.material.Uniform.Uniform4i;
import grondag.frex.api.material.Uniform.UniformMatrix4f;
import grondag.frex.api.material.UniformRefreshFrequency;
import net.minecraft.util.Identifier;

public final class MaterialShaderImpl implements MaterialShader {
    private final int index;
    private GlProgram solidProgram;
    private GlProgram translucentProgram;
    private GlProgram itemProgram;
    private GlProgram guiProgram;
    private final Identifier vertexShader;
    private final Identifier fragmentShader;
    private final ArrayList<Consumer<GlProgram>> uniforms = new ArrayList<>();
    
    public final int spriteDepth;
    private MaterialVertexFormat vertexFormat;

    public MaterialShaderImpl(int index, Identifier vertexShader, Identifier fragmentShader, int spriteDepth) {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.index = index;
        this.spriteDepth = spriteDepth;
        vertexFormat = MaterialShaderManager.FORMATS[spriteDepth - 1];
    }

    //PERF - could be better
    public void activate(ShaderContext context, int shaderProps) {
        switch(context) {
        case BLOCK_SOLID:
            solidProgram.activate();
            break;
            
        case BLOCK_TRANSLUCENT:
            translucentProgram.activate();
            break;
            
        case ITEM_WORLD:
            itemProgram.activate();
            break;
            
        case ITEM_GUI:
            guiProgram.activate();
            break;
            
        default:
            break;
        }
    }

    public void forceReload() {
        GlVertexShader vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, spriteDepth, ShaderContext.BLOCK_SOLID);
        GlFragmentShader fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader, spriteDepth, ShaderContext.BLOCK_SOLID);
        solidProgram = new GlProgram(vs, fs, spriteDepth, true);
        uniforms.forEach(u -> u.accept(solidProgram));
        solidProgram.load();
        
        vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, spriteDepth, ShaderContext.BLOCK_TRANSLUCENT);
        fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader, spriteDepth, ShaderContext.BLOCK_TRANSLUCENT);
        translucentProgram = new GlProgram(vs, fs, spriteDepth, false);
        uniforms.forEach(u -> u.accept(translucentProgram));
        translucentProgram.load();
        
        vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, spriteDepth, ShaderContext.ITEM_WORLD);
        fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader, spriteDepth, ShaderContext.ITEM_WORLD);
        itemProgram = new GlProgram(vs, fs, spriteDepth, false);
        uniforms.forEach(u -> u.accept(itemProgram));
        itemProgram.load();
        
        vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, spriteDepth, ShaderContext.ITEM_GUI);
        fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader, spriteDepth, ShaderContext.ITEM_GUI);
        guiProgram = new GlProgram(vs, fs, spriteDepth, false);
        uniforms.forEach(u -> u.accept(guiProgram));
        guiProgram.load();
    }

    @Override
    public int spriteDepth() {
        return this.spriteDepth;
    }

    public MaterialVertexFormat piplineVertexFormat(int shaderProps) {
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

    //PERF: hmmm....
    public void onRenderTick() {
        this.solidProgram.onRenderTick();
        this.translucentProgram.onRenderTick();
        this.itemProgram.onRenderTick();
        this.guiProgram.onRenderTick();
    }

    //PERF: hmmm....
    public void onGameTick() {
        // can be called before programs are initialized
        if(this.solidProgram != null) {
            this.solidProgram.onGameTick();
            this.translucentProgram.onGameTick();
            this.itemProgram.onGameTick();
            this.guiProgram.onGameTick();
        }
    }
}
