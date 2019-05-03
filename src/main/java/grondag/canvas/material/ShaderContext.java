package grondag.canvas.material;

public enum ShaderContext {
    BLOCK_SOLID(true, false),
    BLOCK_TRANSLUCENT(true, false),
    ITEM_WORLD(false, true),
    ITEM_GUI(false, true);
    
    public final boolean isBlock;
    public final boolean isItem;
    
    private ShaderContext(boolean isBlock, boolean isItem) {
        this.isBlock = isBlock;
        this.isItem = isItem;
    }
}
