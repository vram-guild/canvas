package grondag.canvas.render;

import grondag.canvas.mixinterface.MultiPhaseExt;

public enum RenderLayerHandler {
	INSTANCE;

	private static boolean enableShaderDraw = false;

	public static void enableShaderDraw(boolean enable) {
		enableShaderDraw = enable;
	}

	public static void startDrawing(MultiPhaseExt renderLayer) {
		if (enableShaderDraw) {
			renderLayer.canvas_startDrawing();
		} else {
			renderLayer.canvas_startDrawing();
		}
	}

	public static void endDrawing(MultiPhaseExt renderLayer) {
		if (enableShaderDraw) {
			renderLayer.canvas_endDrawing();
		} else {
			renderLayer.canvas_endDrawing();
		}
	}
}
