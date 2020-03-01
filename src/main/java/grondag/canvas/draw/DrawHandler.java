package grondag.canvas.draw;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.apiimpl.MaterialShaderImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.chunk.draw.DrawableDelegate;
import grondag.canvas.material.MaterialVertexFormat;

public abstract class DrawHandler {
	private static int nextHandlerIndex = 0;

	public final int index = nextHandlerIndex++;

	public final MaterialShaderImpl shader;
	public final MaterialConditionImpl condition;
	public final MaterialVertexFormat format;

	DrawHandler (MaterialVertexFormat format, Value mat) {
		this.format = format;
		shader = mat.shader;
		condition = mat.condition;
	}

	protected abstract void activate();

	public void draw(DrawableDelegate delegate) {
		//state.activate(OldShaderContext.BLOCK_TRANSLUCENT);
		activate();
		delegate.bind();
		delegate.draw();
	}
}
