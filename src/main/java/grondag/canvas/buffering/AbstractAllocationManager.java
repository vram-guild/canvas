package grondag.canvas.buffering;

public abstract class AbstractAllocationManager {

    /**
     * Consumer must not exceed total bytes initially requested.
     * This allows for fixed-size buffer allocation and may allow optimization of other allocation schemes.
     */
    protected abstract AllocationProvider getAllocator(int totalBytes);
    
    /**
     * Override if allocation type has per-frame upkeep
     */
    protected void prepareForFrame() {}

    /**
     * Override if allocation type needs to handle;
     */
    protected void forceReload() { };
}
