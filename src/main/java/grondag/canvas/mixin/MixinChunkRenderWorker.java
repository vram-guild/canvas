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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.ChunkRenderWorker;

@Mixin(ChunkRenderWorker.class)
public abstract class MixinChunkRenderWorker {
    @Redirect(method = "runTask", require = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/chunk/ChunkRenderData;isBufferInitialized(Lnet/minecraft/block/BlockRenderLayer;)Z"))
    private boolean isLayerStarted(ChunkRenderData chunkData, BlockRenderLayer layer) {
        return shouldUploadLayer(chunkData, layer);
    }

    private static boolean shouldUploadLayer(ChunkRenderData chunkData, BlockRenderLayer blockrenderlayer) {
        // skip if empty
        return chunkData.isBufferInitialized(blockrenderlayer) && !chunkData.method_3641(blockrenderlayer);
    }
}
