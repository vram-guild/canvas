/*******************************************************************************
 * Copyright (C) 2018 grondag
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import grondag.acuity.api.AcuityRuntimeImpl;
import grondag.acuity.api.pipeline.PipelineManagerImpl;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BoundingBoxTest;

// PERF: restore visibility hooks if profiling shows worthwhile
// Computation is in class_852
// See forge branch MixinVisGraph.onComputeVisibility for details

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Inject(method = "prepareTerrain", at = @At("HEAD"), cancellable = false, require = 1)
    void onPrepareTerrain(Entity cameraEntity, float fractionalTicks, BoundingBoxTest class_856_1, int int_1, boolean boolean_1)
    {
        PipelineManagerImpl.INSTANCE.prepareForFrame(cameraEntity, fractionalTicks);
    }
    
    @Inject(method = "reload", at = @At("HEAD"), cancellable = false, require = 1)
    void onReload(CallbackInfo ci)
    {
        AcuityRuntimeImpl.INSTANCE.forceReload();
    }
}
