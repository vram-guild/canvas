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
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.level.ColorResolver;

import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.chunk.ChunkPaletteCopier.PaletteCopy;
import grondag.canvas.perf.ChunkRebuildCounters;

public class FastRenderRegion extends AbstractRenderRegion implements RenderAttachedBlockView {
	private final BlockPos.Mutable searchPos = new BlockPos.Mutable();
	private final Int2ObjectOpenHashMap<Object> renderData = new Int2ObjectOpenHashMap<>();
	public final Int2ObjectOpenHashMap<BlockEntity> blockEntities = new Int2ObjectOpenHashMap<>();

	private final BlockState[] states = new BlockState[TOTAL_CACHE_SIZE];
	private final float[] aoCache = new float[TOTAL_CACHE_SIZE];
	private final int[] lightCache = new int[TOTAL_CACHE_SIZE];

	// larger than needed to speed up indexing
	private final WorldChunk[] chunks = new WorldChunk[16];
	private PaletteCopy mainSectionCopy;

	public TerrainRenderContext terrainContext;

	public void prepareForUse() {
		final PaletteCopy pc = mainSectionCopy;
		mainSectionCopy = null;

		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					states[mainChunkLocalIndex(x, y, z)] = pc.apply(x | (y << 8) | (z << 4));
				}
			}
		}

		pc.release();
	}

	private long start;
	ChunkRebuildCounters counter;

	private FastRenderRegion prepare(World world, BlockPos origin) {
		if(ChunkRebuildCounters.ENABLED) {
			counter = ChunkRebuildCounters.get();
			start = counter.copyCounter.startRun();
		}

		this.world = world;

		originX = origin.getX();
		originY = origin.getY();
		originZ = origin.getZ();

		chunkBaseX = (originX >> 4) - 1;
		chunkBaseY = (originY >> 4) - 1;
		chunkBaseZ = (originZ >> 4) - 1;

		final WorldChunk mainChunk = world.getChunk(chunkBaseX + 1, chunkBaseZ + 1);
		mainSectionCopy = ChunkPaletteCopier.captureCopy(mainChunk, 1 + chunkBaseY);

		final FastRenderRegion result;

		if(mainSectionCopy == ChunkPaletteCopier.AIR_COPY) {
			this.release();
			result = null;
		} else {
			captureBlockEntities(mainChunk);
			chunks[1 | (1 << 2)] = mainChunk;
			chunks[0 | (0 << 2)] = world.getChunk(chunkBaseX + 0, chunkBaseZ + 0);
			chunks[0 | (1 << 2)] = world.getChunk(chunkBaseX + 0, chunkBaseZ + 1);
			chunks[0 | (2 << 2)] = world.getChunk(chunkBaseX + 0, chunkBaseZ + 2);
			chunks[1 | (0 << 2)] = world.getChunk(chunkBaseX + 1, chunkBaseZ + 0);
			chunks[1 | (2 << 2)] = world.getChunk(chunkBaseX + 1, chunkBaseZ + 2);
			chunks[2 | (0 << 2)] = world.getChunk(chunkBaseX + 2, chunkBaseZ + 0);
			chunks[2 | (1 << 2)] = world.getChunk(chunkBaseX + 2, chunkBaseZ + 1);
			chunks[2 | (2 << 2)] = world.getChunk(chunkBaseX + 2, chunkBaseZ + 2);

			captureCorners();
			captureEdges();
			captureFaces();

			System.arraycopy(EMPTY_AO_CACHE, 0, aoCache, 0, TOTAL_CACHE_SIZE);
			System.arraycopy(EMPTY_LIGHT_CACHE, 0, lightCache, 0, TOTAL_CACHE_SIZE);

			result = this;
		}

		if(ChunkRebuildCounters.ENABLED) {
			counter.copyCounter.endRun(start);
			counter.copyCounter.addCount(1);
		}

		return result;
	}

	private void captureBlockEntities(WorldChunk mainChunk) {
		renderData.clear();
		blockEntities.clear();

		for(final Map.Entry<BlockPos, BlockEntity> entry : mainChunk.getBlockEntities().entrySet()) {
			final int key = mainChunkBlockIndex(entry.getKey());
			final BlockEntity be = entry.getValue();
			blockEntities.put(key, be);
			final Object rd = ((RenderAttachmentBlockEntity) be).getRenderAttachmentData();

			if(rd != null) {
				renderData.put(key, rd);
			}
		}
	}

	private void captureFaces() {
		final ChunkSection lowX = getSection(0, 1, 1);
		final ChunkSection highX = getSection(2, 1, 1);
		final ChunkSection lowZ = getSection(1, 1, 0);
		final ChunkSection highZ = getSection(1, 1, 2);
		final ChunkSection lowY = getSection(1, 0, 1);
		final ChunkSection highY = getSection(1, 2, 1);

		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				states[localXfaceIndex(false, i, j)] = lowX == null ? AIR : lowX.getBlockState(15, i, j);
				states[localXfaceIndex(true, i, j)] = highX == null ? AIR : highX.getBlockState(0, i, j);

				states[localZfaceIndex(i, j, false)] = lowZ == null ? AIR : lowZ.getBlockState(i, j, 15);
				states[localZfaceIndex(i, j, true)] = highZ == null ? AIR : highZ.getBlockState(i, j, 0);

				states[localYfaceIndex(i, false, j)] = lowY == null ? AIR : lowY.getBlockState(i, 15, j);
				states[localYfaceIndex(i, true, j)] = highY == null ? AIR : highY.getBlockState(i, 0, j);
			}
		}
	}

	private ChunkSection getSection(int x, int y, int z) {
		// TODO: handle world border

		if ((y == 0 && chunkBaseY < 0) || (y == 2 && chunkBaseY > 13)) {
			return null;
		}

		return chunks[x | (z << 2)].getSectionArray()[chunkBaseY + y];
	}

	private void captureEdges() {
		final ChunkSection aaZ = getSection(0, 0, 1);
		final ChunkSection abZ = getSection(0, 2, 1);
		final ChunkSection baZ = getSection(2, 0, 1);
		final ChunkSection bbZ = getSection(2, 2, 1);

		final ChunkSection aYa = getSection(0, 1, 0);
		final ChunkSection aYb = getSection(0, 1, 2);
		final ChunkSection bYa = getSection(2, 1, 0);
		final ChunkSection bYb = getSection(2, 1, 2);

		final ChunkSection Xaa = getSection(1, 0, 0);
		final ChunkSection Xab = getSection(1, 0, 2);
		final ChunkSection Xba = getSection(1, 2, 0);
		final ChunkSection Xbb = getSection(1, 2, 2);

		for(int i = 0; i < 16; i++) {
			states[localZEdgeIndex(false, false, i)] = aaZ == null ? AIR : aaZ.getBlockState(15, 15, i);
			states[localZEdgeIndex(false, true, i)] = abZ == null ? AIR : abZ.getBlockState(15, 0, i);
			states[localZEdgeIndex(true, false, i)] = baZ == null ? AIR : baZ.getBlockState(0, 15, i);
			states[localZEdgeIndex(true, true, i)] = bbZ == null ? AIR : bbZ.getBlockState(0, 0, i);

			states[localYEdgeIndex(false, i, false)] = aYa == null ? AIR : aYa.getBlockState(15, i, 15);
			states[localYEdgeIndex(false, i, true)] = aYb == null ? AIR : aYb.getBlockState(15, i, 0);
			states[localYEdgeIndex(true, i, false)] = bYa == null ? AIR : bYa.getBlockState(0, i, 15);
			states[localYEdgeIndex(true, i, true)] = bYb == null ? AIR : bYb.getBlockState(0, i, 0);

			states[localXEdgeIndex(i, false, false)] = Xaa == null ? AIR : Xaa.getBlockState(i, 15, 15);
			states[localXEdgeIndex(i, false, true)] = Xab == null ? AIR : Xab.getBlockState(i, 15, 0);
			states[localXEdgeIndex(i, true, false)] = Xba == null ? AIR : Xba.getBlockState(i, 0, 15);
			states[localXEdgeIndex(i, true, true)] = Xbb == null ? AIR : Xbb.getBlockState(i, 0, 0);
		}
	}

	private void captureCorners() {
		states[localCornerIndex(false, false, false)] = captureCornerState(0, 0, 0);
		states[localCornerIndex(false, false, true)] = captureCornerState(0, 0, 2);
		states[localCornerIndex(false, true, false)] = captureCornerState(0, 2, 0);
		states[localCornerIndex(false, true, true)] = captureCornerState(0, 2, 2);

		states[localCornerIndex(true, false, false)] = captureCornerState(2, 0, 0);
		states[localCornerIndex(true, false, true)] = captureCornerState(2, 0, 2);
		states[localCornerIndex(true, true, false)] = captureCornerState(2, 2, 0);
		states[localCornerIndex(true, true, true)] = captureCornerState(2, 2, 2);
	}

	private BlockState captureCornerState(int x, int y, int z) {
		final ChunkSection section = getSection(x, y, z);
		return section == null ? AIR : section.getBlockState(x == 0 ? 15 : 0, y == 0 ? 15 : 0, z == 0 ? 15 : 0);
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		final int i = blockIndex(pos.getX(), pos.getY(), pos.getZ());

		if (i == -1) {
			return world.getBlockState(pos);
		}

		return states[i];
	}

	public BlockState getBlockState(int x, int y, int z) {
		final int i = blockIndex(x, y, z);

		if (i == -1) {
			return world.getBlockState(searchPos.set(x, y, z));
		}

		return states[i];
	}

	/**
	 * Assumes values 0-15
	 */
	public BlockState getLocalBlockState(int x, int y, int z) {
		return states[mainChunkLocalIndex(x, y, z)];
	}

	public void release() {
		if (mainSectionCopy != null) {
			mainSectionCopy.release();
			mainSectionCopy = null;
		}

		for (int x = 0; x < 3; x++) {
			for (int z = 0; z < 3; z++) {
				chunks[x | (z << 2)] = null;
			}
		}

		terrainContext = null;
		blockEntities.clear();
		renderData.clear();

		release(this);
	}

	@Override
	@Nullable
	public BlockEntity getBlockEntity(BlockPos pos) {
		return isInMainChunk(pos) ? blockEntities.get(this.mainChunkBlockIndex(pos)) : world.getBlockEntity(pos);
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
		return renderData.get(mainChunkBlockIndex(pos));
	}

	public int cachedBrightness(int x, int y, int z) {
		final int i = blockIndex(x, y, z);

		if (i == -1) {
			searchPos.set(x, y, z);
			final BlockState state = world.getBlockState(searchPos);
			return WorldRenderer.getLightmapCoordinates(world, state, searchPos);
		}

		int result = lightCache[i];

		if (result == Integer.MAX_VALUE) {
			final BlockState state = states[i];
			result = WorldRenderer.getLightmapCoordinates(world, state, searchPos.set(x, y, z));
			lightCache[i] = result;
		}

		return result;
	}

	public int directBrightness(BlockPos pos) {
		return WorldRenderer.getLightmapCoordinates(world, getBlockState(pos), pos);
	}

	public float cachedAoLevel(int x, int y, int z) {
		final int i = blockIndex(x, y, z);

		if (i == -1) {
			searchPos.set(x, y, z);
			final BlockState state = world.getBlockState(searchPos);
			return state.getLuminance() == 0 ? state.getAmbientOcclusionLightLevel(this, searchPos) : 1F;
		}

		float result = aoCache[i];

		if (result == Float.MAX_VALUE) {
			final BlockState state = states[i];
			result = state.getLuminance() == 0 ? state.getAmbientOcclusionLightLevel(this, searchPos.set(x, y, z)) : 1F;
			aoCache[i] = result;
		}

		return result;
	}

	@Override
	public LightingProvider getLightingProvider() {
		return world.getLightingProvider();
	}

	@Override
	public int getColor(BlockPos blockPos, ColorResolver colorResolver) {
		final int x = blockPos.getX();
		final int z = blockPos.getZ();

		final int result = ChunkColorCache.get(getChunk(x >> 4, z >> 4)).getColor(x, blockPos.getY(), z, colorResolver);

		return result;
	}

	private WorldChunk getChunk(int cx, int cz) {
		final int chunkBaseX = this.chunkBaseX;
		final int chunkBaseZ = this.chunkBaseZ;

		if (cx < chunkBaseX || cx > (chunkBaseZ + 2) || cz < chunkBaseZ || cz > (chunkBaseZ + 2)) {
			return world.getChunk(cx, cz);
		} else {
			return chunks[(cx - chunkBaseX) | ((cz - chunkBaseZ) << 2)];
		}
	}

	private static final BlockState AIR = Blocks.AIR.getDefaultState();
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

	private static final float[] EMPTY_AO_CACHE = new float[TOTAL_CACHE_SIZE];
	private static final int[] EMPTY_LIGHT_CACHE = new int[TOTAL_CACHE_SIZE];

	static {
		Arrays.fill(EMPTY_AO_CACHE, Float.MAX_VALUE);
		Arrays.fill(EMPTY_LIGHT_CACHE, Integer.MAX_VALUE);
	}
}
