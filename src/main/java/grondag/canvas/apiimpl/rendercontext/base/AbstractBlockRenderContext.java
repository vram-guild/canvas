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

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.material.MaterialFinder;
import io.vram.frex.api.material.MaterialMap;
import io.vram.frex.base.renderer.context.BaseBlockContext;

public abstract class AbstractBlockRenderContext<T extends BlockAndTintGetter, E> extends AbstractBakedRenderContext<BaseBlockContext<T>, E> {
	/**
	 * For use by chunk builder - avoids another threadlocal.
	 */
	public final BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();

	@Nullable protected VertexConsumer defaultConsumer;

	protected boolean defaultAo;

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
	public void prepareForBlock(BakedModel model, BlockState blockState, BlockPos blockPos, boolean modelAO, long seed, int overlay) {
		inputContext.prepareForBlock(model, blockState, blockPos, seed, overlay);
		prepareForBlock(blockState, modelAO);
	}

	public void prepareForBlock(BakedModel model, BlockState blockState, BlockPos blockPos, boolean modelAO) {
		inputContext.prepareForBlock(model, blockState, blockPos);
		prepareForBlock(blockState, modelAO);
	}

	public void prepareForFluid(BlockState blockState, BlockPos blockPos, boolean modelAO) {
		inputContext.prepareForFluid(blockState, blockPos);
		prepareForBlock(blockState, modelAO);
	}

	private void prepareForBlock(BlockState blockState, boolean modelAO) {
		materialMap = inputContext.isFluidModel() ? MaterialMap.get(blockState.getFluidState()) : MaterialMap.get(blockState);
		defaultAo = modelAO && Minecraft.useAmbientOcclusion() && blockState.getLightEmission() == 0;
	}

	@Override
	protected void adjustMaterial() {
		final MaterialFinder finder = this.finder;

		int bm = finder.preset();

		if (bm == MaterialConstants.PRESET_DEFAULT) {
			bm = inputContext.defaultPreset();
			finder.preset(MaterialConstants.PRESET_NONE);
		}

		if (inputContext.overlay() != OverlayTexture.NO_OVERLAY) {
			finder.overlay(inputContext.overlay());
		}

		// fully specific renderable material
		if (bm == MaterialConstants.PRESET_NONE) return;

		switch (bm) {
			case MaterialConstants.PRESET_CUTOUT: {
				finder.transparency(MaterialConstants.TRANSPARENCY_NONE)
					.cutout(MaterialConstants.CUTOUT_HALF)
					.unmipped(true)
					.target(MaterialConstants.TARGET_MAIN)
					.sorted(false);
				break;
			}
			case MaterialConstants.PRESET_CUTOUT_MIPPED:
				finder
					.transparency(MaterialConstants.TRANSPARENCY_NONE)
					.cutout(MaterialConstants.CUTOUT_HALF)
					.unmipped(false)
					.target(MaterialConstants.TARGET_MAIN)
					.sorted(false);
				break;
			case MaterialConstants.PRESET_TRANSLUCENT:
				finder.transparency(MaterialConstants.TRANSPARENCY_TRANSLUCENT)
					.cutout(MaterialConstants.CUTOUT_NONE)
					.unmipped(false)
					.target(MaterialConstants.TARGET_TRANSLUCENT)
					.sorted(true);
				break;
			case MaterialConstants.PRESET_SOLID:
				finder.transparency(MaterialConstants.TRANSPARENCY_NONE)
					.cutout(MaterialConstants.CUTOUT_NONE)
					.unmipped(false)
					.target(MaterialConstants.TARGET_MAIN)
					.sorted(false);
				break;
			default:
				assert false : "Unhandled blend mode";
		}
	}
}
