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

package grondag.canvas.apiimpl.fluid;

import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import grondag.canvas.config.Configurator;
import grondag.frex.api.fluid.AbstractFluidModel;

public class WaterFluidModel extends AbstractFluidModel {
	protected final Sprite[] sprites = FluidHandler.waterSprites();
	protected final Sprite overlaySprite = ModelLoader.WATER_OVERLAY.getSprite();

	public WaterFluidModel() {
		super(FluidHandler.WATER_MATERIAL, Configurator.blendFluidColors);
	}

	@Override
	public int getFluidColor(BlockRenderView view, BlockPos pos, FluidState state) {
		return BiomeColors.getWaterColor(view, pos);
	}

	@Override
	public Sprite[] getFluidSprites(BlockRenderView view, BlockPos pos, FluidState state) {
		return sprites;
	}

	@Override
	protected boolean needsOverlay() {
		return true;
	}

	@Override
	protected Sprite overlaySprite() {
		return overlaySprite;
	}
}
