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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.util.concurrent.ListenableFuture;

import grondag.acuity.Acuity;
import grondag.acuity.broken.PipelineHooks;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.ChunkRenderer;

@Mixin(ChunkRenderDispatcher.class)
public abstract class MixinChunkRenderDispatcher
{
    @Inject(method = "uploadChunk", at = @At("HEAD"), cancellable = true, require = 1)
    public void onUploadChunk(final BlockRenderLayer blockRenderLayer, final BufferBuilder bufferBuilder, final ChunkRenderer renderChunk, 
            final ChunkRenderData compiledChunk, final double distanceSq, CallbackInfoReturnable<ListenableFuture<Object>> ci)
    {
        if(Acuity.isModEnabled())
            ci.setReturnValue(PipelineHooks.uploadChunk((ChunkRenderDispatcher)(Object)this, blockRenderLayer, bufferBuilder, renderChunk,
                    compiledChunk, distanceSq));
    }
}
