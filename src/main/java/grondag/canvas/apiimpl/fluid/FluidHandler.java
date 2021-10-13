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

package grondag.canvas.apiimpl.fluid;

import java.util.function.BiFunction;
import java.util.function.Function;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.material.MaterialFinder;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.api.model.fluid.FluidAppearance;
import io.vram.frex.api.model.fluid.FluidModel;
import io.vram.frex.api.model.fluid.SimpleFluidModel;

public class FluidHandler {
	// WIP: derive material from fluid render layer and move this all to FREX as the default fluid model handling
	static final RenderMaterial WATER_MATERIAL = MaterialFinder.threadLocal()
			.preset(MaterialConstants.PRESET_TRANSLUCENT).disableAo(true).disableColorIndex(true).find();

	static final RenderMaterial LAVA_MATERIAL = MaterialFinder.threadLocal()
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
