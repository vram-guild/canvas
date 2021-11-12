/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.terrain.region.input;

import static grondag.canvas.terrain.util.RenderRegionStateIndexer.CORNER_INDEX_000;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.CORNER_INDEX_002;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.CORNER_INDEX_020;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.CORNER_INDEX_022;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.CORNER_INDEX_200;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.CORNER_INDEX_202;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.CORNER_INDEX_220;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.CORNER_INDEX_222;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.CORNER_I_MASK;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.CORNER_J_MASK;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.CORNER_J_SHIFT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.CORNER_K_SHIFT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.CORNER_STATE_COUNT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_INDEX_Y0X0;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_INDEX_Y0X2;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_INDEX_Y2X0;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_INDEX_Y2X2;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_INDEX_Z0X0;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_INDEX_Z0X2;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_INDEX_Z0Y0;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_INDEX_Z0Y2;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_INDEX_Z2X0;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_INDEX_Z2X2;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_INDEX_Z2Y0;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_INDEX_Z2Y2;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_I_MASK;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_J_MASK;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_J_SHIFT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_K_SHIFT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EDGE_STATE_COUNT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EXTERIOR_STATE_COUNT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.FACE_I_MASK;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.FACE_J_MASK;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.FACE_J_SHIFT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.FACE_K_SHIFT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.FACE_STATE_COUNT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.INTERIOR_STATE_COUNT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.SIDE_INDEX_X0;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.SIDE_INDEX_X2;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.SIDE_INDEX_Y0;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.SIDE_INDEX_Y2;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.SIDE_INDEX_Z0;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.SIDE_INDEX_Z2;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.interiorIndex;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import io.vram.frex.api.world.BlockEntityRenderData;
import io.vram.frex.api.world.RenderRegionBakeListener;
import io.vram.frex.impl.world.ChunkRenderConditionContext;

import grondag.canvas.perf.ChunkRebuildCounters;
import grondag.canvas.terrain.util.ChunkPaletteCopier;
import grondag.canvas.terrain.util.ChunkPaletteCopier.PaletteCopy;

/**
 * Serves as a container to capture world state data on the main thread as quickly as possible
 * for later consumption on possibly non-render threads for terrain render region rebuild.
 *
 * <p>Also serves as a state indicator for rebuild activity.
 */
public class PackedInputRegion extends AbstractInputRegion {
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private static final ArrayBlockingQueue<PackedInputRegion> POOL = new ArrayBlockingQueue<>(256);

	public final ObjectArrayList<BlockEntity> blockEntities = new ObjectArrayList<>();
	public final ChunkRenderConditionContext bakeListenerContext = new ChunkRenderConditionContext();

	final BlockState[] states = new BlockState[EXTERIOR_STATE_COUNT];
	final ShortArrayList renderDataPos = new ShortArrayList();
	final ObjectArrayList<Object> renderData = new ObjectArrayList<>();
	final ShortArrayList blockEntityPos = new ShortArrayList();
	PaletteCopy mainSectionCopy;

	public static PackedInputRegion claim(ClientLevel world, BlockPos origin) {
		final PackedInputRegion result = POOL.poll();
		return (result == null ? new PackedInputRegion() : result).prepare(world, origin);
	}

	private static void release(PackedInputRegion region) {
		POOL.offer(region);
	}

	public static void reload() {
		// ensure current AoFix rule or other config-dependent lambdas are used
		POOL.clear();
	}

