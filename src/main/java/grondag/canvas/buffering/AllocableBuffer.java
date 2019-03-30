package grondag.canvas.buffering;

import java.nio.ByteBuffer;

// UGLY: this type hierarchy and usages are nuts, same for delegates - needs major refactor
public abstract class AllocableBuffer {
    protected abstract ByteBuffer byteBuffer();
}