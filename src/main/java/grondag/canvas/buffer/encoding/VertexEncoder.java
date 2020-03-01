package grondag.canvas.buffer.encoding;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.material.MaterialVertexFormat;

public abstract class VertexEncoder {
	private static int nextEncoderIndex = 0;

	public final int index = nextEncoderIndex++;

	public final MaterialVertexFormat format;

	VertexEncoder(MaterialVertexFormat format) {
		this.format = format;
	}

	/**
	 * Determines color index and render layer, then routes to appropriate
	 * tesselate routine based on material properties.
	 */
	public final void encodeQuad(MutableQuadViewImpl quad, VertexEncodingContext context) {
		// needs to happen before offsets are applied
		context.computeLighting(quad);

		colorizeQuad(quad, context);

		context.applyLighting(quad);

		bufferQuad(quad, context);
	}

	protected abstract void bufferQuad(MutableQuadViewImpl quad, VertexEncodingContext context);

	protected abstract void colorizeQuad(MutableQuadViewImpl quad, VertexEncodingContext context);
}
