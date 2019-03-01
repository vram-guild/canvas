package grondag.canvas.buffering;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;

import grondag.canvas.core.PipelineVertexFormat;
import grondag.canvas.core.RenderPipeline;
import grondag.canvas.opengl.CanvasGlHelper;
import grondag.canvas.opengl.VaoStore;
import net.minecraft.client.render.VertexFormatElement;

public class DrawableChunkDelegate {
    private AbstractBufferDelegate<?> bufferDelegate;
    private final RenderPipeline pipeline;
    final int vertexCount;
    /**
     * VAO Buffer name if enabled and initialized.
     */
    int vaoBufferId = -1;
    boolean vaoNeedsRefresh = true;

    private boolean isReleased = false;

    public DrawableChunkDelegate(AbstractBufferDelegate<?> bufferDelegate, RenderPipeline pipeline, int vertexCount) {
        this.bufferDelegate = bufferDelegate;
        this.pipeline = pipeline;
        this.vertexCount = vertexCount;
        bufferDelegate.retain(this);
    }

    public AbstractBufferDelegate<?> bufferDelegate() {
        return this.bufferDelegate;
    }

    public void replaceBufferDelegate(AbstractBufferDelegate<?> newDelegate) {
        // possible we have been released after rebuffer happened
        if (isReleased)
            newDelegate.release(this);
        else {
            this.bufferDelegate = newDelegate;
            vaoNeedsRefresh = true;
        }
    }

    /**
     * Instances that share the same GL buffer will have the same ID. Allows sorting
     * in solid layer to avoid rebinding buffers for draws that will have the same
     * vertex buffer and pipeline/format.
     */
    public int bufferId() {
        return this.bufferDelegate.glBufferId();
    }

    /**
     * The pipeline (and vertex format) associated with this delegate.
     */
    public RenderPipeline getPipeline() {
        return this.pipeline;
    }

    /**
     * Won't bind buffer if this buffer same as last - will only do vertex
     * attributes. Returns the buffer Id that is bound, or input if unchanged.
     */
    public int bind(int lastBufferId) {
        if (this.bufferDelegate.isDisposed())
            return lastBufferId;

        if (this.bufferDelegate.glBufferId() != lastBufferId) {
            this.bufferDelegate.bind();
            lastBufferId = this.bufferDelegate.glBufferId();
        }

        final PipelineVertexFormat format = pipeline.piplineVertexFormat();
        
        if (vaoNeedsRefresh) {
            if (CanvasGlHelper.isVaoEnabled()) {
                if (vaoBufferId == -1)
                    vaoBufferId = VaoStore.claimVertexArray();
                CanvasGlHelper.glBindVertexArray(vaoBufferId);
                GlStateManager.enableClientState(GL11.GL_VERTEX_ARRAY);
                CanvasGlHelper.enableAttributesVao(format.attributeCount);
                bindVertexAttributes(format);
                return lastBufferId;
            }
            vaoNeedsRefresh = false;
        }

        if (vaoBufferId > 0)
            CanvasGlHelper.glBindVertexArray(vaoBufferId);
        else {
            GlStateManager.vertexPointer(3, VertexFormatElement.Format.FLOAT.getGlId(), format.vertexStrideBytes,
                    bufferDelegate.byteOffset());
            format.enableAndBindAttributes(bufferDelegate.byteOffset());
        }

        return lastBufferId;

    }

    private void bindVertexAttributes(PipelineVertexFormat format) {
        GlStateManager.vertexPointer(3, VertexFormatElement.Format.FLOAT.getGlId(), format.vertexStrideBytes,
                bufferDelegate.byteOffset());
        format.bindAttributeLocations(bufferDelegate.byteOffset());
    }

    /**
     * Assumes pipeline has already been activated and buffer has already been bound
     * via {@link #bind()}
     */
    public void draw() {
        assert !isReleased;

        if (this.bufferDelegate.isDisposed())
            return;

        GlStateManager.drawArrays(GL11.GL_QUADS, 0, vertexCount);
    }

    public void release() {
        if (!isReleased) {
            isReleased = true;
            bufferDelegate.release(this);
            if (vaoBufferId != -1) {
                VaoStore.releaseVertexArray(vaoBufferId);
                vaoBufferId = -1;
            }
        }
    }

    public void flush() {
        assert !isReleased;
        this.bufferDelegate.flush();
    }
}
