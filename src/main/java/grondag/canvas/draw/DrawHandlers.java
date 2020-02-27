package grondag.canvas.draw;

import net.fabricmc.fabric.impl.client.indigo.renderer.RenderMaterialImpl.Value;

import grondag.canvas.material.MaterialBufferFormat;
import grondag.canvas.material.MaterialContext;

public class DrawHandlers {
	public static DrawHandler get(MaterialContext context, MaterialBufferFormat format, Value mat) {
		return VanillaDrawHandler.INSTANCE;
	}
}
