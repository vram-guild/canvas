package grondag.canvas.buffering;

public class VboAllocationManager extends AbstractAllocationManager {
    @Override
    protected AllocationProvider getAllocator(int totalBytes) {
        return new VboBuffer(totalBytes);
    }
}
