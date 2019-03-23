package grondag.canvas.buffering;

import java.util.concurrent.ArrayBlockingQueue;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;

import grondag.canvas.core.PipelineVertexFormat;
import grondag.canvas.core.RenderPipelineImpl;
import grondag.canvas.core.BufferStore.ExpandableByteBuffer;
import grondag.canvas.opengl.CanvasGlHelper;
import grondag.canvas.opengl.VaoStore;
import net.minecraft.client.render.VertexFormatElement;

public class DrawableDelegate {
    private static final ArrayBlockingQueue<DrawableDelegate> store = new ArrayBlockingQueue<DrawableDelegate>(4096);
    
    public static DrawableDelegate claim(AbstractBufferDelegate<?> bufferDelegate, RenderPipelineImpl pipeline, int vertexCount) {
        DrawableDelegate result = store.poll();
        if(result == null) {
            result = new DrawableDelegate();
        }
        result.bufferDelegate = bufferDelegate;
        result.pipeline = pipeline;
        result.vertexCount = vertexCount;
        result.isReleased = false;
        bufferDelegate.retain(result);
        return result;
    }

    private AbstractBufferDelegate<?> bufferDelegate;
    private RenderPipelineImpl pipeline;
    private int vertexCount;
    private boolean isReleased = false;
    
    /**
     * VAO Buffer name if enabled and initialized.
     */
    int vaoBufferId = -1;
    boolean vaoNeedsRefresh = true;

    private DrawableDelegate() {
        super();
    }

    public AbstractBufferDelegate<?> bufferDelegate() {
        return this.bufferDelegate;
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
    public RenderPipelineImpl getPipeline() {
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
            bufferDelegate = null;
            pipeline =  null;
            store.offer(this);
        }
    }

    public void flush() {
        assert !isReleased;
        this.bufferDelegate.flush();
    }
}
