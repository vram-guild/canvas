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

import java.util.EnumSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import grondag.canvas.chunk.occlusion.ChunkOcclusionBuilderAccessHelper;
import grondag.canvas.chunk.occlusion.ChunkOcclusionMap;
import grondag.canvas.core.PipelineManager;
import grondag.canvas.mixinext.ChunkRenderDataExt;
import grondag.canvas.mixinext.ChunkRendererDispatcherExt;
import grondag.canvas.mixinext.ChunkRendererListExt;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.render.chunk.ChunkRendererList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    @Shadow private ChunkRendererList chunkRendererList;
    @Shadow private ChunkRenderDispatcher chunkRenderDispatcher;
    
    
    @Inject(method = "setUpTerrain", at = @At("HEAD"), cancellable = false, require = 1)
    private void onPrepareTerrain(Camera camera, VisibleRegion region, int int_1, boolean boolean_1, CallbackInfo ci) {
        PipelineManager.INSTANCE.prepareForFrame(camera);
    }
    
    /**
     * Use pre-computed visibility stored during render chunk rebuild vs computing on fly each time.
     */
    @SuppressWarnings("unchecked")
    @Inject(method = "method_3285", at = @At("HEAD"), cancellable = true, require = 1)
    private void hookViewChunkVisibility(BlockPos pos, CallbackInfoReturnable<Set<Direction>> ci) {
        ChunkRenderer renderChunk = ((ChunkRendererDispatcherExt)chunkRenderDispatcher).canvas_chunkRenderer(pos);
        if(renderChunk != null)
        {
            Object visData = ((ChunkRenderDataExt)renderChunk.chunkRenderData).canvas_chunkVisibility().canvas_visibilityData();
            // unbuilt chunks won't have extended info
            if(visData != null) {
                // note we return copies because result may be modified
                EnumSet<Direction> result = EnumSet.noneOf(Direction.class);
                if (visData instanceof Set) {
                    result.addAll((Set<Direction>)visData);
                } else {
                    result.addAll(((ChunkOcclusionMap) visData).getFaceSet(ChunkOcclusionBuilderAccessHelper.PACK_FUNCTION.apply(pos)));
                }
                ci.setReturnValue(result);
            }
        }
    }
    
    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true, require = 1)
    private void onRenderLayer(BlockRenderLayer layer, Camera camera, CallbackInfoReturnable<Integer> ci) {
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
