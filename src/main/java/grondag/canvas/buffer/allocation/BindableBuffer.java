package grondag.canvas.buffer.allocation;

public interface BindableBuffer {
    int glBufferId();
    
    void bind();

    void unbind();
}
