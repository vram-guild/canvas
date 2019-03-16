package grondag.canvas.core;

import org.lwjgl.opengl.GL21;

import net.minecraft.util.Identifier;

public final class PipelineFragmentShader extends AbstractPipelineShader {
    PipelineFragmentShader(Identifier shaderSource, int spriteDepth, boolean isSolidLayer) {
        super(shaderSource, GL21.GL_FRAGMENT_SHADER, spriteDepth, isSolidLayer);
    }

    @Override
    public String getSource() {
        return buildSource(PipelineShaderManager.INSTANCE.fragmentLibrarySource);
    }
}
