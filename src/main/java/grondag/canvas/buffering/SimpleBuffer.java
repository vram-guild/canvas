package grondag.canvas.buffering;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.GLX;

import grondag.canvas.core.RenderPipelineImpl;

public class SimpleBuffer extends AbstractBuffer implements AllocationProvider {
    ByteBuffer uploadBuffer;
    
    int byteOffset = 0;
    
    SimpleBuffer(int bytes) {
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
    public void claimAllocation(RenderPipelineImpl pipeline, int byteCount, Consumer<AbstractBufferDelegate<?>> consumer) {
        consumer.accept(new SimpleBufferDelegate(this, byteOffset, byteCount));
        byteOffset += byteCount;
    }
}
