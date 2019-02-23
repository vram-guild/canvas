package grondag.canvas.core;

import java.util.function.Consumer;

import grondag.canvas.buffering.DrawableChunkDelegate;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SolidRenderCube implements Consumer<DrawableChunkDelegate>
{
    public final ObjectArrayList<DrawableChunkDelegate>[] pipelineLists;
    
    public SolidRenderCube()
    {
        final int size = PipelineManager.INSTANCE.pipelineCount();
        @SuppressWarnings("unchecked")
        ObjectArrayList<DrawableChunkDelegate>[] buffers = new ObjectArrayList[size];
        for(int i = 0; i < size; i++)
        {
            buffers[i] = new ObjectArrayList<DrawableChunkDelegate>();
        }
        this.pipelineLists = buffers;
    }

    @Override
    public void accept(DrawableChunkDelegate d)
    {
        pipelineLists[d.getPipeline().getIndex()].add(d);   
    }
}
