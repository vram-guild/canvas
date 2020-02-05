/*******************************************************************************
 * Copyright 2019, 2020 grondag
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

import static grondag.canvas.chunk.RenderRegionAddressHelper.AIR;
import static grondag.canvas.chunk.RenderRegionAddressHelper.EXTERIOR_CACHE_SIZE;
import static grondag.canvas.chunk.RenderRegionAddressHelper.INTERIOR_CACHE_SIZE;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localCornerIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localXEdgeIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localXfaceIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localYEdgeIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localYfaceIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localZEdgeIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localZfaceIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.mainChunkBlockIndex;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;

import grondag.canvas.chunk.ChunkPaletteCopier.PaletteCopy;
import grondag.canvas.perf.ChunkRebuildCounters;

public class ProtoRenderRegion extends AbstractRenderRegion {
	final BlockState[] states = new BlockState[EXTERIOR_CACHE_SIZE];

	final ShortArrayList renderDataPos = new ShortArrayList();
	final ObjectArrayList<Object> renderData = new ObjectArrayList<>();
	final ShortArrayList blockEntityPos = new ShortArrayList();
	public final ObjectArrayList<BlockEntity> blockEntities = new ObjectArrayList<>();

	PaletteCopy mainSectionCopy;

	private long start;
	ChunkRebuildCounters counter;

	private ProtoRenderRegion prepare(World world, BlockPos origin) {
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

		final ProtoRenderRegion result;

		if(mainSectionCopy == ChunkPaletteCopier.AIR_COPY) {
			release();
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

			result = this;
		}

		if(ChunkRebuildCounters.ENABLED) {
			counter.copyCounter.endRun(start);
			counter.copyCounter.addCount(1);
		}

		return result;
	}

	PaletteCopy takePaletteCopy() {
		final PaletteCopy result = mainSectionCopy;
		mainSectionCopy = null;
		return result;

	}

	private void captureBlockEntities(WorldChunk mainChunk) {
		renderDataPos.clear();
		renderData.clear();
		blockEntityPos.clear();
		blockEntities.clear();

		for(final Map.Entry<BlockPos, BlockEntity> entry : mainChunk.getBlockEntities().entrySet()) {
			final short key = (short) mainChunkBlockIndex(entry.getKey());
			final BlockEntity be = entry.getValue();

			blockEntityPos.add(key);
			blockEntities.add(be);

			final Object rd = ((RenderAttachmentBlockEntity) be).getRenderAttachmentData();

			if(rd != null) {
				renderDataPos.add(key);
				renderData.add(rd);
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
				states[localXfaceIndex(false, i, j) - INTERIOR_CACHE_SIZE] = lowX == null ? AIR : lowX.getBlockState(15, i, j);
				states[localXfaceIndex(true, i, j) - INTERIOR_CACHE_SIZE] = highX == null ? AIR : highX.getBlockState(0, i, j);

				states[localZfaceIndex(i, j, false) - INTERIOR_CACHE_SIZE] = lowZ == null ? AIR : lowZ.getBlockState(i, j, 15);
				states[localZfaceIndex(i, j, true) - INTERIOR_CACHE_SIZE] = highZ == null ? AIR : highZ.getBlockState(i, j, 0);

				states[localYfaceIndex(i, false, j) - INTERIOR_CACHE_SIZE] = lowY == null ? AIR : lowY.getBlockState(i, 15, j);
				states[localYfaceIndex(i, true, j) - INTERIOR_CACHE_SIZE] = highY == null ? AIR : highY.getBlockState(i, 0, j);
			}
		}
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
			states[localZEdgeIndex(false, false, i) - INTERIOR_CACHE_SIZE] = aaZ == null ? AIR : aaZ.getBlockState(15, 15, i);
			states[localZEdgeIndex(false, true, i) - INTERIOR_CACHE_SIZE] = abZ == null ? AIR : abZ.getBlockState(15, 0, i);
			states[localZEdgeIndex(true, false, i) - INTERIOR_CACHE_SIZE] = baZ == null ? AIR : baZ.getBlockState(0, 15, i);
			states[localZEdgeIndex(true, true, i) - INTERIOR_CACHE_SIZE] = bbZ == null ? AIR : bbZ.getBlockState(0, 0, i);

			states[localYEdgeIndex(false, i, false) - INTERIOR_CACHE_SIZE] = aYa == null ? AIR : aYa.getBlockState(15, i, 15);
			states[localYEdgeIndex(false, i, true) - INTERIOR_CACHE_SIZE] = aYb == null ? AIR : aYb.getBlockState(15, i, 0);
			states[localYEdgeIndex(true, i, false) - INTERIOR_CACHE_SIZE] = bYa == null ? AIR : bYa.getBlockState(0, i, 15);
			states[localYEdgeIndex(true, i, true) - INTERIOR_CACHE_SIZE] = bYb == null ? AIR : bYb.getBlockState(0, i, 0);

			states[localXEdgeIndex(i, false, false) - INTERIOR_CACHE_SIZE] = Xaa == null ? AIR : Xaa.getBlockState(i, 15, 15);
			states[localXEdgeIndex(i, false, true) - INTERIOR_CACHE_SIZE] = Xab == null ? AIR : Xab.getBlockState(i, 15, 0);
			states[localXEdgeIndex(i, true, false) - INTERIOR_CACHE_SIZE] = Xba == null ? AIR : Xba.getBlockState(i, 0, 15);
			states[localXEdgeIndex(i, true, true) - INTERIOR_CACHE_SIZE] = Xbb == null ? AIR : Xbb.getBlockState(i, 0, 0);
		}
	}

	private void captureCorners() {
		states[localCornerIndex(false, false, false) - INTERIOR_CACHE_SIZE] = captureCornerState(0, 0, 0);
		states[localCornerIndex(false, false, true) - INTERIOR_CACHE_SIZE] = captureCornerState(0, 0, 2);
		states[localCornerIndex(false, true, false) - INTERIOR_CACHE_SIZE] = captureCornerState(0, 2, 0);
		states[localCornerIndex(false, true, true) - INTERIOR_CACHE_SIZE] = captureCornerState(0, 2, 2);

		states[localCornerIndex(true, false, false) - INTERIOR_CACHE_SIZE] = captureCornerState(2, 0, 0);
		states[localCornerIndex(true, false, true) - INTERIOR_CACHE_SIZE] = captureCornerState(2, 0, 2);
		states[localCornerIndex(true, true, false) - INTERIOR_CACHE_SIZE] = captureCornerState(2, 2, 0);
		states[localCornerIndex(true, true, true) - INTERIOR_CACHE_SIZE] = captureCornerState(2, 2, 2);
	}

	private BlockState captureCornerState(int x, int y, int z) {
		final ChunkSection section = getSection(x, y, z);
		return section == null ? AIR : section.getBlockState(x == 0 ? 15 : 0, y == 0 ? 15 : 0, z == 0 ? 15 : 0);
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

		blockEntities.clear();
		renderData.clear();

		release(this);
	}

	private static final ArrayBlockingQueue<ProtoRenderRegion> POOL = new ArrayBlockingQueue<>(256);

	public static ProtoRenderRegion claim(World world, BlockPos origin) {
		final ProtoRenderRegion result = POOL.poll();
		return (result == null ? new ProtoRenderRegion() : result).prepare(world, origin);
	}

	private static void release(ProtoRenderRegion region) {
		POOL.offer(region);
	}

	public static void forceReload() {
		// ensure current AoFix rule or other config-dependent lambdas are used
		POOL.clear();
	}
}
