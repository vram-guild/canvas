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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOptionsScreen;

import grondag.canvas.apiimpl.CanvasState;

@Mixin(AccessibilityOptionsScreen.class)
public class MixinAccessibilityOptionsScreen {
	private double canvas_prevGlintSpeed = 0.5d;
	private double canvas_prevGlintStrength = 0.75d;

	@Inject(at = @At("HEAD"), method = "init")
	void onInit(CallbackInfo ci) {
		final var mc = Minecraft.getInstance();
		canvas_prevGlintSpeed = mc.options.glintSpeed().get();
		canvas_prevGlintStrength = mc.options.glintStrength().get();
	}

	@Inject(at = @At("HEAD"), method = "method_31384")
	void onClose(CallbackInfo ci) {
		final var mc = Minecraft.getInstance();

		if (mc.level == null) {
			return;
		}

		final double glintSpeed = mc.options.glintSpeed().get();
		final double glintStrength = mc.options.glintStrength().get();

		if (canvas_prevGlintSpeed != glintSpeed || glintStrength != canvas_prevGlintStrength) {
			CanvasState.recompileIfNeeded(true);
		}
	}
}
