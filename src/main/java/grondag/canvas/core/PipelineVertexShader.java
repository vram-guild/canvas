package grondag.canvas.core;

import org.lwjgl.opengl.GL21;

public final class PipelineVertexShader extends AbstractPipelineShader {
    PipelineVertexShader(String fileName, int spriteDepth, boolean isSolidLayer) {
        super(fileName, GL21.GL_VERTEX_SHADER, spriteDepth, isSolidLayer);
    }

    @Override
    public String getSource() {
        return buildSource(PipelineShaderManager.INSTANCE.vertexLibrarySource);
    }
}