package grondag.canvas.render;

import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.shader.ShaderPass;
import grondag.canvas.varia.CanvasGlHelper;

public abstract class DrawHandler {
	private static DrawHandler current = null;

	private static int nextHandlerIndex = 0;

	public final int index = nextHandlerIndex++;

	public final MaterialVertexFormat format;
	public final ShaderPass shaderPass;

	DrawHandler (MaterialVertexFormat format, ShaderPass shaderPass) {
		assert shaderPass != ShaderPass.PROCESS;
		this.format = format;
		this.shaderPass = shaderPass;
	}

	public final void setup() {
		final DrawHandler d = current;

		if (d == null) {
			// PERF: really needed?  Doesn't seem to help or hurt
			// Important this happens BEFORE anything that could affect vertex state
			if (CanvasGlHelper.isVaoEnabled()) {
				CanvasGlHelper.glBindVertexArray(0);
			}

			setupInner();
			current = this;
		} else if (d != this) {
			// Important this happens BEFORE anything that could affect vertex state
			if (CanvasGlHelper.isVaoEnabled()) {
				CanvasGlHelper.glBindVertexArray(0);
			}

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
