package grondag.canvas.hooks;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import grondag.canvas.Canvas;
import grondag.canvas.Configurator;
import grondag.canvas.core.PipelineManager;
import grondag.canvas.core.RenderPipeline;
import grondag.canvas.buffering.DrawableChunk.Solid;
import grondag.canvas.buffering.DrawableChunk.Translucent;
import grondag.canvas.core.CompoundBufferBuilder;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.BlockLayeredBufferBuilder;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class PipelineHooks
{
    // these have to be somewhere other than the static initialize for Direction/mixins thereof
    public static final Direction[] HORIZONTAL_FACES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    public static final Direction[] VERTICAL_FACES = {Direction.UP, Direction.DOWN};
    
    private static boolean didWarnUnhandledFluid = false;
    
    /**
     * When mod is enabled, cutout layers are packed into solid layer, but the
     * chunk render dispatcher doesn't know this and sets flags in the compiled chunk
     * as if the cutout buffers were populated.  We use this hook to correct that
     * so that uploader and rendering work in subsequent operations.<p>
     * 
     * Called from the rebuildChunk method in ChunkRenderer, via a redirect on the call to
     * {@link CompiledChunk#setVisibility(net.minecraft.client.renderer.chunk.SetVisibility)}
     * which is reliably called after the chunks are built in render chunk.<p>
     */
    public static void mergeRenderLayers(CompiledChunk compiledChunk)
    {
        if(Canvas.isModEnabled())
        {
            mergeLayerFlags(compiledChunk.layersStarted);
            mergeLayerFlags(compiledChunk.layersUsed);
        }
    }
    
    private static void mergeLayerFlags(boolean[] layerFlags)
    {
        layerFlags[0]  = layerFlags[0] || layerFlags[1] || layerFlags[2];
        layerFlags[1] = false;
        layerFlags[2] = false;
    }
    
    private static AtomicInteger fluidModelCount = new AtomicInteger();
    private static LongAccumulator fluidModelNanos = new LongAccumulator((l, r) -> l + r, 0);
    
    /**
     * Performance counting version of {@link #renderFluid(BlockFluidRenderer, IBlockAccess, IBlockState, BlockPos, BufferBuilder)}
     */
    public static boolean renderFluidDebug(BlockFluidRenderer fluidRenderer, IBlockAccess blockAccess, IBlockState blockStateIn, BlockPos blockPosIn, BufferBuilder bufferBuilderIn)
    {
        final long start = System.nanoTime();
        final boolean result = renderFluid(fluidRenderer, blockAccess, blockStateIn, blockPosIn, bufferBuilderIn);
        fluidModelNanos.accumulate(System.nanoTime() - start);
        if(fluidModelCount.incrementAndGet() == 50000)
        {
            // could misalign one or two samples but close enough
            long total = fluidModelNanos.getThenReset();
            fluidModelCount.set(0);
            
            Canvas.INSTANCE.getLog().info(String.format("Average ns per fluid model rebuild (Acuity %s) = %f", 
                    Canvas.isModEnabled() ? "Enabled" : "Disabled",
                    total / 50000.0));
        }
        return result;
    }
    
    /**
     * Handles vanilla special-case rendering for lava and water.
     * Forge fluids should come as block models instead.
     */
    public static boolean renderFluid(BlockFluidRenderer fluidRenderer, IBlockAccess blockAccess, IBlockState blockStateIn, BlockPos blockPosIn, BufferBuilder bufferBuilderIn)
    {
        if(Canvas.isModEnabled())
        {
            RenderPipeline target;
            if(blockStateIn.getMaterial() == Material.LAVA)
            {
                target = PipelineManager.INSTANCE.getLavaPipeline();
            }
            else
            {
                if(!didWarnUnhandledFluid && blockStateIn.getMaterial() != Material.WATER)
                {
                    Canvas.INSTANCE.getLog().warn(I18n.translateToLocal("misc.warn_unknown_fluid_render"));
                    didWarnUnhandledFluid = true;
                }
                target = PipelineManager.INSTANCE.getWaterPipeline();
            }
            final CompoundVertexLighter lighter = lighters.get();
            lighter.prepare((CompoundBufferBuilder)bufferBuilderIn, MinecraftForgeClient.getRenderLayer(), blockAccess, blockStateIn, blockPosIn, false);
            return fluidRenderer.renderFluid(blockAccess, blockStateIn, blockPosIn, fluidBuilders.get().prepare(target, lighter));
        }
        else
            return fluidRenderer.renderFluid(blockAccess, blockStateIn, blockPosIn, bufferBuilderIn);
    }

    public static boolean isFirstOrUV(int index, VertexFormatElement.Type usage)
    {
        // has to apply even when mod is disabled so that our formats can be instantiated
        return index == 0 || usage == VertexFormatElement.Type.UV || usage == VertexFormatElement.Type.PADDING;
    }

    public static boolean useVbo()
    {
        return Canvas.isModEnabled() || (OpenGlHelper.vboSupported && Minecraft.getMinecraft().gameSettings.useVbo);
    }

    /**
     * When Acuity is enabled the per-chunk matrix is never used, so is wasteful to update when frustum moves.
     * Matters more when lots of block updates or other high-throughput because adds to contention.
     */
    public static void renderChunkInitModelViewMatrix(ChunkRenderer renderChunk)
    {
        if(Canvas.isModEnabled())
        {
            // this is called right after setting chunk position because it was moved in the frustum
            // let buffers in the chunk know they are no longer valid and can be released.
            ((IChunkRenderer)renderChunk).releaseDrawables();
        }
        else
            renderChunk.initModelviewMatrix();
    }

    public static boolean shouldUploadLayer(CompiledChunk compiledchunk, BlockRenderLayer blockrenderlayer)
    {
        return Canvas.isModEnabled()
            ? compiledchunk.isLayerStarted(blockrenderlayer) && !compiledchunk.isLayerEmpty(blockrenderlayer)
            : compiledchunk.isLayerStarted(blockrenderlayer);
    }
    
    public static int renderBlockLayer(WorldRenderer worldRenderer, BlockRenderLayer blockLayerIn, double partialTicks, int pass, Entity entityIn)
    {
        if(Canvas.isModEnabled())
        {
            switch(blockLayerIn)
            {
                case SOLID:
                case TRANSLUCENT:
                    return worldRenderer.renderLayer(blockLayerIn, partialTicks, entityIn);
                    
                case CUTOUT:
                case MIPPED_CUTOUT:
                default: 
                    return 0;
            }
        }
        else
            return worldRenderer.renderLayer(blockLayerIn, partialTicks, entityIn);
    }

    public static ListenableFuture<Object> uploadChunk(ChunkRenderDispatcher chunkRenderDispatcher, BlockRenderLayer blockRenderLayer,
            BufferBuilder bufferBuilder, ChunkRenderer renderChunk, CompiledChunk compiledChunk, double distanceSq)
    {
        assert blockRenderLayer == BlockRenderLayer.SOLID || blockRenderLayer == BlockRenderLayer.TRANSLUCENT;
        
        if (MinecraftClient.getInstance().isMainThread())
        {
            if(blockRenderLayer == BlockRenderLayer.SOLID)
                ((IRenderChunk)renderChunk).setSolidDrawable((Solid) ((CompoundBufferBuilder)bufferBuilder).produceDrawable());
            else
                ((IRenderChunk)renderChunk).setTranslucentDrawable((Translucent) ((CompoundBufferBuilder)bufferBuilder).produceDrawable());
            
            bufferBuilder.setOffset(0.0D, 0.0D, 0.0D);
            return Futures.<Object>immediateFuture((Object)null);
        }
        else
        {
            ListenableFutureTask<Object> listenablefuturetask = ListenableFutureTask.<Object>create(new Runnable()
            {
                @Override
                public void run()
                {
                    uploadChunk(chunkRenderDispatcher, blockRenderLayer, bufferBuilder, renderChunk,
                            compiledChunk, distanceSq);
                }
            }, (Object)null);

            synchronized (chunkRenderDispatcher.queueChunkUploads)
            {
                chunkRenderDispatcher.queueChunkUploads.add(chunkRenderDispatcher.new PendingUpload(listenablefuturetask, distanceSq));
                return listenablefuturetask;
            }
        }
    }
}
