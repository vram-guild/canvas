package grondag.canvas.salvage;

import grondag.canvas.shader.old.OldShaderContext;

public interface TessellatorExt {
	void canvas_draw();

	void canvas_context(OldShaderContext context);

	OldShaderContext canvas_context();
}
