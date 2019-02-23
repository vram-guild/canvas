package grondag.acuity.core;

import java.util.function.Consumer;

import grondag.acuity.api.RenderPipeline;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Tracks number of vertices, pipeline and sequence thereof within a buffer.
 */
@SideOnly(Side.CLIENT)
public class VertexPackingList
{
    private int[] counts = new int[16];
    private RenderPipeline[] pipelines = new RenderPipeline[16];
    
    private int size = 0;
    private int totalBytes = 0;
    
    public void clear()
    {
        this.size = 0;
        this.totalBytes = 0;
    }
    
    public int size()
    {
        return this.size;
    }
    
    /**
     * For performance testing.
     */
    public int quadCount()
    {
        if(this.size == 0) 
            return 0;
        
        int quads = 0;
        
        for(int i = 0; i < this.size; i++)
        {
            quads += this.counts[i] / 4;
        }
        return quads;
    }
    
    public int totalBytes()
    {
        return this.totalBytes;
    }
    
    public void addPacking(RenderPipeline pipeline, int vertexCount)
    {
        if (size == this.pipelines.length)
        {
            final int iCopy[] = new int[size * 2];
            System.arraycopy(this.counts, 0, iCopy, 0, size);
            this.counts  = iCopy;
            
            final RenderPipeline pCopy[] = new RenderPipeline[size * 2];
            System.arraycopy(this.pipelines, 0, pCopy, 0, size);
            this.pipelines  = pCopy;
        }
        this.pipelines[size] = pipeline;
        this.counts[size] = vertexCount;
        this.totalBytes += pipeline.piplineVertexFormat().stride * vertexCount;
        this.size++;
    }
    
    public static interface VertexPackingConsumer
    {
        void accept(RenderPipeline pipeline, int vertexCount);
    }
    
    public final void forEach(VertexPackingConsumer consumer)
    {
        final int size = this.size;
        for(int i = 0; i < size; i++)
        {
            consumer.accept(this.pipelines[i], this.counts[i]);
        }
    }
    
    public final void forEachPipeline(Consumer<RenderPipeline> consumer)
    {
        final int size = this.size;
        for(int i = 0; i < size; i++)
            consumer.accept(this.pipelines[i]);
    }
    
    public final RenderPipeline getPipeline(int index)
    {
        return this.pipelines[index];
    }
    
    public final int getCount(int index)
    {
        return this.counts[index];
    }
}
