package grondag.canvas.render;

// TODO: implement draw strategies
public enum DrawStrategy {
	/**
	 * Each shader or conditional has a separate draw call and (if necessary) program activation.
	 * Can only be used for non-translucent.
	 */
	MULTI_DRAW_MULTI_PROGRAM,

	/**
	 * Ubershader controlled by a uniform. Uniform is set before each draw call. Probably most optimal.
	 * Can only be used for non-translucent.
	 */
	MULTI_DRAW_SINGLE_PROGRAM,

	/**
	 * Ubershader controlled by vertex attributes. Requires extra vertex data and shader performance may suffer.
	 * Only option for mixed translucency that doesn't require frequent resorting or exotic approaches.
	 */
	SINGLE_DRAW_MULTI_PROGRAM
}
