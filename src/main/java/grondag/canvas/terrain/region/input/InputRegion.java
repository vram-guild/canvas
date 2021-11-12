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
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.FACE_I_MASK;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.FACE_J_MASK;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.FACE_J_SHIFT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.FACE_K_SHIFT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.FACE_STATE_COUNT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.INTERIOR_STATE_COUNT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.REGION_PADDING;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.SIDE_INDEX_X0;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.SIDE_INDEX_X2;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.SIDE_INDEX_Y0;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.SIDE_INDEX_Y2;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.SIDE_INDEX_Z0;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.SIDE_INDEX_Z2;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.TOTAL_STATE_COUNT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.interiorIndex;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.regionIndexToPackedSectionPos;

import java.util.Arrays;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

import io.vram.frex.api.world.RenderRegionBakeListener;

import grondag.canvas.apiimpl.rendercontext.CanvasTerrainRenderContext;
import grondag.canvas.terrain.occlusion.geometry.RegionOcclusionCalculator;
import grondag.canvas.terrain.util.ChunkColorCache;

// FIX: should not allow direct world access, esp from non-main threads
public class InputRegion extends AbstractInputRegion implements BlockAndTintGetter {
	private static final int[] EMPTY_AO_CACHE = new int[TOTAL_STATE_COUNT];
	private static final int[] EMPTY_LIGHT_CACHE = new int[TOTAL_STATE_COUNT];
	private static final Object[] EMPTY_RENDER_DATA = new Object[INTERIOR_STATE_COUNT];
	private static final BlockEntity[] EMPTY_BLOCK_ENTITIES = new BlockEntity[INTERIOR_STATE_COUNT];

	static {
		Arrays.fill(EMPTY_AO_CACHE, Integer.MAX_VALUE);
		Arrays.fill(EMPTY_LIGHT_CACHE, Integer.MAX_VALUE);
	}

	public final BlockEntity[] blockEntities = new BlockEntity[INTERIOR_STATE_COUNT];
	public final CanvasTerrainRenderContext terrainContext;
	protected final BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();
	protected final Object[] renderData = new Object[INTERIOR_STATE_COUNT];
	private final BlockState[] states = new BlockState[TOTAL_STATE_COUNT];
	public final ObjectArrayList<RenderRegionBakeListener> bakeListeners = new ObjectArrayList<>();

	public final RegionOcclusionCalculator occlusion = new RegionOcclusionCalculator() {
		@Override
		protected BlockState blockStateAtIndex(int regionIndex) {
			return states[regionIndex];
		}

		@Override
		protected boolean closedAtRelativePos(BlockState blockState, int regionIndex) {
			final int xyz5 = regionIndexToPackedSectionPos(regionIndex);
			final int x = (xyz5 & 31) - REGION_PADDING;
			final int y = ((xyz5 >> 5) & 31) - REGION_PADDING;
			final int z = ((xyz5 >> 10) & 31) - REGION_PADDING;
			return blockState.isSolidRender(InputRegion.this, searchPos.set(originX + x, originY + y, originZ + z));
		}
	};

	// PERF: pack for reduced memory, better LOC
	private final int[] aoCache = new int[TOTAL_STATE_COUNT];
	private final int[] lightCache = new int[TOTAL_STATE_COUNT];

	public InputRegion(CanvasTerrainRenderContext terrainContext) {
		this.terrainContext = terrainContext;
	}

