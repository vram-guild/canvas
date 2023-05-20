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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

import grondag.canvas.config.Configurator;
import grondag.canvas.perf.Timekeeper;
import grondag.canvas.pipeline.BufferDebug;

@Mixin(Gui.class)
public class MixinGui {
	@Inject(method = "render", at = @At("RETURN"), cancellable = false, require = 1)
	private void afterRender(GuiGraphics guiGraphics, float f, CallbackInfo ci) {
		BufferDebug.renderOverlay(guiGraphics, ((Gui) (Object) this).getFont());
		Timekeeper.renderOverlay(guiGraphics, ((Gui) (Object) this).getFont());
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;useFancyGraphics()Z"))
	private boolean controlVignette() {
		return !Configurator.disableVignette;
	}
}
