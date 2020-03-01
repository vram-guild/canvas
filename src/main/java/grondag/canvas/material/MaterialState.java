package grondag.canvas.material;

import net.minecraft.util.math.MathHelper;

import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
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

	public final MaterialVertexFormat bufferFormat;

	public final int index;

	public final long sortIndex;

	private MaterialState(MaterialContext context, VertexEncoder encoder, DrawHandler drawHandler, int index) {
		this.context = context;
		this.encoder = encoder;
		bufferFormat = encoder.format;
		this.drawHandler = drawHandler;
		this.index = index;
		sortIndex = (bufferFormat.vertexStrideBytes << 24) | index;
	}

	private static final int ENCODER_SHIFT = Useful.bitLength(MathHelper.smallestEncompassingPowerOfTwo(MaterialContext.values().length));
	private static final int DRAW_HANDLER_SHIFT = ENCODER_SHIFT + Useful.bitLength(VertexEncoders.MAX_ENCODERS);

	// TODO: make configurable
	public static int MAX_MATERIAL_STATES = 0xFFFF;

	private static final MaterialState[] VALUES = new MaterialState[0xFFFF];

	public static MaterialState get(MaterialContext context, MutableQuadViewImpl quad) {
		// analyze quad for lighting/color/texture content to allow for compact encoding, subject to material constraints
		final Value mat = quad.material();

		final MaterialVertexFormat format = MaterialVertexFormats.get(context, mat, quad);

		final VertexEncoder encoder = VertexEncoders.get(context, format, mat);

		final DrawHandler drawHandler = DrawHandlers.get(context, format, mat);

		assert encoder.format == drawHandler.format;

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
		return context.ordinal() | (encoder.index << ENCODER_SHIFT) | (drawHandler.index << DRAW_HANDLER_SHIFT);
	}
}
