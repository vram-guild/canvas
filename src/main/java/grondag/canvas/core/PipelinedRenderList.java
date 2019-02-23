package grondag.canvas.core;

import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.chunk.ChunkRenderer;

public class PipelinedRenderList extends AbstractPipelinedRenderList
{
    public PipelinedRenderList()
    {
        super();
    }

    @Override
    public final void addChunkRenderer(ChunkRenderer renderChunkIn, BlockRenderLayer layer)
    {
        super.addChunkRenderer(renderChunkIn, layer);
    }

    @Override
    public final void renderChunkLayer(BlockRenderLayer layer)
    {
        super.renderChunkLayer(layer);
    }
}
