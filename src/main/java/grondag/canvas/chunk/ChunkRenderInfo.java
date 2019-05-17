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

/*
 * Copyright (c) 2016, 2017, 2018 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grondag.canvas.chunk;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.BlockRenderInfo;
import grondag.canvas.light.LightSmoother;
import net.minecraft.block.Block.OffsetType;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.ChunkRenderTask;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

//PERF: many opportunities here

/**
 * Holds, manages and provides access to the chunk-related state needed by
 * fallback and mesh consumers during terrain rendering.
 * <p>
 * 
 * Exception: per-block position offsets are tracked here so they can be applied
 * together with chunk offsets.
 */
public class ChunkRenderInfo {
    private final BlockRenderInfo blockInfo;
    ChunkRenderTask chunkTask;
    ChunkRenderData chunkData;
    ChunkRenderer chunkRenderer;
    FastRenderRegion blockView;

    // model offsets for plants, etc.
    private boolean hasOffsets = false;
    private float offsetX = 0;
    private float offsetY = 0;
    private float offsetZ = 0;

    public ChunkRenderInfo(BlockRenderInfo blockInfo) {
        this.blockInfo = blockInfo;
    }

    public void setBlockView(FastRenderRegion blockView) {
        this.blockView = blockView;
    }

    public void setChunkTask(ChunkRenderTask chunkTask) {
        this.chunkTask = chunkTask;
    }

    public void prepare(ChunkRenderer chunkRenderer, BlockPos.Mutable chunkOrigin) {
        this.chunkData = chunkTask.getRenderData();
        this.chunkRenderer = chunkRenderer;
        if(Configurator.enableLightSmoothing) {
            LightSmoother.computeSmoothedBrightness(chunkOrigin, blockView, blockView.brightnessCache);
        }
    }

    public void release() {
        blockView.release();
        blockView = null;
        chunkData = null;
        chunkTask = null;
        chunkRenderer = null;
    }

    public void beginBlock() {
        final BlockState blockState = blockInfo.blockState;
        final BlockPos blockPos = blockInfo.blockPos;

        if (blockState.getBlock().getOffsetType() == OffsetType.NONE) {
            hasOffsets = false;
        } else {
            hasOffsets = true;
            Vec3d offset = blockState.getOffsetPos(blockInfo.blockView, blockPos);
            offsetX = (float) offset.x;
            offsetY = (float) offset.y;
            offsetZ = (float) offset.z;
        }
    }

    /**
     * Applies position offset for chunk and, if present, block random offset.
     */
    public void applyOffsets(MutableQuadViewImpl q) {
        if(hasOffsets) {
            for (int i = 0; i < 4; i++) {
                q.pos(i, q.x(i) + offsetX, q.y(i) + offsetY, q.z(i) + offsetZ);
            }
        }
    }
    
    public int cachedBrightness(BlockPos pos) {
        return blockView.cachedBrightness(pos);
    }
    
    public float cachedAoLevel(BlockPos pos) {
        return blockView.cachedAoLevel(pos);
    }
}
