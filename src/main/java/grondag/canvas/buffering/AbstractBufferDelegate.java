package grondag.canvas.buffering;

import java.nio.IntBuffer;

abstract class AbstractBufferDelegate<T extends AbstractBuffer> {
    protected final int byteCount;
    protected final int byteOffset;
    protected final T buffer;

    protected AbstractBufferDelegate(T buffer, int byteOffset, int byteCount) {
        this.buffer = buffer;
        this.byteCount = byteCount;
        this.byteOffset = byteOffset;
    }
    
    public abstract boolean isVbo();
    
    /**
     * How many bytes consumed by this delegate in the buffer.
     */
    public final int byteCount() {
        return this.byteCount;
    }

    /**
     * Start of this delegate's bytes in the buffer.
     */
    public final int byteOffset() {
        return this.byteOffset;
    }

    public final int glBufferId() {
        return buffer.glBufferId();
    }

    /** chunk will populate this buffer with vertex data. Will be used off thread. */
    public final IntBuffer intBuffer() {
        return buffer.byteBuffer().asIntBuffer();
    }

    /**
     * True if buffer has been fully released and recycled.  Disposed buffers cannot be used.
     */
    public final boolean isDisposed() {
        return buffer.isDisposed();
    }

    public final void bind() {
        buffer.bind();
    }

    /**
     * Uploads or flushes to GPU, depending on the type of buffer. Always called from main thread.
     */
    public final void flush() {
        buffer.flush();
    }
    
    /**
     * Signals the buffer is in use. May be called off-thread.
     */
    public final void retain(DrawableDelegate drawableChunkDelegate) {
        buffer.retain(drawableChunkDelegate);
    }

    /**
     * Signals the buffer will no longer be used. May be called off-thread.
     */
    public final void release(DrawableDelegate drawableChunkDelegate) {
        buffer.release(drawableChunkDelegate);
    }

    /** called before chunk populates int buffer(). May be called off thread */
    public final void lockForUpload() {
//        buffer.bufferLock.lock();
    }

    /** called after chunk populates int buffer(). May be called off thread */
    public final void unlockForUpload() {
//        buffer.bufferLock.unlock();
    }
}
