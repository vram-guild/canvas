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
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.mixinterface.AccessChunkRendererData;
import grondag.canvas.mixinterface.AccessChunkRendererRegion;

@Mixin(targets = "net.minecraft.client.render.chunk.ChunkBuilder$BuiltChunk$RebuildTask")
public abstract class MixinChunkRebuildTask {
	@Shadow protected BuiltChunk field_20839;
	@Shadow protected ChunkRendererRegion region;
	@Shadow private <E extends BlockEntity> void addBlockEntity(ChunkBuilder.ChunkData chunkData, Set<BlockEntity> set, E blockEntity) {}

	@Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk$RebuildTask;render(FFFLnet/minecraft/client/render/chunk/ChunkBuilder$ChunkData;Lnet/minecraft/client/render/chunk/BlockBufferBuilderStorage;)Ljava/util/Set;", cancellable = true)
	private void hookChunkBuild(float x, float y, float z, ChunkBuilder.ChunkData chunkData, BlockBufferBuilderStorage buffers, CallbackInfoReturnable<Set<BlockEntity>> ci) {
		final ChunkOcclusionDataBuilder chunkOcclusionDataBuilder = new ChunkOcclusionDataBuilder();
		final Set<BlockEntity> blockEntities = Sets.newHashSet();
		final BlockPos origin = field_20839.getOrigin();
		final AccessChunkRendererData chunkDataAccess = (AccessChunkRendererData) chunkData;
		final ChunkRendererRegion region = this.region;
		this.region = null;

		final MatrixStack matrixStack = new MatrixStack();

		if (region != null) {
			final TerrainRenderContext renderer = TerrainRenderContext.POOL.get();
			renderer.prepare((AccessChunkRendererRegion) region, chunkData, buffers, origin);

			final BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
			final BlockPos.Mutable searchPos = renderer.searchPos;

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

						final FluidState fluidState = region.getFluidState(searchPos);
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
							matrixStack.translate(vec3d.x, vec3d.y, vec3d.z);
							((AccessChunkRendererRegion) region).canvas_getTerrainContext().tesselateBlock(blockState, searchPos, blockRenderManager.getModel(blockState), matrixStack);
							matrixStack.pop();
						}
					}
				}
			}

			chunkDataAccess.canvas_endBuffering(x - xMin, y - yMin, z - zMin, buffers);
		}

		chunkDataAccess.canvas_setOcclusionGraph(chunkOcclusionDataBuilder.build());
		ci.setReturnValue(blockEntities);
	}
}
