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

package grondag.canvas.apiimpl.rendercontext.base;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.math.MatrixStack;
import io.vram.frex.api.model.BlockModel;
import io.vram.frex.base.renderer.mesh.BaseQuadEmitter;

/**
 * Context for non-terrain block rendering.
 */
public abstract class BlockRenderContext<E> extends AbstractBlockRenderContext<BlockAndTintGetter, E> {
	public void render(ModelBlockRenderer vanillaRenderer, BlockAndTintGetter blockView, BakedModel model, BlockState state, BlockPos pos, PoseStack poseStack, VertexConsumer buffer, boolean checkSides, long seed, int overlay) {
		// WIP: try moving this to input context?
		defaultConsumer = buffer;
		inputContext.prepareForWorld(blockView, checkSides, MatrixStack.cast(poseStack));
		prepareForBlock(state, pos, model.useAmbientOcclusion(), seed, overlay);
		((BlockModel) model).renderAsBlock(inputContext, emitter());
	}

	@Override
	protected void adjustMaterial() {
		super.adjustMaterial();
		finder.disableAo(true);
	}

	@Override
	public void computeAo(BaseQuadEmitter quad) {
		// NOOP
	}

	@Override
	public void computeFlat(BaseQuadEmitter quad) {
		computeFlatSimple(quad);
	}
}
