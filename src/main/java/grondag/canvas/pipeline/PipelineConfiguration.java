package grondag.canvas.pipeline;

import net.fabricmc.fabric.impl.client.indigo.renderer.RenderMaterialImpl;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.buffer.encoding.VertexEncoder;
import grondag.canvas.buffer.encoding.VertexEncoders;
import grondag.canvas.draw.DrawHandler;
import grondag.fermion.varia.Useful;

public class PipelineConfiguration {
	// vertices with the same target can share the same buffer
	public final PipelineContext context;

	// controls how vertices are written to buffers and does the writing
	// output format must match input format of draw handler
	public final VertexEncoder encoder;

	// sets up gl state, updates uniforms and does draw.  For shaders, handles vertex attributes and will be the same shader.
	// input format must match output format of draw handler
	public final DrawHandler drawHandler;

	public final int index;

	private PipelineConfiguration(PipelineContext context, VertexEncoder encoder, DrawHandler drawHandler, int index) {
		this.context = context;
		this.encoder = encoder;
		this.drawHandler = drawHandler;
		this.index = index;
	}

	private static final int ENCODER_SHIFT = Useful.bitLength(PipelineContext.MAX_CONTEXTS);
	private static final int DRAW_HANDLER_SHIFT = ENCODER_SHIFT + Useful.bitLength(VertexEncoders.MAX_ENCODERS);

	// TODO: make configurable
	public static int MAX_PIPELINE_COUNT = 0xFFFF;

	private static final PipelineConfiguration[] VALUES = new PipelineConfiguration[0xFFFF];

	public static PipelineConfiguration get(PipelineContext context, RenderMaterialImpl.Value mat, MutableQuadViewImpl quad) {
		// analyze quad for lighting/color/texture content to allow for compact encoding, subject to material constraints
		final BufferFormat format = BufferFormat.get(context, mat, quad);

		final VertexEncoder encoder = VertexEncoders.get(context, format, mat);

		final DrawHandler drawHandler = DrawHandler.get(context, format, mat);

		final int index = index(context, encoder, drawHandler);

		PipelineConfiguration result = VALUES[index];

		if (result == null) {
			synchronized(VALUES) {
				result = VALUES[index];

				if (result == null) {
					result = new PipelineConfiguration(context, encoder, drawHandler, index);
					VALUES[index] = result;
				}
			}
		}

		return result;
	}

	public static PipelineConfiguration get(int index) {
		return  VALUES[index];
	}

	private static int index(PipelineContext context, VertexEncoder encoder, DrawHandler drawHandler) {
		return context.index | (encoder.index() << ENCODER_SHIFT) | (drawHandler.index() << DRAW_HANDLER_SHIFT);
	}
}
