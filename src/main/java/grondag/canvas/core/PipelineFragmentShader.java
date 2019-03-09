package grondag.canvas.core;

import org.lwjgl.opengl.GL21;

public final class PipelineFragmentShader extends AbstractPipelineShader {
    PipelineFragmentShader(String fileName, int spriteDepth, boolean isSolidLayer) {
        super(fileName, GL21.GL_FRAGMENT_SHADER, spriteDepth, isSolidLayer);
    }

    @Override
    public String getSource() {
        return buildSource(PipelineShaderManager.INSTANCE.fragmentLibrarySource);
    }
}
