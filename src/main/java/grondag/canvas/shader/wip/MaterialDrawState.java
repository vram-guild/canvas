package grondag.canvas.shader.wip;

/**
 * Primitives with the same DrawState have the same vertex format/buffer,
 * and same uniform state and gl state.
 *
 * Primitives with draw states will be collected in different arrays
 * and then packed into shared buffers (if applicable) with the same buffer key.
 *
 * Order of packing will be a hierarchy of Gl state and uniform state
 */
public class MaterialDrawState {
	public final MaterialBufferKey bufferKey;
	public final MaterialGlState drawState;
	public final MaterialUniformState uniformState;

	private MaterialDrawState(MaterialBufferKey bufferKey, MaterialGlState drawState, MaterialUniformState uniformState) {
		this.bufferKey = bufferKey;
		this.drawState = drawState;
		this.uniformState = uniformState;
	}
}
