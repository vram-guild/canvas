package grondag.acuity.core;

import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PipelinedRenderList extends AbstractPipelinedRenderList
{
    public PipelinedRenderList()
    {
        super();
    }

    @Override
    public final void addRenderChunk(RenderChunk renderChunkIn, BlockRenderLayer layer)
    {
        super.addRenderChunk(renderChunkIn, layer);
    }

    @Override
    public final void renderChunkLayer(BlockRenderLayer layer)
    {
        super.renderChunkLayer(layer);
    }
}
