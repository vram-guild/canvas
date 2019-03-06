package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.canvas.LoadingConfig;
import grondag.canvas.core.AbstractPipelinedRenderList;
import grondag.canvas.core.PipelinedRenderList;
import grondag.canvas.core.PipelinedRenderListDebug;
import grondag.canvas.mixinext.ChunkRendererListExt;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.render.chunk.ChunkRendererList;
import net.minecraft.client.render.chunk.VboChunkRendererList;

@Mixin(VboChunkRendererList.class)
public abstract class MixinVboChunkRendererList extends ChunkRendererList implements ChunkRendererListExt {
    private AbstractPipelinedRenderList ext;
    
    @Inject(method = "<init>*", at = @At("RETURN"), require = 1)
    private void onConstructed(CallbackInfo ci)
    {
        ext = LoadingConfig.INSTANCE.enableRenderStats 
                ? new PipelinedRenderListDebug()
                : new PipelinedRenderList();
    }
    
    @Override
    public void add(ChunkRenderer renderChunkIn, BlockRenderLayer layer) {
        ext.addChunkRenderer(renderChunkIn, layer);
    }
    
    @Override
    public void setCameraPosition(double viewEntityXIn, double viewEntityYIn, double viewEntityZIn) {
        ext.initialize(viewEntityXIn, viewEntityYIn, viewEntityZIn);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 1)
    private void onRender(BlockRenderLayer layer, CallbackInfo ci) {
            ext.renderChunkLayer(layer);
            ci.cancel();
    }
    
    @Override
    public void canvas_prepareForFrame() {
        ext.downloadModelViewMatrix();
    }
}
