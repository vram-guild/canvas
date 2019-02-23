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
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.LoadingConfig;
import grondag.acuity.broken.PipelineHooks;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;

@Mixin(BlockRenderManager.class)
public abstract class MixinBlockRendererManager
{
    @Redirect(method = "tesselateBlock", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/client/render/block/BlockRenderManager;tesselate(Lnet/minecraft/world/ExtendedBlockView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/BufferBuilder;ZLjava/util/Random;J)Z"))
    private boolean renderModel(BlockModelRenderer blockModelRenderer, ExtendedBlockView blockAccessIn, BakedModel modelIn, BlockState blockStateIn, BlockPos blockPosIn, BufferBuilder buffer, boolean checkSides)
    {
        if(LoadingConfig.INSTANCE.enableBlockStats)
            return PipelineHooks.renderModelDebug(blockModelRenderer, blockAccessIn, modelIn, blockStateIn, blockPosIn, buffer, checkSides);
        else
            return PipelineHooks.renderModel(blockModelRenderer, blockAccessIn, modelIn, blockStateIn, blockPosIn, buffer, checkSides);
    }
    
    @Redirect(method = "tesselateFluid", at = @At(value = "INVOKE", 
            target = "net/minecraft/client/render/block/FluidRenderer;tesselate(Lnet/minecraft/world/ExtendedBlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/BufferBuilder;Lnet/minecraft/fluid/FluidState;)Z"))
    
    
    private boolean renderFluid(FluidRenderer fluidRenderer, ExtendedBlockView blockAccess, BlockPos blockPosIn, BufferBuilder bufferBuilderIn, FluidState fluidStateIn)
    {
        if(LoadingConfig.INSTANCE.enableFluidStats)
            return PipelineHooks.renderFluidDebug(fluidRenderer, blockAccess, fluidStateIn, blockPosIn, bufferBuilderIn);
        else
            return PipelineHooks.renderFluid(fluidRenderer, blockAccess, fluidStateIn, blockPosIn, bufferBuilderIn);
    }
}
