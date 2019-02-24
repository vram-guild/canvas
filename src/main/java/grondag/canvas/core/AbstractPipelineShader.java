package grondag.canvas.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.lwjgl.opengl.GL11;

import com.google.common.io.CharStreams;
import com.mojang.blaze3d.platform.GLX;

import grondag.canvas.Canvas;
import grondag.canvas.opengl.CanvasGlHelper;
import net.minecraft.client.resource.language.I18n;

abstract class AbstractPipelineShader {
    public final String fileName;

    private final int shaderType;
    public final int spriteDepth;
    public final boolean isSolidLayer;

    private int glId = -1;
    private boolean needsLoad = true;
    private boolean isErrored = false;

    AbstractPipelineShader(String fileName, int shaderType, int spriteDepth, boolean isSolidLayer) {
        this.fileName = fileName;
        this.shaderType = shaderType;
        this.spriteDepth = spriteDepth;
        this.isSolidLayer = isSolidLayer;
    }

    /**
     * Call after render / resource refresh to force shader reload.
     */
    public final void forceReload() {
        this.needsLoad = true;
    }

    public final int glId() {
        if (this.needsLoad)
            this.load();

        return this.isErrored ? -1 : this.glId;
    }

    private final void load() {
        this.needsLoad = false;
        this.isErrored = false;
        try {
            if (this.glId <= 0) {
                this.glId = GLX.glCreateShader(shaderType);
                if (this.glId == 0) {
                    this.glId = -1;
                    this.isErrored = true;
                    return;
                }
            }

            GLX.glShaderSource(this.glId, this.getSource());
            GLX.glCompileShader(this.glId);

            if (GLX.glGetShaderi(this.glId, GLX.GL_COMPILE_STATUS) == GL11.GL_FALSE)
                throw new RuntimeException(CanvasGlHelper.getShaderInfoLog(this.glId));

        } catch (Exception e) {
            this.isErrored = true;
            if (this.glId > 0) {
                GLX.glDeleteShader(glId);
                this.glId = -1;
            }
            Canvas.INSTANCE.getLog().error(I18n.translate("misc.fail_create_shader", this.fileName,
                    Integer.toString(this.spriteDepth), e.getMessage()));
        }
    }

    public String buildSource(String librarySource) {
        String result = getShaderSource(this.fileName);
        result = result.replaceAll("#version\\s+120", "");
        result = librarySource + result;

        if (spriteDepth > 1)
            result = result.replaceAll("#define LAYER_COUNT 1", String.format("#define LAYER_COUNT %d", spriteDepth));

        if (!isSolidLayer)
            result = result.replaceAll("#define SOLID", "#define TRANSLUCENT");

        return result;
    }

    abstract String getSource();

    public static String getShaderSource(String fileName) {
        InputStream in = PipelineManager.class.getResourceAsStream(fileName);

        if (in == null)
            return "";

        try (final Reader reader = new InputStreamReader(in)) {
            return CharStreams.toString(reader);
        } catch (IOException e) {
            return "";
        }
    }
}
