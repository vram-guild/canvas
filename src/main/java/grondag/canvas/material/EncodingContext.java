package grondag.canvas.material;

/**
 * Describes the type of render context
 * and controls how materials are packed to buffers.
 */
public enum EncodingContext {
	TERRAIN(true, false, false),
	BLOCK(true, false, false),
	ITEM(false, true, false),
	ENTITY_BLOCK(true, false, false),
	ENTITY_ITEM(false, true, false),
	ENTITY_ENTITY(false, false, false),
	PROCESS(false, false, false);

	public final boolean isBlock;
	public final boolean isItem;
	public final boolean isGui;
	public final boolean isWorld;

	private EncodingContext(boolean isBlock, boolean isItem, boolean isGui) {
		this.isBlock = isBlock;
		this.isItem = isItem;
		this.isGui = isGui;
		isWorld = !isGui;
	}
}
