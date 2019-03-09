package grondag.canvas.core;

import java.util.function.Consumer;

import grondag.frex.api.Uniform.Uniform1f;
import grondag.frex.api.Uniform.Uniform1i;
import grondag.frex.api.Uniform.Uniform2f;
import grondag.frex.api.Uniform.Uniform2i;
import grondag.frex.api.Uniform.Uniform3f;
import grondag.frex.api.Uniform.Uniform3i;
import grondag.frex.api.Uniform.Uniform4f;
import grondag.frex.api.Uniform.Uniform4i;
import grondag.frex.api.Uniform.UniformMatrix4f;
import grondag.frex.api.UniformRefreshFrequency;
import net.minecraft.client.render.VertexFormat;

public final class RenderPipeline {
    private final int index;
    private final Program solidProgram;
    private final Program translucentProgram;
    public final int spriteDepth;
    private PipelineVertexFormat pipelineVertexFormat;
    private VertexFormat vertexFormat;

    RenderPipeline(int index, String vertexShader, String fragmentShader, int spriteDepth) {
        PipelineVertexShader vs = PipelineShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, spriteDepth,
                true);
        PipelineFragmentShader fs = PipelineShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader,
                spriteDepth, true);
        this.solidProgram = new Program(vs, fs, spriteDepth, true);

        vs = PipelineShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, spriteDepth, false);
        fs = PipelineShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader, spriteDepth, false);
        this.translucentProgram = new Program(vs, fs, spriteDepth, false);

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
        this.solidProgram.forceReload();
        this.translucentProgram.forceReload();
    }

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
        if (solidProgram.containsUniformSpec("sampler2D", name))
            solidProgram.uniform1i(name, frequency, initializer);
        if (translucentProgram.containsUniformSpec("sampler2D", name))
            translucentProgram.uniform1i(name, frequency, initializer);
    }

    public void uniform1f(String name, UniformRefreshFrequency frequency, Consumer<Uniform1f> initializer) {
        if (solidProgram.containsUniformSpec("float", name))
            solidProgram.uniform1f(name, frequency, initializer);
        if (translucentProgram.containsUniformSpec("float", name))
            translucentProgram.uniform1f(name, frequency, initializer);
    }

    public void uniform2f(String name, UniformRefreshFrequency frequency, Consumer<Uniform2f> initializer) {
        if (solidProgram.containsUniformSpec("vec2", name))
            solidProgram.uniform2f(name, frequency, initializer);
        if (translucentProgram.containsUniformSpec("vec2", name))
            translucentProgram.uniform2f(name, frequency, initializer);
    }

    public void uniform3f(String name, UniformRefreshFrequency frequency, Consumer<Uniform3f> initializer) {
        if (solidProgram.containsUniformSpec("vec3", name))
            solidProgram.uniform3f(name, frequency, initializer);
        if (translucentProgram.containsUniformSpec("vec3", name))
            translucentProgram.uniform3f(name, frequency, initializer);
    }

    public void uniform4f(String name, UniformRefreshFrequency frequency, Consumer<Uniform4f> initializer) {
        if (solidProgram.containsUniformSpec("vec4", name))
            solidProgram.uniform4f(name, frequency, initializer);
        if (translucentProgram.containsUniformSpec("vec4", name))
            translucentProgram.uniform4f(name, frequency, initializer);
    }

    public void uniform1i(String name, UniformRefreshFrequency frequency, Consumer<Uniform1i> initializer) {
        if (solidProgram.containsUniformSpec("int", name))
            solidProgram.uniform1i(name, frequency, initializer);
        if (translucentProgram.containsUniformSpec("int", name))
            translucentProgram.uniform1i(name, frequency, initializer);
    }

    public void uniform2i(String name, UniformRefreshFrequency frequency, Consumer<Uniform2i> initializer) {
        if (solidProgram.containsUniformSpec("ivec2", name))
            solidProgram.uniform2i(name, frequency, initializer);
        if (translucentProgram.containsUniformSpec("ivec2", name))
            translucentProgram.uniform2i(name, frequency, initializer);
    }

    public void uniform3i(String name, UniformRefreshFrequency frequency, Consumer<Uniform3i> initializer) {
        if (solidProgram.containsUniformSpec("ivec3", name))
            solidProgram.uniform3i(name, frequency, initializer);
        if (translucentProgram.containsUniformSpec("ivec3", name))
            translucentProgram.uniform3i(name, frequency, initializer);
    }

    public void uniform4i(String name, UniformRefreshFrequency frequency, Consumer<Uniform4i> initializer) {
        if (solidProgram.containsUniformSpec("ivec4", name))
            solidProgram.uniform4i(name, frequency, initializer);
        if (translucentProgram.containsUniformSpec("ivec4", name))
            translucentProgram.uniform4i(name, frequency, initializer);
    }

    public void uniformMatrix4f(String name, UniformRefreshFrequency frequency, Consumer<UniformMatrix4f> initializer) {
        if (solidProgram.containsUniformSpec("mat4", name))
            solidProgram.uniformMatrix4f(name, frequency, initializer);
        if (translucentProgram.containsUniformSpec("mat4", name))
            translucentProgram.uniformMatrix4f(name, frequency, initializer);
    }

    public void onRenderTick() {
        this.solidProgram.onRenderTick();
        this.translucentProgram.onRenderTick();
    }

    public void onGameTick() {
        this.solidProgram.onGameTick();
        this.translucentProgram.onGameTick();
    }

    public void setupModelViewUniforms() {
        this.solidProgram.setupModelViewUniforms();
        this.translucentProgram.setupModelViewUniforms();
    }
}
