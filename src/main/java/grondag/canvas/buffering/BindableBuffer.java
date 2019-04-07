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

package grondag.canvas.buffering;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.platform.GLX;

import grondag.canvas.opengl.GLBufferStore;

public abstract class BindableBuffer extends AllocableBuffer {
    protected static int nextID = 0;

    private int glBufferId = -1;

    public final int id = nextID++;

    /**
     * DrawableChunkDelegates currently using the buffer for rendering.
     */
    protected final Set<DrawableDelegate> retainers = Collections
            .newSetFromMap(new ConcurrentHashMap<DrawableDelegate, Boolean>());

    public int glBufferId() {
        int result = glBufferId;
        if(result == -1) {
            result = GLBufferStore.claimBuffer();
            glBufferId = result;
        }
        return result;
    }
    
    protected void bind() {
        GLX.glBindBuffer(GLX.GL_ARRAY_BUFFER, this.glBufferId());
    }

    protected void unbind() {
        GLX.glBindBuffer(GLX.GL_ARRAY_BUFFER, 0);
    }

    protected abstract void flush();

    /**
     * Called implicitly when bytes are allocated. Store calls explicitly to retain
     * while this buffer is being filled.
     */
    protected void retain(DrawableDelegate drawable) {
//        traceLog.add(String.format("retain(%s)", drawable.toString()));
        retainers.add(drawable);
    }

    protected void release(DrawableDelegate drawable) {
        retainers.remove(drawable);
    }

    protected boolean isDisposed = false;

    protected boolean isDisposed() {
        return isDisposed;
    }

    /** called by store on render reload to recycle GL buffer */
    protected void dispose() {
        if (!isDisposed) {
            isDisposed = true;
        }
        if(glBufferId != -1) {
            GLBufferStore.releaseBuffer(glBufferId);
            glBufferId = -1;
        }
    }
}
