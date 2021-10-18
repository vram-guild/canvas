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

import static grondag.canvas.buffer.format.EncoderUtils.applyBlockLighting;
import static grondag.canvas.buffer.format.EncoderUtils.bufferQuad;
import static grondag.canvas.buffer.format.EncoderUtils.colorizeQuad;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.material.MaterialMap;
import io.vram.frex.base.renderer.context.BaseBlockContext;
import io.vram.frex.base.renderer.mesh.BaseQuadEmitter;

import grondag.canvas.buffer.format.QuadEncoders;
import grondag.canvas.material.state.CanvasRenderMaterial;
import grondag.canvas.mixinterface.RenderTypeExt;

public abstract class AbstractBlockRenderContext<T extends BlockAndTintGetter> extends AbstractRenderContext<BaseBlockContext<T>> {
	/**
	 * For use by chunk builder - avoids another threadlocal.
	 */
	public final BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();

	@Nullable protected VertexConsumer defaultConsumer;

	public boolean defaultAo;

	protected AbstractBlockRenderContext(String name) {
		super(name);
	}

	@Override
	protected BaseBlockContext<T> createInputContext() {
		return new BaseBlockContext<>();
	}

	/**
	 * @param blockState
	 * @param blockPos
	 * @param modelAO
	 * @param seed       pass -1 for default behavior
	 */
	public void prepareForBlock(BlockState blockState, BlockPos blockPos, boolean modelAO, long seed) {
		inputContext.prepareForBlock(blockState, blockPos, seed);
		materialMap = isFluidModel ? MaterialMap.get(blockState.getFluidState()) : MaterialMap.get(blockState);
		defaultAo = modelAO && Minecraft.useAmbientOcclusion() && blockState.getLightEmission() == 0;

		// FEAT: support additional blend modes on terrain blocks?
		defaultPreset = isFluidModel
			? ((RenderTypeExt) ItemBlockRenderTypes.getRenderLayer(blockState.getFluidState())).canvas_preset()
			: ((RenderTypeExt) ItemBlockRenderTypes.getChunkRenderType(blockState)).canvas_preset();
	}

	@Override
	public boolean cullTest(int faceIndex) {
		return inputContext.cullTest(faceIndex);
	}

	@Override
	public final boolean defaultAo() {
		return defaultAo;
	}

	@Override
	protected void encodeQuad(BaseQuadEmitter quad) {
		// needs to happen before offsets are applied
		applyBlockLighting(quad, this);
		colorizeQuad(quad, this.inputContext);

		if (collectors == null) {
			bufferQuad(quad, encodingContext, defaultConsumer);
		} else {
			QuadEncoders.STANDARD_ENCODER.encode(quad, encodingContext, collectors.get((CanvasRenderMaterial) quad.material()));
		}
	}
}
