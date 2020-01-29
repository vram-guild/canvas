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

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

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
import grondag.canvas.light.AoLuminanceFix;
import grondag.canvas.perf.ChunkRebuildCounters;
import grondag.fermion.position.PackedBlockPos;

public class FastRenderRegion implements RenderAttachedBlockView {
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

	public final Long2IntOpenHashMap brightnessCache;
	public final Long2FloatOpenHashMap aoLevelCache;
	private final AoLuminanceFix aoFix = AoLuminanceFix.effective();
	private final Int2ObjectOpenHashMap<Object> renderData = new Int2ObjectOpenHashMap<>();
	public final Int2ObjectOpenHashMap<BlockEntity> blockEntities = new Int2ObjectOpenHashMap<>();

	public final BlockState[] states = new BlockState[4096];
	private final Long2ObjectOpenHashMap<BlockState> edgeStates = new Long2ObjectOpenHashMap<>();

	// larger than needed to speed up indexing
	private final WorldChunk[] chunks = new WorldChunk[16];
	private PaletteCopy mainSectionCopy;

	private long chunkOriginKey;

	private int chunkBaseX;
	private int chunkBaseY;
	private int chunkBaseZ;

	private World world;
	public TerrainRenderContext terrainContext;

	private FastRenderRegion() {
		brightnessCache = new Long2IntOpenHashMap(32768);
		brightnessCache.defaultReturnValue(Integer.MAX_VALUE);
		aoLevelCache = new Long2FloatOpenHashMap(32768);
		aoLevelCache.defaultReturnValue(Float.MAX_VALUE);
	}

	public void prepareForUse() {
		final PaletteCopy pc = mainSectionCopy;
		mainSectionCopy = null;

		for(int i = 0; i < 4096; i++) {
			states[i] = pc.apply(i);
		}

		pc.release();
	}

	private long chunkKey(int x, int y, int z) {
		return PackedBlockPos.pack(x & 0xFFFFFFF0, y & 0xF0, z & 0xFFFFFFF0);
	}

	private FastRenderRegion prepare(World world, BlockPos origin) {
		final ChunkRebuildCounters counter = ChunkRebuildCounters.get();
		final long start = counter.copyCounter.startRun();

		this.world = world;

		final int ox = origin.getX();
		final int oy = origin.getY();
		final int oz = origin.getZ();

		chunkOriginKey = chunkKey(ox, oy, oz);

		final int blockBaseX = ox - 1;
		final int blockBaseY = oy - 1;
		final int blockBaseZ = oz - 1;

		chunkBaseX = blockBaseX >> 4;
		// eclipse save action formatting shits the bed here for no apparent reason
		chunkBaseY = blockBaseY >> 4;
		chunkBaseZ = blockBaseZ >> 4;

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

			edgeStates.clear();
			captureCorners(ox, oy, oz);
			captureEdges(ox, oy, oz);
			captureFaces(ox, oy, oz);

			brightnessCache.clear();
			aoLevelCache.clear();

			result = this;
		}

		counter.copyCounter.endRun(start);
		counter.copyCounter.addCount(1);

