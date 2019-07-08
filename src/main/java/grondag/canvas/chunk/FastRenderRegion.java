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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Function;

import javax.annotation.Nullable;

import grondag.canvas.chunk.ChunkPaletteCopier.PaletteCopy;
import grondag.canvas.light.AoLuminanceFix;
import grondag.fermion.world.PackedBlockPos;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;

public class FastRenderRegion implements RenderAttachedBlockView {
    private static final ArrayBlockingQueue<FastRenderRegion> POOL = new ArrayBlockingQueue<>(256); 

    public static FastRenderRegion claim() {
        final FastRenderRegion result = POOL.poll();
        return result == null ? new FastRenderRegion() : result;
    }

    private static void release(FastRenderRegion region) {
        POOL.offer(region);
    }
    
    public static void forceReload() {
    	// ensure current AoFix rule or other config-dependent lambdas are used
    	POOL.clear();
    }
    
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
    public final Long2IntOpenHashMap brightnessCache;
    public final Long2FloatOpenHashMap aoLevelCache;
    private final AoLuminanceFix aoFix = AoLuminanceFix.effective();
    
    private World world;
    private WorldChunk[][] chunks;
    private int chunkXOffset;
    private int chunkZOffset;
    
    private int secBaseX;
    private int secBaseY;
    private int secBaseZ;
    
    private Function<BlockPos, Object> renderFunc;

    // larger than it needs to be to speed up indexing
    public final PaletteCopy[] sectionCopies = new PaletteCopy[64];

    private FastRenderRegion() {
        brightnessCache = new Long2IntOpenHashMap(65536);
        brightnessCache.defaultReturnValue(Integer.MAX_VALUE);
        aoLevelCache = new Long2FloatOpenHashMap(65536);
        aoLevelCache.defaultReturnValue(Float.MAX_VALUE);
    }
    
    public FastRenderRegion prepare(World world, int cxOff, int czOff, WorldChunk[][] chunks, BlockPos posFrom, Function<BlockPos, Object> renderFunc) {
        this.world = world;
        this.chunks = chunks;
        this.chunkXOffset = cxOff;
        this.chunkZOffset = czOff;
        this.renderFunc = renderFunc;
        secBaseX = posFrom.getX() >> 4;
        secBaseY = posFrom.getY() >> 4;
        secBaseZ = posFrom.getZ() >> 4;
        brightnessCache.clear();
        aoLevelCache.clear();
        
        for(int x = 0; x < 3; x++) {
            for(int z = 0; z < 3; z++) {
                for(int y = 0; y < 3; y++) {
                    sectionCopies[x | (y << 2) | (z << 4)] = ChunkPaletteCopier.captureCopy(chunks[x][z], y + secBaseY);
                }
            }
        }

        return this;
    }
    
    public BlockState getBlockState(int x, int y, int z) {
        return sectionCopies[secIndex(x, y, z)].apply(secBlockIndex(x, y, z));
    }

    private int secBlockIndex(int x, int y, int z) {
        return (x & 0xF) | ((y & 0xF) << 8) | ((z & 0xF) << 4);
    }

    private int secIndex(int x, int y, int z) {
        int bx = (x >> 4) - secBaseX;
        int by = (y >> 4) - secBaseY;
        int bz = (z >> 4) - secBaseZ;
        return bx | (by << 2) | (bz << 4);
    }

    public void release() {
        for(PaletteCopy c : sectionCopies) {
            if(c != null) c.release();
        }
        release(this);
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos blockPos_1) {
        return this.getBlockEntity(blockPos_1, WorldChunk.CreationType.IMMEDIATE);
    }

    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos, WorldChunk.CreationType creationType) {
        int int_1 = (pos.getX() >> 4) - this.chunkXOffset;
        int int_2 = (pos.getZ() >> 4) - this.chunkZOffset;
        return this.chunks[int_1][int_2].getBlockEntity(pos, creationType);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ()).getFluidState();
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        int int_1 = (pos.getX() >> 4) - this.chunkXOffset;
        int int_2 = (pos.getZ() >> 4) - this.chunkZOffset;
        return this.chunks[int_1][int_2].getBiome(pos);
    }

    @Override
    public int getLightLevel(LightType type, BlockPos pos) {
        return this.world.getLightLevel(type, pos);
    }

    @Override
    public Object getBlockEntityRenderAttachment(BlockPos pos) {
        return renderFunc.apply(pos);
    }
    
    public int cachedBrightness(BlockPos pos) {
        long key = PackedBlockPos.pack(pos);
        int result = brightnessCache.get(key);
        if (result == Integer.MAX_VALUE) {
            result = getBlockState(pos).getBlockBrightness(this, pos);
            brightnessCache.put(key, result);
        }
        return result;
    }
    
    public int directBrightness(BlockPos pos) {
        return getBlockState(pos).getBlockBrightness(this, pos);
    }
    
    public float cachedAoLevel(BlockPos pos) {
        long key = PackedBlockPos.pack(pos);
        float result = aoLevelCache.get(key);
        if (result == Float.MAX_VALUE) {
            result = aoFix.apply(this, pos);
            aoLevelCache.put(key, result);
        }
        return result;
    }
}
