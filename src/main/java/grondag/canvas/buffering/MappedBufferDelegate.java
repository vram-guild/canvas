package grondag.canvas.buffering;

public class MappedBufferDelegate extends AbstractBufferDelegate<MappedBuffer> {
    public MappedBufferDelegate(MappedBuffer buffer, int byteOffset, int byteCount) {
        super(buffer, byteOffset, byteCount);
    }
}
