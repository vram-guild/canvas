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

package grondag.canvas.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.Minecraft;

import grondag.canvas.config.Configurator;

// Approach is based on that used by RetiNo, by Julian Dunskus
// https://github.com/juliand665/retiNO
// Original is licensed under MIT
@Mixin(Window.class)
public class MixinWindow {
	@Shadow private int framebufferWidth;
	@Shadow private int framebufferHeight;

	@Redirect (at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwDefaultWindowHints()V"), method = "<init>", remap = false)
	private void onDefaultWindowHints() {
		GLFW.glfwDefaultWindowHints();

		if (Minecraft.ON_OSX && Configurator.reduceResolutionOnMac) {
			GLFW.glfwWindowHint(GLFW.GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW.GLFW_FALSE);
		}
	}

	@Inject (at = @At(value = "RETURN"), method = "refreshFramebufferSize")
	private void afterRefreshFramebufferSize(CallbackInfo ci) {
		// prevents mis-scaled startup screen
		if (Minecraft.ON_OSX && Configurator.reduceResolutionOnMac) {
			framebufferWidth /= 2;
			framebufferHeight /= 2;
		}
	}
}
