package grondag.canvas.buffer.allocation;

public interface BindableBuffer {
	/**
	 * Returns true if bind resulted in a new buffer binding, meaning
	 * that vertex bindings and other buffer-dependent state should also be refreshed.
	 */
	boolean bind();

	void unbind();

	int glBufferId();
}
