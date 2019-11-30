package grondag.canvas.draw;

import grondag.canvas.material.ShaderContext;

public interface TessellatorExt {
	void canvas_draw();

	void canvas_context(ShaderContext context);

	ShaderContext canvas_context();
}
