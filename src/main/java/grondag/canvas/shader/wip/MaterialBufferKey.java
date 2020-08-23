package grondag.canvas.shader.wip;

import grondag.canvas.buffer.encoding.VertexEncoder;
import grondag.canvas.material.MaterialVertexFormat;

/**
 * Primitives with the same buffer key can share the same buffer.
 * They should share the same encoder/vertex format, same  and have the same sorting requirements.
 *
 * Content of the buffer should also share the same matrix state but this is
 * not enforced and must be controlled through appropriate usage.
 *
 */
public class MaterialBufferKey {
	public final MaterialVertexFormat format;
	public final VertexEncoder encoder;
	/** true only for translucent */
	public final boolean sorted;

	private MaterialBufferKey(VertexEncoder encoder,  MaterialVertexFormat format, boolean sorted) {
		this.format = format;
		this.encoder = encoder;
		this.sorted = sorted;
	}
}
