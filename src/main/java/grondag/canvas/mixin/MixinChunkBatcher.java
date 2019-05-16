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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import grondag.canvas.chunk.ChunkRendererExt;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.chunk.ChunkBatcher;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.ChunkRenderer;

@Mixin(ChunkBatcher.class)
public abstract class MixinChunkBatcher {

//    @Shadow private BlockingQueue<BlockLayeredBufferBuilder> availableBuffers;
    
    //TODO: remove - doesn't seem necessary if buffer builder memory allocation is small anyway
    /**
     * Remove BlockLayerBufferBuilder instances - never needed/used with Canvas
     */
//    @Inject(method = "<init>*", at = @At("RETURN"), cancellable = true, require = 1)
//    private void onInit(CallbackInfo ci) {
//        availableBuffers.clear();
//    }

    
    /**
     * Making layer builder count zero will cause an error in creating queue, so avoid there here.
     */
//    @Redirect(method = "<init>*",  allow = 1, require = 1, at = @At(value = "INVOKE", ordinal = 0,
//            target = "Lcom/google/common/collect/Queues;newArrayBlockingQueue(I)Ljava/util/concurrent/ArrayBlockingQueue;"))
//    private ArrayBlockingQueue<?> getQueue(int size) {
//        return Queues.newArrayBlockingQueue(1);
//    }

    @Inject(method = "upload", at = @At("HEAD"), cancellable = true, require = 1)
    public void onUpload(final BlockRenderLayer blockRenderLayer, final BufferBuilder bufferBuilder,
            final ChunkRenderer renderChunk, final ChunkRenderData chunkData, final double distanceSq,
            CallbackInfoReturnable<ListenableFuture<Object>> ci) {
        if (MinecraftClient.getInstance().isOnThread()) // main thread check
            ci.setReturnValue(uploadChunk(blockRenderLayer, bufferBuilder, renderChunk, chunkData, distanceSq));
    }

    private static ListenableFuture<Object> uploadChunk(BlockRenderLayer blockRenderLayer, BufferBuilder bufferBuilder,
            ChunkRenderer renderChunk, ChunkRenderData compiledChunk, double distanceSq) {
        assert blockRenderLayer == BlockRenderLayer.SOLID || blockRenderLayer == BlockRenderLayer.TRANSLUCENT;

        if (blockRenderLayer == BlockRenderLayer.SOLID)
            ((ChunkRendererExt) renderChunk).canvas_solidUpload();
        else
            ((ChunkRendererExt) renderChunk).canvas_translucentUpload();

        return Futures.<Object>immediateFuture((Object) null);

    }
}
