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

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import grondag.canvas.Canvas;
import grondag.canvas.RendererImpl;
import grondag.canvas.core.PipelineManager;
import grondag.canvas.mixinext.ChunkRendererListExt;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkRendererList;
import net.minecraft.entity.Entity;

// PERF: restore visibility hooks if profiling shows worthwhile
// Computation is in class_852
// See forge branch MixinVisGraph.onComputeVisibility for details

// PERF: also see setupTerrain vs setupTerrainFast in Acuity

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    @Shadow private ChunkRendererList chunkRendererList;
    
    @Inject(method = "setUpTerrain", at = @At("HEAD"), cancellable = false, require = 1)
    private void onPrepareTerrain(Entity cameraEntity, float fractionalTicks, VisibleRegion region, int int_1, boolean boolean_1, CallbackInfo ci) {
        PipelineManager.INSTANCE.prepareForFrame(cameraEntity, fractionalTicks);
    }

    @Inject(method = "reload", at = @At("HEAD"), cancellable = false, require = 1)
    private void onReload(CallbackInfo ci) {
        RendererImpl.INSTANCE.forceReload();
    }
    
    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true, require = 1)
    private void onRenderLayer(BlockRenderLayer layer, double fractionalTick, Entity viewEntity, CallbackInfoReturnable<Integer> ci) {
        if (Canvas.isModEnabled()) {
            switch (layer) {

            case CUTOUT:
            case MIPPED_CUTOUT:
                ci.setReturnValue(0);
                break;
                
            case SOLID:
                // Must happen after camera transform is set up and before chunk render
                ((ChunkRendererListExt)chunkRendererList).canvas_prepareForFrame();
                break;
                
            case TRANSLUCENT:
            default:
                // nothing
                break;
            }
        }
    }
}
