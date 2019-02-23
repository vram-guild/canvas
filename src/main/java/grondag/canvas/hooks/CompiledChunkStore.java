package grondag.acuity.hooks;

import java.util.concurrent.ArrayBlockingQueue;

import net.minecraft.client.renderer.chunk.CompiledChunk;

public class CompiledChunkStore
{
    private static final ArrayBlockingQueue<CompiledChunk> chunks = new ArrayBlockingQueue<>(4096);
 
    public static CompiledChunk claim()
    {
        CompiledChunk result = chunks.poll();
        if(result == null)
            result = new CompiledChunk();
        
        return result;
    }
    
    @SuppressWarnings("null")
    public static void release(CompiledChunk chunk)
    {
        chunk.empty = true;
        System.arraycopy(ChunkRebuildHelper.EMPTY_RENDER_LAYER_FLAGS, 0, chunk.layersStarted, 0, ChunkRebuildHelper.BLOCK_RENDER_LAYER_COUNT);
        System.arraycopy(ChunkRebuildHelper.EMPTY_RENDER_LAYER_FLAGS, 0, chunk.layersUsed, 0, ChunkRebuildHelper.BLOCK_RENDER_LAYER_COUNT);
        chunk.setVisibility.bitSet.clear();
        chunk.state = null;
        chunk.tileEntities.clear();
        ((ISetVisibility)chunk.setVisibility).setVisibilityData(null);
        chunks.offer(chunk);
    }
}
