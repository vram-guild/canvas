package grondag.canvas.buffering;

import java.nio.IntBuffer;

import grondag.canvas.core.ConditionalPipeline;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BufferPacker {
    private static final ThreadLocal<BufferPacker> THREADLOCAL = ThreadLocal.withInitial(BufferPacker::new);
    
    ObjectArrayList<DrawableDelegate> delegates;
    VertexCollectorList collectorList;
    AllocationProvider allocator;
    
    private BufferPacker() {
        //private
    }
    
    /** Does not retain packing list reference */
    public static ObjectArrayList<DrawableDelegate> pack(BufferPackingList packingList, VertexCollectorList collectorList, AllocationProvider allocator) {
        final BufferPacker packer = THREADLOCAL.get();
        final ObjectArrayList<DrawableDelegate> result = DelegateLists.getReadyDelegateList();
        packer.delegates = result;
        packer.collectorList = collectorList;
        packer.allocator = allocator;
        packingList.forEach(packer);
        packer.delegates = null;
        packer.collectorList = null;
        packer.allocator = null;
        return result;
    }

    public void accept(ConditionalPipeline conditionalPipeline, int vertexStart, int vertexCount) {
        final int stride = conditionalPipeline.pipeline.piplineVertexFormat().vertexStrideBytes;
        allocator.claimAllocation(conditionalPipeline, vertexCount * stride, ref -> {
            final int byteOffset = ref.byteOffset();
            final int byteCount = ref.byteCount();
            final int intLength = byteCount / 4;

            ref.lockForUpload();
            final IntBuffer intBuffer = ref.intBuffer();
            intBuffer.position(byteOffset / 4);
            intBuffer.put(collectorList.get(conditionalPipeline).rawData(), vertexStart * stride / 4, intLength);
            ref.unlockForUpload();

            delegates.add(DrawableDelegate.claim(ref, conditionalPipeline, byteCount / stride));
        });
    }
}