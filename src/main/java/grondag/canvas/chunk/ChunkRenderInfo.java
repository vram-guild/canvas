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
import grondag.canvas.apiimpl.util.SafeWorldViewExt;
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
import net.minecraft.world.BlockView;
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
        if(Configurator.enableSmoothLightmaps) {
            computeSmoothedBrightness(chunkOrigin);
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

    /**
     * Cached values for
     * {@link BlockState#getBlockBrightness(ExtendedBlockView, BlockPos)}. See also
     * the comments for {@link #brightnessCache}.
     */
    public int cachedBrightness(BlockState blockState, BlockPos pos) {
        long key = PackedBlockPos.pack(pos);
        int result = brightnessCache.get(key);
        if (result == Integer.MAX_VALUE) {
            result = blockState.getBlockBrightness(blockView, pos);
            brightnessCache.put(key, result);
        }
        return result;
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
    
    private static final int POS_COUNT = 16 * 16 * 16 * 27;
    private static final int OPAQUE = -1;
    
    private static class Helper {
        private final BlockPos.Mutable smoothPos = new BlockPos.Mutable();
        private final float a[] = new float[POS_COUNT];
        private final float b[] = new float[POS_COUNT];
        private final float c[] = new float[POS_COUNT];
    }
    
    private static final ThreadLocal<Helper> helpers = ThreadLocal.withInitial(Helper::new);
    
    private void computeSmoothedBrightness(BlockPos chunkOrigin) {
        final Helper help = helpers.get();
        final BlockPos.Mutable smoothPos = help.smoothPos;
        float[] sky = help.a;
        float[] block = help.b;
        
        final int minX = chunkOrigin.getX() - 16;
        final int minY = chunkOrigin.getY() - 16;
        final int minZ = chunkOrigin.getZ() - 16;
        
        BlockView view = ((SafeWorldViewExt)blockView).canvas_worldHack();
        
        for(int x = 0; x < 48; x++) {
            for(int y = 0; y < 48; y++) {
                for(int z = 0; z < 48; z++) {
                    smoothPos.set(x + minX, y + minY, z + minZ);
//                    final long packedPos = PackedBlockPos.pack(smoothPos);
                    final int i = index(x, y, z);
                    BlockState state = view.getBlockState(smoothPos);
                    final int packedLight = state.getBlockBrightness(blockView, smoothPos);
                    
                    //PERF: still needed for Ao calc?
//                    brightnessCache.put(packedPos, packedLight);
                    
                    final int subtracted = (short) state.getLightSubtracted(blockView, smoothPos);
//                    subtractedCache.put(packedPos, (short) subtracted);
                    
                    if(subtracted > 0) {
                        block[i] = OPAQUE;
                        sky[i] = OPAQUE;
                    } else if(packedLight == 0) {
                        block[i] = 0;
                        sky[i] = 0;
                    } else {
                        block[i] = (packedLight & 0xFF) / 16f;
                        sky[i] = ((packedLight >>> 16) & 0xFF) / 16f;
                    }
                }
            }
        }
        
        float[] work = help.c;
//        smooth(4, block, work);
//        smooth(3, work, block);
        smooth(2, block, work);
        smooth(1, work, block);
        
//        float[] swap = block;
//        block = work;
//        work = swap;
        
//        smooth(4, sky, work);
//        smooth(3, work, sky);
        smooth(2, sky, work);
        smooth(1, work, sky);
        
        final Long2IntOpenHashMap cache = this.brightnessCache;
        for(int x = 15; x < 33; x++) {
            for(int y = 15; y < 33; y++) {
                for(int z = 15; z < 33; z++) {
                    final long packedPos = PackedBlockPos.pack(x + minX, y + minY, z + minZ);
                    final int i = index(x, y, z);
                    
                    final int b = Math.round(Math.max(0, block[i]) * 16 * 1.2f);
                    final int k = Math.round(Math.max(0, sky[i]) * 16 * 1.2f);
                    cache.put(packedPos, (Math.min(b, 240) & 0b11111100) | ((Math.min(k, 240) & 0b11111100)  << 16));
                }
            }
        }
    }
    
    private static int index(int x, int y, int z) {
        return x + y * 48 + z * 2304;
    }
    
    private static final float INNER_DIST = 0.44198f;
    private static final float OUTER_DIST = (1.0f - INNER_DIST) / 2f;
    private static final float CENTER = INNER_DIST * INNER_DIST * INNER_DIST;
    private static final float SIDE = INNER_DIST * INNER_DIST * OUTER_DIST;
    private static final float NEAR_CORNER = INNER_DIST * OUTER_DIST * OUTER_DIST;
    private static final float FAR_CORNER = OUTER_DIST * OUTER_DIST * OUTER_DIST;
    
    private void smooth(int margin, float[] src, float[] dest) {
        final int base = 16 - margin;
        final int limit = 32 + margin;
        for(int x = base; x < limit; x++) {
            for(int y = base; y < limit; y++) {
                for(int z = base; z < limit; z++) {
                    int i = index(x, y, z);
                    
                    float b = src[i];
                    if(b == OPAQUE) {
                        dest[i] = OPAQUE;
                        continue;
                    }
                    
                    float minWeight = 0;
                    float min = b;
                    float l = b * CENTER;
                    
                    // SIDES
                    
                    b = src[index(x + 1, y, z)];
                    if(b == OPAQUE) {
                        minWeight += SIDE;
                    } else {
                        min = nzMin(min, b);
                        l += b * SIDE;
                    }
                    
                    b = src[index(x - 1, y, z)];
                    if(b == OPAQUE) {
                        minWeight += SIDE;
                    } else {
                        min = nzMin(min, b);
                        l += b * SIDE;
                    }
                    
                    b = src[index(x, y + 1, z)];
                    if(b == OPAQUE) {
                        minWeight += SIDE;
                    } else {
                        min = nzMin(min, b);
                        l += b * SIDE;
                    }
                    
                    b = src[index(x, y - 1, z)];
                    if(b == OPAQUE) {
                        minWeight += SIDE;
                    } else {
                        min = nzMin(min, b);
                        l += b * SIDE;
                    }
                    
                    b = src[index(x, y, z + 1)];
                    if(b == OPAQUE) {
                        minWeight += SIDE;
                    } else {
                        min = nzMin(min, b);
                        l += b * SIDE;
                    }
                    
                    b = src[index(x, y, z - 1)];
                    if(b == OPAQUE) {
                        minWeight += SIDE;
                    } else {
                        min = nzMin(min, b);
                        l += b * SIDE;
                    }
                    
                    // NEAR CORNERS - X
                    
                    b = src[index(x, y - 1, z - 1)];
                    if(b == OPAQUE) {
                        minWeight += NEAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * NEAR_CORNER;
                    }
                    
                    b = src[index(x, y - 1, z + 1)];
                    if(b == OPAQUE) {
                        minWeight += NEAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * NEAR_CORNER;
                    }
                    
                    b = src[index(x, y + 1, z - 1)];
                    if(b == OPAQUE) {
                        minWeight += NEAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * NEAR_CORNER;
                    }
                    
                    b = src[index(x, y + 1, z + 1)];
                    if(b == OPAQUE) {
                        minWeight += NEAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * NEAR_CORNER;
                    }
                    
                    // NEAR CORNERS - Y
                    
                    b = src[index(x - 1, y, z - 1)];
                    if(b == OPAQUE) {
                        minWeight += NEAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * NEAR_CORNER;
                    }
                    
                    b = src[index(x - 1, y, z + 1)];
                    if(b == OPAQUE) {
                        minWeight += NEAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * NEAR_CORNER;
                    }
                    
                    b = src[index(x + 1, y, z - 1)];
                    if(b == OPAQUE) {
                        minWeight += NEAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * NEAR_CORNER;
                    }
                    
                    b = src[index(x + 1, y, z + 1)];
                    if(b == OPAQUE) {
                        minWeight += NEAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * NEAR_CORNER;
                    }
                    
                    // NEAR CORNERS - Z
                    
                    b = src[index(x - 1, y - 1, z)];
                    if(b == OPAQUE) {
                        minWeight += NEAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * NEAR_CORNER;
                    }
                    
                    b = src[index(x + 1, y - 1, z)];
                    if(b == OPAQUE) {
                        minWeight += NEAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * NEAR_CORNER;
                    }
                    
                    b = src[index(x - 1, y + 1, z)];
                    if(b == OPAQUE) {
                        minWeight += NEAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * NEAR_CORNER;
                    }
                    
                    b = src[index(x + 1, y + 1, z)];
                    if(b == OPAQUE) {
                        minWeight += NEAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * NEAR_CORNER;
                    }
                    
                    // FAR CORNERS
                    
                    b = src[index(x - 1, y - 1, z - 1)];
                    if(b == OPAQUE) {
                        minWeight += FAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * FAR_CORNER;
                    }
                    
                    b = src[index(x - 1, y - 1, z + 1)];
                    if(b == OPAQUE) {
                        minWeight += FAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * FAR_CORNER;
                    }
                    
                    b = src[index(x - 1, y + 1, z - 1)];
                    if(b == OPAQUE) {
                        minWeight += FAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * FAR_CORNER;
                    }
                    
                    b = src[index(x - 1, y + 1, z + 1)];
                    if(b == OPAQUE) {
                        minWeight += FAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * FAR_CORNER;
                    }
                    
                    b = src[index(x + 1, y - 1, z - 1)];
                    if(b == OPAQUE) {
                        minWeight += FAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * FAR_CORNER;
                    }
                    
                    b = src[index(x + 1, y - 1, z + 1)];
                    if(b == OPAQUE) {
                        minWeight += FAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * FAR_CORNER;
                    }
                    
                    b = src[index(x + 1, y + 1, z - 1)];
                    if(b == OPAQUE) {
                        minWeight += FAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * FAR_CORNER;
                    }
                    
                    b = src[index(x + 1, y + 1, z + 1)];
                    if(b == OPAQUE) {
                        minWeight += FAR_CORNER;
                    } else {
                        min = nzMin(min, b);
                        l += b * FAR_CORNER;
                    }
                    
                    dest[i] = l + minWeight * min;
                }
            }
        }
    }
    
    static float nzMin(float a, float b) {
        if(a == 0) return b;
        if(b == 0) return a;
        return a < b ? a : b;
    }
}
