/*******************************************************************************
 * Copyright 2019, 2020 grondag
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
package grondag.canvas.chunk;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.mixinterface.AccessChunkRendererData;

public class ChunkRebuildinator {
	//	public static MicroTimer outer = new MicroTimer("outer", 1000);
	//	public static MicroTimer inner = new MicroTimer("inner", -1);

	public static void rebuildChunk(float x, float y, float z, AccessChunkRendererData chunkDataAccess, BlockBufferBuilderStorage buffers, ProtoRenderRegion protoRegion, BlockPos origin) {
		//outer.start();

		final TerrainRenderContext context = TerrainRenderContext.POOL.get().prepare(protoRegion, chunkDataAccess, buffers, origin);
		final FastRenderRegion region = context.region;
		final BlockPos.Mutable searchPos = context.searchPos;

		final int xMin = origin.getX();
		final int yMin = origin.getY();
		final int zMin = origin.getZ();

		chunkDataAccess.canvas_setOcclusionGraph(region.occlusion.build());

		ChunkRebuildinator.buildTerrain(chunkDataAccess, buffers, context, xMin, yMin, zMin, region, searchPos);

		chunkDataAccess.canvas_endBuffering(x - xMin, y - yMin, z - zMin, buffers);
		context.release();

		//		if (outer.stop()) {
		//			inner.reportAndClear();
		//			System.out.println();
		//		}
	}

	private static void buildTerrain(AccessChunkRendererData chunkDataAccess, BlockBufferBuilderStorage buffers, TerrainRenderContext context, int xMin, int yMin, int zMin, FastRenderRegion region, BlockPos.Mutable searchPos) {
		final MatrixStack matrixStack = new MatrixStack();
		final BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
		final OcclusionRegion occlusionRegion = region.occlusion;

		// TODO: use staight 0-4096 loop here and only split if going to render
		for (int xPos = 0; xPos < 16; xPos++) {
			final int xb = xPos + xMin;

			for (int yPos = 0; yPos < 16; yPos++) {
				final int yb = yPos + yMin;

				for (int zPos = 0; zPos < 16; zPos++) {

					if(occlusionRegion.shouldRender(xPos, yPos, zPos)) {
						searchPos.set(xb, yb, zPos + zMin);
						final BlockState blockState = region.getLocalBlockState(xPos, yPos, zPos);
						final FluidState fluidState = blockState.getFluidState();

						if (!fluidState.isEmpty()) {
							final RenderLayer fluidLayer = RenderLayers.getFluidLayer(fluidState);
							final BufferBuilder fluidBuffer = buffers.get(fluidLayer);

							if (chunkDataAccess.canvas_markInitialized(fluidLayer)) {
								fluidBuffer.begin(7, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
							}

							if (blockRenderManager.renderFluid(searchPos, region, fluidBuffer, fluidState)) {
								chunkDataAccess.canvas_markPopulated(fluidLayer);
							}
						}

						if (blockState.getRenderType() != BlockRenderType.INVISIBLE) {
							matrixStack.push();
							matrixStack.translate(xPos, yPos, zPos);

							if (blockState.getBlock().getOffsetType() != Block.OffsetType.NONE) {
								final Vec3d vec3d = blockState.getOffsetPos(region, searchPos);

								if (vec3d != Vec3d.ZERO) {
									matrixStack.translate(vec3d.x, vec3d.y, vec3d.z);
								}
							}

							context.tesselateBlock(blockState, searchPos, blockRenderManager.getModel(blockState), matrixStack);

							matrixStack.pop();
						}
					}
				}
			}
		}
	}
}
