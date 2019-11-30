/*******************************************************************************
 * Copyright 2019 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.LightmapTextureManager;

import grondag.canvas.material.ShaderManager;
import grondag.canvas.varia.WorldDataManager;

@Mixin(LightmapTextureManager.class)
public abstract class MixinLightmapTextureManager {

	@Shadow
	private float prevFlicker;

	@ModifyArg(method = "update", index = 2, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/texture/NativeImage;setPixelRGBA(III)V"))
	private int onSetPixelRGBA(int i, int j, int color) {
		if(i == 15 && j == 15) {
			ShaderManager.INSTANCE.updateEmissiveColor(color);
		}
		return color;
	}

	//UGLY: still needed?
	@Inject(at = @At("RETURN"), method = "update")
	private void afterUpdate(float tick, CallbackInfo info) {
		WorldDataManager.updateLight(tick, prevFlicker);
	}
}
