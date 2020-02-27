package grondag.canvas.buffer.encoding;


import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.material.MaterialBufferFormat;
import grondag.canvas.material.MaterialContext;

public class VertexEncoders {
	public static final int MAX_ENCODERS = 8;

	public static VertexEncoder get(MaterialContext context, MaterialBufferFormat format, Value mat) {
		return VanillaEncoder.INSTANCE;
	}
}
