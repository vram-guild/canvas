package grondag.canvas.pipeline;

/**
 * Identifies render pass. Vertices with the same pass can share the same buffer.
 */
public enum PipelineTarget {
	SKYBOX,
	WORLD_SOLID,
	WORLD_TRANSLUCENT,
	PLAYER,
	GUI,
	HUD
}
