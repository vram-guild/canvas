package grondag.canvas.buffering;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

import grondag.canvas.core.RenderPipeline;

//PERF: provide diff buffers by vertex format and handle VAO binding 1X per buffer bind in buffers
public class MappedAllocationManager extends AbstractAllocationManager implements AllocationProvider {
    private static final ConcurrentSkipListMap<Long, MappedBuffer> BUFFERS = new ConcurrentSkipListMap<>();

    private static final int KEY_SHIFT_BITS = 16;
   
    @Override 
    protected void prepareForFrame() {
        MappedBufferStore.prepareEmpties();
    };
    
    @Override
    public void claimAllocation(RenderPipeline pipeline, int byteCount,
            Consumer<AbstractBufferDelegate<?>> consumer) {
        while (byteCount >= MappedBuffer.CAPACITY_BYTES) {
            MappedBuffer target = MappedBufferStore.getEmptyMapped();
            if (target == null)
                return;
            MappedBufferDelegate result = target.requestBytes(byteCount, pipeline.piplineVertexFormat().stride * 4);
            assert result != null;
            if (result == null)
                return;
            consumer.accept(result);
            byteCount -= result.byteCount();
            handleRemainder(target);
        }

        if (byteCount != 0)
            claimPartialAllocation(byteCount, consumer);
    }

    private static void claimPartialAllocation(final int byteCount, Consumer<AbstractBufferDelegate<?>> consumer) {
        final long byteKey = ((long) byteCount) << KEY_SHIFT_BITS;

        Long candidateKey = BUFFERS.ceilingKey(byteKey);

        while (candidateKey != null) {
            MappedBuffer target = BUFFERS.remove(candidateKey);
            if (target == null) {
                candidateKey = BUFFERS.ceilingKey(byteKey);
                continue;
            }

            MappedBufferDelegate result = target.requestBytes(byteCount, byteCount);
            assert result != null;
            if (result == null)
                return;
            assert result.byteCount() == byteCount;

            consumer.accept(result);
            handleRemainder(target);
            return;
        }

        // nothing available so get a new buffer
        MappedBuffer target = MappedBufferStore.getEmptyMapped();
        if (target == null)
            return;
        MappedBufferDelegate result = target.requestBytes(byteCount, byteCount);
        assert result != null;
        if (result == null)
            return;
        consumer.accept(result);
        handleRemainder(target);
    }

    private static void handleRemainder(MappedBuffer target) {
        final int remainingBytes = target.unallocatedBytes();
        if (remainingBytes < 4096)
            target.setFinal();
        else {
            final Long byteKey = (((long) remainingBytes) << KEY_SHIFT_BITS) | target.id;
            BUFFERS.put(byteKey, target);
        }
    }

    @Override
    protected void forceReload() {
        MappedBufferStore.forceReload();
        BUFFERS.clear();
    }

    @Override
    protected AllocationProvider getAllocator(int totalBytes) {
        return this;
    }
}
