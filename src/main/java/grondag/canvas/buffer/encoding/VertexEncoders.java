package grondag.canvas.buffer.encoding;


import net.minecraft.client.render.RenderLayer;

import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.material.MaterialVertexFormat;

public class VertexEncoders {
	public static final int MAX_ENCODERS = 8;

	public static VertexEncoder get(MaterialContext context, MaterialVertexFormat format, Value mat) {
		return VanillaEncoder.INSTANCE;
	}

	public static VertexEncoder get(MaterialContext context, RenderLayer layer) {
		return VanillaEncoder.INSTANCE;
	}
}
