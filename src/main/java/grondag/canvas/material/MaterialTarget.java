package grondag.canvas.material;

/**
 * Identifies render pass. Vertices with the same pass can share the same buffer.
 */
public enum MaterialTarget {
	SKYBOX,
	WORLD_SOLID,
	WORLD_TRANSLUCENT,
	PLAYER,
	GUI,
	HUD
}
