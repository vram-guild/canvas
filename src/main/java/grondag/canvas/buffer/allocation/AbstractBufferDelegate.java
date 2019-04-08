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

package grondag.canvas.buffer.allocation;

import java.nio.IntBuffer;

import grondag.canvas.buffer.DrawableDelegate;

public abstract class AbstractBufferDelegate<T extends AllocableBuffer> {
    protected final int byteCount;
    protected final int byteOffset;
    public final T buffer;

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

    /** chunk will populate this buffer with vertex data. Will be used off thread. */
    public final IntBuffer intBuffer() {
        return buffer.byteBuffer().asIntBuffer();
    }

    public abstract void lockForUpload();

    public abstract void unlockForUpload();

    public abstract void retain(DrawableDelegate result);

    public abstract int glBufferId();

    public abstract void bind();

    public abstract boolean isDisposed();

    public abstract void release(DrawableDelegate drawableDelegate);

    public abstract void flush();
}
