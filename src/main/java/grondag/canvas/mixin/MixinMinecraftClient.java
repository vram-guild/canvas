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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.canvas.pipeline.PipelineManager;
import grondag.canvas.varia.CanvasGlHelper;
import grondag.canvas.varia.MinecraftClientExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.profiler.DisableableProfiler;
import net.minecraft.util.profiler.Profiler;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient implements MinecraftClientExt {
    @Shadow
    DisableableProfiler profiler;

    @Override
    public Profiler canvas_profiler() {
        return profiler;
    }

    @Inject(at = @At("RETURN"), method = "init")
    private void hookInit(CallbackInfo info) {
        CanvasGlHelper.init();
        PipelineManager.INSTANCE.forceReload();
    }
}
