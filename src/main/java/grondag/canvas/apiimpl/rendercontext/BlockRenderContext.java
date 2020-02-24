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

import java.util.function.Consumer;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.Matrix3f;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.light.AoCalculator;

/**
 * Context for non-terrain block rendering.
 */
public class BlockRenderContext extends AbstractRenderContext implements RenderContext {
	public static ThreadLocal<BlockRenderContext> POOL = ThreadLocal.withInitial(BlockRenderContext::new);

	public static void forceReload() {
		POOL = ThreadLocal.withInitial(BlockRenderContext::new);
	}

	private final BlockPos.Mutable searchPos = new BlockPos.Mutable();
	private final BlockRenderInfo blockInfo = new BlockRenderInfo();

	private final AoCalculator aoCalc = new AoCalculator() {
		@Override
		protected float ao(int cacheIndex) {
			final BlockRenderView blockView = blockInfo.blockView;
			if (blockView == null) {
				return 1f;
			}

			final int packedXyz5 = cacheIndexToXyz5(cacheIndex);
			final BlockPos pos = blockInfo.blockPos;
			final int x = (packedXyz5 & 31) - 1 + pos.getX();
			final int y = ((packedXyz5 >> 5) & 31) - 1+ pos.getY();
			final int z = (packedXyz5 >> 10) - 1+ pos.getZ();
			searchPos.set(x, y, z);
			final BlockState state = blockView.getBlockState(searchPos);
			return state.getLuminance() == 0 ? state.getAmbientOcclusionLightLevel(blockView, searchPos) : 1F;
		}

		@Override
		protected int brightness(int cacheIndex) {
			if (blockInfo.blockView == null) {
				return 15 << 20 | 15 << 4;
			}

			final int packedXyz5 = cacheIndexToXyz5(cacheIndex);
			final BlockPos pos = blockInfo.blockPos;
			final int x = (packedXyz5 & 31) - 1 + pos.getX();
			final int y = ((packedXyz5 >> 5) & 31) - 1+ pos.getY();
			final int z = (packedXyz5 >> 10) - 1+ pos.getZ();
			searchPos.set(x, y, z);
			return WorldRenderer.getLightmapCoordinates(blockInfo.blockView, blockInfo.blockView.getBlockState(searchPos), searchPos);
		}

		@Override
		protected boolean isOpaque(int cacheIndex) {
			final BlockRenderView blockView = blockInfo.blockView;

			if (blockView == null) {
				return false;
			}

			final int packedXyz5 = cacheIndexToXyz5(cacheIndex);
			final BlockPos pos = blockInfo.blockPos;
			final int x = (packedXyz5 & 31) - 1 + pos.getX();
			final int y = ((packedXyz5 >> 5) & 31) - 1+ pos.getY();
			final int z = (packedXyz5 >> 10) - 1+ pos.getZ();
			searchPos.set(x, y, z);
			final BlockState state = blockView.getBlockState(searchPos);
			return state.isFullOpaque(blockView, searchPos);
		}
	};

	private VertexConsumer bufferBuilder;
	private boolean didOutput = false;

	private VertexConsumer outputBuffer(RenderLayer renderLayer) {
		didOutput = true;
		return bufferBuilder;
	}

	public boolean tesselate(BlockModelRenderer vanillaRenderer, BlockRenderView blockView, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrixStack, VertexConsumer buffer, boolean checkSides, long seed, int overlay) {
		bufferBuilder = buffer;
		matrix = matrixStack.peek().getModel();
		normalMatrix = matrixStack.peek().getNormal();

		this.overlay = overlay;
		didOutput = false;
		aoCalc.prepare(0);
		blockInfo.setBlockView(blockView);
		blockInfo.prepareForBlock(state, pos, model.useAmbientOcclusion(), seed);

		((FabricBakedModel) model).emitBlockQuads(blockView, state, pos, blockInfo.randomSupplier, this);

		blockInfo.release();
		bufferBuilder = null;

		return didOutput;
	}

	private final AbstractBlockEncodingContext encodingContext = new AbstractBlockEncodingContext(blockInfo, this::outputBuffer, this::transform) {
		@Override
		public int overlay() {
			return overlay;
		}

		@Override
		public Matrix4f matrix() {
			return matrix;
		}

		@Override
		public Matrix3f normalMatrix() {
			return normalMatrix;
		}

		@Override
		public void computeLighting(MutableQuadViewImpl quad) {
			aoCalc.compute(quad);
		}
	};

	private final MeshConsumer meshConsumer = new MeshConsumer(encodingContext);

	private final FallbackConsumer fallbackConsumer = new FallbackConsumer(encodingContext, blockInfo);

	@Override
	public Consumer<Mesh> meshConsumer() {
		return meshConsumer;
	}

	@Override
	public Consumer<BakedModel> fallbackConsumer() {
		return fallbackConsumer;
	}

	@Override
	public QuadEmitter getEmitter() {
		return meshConsumer.getEmitter();
	}
}
