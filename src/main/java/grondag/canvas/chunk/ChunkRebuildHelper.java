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

import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.buffer.packing.FluidBufferBuilder;
import grondag.canvas.buffer.packing.VertexCollector;
import grondag.canvas.buffer.packing.VertexCollectorList;
import grondag.canvas.chunk.occlusion.ChunkOcclusionBuilderAccessHelper.ChunkOcclusionGraphBuilderExt;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.chunk.ChunkOcclusionGraphBuilder;
import net.minecraft.util.math.BlockPos;

public class ChunkRebuildHelper {
    public static final int BLOCK_RENDER_LAYER_COUNT = BlockRenderLayer.values().length;
    public static final boolean[] EMPTY_RENDER_LAYER_FLAGS = new boolean[BLOCK_RENDER_LAYER_COUNT];

    public final BlockPos.Mutable searchPos = new BlockPos.Mutable();
    public final HashSet<BlockEntity> tileEntities = Sets.newHashSet();
    public final Set<BlockEntity> tileEntitiesToAdd = Sets.newHashSet();
    public final Set<BlockEntity> tileEntitiesToRemove = Sets.newHashSet();
    public final ChunkOcclusionGraphBuilder visGraph = new ChunkOcclusionGraphBuilder();
    public final Random random = new Random();
    public final FluidBufferBuilder fluidBuilder = new FluidBufferBuilder();
    public final VertexCollectorList solidCollector = new VertexCollectorList();
    public final VertexCollectorList translucentCollector = new VertexCollectorList();
    
    public VertexCollectorList getCollector(BlockRenderLayer layer) {
        return layer == BlockRenderLayer.TRANSLUCENT ? translucentCollector : solidCollector;
    }
    
    public VertexCollector collectorForMaterial(RenderMaterialImpl.Value mat) {
        return getCollector(mat.renderLayer).get(mat);
    }

    public void clear() {
        tileEntities.clear();
        tileEntitiesToAdd.clear();
        tileEntitiesToRemove.clear();
        ((ChunkOcclusionGraphBuilderExt)visGraph).canvas_clear();
    }

    public void prepareCollectors(int x, int y, int z) {
        solidCollector.clear();
        solidCollector.setRelativeRenderOrigin(x, y, z);  
        translucentCollector.clear();
        translucentCollector.setRelativeRenderOrigin(x, y, z); 
    }
}
