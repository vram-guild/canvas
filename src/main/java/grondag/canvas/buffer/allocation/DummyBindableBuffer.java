package grondag.canvas.buffer.allocation;

public class DummyBindableBuffer implements BindableBuffer {
    public static final DummyBindableBuffer INSTANCE = new DummyBindableBuffer();
    
    @Override
    public int glBufferId() {
        return 0;
    }
    
    @Override
    public boolean bind() {
        return false;
    }

    @Override
    public void unbind() {
        
    }
}
