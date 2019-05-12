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
import grondag.fermion.world.PackedBlockPos;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.block.Block.OffsetType;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.ChunkRenderTask;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.world.SafeWorldView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ExtendedBlockView;

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
    /**
     * Serves same function as brightness cache in Mojang's AO calculator, with some
     * differences as follows...
     * <p>
     * 
     * 1) Mojang uses Object2Int. This uses Long2Int for performance and to avoid
     * creating new immutable BlockPos references. But will break if someone wants
     * to expand Y limit or world borders. If we want to support that may need to
     * switch or make configurable.
     * <p>
     * 
     * 2) Mojang overrides the map methods to limit the cache to 50 values. However,
     * a render chunk only has 18^3 blocks in it, and the cache is cleared every
     * chunk. For performance and simplicity, we just let map grow to the size of
     * the render chunk.
     * 
     * 3) Mojang only uses the cache for Ao. Here it is used for all brightness
     * lookups, including flat lighting.
     * 
     * 4) The Mojang cache is a separate threadlocal with a threadlocal boolean to
     * enable disable. Cache clearing happens with the disable. There's no use case
     * for us when the cache needs to be disabled (and no apparent case in Mojang's
     * code either) so we simply clear the cache at the start of each new chunk. It
     * is also not a threadlocal because it's held within a threadlocal
     * BlockRenderer.
     */
    private final Long2IntOpenHashMap brightnessCache;
    // currently not used
//    private final Long2ShortOpenHashMap subtractedCache;
    private final Long2FloatOpenHashMap aoLevelCache;
    
    private final BlockRenderInfo blockInfo;
    ChunkRenderTask chunkTask;
    ChunkRenderData chunkData;
    ChunkRenderer chunkRenderer;
    ExtendedBlockView blockView;

    // model offsets for plants, etc.
    private boolean hasOffsets = false;
    private float offsetX = 0;
    private float offsetY = 0;
    private float offsetZ = 0;

    public ChunkRenderInfo(BlockRenderInfo blockInfo) {
        this.blockInfo = blockInfo;
        brightnessCache = new Long2IntOpenHashMap();
        brightnessCache.defaultReturnValue(Integer.MAX_VALUE);
        aoLevelCache = new Long2FloatOpenHashMap();
        aoLevelCache.defaultReturnValue(Float.MAX_VALUE);
//        subtractedCache = new Long2ShortOpenHashMap();
//        subtractedCache.defaultReturnValue(Short.MIN_VALUE);
    }

    public void setBlockView(SafeWorldView blockView) {
        this.blockView = blockView;
    }

    public void setChunkTask(ChunkRenderTask chunkTask) {
        this.chunkTask = chunkTask;
    }

    public void prepare(ChunkRenderer chunkRenderer, BlockPos.Mutable chunkOrigin) {
        this.chunkData = chunkTask.getRenderData();
        this.chunkRenderer = chunkRenderer;
        brightnessCache.clear();
        aoLevelCache.clear();
        if(Configurator.enableLightSmoothing) {
            LightSmoother.computeSmoothedBrightness(chunkOrigin, blockView, brightnessCache);
        }
    }

    public void release() {
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
        long key = PackedBlockPos.pack(pos);
        int result = brightnessCache.get(key);
        if (result == Integer.MAX_VALUE) {
            result = blockView.getBlockState(pos).getBlockBrightness(blockView, pos);
            brightnessCache.put(key, result);
        }
        return result;
    }
    
//    public boolean cachedClearness(BlockPos pos) {
//        long key = PackedBlockPos.pack(pos);
//        short result = subtractedCache.get(key);
//        if (result == Short.MIN_VALUE) {
//            result = (short) blockView.getBlockState(pos).getLightSubtracted(blockView, pos);
//            subtractedCache.put(key, result);
//        }
//        return result == 0;
//    }
    
    public float cachedAoLevel(BlockPos pos) {
        long key = PackedBlockPos.pack(pos);
        float result = aoLevelCache.get(key);
        if (result == Float.MAX_VALUE) {
            result = blockView.getBlockState(pos).getAmbientOcclusionLightLevel(blockView, pos);
            aoLevelCache.put(key, result);
        }
        return result;
    }
}
