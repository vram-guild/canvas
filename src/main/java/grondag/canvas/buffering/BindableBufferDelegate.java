package grondag.canvas.buffering;

public abstract class BindableBufferDelegate<T extends BindableBuffer> extends AbstractBufferDelegate<T>{
    protected BindableBufferDelegate(T buffer, int byteOffset, int byteCount) {
        super(buffer, byteOffset, byteCount);
    }    
    
    @Override
    public final int glBufferId() {
        return buffer.glBufferId();
    }

    /**
     * True if buffer has been fully released and recycled.  Disposed buffers cannot be used.
     */
    @Override
    public final boolean isDisposed() {
        return buffer.isDisposed();
    }

    @Override
    public final void bind() {
        buffer.bind();
    }

    /**
     * Uploads or flushes to GPU, depending on the type of buffer. Always called from main thread.
     */
    @Override
    public final void flush() {
        buffer.flush();
    }
    
    /**
     * Signals the buffer is in use. May be called off-thread.
     */
    @Override
    public final void retain(DrawableDelegate drawableChunkDelegate) {
        buffer.retain(drawableChunkDelegate);
    }

    /**
     * Signals the buffer will no longer be used. May be called off-thread.
     */
    @Override
    public final void release(DrawableDelegate drawableChunkDelegate) {
        buffer.release(drawableChunkDelegate);
    }

    /** called before chunk populates int buffer(). May be called off thread */
    @Override
    public final void lockForUpload() {
//        buffer.bufferLock.lock();
    }

    /** called after chunk populates int buffer(). May be called off thread */
    @Override
    public final void unlockForUpload() {
//        buffer.bufferLock.unlock();
    }
}
