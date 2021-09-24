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
import io.vram.frex.api.model.FluidAppearance;
import io.vram.frex.api.model.FluidModel;
import io.vram.frex.api.model.SimpleFluidModel;
import io.vram.frex.api.renderer.Renderer;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class FluidHandler {
	// WIP: let material be registered with the appearance
	static final RenderMaterial WATER_MATERIAL = Renderer.get().materialFinder()
			.preset(MaterialConstants.PRESET_TRANSLUCENT).disableAo(true).disableColorIndex(true).find();

	static final RenderMaterial LAVA_MATERIAL = Renderer.get().materialFinder()
			.preset(MaterialConstants.PRESET_SOLID).disableAo(true).disableColorIndex(true).emissive(true).find();

	public static final BiFunction<Fluid, Function<Fluid, FluidModel>, FluidModel> HANDLER = (fluid, supplier) -> {
		if (fluid == Fluids.FLOWING_LAVA || fluid == Fluids.LAVA) {
			return new SimpleFluidModel(LAVA_MATERIAL, false, FluidAppearance.LAVA_APPEARANCE);
		} else if (fluid == Fluids.FLOWING_WATER || fluid == Fluids.WATER) {
			return new SimpleFluidModel(WATER_MATERIAL, false, FluidAppearance.WATER_APPEARANCE);
		} else if (supplier != null) {
			return supplier.apply(fluid);
		} else {
			final var app = FluidAppearance.get(fluid);
			return app == null ? new SimpleFluidModel(WATER_MATERIAL, false, FluidAppearance.WATER_APPEARANCE) : new SimpleFluidModel(WATER_MATERIAL, false, app);
		}
	};
}
