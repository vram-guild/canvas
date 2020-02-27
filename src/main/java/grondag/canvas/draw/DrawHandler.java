package grondag.canvas.draw;

import grondag.canvas.material.MaterialBufferFormat;

public abstract class DrawHandler {
	private static int nextHandlerIndex = 0;

	final int index = nextHandlerIndex++;

	static DrawHandler get() {
		return null;
	}

	public final int index() {
		return index;
	}

	public abstract MaterialBufferFormat inputFormat();
}
