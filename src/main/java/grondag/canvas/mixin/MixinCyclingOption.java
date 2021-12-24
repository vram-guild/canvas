/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.CycleOption;
import net.minecraft.client.Option;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.TranslatableComponent;

import grondag.canvas.varia.CanvasButtonWidget;

@Mixin(CycleOption.class)
public abstract class MixinCyclingOption {
	@Inject(at = @At("HEAD"), method = "createButton", cancellable = true)
	private void onCreateButton(Options options, int x, int y, int width, CallbackInfoReturnable<AbstractWidget> info) {
		final CycleOption<?> self = (CycleOption<?>) (Object) this;

		if (self == Option.GRAPHICS) {
			info.setReturnValue(new CanvasButtonWidget(x, y, width, 20, new TranslatableComponent("config.canvas.button")));
		}
	}
}
