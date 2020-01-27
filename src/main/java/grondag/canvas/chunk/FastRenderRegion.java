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

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

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

	public final Long2IntOpenHashMap brightnessCache;
	public final Long2FloatOpenHashMap aoLevelCache;
	public final Long2ObjectOpenHashMap<BlockState> stateCache;
	private final AoLuminanceFix aoFix = AoLuminanceFix.effective();

	private static final int STATE_COUNT = 18 * 18 * 18;
	private final BlockState[] states = new BlockState[STATE_COUNT];

	// larger than they need to be to speed up indexing
	private final WorldChunk[] chunks = new WorldChunk[16];
	private final PaletteCopy[] sectionCopies = new PaletteCopy[64];

	private int chunkXOrigin;
	private int chunkZOrigin;

	private int blockBaseX;
	private int blockBaseY;
	private int blockBaseZ;

	private int chunkBaseX;
	private int chunkBaseY;
	private int chunkBaseZ;

	private World world;
	public TerrainRenderContext terrainContext;

	private FastRenderRegion() {
		brightnessCache = new Long2IntOpenHashMap(65536);
		brightnessCache.defaultReturnValue(Integer.MAX_VALUE);
		aoLevelCache = new Long2FloatOpenHashMap(65536);
		aoLevelCache.defaultReturnValue(Float.MAX_VALUE);
		stateCache = new Long2ObjectOpenHashMap<>(65536);
	}

	// PERF: consider morton numbering for better locality
	private int stateIndex(int x, int y, int z) {
		return x + y * 18 + z * 324;
	}

	public void prepareForUse() {
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				for(int k = 0; k < 3; k++) {
					final PaletteCopy pc = sectionCopies[i | (j << 2) | (k << 4)];

					final int minX = i == 0 ? 15 : 0;
					final int minY = j == 0 ? 15 : 0;
					final int minZ = k == 0 ? 15 : 0;

					final int maxX = i == 2 ? 0 : 15;
					final int maxY = j == 2 ? 0 : 15;
					final int maxZ = k == 2 ? 0 : 15;

					final int bx = i == 0 ? -15 : i == 1 ? 1 : 17;
					final int by = j == 0 ? -15 : j == 1 ? 1 : 17;
					final int bz = k == 0 ? -15 : k == 1 ? 1 : 17;

					for(int x = minX; x <= maxX; x++) {
						for(int y = minY; y <= maxY; y++) {
							for(int z = minZ; z <= maxZ; z++) {
								states[stateIndex(bx + x, by + y, bz + z)] = pc.apply(x | (y << 8) | (z << 4));
							}
						}
					}
				}
			}
		}
	}

	private FastRenderRegion prepare(World world, BlockPos origin) {
		this.world = world;

		final int ox = origin.getX();
		final int oy = origin.getY();
		final int oz = origin.getZ();

		// eclipse save action formatting shits the bed here for no apparent reason
		chunkXOrigin = ox >> 4;
								chunkZOrigin = oz >> 4;

								blockBaseX = ox - 1;
								blockBaseY = oy - 1;
								blockBaseZ = oz - 1;

								chunkBaseX = blockBaseX >> 4;
							chunkBaseY = blockBaseY >> 4;
					chunkBaseZ = blockBaseZ >> 4;

					brightnessCache.clear();
					aoLevelCache.clear();
					stateCache.clear();

					boolean isEmpty = true;

					for(int x = 0; x < 3; x++) {
						for(int z = 0; z < 3; z++) {
							for(int y = 0; y < 3; y++) {
								final WorldChunk chunk = world.getChunk(chunkBaseX + x, chunkBaseZ + z);
								chunks[x | (z << 2)] = chunk;

								final PaletteCopy pCopy = ChunkPaletteCopier.captureCopy(chunk, y + chunkBaseY);
								sectionCopies[x | (y << 2) | (z << 4)] = pCopy;

								if(isEmpty && pCopy != ChunkPaletteCopier.AIR_COPY) {
									isEmpty = false;
								}
							}
						}
					}

					return isEmpty ? null : this;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		final int x = pos.getX();
		final int y = pos.getY();
		final int z = pos.getZ();

		final int i = stateIndex(x - blockBaseX, y - blockBaseY, z - blockBaseZ);

		if(i < 0 || i >= STATE_COUNT) {
			return world.getBlockState(pos);
		}

		return states[i];
	}

	public void release() {
		for(final PaletteCopy c : sectionCopies) {
			if(c != null) {
				c.release();
			}
		}

		for(int x = 0; x < 3; x++) {
			for(int z = 0; z < 3; z++) {
				chunks[x | (z << 2)] = null;
			}
		}

		Arrays.fill(states, null);

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
		final int i = (pos.getX() >> 4) - chunkXOrigin + 1;
		final int j = (pos.getZ() >> 4) - chunkZOrigin + 1;
		return chunks[i | (j << 2)].getBlockEntity(pos, creationType);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getBlockState(pos).getFluidState();
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
