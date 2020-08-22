package grondag.canvas.material;

// WIP: remove this
public enum MaterialContext {
	TERRAIN(true, false, false),
	BLOCK(true, false, false),
	ITEM_HELD(false, true, false),
	ITEM_HEAD(false, true, false),
	ITEM_GUI(false, true, true),
	ITEM_GROUND(false, true, false),
	ITEM_FIXED(false, true, false),
	ENTITY_BLOCK(true, false, false),
	ENTITY_ITEM(false, true, false),
	ENTITY_ENTITY(false, false, false),
	PROCESS(false, false, false);

	public final boolean isBlock;
	public final boolean isItem;
	public final boolean isGui;
	public final boolean isWorld;

	private MaterialContext(boolean isBlock, boolean isItem, boolean isGui) {
		this.isBlock = isBlock;
		this.isItem = isItem;
		this.isGui = isGui;
		isWorld = !isGui;
	}
}
