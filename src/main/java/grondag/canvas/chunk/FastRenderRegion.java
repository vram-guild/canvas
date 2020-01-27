/*******************************************************************************
 * Copyright 2019, 2020 grondag
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

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.level.ColorResolver;

import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.chunk.ChunkPaletteCopier.PaletteCopy;
import grondag.canvas.light.AoLuminanceFix;

public class FastRenderRegion implements RenderAttachedBlockView {
	private static final ArrayBlockingQueue<FastRenderRegion> POOL = new ArrayBlockingQueue<>(256);

	public static FastRenderRegion claim(World world, BlockPos origin) {
		final FastRenderRegion result = POOL.poll();
		return (result == null ? new FastRenderRegion() : result).prepare(world, origin);
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
	private final WorldChunk[][] chunks = new WorldChunk[18][18];
	private int chunkXOffset;
	private int chunkZOffset;

	private int secBaseX;
	private int secBaseY;
	private int secBaseZ;

	// larger than it needs to be to speed up indexing
	private final PaletteCopy[] sectionCopies = new PaletteCopy[64];

	public TerrainRenderContext terrainContext;

	private FastRenderRegion() {
		brightnessCache = new Long2IntOpenHashMap(65536);
		brightnessCache.defaultReturnValue(Integer.MAX_VALUE);
		aoLevelCache = new Long2FloatOpenHashMap(65536);
		aoLevelCache.defaultReturnValue(Float.MAX_VALUE);
	}

	private FastRenderRegion prepare(World world, BlockPos origin) {
		this.world = world;

		final int ox = origin.getX();
		final int oy = origin.getY();
		final int oz = origin.getZ();

		chunkXOffset = ox >> 4;
		chunkZOffset = oz >> 4;
		secBaseX = (ox - 1) >> 4;
		secBaseY = (oy - 1) >> 4;
		secBaseZ = (oz - 1) >> 4;

		brightnessCache.clear();
		aoLevelCache.clear();

		boolean isEmpty = true;

		for(int x = 0; x < 3; x++) {
			for(int z = 0; z < 3; z++) {
				for(int y = 0; y < 3; y++) {
					final WorldChunk chunk = world.getChunk(secBaseX + x, secBaseZ + z);
					chunks[x][z] = chunk;

					final PaletteCopy pCopy = ChunkPaletteCopier.captureCopy(chunks[x][z], y + secBaseY);
					sectionCopies[x | (y << 2) | (z << 4)] = pCopy;

					if(isEmpty && pCopy != ChunkPaletteCopier.AIR_COPY) {
						isEmpty = false;
					}
				}
			}
		}

		return isEmpty ? null : this;
	}

	public BlockState getBlockState(int x, int y, int z) {
		return sectionCopies[secIndex(x, y, z)].apply(secBlockIndex(x, y, z));
	}

	private int secBlockIndex(int x, int y, int z) {
		return (x & 0xF) | ((y & 0xF) << 8) | ((z & 0xF) << 4);
	}

	private int secIndex(int x, int y, int z) {
		final int bx = (x >> 4) - secBaseX;
		final int by = (y >> 4) - secBaseY;
		final int bz = (z >> 4) - secBaseZ;
		return bx | (by << 2) | (bz << 4);
	}

	public void release() {
		for(final PaletteCopy c : sectionCopies) {
			if(c != null) {
				c.release();
			}
		}

		for(int x = 0; x < 3; x++) {
			for(int z = 0; z < 3; z++) {
				chunks[x][z] = null;
			}
		}

		terrainContext = null;

		release(this);
	}

	@Override
	@Nullable
	public BlockEntity getBlockEntity(BlockPos pos) {
		return this.getBlockEntity(pos, WorldChunk.CreationType.IMMEDIATE);
	}

	@Nullable
	public BlockEntity getBlockEntity(BlockPos pos, WorldChunk.CreationType creationType) {
		final int i = (pos.getX() >> 4) - chunkXOffset;
		final int j = (pos.getZ() >> 4) - chunkZOffset;
		return chunks[i][j].getBlockEntity(pos, creationType);
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
	public int getLightLevel(LightType type, BlockPos pos) {
		return world.getLightLevel(type, pos);
	}

	@Override
	public Object getBlockEntityRenderAttachment(BlockPos pos) {
		final BlockEntity be = getBlockEntity(pos);

		return be == null ? null : ((RenderAttachmentBlockEntity) be).getRenderAttachmentData();
	}

	public int cachedBrightness(BlockPos pos) {
		final long key = pos.asLong();
		int result = brightnessCache.get(key);

		if (result == Integer.MAX_VALUE) {
			result = WorldRenderer.getLightmapCoordinates(world, getBlockState(pos), pos);
			brightnessCache.put(key, result);
		}

		return result;
	}

	public int directBrightness(BlockPos pos) {
		return WorldRenderer.getLightmapCoordinates(world, getBlockState(pos), pos);
	}

	public float cachedAoLevel(BlockPos pos) {
		final long key = pos.asLong();
		float result = aoLevelCache.get(key);

		if (result == Float.MAX_VALUE) {
			result = aoFix.apply(this, pos);
			aoLevelCache.put(key, result);
		}

		return result;
	}

	@Override
	public LightingProvider getLightingProvider() {
		return world.getLightingProvider();
	}

	@Override
	public int getColor(BlockPos blockPos, ColorResolver colorResolver) {
		return world.getColor(blockPos, colorResolver);
	}
}
