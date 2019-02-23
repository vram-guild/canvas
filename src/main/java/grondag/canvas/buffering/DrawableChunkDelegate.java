package grondag.acuity.buffering;

import org.lwjgl.opengl.GL11;

import grondag.acuity.api.RenderPipeline;
import grondag.acuity.core.PipelineVertexFormat;
import grondag.acuity.opengl.OpenGlHelperExt;
import grondag.acuity.opengl.VaoStore;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

public class DrawableChunkDelegate
{
    private MappedBufferDelegate bufferDelegate;
    private final RenderPipeline pipeline;
    final int vertexCount;
    /**
     * VAO Buffer name if enabled and initialized.
     */
    int vaoBufferId = -1;
    boolean vaoNeedsRefresh = true;
    
    private boolean isReleased = false;
    
    public DrawableChunkDelegate(MappedBufferDelegate bufferDelegate, RenderPipeline pipeline, int vertexCount)
    {
        this.bufferDelegate = bufferDelegate;
        this.pipeline = pipeline;
        this.vertexCount = vertexCount;
        bufferDelegate.retain(this);
    }
    
    public MappedBufferDelegate bufferDelegate()
    {
        return this.bufferDelegate;
    }
    
    public void replaceBufferDelegate(MappedBufferDelegate newDelegate)
    {
        // possible we have been released after rebuffer happened
        if(isReleased)
            newDelegate.release(this);
        else
        {
            this.bufferDelegate = newDelegate;
            vaoNeedsRefresh = true;
        }
    }
    
    /**
     * Instances that share the same GL buffer will have the same ID.
     * Allows sorting in solid layer to avoid rebinding buffers for draws that
     * will have the same vertex buffer and pipeline/format.
     */
    public int bufferId()
    {
        return this.bufferDelegate.glBufferId();
    }
    
    /**
     * The pipeline (and vertex format) associated with this delegate.
     */
    public RenderPipeline getPipeline()
    {
        return this.pipeline;
    }
    
    /**
     * Won't bind buffer if this buffer same as last - will only do vertex attributes.
     * Returns the buffer Id that is bound, or input if unchanged.
     */
    public int bind(int lastBufferId)
    {
        if(this.bufferDelegate.isDisposed())
            return lastBufferId;
        
        if(this.bufferDelegate.glBufferId() != lastBufferId)
        {
            this.bufferDelegate.bind();
            lastBufferId = this.bufferDelegate.glBufferId();
        }
        
        if(vaoNeedsRefresh)
        {
            final PipelineVertexFormat format = pipeline.piplineVertexFormat();
            if(OpenGlHelperExt.isVaoEnabled())
            {
                if(vaoBufferId == -1)
                    vaoBufferId = VaoStore.claimVertexArray();
                OpenGlHelperExt.glBindVertexArray(vaoBufferId);
                GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                OpenGlHelperExt.enableAttributesVao(format.attributeCount);
                bindVertexAttributes(format);
                return lastBufferId;
            }
            vaoNeedsRefresh = false;
        }
        
        if(vaoBufferId > 0)
            OpenGlHelperExt.glBindVertexArray(vaoBufferId);
        else
            bindVertexAttributes(pipeline.piplineVertexFormat());
        
        return lastBufferId; 
       
    }
    
    private void bindVertexAttributes(PipelineVertexFormat format)
    {
        OpenGlHelperExt.glVertexPointerFast(3, VertexFormatElement.EnumType.FLOAT.getGlConstant(), format.stride, bufferDelegate.byteOffset());
        format.bindAttributeLocations(bufferDelegate.byteOffset());
    }
    
    /**
     * Assumes pipeline has already been activated and buffer has already been bound via {@link #bind()}
     */
    public void draw()
    {
        assert !isReleased;
        
        if(this.bufferDelegate.isDisposed())
            return;
        OpenGlHelperExt.glDrawArraysFast(GL11.GL_QUADS, 0, vertexCount);
    }
    
    public void release()
    {
        if(!isReleased)
        {
            isReleased = true;
            bufferDelegate.release(this);
            if(vaoBufferId != -1)
            {
                VaoStore.releaseVertexArray(vaoBufferId);
                vaoBufferId = -1;
            }
        }
    }

    public void flush()
    {
        assert !isReleased;
        this.bufferDelegate.flush();
    }
}
