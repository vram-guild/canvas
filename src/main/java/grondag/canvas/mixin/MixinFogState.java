/*
 * Copyright 2019, 2020 grondag
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
 */

package grondag.canvas.mixin;

import grondag.canvas.mixinterface.FogStateExt;
import grondag.canvas.varia.FogStateExtHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.mojang.blaze3d.platform.GlStateManager$FogState")
public abstract class MixinFogState implements FogStateExt {
	@Shadow
	public int mode;

	@Shadow
	public float density;

	@Shadow
	public float start;

	@Shadow
	public float end;

	@Override
	public int getMode() {
		return mode;
	}

	@Override
	public float getDensity() {
		return density;
	}

	@Override
	public float getStart() {
		return start;
	}

	@Override
	public float getEnd() {
		return end;
	}

	@Inject(method = "<init>()V", require = 1, at = @At("RETURN"))
	private void onConstructed(CallbackInfo ci) {
		FogStateExtHolder.INSTANCE = (this);
	}
}
