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
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Sets;

import grondag.canvas.Canvas;
import grondag.canvas.accessor.AccessChunkRenderer;
import grondag.canvas.buffering.DrawableChunk.Solid;
import grondag.canvas.buffering.DrawableChunk.Translucent;
import grondag.canvas.hooks.ChunkRebuildHelper;
import grondag.canvas.hooks.ChunkRenderDataStore;
import grondag.canvas.mixinext.ChunkRenderDataExt;
import grondag.canvas.mixinext.ChunkRendererExt;
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
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

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
            
            //TODO: put back - disabled for testing
//            ci.cancel();
        }
    }

    @Inject(method = "delete", at = @At("RETURN"), require = 1)
    private void onDeleteGlResources(CallbackInfo ci) {
        releaseDrawables();
    }

    @Inject(method = "method_3665", require = 1, at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/render/chunk/ChunkRenderer;chunkRenderData:Lnet/minecraft/client/render/chunk/ChunkRenderData;"))
    private void onSetChunkData(ChunkRenderData chunkDataIn, CallbackInfo ci) {
        if (chunkRenderData == null || chunkRenderData == ChunkRenderData.EMPTY || chunkDataIn == chunkRenderData)
            return;

        ((ChunkRenderDataExt) chunkRenderData).getVisibilityData().releaseVisibilityData();
        ChunkRenderDataStore.release(chunkRenderData);
    }

    // shouldn't be necessary if rebuild chunk hook works, but insurance if not
    @Redirect(method = "rebuildChunk", require = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/chunk/ChunkRenderData;method_3640(Lnet/minecraft/class_854;)V"))
    private void onSetVisibility(ChunkRenderData compiledChunk, class_854 chunkVisibility) {
        compiledChunk.method_3640(chunkVisibility);
        ((ChunkRenderDataExt) compiledChunk).canvas_mergeRenderLayers();
    }

    @Inject(method = "clear", require = 1, at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/render/chunk/ChunkRenderer;chunkRenderData:Lnet/minecraft/client/render/chunk/ChunkRenderData;"))
    private void onClear(CallbackInfo ci) {
        if (chunkRenderData == null || chunkRenderData == ChunkRenderData.EMPTY)
            return;

        ((ChunkRenderDataExt) chunkRenderData).getVisibilityData().releaseVisibilityData();
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
    private void onRebuildChunk(final float x, final float y, final float z, final ChunkRenderTask chunkRenderTask,
            final CallbackInfo ci) {
        if (!Canvas.isModEnabled())
            return;

        final ChunkRebuildHelper help = ChunkRebuildHelper.get();
        help.clear();

        ChunkRenderData chunkRenderData = ChunkRenderDataStore.claim();
        BlockPos.Mutable origin = this.origin;

        World world = this.world;

        if (world != null) {

            chunkRenderTask.getLock().lock();

            try {
                if (chunkRenderTask.getStage() != ChunkRenderTask.Stage.COMPILING) {
                    return;
                }

                chunkRenderTask.setRenderData(chunkRenderData);
            } finally {
                chunkRenderTask.getLock().unlock();
            }

            class_852 visibilityData = new class_852();
            HashSet<BlockEntity> blockEntities = Sets.newHashSet();

            
            SafeWorldView safeWorldView = chunkRenderTask.getAndInvalidateWorldView();
            if (safeWorldView != null) {
                ++chunkUpdateCount;
                
                boolean[] layerFlags = help.layerFlags;
                TerrainRenderContext renderContext = TerrainRenderContext.POOL.get();
                renderContext.setChunkTask(chunkRenderTask);
                
                /**
                 * Capture the block layer result flags so our renderer can update then when more 
                 * than one layer is renderer for a single model. This is also where we signal the 
                 * renderer to prepare for a new chunk using the data we've accumulated up to this point.
                 */
                TerrainRenderContext.POOL.get().prepare((ChunkRenderer) (Object) this, origin, layerFlags);
                
                // TODO: can remove these?
                BlockModelRenderer.enableBrightnessCache();
                BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();

                final BlockPos.Mutable searchPos = help.searchPos;
                final int xMin = origin.getX();
                final int yMin = origin.getY();
                final int zMin = origin.getZ();
                final BufferBuilder[] builders = help.builders(chunkRenderTask.getBufferBuilders());

                for (int xPos = 0; xPos < 16; xPos++) {
                    for (int yPos = 0; yPos < 16; yPos++) {
                        for (int zPos = 0; zPos < 16; zPos++) {
                            searchPos.set(xMin + xPos, yMin + yPos, zMin + zPos);
                            BlockState blockState = safeWorldView.getBlockState(searchPos);
                            Block block = blockState.getBlock();
                            if (blockState.isFullOpaque(safeWorldView, searchPos)) {
                                visibilityData.method_3682(searchPos);
                            }

                            if (block.hasBlockEntity()) {
                                final BlockEntity blockEntity = safeWorldView.getBlockEntity(searchPos,
                                        WorldChunk.CreationType.CHECK);
                                if (blockEntity != null) {
                                    BlockEntityRenderer<BlockEntity> blockEntityRenderer = BlockEntityRenderDispatcher.INSTANCE
                                            .get(blockEntity);
                                    if (blockEntityRenderer != null) {
                                        // Fixes MC-112730 - no reason to render both globally and in chunk
                                        if (blockEntityRenderer.method_3563(blockEntity)) {
                                            // global renderer - like beacons
                                            blockEntities.add(blockEntity);
                                        } else {
                                            // chunk-local renderer
                                            chunkRenderData.addBlockEntity(blockEntity);
                                        }
                                    }
                                }
                            }

                            FluidState fluidState = safeWorldView.getFluidState(searchPos);
                            BlockRenderLayer renderLayer;
                            int renderLayerIndex;
                            BufferBuilder bufferBuilder;
                            if (!fluidState.isEmpty()) {
                                renderLayer = fluidState.getRenderLayer();
                                renderLayerIndex = renderLayer.ordinal();
                                bufferBuilder = chunkRenderTask.getBufferBuilders().get(renderLayerIndex);
                                if (!chunkRenderData.isBufferInitialized(renderLayer)) {
                                    chunkRenderData.markBufferInitialized(renderLayer);
                                    this.beginBufferBuilding(bufferBuilder, origin);
                                }

                                layerFlags[renderLayerIndex] |= blockRenderManager.tesselateFluid(searchPos,
                                        safeWorldView, bufferBuilder, fluidState);
                            }

                            if (blockState.getRenderType() == BlockRenderType.MODEL) {
                                //TODO: confirm main layer always updated, stop returning a value
                                renderContext.tesselateBlock(blockState, searchPos);
                            } else if (blockState.getRenderType() != BlockRenderType.INVISIBLE) {
                                renderLayer = block.getRenderLayer();
                                renderLayerIndex = renderLayer.ordinal();
                                bufferBuilder = chunkRenderTask.getBufferBuilders().get(renderLayerIndex);
                                if (!chunkRenderData.isBufferInitialized(renderLayer)) {
                                    chunkRenderData.markBufferInitialized(renderLayer);
                                    this.beginBufferBuilding(bufferBuilder, origin);
                                }

                                layerFlags[renderLayerIndex] |= blockRenderManager.tesselateBlock(blockState,
                                        searchPos, safeWorldView, bufferBuilder, help.random);
                            }
                        }
                    }
                }

                for (int i = 0; i < ChunkRebuildHelper.BLOCK_RENDER_LAYER_COUNT; i++) {
                    final BlockRenderLayer layer = help.layers[i];
                    if (layerFlags[layer.ordinal()]) {
                        ((ChunkRenderDataExt) chunkRenderData).canvas_setNonEmpty(layer);
                    }

                    if (chunkRenderData.isBufferInitialized(layer)) {
                        this.endBufferBuilding(layer, x, y, z, builders[i], chunkRenderData);
                    }
                }

                /**
                 * Release all references. Probably not necessary but would be $#%! to debug if
                 * it is.
                 */
                renderContext.release();
                BlockModelRenderer.disableBrightnessCache();
            }

            chunkRenderData.method_3640(visibilityData.method_3679());
            ((ChunkRenderDataExt)chunkRenderData).canvas_mergeRenderLayers();
            this.chunkRenderLock.lock();

            try {
                help.tileEntitiesToAdd.addAll(blockEntities);
                help.tileEntitiesToRemove.addAll(this.blockEntities);
                
                help.tileEntitiesToAdd.removeAll(this.blockEntities);
                help.tileEntitiesToRemove.removeAll(blockEntities);
                
                this.blockEntities.clear();
                this.blockEntities.addAll(blockEntities);
                this.renderer.method_3245(help.tileEntitiesToRemove, help.tileEntitiesToAdd);
            } finally {
                this.chunkRenderLock.unlock();
            }
        }
        ci.cancel();
    }

    /////

    // TODO: remove at release
//    private static final AtomicLong nanos = new AtomicLong();
//    private static final AtomicInteger count = new AtomicInteger();
//        long start = System.nanoTime();
//        boolean result;
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

    /**
     * Access method for renderer.
     */
    @Override
    public void fabric_beginBufferBuilding(BufferBuilder bufferBuilder_1, BlockPos blockPos_1) {
        beginBufferBuilding(bufferBuilder_1, blockPos_1);
    }
}
