package grondag.canvas.material;

import net.fabricmc.fabric.impl.client.indigo.renderer.RenderMaterialImpl;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.buffer.encoding.VertexEncoder;
import grondag.canvas.buffer.encoding.VertexEncoders;
import grondag.canvas.draw.DrawHandler;
import grondag.canvas.draw.DrawHandlers;
import grondag.fermion.varia.Useful;

public class MaterialState {
	// vertices with the same target can share the same buffer
	public final MaterialContext context;

	// controls how vertices are written to buffers and does the writing
	// output format must match input format of draw handler
	public final VertexEncoder encoder;

	// sets up gl state, updates uniforms and does draw.  For shaders, handles vertex attributes and same handler implies same shader.
	// input format must match output format of draw handler
	public final DrawHandler drawHandler;

	public final int index;

	private MaterialState(MaterialContext context, VertexEncoder encoder, DrawHandler drawHandler, int index) {
		this.context = context;
		this.encoder = encoder;
		this.drawHandler = drawHandler;
		this.index = index;
	}

	private static final int ENCODER_SHIFT = Useful.bitLength(MaterialContext.MAX_CONTEXTS);
	private static final int DRAW_HANDLER_SHIFT = ENCODER_SHIFT + Useful.bitLength(VertexEncoders.MAX_ENCODERS);

	// TODO: make configurable
	public static int MAX_MATERIAL_STATES = 0xFFFF;

	private static final MaterialState[] VALUES = new MaterialState[0xFFFF];

	public static MaterialState get(MaterialContext context, RenderMaterialImpl.Value mat, MutableQuadViewImpl quad) {
		// analyze quad for lighting/color/texture content to allow for compact encoding, subject to material constraints
		final MaterialBufferFormat format = MaterialBufferFormat.get(context, mat, quad);

		final VertexEncoder encoder = VertexEncoders.get(context, format, mat);

		final DrawHandler drawHandler = DrawHandlers.get(context, format, mat);

		assert encoder.outputFormat() == drawHandler.inputFormat();

		final int index = index(context, encoder, drawHandler);

		MaterialState result = VALUES[index];

		if (result == null) {
			synchronized(VALUES) {
				result = VALUES[index];

				if (result == null) {
					result = new MaterialState(context, encoder, drawHandler, index);
					VALUES[index] = result;
				}
			}
		}

		return result;
	}

	public static MaterialState get(int index) {
		return  VALUES[index];
	}

	private static int index(MaterialContext context, VertexEncoder encoder, DrawHandler drawHandler) {
		return context.index | (encoder.index() << ENCODER_SHIFT) | (drawHandler.index() << DRAW_HANDLER_SHIFT);
	}
}
