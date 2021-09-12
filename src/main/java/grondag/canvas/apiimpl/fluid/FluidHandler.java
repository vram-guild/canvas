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

import java.util.function.BiFunction;
import java.util.function.Function;

import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.api.renderer.Renderer;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;

import grondag.frex.api.fluid.FluidQuadSupplier;

public class FluidHandler {
	public static final BiFunction<Fluid, Function<Fluid, FluidQuadSupplier>, FluidQuadSupplier> HANDLER = (fluid, supplier) -> {
		if (fluid == Fluids.FLOWING_LAVA || fluid == Fluids.LAVA) {
			return new LavaFluidModel();
		} else if (fluid == Fluids.FLOWING_WATER || fluid == Fluids.WATER) {
			return new WaterFluidModel();
		} else if (supplier != null) {
			return supplier.apply(fluid);
		} else {
			final FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid);
			return handler == null ? new WaterFluidModel() : new FluidHandlerModel(fluid, handler);
		}
	};

	static final RenderMaterial WATER_MATERIAL = Renderer.get().materialFinder()
			.preset(MaterialConstants.PRESET_TRANSLUCENT).disableAo(true).disableColorIndex(true).find();

	static final RenderMaterial LAVA_MATERIAL = Renderer.get().materialFinder()
			.preset(MaterialConstants.PRESET_SOLID).disableAo(true).disableColorIndex(true).emissive(true).find();

	static final Sprite[] lavaSprites() {
		final Sprite[] result = new Sprite[2];
		result[0] = MinecraftClient.getInstance().getBakedModelManager().getBlockModels().getModel(Blocks.LAVA.getDefaultState()).getParticleSprite();
		result[1] = ModelLoader.LAVA_FLOW.getSprite();
		return result;
	}

	static final Sprite[] waterSprites() {
		final Sprite[] result = new Sprite[2];
		result[0] = MinecraftClient.getInstance().getBakedModelManager().getBlockModels().getModel(Blocks.WATER.getDefaultState()).getParticleSprite();
		result[1] = ModelLoader.WATER_FLOW.getSprite();
		return result;
	}
}
