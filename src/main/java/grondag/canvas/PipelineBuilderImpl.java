package grondag.canvas;

import java.util.function.Consumer;

import grondag.canvas.core.PipelineManager;
import grondag.canvas.core.RenderPipelineImpl;
import grondag.frex.api.Pipeline;
import grondag.frex.api.PipelineBuilder;
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
import net.minecraft.util.Identifier;

public class PipelineBuilderImpl implements PipelineBuilder {
    private Identifier vertexSource;
    private Identifier fragmentSource;
    private int spriteDepth = 1;
    
    @Override
    public Pipeline build() {
        RenderPipelineImpl result = PipelineManager.INSTANCE.createPipeline(spriteDepth, vertexSource, fragmentSource);
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
    public PipelineBuilderImpl uniform1f(String arg0, UniformRefreshFrequency arg1, Consumer<Uniform1f> arg2) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public PipelineBuilderImpl uniform1i(String arg0, UniformRefreshFrequency arg1, Consumer<Uniform1i> arg2) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public PipelineBuilderImpl uniform2f(String arg0, UniformRefreshFrequency arg1, Consumer<Uniform2f> arg2) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public PipelineBuilderImpl uniform2i(String arg0, UniformRefreshFrequency arg1, Consumer<Uniform2i> arg2) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public PipelineBuilderImpl uniform3f(String arg0, UniformRefreshFrequency arg1, Consumer<Uniform3f> arg2) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public PipelineBuilderImpl uniform3i(String arg0, UniformRefreshFrequency arg1, Consumer<Uniform3i> arg2) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public PipelineBuilderImpl uniform4f(String arg0, UniformRefreshFrequency arg1, Consumer<Uniform4f> arg2) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public PipelineBuilderImpl uniform4i(String arg0, UniformRefreshFrequency arg1, Consumer<Uniform4i> arg2) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public PipelineBuilderImpl uniformMatrix4f(String arg0, UniformRefreshFrequency arg1, Consumer<UniformMatrix4f> arg2) {
        // TODO Auto-generated method stub
        return this;
    }
}
