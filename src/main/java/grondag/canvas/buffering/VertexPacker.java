package grondag.canvas.buffering;

import java.nio.IntBuffer;

import grondag.canvas.core.RenderPipelineImpl;
import grondag.canvas.core.VertexCollectorList;
import grondag.canvas.core.VertexPackingList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VertexPacker {
    private static final ThreadLocal<VertexPacker> THREADLOCAL = ThreadLocal.withInitial(VertexPacker::new);
    
    ObjectArrayList<DrawableDelegate> delegates;
    VertexCollectorList collectorList;
    int intOffset = 0;
    AllocationProvider allocator;
    
    private VertexPacker() {
        //private
    }
    
    /** Does not retain packing list reference */
    public static void pack(ObjectArrayList<DrawableDelegate> delegates, VertexPackingList packingList, VertexCollectorList collectorList, AllocationProvider allocator) {
        VertexPacker packer = THREADLOCAL.get();
        packer.delegates = delegates;
        packer.collectorList = collectorList;
        packer.allocator = allocator;
        packingList.forEach(packer);
    }

    public void accept(RenderPipelineImpl pipeline, int vertexCount) {
        final int stride = pipeline.piplineVertexFormat().vertexStrideBytes;
        // array offset will be zero unless multiple buffers are needed
        intOffset = 0;
        allocator.claimAllocation(pipeline, vertexCount * stride, ref -> {
            final int byteOffset = ref.byteOffset();
            final int byteCount = ref.byteCount();
            final int intLength = byteCount / 4;

            ref.lockForUpload();
            final IntBuffer intBuffer = ref.intBuffer();
            intBuffer.position(byteOffset / 4);
            intBuffer.put(collectorList.get(pipeline).rawData(), intOffset, intLength);
            ref.unlockForUpload();

            intOffset += intLength;
            delegates.add(DrawableDelegate.claim(ref, pipeline, byteCount / stride));
        });
    }
}