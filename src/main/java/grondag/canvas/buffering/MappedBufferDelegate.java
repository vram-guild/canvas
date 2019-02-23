package grondag.acuity.buffering;

import java.nio.IntBuffer;

public class MappedBufferDelegate
{
    private final int byteCount;
    private final int byteOffset;
    private final MappedBuffer buffer;
    
    public MappedBufferDelegate(MappedBuffer buffer, int byteOffset, int byteCount)
    {
        this.buffer = buffer;
        this.byteCount = byteCount;
        this.byteOffset = byteOffset;
    }

    public final int byteCount()
    {
        return this.byteCount;
    }

    public final int byteOffset()
    {
        return this.byteOffset;
    }

    public final int glBufferId()
    {
        return buffer.glBufferId;
    }

    public final IntBuffer intBuffer()
    {
        return buffer.byteBuffer().asIntBuffer();
    }

    public final boolean isDisposed()
    {
        return buffer.isDisposed();
    }

    public final void bind()
    {
        buffer.bind();
    }

    public final void flush()
    {
        buffer.flush();
    }

    public final void release(DrawableChunkDelegate drawableChunkDelegate)
    {
        buffer.release(drawableChunkDelegate);
    }

    public final void retain(DrawableChunkDelegate drawableChunkDelegate)
    {
        buffer.retain(drawableChunkDelegate);
    }
    
    public final void lockForUpload()
    {
//        buffer.bufferLock.lock();
    }
    
    public final void unlockForUpload()
    {
//        buffer.bufferLock.unlock();
    }
}
