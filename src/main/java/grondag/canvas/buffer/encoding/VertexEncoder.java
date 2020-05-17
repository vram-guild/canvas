package grondag.canvas.buffer.encoding;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.buffer.packing.VertexCollectorImpl;
import grondag.canvas.material.MaterialVertexFormat;

/**
 * Encodes quads according to a specific GPU-side vertex format/shader/lighting scheme.
 * Also provides compatibility handlers for Mojang's vertex consumer interface.
 */
public abstract class VertexEncoder {
	public static final int FULL_BRIGHTNESS = 0xF000F0;

	/** Two encoders can share the same index but only one will ever be active (based on config) */
	public final int index;

	public final MaterialVertexFormat format;

	protected VertexEncoder(MaterialVertexFormat format, int index) {
		this.format = format;
		this.index = index;
	}

	/**
	 * Determines color index and render layer, then routes to appropriate
	 * tesselate routine based on material properties.
	 */
	public abstract void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context);

	public abstract void vertex(VertexCollectorImpl collector, double x, double y, double z);

	public abstract void vertex(VertexCollectorImpl collector, float x, float y, float z, float i, float j, float k, float l, float m, float n, int o, int p, float q, float r, float s);

	public abstract void color(VertexCollectorImpl collector, int r, int g, int b, int a);

	public abstract void texture(VertexCollectorImpl collector, float u, float v);

	public abstract void overlay(VertexCollectorImpl collector, int s, int t);

	public abstract void light(VertexCollectorImpl collector, int s, int t);

	public abstract void normal(VertexCollectorImpl collector, float x, float y, float z);
}
