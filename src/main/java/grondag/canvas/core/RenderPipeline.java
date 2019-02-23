package grondag.acuity.api;

import java.util.function.Consumer;

import grondag.acuity.Configurator;
import grondag.acuity.api.IUniform.IUniform1f;
import grondag.acuity.api.IUniform.IUniform1i;
import grondag.acuity.api.IUniform.IUniform2f;
import grondag.acuity.api.IUniform.IUniform2i;
import grondag.acuity.api.IUniform.IUniform3f;
import grondag.acuity.api.IUniform.IUniform3i;
import grondag.acuity.api.IUniform.IUniform4f;
import grondag.acuity.api.IUniform.IUniform4i;
import grondag.acuity.api.IUniform.IUniformMatrix4f;
import grondag.acuity.core.PipelineFragmentShader;
import grondag.acuity.core.PipelineShaderManager;
import grondag.acuity.core.PipelineVertexFormat;
import grondag.acuity.core.PipelineVertexShader;
import grondag.acuity.core.Program;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RenderPipeline implements IRenderPipeline
{
    private final int index;
    private final Program solidProgram;
    private final Program translucentProgram;
    public final TextureFormat textureFormat;
    private boolean isFinal = false;
    private PipelineVertexFormat pipelineVertexFormat;
    private VertexFormat vertexFormat;
    
    
    RenderPipeline(int index, String vertexShader, String fragmentShader, TextureFormat textureFormat)
    {
        PipelineVertexShader  vs = PipelineShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, textureFormat, true);
        PipelineFragmentShader  fs = PipelineShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader, textureFormat, true);
        this.solidProgram = new Program(vs, fs, textureFormat, true);
        
        vs = PipelineShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, textureFormat, false);
        fs = PipelineShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader, textureFormat, false);
        this.translucentProgram = new Program(vs, fs, textureFormat, false);
        
        this.index = index;
        this.textureFormat = textureFormat;
        this.refreshVertexFormats();
    }
    
    public void activate(boolean isSolidLayer)
    {
        if(isSolidLayer)
            this.solidProgram.activate();
        else
            this.translucentProgram.activate();
    }
    
    public void forceReload()
    {
        this.solidProgram.forceReload();
        this.translucentProgram.forceReload();
        this.refreshVertexFormats();
    }
    
    public void refreshVertexFormats()
    {
        this.pipelineVertexFormat = Configurator.lightingModel.vertexFormat(this.textureFormat);
        this.vertexFormat = this.pipelineVertexFormat.vertexFormat;
    }
    
    @Override
    public TextureFormat textureFormat()
    {
        return this.textureFormat;
    }
    
    @Override
    public IRenderPipeline finish()
    {
        this.isFinal = true;
        this.forceReload();
        return this;
    }
    
    public PipelineVertexFormat piplineVertexFormat()
    {
        return this.pipelineVertexFormat;
    }
    
    /**
     * Avoids a pointer chase, more concise code.
     */
    public VertexFormat vertexFormat()
    {
        return this.vertexFormat;
    }
    
    @Override
    public int getIndex()
    {
        return this.index;
    }

    private void checkFinal()
    {
        if(this.isFinal)
            throw new UnsupportedOperationException(I18n.translateToLocal("misc.warn_uniform_program_immutable_exception"));    
    }
    
    public void uniformSampler2d(String name, UniformUpdateFrequency frequency, Consumer<IUniform1i> initializer)
    {
        if(solidProgram.containsUniformSpec("sampler2D", name))
            solidProgram.uniform1i(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("sampler2D", name))
            translucentProgram.uniform1i(name, frequency, initializer);
    }
    
    @Override
    public void uniform1f(String name, UniformUpdateFrequency frequency, Consumer<IUniform1f> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("float", name))
            solidProgram.uniform1f(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("float", name))
            translucentProgram.uniform1f(name, frequency, initializer);
    }

    @Override
    public void uniform2f(String name, UniformUpdateFrequency frequency, Consumer<IUniform2f> initializer)
    {
        checkFinal();    
        if(solidProgram.containsUniformSpec("vec2", name))
            solidProgram.uniform2f(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("vec2", name))
            translucentProgram.uniform2f(name, frequency, initializer);
    }

    @Override
    public void uniform3f(String name, UniformUpdateFrequency frequency, Consumer<IUniform3f> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("vec3", name))
            solidProgram.uniform3f(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("vec3", name))
            translucentProgram.uniform3f(name, frequency, initializer);        
    }

    @Override
    public void uniform4f(String name, UniformUpdateFrequency frequency, Consumer<IUniform4f> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("vec4", name))
            solidProgram.uniform4f(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("vec4", name))
            translucentProgram.uniform4f(name, frequency, initializer);        
    }

    @Override
    public void uniform1i(String name, UniformUpdateFrequency frequency, Consumer<IUniform1i> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("int", name))
            solidProgram.uniform1i(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("int", name))
            translucentProgram.uniform1i(name, frequency, initializer);        
    }

    @Override
    public void uniform2i(String name, UniformUpdateFrequency frequency, Consumer<IUniform2i> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("ivec2", name))
            solidProgram.uniform2i(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("ivec2", name))
            translucentProgram.uniform2i(name, frequency, initializer);         
    }

    @Override
    public void uniform3i(String name, UniformUpdateFrequency frequency, Consumer<IUniform3i> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("ivec3", name))
            solidProgram.uniform3i(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("ivec3", name))
            translucentProgram.uniform3i(name, frequency, initializer);           
    }

    @Override
    public void uniform4i(String name, UniformUpdateFrequency frequency, Consumer<IUniform4i> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("ivec4", name))
            solidProgram.uniform4i(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("ivec4", name))
            translucentProgram.uniform4i(name, frequency, initializer);         
    }

    @Override
    public void uniformMatrix4f(String name, UniformUpdateFrequency frequency, Consumer<IUniformMatrix4f> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("mat4", name))
            solidProgram.uniformMatrix4f(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("mat4", name))
            translucentProgram.uniformMatrix4f(name, frequency, initializer);         
    }

    public void onRenderTick()
    {
        this.solidProgram.onRenderTick();
        this.translucentProgram.onRenderTick();
    }

    public void onGameTick()
    {
        this.solidProgram.onGameTick();
        this.translucentProgram.onGameTick();        
    }

    public void setupModelViewUniforms()
    {
        this.solidProgram.setupModelViewUniforms();
        this.translucentProgram.setupModelViewUniforms();
    }
}
