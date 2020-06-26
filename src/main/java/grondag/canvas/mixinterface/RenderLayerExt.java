package grondag.canvas.mixinterface;

public interface RenderLayerExt {
	boolean canvas_isTranslucent();

	void canvas_blendModeIndex(int blendModeIndex);

	int canvas_blendModeIndex();
}
