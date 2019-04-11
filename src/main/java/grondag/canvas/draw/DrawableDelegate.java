/*******************************************************************************
 * Copyright 2019 grondag
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.draw;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;

import grondag.canvas.buffer.allocation.AbstractBuffer;
import grondag.canvas.buffer.allocation.BufferDelegate;
import grondag.canvas.pipeline.RenderState;
import grondag.canvas.pipeline.PipelineVertexFormat;
import grondag.canvas.varia.CanvasGlHelper;
import grondag.canvas.varia.VaoStore;
import net.minecraft.client.render.VertexFormatElement;

public class DrawableDelegate {
    private static final ArrayBlockingQueue<DrawableDelegate> store = new ArrayBlockingQueue<DrawableDelegate>(4096);
    
    /**
     * Signals the gl buffer not determined.  
     * Can't be populated at construction because that can happen off thread.
     */
    private static final int BUFFER_UNKNOWN = -1000;
    
    /**
     * Pointer to start of vertex data for current vertex binding.
     * Set to zero when new vertex bindings applied.
     * When vertex binding is updated, if buffer and format are the same and 
     * byte offset can be expressed as a multiple of current stride, then this
     * is updated to a vertex offset to avoid rebinding vertex attributes.
     */
    private static int vertexOffset = 0;
    
    /**
     * Byte offset used for last vertex binding. 
     */
    private static int boundByteOffset = 0;
    
    @FunctionalInterface
    private static interface VertexBinder {
        void bind(PipelineVertexFormat format, boolean isNewBuffer);
    }
    
    public static DrawableDelegate claim(BufferDelegate bufferDelegate, RenderState renderState, int vertexCount) {
        DrawableDelegate result = store.poll();
        if(result == null) {
            result = new DrawableDelegate();
        }
        result.bufferDelegate = bufferDelegate;
        result.renderState = renderState;
        result.vertexCount = vertexCount;
        result.isReleased = false;
        result.vertexBinder = bufferDelegate.buffer().isVbo() 
                ? (CanvasGlHelper.isVaoEnabled() ? result::bindVao : result::bindVbo)
                : result::bindBuffer;
        bufferDelegate.buffer().retain(result);
        result.bufferId = BUFFER_UNKNOWN;
        
        return result;
    }

    private BufferDelegate bufferDelegate;
    private RenderState renderState;
    private int vertexCount;
    private boolean isReleased = false;
    private VertexBinder vertexBinder;
    private int bufferId = BUFFER_UNKNOWN;
    
    /**
     * VAO Buffer name if enabled and initialized.
     */
    private int vaoBufferId = -1;

    private DrawableDelegate() {
        super();
    }

    public BufferDelegate bufferDelegate() {
        return this.bufferDelegate;
    }

    /**
     * Instances that share the same GL buffer will have the same ID. Allows sorting
     * in solid layer to avoid rebinding buffers for draws that will have the same
     * vertex buffer and pipeline/format.
     */
    public int bufferId() {
        int result = bufferId;
        if(bufferId == BUFFER_UNKNOWN) {
            bufferDelegate.buffer().bindable().glBufferId();
            bufferId = result;
        }
        return result;
    }

    /**
     * The pipeline (and vertex format) associated with this delegate.
     */
    public RenderState renderState() {
        return this.renderState;
    }

    /**
     * Won't bind buffer if this buffer same as last - will only do vertex
     * attributes. Returns the buffer Id that is bound, or input if unchanged.
     */
    public void bind() {
        final AbstractBuffer buffer = this.bufferDelegate.buffer();
        if (buffer.isDisposed())
            return;

        final boolean isNewBuffer = buffer.bindable().bind();
        vertexBinder.bind(renderState.pipeline.piplineVertexFormat(), isNewBuffer);
    }

    /**
     * Assumes pipeline has already been activated and buffer has already been bound
     * via {@link #bind()}
     */
    public void draw() {
        assert !isReleased;

        if (this.bufferDelegate.buffer().isDisposed())
            return;

        GlStateManager.drawArrays(GL11.GL_QUADS, vertexOffset, vertexCount);
    }

    public void release() {
        if (!isReleased) {
            isReleased = true;
            bufferDelegate.buffer().release(this);
            bufferDelegate.release();
            bufferDelegate = null;
            if (vaoBufferId != -1) {
                VaoStore.releaseVertexArray(vaoBufferId);
                vaoBufferId = -1;
            }
            renderState =  null;
            store.offer(this);
        }
    }
    
    public void flush() {
        assert !isReleased;
        this.bufferDelegate.buffer().upload();
    }
    
    void bindVao(PipelineVertexFormat format, boolean isNewBuffer) {
        if (vaoBufferId == -1) {
            vaoBufferId = VaoStore.claimVertexArray();
            CanvasGlHelper.glBindVertexArray(vaoBufferId);
            GlStateManager.enableClientState(GL11.GL_VERTEX_ARRAY);
            CanvasGlHelper.enableAttributesVao(format.attributeCount);
            GlStateManager.vertexPointer(3, VertexFormatElement.Format.FLOAT.getGlId(), format.vertexStrideBytes, bufferDelegate.byteOffset());
            format.bindAttributeLocations(bufferDelegate.byteOffset());
        } else {
            CanvasGlHelper.glBindVertexArray(vaoBufferId);
        }
    }
    
    private boolean needsRebind(PipelineVertexFormat format) {
        final int byteOffset = bufferDelegate.byteOffset();
        final int vertexStrideBytes = format.vertexStrideBytes;
        final int gap = byteOffset - boundByteOffset;
        if(gap > 0) {
            final int offset = gap / vertexStrideBytes;
            if(offset * vertexStrideBytes == gap) {
                // reuse vertex binding with offset
                vertexOffset = offset;
                return false;
            }
        }
        return true;
    }
    
    void bindVbo(PipelineVertexFormat format, boolean isNewBuffer) {
        // don't check for bind reuse if not possible due to new buffer
        if(isNewBuffer || needsRebind(format)) {
            final int byteOffset = bufferDelegate.byteOffset();
            vertexOffset = 0;
            boundByteOffset = byteOffset;
            GlStateManager.vertexPointer(3, VertexFormatElement.Format.FLOAT.getGlId(), format.vertexStrideBytes, byteOffset);
            format.enableAndBindAttributes(byteOffset);
        }
    }

    void bindBuffer(PipelineVertexFormat format, boolean isNewBuffer) {
        if(isNewBuffer || needsRebind(format)) {
            final ByteBuffer buffer = bufferDelegate.buffer().byteBuffer();
            final int byteOffset = bufferDelegate.byteOffset();
            vertexOffset = 0;
            boundByteOffset = byteOffset;
            buffer.position(byteOffset);
            GlStateManager.enableClientState(GL11.GL_VERTEX_ARRAY);
            GlStateManager.vertexPointer(3, VertexFormatElement.Format.FLOAT.getGlId(), format.vertexStrideBytes, buffer);
            format.enableAndBindAttributes(buffer, byteOffset);
        }
    }
}
