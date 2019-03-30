package grondag.canvas.buffering;

public class VboBufferManager {
    public static final AbstractAllocationManager ALLOCATION_MANAGER = new VboAllocationManager();

    public static void prepareForFrame() {
        ALLOCATION_MANAGER.prepareForFrame();
    }
    
    public static void forceReload() {
        ALLOCATION_MANAGER.forceReload();
    }
}