	private PackedInputRegion prepare(ClientLevel world, BlockPos origin) {
		if (ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.startCopy();
		}

		this.world = world;

		final int originX = origin.getX();
		final int originY = origin.getY();
		final int originZ = origin.getZ();

		this.originX = originX;
		this.originY = originY;
		this.originZ = originZ;

		final int chunkBaseX = (originX >> 4) - 1;
		final int chunkBaseZ = (originZ >> 4) - 1;

		this.chunkBaseX = chunkBaseX;
		baseSectionIndex = ((originY - world.getMinBuildHeight()) >> 4) - 1;
		this.chunkBaseZ = chunkBaseZ;

		final LevelChunk mainChunk = world.getChunk(chunkBaseX + 1, chunkBaseZ + 1);
		mainSectionCopy = ChunkPaletteCopier.captureCopy(mainChunk, originY);

		final ChunkRenderConditionContext bakeListenerContext = this.bakeListenerContext.prepare(world, originX, originY, originZ);
		RenderRegionBakeListener.prepareInvocations(bakeListenerContext, bakeListenerContext.listeners);

		final PackedInputRegion result;

		if (mainSectionCopy == ChunkPaletteCopier.AIR_COPY && bakeListenerContext.listeners.isEmpty()) {
			release();
			result = SignalInputRegion.EMPTY;
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

		if (ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.completeCopy();
		}

		return result;
	}

	PaletteCopy takePaletteCopy() {
		final PaletteCopy result = mainSectionCopy;
		mainSectionCopy = null;
		return result;
	}

	private void captureBlockEntities(LevelChunk mainChunk) {
		renderDataPos.clear();
		renderData.clear();
		blockEntityPos.clear();
		blockEntities.clear();
		final int yCheck = (originY >> 4);

		for (final Map.Entry<BlockPos, BlockEntity> entry : mainChunk.getBlockEntities().entrySet()) {
			final BlockPos pos = entry.getKey();

			// only those in this chunk
			if (pos.getY() >> 4 != yCheck) {
				continue;
			}

			final short key = (short) interiorIndex(pos);
			final BlockEntity be = entry.getValue();

			blockEntityPos.add(key);
			blockEntities.add(be);

			final Object rd = BlockEntityRenderData.get(be);

			if (rd != null) {
				renderDataPos.add(key);
				renderData.add(rd);
			}
		}
	}

	private interface BlockStateFunction {
		BlockState apply (int i, int j, int k);
	}

	private static BlockStateFunction AIR_FUNCTION = (i, j, k) -> AIR;

	//NB: the addressing math here must match what is in RenderRegionAddressHelper
	private void captureFace(int baseIndex, BlockStateFunction func) {
		for (int n = 0; n < FACE_STATE_COUNT; ++n) {
			states[baseIndex + n] = func.apply(n & FACE_I_MASK, (n >> FACE_J_SHIFT) & FACE_J_MASK, n >> FACE_K_SHIFT);
		}
	}

	private void captureFaces() {
		final LevelChunkSection lowX = getSection(0, 1, 1);
		captureFace(SIDE_INDEX_X0 - INTERIOR_STATE_COUNT, lowX == null ? AIR_FUNCTION : (i, j, k) -> lowX.getBlockState(14 + k, i, j));

		final LevelChunkSection highX = getSection(2, 1, 1);
		captureFace(SIDE_INDEX_X2 - INTERIOR_STATE_COUNT, highX == null ? AIR_FUNCTION : (i, j, k) -> highX.getBlockState(k, i, j));

		final LevelChunkSection lowZ = getSection(1, 1, 0);
		captureFace(SIDE_INDEX_Z0 - INTERIOR_STATE_COUNT, lowZ == null ? AIR_FUNCTION : (i, j, k) -> lowZ.getBlockState(i, j, 14 + k));

		final LevelChunkSection highZ = getSection(1, 1, 2);
		captureFace(SIDE_INDEX_Z2 - INTERIOR_STATE_COUNT, highZ == null ? AIR_FUNCTION : (i, j, k) -> highZ.getBlockState(i, j, k));

		final LevelChunkSection lowY = getSection(1, 0, 1);
		captureFace(SIDE_INDEX_Y0 - INTERIOR_STATE_COUNT, lowY == null ? AIR_FUNCTION : (i, j, k) -> lowY.getBlockState(i, 14 + k, j));

		final LevelChunkSection highY = getSection(1, 2, 1);
		captureFace(SIDE_INDEX_Y2 - INTERIOR_STATE_COUNT, highY == null ? AIR_FUNCTION : (i, j, k) -> highY.getBlockState(i, k, j));
	}

	//NB: the addressing math here must match what is in RenderRegionAddressHelper
	private void captureEdge(int baseIndex, BlockStateFunction func) {
		for (int n = 0; n < EDGE_STATE_COUNT; ++n) {
			states[baseIndex + n] = func.apply(n & EDGE_I_MASK, (n >> EDGE_J_SHIFT) & EDGE_J_MASK, n >> EDGE_K_SHIFT);
		}
	}

	private void captureEdges() {
		final LevelChunkSection aaZ = getSection(0, 0, 1);
		captureEdge(EDGE_INDEX_Y0X0 - INTERIOR_STATE_COUNT, aaZ == null ? AIR_FUNCTION : (i, j, k) -> aaZ.getBlockState(14 + i, 14 + j, k));

		final LevelChunkSection abZ = getSection(0, 2, 1);
		captureEdge(EDGE_INDEX_Y2X0 - INTERIOR_STATE_COUNT, abZ == null ? AIR_FUNCTION : (i, j, k) -> abZ.getBlockState(14 + i, j, k));

		final LevelChunkSection baZ = getSection(2, 0, 1);
		captureEdge(EDGE_INDEX_Y0X2 - INTERIOR_STATE_COUNT, baZ == null ? AIR_FUNCTION : (i, j, k) -> baZ.getBlockState(i, 14 + j, k));

		final LevelChunkSection bbZ = getSection(2, 2, 1);
		captureEdge(EDGE_INDEX_Y2X2 - INTERIOR_STATE_COUNT, bbZ == null ? AIR_FUNCTION : (i, j, k) -> bbZ.getBlockState(i, j, k));

		final LevelChunkSection aYa = getSection(0, 1, 0);
		captureEdge(EDGE_INDEX_Z0X0 - INTERIOR_STATE_COUNT, aYa == null ? AIR_FUNCTION : (i, j, k) -> aYa.getBlockState(14 + i, k, 14 + j));

		final LevelChunkSection aYb = getSection(0, 1, 2);
		captureEdge(EDGE_INDEX_Z2X0 - INTERIOR_STATE_COUNT, aYb == null ? AIR_FUNCTION : (i, j, k) -> aYb.getBlockState(14 + i, k, j));

		final LevelChunkSection bYa = getSection(2, 1, 0);
		captureEdge(EDGE_INDEX_Z0X2 - INTERIOR_STATE_COUNT, bYa == null ? AIR_FUNCTION : (i, j, k) -> bYa.getBlockState(i, k, 14 + j));

		final LevelChunkSection bYb = getSection(2, 1, 2);
		captureEdge(EDGE_INDEX_Z2X2 - INTERIOR_STATE_COUNT, bYb == null ? AIR_FUNCTION : (i, j, k) -> bYb.getBlockState(i, k, j));

		final LevelChunkSection Xaa = getSection(1, 0, 0);
		captureEdge(EDGE_INDEX_Z0Y0 - INTERIOR_STATE_COUNT, Xaa == null ? AIR_FUNCTION : (i, j, k) -> Xaa.getBlockState(k, 14 + i, 14 + j));

		final LevelChunkSection Xab = getSection(1, 0, 2);
		captureEdge(EDGE_INDEX_Z2Y0 - INTERIOR_STATE_COUNT, Xab == null ? AIR_FUNCTION : (i, j, k) -> Xab.getBlockState(k, 14 + i, j));

		final LevelChunkSection Xba = getSection(1, 2, 0);
		captureEdge(EDGE_INDEX_Z0Y2 - INTERIOR_STATE_COUNT, Xba == null ? AIR_FUNCTION : (i, j, k) -> Xba.getBlockState(k, i, 14 + j));

		final LevelChunkSection Xbb = getSection(1, 2, 2);
		captureEdge(EDGE_INDEX_Z2Y2 - INTERIOR_STATE_COUNT, Xbb == null ? AIR_FUNCTION : (i, j, k) -> Xbb.getBlockState(k, i, j));
	}

	//NB: the addressing math here must match what is in RenderRegionAddressHelper
	private void captureCorner(int baseIndex, BlockStateFunction func) {
		for (int n = 0; n < CORNER_STATE_COUNT; ++n) {
			states[baseIndex + n] = func.apply(n & CORNER_I_MASK, (n >> CORNER_J_SHIFT) & CORNER_J_MASK, n >> CORNER_K_SHIFT);
		}
	}

	private void captureCorners() {
		final LevelChunkSection xyz = getSection(0, 0, 0);
		captureCorner(CORNER_INDEX_000 - INTERIOR_STATE_COUNT, xyz == null ? AIR_FUNCTION : (i, j, k) -> xyz.getBlockState(14 + i, 14 + j, 14 + k));

		final LevelChunkSection xyZ = getSection(0, 0, 2);
		captureCorner(CORNER_INDEX_200 - INTERIOR_STATE_COUNT, xyZ == null ? AIR_FUNCTION : (i, j, k) -> xyZ.getBlockState(14 + i, 14 + j, k));

		final LevelChunkSection xYz = getSection(0, 2, 0);
		captureCorner(CORNER_INDEX_020 - INTERIOR_STATE_COUNT, xYz == null ? AIR_FUNCTION : (i, j, k) -> xYz.getBlockState(14 + i, j, 14 + k));

		final LevelChunkSection xYZ = getSection(0, 2, 2);
		captureCorner(CORNER_INDEX_220 - INTERIOR_STATE_COUNT, xYZ == null ? AIR_FUNCTION : (i, j, k) -> xYZ.getBlockState(14 + i, j, k));

		final LevelChunkSection Xyz = getSection(2, 0, 0);
		captureCorner(CORNER_INDEX_002 - INTERIOR_STATE_COUNT, Xyz == null ? AIR_FUNCTION : (i, j, k) -> Xyz.getBlockState(i, 14 + j, 14 + k));

		final LevelChunkSection XyZ = getSection(2, 0, 2);
		captureCorner(CORNER_INDEX_202 - INTERIOR_STATE_COUNT, XyZ == null ? AIR_FUNCTION : (i, j, k) -> XyZ.getBlockState(i, 14 + j, k));

		final LevelChunkSection XYz = getSection(2, 2, 0);
		captureCorner(CORNER_INDEX_022 - INTERIOR_STATE_COUNT, XYz == null ? AIR_FUNCTION : (i, j, k) -> XYz.getBlockState(i, j, 14 + k));

		final LevelChunkSection XYZ = getSection(2, 2, 2);
		captureCorner(CORNER_INDEX_222 - INTERIOR_STATE_COUNT, XYZ == null ? AIR_FUNCTION : (i, j, k) -> XYZ.getBlockState(i, j, k));
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
}
