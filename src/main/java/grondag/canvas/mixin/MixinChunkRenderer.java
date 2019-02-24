/*
 * Copyright (c) 2016, 2017, 2018 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grondag.canvas.mixin;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Sets;

import grondag.canvas.Canvas;
import grondag.canvas.accessor.AccessChunkRenderer;
import grondag.canvas.accessor.AccessSafeWorldView;
import grondag.canvas.buffering.DrawableChunk.Solid;
import grondag.canvas.buffering.DrawableChunk.Translucent;
import grondag.canvas.hooks.ChunkRebuildHelper;
import grondag.canvas.hooks.ChunkRenderDataStore;
import grondag.canvas.mixinext.ChunkRendererExt;
import grondag.canvas.mixinext.ChunkRenderDataExt;
import grondag.canvas.render.TerrainRenderContext;
import net.minecraft.class_852;
import net.minecraft.class_854;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.ChunkRenderTask;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.world.SafeWorldView;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Implements the main hooks for terrain rendering. Attempts to tread lightly.
 * This means we are deliberately stepping over some minor optimization
 * opportunities.
 * <p>
 * 
 * Non-Fabric renderer implementations that are looking to maximize performance
 * will likely take a much more aggressive approach. For that reason, mod
 * authors who want compatibility with advanced renderers will do well to steer
 * clear of chunk rebuild hooks unless they are creating a renderer.
 * <p>
 * 
 * These hooks are intended only for the Fabric default renderer and aren't
 * expected to be present when a different renderer is being used. Renderer
 * authors are responsible for creating the hooks they need. (Though they can
 * use these as a example if they wish.)
 */
@Mixin(ChunkRenderer.class)
public abstract class MixinChunkRenderer implements AccessChunkRenderer, ChunkRendererExt {
    @Shadow
    private volatile World world;
    @Shadow
    private WorldRenderer renderer;
    @Shadow
    public static int chunkUpdateCount;
    @Shadow
    public ChunkRenderData chunkRenderData;
    @Shadow
    private ReentrantLock chunkRenderLock;
    @Shadow
    private BlockPos.Mutable origin;
    @Shadow
    private Set<BlockEntity> blockEntities;

    @Shadow
    abstract void beginBufferBuilding(BufferBuilder bufferBuilder_1, BlockPos blockPos_1);

    @Shadow
    abstract void endBufferBuilding(BlockRenderLayer blockRenderLayer_1, float float_1, float float_2, float float_3,
            BufferBuilder bufferBuilder_1, ChunkRenderData chunkRenderData_1);

    @Shadow
    abstract void updateTransformationMatrix();

    Solid solidDrawable;
    Translucent translucentDrawable;

    // TODO: substitute our visibility graph

    @Override
    public Solid getSolidDrawable() {
        return solidDrawable;
    }

    @Override
    public Translucent getTranslucentDrawable() {
        return translucentDrawable;
    }
    
    /**
     * When Canvas is enabled the per-chunk matrix is never used, so is wasteful to
     * update when frustum moves. Matters more when lots of block updates or other
     * high-throughput because adds to contention.
     */
    @Inject(at = @At("HEAD"), method = "updateTransformationMatrix", cancellable = true)
    private void hookUpdateTransformationMatrix(CallbackInfo ci) {
        if (Canvas.isModEnabled()) {
            // this is called right after setting chunk position because it was moved in the
            // frustum
            // let buffers in the chunk know they are no longer valid and can be released.
            ((ChunkRendererExt) this).releaseDrawables();
            ci.cancel();
        }
    }
    
    @Inject(method = "delete", at = @At("RETURN"), require = 1)
    private void onDeleteGlResources(CallbackInfo ci) {
        releaseDrawables();
    }

