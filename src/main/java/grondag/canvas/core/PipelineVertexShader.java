package grondag.canvas.core;

import org.lwjgl.opengl.GL21;

import net.minecraft.util.Identifier;

public final class PipelineVertexShader extends AbstractPipelineShader {
    PipelineVertexShader(Identifier shaderSource, int spriteDepth, boolean isSolidLayer) {
        super(shaderSource, GL21.GL_VERTEX_SHADER, spriteDepth, isSolidLayer);
    }

    @Override
    public String getSource() {
        return buildSource(PipelineShaderManager.INSTANCE.vertexLibrarySource);
    }
}