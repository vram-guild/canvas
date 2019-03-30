package grondag.canvas.buffering;

public class VboBufferDelegate extends BindableBufferDelegate<VboBuffer> {
    protected VboBufferDelegate(VboBuffer buffer, int byteOffset, int byteCount) {
        super(buffer, byteOffset, byteCount);
    }

    @Override
    public boolean isVbo() {
        return true;
    }
}
