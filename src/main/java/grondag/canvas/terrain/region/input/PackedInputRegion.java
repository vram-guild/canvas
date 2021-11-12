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
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.interiorIndex;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import io.vram.frex.api.world.BlockEntityRenderData;
import io.vram.frex.api.world.RenderRegionBakeListener;
import io.vram.frex.impl.world.ChunkRenderConditionContext;

import grondag.canvas.perf.ChunkRebuildCounters;

/**
 * Serves as a container to capture world state data on the main thread as quickly as possible
 * for later consumption on possibly non-render threads for terrain render region rebuild.
 *
 * <p>Also serves as a state indicator for rebuild activity.
 */
public class PackedInputRegion extends AbstractInputRegion {
	private static final ArrayBlockingQueue<PackedInputRegion> POOL = new ArrayBlockingQueue<>(256);

	public final ObjectArrayList<BlockEntity> blockEntities = new ObjectArrayList<>();
	public final ChunkRenderConditionContext bakeListenerContext = new ChunkRenderConditionContext();

	final BlockState[] states = new BlockState[EXTERIOR_STATE_COUNT];
	final ShortArrayList renderDataPos = new ShortArrayList();
	final ObjectArrayList<Object> renderData = new ObjectArrayList<>();
	final ShortArrayList blockEntityPos = new ShortArrayList();

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

		final ChunkRenderConditionContext bakeListenerContext = this.bakeListenerContext.prepare(world, originX, originY, originZ);
		RenderRegionBakeListener.prepareInvocations(bakeListenerContext, bakeListenerContext.listeners);

		final PackedInputRegion result;

		if (mainChunk.isEmpty() && bakeListenerContext.listeners.isEmpty()) {
			release();
			result = SignalInputRegion.EMPTY;
		} else {
			// WIP: move this to input region?
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

			result = this;
		}

		if (ChunkRebuildCounters.ENABLED) {
			ChunkRebuildCounters.completeCopy();
		}

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

	public void release() {
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
