package grondag.canvas.draw;

import net.fabricmc.fabric.impl.client.indigo.renderer.RenderMaterialImpl.Value;

import grondag.canvas.material.MaterialBufferFormat;
import grondag.canvas.material.MaterialContext;

public interface DrawHandler {
	static DrawHandler get() {
		return null;
	}

	int index();

	MaterialBufferFormat inputFormat();

	static DrawHandler get(MaterialContext context, MaterialBufferFormat format, Value mat) {
		// TODO Auto-generated method stub
		return null;
	}
}
