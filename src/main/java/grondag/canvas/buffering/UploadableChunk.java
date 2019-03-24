package grondag.canvas.buffering;

import grondag.canvas.core.VertexCollectorList;
import grondag.canvas.core.VertexPackingList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public abstract class UploadableChunk<V extends DrawableChunk> {
    protected final ObjectArrayList<DrawableDelegate> delegates = DelegateLists.getReadyDelegateList();

    /** Does not retain packing list reference */
    protected UploadableChunk(VertexPackingList packingList, VertexCollectorList collectorList) {
        VertexPacker.pack(delegates, packingList, collectorList, BufferManager.ALLOCATION_MANAGER.getAllocator(packingList.totalBytes()));
    }

    /**
     * Will be called from client thread - is where flush/unmap needs to happen.
     */
    public abstract V produceDrawable();

    /**
     * Called if {@link #produceDrawable()} will not be called, so can release
     * MappedBuffer(s).
     */
    public final void cancel() {
        final int limit = delegates.size();
        for (int i = 0; i < limit; i++)
            delegates.get(i).release();

        delegates.clear();
    }

    public static class Solid extends UploadableChunk<DrawableChunk.Solid> {
        public Solid(VertexPackingList packing, VertexCollectorList collectorList) {
            super(packing, collectorList);
        }

        @Override
        public DrawableChunk.Solid produceDrawable() {
            final int limit = delegates.size();
            for (int i = 0; i < limit; i++)
                delegates.get(i).flush();
            return new DrawableChunk.Solid(delegates);
        }
    }

    public static class Translucent extends UploadableChunk<DrawableChunk.Translucent> {
        public Translucent(VertexPackingList packing, VertexCollectorList collectorList) {
            super(packing, collectorList);
        }

        @Override
        public DrawableChunk.Translucent produceDrawable() {
            final int limit = delegates.size();
            for (int i = 0; i < limit; i++)
                delegates.get(i).flush();
            return new DrawableChunk.Translucent(delegates);
        }
    }
}
