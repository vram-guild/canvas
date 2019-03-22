package grondag.canvas.core;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Consumer;

import grondag.canvas.buffering.UploadableChunk;
import net.minecraft.util.math.MathHelper;

public class VertexCollectorList {
    private static final Comparator<VertexCollector> vertexCollectionComparator = new Comparator<VertexCollector>() {
        @Override
        public int compare(VertexCollector o1, VertexCollector o2) {
            // note reverse order - take most distant first
            return Double.compare(o2.firstUnpackedDistance(), o1.firstUnpackedDistance());
        }
    };

    private static final ThreadLocal<PriorityQueue<VertexCollector>> sorters = new ThreadLocal<PriorityQueue<VertexCollector>>() {
        @Override
        protected PriorityQueue<VertexCollector> initialValue() {
            return new PriorityQueue<VertexCollector>(vertexCollectionComparator);
        }

        @Override
        public PriorityQueue<VertexCollector> get() {
            PriorityQueue<VertexCollector> result = super.get();
            result.clear();
            return result;
        }
    };

    /**
     * Fast lookup of buffers by pipeline index. Null in CUTOUT layer buffers.
     */
    private VertexCollector[] vertexCollectors = new VertexCollector[PipelineManager.MAX_PIPELINES];

    private int maxIndex = -1;

    /** used in transparency layer sorting - updated with player eye coordinates */
    private double viewX;
    /** used in transparency layer sorting - updated with player eye coordinates */
    private double viewY;
    /** used in transparency layer sorting - updated with player eye coordinates */
    private double viewZ;

    /** used in transparency layer sorting - updated with origin of render cube */
    int renderOriginX = Integer.MIN_VALUE;
    /** used in transparency layer sorting - updated with origin of render cube */
    int renderOriginY = Integer.MIN_VALUE;
    /** used in transparency layer sorting - updated with origin of render cube */
    int renderOriginZ = Integer.MIN_VALUE;

    /**
     * Releases any held vertex collectors and resets state
     */
    void clear() {
        renderOriginX = Integer.MIN_VALUE;
        renderOriginY = Integer.MIN_VALUE;
        renderOriginZ = Integer.MIN_VALUE;

        final int limit = maxIndex;
        if (limit == -1)
            return;

        maxIndex = -1;
        for (int i = 0; i <= limit; i++) {
            VertexCollector vc = vertexCollectors[i];
            if(vc != null) {
                vc.clear();
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        clear();
    }

    public final boolean isEmpty() {
        return this.maxIndex == -1;
    }

    /**
     * Saves player eye coordinates for vertex sorting. Normally these will be the
     * same values for every list but save in instance to stay consistent with
     * Vanilla and possibly to support mods that do strange things with view entity
     * perspective. (PortalGun?)
     */
    public void setViewCoordinates(double x, double y, double z) {
        viewX = x;
        viewY = y;
        viewZ = z;
    }

    /**
     * Called when a render chunk initializes buffer builders with offset.
     */
    public void setRenderOrigin(double x, double y, double z) {
        renderOriginX = RenderCube.renderCubeOrigin(MathHelper.fastFloor(x));
        renderOriginY = RenderCube.renderCubeOrigin(MathHelper.fastFloor(y));
        renderOriginZ = RenderCube.renderCubeOrigin(MathHelper.fastFloor(z));
    }

    public final VertexCollector get(RenderPipelineImpl pipeline) {
        return get(pipeline.getIndex());
    }
    
    public final VertexCollector get(int pipelineIndex) {
        if (pipelineIndex > maxIndex) {
            maxIndex = pipelineIndex;
        }
        
        VertexCollector result = vertexCollectors[pipelineIndex];
        if(result == null) {
            result = new VertexCollector(PipelineManager.INSTANCE.getPipelineByIndex(pipelineIndex), this);
            vertexCollectors[pipelineIndex] = result;
        }
        return result;
    }

    public final void forEachExisting(Consumer<VertexCollector> consumer) {
        final int limit = maxIndex;
        if (limit == -1)
            return;

        for (int i = 0; i <= limit; i++) {
            VertexCollector vc = vertexCollectors[i];
            if (vc == null)
                continue;
            consumer.accept(vc);
        }
    }

    public final UploadableChunk.Solid packUploadSolid() {
        VertexPackingList packing = new VertexPackingList();

        // NB: for solid render, relying on pipelines being added to packing in
        // numerical order so that
        // all chunks can iterate pipelines independently while maintaining same
        // pipeline order within chunk
        forEachExisting(vertexCollector -> {
            final int vertexCount = vertexCollector.vertexCount();
            if (vertexCount != 0)
                packing.addPacking(vertexCollector.pipeline(), vertexCount);
        });

        if (packing.size() == 0)
            return null;

        return new UploadableChunk.Solid(packing, this);
    }

    public final UploadableChunk.Translucent packUploadTranslucent() {
        final VertexPackingList packing = new VertexPackingList();

        final PriorityQueue<VertexCollector> sorter = sorters.get();

        final double x = viewX - renderOriginX;
        final double y = viewY - renderOriginY;
        final double z = viewZ - renderOriginZ;

        // Sort quads within each pipeline, while accumulating in priority queue
        forEachExisting(vertexCollector -> {
            if (vertexCollector.vertexCount() != 0) {
                vertexCollector.sortQuads(x, y, z);
                sorter.add(vertexCollector);
            }
        });

        // exploit special case when only one transparent pipeline in this render chunk
        if (sorter.size() == 1) {
            VertexCollector only = sorter.poll();
            packing.addPacking(only.pipeline(), only.vertexCount());
        } else if (sorter.size() != 0) {
            VertexCollector first = sorter.poll();
            VertexCollector second = sorter.poll();
            do {
                // x4 because packing is vertices vs quads
                packing.addPacking(first.pipeline(), 4 * first.unpackUntilDistance(second.firstUnpackedDistance()));

                if (first.hasUnpackedSortedQuads())
                    sorter.add(first);

                first = second;
                second = sorter.poll();

            } while (second != null);

            packing.addPacking(first.pipeline(), 4 * first.unpackUntilDistance(Double.MIN_VALUE));
        }

        return packing.size() == 0 ? null : new UploadableChunk.Translucent(packing, this);
    }

    public int[][] getCollectorState(int[][] priorState) {
        int[][] result = priorState;

        if (result == null || result.length != maxIndex + 1)
            result = new int[maxIndex + 1][0];

        for (int i = 0; i <= maxIndex; i++)
            result[i] = this.vertexCollectors[i].saveState(result[i]);

        return result;
    }

    public void loadCollectorState(int[][] stateData) {
        maxIndex = stateData.length - 1;
        for (int i = 0; i <= maxIndex; i++) {
            get(i).loadState(stateData[i]);
        }
    }
}
