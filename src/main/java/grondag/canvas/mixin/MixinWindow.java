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

package grondag.canvas.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.platform.Window;
import grondag.canvas.config.Configurator;
import net.minecraft.client.Minecraft;

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

	@Inject (at = @At(value = "RETURN"), method = "updateFramebufferSize")
	private void afterUpdateFrameBufferSize(CallbackInfo ci) {
		// prevents mis-scaled startup screen
		if (Minecraft.ON_OSX && Configurator.reduceResolutionOnMac) {
			framebufferWidth /= 2;
			framebufferHeight /= 2;
		}
	}
}
