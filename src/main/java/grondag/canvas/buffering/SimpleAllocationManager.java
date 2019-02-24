package grondag.canvas.buffering;

public class SimpleAllocationManager extends AbstractAllocationManager {
    @Override
    protected AllocationProvider getAllocator(int totalBytes) {
        return new SimpleBuffer(totalBytes);
    }
}
