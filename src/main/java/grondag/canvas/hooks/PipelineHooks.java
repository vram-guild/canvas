package grondag.canvas.hooks;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import grondag.acuity.Acuity;
import grondag.acuity.Configurator;
import grondag.acuity.api.IPipelinedBakedModel;
import grondag.acuity.api.PipelineManager;
import grondag.acuity.api.RenderPipeline;
import grondag.acuity.buffering.DrawableChunk.Solid;
import grondag.acuity.buffering.DrawableChunk.Translucent;
import grondag.acuity.core.CompoundBufferBuilder;
import grondag.acuity.core.CompoundVertexLighter;
import grondag.acuity.core.FluidBuilder;
import grondag.acuity.core.VanillaQuadWrapper;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PipelineHooks
{
    // these have to be somewhere other than the static initialize for EnumFacing/mixins thereof
    public static final EnumFacing[] HORIZONTAL_FACES = {EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};
    public static final EnumFacing[] VERTICAL_FACES = {EnumFacing.UP, EnumFacing.DOWN};
    
    private static ThreadLocal<CompoundVertexLighter> lighters;
    private static ThreadLocal<FluidBuilder> fluidBuilders;
    
    static
    {
        createLighters();
    }
    
    private static void createLighters()
    {
        lighters = new ThreadLocal<CompoundVertexLighter>()
        {
            @Override
            protected CompoundVertexLighter initialValue()
            {
                return Configurator.lightingModel.createLighter();
            }
        };
        
        fluidBuilders = new ThreadLocal<FluidBuilder>()
        {
            @Override
            protected FluidBuilder initialValue()
            {
                return new FluidBuilder();
            }
        };
    }
    
    private static final ThreadLocal<VanillaQuadWrapper> quadWrappers = new ThreadLocal<VanillaQuadWrapper>()
    {
        @Override
        protected VanillaQuadWrapper initialValue()
        {
            return new VanillaQuadWrapper();
        }
    };
    
    public static void forceReload()
    {
        createLighters(); 
    }
    
    private static boolean didWarnUnhandledFluid = false;
    
    public static void linkBuilders(RegionRenderCacheBuilder cache)
    {
        linkBuildersInner(cache, BlockRenderLayer.SOLID);
        linkBuildersInner(cache, BlockRenderLayer.CUTOUT);
        linkBuildersInner(cache, BlockRenderLayer.CUTOUT_MIPPED);
        linkBuildersInner(cache, BlockRenderLayer.TRANSLUCENT);
    }
    
    private static void linkBuildersInner(RegionRenderCacheBuilder cache, BlockRenderLayer layer)
    {
        CompoundBufferBuilder builder = (CompoundBufferBuilder) cache.getWorldRendererByLayer(layer);
        builder.setupLinks(cache, layer);
    }
    
    /**
     * When mod is enabled, cutout layers are packed into solid layer, but the
     * chunk render dispatcher doesn't know this and sets flags in the compiled chunk
     * as if the cutout buffers were populated.  We use this hook to correct that
     * so that uploader and rendering work in subsequent operations.<p>
     * 
     * Called from the rebuildChunk method in RenderChunk, via a redirect on the call to
     * {@link CompiledChunk#setVisibility(net.minecraft.client.renderer.chunk.SetVisibility)}
     * which is reliably called after the chunks are built in render chunk.<p>
     */
    public static void mergeRenderLayers(CompiledChunk compiledChunk)
    {
        if(Acuity.isModEnabled())
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
            
            Acuity.INSTANCE.getLog().info(String.format("Average ns per fluid model rebuild (Acuity %s) = %f", 
                    Acuity.isModEnabled() ? "Enabled" : "Disabled",
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
        if(Acuity.isModEnabled())
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
                    Acuity.INSTANCE.getLog().warn(I18n.translateToLocal("misc.warn_unknown_fluid_render"));
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

    private static AtomicInteger blockModelCount = new AtomicInteger();
    private static LongAccumulator blockModelNanos = new LongAccumulator((l, r) -> l + r, 0);
    
    /**
     * Performance counting version of #
     */
    public static boolean renderModelDebug(BlockModelRenderer blockModelRenderer, IBlockAccess blockAccess, IBakedModel model, 
            IBlockState state, BlockPos pos, BufferBuilder bufferBuilderIn, boolean checkSides)
    {
        final long start = System.nanoTime();
        final boolean result = renderModel(blockModelRenderer, blockAccess, model, state, pos, bufferBuilderIn, checkSides);
        blockModelNanos.accumulate(System.nanoTime() - start);
        if(blockModelCount.incrementAndGet() == 500000)
        {
            // could misalign one or two samples but close enough
            long total = blockModelNanos.getThenReset();
            blockModelCount.set(0);
            
            Acuity.INSTANCE.getLog().info(String.format("Average ns per block model rebuild (Acuity %s) = %f", 
                    Acuity.isModEnabled() ? "Enabled" : "Disabled",
                    total / 500000.0));
        }
        return result;
    }
    
    public static boolean renderModel(BlockModelRenderer blockModelRenderer, IBlockAccess blockAccess, IBakedModel model, IBlockState state, BlockPos pos,
            BufferBuilder bufferBuilderIn, boolean checkSides)
    {
        if(Acuity.isModEnabled())
        {
            if(model instanceof IPipelinedBakedModel)
                return renderModel(blockAccess, model, state, pos, bufferBuilderIn, checkSides);
            else
                return renderVanillaModel(blockAccess, model, state, pos, bufferBuilderIn, checkSides);
        }
        else
            return blockModelRenderer.renderModel(blockAccess, model, state, pos, bufferBuilderIn, checkSides);
    }
    
    private static boolean renderModel(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn, BlockPos posIn, BufferBuilder bufferIn, boolean checkSides)
    {
        try
        {
            final IPipelinedBakedModel model = (IPipelinedBakedModel)modelIn;
            final BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
            if(!model.mightRenderInLayer(layer)) 
                return false;

            final CompoundVertexLighter lighter = lighters.get();
            lighter.prepare((CompoundBufferBuilder)bufferIn, layer, worldIn, (IExtendedBlockState) stateIn, posIn, checkSides);
            model.produceQuads(lighter);
            return lighter.didOutput();
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Tesselating block model (Acuity pipelined render)");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Block model being tesselated");
            CrashReportCategory.addBlockInfo(crashreportcategory, posIn, stateIn);
            throw new ReportedException(crashreport);
        }
    }

    private static boolean renderVanillaModel(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn, BlockPos posIn, BufferBuilder bufferIn, boolean checkSides)
    {
        try
        {
            final CompoundVertexLighter lighter = lighters.get();
            final VanillaQuadWrapper wrapper = quadWrappers.get();
            final BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
            
            lighter.prepare((CompoundBufferBuilder)bufferIn, layer, worldIn, stateIn, posIn, checkSides);
            wrapper.prepare(layer, modelIn.isAmbientOcclusion() && stateIn.getLightValue(worldIn, posIn) == 0);
            
            renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, null);
            
            if(checkSides)
            {
                if(stateIn.shouldSideBeRendered(worldIn, posIn, EnumFacing.DOWN))
                        renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, EnumFacing.DOWN);
                if(stateIn.shouldSideBeRendered(worldIn, posIn, EnumFacing.UP))
                    renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, EnumFacing.UP);
                if(stateIn.shouldSideBeRendered(worldIn, posIn, EnumFacing.EAST))
                    renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, EnumFacing.EAST);
                if(stateIn.shouldSideBeRendered(worldIn, posIn, EnumFacing.WEST))
                    renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, EnumFacing.WEST);
                if(stateIn.shouldSideBeRendered(worldIn, posIn, EnumFacing.NORTH))
                    renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, EnumFacing.NORTH);
                if(stateIn.shouldSideBeRendered(worldIn, posIn, EnumFacing.SOUTH))
                    renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, EnumFacing.SOUTH);
            }
            else
            {
                renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, EnumFacing.DOWN);
                renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, EnumFacing.UP);
                renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, EnumFacing.EAST);
                renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, EnumFacing.WEST);
                renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, EnumFacing.NORTH);
                renderVanillaModelInner(modelIn, stateIn, lighter, wrapper, EnumFacing.SOUTH);
            }
            
            return lighter.didOutput();
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Tesselating block model (Acuity pipelined vanilla render)");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Block model being tesselated");
            CrashReportCategory.addBlockInfo(crashreportcategory, posIn, stateIn);
            throw new ReportedException(crashreport);
        }
    }
    
    private static void renderVanillaModelInner(IBakedModel modelIn, IBlockState stateIn, CompoundVertexLighter lighter, VanillaQuadWrapper wrapper, @Nullable EnumFacing face)
    {
        List<BakedQuad> quads = modelIn.getQuads(stateIn, face, lighter.positionRandom());
        final int limit = quads.size();
        
        if(limit == 0)
            return;
        
        for(int i = 0; i < limit; i ++)
            wrapper.wrapAndLight(lighter, quads.get(i));
    }
    
    public static boolean isFirstOrUV(int index, VertexFormatElement.EnumUsage usage)
    {
        // has to apply even when mod is disabled so that our formats can be instantiated
        return index == 0 || usage == VertexFormatElement.EnumUsage.UV || usage == VertexFormatElement.EnumUsage.GENERIC;
    }

    public static boolean useVbo()
    {
        return Acuity.isModEnabled() || (OpenGlHelper.vboSupported && Minecraft.getMinecraft().gameSettings.useVbo);
    }

    /**
     * When Acuity is enabled the per-chunk matrix is never used, so is wasteful to update when frustum moves.
     * Matters more when lots of block updates or other high-throughput because adds to contention.
     */
    public static void renderChunkInitModelViewMatrix(RenderChunk renderChunk)
    {
        if(Acuity.isModEnabled())
        {
            // this is called right after setting chunk position because it was moved in the frustum
            // let buffers in the chunk know they are no longer valid and can be released.
            ((IRenderChunk)renderChunk).releaseDrawables();
        }
        else
            renderChunk.initModelviewMatrix();
    }

    public static boolean shouldUploadLayer(CompiledChunk compiledchunk, BlockRenderLayer blockrenderlayer)
    {
        return Acuity.isModEnabled()
            ? compiledchunk.isLayerStarted(blockrenderlayer) && !compiledchunk.isLayerEmpty(blockrenderlayer)
            : compiledchunk.isLayerStarted(blockrenderlayer);
    }
    
    public static int renderBlockLayer(RenderGlobal renderGlobal, BlockRenderLayer blockLayerIn, double partialTicks, int pass, Entity entityIn)
    {
        if(Acuity.isModEnabled())
        {
            switch(blockLayerIn)
            {
                case SOLID:
                case TRANSLUCENT:
                    return renderGlobal.renderBlockLayer(blockLayerIn, partialTicks, pass, entityIn);
                    
                case CUTOUT:
                case CUTOUT_MIPPED:
                default: 
                    return 0;
            }
        }
        else
            return renderGlobal.renderBlockLayer(blockLayerIn, partialTicks, pass, entityIn);
    }

    @SuppressWarnings("null")
    public static ListenableFuture<Object> uploadChunk(ChunkRenderDispatcher chunkRenderDispatcher, BlockRenderLayer blockRenderLayer,
            BufferBuilder bufferBuilder, RenderChunk renderChunk, CompiledChunk compiledChunk, double distanceSq)
    {
        assert blockRenderLayer == BlockRenderLayer.SOLID || blockRenderLayer == BlockRenderLayer.TRANSLUCENT;
        
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread())
        {
            if(blockRenderLayer == BlockRenderLayer.SOLID)
                ((IRenderChunk)renderChunk).setSolidDrawable((Solid) ((CompoundBufferBuilder)bufferBuilder).produceDrawable());
            else
                ((IRenderChunk)renderChunk).setTranslucentDrawable((Translucent) ((CompoundBufferBuilder)bufferBuilder).produceDrawable());
            
            bufferBuilder.setTranslation(0.0D, 0.0D, 0.0D);
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
