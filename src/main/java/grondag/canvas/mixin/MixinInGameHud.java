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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import grondag.canvas.config.Configurator;
import grondag.canvas.perf.Timekeeper;
import grondag.canvas.pipeline.BufferDebug;
import net.minecraft.client.gui.Gui;

@Mixin(Gui.class)
public class MixinInGameHud {
	@Inject(method = "render", at = @At("RETURN"), cancellable = false, require = 1)
	private void afterRender(PoseStack matrices, float tickDelta, CallbackInfo ci) {
		BufferDebug.renderOverlay(matrices, ((Gui) (Object) this).getFont());
		Timekeeper.renderOverlay(matrices, ((Gui) (Object) this).getFont());
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;isFancyGraphicsOrBetter()Z"))
	private boolean controlVignette() {
		return !Configurator.disableVignette;
	}
}
