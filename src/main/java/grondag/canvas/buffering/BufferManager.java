package grondag.canvas.buffering;

public class BufferManager {
    public static final AbstractAllocationManager ALLOCATION_MANAGER = new SimpleAllocationManager();

    public static void prepareForFrame() {
        ALLOCATION_MANAGER.prepareForFrame();
    }
    
    public static void forceReload() {
        ALLOCATION_MANAGER.forceReload();
    }
}