	public void prepare(PackedInputRegion packedRegion) {
		bakeListeners.clear();
		bakeListeners.addAll(packedRegion.bakeListenerContext.listeners);

		System.arraycopy(packedRegion.chunks, 0, chunks, 0, 16);
		System.arraycopy(EMPTY_BLOCK_ENTITIES, 0, blockEntities, 0, INTERIOR_STATE_COUNT);
		System.arraycopy(EMPTY_RENDER_DATA, 0, renderData, 0, INTERIOR_STATE_COUNT);
		System.arraycopy(EMPTY_AO_CACHE, 0, aoCache, 0, TOTAL_STATE_COUNT);
		System.arraycopy(EMPTY_LIGHT_CACHE, 0, lightCache, 0, TOTAL_STATE_COUNT);

		world = packedRegion.world;

		originX = packedRegion.originX;
		originY = packedRegion.originY;
		originZ = packedRegion.originZ;

		chunkBaseX = packedRegion.chunkBaseX;
		baseSectionIndex = packedRegion.baseSectionIndex;
		chunkBaseZ = packedRegion.chunkBaseZ;

		final var mainSection = getSection(1, 1, 1);

		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					states[interiorIndex(x, y, z)] = mainSection.getBlockState(x, y, z);
				}
			}
		}

		captureCorners();
		captureEdges();
		captureFaces();

		copyBeData(packedRegion);

		occlusion.prepare();
	}

	private interface BlockStateFunction {
		BlockState apply (int i, int j, int k);
	}

	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private static BlockStateFunction AIR_FUNCTION = (i, j, k) -> AIR;

	//NB: the addressing math here must match what is in RenderRegionAddressHelper
	private void captureFace(int baseIndex, BlockStateFunction func) {
		for (int n = 0; n < FACE_STATE_COUNT; ++n) {
			states[baseIndex + n] = func.apply(n & FACE_I_MASK, (n >> FACE_J_SHIFT) & FACE_J_MASK, n >> FACE_K_SHIFT);
		}
	}

	private void captureFaces() {
		final LevelChunkSection lowX = getSection(0, 1, 1);
		captureFace(SIDE_INDEX_X0, lowX == null ? AIR_FUNCTION : (i, j, k) -> lowX.getBlockState(14 + k, i, j));

		final LevelChunkSection highX = getSection(2, 1, 1);
		captureFace(SIDE_INDEX_X2, highX == null ? AIR_FUNCTION : (i, j, k) -> highX.getBlockState(k, i, j));

		final LevelChunkSection lowZ = getSection(1, 1, 0);
		captureFace(SIDE_INDEX_Z0, lowZ == null ? AIR_FUNCTION : (i, j, k) -> lowZ.getBlockState(i, j, 14 + k));

		final LevelChunkSection highZ = getSection(1, 1, 2);
		captureFace(SIDE_INDEX_Z2, highZ == null ? AIR_FUNCTION : (i, j, k) -> highZ.getBlockState(i, j, k));

		final LevelChunkSection lowY = getSection(1, 0, 1);
		captureFace(SIDE_INDEX_Y0, lowY == null ? AIR_FUNCTION : (i, j, k) -> lowY.getBlockState(i, 14 + k, j));

		final LevelChunkSection highY = getSection(1, 2, 1);
		captureFace(SIDE_INDEX_Y2, highY == null ? AIR_FUNCTION : (i, j, k) -> highY.getBlockState(i, k, j));
	}

	//NB: the addressing math here must match what is in RenderRegionAddressHelper
	private void captureEdge(int baseIndex, BlockStateFunction func) {
		for (int n = 0; n < EDGE_STATE_COUNT; ++n) {
			states[baseIndex + n] = func.apply(n & EDGE_I_MASK, (n >> EDGE_J_SHIFT) & EDGE_J_MASK, n >> EDGE_K_SHIFT);
		}
	}

	private void captureEdges() {
		final LevelChunkSection aaZ = getSection(0, 0, 1);
		captureEdge(EDGE_INDEX_Y0X0, aaZ == null ? AIR_FUNCTION : (i, j, k) -> aaZ.getBlockState(14 + i, 14 + j, k));

		final LevelChunkSection abZ = getSection(0, 2, 1);
		captureEdge(EDGE_INDEX_Y2X0, abZ == null ? AIR_FUNCTION : (i, j, k) -> abZ.getBlockState(14 + i, j, k));

		final LevelChunkSection baZ = getSection(2, 0, 1);
		captureEdge(EDGE_INDEX_Y0X2, baZ == null ? AIR_FUNCTION : (i, j, k) -> baZ.getBlockState(i, 14 + j, k));

		final LevelChunkSection bbZ = getSection(2, 2, 1);
		captureEdge(EDGE_INDEX_Y2X2, bbZ == null ? AIR_FUNCTION : (i, j, k) -> bbZ.getBlockState(i, j, k));

		final LevelChunkSection aYa = getSection(0, 1, 0);
		captureEdge(EDGE_INDEX_Z0X0, aYa == null ? AIR_FUNCTION : (i, j, k) -> aYa.getBlockState(14 + i, k, 14 + j));

		final LevelChunkSection aYb = getSection(0, 1, 2);
		captureEdge(EDGE_INDEX_Z2X0, aYb == null ? AIR_FUNCTION : (i, j, k) -> aYb.getBlockState(14 + i, k, j));

		final LevelChunkSection bYa = getSection(2, 1, 0);
		captureEdge(EDGE_INDEX_Z0X2, bYa == null ? AIR_FUNCTION : (i, j, k) -> bYa.getBlockState(i, k, 14 + j));

		final LevelChunkSection bYb = getSection(2, 1, 2);
		captureEdge(EDGE_INDEX_Z2X2, bYb == null ? AIR_FUNCTION : (i, j, k) -> bYb.getBlockState(i, k, j));

		final LevelChunkSection Xaa = getSection(1, 0, 0);
		captureEdge(EDGE_INDEX_Z0Y0, Xaa == null ? AIR_FUNCTION : (i, j, k) -> Xaa.getBlockState(k, 14 + i, 14 + j));

		final LevelChunkSection Xab = getSection(1, 0, 2);
		captureEdge(EDGE_INDEX_Z2Y0, Xab == null ? AIR_FUNCTION : (i, j, k) -> Xab.getBlockState(k, 14 + i, j));

		final LevelChunkSection Xba = getSection(1, 2, 0);
		captureEdge(EDGE_INDEX_Z0Y2, Xba == null ? AIR_FUNCTION : (i, j, k) -> Xba.getBlockState(k, i, 14 + j));

		final LevelChunkSection Xbb = getSection(1, 2, 2);
		captureEdge(EDGE_INDEX_Z2Y2, Xbb == null ? AIR_FUNCTION : (i, j, k) -> Xbb.getBlockState(k, i, j));
	}

	//NB: the addressing math here must match what is in RenderRegionAddressHelper
	private void captureCorner(int baseIndex, BlockStateFunction func) {
		for (int n = 0; n < CORNER_STATE_COUNT; ++n) {
			states[baseIndex + n] = func.apply(n & CORNER_I_MASK, (n >> CORNER_J_SHIFT) & CORNER_J_MASK, n >> CORNER_K_SHIFT);
		}
	}

	private void captureCorners() {
		final LevelChunkSection xyz = getSection(0, 0, 0);
		captureCorner(CORNER_INDEX_000, xyz == null ? AIR_FUNCTION : (i, j, k) -> xyz.getBlockState(14 + i, 14 + j, 14 + k));

		final LevelChunkSection xyZ = getSection(0, 0, 2);
		captureCorner(CORNER_INDEX_200, xyZ == null ? AIR_FUNCTION : (i, j, k) -> xyZ.getBlockState(14 + i, 14 + j, k));

		final LevelChunkSection xYz = getSection(0, 2, 0);
		captureCorner(CORNER_INDEX_020, xYz == null ? AIR_FUNCTION : (i, j, k) -> xYz.getBlockState(14 + i, j, 14 + k));

		final LevelChunkSection xYZ = getSection(0, 2, 2);
		captureCorner(CORNER_INDEX_220, xYZ == null ? AIR_FUNCTION : (i, j, k) -> xYZ.getBlockState(14 + i, j, k));

		final LevelChunkSection Xyz = getSection(2, 0, 0);
		captureCorner(CORNER_INDEX_002, Xyz == null ? AIR_FUNCTION : (i, j, k) -> Xyz.getBlockState(i, 14 + j, 14 + k));

		final LevelChunkSection XyZ = getSection(2, 0, 2);
		captureCorner(CORNER_INDEX_202, XyZ == null ? AIR_FUNCTION : (i, j, k) -> XyZ.getBlockState(i, 14 + j, k));

		final LevelChunkSection XYz = getSection(2, 2, 0);
		captureCorner(CORNER_INDEX_022, XYz == null ? AIR_FUNCTION : (i, j, k) -> XYz.getBlockState(i, j, 14 + k));

		final LevelChunkSection XYZ = getSection(2, 2, 2);
		captureCorner(CORNER_INDEX_222, XYZ == null ? AIR_FUNCTION : (i, j, k) -> XYZ.getBlockState(i, j, k));
	}

	private void copyBeData(PackedInputRegion protoRegion) {
		final ShortArrayList blockEntityPos = protoRegion.blockEntityPos;

		if (!blockEntityPos.isEmpty()) {
			final ObjectArrayList<BlockEntity> blockEntities = protoRegion.blockEntities;
			final int limit = blockEntityPos.size();

			for (int i = 0; i < limit; i++) {
				this.blockEntities[blockEntityPos.getShort(i)] = blockEntities.get(i);
			}
		}

		final ShortArrayList renderDataPos = protoRegion.renderDataPos;

		if (!renderDataPos.isEmpty()) {
			final ObjectArrayList<Object> renderData = protoRegion.renderData;
			final int limit = renderDataPos.size();

			for (int i = 0; i < limit; i++) {
				this.renderData[renderDataPos.getShort(i)] = renderData.get(i);
			}
		}
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
	 * Assumes values 0-15.
	 */
	public BlockState getLocalBlockState(int interiorIndex) {
		return states[interiorIndex];
	}

	@Override
	@Nullable
	public BlockEntity getBlockEntity(BlockPos pos) {
		return isInMainChunk(pos) ? blockEntities[interiorIndex(pos)] : world.getBlockEntity(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getBlockState(pos).getFluidState();
	}

	@Override
	public int getBrightness(LightLayer type, BlockPos pos) {
		return world.getBrightness(type, pos);
	}

	// Implements Fabrics API RenderAttachedBlockView
	public Object getBlockEntityRenderAttachment(BlockPos pos) {
		return isInMainChunk(pos) ? renderData[interiorIndex(pos)] : null;
	}

	public int cachedBrightness(BlockPos pos) {
		return cachedBrightness(blockIndex(pos.getX(), pos.getY(), pos.getZ()));
	}

	public int cachedBrightness(int cacheIndex) {
		int result = lightCache[cacheIndex];

		if (result == Integer.MAX_VALUE) {
			final BlockState state = states[cacheIndex];
			final int packedXyz5 = regionIndexToPackedSectionPos(cacheIndex);
			final int x = (packedXyz5 & 31) - 2 + originX;
			final int y = ((packedXyz5 >> 5) & 31) - 2 + originY;
			final int z = (packedXyz5 >> 10) - 2 + originZ;
			result = LevelRenderer.getLightColor(world, state, searchPos.set(x, y, z));
			lightCache[cacheIndex] = result;
		}

		return result;
	}

	/**
	 * For light smoothing.
	 */
	public void setLightCache(int x, int y, int z, int val) {
		lightCache[blockIndex(x, y, z)] = val;
	}

	public int directBrightness(BlockPos pos) {
		return LevelRenderer.getLightColor(world, getBlockState(pos), pos);
	}

	// TODO: do anything with this?
	// Vanilla now computes diffuse shading at chunk bake time and consumes this value in AO calc
	@Override
	public float getShade(Direction direction, boolean shaded) {
		return world.getShade(direction, shaded);
	}

	public int cachedAoLevel(int cacheIndex) {
		int result = aoCache[cacheIndex];

		if (result == Integer.MAX_VALUE) {
			final BlockState state = states[cacheIndex];

			if (state.getLightEmission() == 0) {
				final int packedXyz5 = regionIndexToPackedSectionPos(cacheIndex);
				final int x = (packedXyz5 & 31) - 2 + originX;
				final int y = ((packedXyz5 >> 5) & 31) - 2 + originY;
				final int z = (packedXyz5 >> 10) - 2 + originZ;
				result = Math.round(255f * state.getShadeBrightness(this, searchPos.set(x, y, z)));
			} else {
				result = 255;
			}

			aoCache[cacheIndex] = result;
		}

		return result;
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return world.getLightEngine();
	}

	@Override
	public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
		final int x = blockPos.getX();
		final int z = blockPos.getZ();
		return ChunkColorCache.get(getChunk(x >> 4, z >> 4)).getColor(x, blockPos.getY(), z, colorResolver);
	}

	public Biome getBiome(BlockPos blockPos) {
		final int x = blockPos.getX();
		final int z = blockPos.getZ();
		return ChunkColorCache.get(getChunk(x >> 4, z >> 4)).getBiome(x, blockPos.getY(), z);
	}

	/**
	 * Only valid for positions in render region, including exterior.
	 */
	public boolean isClosed(int cacheIndex) {
		return occlusion.isClosed(cacheIndex);
	}

	public int originX() {
		return originX;
	}

	public int originY() {
		return originY;
	}

	public int originZ() {
		return originZ;
	}

	@Override
	public int getHeight() {
		return world.getHeight();
	}

	@Override
	public int getMinBuildHeight() {
		return world.getMinBuildHeight();
	}
}
