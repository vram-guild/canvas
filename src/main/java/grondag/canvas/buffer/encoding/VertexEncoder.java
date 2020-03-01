package grondag.canvas.buffer.encoding;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.material.MaterialVertexFormat;

public abstract class VertexEncoder {
	public static final int FULL_BRIGHTNESS = 0xF000F0;

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
	public abstract void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context);
}