		return result;
	}

	private int singleChunkBlockIndex(int x, int y, int z) {
		return (x & 0xF) | ((y & 0xF) << 4) | ((z & 0xF) << 8);
	}

	private int singleChunkBlockIndex(BlockPos pos) {
		return singleChunkBlockIndex(pos.getX(), pos.getY(), pos.getZ());
	}

	private boolean isInMainChunk(int x, int y, int z) {
		return chunkKey(x, y, z) == chunkOriginKey;
	}

	private boolean isInMainChunk(BlockPos pos) {
		return isInMainChunk(pos.getX(), pos.getY(), pos.getZ());
	}

	private void captureBlockEntities(WorldChunk mainChunk) {
		renderData.clear();
		blockEntities.clear();

		for(final Map.Entry<BlockPos, BlockEntity> entry : mainChunk.getBlockEntities().entrySet()) {
			final int key = singleChunkBlockIndex(entry.getKey());
			final BlockEntity be = entry.getValue();
			blockEntities.put(key, be);
			final Object rd = ((RenderAttachmentBlockEntity) be).getRenderAttachmentData();

			if(rd != null) {
				renderData.put(key, rd);
			}
		}
	}

	private void captureFaces(int ox, int oy, int oz) {
		final ChunkSection lowX = getSection(0, 1, 1);
		final ChunkSection highX = getSection(2, 1, 1);
		final ChunkSection lowZ = getSection(1, 1, 0);
		final ChunkSection highZ = getSection(1, 1, 2);
		final ChunkSection lowY = getSection(1, 0, 1);
		final ChunkSection highY = getSection(1, 2, 1);

		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				edgeStates.put(PackedBlockPos.pack(ox - 1, oy + i, oz + j), lowX == null ? AIR : lowX.getBlockState(15, i, j));
				edgeStates.put(PackedBlockPos.pack(ox + 16, oy + i, oz + j), highX == null ? AIR : highX.getBlockState(0, i, j));

				edgeStates.put(PackedBlockPos.pack(ox + i, oy + j, oz - 1), lowZ == null ? AIR : lowZ.getBlockState(i, j, 15));
				edgeStates.put(PackedBlockPos.pack(ox + i, oy + j, oz + 16), highZ == null ? AIR : highZ.getBlockState(i, j, 0));

				edgeStates.put(PackedBlockPos.pack(ox + i, oy - 1, oz + j), lowY == null ? AIR : lowY.getBlockState(i, 15, j));
				edgeStates.put(PackedBlockPos.pack(ox + i, oy + 16, oz + j),  highY == null ? AIR : highY.getBlockState(i, 0, j));
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

	private void captureEdges(int ox, int oy, int oz) {
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
			edgeStates.put(PackedBlockPos.pack(ox - 1, oy - 1, oz + i),  aaZ == null ? AIR : aaZ.getBlockState(15, 15, i));
			edgeStates.put(PackedBlockPos.pack(ox - 1, oy + 16, oz + i),  abZ == null ? AIR : abZ.getBlockState(15, 0, i));
			edgeStates.put(PackedBlockPos.pack(ox + 16, oy - 1, oz + i),  baZ == null ? AIR : baZ.getBlockState(0, 15, i));
			edgeStates.put(PackedBlockPos.pack(ox + 16, oy + 16, oz + i),  bbZ == null ? AIR : bbZ.getBlockState(0, 0, i));

			edgeStates.put(PackedBlockPos.pack(ox - 1, oy + i, oz - 1),  aYa == null ? AIR : aYa.getBlockState(15, i, 15));
			edgeStates.put(PackedBlockPos.pack(ox - 1, oy + i, oz + 16),  aYb == null ? AIR : aYb.getBlockState(15, i, 0));
			edgeStates.put(PackedBlockPos.pack(ox + 16, oy + i, oz - 1),  bYa == null ? AIR : bYa.getBlockState(0, i, 15));
			edgeStates.put(PackedBlockPos.pack(ox + 16, oy + i, oz + 16),  bYb == null ? AIR : bYb.getBlockState(0, i, 0));

			edgeStates.put(PackedBlockPos.pack(ox + i, oy - 1, oz - 1),  Xaa == null ? AIR : Xaa.getBlockState(i, 15, 15));
			edgeStates.put(PackedBlockPos.pack(ox + i, oy - 1, oz + 16),  Xab == null ? AIR : Xab.getBlockState(i, 15, 0));
			edgeStates.put(PackedBlockPos.pack(ox + i, oy + 16, oz - 1),  Xba == null ? AIR : Xba.getBlockState(i, 0, 15));
			edgeStates.put(PackedBlockPos.pack(ox + i, oy + 16, oz + 16),  Xbb == null ? AIR : Xbb.getBlockState(i, 0, 0));
		}
	}

	private void captureCorners(int ox, int oy, int oz) {
		edgeStates.put(PackedBlockPos.pack(ox - 1, oy - 1, oz - 1),  captureCornerState(0, 0, 0));
		edgeStates.put(PackedBlockPos.pack(ox - 1, oy - 1, oz + 16),  captureCornerState(0, 0, 2));
		edgeStates.put(PackedBlockPos.pack(ox - 1, oy + 16, oz - 1),  captureCornerState(0, 2, 0));
		edgeStates.put(PackedBlockPos.pack(ox - 1, oy + 16, oz + 16),  captureCornerState(0, 2, 2));

		edgeStates.put(PackedBlockPos.pack(ox + 16, oy - 1, oz - 1),  captureCornerState(2, 0, 0));
		edgeStates.put(PackedBlockPos.pack(ox + 16, oy - 1, oz + 16),  captureCornerState(2, 0, 2));
		edgeStates.put(PackedBlockPos.pack(ox + 16, oy + 16, oz - 1),  captureCornerState(2, 2, 0));
		edgeStates.put(PackedBlockPos.pack(ox + 16, oy + 16, oz + 16),  captureCornerState(2, 2, 2));
	}

	private BlockState captureCornerState(int x, int y, int z) {
		final ChunkSection section = getSection(x, y, z);
		return section == null ? AIR : section.getBlockState(x == 0 ? 15 : 0, y == 0 ? 15 : 0, z == 0 ? 15 : 0);
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		final int x = pos.getX();
		final int y = pos.getY();
		final int z = pos.getZ();

		if (this.isInMainChunk(x, y, z)) {
			// indexing scheme here matches blockstate palette
			return states[(x & 0xF) | ((y & 0xF) << 8) | ((z & 0xF) << 4)];
		} else {
			final BlockState result = edgeStates.get(PackedBlockPos.pack(x, y, z));
			return result == null ? world.getBlockState(pos) : result;
		}
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
		return isInMainChunk(pos) ? blockEntities.get(this.singleChunkBlockIndex(pos)) : world.getBlockEntity(pos);
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
		return renderData.get(singleChunkBlockIndex(pos));
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
