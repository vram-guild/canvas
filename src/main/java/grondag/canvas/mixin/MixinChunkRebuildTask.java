/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
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

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.chunk.FastRenderRegion;
import grondag.canvas.mixinterface.AccessChunkRendererData;
import grondag.canvas.mixinterface.AccessRebuildTask;
import grondag.canvas.perf.ChunkRebuildCounters;

@Mixin(targets = "net.minecraft.client.render.chunk.ChunkBuilder$BuiltChunk$RebuildTask")
public abstract class MixinChunkRebuildTask implements AccessRebuildTask {
	@Shadow protected BuiltChunk field_20839;
	private FastRenderRegion fastRegion;
	@Shadow private <E extends BlockEntity> void addBlockEntity(ChunkBuilder.ChunkData chunkData, Set<BlockEntity> set, E blockEntity) {}

	@Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk$RebuildTask;render(FFFLnet/minecraft/client/render/chunk/ChunkBuilder$ChunkData;Lnet/minecraft/client/render/chunk/BlockBufferBuilderStorage;)Ljava/util/Set;", cancellable = true)
	private void hookChunkBuild(float x, float y, float z, ChunkBuilder.ChunkData chunkData, BlockBufferBuilderStorage buffers, CallbackInfoReturnable<Set<BlockEntity>> ci) {
		final ChunkOcclusionDataBuilder chunkOcclusionDataBuilder = new ChunkOcclusionDataBuilder();
		final Set<BlockEntity> blockEntities = Sets.newHashSet();
		final BlockPos origin = field_20839.getOrigin();
		final AccessChunkRendererData chunkDataAccess = (AccessChunkRendererData) chunkData;
		final FastRenderRegion region = fastRegion;
		fastRegion = null;

		final MatrixStack matrixStack = new MatrixStack();

		if (region != null) {
			final ChunkRebuildCounters counter = ChunkRebuildCounters.get();
			final long start = counter.buildCounter.startRun();
			region.prepareForUse();

			final TerrainRenderContext context = TerrainRenderContext.POOL.get();
			context.prepare(region, chunkData, buffers, origin);

			final BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
			final BlockPos.Mutable searchPos = context.searchPos;

			final int xMin = origin.getX();
			final int yMin = origin.getY();
			final int zMin = origin.getZ();
			final int xMax = xMin + 16;
			final int yMax = yMin + 16;
			final int zMax = zMin + 16;

			for (int xPos = xMin; xPos < xMax; xPos++) {
				for (int yPos = yMin; yPos < yMax; yPos++) {
					for (int zPos = zMin; zPos < zMax; zPos++) {
						searchPos.set(xPos, yPos, zPos);

						final BlockState blockState = region.getBlockState(searchPos);
						final Block block = blockState.getBlock();

						if (blockState.isFullOpaque(region, searchPos)) {
							chunkOcclusionDataBuilder.markClosed(searchPos);
						}

						if (block.hasBlockEntity()) {
							final BlockEntity blockEntity = region.getBlockEntity(searchPos, WorldChunk.CreationType.CHECK);

							if (blockEntity != null) {
								this.addBlockEntity(chunkData, blockEntities, blockEntity);
							}
						}

						final FluidState fluidState = blockState.getFluidState();
						RenderLayer fluidLayer;
						BufferBuilder fluidBuffer;

						if (!fluidState.isEmpty()) {
							fluidLayer = RenderLayers.getFluidLayer(fluidState);
							fluidBuffer = buffers.get(fluidLayer);

							if(chunkDataAccess.canvas_markInitialized(fluidLayer)) {
								fluidBuffer.begin(7, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
							}

							if (blockRenderManager.renderFluid(searchPos, region, fluidBuffer, fluidState)) {
								chunkDataAccess.canvas_markPopulated(fluidLayer);
							}
						}

						if (blockState.getRenderType() != BlockRenderType.INVISIBLE) {
							matrixStack.push();
							matrixStack.translate(xPos & 15, yPos & 15, zPos & 15);
							final Vec3d vec3d = blockState.getOffsetPos(region, searchPos);

							if(vec3d != Vec3d.ZERO) {
								matrixStack.translate(vec3d.x, vec3d.y, vec3d.z);
							}

							context.tesselateBlock(blockState, searchPos, blockRenderManager.getModel(blockState), matrixStack);
							matrixStack.pop();
						}
					}
				}
			}

			chunkDataAccess.canvas_endBuffering(x - xMin, y - yMin, z - zMin, buffers);
			endTimer(counter, region, start);
			context.release();
			region.release();
		}

		chunkDataAccess.canvas_setOcclusionGraph(chunkOcclusionDataBuilder.build());
		ci.setReturnValue(blockEntities);
	}

	@Override
	public void canvas_setRegion(FastRenderRegion region) {
		fastRegion = region;
	}

	private void endTimer(ChunkRebuildCounters counter, FastRenderRegion world, long start) {
		final long nanos = counter.buildCounter.endRun(start);

		int blockCount = 0;
		int fluidCount = 0;

		final Iterator<BlockPos> it = BlockPos.iterate(field_20839.getOrigin(), field_20839.getOrigin().add(15, 15, 15)).iterator();
		while(it.hasNext()) {
			final BlockState state = world.getBlockState(it.next());
			if(state.getBlock().getRenderType(state) == BlockRenderType.MODEL) {
				blockCount++;
			}
			if(!state.getFluidState().isEmpty()) {
				fluidCount++;
			}
		}

		final int chunkCount = counter.buildCounter.addCount(1);
		blockCount = counter.blockCounter.addAndGet(blockCount);
		fluidCount = counter.fluidCounter.addAndGet(fluidCount);

		if(chunkCount == 2000) {
			final String copyStats = counter.copyCounter.stats();

			ChunkRebuildCounters.reset();

			final int total = blockCount + fluidCount;
			CanvasMod.LOG.info(String.format("Chunk Rebuild elapsed time per chunk for last 2000 chunks = %,dns", nanos / 2000));
			CanvasMod.LOG.info(String.format("Chunk Copy Stats: " + copyStats));
			CanvasMod.LOG.info(String.format("Time per fluid/block = %,dns  Count = %,d  fluid:block ratio = %d:%d",
					Math.round((double)nanos / total), total,
					Math.round(fluidCount * 100f / total), Math.round(blockCount * 100f / total)));

			CanvasMod.LOG.info("");
		}
	}
}
