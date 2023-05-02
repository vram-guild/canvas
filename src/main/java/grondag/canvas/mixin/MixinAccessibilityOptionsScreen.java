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
import net.minecraft.client.Option;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.SimpleOptionsSubScreen;
import net.minecraft.network.chat.Component;

import grondag.canvas.apiimpl.CanvasState;
import grondag.canvas.shader.data.AccessibilityData;

@Mixin(AccessibilityOptionsScreen.class)
public class MixinAccessibilityOptionsScreen extends SimpleOptionsSubScreen {
	public MixinAccessibilityOptionsScreen(Screen screen, Options options, Component component, Option[] options2) {
		super(screen, options, component, options2);
	}

	@Inject(at = @At("HEAD"), method = "method_31384")
	void onClosing(CallbackInfo ci) {
		canvas_onClose();
	}

	@Override
	public void onClose() {
		canvas_onClose();
		super.onClose();
	}

	private static void canvas_onClose() {
		if (AccessibilityData.checkChanged() && Minecraft.getInstance().level != null) {
			CanvasState.recompileIfNeeded(true);
		}
	}
}
