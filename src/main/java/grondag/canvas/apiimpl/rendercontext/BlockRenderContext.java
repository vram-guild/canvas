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


package grondag.canvas.apiimpl.rendercontext;

import static grondag.canvas.chunk.RenderRegionAddressHelper.cacheIndexToXyz5;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;

import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.light.AoCalculator;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.mixinterface.Matrix3fExt;

/**
 * Context for non-terrain block rendering.
 */
public class BlockRenderContext extends AbstractBlockRenderContext<BlockRenderView> {
	public static ThreadLocal<BlockRenderContext> POOL = ThreadLocal.withInitial(BlockRenderContext::new);

	public static void forceReload() {
		POOL = ThreadLocal.withInitial(BlockRenderContext::new);
	}

	private final AoCalculator aoCalc = new AoCalculator() {
		@Override
		protected float ao(int cacheIndex) {
			if (region == null) {
				return 1f;
			}

			final int packedXyz5 = cacheIndexToXyz5(cacheIndex);
			final BlockPos pos = blockPos;
			final int x = (packedXyz5 & 31) - 1 + pos.getX();
			final int y = ((packedXyz5 >> 5) & 31) - 1+ pos.getY();
			final int z = (packedXyz5 >> 10) - 1+ pos.getZ();
			internalSearchPos.set(x, y, z);
			final BlockState state = region.getBlockState(internalSearchPos);
			return state.getLuminance() == 0 ? state.getAmbientOcclusionLightLevel(region, internalSearchPos) : 1F;
		}

		@Override
		protected int brightness(int cacheIndex) {
			if (region == null) {
				return 15 << 20 | 15 << 4;
			}

			final int packedXyz5 = cacheIndexToXyz5(cacheIndex);
			final BlockPos pos = blockPos;
			final int x = (packedXyz5 & 31) - 1 + pos.getX();
			final int y = ((packedXyz5 >> 5) & 31) - 1+ pos.getY();
			final int z = (packedXyz5 >> 10) - 1+ pos.getZ();
			internalSearchPos.set(x, y, z);
			return WorldRenderer.getLightmapCoordinates(region, region.getBlockState(internalSearchPos), internalSearchPos);
		}

		@Override
		protected boolean isOpaque(int cacheIndex) {
			final BlockRenderView blockView = region;

			if (blockView == null) {
				return false;
			}

			final int packedXyz5 = cacheIndexToXyz5(cacheIndex);
			final BlockPos pos = blockPos;
			final int x = (packedXyz5 & 31) - 1 + pos.getX();
			final int y = ((packedXyz5 >> 5) & 31) - 1+ pos.getY();
			final int z = (packedXyz5 >> 10) - 1+ pos.getZ();
			internalSearchPos.set(x, y, z);
			final BlockState state = blockView.getBlockState(internalSearchPos);
			return state.isOpaqueFullCube(blockView, internalSearchPos);
		}
	};

	private VertexConsumer bufferBuilder;
	private boolean didOutput = false;

	public boolean tesselate(BlockModelRenderer vanillaRenderer, BlockRenderView blockView, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrixStack, VertexConsumer buffer, boolean checkSides, long seed, int overlay) {
		bufferBuilder = buffer;
		matrix = matrixStack.peek().getModel();
		normalMatrix = (Matrix3fExt)(Object) matrixStack.peek().getNormal();

		this.overlay = overlay;
		didOutput = false;
		aoCalc.prepare(0);
		region = blockView;
		prepareForBlock(state, pos, model.useAmbientOcclusion(), seed);

		((FabricBakedModel) model).emitBlockQuads(blockView, state, pos, randomSupplier, this);

		bufferBuilder = null;

		return didOutput;
	}

	@Override
	public MaterialContext materialContext() {
		return MaterialContext.BLOCK;
	}

	@Override
	public VertexConsumer consumer(Value mat) {
		didOutput = true;
		return bufferBuilder;
	}

	@Override
	public int brightness() {
		return 0;
	}

	@Override
	public AoCalculator aoCalc() {
		return aoCalc;
	}

	@Override
	public boolean cullTest(Direction face) {
		return true;
	}

	@Override
	protected int fastBrightness(BlockState blockState, BlockPos pos) {
		return WorldRenderer.getLightmapCoordinates(region, blockState, pos);
	}
}
