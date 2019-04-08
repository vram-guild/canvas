package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.canvas.mixinext.ChunkRendererDispatcherExt;
import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.util.math.BlockPos;

@Mixin(ChunkRenderDispatcher.class)
public abstract class MixinChunkRendererDispatcher implements ChunkRendererDispatcherExt {
    @Shadow protected abstract ChunkRenderer getChunk(BlockPos blockPos);
    
    @Override
    public ChunkRenderer canvas_chunkRenderer(BlockPos blockPos) {
        return getChunk(blockPos);
    }
}
