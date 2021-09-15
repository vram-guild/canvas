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
import io.vram.frex.api.model.FluidModel;
import io.vram.frex.api.renderer.Renderer;

import net.minecraft.fluid.Fluid;

public class FluidHandler {
	public static final BiFunction<Fluid, Function<Fluid, FluidModel>, FluidModel> HANDLER = (fluid, supplier) -> {
		return null;
		//		if (fluid == Fluids.FLOWING_LAVA || fluid == Fluids.LAVA || fluid == Fluids.FLOWING_WATER || fluid == Fluids.WATER) {
		//			new BasicFluidModel(fluid, FluidAppearance.STILL_WATER_APPEARANCE);
		//			return new LavaFluidModel();
		//		} else if (fluid == Fluids.FLOWING_WATER || fluid == Fluids.WATER) {
		//			return new WaterFluidModel();
		//		} else if (supplier != null) {
		//			return supplier.apply(fluid);
		//		} else {
		//			final FluidAppearance handler = FluidAppearance.get(fluid);
		//			return handler == null ? new BasicFluidModel(fluid, FluidAppearance.STILL_WATER_APPEARANCE) : new BasicFluidModel(fluid, handler);
		//		}
	};

	static final RenderMaterial WATER_MATERIAL = Renderer.get().materialFinder()
			.preset(MaterialConstants.PRESET_TRANSLUCENT).disableAo(true).disableColorIndex(true).find();

	static final RenderMaterial LAVA_MATERIAL = Renderer.get().materialFinder()
			.preset(MaterialConstants.PRESET_SOLID).disableAo(true).disableColorIndex(true).emissive(true).find();
}
