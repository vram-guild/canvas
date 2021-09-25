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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.vram.frex.api.model.BlockModel;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import grondag.canvas.apiimpl.mesh.QuadEditorImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.fermion.sc.concurrency.SimpleConcurrentList;

/**
 * Context for non-terrain block rendering.
 */
public class BlockRenderContext extends AbstractBlockRenderContext<BlockAndTintGetter> {
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

	public void render(ModelBlockRenderer vanillaRenderer, BlockAndTintGetter blockView, BakedModel model, BlockState state, BlockPos pos, PoseStack matrixStack, VertexConsumer buffer, boolean checkSides, long seed, int overlay) {
		defaultConsumer = buffer;
		matrix = matrixStack.last().pose();
		normalMatrix = (Matrix3fExt) (Object) matrixStack.last().normal();
		this.overlay = overlay;
		region = blockView;
		prepareForBlock(state, pos, model.useAmbientOcclusion(), seed);
		((BlockModel) model).renderAsBlock(blockView, state, pos, this);
		defaultConsumer = null;
	}

	@Override
	public int brightness() {
		return 0;
	}

	@Override
	protected int fastBrightness(BlockState blockState, BlockPos pos) {
		return LevelRenderer.getLightColor(region, blockState, pos);
	}

	@Override
	protected void adjustMaterial() {
		super.adjustMaterial();
		finder.disableAo(true);
	}

	@Override
	public void computeAo(QuadEditorImpl quad) {
		// NOOP
	}

	@Override
	public void computeFlat(QuadEditorImpl quad) {
		computeFlatSimple(quad);
	}
}
