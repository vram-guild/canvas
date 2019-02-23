package grondag.acuity.buffering;

import java.nio.IntBuffer;

import javax.annotation.Nullable;

import grondag.acuity.api.RenderPipeline;
import grondag.acuity.core.VertexCollectorList;
import grondag.acuity.core.VertexPackingList;
import grondag.acuity.core.VertexPackingList.VertexPackingConsumer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public abstract class UploadableChunk<V extends DrawableChunk>
{
    protected final VertexPackingList packingList;
    protected final ObjectArrayList<DrawableChunkDelegate> delegates = DelegateLists.getReadyDelegateList();
    
    private static class UploadConsumer implements VertexPackingConsumer
    {
        ObjectArrayList<DrawableChunkDelegate> delegates;
        VertexCollectorList collectorList;
        int intOffset = 0;
        
        void prepare(ObjectArrayList<DrawableChunkDelegate> delegates, VertexCollectorList collectorList)
        {
            this.delegates = delegates;
            this.collectorList = collectorList;
        }
        
        @Override
        public void accept(RenderPipeline pipeline, int vertexCount)
        {
            final int stride = pipeline.piplineVertexFormat().stride;
            // array offset will be zero unless multiple buffers are needed
            intOffset = 0;
            AllocationManager.claimAllocation(pipeline, vertexCount * stride, ref ->
            {
                final int byteOffset = ref.byteOffset();
                final int byteCount = ref.byteCount();
                final int intLength = byteCount / 4;
                
                ref.lockForUpload();
                final IntBuffer intBuffer = ref.intBuffer();
                intBuffer.position(byteOffset / 4);
                intBuffer.put(collectorList.getIfExists(pipeline.getIndex()).rawData(), intOffset, intLength);
                ref.unlockForUpload();
                
                intOffset += intLength;
                final DrawableChunkDelegate delegate = new DrawableChunkDelegate(ref, pipeline, byteCount / stride);
                delegates.add(delegate);
            });            
        }
    }
    
    ThreadLocal<UploadConsumer> uploadConsumer = new ThreadLocal<UploadConsumer>()
    {
        @Override
        protected UploadConsumer initialValue()
        {
            return new UploadConsumer();
        }
    };
    
    protected UploadableChunk(VertexPackingList packingList, VertexCollectorList collectorList)
    {
        this.packingList = packingList;
        UploadConsumer uc = uploadConsumer.get();
        uc.prepare(delegates, collectorList);
        packingList.forEach(uc);
    }
    
    @Override
    protected void finalize()
    {
        assert this.delegates != null;
        
    }
    
    /**
     * Will be called from client thread - is where flush/unmap needs to happen.
     */
    public abstract @Nullable V produceDrawable();
    
    /**
     * Called if {@link #produceDrawable()} will not be called, 
     * so can release MappedBuffer(s).
     */
    public final void cancel()
    {
        final int limit = delegates.size();
        for(int i = 0; i < limit; i++)
            delegates.get(i).release();
        
        delegates.clear();
    }
    
    public static class Solid extends UploadableChunk<DrawableChunk.Solid>
    {
        public Solid(VertexPackingList packing, VertexCollectorList collectorList)
        {
            super(packing, collectorList);
        }

        @Override
        public @Nullable DrawableChunk.Solid produceDrawable()
        {
            final int limit = delegates.size();
            for(int i = 0; i < limit; i++)
                delegates.get(i).flush();
            return new DrawableChunk.Solid(delegates);
        }
    }

    public static class Translucent extends UploadableChunk<DrawableChunk.Translucent>
    {
        public Translucent(VertexPackingList packing, VertexCollectorList collectorList)
        {
            super(packing, collectorList);
        }

        @Override
        public @Nullable DrawableChunk.Translucent produceDrawable()
        {
            final int limit = delegates.size();
            for(int i = 0; i < limit; i++)
                delegates.get(i).flush();
            return new DrawableChunk.Translucent(delegates);
        }
    }
}
