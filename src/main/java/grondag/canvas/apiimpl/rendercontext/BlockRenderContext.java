/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.apiimpl.rendercontext;

import java.util.function.Supplier;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.fermion.sc.concurrency.SimpleConcurrentList;

/**
 * Context for non-terrain block rendering.
 */
public class BlockRenderContext extends AbstractBlockRenderContext<BlockRenderView> {
	private static final SimpleConcurrentList<AbstractRenderContext> LOADED = new SimpleConcurrentList<>(AbstractRenderContext.class);

	private static final Supplier<ThreadLocal<BlockRenderContext>> POOL_FACTORY = () -> ThreadLocal.withInitial(() -> {
		final BlockRenderContext result = new BlockRenderContext();
		LOADED.add(result);
		return result;
	});

	private static ThreadLocal<BlockRenderContext> POOL = POOL_FACTORY.get();

	public BlockRenderContext() {
		super("BlockRenderContext");
	}

	public static void reload() {
		LOADED.forEach(c -> c.close());
		LOADED.clear();
		POOL = POOL_FACTORY.get();
	}

	public static BlockRenderContext get() {
		return POOL.get();
	}

	public void render(BlockModelRenderer vanillaRenderer, BlockRenderView blockView, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrixStack, VertexConsumer buffer, boolean checkSides, long seed, int overlay) {
		defaultConsumer = buffer;
		matrix = matrixStack.peek().getModel();
		normalMatrix = (Matrix3fExt) (Object) matrixStack.peek().getNormal();
		this.overlay = overlay;
		region = blockView;
		prepareForBlock(state, pos, model.useAmbientOcclusion(), seed);
		((FabricBakedModel) model).emitBlockQuads(blockView, state, pos, randomSupplier, this);
		defaultConsumer = null;
	}

	@Override
	public int brightness() {
		return 0;
	}

	@Override
	protected int fastBrightness(BlockState blockState, BlockPos pos) {
		return WorldRenderer.getLightmapCoordinates(region, blockState, pos);
	}

	@Override
	protected void adjustMaterial() {
		super.adjustMaterial();
		finder.disableAo(true);
	}

	@Override
	public void computeAo(MutableQuadViewImpl quad) {
		// NOOP
	}

	@Override
	public void computeFlat(MutableQuadViewImpl quad) {
		computeFlatSimple(quad);
	}
}
