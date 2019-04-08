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

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.canvas.hooks.ChunkRebuildHelper;
import grondag.canvas.mixinext.ChunkRenderDataExt;
import grondag.canvas.mixinext.ChunkVisibility;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.chunk.ChunkOcclusionGraph;
import net.minecraft.client.render.chunk.ChunkRenderData;

@Mixin(ChunkRenderData.class)
public abstract class MixinChunkRenderData implements ChunkRenderDataExt {
    @Shadow
    private boolean[] field_4450; // has content
    @Shadow
    private boolean[] initialized;
    @Shadow
    private boolean empty;
    @Shadow
    private List<BlockEntity> blockEntities;
    @Shadow
    private ChunkOcclusionGraph field_4455;
    @Shadow
    private BufferBuilder.State bufferState;

    @Shadow
    protected abstract void method_3643(BlockRenderLayer blockRenderLayer);
    
    @Override
    public void canvas_setNonEmpty(BlockRenderLayer blockRenderLayer) {
        method_3643(blockRenderLayer);
    }

    @Override
    public void canvas_clear() {
        empty = true;
        System.arraycopy(ChunkRebuildHelper.EMPTY_RENDER_LAYER_FLAGS, 0, field_4450, 0,
                ChunkRebuildHelper.BLOCK_RENDER_LAYER_COUNT);
        System.arraycopy(ChunkRebuildHelper.EMPTY_RENDER_LAYER_FLAGS, 0, initialized, 0,
                ChunkRebuildHelper.BLOCK_RENDER_LAYER_COUNT);
        field_4455.fill(false); // set all false
        ((ChunkVisibility) field_4455).canvas_visibilityData(null);
        bufferState = null;
        blockEntities.clear();
    }

    /**
     * When mod is enabled, cutout layers are packed into solid layer, but the chunk
     * render dispatcher doesn't know this and sets flags in the compiled chunk as
     * if the cutout buffers were populated. We use this hook to correct that so
     * that uploader and rendering work in subsequent operations.
     * <p>
     * 
     * Called from the rebuildChunk method in ChunkRenderer, via a redirect on the
     * call to
     * {@link CompiledChunk#setVisibility(net.minecraft.client.renderer.chunk.SetVisibility)}
     * which is reliably called after the chunks are built in render chunk.
     * <p>
     */
    @Override
    public void canvas_mergeRenderLayers() {
        mergeLayerFlags(initialized);
        mergeLayerFlags(field_4450);
    }

    private static void mergeLayerFlags(boolean[] layerFlags) {
        layerFlags[0] = layerFlags[0] || layerFlags[1] || layerFlags[2];
        layerFlags[1] = false;
        layerFlags[2] = false;
    }

    @Override
    public ChunkVisibility canvas_chunkVisibility() {
        return (ChunkVisibility) field_4455;
    }
}
