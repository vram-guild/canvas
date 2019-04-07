package grondag.canvas.buffering;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.GLX;

import grondag.canvas.core.ConditionalPipeline;

public class VboBuffer extends BindableBuffer implements AllocationProvider {
    ByteBuffer uploadBuffer;
    
    int byteOffset = 0;
    
    VboBuffer(int bytes) {
        uploadBuffer = MemoryUtil.memAlloc(bytes);
    }
    
    @Override
    protected void flush() {
        if(uploadBuffer != null) {
            bind();
            uploadBuffer.rewind();
            GLX.glBufferData(GLX.GL_ARRAY_BUFFER, uploadBuffer, GLX.GL_STATIC_DRAW);
            unbind();
            MemoryUtil.memFree(uploadBuffer);
            uploadBuffer = null;
        }
    }
    
    @Override
    protected void dispose() {
        super.dispose();
        if(uploadBuffer != null) {
            MemoryUtil.memFree(uploadBuffer);
            uploadBuffer = null;
        }
    }
    
    @Override
    protected ByteBuffer byteBuffer() {
        return uploadBuffer;
    }
    
    @Override
    public void claimAllocation(ConditionalPipeline pipeline, int byteCount, Consumer<AbstractBufferDelegate<?>> consumer) {
        // PERF: reuse delegates
        consumer.accept(new VboBufferDelegate(this, byteOffset, byteCount));
        byteOffset += byteCount;
    }
}
