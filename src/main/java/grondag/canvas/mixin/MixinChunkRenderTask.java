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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.apiimpl.util.ChunkRendererRegionExt;
import net.minecraft.client.render.chunk.ChunkRenderTask;
import net.minecraft.client.render.chunk.ChunkRendererRegion;

@Mixin(ChunkRenderTask.class)
public abstract class MixinChunkRenderTask {
    @Shadow
    private ChunkRendererRegion region;

    /**
     * The block view reference is voided when
     * {@link ChunkRenderTask#takeRegion()} is called during chunk
     * rebuild, but we need it and it is harder to make reliable, non-invasive
     * changes there. So we capture the block view before the reference is voided
     * and send it to the renderer.
     * <p>
     * 
     * We also store a reference to the renderer in the view to avoid doing
     * thread-local lookups for each block.
     */
    @Inject(at = @At("HEAD"), method = "takeRegion")
    private void onTakeRegion(CallbackInfoReturnable<ChunkRendererRegion> info) {
        final ChunkRendererRegion blockView = region;
        if (blockView != null) {
            final TerrainRenderContext renderer = TerrainRenderContext.POOL.get();
            ChunkRendererRegionExt regionExt = (ChunkRendererRegionExt)region;
            renderer.setBlockView(regionExt.canvas_fastRegion());
            regionExt.canvas_renderer(renderer);
        }
    }
}
