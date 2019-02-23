package grondag.canvas.hooks;

import grondag.canvas.Canvas;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class PipelineHooks
{
    // these have to be somewhere other than the static initialize for Direction/mixins thereof
    public static final Direction[] HORIZONTAL_FACES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    public static final Direction[] VERTICAL_FACES = {Direction.UP, Direction.DOWN};
    
//    private static boolean didWarnUnhandledFluid = false;
//    
//    private static AtomicInteger fluidModelCount = new AtomicInteger();
//    private static LongAccumulator fluidModelNanos = new LongAccumulator((l, r) -> l + r, 0);
    
    /**
     * Performance counting version of {@link #renderFluid(BlockFluidRenderer, IBlockAccess, IBlockState, BlockPos, BufferBuilder)}
     */
//    public static boolean renderFluidDebug(BlockFluidRenderer fluidRenderer, IBlockAccess blockAccess, IBlockState blockStateIn, BlockPos blockPosIn, BufferBuilder bufferBuilderIn)
//    {
//        final long start = System.nanoTime();
//        final boolean result = renderFluid(fluidRenderer, blockAccess, blockStateIn, blockPosIn, bufferBuilderIn);
//        fluidModelNanos.accumulate(System.nanoTime() - start);
//        if(fluidModelCount.incrementAndGet() == 50000)
//        {
//            // could misalign one or two samples but close enough
//            long total = fluidModelNanos.getThenReset();
//            fluidModelCount.set(0);
//            
//            Canvas.INSTANCE.getLog().info(String.format("Average ns per fluid model rebuild (Acuity %s) = %f", 
//                    Canvas.isModEnabled() ? "Enabled" : "Disabled",
//                    total / 50000.0));
//        }
//        return result;
//    }
//    
//    /**
//     * Handles vanilla special-case rendering for lava and water.
//     * Forge fluids should come as block models instead.
//     */
//    public static boolean renderFluid(BlockFluidRenderer fluidRenderer, IBlockAccess blockAccess, IBlockState blockStateIn, BlockPos blockPosIn, BufferBuilder bufferBuilderIn)
//    {
//        if(Canvas.isModEnabled())
//        {
//            RenderPipeline target;
//            if(blockStateIn.getMaterial() == Material.LAVA)
//            {
//                target = PipelineManager.INSTANCE.getLavaPipeline();
//            }
//            else
//            {
//                if(!didWarnUnhandledFluid && blockStateIn.getMaterial() != Material.WATER)
//                {
//                    Canvas.INSTANCE.getLog().warn(I18n.translateToLocal("misc.warn_unknown_fluid_render"));
//                    didWarnUnhandledFluid = true;
//                }
//                target = PipelineManager.INSTANCE.getWaterPipeline();
//            }
//            final CompoundVertexLighter lighter = lighters.get();
//            lighter.prepare((CompoundBufferBuilder)bufferBuilderIn, MinecraftForgeClient.getRenderLayer(), blockAccess, blockStateIn, blockPosIn, false);
//            return fluidRenderer.renderFluid(blockAccess, blockStateIn, blockPosIn, fluidBuilders.get().prepare(target, lighter));
//        }
//        else
//            return fluidRenderer.renderFluid(blockAccess, blockStateIn, blockPosIn, bufferBuilderIn);
//    }

    public static boolean isFirstOrUV(int index, VertexFormatElement.Type usage)
    {
        // has to apply even when mod is disabled so that our formats can be instantiated
        return index == 0 || usage == VertexFormatElement.Type.UV || usage == VertexFormatElement.Type.PADDING;
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
}
