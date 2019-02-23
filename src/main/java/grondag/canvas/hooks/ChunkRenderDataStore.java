package grondag.canvas.hooks;

import java.util.concurrent.ArrayBlockingQueue;

import grondag.canvas.mixin.extension.ChunkRenderDataExt;
import net.minecraft.client.render.chunk.ChunkRenderData;

public class ChunkRenderDataStore
{
    private static final ArrayBlockingQueue<ChunkRenderData> chunks = new ArrayBlockingQueue<>(4096);
 
    public static ChunkRenderData claim()
    {
        ChunkRenderData result = chunks.poll();
        if(result == null)
            result = new ChunkRenderData();
        
        return result;
    }
    
    public static void release(ChunkRenderData chunk)
    {
        ((ChunkRenderDataExt)chunk).clear();
        chunks.offer(chunk);
    }
}
