package grondag.acuity.core;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.acuity.api.PipelineManager;
import grondag.acuity.api.RenderPipeline;
import grondag.acuity.buffering.UploadableChunk;
import net.minecraft.util.math.MathHelper;

public class VertexCollectorList
{
    private static final Comparator<VertexCollector> vertexCollectionComparator = new Comparator<VertexCollector>() 
    {
        @SuppressWarnings("null")
        @Override
        public int compare(VertexCollector o1, VertexCollector o2)
        {
            // note reverse order - take most distant first
            return Double.compare(o2.firstUnpackedDistance(), o1.firstUnpackedDistance());
        }
    };

    private static final ThreadLocal<PriorityQueue<VertexCollector>> sorters = new ThreadLocal<PriorityQueue<VertexCollector>>()
    {
        @Override
        protected PriorityQueue<VertexCollector> initialValue()
        {
            return new PriorityQueue<VertexCollector>(vertexCollectionComparator);
        }

        @Override
        public PriorityQueue<VertexCollector> get()
        {
            PriorityQueue<VertexCollector> result = super.get();
            result.clear();
            return result;
        }
    };

    /**
     * Fast lookup of buffers by pipeline index. Null in CUTOUT layer buffers.
     */
    private VertexCollector[] vertexCollectors  = new VertexCollector[PipelineManager.INSTANCE.pipelineCount()];

    private int maxIndex = -1;

    /** used in transparency layer sorting - updated with player eye coordinates */
    private double viewX;
    /** used in transparency layer sorting - updated with player eye coordinates */
    private double viewY;
    /** used in transparency layer sorting - updated with player eye coordinates */
    private double viewZ;

    /** used in transparency layer sorting - updated with origin of render cube */
    private int renderOriginX = Integer.MIN_VALUE;
    /** used in transparency layer sorting - updated with origin of render cube */
    private int renderOriginY = Integer.MIN_VALUE;
    /** used in transparency layer sorting - updated with origin of render cube */
    private int renderOriginZ = Integer.MIN_VALUE;

    VertexCollectorList()
    {
        for(int i = 0; i < vertexCollectors.length; i++)
            vertexCollectors[i] = new VertexCollector(PipelineManager.INSTANCE.getPipeline(i), this);
    }

    /**
     * Releases any held vertex collectors and resets state
     */
    void clear()
    {
        renderOriginX = Integer.MIN_VALUE;
        renderOriginY = Integer.MIN_VALUE;
        renderOriginZ = Integer.MIN_VALUE;
        
        final int limit = maxIndex;
        if(limit == -1) 
            return;

        maxIndex = -1;
        for(int i = 0; i <= limit; i++)
            vertexCollectors[i].clear();
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        clear();
    }

    public final boolean isEmpty()
    {
        return this.maxIndex == -1;
    }

    /**
     * Saves player eye coordinates for vertex sorting.
     * Normally these will be the same values for every list but
     * save in instance to stay consistent with Vanilla and possibly to 
     * support mods that do strange things with view entity perspective. (PortalGun?) 
     */
    public void setViewCoordinates(double x, double y, double z)
    {
        viewX = x;
        viewY = y;
        viewZ = z;
    }


    /**
     * Called by child collectors the first time they get a vertex.
     * All collectors in the list should share the same render origin.
     */
    public void setRenderOrigin(float x, float y, float z)
    {
        final int rX = RenderCube.renderCubeOrigin(MathHelper.fastFloor(x));
        final int rY = RenderCube.renderCubeOrigin(MathHelper.fastFloor(y));
        final int rZ = RenderCube.renderCubeOrigin(MathHelper.fastFloor(z));
        
        if(renderOriginX == Integer.MIN_VALUE)
        {
            renderOriginX = rX;
            renderOriginY = rY;
            renderOriginZ = rZ;
        }
        else
        {
            assert renderOriginX == rX;
            assert renderOriginY == rY;
            assert renderOriginZ == rZ;
        }
    }

    public final VertexCollector getIfExists(final int pipelineIndex)
    {
        return vertexCollectors[pipelineIndex];
    }

    public final VertexCollector get(RenderPipeline pipeline)
    {
        final int index = pipeline.getIndex();
        if(index > maxIndex)
            maxIndex = index;
        return vertexCollectors[index];
    }

    public final void forEachExisting(Consumer<VertexCollector> consumer)
    {
        final int limit = maxIndex;
        if(limit == -1) 
            return;

        for(int i = 0; i <= limit; i++)
        {
            VertexCollector vc = vertexCollectors[i];
            if(vc == null)
                continue;
            consumer.accept(vc);
        }
    }

    public final @Nullable UploadableChunk.Solid packUploadSolid()
    {
        VertexPackingList packing = new VertexPackingList();

        // NB: for solid render, relying on pipelines being added to packing in numerical order so that
        // all chunks can iterate pipelines independently while maintaining same pipeline order within chunk
        forEachExisting(vertexCollector ->
        {
            final int vertexCount = vertexCollector.vertexCount();
            if(vertexCount != 0)
                packing.addPacking(vertexCollector.pipeline(), vertexCount);
        });

        if(packing.size() == 0)
            return null;
        
        return new UploadableChunk.Solid(packing, this);
    }

    public final @Nullable UploadableChunk.Translucent packUploadTranslucent()
    {
        final VertexPackingList packing = new VertexPackingList();

        final PriorityQueue<VertexCollector> sorter = sorters.get();

        final double x = viewX - renderOriginX;
        final double y = viewY - renderOriginY;
        final double z = viewZ - renderOriginZ;
        
        // Sort quads within each pipeline, while accumulating in priority queue
        forEachExisting(vertexCollector ->
        {
            if(vertexCollector.vertexCount() != 0)
            {            
                vertexCollector.sortQuads(x, y, z);
                sorter.add(vertexCollector);
            }
        });

        // exploit special case when only one transparent pipeline in this render chunk
        if(sorter.size() == 1)
        {
            VertexCollector only = sorter.poll();
            packing.addPacking(only.pipeline(), only.vertexCount());
        }
        else if(sorter.size() != 0)
        {
            VertexCollector first = sorter.poll();
            VertexCollector second = sorter.poll();
            do
            {   
                // x4 because packing is vertices vs quads
                packing.addPacking(first.pipeline(), 4 * first.unpackUntilDistance(second.firstUnpackedDistance()));

                if(first.hasUnpackedSortedQuads())
                    sorter.add(first);

                first = second;
                second = sorter.poll();

            } while(second != null);

            packing.addPacking(first.pipeline(), 4 * first.unpackUntilDistance(Double.MIN_VALUE));
        }

        return packing.size() == 0 ? null : new UploadableChunk.Translucent(packing, this);
    }

    public int[][] getCollectorState(@Nullable int[][] priorState)
    {
        int[][] result = priorState;
        
        if(result == null || result.length != maxIndex + 1)
            result = new int[maxIndex + 1][0];
        
        for(int i = 0; i <= maxIndex; i++)
            result[i] = this.vertexCollectors[i].saveState(result[i]);
        
       return result;
    }
    
    public void loadCollectorState(int[][] stateData)
    {
        maxIndex = stateData.length - 1;
        for(int i = 0; i <= maxIndex; i++)
            this.vertexCollectors[i].loadState(stateData[i]);
    }
}
