/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.light.color;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongPriorityQueue;
import it.unimi.dsi.fastutil.longs.LongPriorityQueues;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

// TODO: cluster slab allocation? -> maybe unneeded now?
// TODO: a way to repopulate cluster if needed
class LightRegion implements LightRegionAccess {
	private enum Side {
		DOWN(0, -1, 0, 1, Direction.DOWN),
		UP(0, 1, 0, 2, Direction.UP),
		NORTH(0, 0, -1, 3, Direction.NORTH),
		SOUTH(0, 0, 1, 4, Direction.SOUTH),
		WEST(-1, 0, 0, 5, Direction.WEST),
		EAST(1, 0, 0, 6, Direction.EAST);

		final int x, y, z, id;
		final Direction vanilla;
		Side opposite;
		static final int nullId = 0;

		static {
			DOWN.opposite = UP;
			UP.opposite = DOWN;
			NORTH.opposite = SOUTH;
			SOUTH.opposite = NORTH;
			WEST.opposite = EAST;
			EAST.opposite = WEST;
		}

		Side(int x, int y, int z, int id, Direction vanilla) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.id = id;
			this.vanilla = vanilla;
		}

		static Side infer(BlockPos from, BlockPos to) {
			int x = to.getX() - from.getX();
			int y = to.getY() - from.getY();
			int z = to.getZ() - from.getZ();

			for (Side side : Side.values()) {
				if (side.x == x && side.y == y && side.z == z) {
					return side;
				}
			}

			// detects programming error (happened once)
			throw new IndexOutOfBoundsException("Can't infer side. From: " + from + ", To: " + to);
		}
	}

	private static class Queues {
		static void enqueue(LongPriorityQueue queue, long index, long light) {
			queue.enqueue(((long) Side.nullId << 48L) | (index << 16L) | light & 0xffffL);
		}

		static void enqueue(LongPriorityQueue queue, long index, long light, Side target) {
			queue.enqueue(((long) target.opposite.id << 48L) | (index << 16L) | light & 0xffffL);
		}

		static int from(long entry) {
			return (int) (entry >> 48L);
		}

		static int index(long entry) {
			return (int) (entry >> 16L);
		}

		static short light(long entry) {
			return (short) (entry);
		}
	}

	final LightRegionData lightData;
	final long origin;
	final BlockPos originPos;
	private final LightOp.BVec less = new LightOp.BVec();
	private final BlockPos.MutableBlockPos sourcePos = new BlockPos.MutableBlockPos();
	private final BlockPos.MutableBlockPos nodePos = new BlockPos.MutableBlockPos();
	private final LongArrayFIFOQueue incQueue = new LongArrayFIFOQueue();
	private final LongArrayFIFOQueue decQueue = new LongArrayFIFOQueue();

	// PERF: since increase queue is only honored if it's "latest", it can be optimized with a Map
	private final LongPriorityQueue globalIncQueue = LongPriorityQueues.synchronize(new LongArrayFIFOQueue());
	private final LongPriorityQueue globalDecQueue = LongPriorityQueues.synchronize(new LongArrayFIFOQueue());

	short texAllocation = LightDataAllocator.EMPTY_ADDRESS;
	boolean hasData = false;
	private boolean needCheckEdges = true;

	LightRegion(BlockPos origin) {
		this.originPos = new BlockPos(origin);
		this.origin = origin.asLong();
		this.lightData = new LightRegionData(origin.getX(), origin.getY(), origin.getZ());
	}

	@Override
	public void checkBlock(BlockPos pos, @Nullable BlockState blockState) {
		if (!lightData.withinExtents(pos)) {
			return;
		}

		final int index = lightData.indexify(pos);
		final short registeredLight = LightDataManager.INSTANCE.lightLevel().getRegistered(pos, blockState);
		final boolean occluding = LightOp.occluder(registeredLight);

		final short getLight = lightData.get(index);
		final boolean emitting = LightOp.emitter(registeredLight);

		// Equivalent of "hard reset" on this particular block pos, so we want it to pass specific checks
		final boolean replaceEmitter = emitting && getLight != registeredLight;
		final boolean removeEmitter = LightOp.emitter(getLight);
		final boolean replaceOccluder = LightOp.occluder(getLight) != occluding;
		final boolean occludeLitBlock = LightOp.lit(getLight) && occluding;

		if (replaceEmitter || removeEmitter || replaceOccluder || occludeLitBlock) {
			// If emitter, light with be placed later so put 0 here for removal purpose. Otherwise, put occlusion data here
			lightData.put(index, emitting ? (short) 0 : registeredLight);

			// Removal queue, since we're doing "hard reset"
			Queues.enqueue(globalDecQueue, index, getLight);

			// Emission queue
			if (emitting) {
				Queues.enqueue(globalIncQueue, index, registeredLight);
			}

			// At this point, no light data yet, but we might have occlusion data
			if (LightDataManager.INSTANCE.useOcclusionData) {
				hasData = true;
			}
		}
	}

	private boolean urgent = false;

	@Override
	public void submitChecks() {
		if (!globalDecQueue.isEmpty() || !globalIncQueue.isEmpty()) {
			if (urgent) {
				LightDataManager.INSTANCE.publicUrgentUpdateQueue.add(origin);
			} else {
				LightDataManager.INSTANCE.publicUpdateQueue.add(origin);
			}
		}

		urgent = false;
	}

	@Override
	public void markUrgent() {
		urgent = true;
	}

	private boolean occludeSide(Side dir, LightLevelAccess lightLevel, BlockPos pos) {
		// vanilla checks state.useShapeForLightOcclusion() but here it's always false for some reason. this is fine...
		var state = lightLevel.level().getBlockState(pos);

		if (!state.canOcclude()) {
			return false;
		}

		return Shapes.faceShapeOccludes(Shapes.empty(), state.getFaceOcclusionShape(lightLevel.level(), pos, dir.vanilla));
	}

	void updateDecrease(LightLevelAccess lightLevel, LongPriorityQueue neighborDecreaseQueue, LongPriorityQueue neighborIncreaseQueue) {
		// faster exit when not necessary
		if (globalDecQueue.isEmpty()) {
			return;
		}

		while (!globalDecQueue.isEmpty()) {
			decQueue.enqueue(globalDecQueue.dequeueLong());
		}

		final LightOp.BVec removeFlag = new LightOp.BVec();
		final LightOp.BVec removeMask = new LightOp.BVec();

		boolean didUpdate = false;

		while (!decQueue.isEmpty()) {
			final long entry = decQueue.dequeueLong();
			final int index = Queues.index(entry);
			final short sourcePrevLight = Queues.light(entry);
			final int from = Queues.from(entry);

			// only remove elements that are less than 1 (zero)
			final short sourceCurrentLight = lightData.get(index);
			removeFlag.lessThan(sourceCurrentLight, (short) 0x1110);

			// NB: rarely happens, not worth the "if"
			// if (!removeFlag.any()) continue;

			lightData.reverseIndexify(index, sourcePos);

			// unused for some reason
			// final BlockState sourceState = blockView.getBlockState(sourcePos);

			for (var side : Side.values()) {
				if (side.id == from) {
					continue;
				}

				nodePos.setWithOffset(sourcePos, side.x, side.y, side.z);

				final LightRegionData dataAccess;
				final LongPriorityQueue increaseQueue;
				final LongPriorityQueue decreaseQueue;
				boolean isNeighbor = !lightData.withinExtents(nodePos);
				LightRegion neighbor = null;

				if (isNeighbor) {
					neighbor = LightDataManager.INSTANCE.getFromBlock(nodePos);

					if (neighbor == null || neighbor.isClosed()) {
						continue;
					}

					increaseQueue = neighbor.globalIncQueue;
					decreaseQueue = neighbor.globalDecQueue;
					dataAccess = neighbor.lightData;
				} else {
					increaseQueue = incQueue;
					decreaseQueue = decQueue;
					dataAccess = lightData;
				}

				final int nodeIndex = dataAccess.indexify(nodePos);
				final short nodeLight = dataAccess.get(nodeIndex);

				// Important: extremely high frequency redundancy filter (removes 99% of operations)
				if (!LightOp.lit(nodeLight)) continue;

				// check neighbor occlusion for decrease
				if (!LightOp.emitter(nodeLight) && occludeSide(side.opposite, lightLevel, nodePos)) {
					continue;
				}

				// only propagate removal according to removeFlag
				removeMask.and(less.lessThan(nodeLight, sourcePrevLight), removeFlag);

				final short registered = lightLevel.getRegistered(nodePos);
				final boolean restoreLightSource = removeMask.any() && LightOp.emitter(registered);
				final short repropLight;

				if (removeMask.any()) {
					final short resultLight = LightOp.remove(nodeLight, removeMask);

					// high frequency redundancy when removing next to a different colored light, low otherwise
					if (resultLight == nodeLight) continue;

					dataAccess.put(nodeIndex, resultLight);
					Queues.enqueue(decreaseQueue, nodeIndex, nodeLight, side);

					// congrats, the queued update was not redundant!
					didUpdate = true;

					if (isNeighbor) {
						neighborDecreaseQueue.enqueue(neighbor.origin);
					}

					// restore obliterated light source
					if (restoreLightSource) {
						// defer putting light source as to not mess with decrease step
						repropLight = registered;
					} else {
						repropLight = resultLight;
					}
				} else {
					repropLight = nodeLight;
				}

				if (removeMask.and(less.not(), removeFlag).any() || restoreLightSource) {
					// increases queued in decrease may propagate to all directions as if a light source
					Queues.enqueue(increaseQueue, nodeIndex, repropLight);

					if (isNeighbor) {
						neighborIncreaseQueue.enqueue(neighbor.origin);
					}
				}
			}
		}

		if (didUpdate) {
			lightData.markAsDirty();
			LightDataManager.INSTANCE.publicDrawQueue.add(origin);
		}
	}

	void updateIncrease(LightLevelAccess lightLevel, LongPriorityQueue neighborIncreaseQueue) {
		if (needCheckEdges) {
			needCheckEdges = false;
			checkEdges(lightLevel);
		}

		while (!globalIncQueue.isEmpty()) {
			incQueue.enqueue(globalIncQueue.dequeueLong());
		}

		while (!incQueue.isEmpty()) {
			final long entry = incQueue.dequeueLong();
			final int index = Queues.index(entry);
			final short recordedLight = Queues.light(entry);
			final int from = Queues.from(entry);

			final short getLight = lightData.get(index);
			final short sourceLight;

			if (getLight != recordedLight) {
				if (LightOp.emitter(recordedLight)) {
					lightData.reverseIndexify(index, sourcePos);

					if (lightLevel.getRegistered(sourcePos) != recordedLight) {
						continue;
					}

					// take max of current and recorded source
					sourceLight = LightOp.max(recordedLight, getLight);
					lightData.put(index, sourceLight);
				} else {
					continue;
				}
			} else {
				lightData.reverseIndexify(index, sourcePos);

				sourceLight = getLight;
			}

			for (var side : Side.values()) {
				if (side.id == from) {
					continue;
				}

				// check self occlusion for increase
				if (!LightOp.emitter(sourceLight) && occludeSide(side, lightLevel, sourcePos)) {
					continue;
				}

				nodePos.setWithOffset(sourcePos, side.x, side.y, side.z);

				final LightRegionData dataAccess;
				final LongPriorityQueue increaseQueue;
				boolean isNeighbor = !lightData.withinExtents(nodePos);
				LightRegion neighbor = null;

				if (isNeighbor) {
					neighbor = LightDataManager.INSTANCE.getFromBlock(nodePos);

					if (neighbor == null || neighbor.isClosed()) {
						continue;
					}

					increaseQueue = neighbor.globalIncQueue;
					dataAccess = neighbor.lightData;
				} else {
					increaseQueue = incQueue;
					dataAccess = lightData;
				}

				final int nodeIndex = dataAccess.indexify(nodePos);
				final short nodeLight = dataAccess.get(nodeIndex);

				// check neighbor occlusion for increase
				if (occludeSide(side.opposite, lightLevel, nodePos)) {
					continue;
				}

				if (less.lessThanMinusOne(nodeLight, sourceLight).any()) {
					final short resultLight = LightOp.replaceMinusOne(nodeLight, sourceLight, less);
					dataAccess.put(nodeIndex, resultLight);
					Queues.enqueue(increaseQueue, nodeIndex, resultLight, side);

					if (isNeighbor) {
						neighborIncreaseQueue.enqueue(neighbor.origin);
					}
				}
			}
		}

		// If we reached here we should draw
		hasData = true;
		lightData.markAsDirty();
		LightDataManager.INSTANCE.publicDrawQueue.add(origin);
	}

	private void checkEdgeBlock(LightRegion neighbor, BlockPos.MutableBlockPos sourcePos, BlockPos.MutableBlockPos targetPos, Side side, LightLevelAccess lightLevel) {
		final int sourceIndex = neighbor.lightData.indexify(sourcePos);
		final short sourceLight = neighbor.lightData.get(sourceIndex);

		if (LightOp.lit(sourceLight)) {
			// TODO: generalize for all increase process, with check-neighbor flag
			// check self occlusion for increase
			if (!LightOp.emitter(sourceLight) && occludeSide(side, lightLevel, sourcePos)) {
				return;
			}

			final int targetIndex = lightData.indexify(targetPos);
			final short targetLight = lightData.get(targetIndex);

			// check neighbor occlusion for increase
			if (occludeSide(side.opposite, lightLevel, targetPos)) {
				return;
			}

			if (less.lessThanMinusOne(targetLight, sourceLight).any()) {
				final short resultLight = LightOp.replaceMinusOne(targetLight, sourceLight, less);
				lightData.put(targetIndex, resultLight);
				Queues.enqueue(incQueue, targetIndex, resultLight, side);
			}
		}
	}

	private void checkEdges(LightLevelAccess lightLevel) {
		final int size = LightRegionData.Const.WIDTH;
		final BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();
		final BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();
		final int[] searchOffsets = new int[]{-1, size};
		final int[] targetOffsets = new int[]{0, size - 1};

		for (int i = 0; i < searchOffsets.length; i++) {
			final int x = searchOffsets[i];
			final int xTarget = targetOffsets[i];

			searchPos.setWithOffset(originPos, x, 0, 0);
			targetPos.setWithOffset(originPos, xTarget, 0, 0);
			final Side side = Side.infer(searchPos, targetPos);
			final LightRegion neighbor = LightDataManager.INSTANCE.getFromBlock(searchPos);

			if (neighbor == null) {
				continue;
			}

			for (int y = 0; y < size; y++) {
				for (int z = 0; z < size; z++) {
					searchPos.setWithOffset(originPos, x, y, z);
					targetPos.setWithOffset(originPos, xTarget, y, z);
					checkEdgeBlock(neighbor, searchPos, targetPos, side, lightLevel);
				}
			}
		}

		// TODO: generalize with Axis parameter
		for (int i = 0; i < searchOffsets.length; i++) {
			final int y = searchOffsets[i];
			final int yTarget = targetOffsets[i];

			searchPos.setWithOffset(originPos, 0, y, 0);
			targetPos.setWithOffset(originPos, 0, yTarget, 0);
			final Side side = Side.infer(searchPos, targetPos);
			final LightRegion neighbor = LightDataManager.INSTANCE.getFromBlock(searchPos);

			if (neighbor == null) {
				continue;
			}

			for (int z = 0; z < size; z++) {
				for (int x = 0; x < size; x++) {
					searchPos.setWithOffset(originPos, x, y, z);
					targetPos.setWithOffset(originPos, x, yTarget, z);
					checkEdgeBlock(neighbor, searchPos, targetPos, side, lightLevel);
				}
			}
		}

		for (int i = 0; i < searchOffsets.length; i++) {
			final int z = searchOffsets[i];
			final int zTarget = targetOffsets[i];

			searchPos.setWithOffset(originPos, 0, 0, z);
			targetPos.setWithOffset(originPos, 0, 0, zTarget);
			final Side side = Side.infer(searchPos, targetPos);
			final LightRegion neighbor = LightDataManager.INSTANCE.getFromBlock(searchPos);

			if (neighbor == null) {
				continue;
			}

			for (int x = 0; x < size; x++) {
				for (int y = 0; y < size; y++) {
					searchPos.setWithOffset(originPos, x, y, z);
					targetPos.setWithOffset(originPos, x, y, zTarget);
					checkEdgeBlock(neighbor, searchPos, targetPos, side, lightLevel);
				}
			}
		}
	}

	public void close() {
		if (!lightData.isClosed()) {
			lightData.close();
		}
	}

	@Override
	public boolean isClosed() {
		return lightData == null || lightData.isClosed();
	}
}
