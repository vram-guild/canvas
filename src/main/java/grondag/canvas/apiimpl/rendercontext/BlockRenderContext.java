/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.apiimpl.rendercontext;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.math.FastMatrix3f;
import io.vram.frex.api.model.BlockModel;
import io.vram.sc.concurrency.SimpleConcurrentList;

import grondag.canvas.apiimpl.mesh.QuadEditorImpl;

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

	// FEAT: honor checkides parameter
	public void render(ModelBlockRenderer vanillaRenderer, BlockAndTintGetter blockView, BakedModel model, BlockState state, BlockPos pos, PoseStack matrixStack, VertexConsumer buffer, boolean checkSides, long seed, int overlay) {
		defaultConsumer = buffer;
		matrix = matrixStack.last().pose();
		normalMatrix = (FastMatrix3f) (Object) matrixStack.last().normal();
		this.overlay = overlay;
		region = blockView;
		prepareForBlock(state, pos, model.useAmbientOcclusion(), seed);
		((BlockModel) model).renderAsBlock(this, emitter());
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
