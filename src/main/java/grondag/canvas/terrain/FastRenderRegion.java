/*
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
 */

package grondag.canvas.terrain;

import static grondag.canvas.terrain.RenderRegionAddressHelper.EXTERIOR_CACHE_SIZE;
import static grondag.canvas.terrain.RenderRegionAddressHelper.INTERIOR_CACHE_SIZE;
import static grondag.canvas.terrain.RenderRegionAddressHelper.TOTAL_CACHE_SIZE;
import static grondag.canvas.terrain.RenderRegionAddressHelper.cacheIndexToXyz5;
import static grondag.canvas.terrain.RenderRegionAddressHelper.interiorIndex;

import java.util.Arrays;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.level.ColorResolver;

import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.terrain.ChunkPaletteCopier.PaletteCopy;
import grondag.canvas.terrain.occlusion.region.OcclusionRegion;

public class FastRenderRegion extends AbstractRenderRegion implements RenderAttachedBlockView {
	protected final BlockPos.Mutable searchPos = new BlockPos.Mutable();

	protected final Object[] renderData = new Object[INTERIOR_CACHE_SIZE];
	public final BlockEntity[] blockEntities = new BlockEntity[INTERIOR_CACHE_SIZE];

	private final BlockState[] states = new BlockState[TOTAL_CACHE_SIZE];
	// PERF: pack for reduced memory, better LOC
	private final int[] aoCache = new int[TOTAL_CACHE_SIZE];
	private final int[] lightCache = new int[TOTAL_CACHE_SIZE];


	public final TerrainRenderContext terrainContext;

	public FastRenderRegion(TerrainRenderContext terrainContext) {
		this.terrainContext = terrainContext;
	}

	public void prepare(ProtoRenderRegion protoRegion) {
		System.arraycopy(protoRegion.chunks, 0, chunks, 0, 16);
		System.arraycopy(EMPTY_BLOCK_ENTITIES, 0, blockEntities, 0, INTERIOR_CACHE_SIZE);
		System.arraycopy(EMPTY_RENDER_DATA, 0, renderData, 0, INTERIOR_CACHE_SIZE);
		System.arraycopy(EMPTY_AO_CACHE, 0, aoCache, 0, TOTAL_CACHE_SIZE);
		System.arraycopy(EMPTY_LIGHT_CACHE, 0, lightCache, 0, TOTAL_CACHE_SIZE);

		world = protoRegion.world;

		originX = protoRegion.originX;
		originY = protoRegion.originY;
		originZ = protoRegion.originZ;

		chunkBaseX = protoRegion.chunkBaseX;
		chunkBaseY = protoRegion.chunkBaseY;
		chunkBaseZ = protoRegion.chunkBaseZ;

		final PaletteCopy pc  = protoRegion.takePaletteCopy();

		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					states[interiorIndex(x, y, z)] = pc.apply(x | (y << 8) | (z << 4));
				}
			}
		}

		pc.release();

		System.arraycopy(protoRegion.states, 0, states, INTERIOR_CACHE_SIZE, EXTERIOR_CACHE_SIZE);

		copyBeData(protoRegion);

		occlusion.prepare();
	}

	private void copyBeData(ProtoRenderRegion protoRegion) {
		final ShortArrayList blockEntityPos = protoRegion.blockEntityPos;

		if( !blockEntityPos.isEmpty()) {
			final ObjectArrayList<BlockEntity> blockEntities = protoRegion.blockEntities;
			final int limit = blockEntityPos.size();

			for (int i = 0; i < limit; i++) {
				this.blockEntities[blockEntityPos.getShort(i)] = blockEntities.get(i);
			}
		}

		final ShortArrayList renderDataPos = protoRegion.renderDataPos;

		if( !renderDataPos.isEmpty()) {
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
	 * Assumes values 0-15
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
	public int getLightLevel(LightType type, BlockPos pos) {
		return world.getLightLevel(type, pos);
	}

	@Override
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
			final int packedXyz5 = cacheIndexToXyz5(cacheIndex);
			final int x = (packedXyz5 & 31) - 1 + originX;
			final int y = ((packedXyz5 >> 5) & 31) - 1 + originY;
			final int z = (packedXyz5 >> 10) - 1 + originZ;
			result = WorldRenderer.getLightmapCoordinates(world, state, searchPos.set(x, y, z));
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
		return WorldRenderer.getLightmapCoordinates(world, getBlockState(pos), pos);
	}

	// TODO: do anything with this?
	// Vanilla now computes diffuse shading at chunk bake time and consumes this value in AO calc
	@Override
	public float getBrightness(Direction direction, boolean shaded) {
		return world.getBrightness(direction, shaded);
	}

	public int cachedAoLevel(int cacheIndex) {
		int result = aoCache[cacheIndex];

		if (result == Integer.MAX_VALUE) {
			final BlockState state = states[cacheIndex];

			if(state.getLuminance() == 0) {
				final int packedXyz5 = cacheIndexToXyz5(cacheIndex);
				final int x = (packedXyz5 & 31) - 1 + originX;
				final int y = ((packedXyz5 >> 5) & 31) - 1 + originY;
				final int z = (packedXyz5 >> 10) - 1 + originZ;
				result = Math.round(255f * state.getAmbientOcclusionLightLevel(this, searchPos.set(x, y, z)));
			} else {
				result = 255;
			}

			aoCache[cacheIndex] = result;
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

	/** only valid for positions in render region, including exterior */
	public boolean isClosed(int cacheIndex) {
		return occlusion.isClosed(cacheIndex);
	}

	public final OcclusionRegion occlusion = new OcclusionRegion() {
		@Override
		protected BlockState blockStateAtIndex(int index) {
			return states[index];
		}

		@Override
		protected boolean closedAtRelativePos(BlockState blockState, int x, int y, int z) {
			return blockState.isOpaqueFullCube(world, searchPos.set(originX + x, originY + y, originZ + z));
		}
	};

	private static final int[] EMPTY_AO_CACHE = new int[TOTAL_CACHE_SIZE];
	private static final int[] EMPTY_LIGHT_CACHE = new int[TOTAL_CACHE_SIZE];
	private static final Object[] EMPTY_RENDER_DATA = new Object[INTERIOR_CACHE_SIZE];
	private static final BlockEntity[] EMPTY_BLOCK_ENTITIES = new BlockEntity[INTERIOR_CACHE_SIZE];

	static {
		Arrays.fill(EMPTY_AO_CACHE, Integer.MAX_VALUE);
		Arrays.fill(EMPTY_LIGHT_CACHE, Integer.MAX_VALUE);
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
}
