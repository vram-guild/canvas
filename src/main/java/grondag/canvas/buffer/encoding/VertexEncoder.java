package grondag.canvas.buffer.encoding;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.material.MaterialVertexFormat;

/**
 * Encodes quads according to a specific GPU-side vertex format/shader/lighting scheme.
 * Also provides compatibility handlers for Mojang's vertex consumer interface.
 */
public abstract class VertexEncoder {
	public static final int FULL_BRIGHTNESS = 0xF000F0;

	public final MaterialVertexFormat format;

	protected VertexEncoder(MaterialVertexFormat format) {
		this.format = format;
	}

	/**
	 * Determines color index and render layer, then routes to appropriate
	 * tesselate routine based on material properties.
	 */
	public abstract void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context);

	public final void vertex(VertexCollectorImpl collector, double x, double y, double z) {
		collector.addf((float) x);
		collector.addf((float) y);
		collector.addf((float) z);
	}

	public final void vertex(VertexCollectorImpl collector, float x, float y, float z, float i, float j, float k, float l, float m, float n, int o, int p, float q, float r, float s) {
		collector.addf(x);
		collector.addf(y);
		collector.addf(z);
		collector.color(i, j, k, l);
		collector.texture(m, n);
		collector.overlay(o);
		collector.light(p);
		collector.normal(q, r, s);
	}

	public final void color(VertexCollectorImpl collector, int r, int g, int b, int a) {
		collector.addi((r & 0xFF) | ((g & 0xFF) << 8) | ((b & 0xFF) << 16) | ((a & 0xFF) << 24));
	}

	public final void texture(VertexCollectorImpl collector, float u, float v) {
		collector.addf(u);
		collector.addf(v);
	}

	public final void overlay(VertexCollectorImpl collector, int s, int t) {
		// TODO: disabled for now - needs to be controlled by format because is called when not present
		//add((s & 0xFFFF) | ((t & 0xFFFF) << 16));
	}

	public void light(VertexCollectorImpl collector, int blockLight, int skyLight) {
		collector.addi((blockLight & 0xFFFF) | ((skyLight & 0xFFFF) << 16));
	}

	public final void normal(VertexCollectorImpl collector, float x, float y, float z) {
		collector.addi(NormalHelper.packNormal(x, y, z, 1));
	}
}
