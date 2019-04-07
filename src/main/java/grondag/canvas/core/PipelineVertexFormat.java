package grondag.canvas.core;

import static grondag.canvas.core.PipelineVertextFormatElement.BASE_RGBA_4UB;
import static grondag.canvas.core.PipelineVertextFormatElement.BASE_TEX_2F;
import static grondag.canvas.core.PipelineVertextFormatElement.LIGHTMAPS_4UB;
import static grondag.canvas.core.PipelineVertextFormatElement.NORMAL_AO_4UB;
import static grondag.canvas.core.PipelineVertextFormatElement.POSITION_3F;
import static grondag.canvas.core.PipelineVertextFormatElement.SECONDARY_RGBA_4UB;
import static grondag.canvas.core.PipelineVertextFormatElement.SECONDARY_TEX_2F;
import static grondag.canvas.core.PipelineVertextFormatElement.TERTIARY_RGBA_4UB;
import static grondag.canvas.core.PipelineVertextFormatElement.TERTIARY_TEX_2F;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL20;
import grondag.canvas.opengl.CanvasGlHelper;
import net.minecraft.client.render.VertexFormat;

public enum PipelineVertexFormat {
    SINGLE(0,
            new VertexFormat().add(POSITION_3F).add(BASE_RGBA_4UB).add(BASE_TEX_2F).add(LIGHTMAPS_4UB).add(NORMAL_AO_4UB)),
    /**
     * Adds one extra color and texture coordinate. Use for two-layered textures.
     */
    DOUBLE(1,
            new VertexFormat().add(POSITION_3F).add(BASE_RGBA_4UB).add(BASE_TEX_2F).add(LIGHTMAPS_4UB).add(NORMAL_AO_4UB)
                    .add(SECONDARY_RGBA_4UB).add(SECONDARY_TEX_2F)),
    /**
     * Adds two extra colors and texture coordinates. Use for three-layered
     * materials.
     */
    TRIPLE(2,
            new VertexFormat().add(POSITION_3F).add(BASE_RGBA_4UB).add(BASE_TEX_2F).add(LIGHTMAPS_4UB).add(NORMAL_AO_4UB)
                    .add(SECONDARY_RGBA_4UB).add(SECONDARY_TEX_2F)
                    .add(TERTIARY_RGBA_4UB).add(TERTIARY_TEX_2F));

    public final VertexFormat vertexFormat;

    /**
     * Will be a unique, 0-based ordinal within the current lighting model.
     */
    public final int layerIndex;

    public final int attributeCount;
    
    /** vertex stride in bytes */
    public final int vertexStrideBytes;

    private final PipelineVertextFormatElement[] elements;

    private PipelineVertexFormat(int layerIndex, VertexFormat vertexFormat) {
        this.layerIndex = layerIndex;
        this.vertexFormat = vertexFormat;
        this.vertexStrideBytes = vertexFormat.getVertexSize();
        this.elements = vertexFormat.getElements()
                .toArray(new PipelineVertextFormatElement[vertexFormat.getElementCount()]);
        int count = 0;
        for (PipelineVertextFormatElement e : elements) {
            if (e.attributeName != null)
                count++;
        }
        this.attributeCount = count;
    }

    /**
     * Enables generic vertex attributes and binds their location.
     * For use with non-VAO VBOs
     */
    public void enableAndBindAttributes(int bufferOffset) {
        CanvasGlHelper.enableAttributes(this.attributeCount);
        bindAttributeLocations(bufferOffset);
    }

    /**
     * Enables generic vertex attributes and binds their location.
     * For use with non-VBO buffers.
     */
    public void enableAndBindAttributes(ByteBuffer buffer, int bufferOffset) {
        CanvasGlHelper.enableAttributes(this.attributeCount);
        int offset = 0;
        int index = 1;
        for (PipelineVertextFormatElement e : elements) {
            if (e.attributeName != null) {
                buffer.position(bufferOffset + offset);
                GL20.glVertexAttribPointer(index++, e.elementCount, e.glConstant, e.isNormalized, vertexStrideBytes, buffer);
            }
            offset += e.byteSize;
        }
    }
    
    /**
     * Binds attribute locations without enabling them. For use with VAOs. In other
     * cases just call {@link #enableAndBindAttributes(int)}
     */
    public void bindAttributeLocations(int bufferOffset) {
        int offset = 0;
        int index = 1;
        for (PipelineVertextFormatElement e : elements) {
            if (e.attributeName != null) {
                GL20.glVertexAttribPointer(index++, e.elementCount, e.glConstant, e.isNormalized, vertexStrideBytes, bufferOffset + offset);
            }
            offset += e.byteSize;
        }
    }

    public void bindProgramAttributes(int programID) {
        int index = 1;
        for (PipelineVertextFormatElement e : elements) {
            if (e.attributeName != null) {
                GL20.glBindAttribLocation(programID, index++, e.attributeName);
            }
        }
    }
}
