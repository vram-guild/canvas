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

package grondag.canvas.chunk;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Sets;

import grondag.canvas.core.FluidBufferBuilder;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.chunk.BlockLayeredBufferBuilder;
import net.minecraft.client.render.chunk.ChunkOcclusionGraphBuilder;
import net.minecraft.util.math.BlockPos;

public class ChunkRebuildHelper {
    public static final int BLOCK_RENDER_LAYER_COUNT = BlockRenderLayer.values().length;
    public static final boolean[] EMPTY_RENDER_LAYER_FLAGS = new boolean[BLOCK_RENDER_LAYER_COUNT];

    public final BlockRenderLayer[] layers = BlockRenderLayer.values().clone();
    public final boolean[] layerFlags = new boolean[BLOCK_RENDER_LAYER_COUNT];
    public final BlockPos.Mutable searchPos = new BlockPos.Mutable();
    public final HashSet<BlockEntity> tileEntities = Sets.newHashSet();
    public final Set<BlockEntity> tileEntitiesToAdd = Sets.newHashSet();
    public final Set<BlockEntity> tileEntitiesToRemove = Sets.newHashSet();
    public final ChunkOcclusionGraphBuilder visGraph = new ChunkOcclusionGraphBuilder();
    public final Random random = new Random();
    private final BufferBuilder[] builders = new BufferBuilder[BLOCK_RENDER_LAYER_COUNT];
    public final FluidBufferBuilder fluidBuilder = new FluidBufferBuilder();
    
    public BufferBuilder[] builders(BlockLayeredBufferBuilder regionCache) {
        builders[BlockRenderLayer.SOLID.ordinal()] = regionCache.get(BlockRenderLayer.SOLID);
        builders[BlockRenderLayer.CUTOUT.ordinal()] = regionCache.get(BlockRenderLayer.CUTOUT);
        builders[BlockRenderLayer.MIPPED_CUTOUT.ordinal()] = regionCache.get(BlockRenderLayer.MIPPED_CUTOUT);
        builders[BlockRenderLayer.TRANSLUCENT.ordinal()] = regionCache.get(BlockRenderLayer.TRANSLUCENT);
        return builders;
    }

    public void clear() {
        System.arraycopy(EMPTY_RENDER_LAYER_FLAGS, 0, layerFlags, 0, BLOCK_RENDER_LAYER_COUNT);
        tileEntities.clear();
        tileEntitiesToAdd.clear();
        tileEntitiesToRemove.clear();

        // PERF: put these back when re-enable faster visibility
//        visGraph..bitSet.clear();
//        visGraph.empty = 4096;
    }
}
