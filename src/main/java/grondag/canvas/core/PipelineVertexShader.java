package grondag.canvas.core;

import com.mojang.blaze3d.platform.GLX;

public final class PipelineVertexShader  extends AbstractPipelineShader
{
    PipelineVertexShader(String fileName, int spriteDepth, boolean isSolidLayer)
    {
        super(fileName, GLX.GL_VERTEX_SHADER, spriteDepth, isSolidLayer);
    }
    
    @Override
    public String getSource()
    {
        return buildSource(PipelineShaderManager.INSTANCE.vertexLibrarySource);
    }
}