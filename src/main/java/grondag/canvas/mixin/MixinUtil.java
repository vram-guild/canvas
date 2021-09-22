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

import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.Util;

import grondag.canvas.config.Configurator;
import grondag.canvas.terrain.util.TerrainExecutor;

@Mixin(Util.class)
public class MixinUtil {
	@Inject(at = @At("HEAD"), method = "backgroundExecutor", cancellable = true)
	private static void onGetBackgroundExecutor(CallbackInfoReturnable<Executor> ci) {
		if (Configurator.useCombinedThreadPool) {
			ci.setReturnValue(TerrainExecutor.INSTANCE);
		}
	}
}
