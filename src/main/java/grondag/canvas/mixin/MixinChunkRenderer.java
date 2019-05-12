/*******************************************************************************
 * Copyright 2019 grondag
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.mixin;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Sets;

import grondag.canvas.apiimpl.RendererImpl;
import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.buffer.packing.FluidBufferBuilder;
import grondag.canvas.buffer.packing.VertexCollectorList;
import grondag.canvas.chunk.ChunkRebuildHelper;
import grondag.canvas.chunk.ChunkRenderDataExt;
import grondag.canvas.chunk.ChunkRenderDataStore;
import grondag.canvas.chunk.ChunkRendererExt;
import grondag.canvas.chunk.DrawableChunk.Solid;
import grondag.canvas.chunk.DrawableChunk.Translucent;
import grondag.canvas.chunk.UploadableChunk;
import grondag.canvas.material.ShaderProps;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkOcclusionGraphBuilder;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.ChunkRenderTask;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.world.SafeWorldView;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(ChunkRenderer.class)
public abstract class MixinChunkRenderer implements ChunkRendererExt {
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

    /**
     * Holds vertex data and packing data for next upload if we have it. Buffer is
     * obtained from BufferStore and will be released back to store by upload.
     */
    private final AtomicReference<UploadableChunk.Solid> uploadSolid = new AtomicReference<>();
    private final AtomicReference<UploadableChunk.Translucent> uploadTranslucent = new AtomicReference<>();
    
    Solid solidDrawable;
    Translucent translucentDrawable;

    @Override
    public Solid canvas_solidDrawable() {
        return solidDrawable;
    }

    @Override
    public Translucent canvas_translucentDrawable() {
        return translucentDrawable;
    }

    @Inject(method = "delete", at = @At("RETURN"), require = 1)
    private void onDeleteGlResources(CallbackInfo ci) {
        canvas_releaseDrawables();
    }

    @Inject(method = "setChunkRenderData", require = 1, at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/render/chunk/ChunkRenderer;chunkRenderData:Lnet/minecraft/client/render/chunk/ChunkRenderData;"))
    private void onsetChunkRenderData(ChunkRenderData chunkDataIn, CallbackInfo ci) {
        if (chunkRenderData == null || chunkRenderData == ChunkRenderData.EMPTY || chunkDataIn == chunkRenderData)
            return;

        ((ChunkRenderDataExt) chunkRenderData).canvas_chunkVisibility().canvas_releaseVisibilityData();
        ChunkRenderDataStore.release(chunkRenderData);
    }

    @Inject(method = "clear", require = 1, at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/render/chunk/ChunkRenderer;chunkRenderData:Lnet/minecraft/client/render/chunk/ChunkRenderData;"))
    private void onClear(CallbackInfo ci) {
        canvas_releaseDrawables();
        
        if (chunkRenderData == null || chunkRenderData == ChunkRenderData.EMPTY)
            return;

        ((ChunkRenderDataExt) chunkRenderData).canvas_chunkVisibility().canvas_releaseVisibilityData();
        ChunkRenderDataStore.release(chunkRenderData);
    }

    @Override
    public void canvas_solidUpload() {
        final UploadableChunk.Solid uploadBuffer = uploadSolid.getAndSet(null);
        solidDrawable = uploadBuffer == null ? null : uploadBuffer.produceDrawable();
    }

    @Override
    public void canvas_translucentUpload() {
        final UploadableChunk.Translucent uploadBuffer = uploadTranslucent.getAndSet(null);
        translucentDrawable = uploadBuffer == null ? null : uploadBuffer.produceDrawable();
    }

    @Override
    public void canvas_releaseDrawables() {
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
        final TerrainRenderContext renderContext = TerrainRenderContext.POOL.get();
        final ChunkRebuildHelper help = renderContext.chunkRebuildHelper;
        help.clear();

        final ChunkRenderData chunkRenderData = ChunkRenderDataStore.claim();
        final ChunkRenderDataExt chunkDataExt = (ChunkRenderDataExt) chunkRenderData;
        final BlockPos.Mutable origin = this.origin;

        final World world = this.world;

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

            ChunkOcclusionGraphBuilder visibilityData = new ChunkOcclusionGraphBuilder();
            HashSet<BlockEntity> blockEntities = Sets.newHashSet();

            
            SafeWorldView safeWorldView = chunkRenderTask.getAndInvalidateWorldView();
            if (safeWorldView != null) {
                ++chunkUpdateCount;
                help.prepareCollectors(origin.getX(), origin.getY(), origin.getZ());
                
                renderContext.setChunkTask(chunkRenderTask);
                
                /**
                 * Capture the block layer result flags so our renderer can update them when more 
                 * than one layer is rendered for a single model. This is also where we signal the 
                 * renderer to prepare for a new chunk using the data we've accumulated up to this point.
                 */
                renderContext.prepare((ChunkRenderer) (Object) this, origin);
                
                // PERF: should still happen?  Replace with ours?
                BlockModelRenderer.enableBrightnessCache();
                final BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();

                final BlockPos.Mutable searchPos = help.searchPos;
                final int xMin = origin.getX();
                final int yMin = origin.getY();
                final int zMin = origin.getZ();

                for (int xPos = 0; xPos < 16; xPos++) {
                    for (int yPos = 0; yPos < 16; yPos++) {
                        for (int zPos = 0; zPos < 16; zPos++) {
                            searchPos.set(xMin + xPos, yMin + yPos, zMin + zPos);
                            BlockState blockState = safeWorldView.getBlockState(searchPos);
                            Block block = blockState.getBlock();
                            if (blockState.isFullOpaque(safeWorldView, searchPos)) {
                                visibilityData.markClosed(searchPos);
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

                            BlockRenderLayer renderLayer;
                            FluidState fluidState = safeWorldView.getFluidState(searchPos);
                            if (!fluidState.isEmpty()) {
                                renderLayer = fluidState.getRenderLayer();
                                //TODO: apply appropriate shader props for fluids
                                FluidBufferBuilder fluidBuilder = help.fluidBuilder.prepare(help.getCollector(renderLayer).get(RendererImpl.MATERIAL_STANDARD, ShaderProps.waterProps()), searchPos, renderLayer);
                                blockRenderManager.tesselateFluid(searchPos, safeWorldView, fluidBuilder, fluidState);
                            }

                            if (blockState.getRenderType() == BlockRenderType.MODEL) {
                                renderContext.tesselateBlock(blockState, searchPos);
                            }
                        }
                    }
                }

                if(!help.solidCollector.isEmpty()) {
                    chunkRenderData.markBufferInitialized(BlockRenderLayer.SOLID);
                    chunkDataExt.canvas_setNonEmpty(BlockRenderLayer.SOLID);
                    UploadableChunk.Solid abandoned = uploadSolid.getAndSet(help.solidCollector.packUploadSolid());
                    if(abandoned != null) {
                        abandoned.cancel();
                    }
                }
                
                if(!help.translucentCollector.isEmpty()) {
                    final VertexCollectorList vcl = help.translucentCollector;
                    chunkRenderData.markBufferInitialized(BlockRenderLayer.TRANSLUCENT);
                    chunkDataExt.canvas_setNonEmpty(BlockRenderLayer.TRANSLUCENT);
                    vcl.setViewCoordinates(x, y, z);
                    chunkDataExt.canvas_collectorState(vcl.getCollectorState(null));
                    UploadableChunk.Translucent abandoned = uploadTranslucent.getAndSet(vcl.packUploadTranslucent());
                    if(abandoned != null) {
                        abandoned.cancel();
                    }
                }

                /**
                 * Release all references. Probably not necessary but would be $#%! to debug if it is.
                 */
                renderContext.release();
                BlockModelRenderer.disableBrightnessCache();
            }

            chunkRenderData.method_3640(visibilityData.build());
            
            this.chunkRenderLock.lock();
            try {
                help.tileEntitiesToAdd.addAll(blockEntities);
                help.tileEntitiesToRemove.addAll(this.blockEntities);
                
                help.tileEntitiesToAdd.removeAll(this.blockEntities);
                help.tileEntitiesToRemove.removeAll(blockEntities);
                
                this.blockEntities.clear();
                this.blockEntities.addAll(blockEntities);
                this.renderer.updateBlockEntities(help.tileEntitiesToRemove, help.tileEntitiesToAdd);
            } finally {
                this.chunkRenderLock.unlock();
            }
        }
        ci.cancel();
    }
    
    @Inject(method = "resortTransparency", at = @At("HEAD"), cancellable = true, require = 1)
    public void onResortTransparency(float x, float y, float z, ChunkRenderTask chunkRenderTask, CallbackInfo ci) {
        final ChunkRenderData chunkRenderData = chunkRenderTask.getRenderData();
        final ChunkRenderDataExt chunkDataExt = (ChunkRenderDataExt) chunkRenderData;
        int[][] collectorState = chunkDataExt.canvas_collectorState();
        if (collectorState != null && !chunkRenderData.method_3641(BlockRenderLayer.TRANSLUCENT)) {
            VertexCollectorList translucentCollector = TerrainRenderContext.POOL.get().chunkRebuildHelper.translucentCollector;
            translucentCollector.loadCollectorState(collectorState);
            translucentCollector.setViewCoordinates(x, y, z);
            translucentCollector.setRelativeRenderOrigin(origin.getX(), origin.getY(), origin.getZ());
            UploadableChunk.Translucent abandoned = uploadTranslucent.getAndSet(translucentCollector.packUploadTranslucent());
            if(abandoned != null) {
                abandoned.cancel();
            }
        }
        ci.cancel();
     }
}
