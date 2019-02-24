package grondag.canvas.buffering;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.platform.GLX;

import grondag.canvas.opengl.GLBufferStore;

public abstract class AbstractBuffer {
    protected static int nextID = 0;

    private int glBufferId = -1;

    public final int id = nextID++;

    /**
     * DrawableChunkDelegates currently using the buffer for rendering.
     */
    protected final Set<DrawableChunkDelegate> retainers = Collections
            .newSetFromMap(new ConcurrentHashMap<DrawableChunkDelegate, Boolean>());

    public int glBufferId() {
        int result = glBufferId;
        if(result == -1) {
            result = GLBufferStore.claimBuffer();
            glBufferId = result;
        }
        return result;
    }
    
    protected void bind() {
        GLX.glBindBuffer(GLX.GL_ARRAY_BUFFER, this.glBufferId);
    }

    protected void unbind() {
        GLX.glBindBuffer(GLX.GL_ARRAY_BUFFER, 0);
    }

    protected abstract void flush();

    /**
     * Called implicitly when bytes are allocated. Store calls explicitly to retain
     * while this buffer is being filled.
     */
    protected void retain(DrawableChunkDelegate drawable) {
//        traceLog.add(String.format("retain(%s)", drawable.toString()));
        retainers.add(drawable);
    }

    protected void release(DrawableChunkDelegate drawable) {
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

    protected abstract ByteBuffer byteBuffer();
}
