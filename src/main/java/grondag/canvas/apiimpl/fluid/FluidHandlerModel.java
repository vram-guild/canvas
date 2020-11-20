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

import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;

import grondag.canvas.Configurator;
import grondag.frex.api.fluid.AbstractFluidModel;

public class FluidHandlerModel extends AbstractFluidModel {
	protected final FluidRenderHandler handler;
	protected final Sprite overlaySprite = ModelLoader.WATER_OVERLAY.getSprite();

	public FluidHandlerModel(Fluid fluid, FluidRenderHandler handler) {
		super(fluid == Fluids.FLOWING_LAVA || fluid == Fluids.LAVA ? FluidHandler.LAVA_MATERIAL : FluidHandler.WATER_MATERIAL, Configurator.blendFluidColors);
		this.handler = handler;
	}

	@Override
	public int getFluidColor(BlockRenderView view, BlockPos pos, FluidState state) {
		return handler.getFluidColor(view, pos, state);
	}

	@Override
	public Sprite[] getFluidSprites(BlockRenderView view, BlockPos pos, FluidState state) {
		return handler.getFluidSprites(view, pos, state);
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
