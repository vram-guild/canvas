package grondag.canvas.buffering;

import java.util.Arrays;
import java.util.function.Consumer;

import grondag.canvas.core.ConditionalPipeline;

/**
 * Tracks number of vertices, pipeline and sequence thereof within a buffer.
 */
public class BufferPackingList {
    private int[] starts = new int[16];
    private int[] counts = new int[16];
    private ConditionalPipeline[] pipelines = new ConditionalPipeline[16];
    
    private int size = 0;
    private int totalBytes = 0;
    
    BufferPackingList() {
        
    }
    
    public void clear() {
        this.size = 0;
        this.totalBytes = 0;
        //PERF: remove
        Arrays.fill(starts, 0);
        Arrays.fill(counts, 0);
        Arrays.fill(pipelines, null);
    }

    public int size() {
        return this.size;
    }

    /**
     * For performance testing.
     */
    public int quadCount() {
        if (this.size == 0)
            return 0;

        int quads = 0;

        for (int i = 0; i < this.size; i++) {
            quads += this.counts[i] / 4;
        }
        return quads;
    }

    public int totalBytes() {
        return this.totalBytes;
    }

    public void addPacking(ConditionalPipeline conditionalPipeline, int startVertex, int vertexCount) {
        if (size == this.pipelines.length) {
            final int cCopy[] = new int[size * 2];
            System.arraycopy(this.counts, 0, cCopy, 0, size);
            this.counts = cCopy;

            final int sCopy[] = new int[size * 2];
            System.arraycopy(this.starts, 0, sCopy, 0, size);
            this.starts = sCopy;
            
            final ConditionalPipeline pCopy[] = new ConditionalPipeline[size * 2];
            System.arraycopy(this.pipelines, 0, pCopy, 0, size);
            this.pipelines = pCopy;
        }
        this.pipelines[size] = conditionalPipeline;
        this.starts[size] = startVertex;
        this.counts[size] = vertexCount;
        this.totalBytes += conditionalPipeline.pipeline.piplineVertexFormat().vertexStrideBytes * vertexCount;
        this.size++;
    }

    public final void forEach(BufferPacker consumer) {
        final int size = this.size;
        for (int i = 0; i < size; i++) {
            consumer.accept(this.pipelines[i], this.starts[i], this.counts[i]);
        }
    }

    public final void forEachPipeline(Consumer<ConditionalPipeline> consumer) {
        final int size = this.size;
        for (int i = 0; i < size; i++)
            consumer.accept(this.pipelines[i]);
    }

    public final ConditionalPipeline getPipeline(int index) {
        return this.pipelines[index];
    }

    public final int getCount(int index) {
        return this.counts[index];
    }
    
    public final int getStart(int index) {
        return this.starts[index];
    }
}
