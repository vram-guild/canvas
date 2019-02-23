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

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.canvas.hooks.ISetVisibility;
import grondag.canvas.hooks.ChunkRebuildHelper;
import grondag.canvas.mixin.extension.ChunkRenderDataExt;
import net.minecraft.class_854;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.chunk.ChunkRenderData;

@Mixin(ChunkRenderData.class)
public abstract class MixinChunkRenderData implements ChunkRenderDataExt
{
    @Shadow private boolean[] hasContent;
    @Shadow private boolean[] isInitialized;
    @Shadow private boolean empty;
    @Shadow private List<BlockEntity> blockEntities;
    @Shadow private class_854 field_4455;
    @Shadow private BufferBuilder.State bufferState;
    
    @Override
    @Shadow public abstract void setNonEmpty(BlockRenderLayer blockRenderLayer);
    
    @Override
    public void clear()
    {
        empty = true;
        System.arraycopy(ChunkRebuildHelper.EMPTY_RENDER_LAYER_FLAGS, 0, hasContent, 0, ChunkRebuildHelper.BLOCK_RENDER_LAYER_COUNT);
        System.arraycopy(ChunkRebuildHelper.EMPTY_RENDER_LAYER_FLAGS, 0, isInitialized, 0, ChunkRebuildHelper.BLOCK_RENDER_LAYER_COUNT);
        field_4455.method_3694(false); // set all false
        ((ISetVisibility)field_4455).setVisibilityData(null);
        bufferState = null;
        blockEntities.clear();
    }
}
