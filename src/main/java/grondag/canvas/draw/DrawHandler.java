package grondag.canvas.draw;

import javax.annotation.Nullable;

import net.minecraft.client.render.RenderLayer;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.apiimpl.MaterialShaderImpl;
import grondag.canvas.material.MaterialVertexFormat;

public abstract class DrawHandler {
	private static DrawHandler current = null;

	private static int nextHandlerIndex = 0;

	public final int index = nextHandlerIndex++;

	public final MaterialShaderImpl shader;
	public final MaterialConditionImpl condition;
	public final MaterialVertexFormat format;

	public @Nullable RenderLayer renderLayer;

	DrawHandler (MaterialVertexFormat format, MaterialShaderImpl shader,  MaterialConditionImpl condition, @Nullable RenderLayer renderLayer) {
		this.format = format;
		this.shader = shader;
		this.condition = condition;
		this.renderLayer = renderLayer;
	}

	public final void setup() {
		final DrawHandler d = current;

		if (d == null) {
			setupInner();
			current = this;
		} else if (d != this) {
			d.teardownInner();
			setupInner();
			current = this;
		}
	}

	public static void teardown() {
		if (current != null) {
			current.teardownInner();
			current = null;
		}
	}

	protected abstract void setupInner();
	//state.activate(OldShaderContext.BLOCK_TRANSLUCENT);

	protected abstract void teardownInner();
}
