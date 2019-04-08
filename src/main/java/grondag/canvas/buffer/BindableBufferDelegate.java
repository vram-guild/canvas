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

package grondag.canvas.buffer;

import grondag.canvas.buffer.allocation.AbstractBufferDelegate;

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
