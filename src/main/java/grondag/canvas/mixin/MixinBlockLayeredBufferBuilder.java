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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.canvas.buffer.packing.CompoundBufferBuilder;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.chunk.BlockLayeredBufferBuilder;

@Mixin(BlockLayeredBufferBuilder.class)
public abstract class MixinBlockLayeredBufferBuilder {
    @Redirect(method = "<init>*", require = 4, at = @At(value = "NEW", args = "class=net/minecraft/client/render/BufferBuilder"))
    private BufferBuilder newBuferBuilder(int bufferSizeIn) {
        return new CompoundBufferBuilder(bufferSizeIn);
    }

    @Inject(method = "<init>*", require = 1, at = @At("RETURN"))
    private void onConstructed(CallbackInfo ci) {
        linkBuilders((BlockLayeredBufferBuilder) (Object) this);
    }

    private static void linkBuilders(BlockLayeredBufferBuilder cache) {
        linkBuildersInner(cache, BlockRenderLayer.SOLID);
        linkBuildersInner(cache, BlockRenderLayer.CUTOUT);
        linkBuildersInner(cache, BlockRenderLayer.MIPPED_CUTOUT);
        linkBuildersInner(cache, BlockRenderLayer.TRANSLUCENT);
    }

    private static void linkBuildersInner(BlockLayeredBufferBuilder cache, BlockRenderLayer layer) {
        CompoundBufferBuilder builder = (CompoundBufferBuilder) cache.get(layer);
        builder.setupLinks(cache, layer);
    }
}