    @Inject(method = "method_3665", require = 1, 
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/client/render/chunk/RenderChunk;chunkRenderData:Lnet/minecraft/client/render/chunk/ChunkRenderData;"))
    private void onSetChunkData(ChunkRenderData chunkDataIn, CallbackInfo ci) {
        if(chunkRenderData == null || chunkRenderData == ChunkRenderData.EMPTY || chunkDataIn == chunkRenderData)
            return;

        ((ChunkRenderDataExt)chunkRenderData).getVisibilityData().releaseVisibilityData();
        ChunkRenderDataStore.release(chunkRenderData);
    }
    
    // shouldn't be necessary if rebuild chunk hook works, but insurance if not
    @Redirect(method = "rebuildChunk", require = 1,       
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/chunk/ChunkRenderData;method_3640(Lnet/minecraft/class_854;)V"))       
    private void onSetVisibility(ChunkRenderData compiledChunk, class_854 chunkVisibility) {        
        compiledChunk.method_3640(chunkVisibility);      
        ((ChunkRenderDataExt)compiledChunk).mergeRenderLayers();
    }
    
    @Inject(method = "clear", require = 1, 
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/client/render/chunk/ChunkRenderer;chunkRenderData:Lnet/minecraft/client/render/chunk/ChunkRenderData;"))
    private void onClear(CallbackInfo ci)
    {
        if(chunkRenderData == null || chunkRenderData == ChunkRenderData.EMPTY)
            return;

        ((ChunkRenderDataExt)chunkRenderData).getVisibilityData().releaseVisibilityData();
        ChunkRenderDataStore.release(chunkRenderData);
    }
    
    @Override
    public void setSolidDrawable(Solid drawable) {
        solidDrawable = drawable;
    }

    @Override
    public void setTranslucentDrawable(Translucent drawable) {
        translucentDrawable = drawable;
    }

    @Override
    public void releaseDrawables() {
        if (solidDrawable != null) {
            solidDrawable.clear();
            solidDrawable = null;
        }

        if (translucentDrawable != null) {
            translucentDrawable.clear();
            translucentDrawable = null;
        }
    }
    
    @Inject(method = "rebuildChunk", at = @At("HEAD"), cancellable = true, require = 1)
    private void onRebuildChunk(final float x, final float y, final float z, final ChunkRenderTask chunkRenderTask, final CallbackInfo ci) {
        if(!Canvas.isModEnabled())
            return;
        
        final ChunkRebuildHelper help = ChunkRebuildHelper.get();
        help.clear();
        
        ChunkRenderData chunkRenderData = ChunkRenderDataStore.claim();
        final BlockPos.Mutable origin = this.origin;
        
        BlockPos blockPos_1 = this.origin.toImmutable();
        BlockPos blockPos_2 = blockPos_1.add(15, 15, 15);
        World world_1 = this.world;
        
        if (world_1 != null) {
            
           chunkRenderTask.getLock().lock();

           try {
              if (chunkRenderTask.getStage() != ChunkRenderTask.Stage.COMPILING) {
                 return;
              }

              chunkRenderTask.setRenderData(chunkRenderData);
           } finally {
              chunkRenderTask.getLock().unlock();
           }

           class_852 class_852_1 = new class_852();
           HashSet set_1 = Sets.newHashSet();
           SafeWorldView safeWorldView_1 = chunkRenderTask.getAndInvalidateWorldView();
           if (safeWorldView_1 != null) {
              ++chunkUpdateCount;
              boolean[] booleans_1 = new boolean[BlockRenderLayer.values().length];
              BlockModelRenderer.enableBrightnessCache();
              Random random_1 = new Random();
              BlockRenderManager blockRenderManager_1 = MinecraftClient.getInstance().getBlockRenderManager();
              Iterator var16 = BlockPos.iterateBoxPositions(blockPos_1, blockPos_2).iterator();

              while(var16.hasNext()) {
                 BlockPos blockPos_3 = (BlockPos)var16.next();
                 BlockState blockState_1 = safeWorldView_1.getBlockState(blockPos_3);
                 Block block_1 = blockState_1.getBlock();
                 if (blockState_1.isFullOpaque(safeWorldView_1, blockPos_3)) {
                    class_852_1.method_3682(blockPos_3);
                 }

                 if (block_1.hasBlockEntity()) {
                    BlockEntity blockEntity_1 = safeWorldView_1.getBlockEntity(blockPos_3, WorldChunk.CreationType.CHECK);
                    if (blockEntity_1 != null) {
                       BlockEntityRenderer<BlockEntity> blockEntityRenderer_1 = BlockEntityRenderDispatcher.INSTANCE.get(blockEntity_1);
                       if (blockEntityRenderer_1 != null) {
                          chunkRenderData.addBlockEntity(blockEntity_1);
                          if (blockEntityRenderer_1.method_3563(blockEntity_1)) {
                             set_1.add(blockEntity_1);
                          }
                       }
                    }
                 }

                 FluidState fluidState_1 = safeWorldView_1.getFluidState(blockPos_3);
                 int int_3;
                 BufferBuilder bufferBuilder_2;
                 BlockRenderLayer blockRenderLayer_2;
                 if (!fluidState_1.isEmpty()) {
                    blockRenderLayer_2 = fluidState_1.getRenderLayer();
                    int_3 = blockRenderLayer_2.ordinal();
                    bufferBuilder_2 = chunkRenderTask.getBufferBuilders().get(int_3);
                    if (!chunkRenderData.isBufferInitialized(blockRenderLayer_2)) {
                       chunkRenderData.markBufferInitialized(blockRenderLayer_2);
                       this.beginBufferBuilding(bufferBuilder_2, blockPos_1);
                    }

                    booleans_1[int_3] |= blockRenderManager_1.tesselateFluid(blockPos_3, safeWorldView_1, bufferBuilder_2, fluidState_1);
                 }

                 if (blockState_1.getRenderType() != BlockRenderType.INVISIBLE) {
                    blockRenderLayer_2 = block_1.getRenderLayer();
                    int_3 = blockRenderLayer_2.ordinal();
                    bufferBuilder_2 = chunkRenderTask.getBufferBuilders().get(int_3);
                    if (!chunkRenderData.isBufferInitialized(blockRenderLayer_2)) {
                       chunkRenderData.markBufferInitialized(blockRenderLayer_2);
                       this.beginBufferBuilding(bufferBuilder_2, blockPos_1);
                    }

                    booleans_1[int_3] |= blockRenderManager_1.tesselateBlock(blockState_1, blockPos_3, safeWorldView_1, bufferBuilder_2, random_1);
                 }
              }

              BlockRenderLayer[] var33 = BlockRenderLayer.values();
              int var34 = var33.length;

              for(int var35 = 0; var35 < var34; ++var35) {
                 BlockRenderLayer blockRenderLayer_3 = var33[var35];
                 if (booleans_1[blockRenderLayer_3.ordinal()]) {
                    ((ChunkRenderDataExt)chunkRenderData).setNonEmpty(blockRenderLayer_3);
                 }

                 if (chunkRenderData.isBufferInitialized(blockRenderLayer_3)) {
                    this.endBufferBuilding(blockRenderLayer_3, x, y, z, chunkRenderTask.getBufferBuilders().get(blockRenderLayer_3), chunkRenderData);
                 }
              }

              BlockModelRenderer.disableBrightnessCache();
           }

           chunkRenderData.method_3640(class_852_1.method_3679());
           this.chunkRenderLock.lock();

           try {
              Set<BlockEntity> set_2 = Sets.newHashSet(set_1);
              Set<BlockEntity> set_3 = Sets.newHashSet(this.blockEntities);
              set_2.removeAll(this.blockEntities);
              set_3.removeAll(set_1);
              this.blockEntities.clear();
              this.blockEntities.addAll(set_1);
              this.renderer.method_3245(set_3, set_2);
           } finally {
              this.chunkRenderLock.unlock();
           }

        }
    }
    
    /////
    
    /**
     * Save task to renderer, this is the easiest place to capture it.
     */
    @Inject(at = @At("HEAD"), method = "rebuildChunk")
    private void hookRebuildChunkHead(float float_1, float float_2, float float_3, ChunkRenderTask chunkRenderTask_1,
            CallbackInfo info) {
        if (chunkRenderTask_1 != null) {
            TerrainRenderContext renderer = TerrainRenderContext.POOL.get();
            renderer.setChunkTask(chunkRenderTask_1);
        }
    }

    /**
     * Capture the block layer result flags when they are first created so our
     * renderer can update then when more than one layer is renderer for a single
     * model. This is also where we signal the renderer to prepare for a new chunk
     * using the data we've accumulated up to this point.
     */
    @ModifyVariable(method = "rebuildChunk", at = @At(value = "STORE", ordinal = 0), allow = 1, require = 1)
    private boolean[] hookResultFlagsAndPrepare(boolean[] flagsIn) {
        TerrainRenderContext.POOL.get().prepare((ChunkRenderer) (Object) this, origin, flagsIn);
        return flagsIn;
    }

    // TODO: remove at release
//    private static final AtomicLong nanos = new AtomicLong();
//    private static final AtomicInteger count = new AtomicInteger();

    /**
     * This is the hook that actually implements the rendering API for terrain
     * rendering.
     * <p>
     * 
     * It's unusual to have a @Redirect in a Fabric library, but in this case it is
     * our explicit intention that
     * {@link BlockRenderManager#tesselateBlock(BlockState, BlockPos, ExtendedBlockView, BufferBuilder, Random)}
     * does not execute for models that will be rendered by our renderer.
     * <p>
     * 
     * Any mod that wants to redirect this specific call is likely also a renderer,
     * in which case this renderer should not be present, or the mod should probably
     * instead be relying on the renderer API which was specifically created to
     * provide for enhanced terrain rendering.
     * <p>
     * 
     * Note also that
     * {@link BlockRenderManager#tesselateBlock(BlockState, BlockPos, ExtendedBlockView, BufferBuilder, Random)}
     * IS called if the block render type is something other than
     * {@link BlockRenderType#MODEL}. Normally this does nothing but will allow mods
     * to create rendering hooks that are driven off of render type. (Not
     * recommended or encouraged, but also not prevented.)
     */
    @Redirect(method = "rebuildChunk", require = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/BlockRenderManager;tesselateBlock(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/ExtendedBlockView;Lnet/minecraft/client/render/BufferBuilder;Ljava/util/Random;)Z"))
    private boolean hookChunkBuildTesselate(BlockRenderManager renderManager, BlockState blockState, BlockPos blockPos,
            ExtendedBlockView blockView, BufferBuilder bufferBuilder, Random random) {
        // TODO: remove at release
//        long start = System.nanoTime();
//        boolean result;
        if (blockState.getRenderType() == BlockRenderType.MODEL) {
            return ((AccessSafeWorldView) blockView).fabric_getRenderer().tesselateBlock(blockState, blockPos);
        } else {
            return renderManager.tesselateBlock(blockState, blockPos, blockView, bufferBuilder, random);
        }

        // TODO: remove at release
//        long finish = System.nanoTime();
//        if(blockState.getRenderType() == BlockRenderType.MODEL) {
//            long n = nanos.addAndGet(finish - start);
//            if(count.incrementAndGet() == 1000000) {
//                System.out.println(String.format("Avg block tesselate ns last 1000000 blocks = %d", n / 1000000));
//                nanos.set(0);
//                count.set(0);
//            }
//        }
//        return result;
    }

    /**
     * Release all references. Probably not necessary but would be $#%! to debug if
     * it is.
     */
    @Inject(at = @At("RETURN"), method = "rebuildChunk")
    private void hookRebuildChunkReturn(float float_1, float float_2, float float_3, ChunkRenderTask chunkRenderTask_1,
            CallbackInfo info) {
        TerrainRenderContext.POOL.get().release();
    }

    /**
     * Access method for renderer.
     */
    @Override
    public void fabric_beginBufferBuilding(BufferBuilder bufferBuilder_1, BlockPos blockPos_1) {
        beginBufferBuilding(bufferBuilder_1, blockPos_1);
    }
}
