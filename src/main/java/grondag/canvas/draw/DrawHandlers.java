package grondag.canvas.draw;


import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.material.MaterialBufferFormat;
import grondag.canvas.material.MaterialContext;

public class DrawHandlers {
	public static DrawHandler get(MaterialContext context, MaterialBufferFormat format, Value mat) {
		return VanillaDrawHandler.INSTANCE;
	}
}
