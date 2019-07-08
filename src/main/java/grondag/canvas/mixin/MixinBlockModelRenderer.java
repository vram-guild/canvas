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

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import grondag.canvas.apiimpl.rendercontext.BlockRenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;

@Mixin(BlockModelRenderer.class)
public abstract class MixinBlockModelRenderer { 
    @Shadow
    protected BlockColors colorMap;

    @Inject(at = @At("HEAD"), method = "tesselate", cancellable = true)
    private void hookTesselate(ExtendedBlockView blockView, BakedModel model, BlockState state, BlockPos pos,
            BufferBuilder buffer, boolean checkSides, Random rand, long seed, CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(BlockRenderContext.POOL.get().tesselate((BlockModelRenderer) (Object) this,
                (RenderAttachedBlockView) blockView, model, state, pos, buffer, seed));
    }
}
