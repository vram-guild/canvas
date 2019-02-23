package grondag.canvas.core;

import com.mojang.blaze3d.platform.GLX;

public final class PipelineFragmentShader extends AbstractPipelineShader
{
    PipelineFragmentShader(String fileName, int spriteDepth, boolean isSolidLayer)
    {
        super(fileName, GLX.GL_FRAGMENT_SHADER, spriteDepth, isSolidLayer);
    }
    
    @Override
    public String getSource()
    {
        return buildSource(PipelineShaderManager.INSTANCE.fragmentLibrarySource);
    }
}
