package grondag.canvas.buffering;

public class SimpleBufferDelegate extends AbstractBufferDelegate<SimpleBuffer> {
    protected SimpleBufferDelegate(SimpleBuffer buffer, int byteOffset, int byteCount) {
        super(buffer, byteOffset, byteCount);
    }
}
