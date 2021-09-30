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

import static grondag.canvas.terrain.util.RenderRegionStateIndexer.EXTERIOR_STATE_COUNT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.INTERIOR_STATE_COUNT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.REGION_PADDING;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.TOTAL_STATE_COUNT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.interiorIndex;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.regionIndexToXyz5;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.terrain.occlusion.geometry.RegionOcclusionCalculator;
import grondag.canvas.terrain.util.ChunkColorCache;
import grondag.canvas.terrain.util.ChunkPaletteCopier.PaletteCopy;

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
	public final TerrainRenderContext terrainContext;
	protected final BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();
	protected final Object[] renderData = new Object[INTERIOR_STATE_COUNT];
	private final BlockState[] states = new BlockState[TOTAL_STATE_COUNT];

	public final RegionOcclusionCalculator occlusion = new RegionOcclusionCalculator() {
		@Override
		protected BlockState blockStateAtIndex(int regionIndex) {
			return states[regionIndex];
		}

		@Override
		protected boolean closedAtRelativePos(BlockState blockState, int regionIndex) {
			final int xyz5 = regionIndexToXyz5(regionIndex);
			final int x = (xyz5 & 31) - REGION_PADDING;
			final int y = ((xyz5 >> 5) & 31) - REGION_PADDING;
			final int z = ((xyz5 >> 10) & 31) - REGION_PADDING;
			return blockState.isSolidRender(InputRegion.this, searchPos.set(originX + x, originY + y, originZ + z));
		}
	};

	// PERF: pack for reduced memory, better LOC
	private final int[] aoCache = new int[TOTAL_STATE_COUNT];
	private final int[] lightCache = new int[TOTAL_STATE_COUNT];

	public InputRegion(TerrainRenderContext terrainContext) {
		this.terrainContext = terrainContext;
	}

	public void prepare(PackedInputRegion packedRegion) {
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

		final PaletteCopy pc = packedRegion.takePaletteCopy();

		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					states[interiorIndex(x, y, z)] = pc.apply(x | (y << 8) | (z << 4));
				}
			}
		}

		pc.release();

		System.arraycopy(packedRegion.states, 0, states, INTERIOR_STATE_COUNT, EXTERIOR_STATE_COUNT);

		copyBeData(packedRegion);

		occlusion.prepare();
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
			final int packedXyz5 = regionIndexToXyz5(cacheIndex);
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
				final int packedXyz5 = regionIndexToXyz5(cacheIndex);
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

		final int result = ChunkColorCache.get(getChunk(x >> 4, z >> 4)).getColor(x, blockPos.getY(), z, colorResolver);

		return result;
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
