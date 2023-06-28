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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

import grondag.canvas.light.color.LightRegionData.Encoding;

// TODO: cluster slab allocation? -> maybe unneeded now?
// TODO: a way to repopulate cluster if needed
public class LightRegion {
	private static class BVec {
		boolean r, g, b;

		BVec() {
			this.r = false;
			this.g = false;
			this.b = false;
		}

		boolean get(int i) {
			return switch (i) {
				case 0 -> r;
				case 1 -> g;
				case 2 -> b;
				default -> false;
			};
		}

		boolean any() {
			return r || g || b;
		}

		boolean all() {
			return r && g && b;
		}

		BVec not() {
			r = !r;
			g = !g;
			b = !b;
			return this;
		}

		void lessThan(short left, short right) {
			r = Elem.R.of(left) < Elem.R.of(right);
			g = Elem.G.of(left) < Elem.G.of(right);
			b = Elem.B.of(left) < Elem.B.of(right);
		}

		BVec lessThanMinusOne(short left, short right) {
			r = Elem.R.of(left) < Elem.R.of(right) - 1;
			g = Elem.G.of(left) < Elem.G.of(right) - 1;
			b = Elem.B.of(left) < Elem.B.of(right) - 1;
			return this;
		}

		BVec and(BVec other, BVec another) {
			r = other.r && another.r;
			g = other.g && another.g;
			b = other.b && another.b;
			return this;
		}
	}

	private static enum Side {
		DOWN(0, -1, 0, 1, Direction.DOWN),
		UP(0, 1, 0, 2, Direction.UP),
		NORTH(0, 0, -1, 3, Direction.NORTH),
		SOUTH(0, 0, 1, 4, Direction.SOUTH),
		WEST(-1, 0, 0, 5, Direction.WEST),
		EAST(1, 0, 0, 6, Direction.EAST);

		final int x, y, z, id;
		final Direction vanilla;
		Side opposite;
		final static int nullId = 0;

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

			for (Side side:Side.values()) {
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
	private final BVec less = new BVec();
	private final BlockPos.MutableBlockPos sourcePos = new BlockPos.MutableBlockPos();
	private final BlockPos.MutableBlockPos nodePos = new BlockPos.MutableBlockPos();
	private final LongArrayFIFOQueue incQueue = new LongArrayFIFOQueue();
	private final LongArrayFIFOQueue decQueue = new LongArrayFIFOQueue();

	// This is bad and defeats the point of Canvas multithreading, maybe
	private final LongPriorityQueue globalIncQueue = LongPriorityQueues.synchronize(new LongArrayFIFOQueue());
	private final LongPriorityQueue globalDecQueue = LongPriorityQueues.synchronize(new LongArrayFIFOQueue());

	private boolean needCheckEdges = true;

	LightRegion(BlockPos origin) {
		this.origin = origin.asLong();
		this.lightData = new LightRegionData(origin.getX(), origin.getY(), origin.getZ());
	}

	public void checkBlock(BlockPos pos, BlockState blockState) {
		if (!lightData.withinExtents(pos)) {
			return;
		}

		final short registeredLight = LightRegistry.get(blockState);
		final int index = lightData.indexify(pos);
		final short getLight = lightData.get(index);
		final boolean occluding = blockState.canOcclude();

		if (Encoding.isLightSource(registeredLight)) {
			if (getLight != registeredLight) {
				// replace light
				if (Encoding.isLightSource(getLight)) {
					lightData.put(index, (short) 0);
					Queues.enqueue(globalDecQueue, index, getLight);
				}

				// place light
				Queues.enqueue(globalIncQueue, index, registeredLight);
			}
		} else if (Encoding.isLightSource(getLight) || Encoding.isOccluding(getLight) != occluding) {
			// remove light or replace occluder
			lightData.put(index, registeredLight);
			Queues.enqueue(globalDecQueue, index, getLight);
		}
	}

	private boolean occludeSide(BlockState state, Side dir, BlockAndTintGetter view, BlockPos pos) {
		// vanilla checks state.useShapeForLightOcclusion() but here it's always false for some reason. this is fine...

		if (!state.canOcclude()) {
			return false;
		}

		return Shapes.faceShapeOccludes(Shapes.empty(), state.getFaceOcclusionShape(view, pos, dir.vanilla));
	}

	public boolean updateDecrease(BlockAndTintGetter blockView) {
		if (needCheckEdges) {
			checkEdges(blockView);
			needCheckEdges = false;
		}

		// faster exit when not necessary
		if (globalDecQueue.isEmpty()) {
			return false;
		}

		while (!globalDecQueue.isEmpty()) {
			decQueue.enqueue(globalDecQueue.dequeueLong());
		}

		final BVec removeFlag = new BVec();
		final BVec removeMask = new BVec();

		int decCount = 0;
		boolean accessedNeighborDecrease = false;

		while(!decQueue.isEmpty()) {
			decCount++;

			final long entry = decQueue.dequeueLong();
			final int index = Queues.index(entry);
			final short sourcePrevLight = Queues.light(entry);
			final int from = Queues.from(entry);

			// only remove elements that are less than 1 (zero)
			final short sourceCurrentLight = lightData.get(index);
			removeFlag.lessThan(sourceCurrentLight, (short) 0x1110);

			lightData.reverseIndexify(index, sourcePos);

			final BlockState sourceState = blockView.getBlockState(sourcePos);

			for (var side:Side.values()) {
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
				short nodeLight = dataAccess.get(nodeIndex);

				if (Encoding.pure(nodeLight) == 0) {
					continue;
				}

				final BlockState nodeState = blockView.getBlockState(nodePos);

				// check neighbor occlusion for decrease
				if (!Encoding.isLightSource(nodeLight) && occludeSide(nodeState, side.opposite, blockView, nodePos)) {
					continue;
				}

				less.lessThan(nodeLight, sourcePrevLight);

				// only propagate removal according to removeFlag
				removeMask.and(less, removeFlag);

				boolean restoreLightSource = removeMask.any() && Encoding.isLightSource(nodeLight);

				if (removeMask.any()) {
					int mask = 0;

					if (removeMask.r) {
						mask |= Elem.R.mask;
					}

					if (removeMask.g) {
						mask |= Elem.G.mask;
					}

					if (removeMask.b) {
						mask |= Elem.B.mask;
					}

					final short resultLight = (short) (nodeLight & ~(mask));
					dataAccess.put(nodeIndex, resultLight);

					Queues.enqueue(decreaseQueue, nodeIndex, nodeLight, side);

					nodeLight = resultLight;

					// restore obliterated light source
					if (restoreLightSource) {
						// defer putting light source as to not mess with decrease step
						// take RGB of maximum and Alpha of registered
						final short registered = LightRegistry.get(nodeState);
						nodeLight = Elem.maxRGB(nodeLight, registered, Elem.A.of(registered));
					}

					accessedNeighborDecrease = accessedNeighborDecrease || isNeighbor;
				}

				if (removeMask.and(less.not(), removeFlag).any() || restoreLightSource) {
					// increases queued in decrease may propagate to all directions as if a light source
					Queues.enqueue(increaseQueue, nodeIndex, nodeLight);
				}
			}
		}

		if (decCount > 0) {
			lightData.markAsDirty();
		}

		return accessedNeighborDecrease;
	}

	public void updateIncrease(BlockAndTintGetter blockView) {
		while (!globalIncQueue.isEmpty()) {
			incQueue.enqueue(globalIncQueue.dequeueLong());
		}

		int incCount = 0;

		while (!incQueue.isEmpty()) {
			incCount++;

			final long entry = incQueue.dequeueLong();
			final int index = Queues.index(entry);
			final short recordedLight = Queues.light(entry);
			final int from = Queues.from(entry);

			short sourceLight = lightData.get(index);

			if (sourceLight != recordedLight) {
				if (Encoding.isLightSource(recordedLight)) {
					sourceLight = Elem.maxRGB(sourceLight, recordedLight, Elem.A.of(recordedLight));
					lightData.put(index, sourceLight);
				} else {
					continue;
				}
			}

			lightData.reverseIndexify(index, sourcePos);

			final BlockState sourceState = blockView.getBlockState(sourcePos);

			for (var side:Side.values()) {
				if (side.id == from) {
					continue;
				}

				// check self occlusion for increase
				if (!Encoding.isLightSource(sourceLight) && occludeSide(sourceState, side, blockView, sourcePos)) {
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
				final BlockState nodeState = blockView.getBlockState(nodePos);

				// check neighbor occlusion for increase
				if (occludeSide(nodeState, side.opposite, blockView, nodePos)) {
					continue;
				}

				if (less.lessThanMinusOne(nodeLight, sourceLight).any()) {
					short resultLight = nodeLight;

					if (less.r) {
						resultLight = Elem.R.replace(resultLight, (short) (Elem.R.of(sourceLight) - 1));
					}

					if (less.g) {
						resultLight = Elem.G.replace(resultLight, (short) (Elem.G.of(sourceLight) - 1));
					}

					if (less.b) {
						resultLight = Elem.B.replace(resultLight, (short) (Elem.B.of(sourceLight) - 1));
					}

					dataAccess.put(nodeIndex, resultLight);
					Queues.enqueue(increaseQueue, nodeIndex, resultLight, side);
				}
			}
		}

		if (incCount > 0) {
			lightData.markAsDirty();
		}
	}

	private void checkEdgeBlock(LightRegion neighbor, BlockPos.MutableBlockPos sourcePos, BlockPos.MutableBlockPos targetPos, Side side, BlockAndTintGetter blockView) {
		final int sourceIndex = neighbor.lightData.indexify(sourcePos);
		final short sourceLight = neighbor.lightData.get(sourceIndex);
		final BlockState sourceState = blockView.getBlockState(sourcePos);

		if (Encoding.pure(sourceLight) != 0) {
			// TODO: generalize for all increase process, with check-neighbor flag
			// check self occlusion for increase
			if (!Encoding.isLightSource(sourceLight) && occludeSide(sourceState, side, blockView, sourcePos)) {
				return;
			}

			final int targetIndex = lightData.indexify(targetPos);
			final short targetLight = lightData.get(targetIndex);
			final BlockState nodeState = blockView.getBlockState(targetPos);

			// check neighbor occlusion for increase
			if (occludeSide(nodeState, side.opposite, blockView, targetPos)) {
				return;
			}

			if (less.lessThanMinusOne(targetLight, sourceLight).any()) {
				short resultLight = targetLight;

				if (less.r) {
					resultLight = Elem.R.replace(resultLight, (short) (Elem.R.of(sourceLight) - 1));
				}

				if (less.g) {
					resultLight = Elem.G.replace(resultLight, (short) (Elem.G.of(sourceLight) - 1));
				}

				if (less.b) {
					resultLight = Elem.B.replace(resultLight, (short) (Elem.B.of(sourceLight) - 1));
				}

				lightData.put(targetIndex, resultLight);
				Queues.enqueue(incQueue, targetIndex, resultLight, side);
			}
		}
	}

	private void checkEdges(BlockAndTintGetter blockView) {
		final int size = LightRegionData.Const.WIDTH;
		final BlockPos originPos = BlockPos.of(origin);
		final BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();
		final BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();
		final int[] searchOffsets = new int[]{-1, size};
		final int[] targetOffsets = new int[]{0, size - 1};

		for (int i = 0; i < searchOffsets.length; i ++) {
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
					checkEdgeBlock(neighbor, searchPos, targetPos, side, blockView);
				}
			}
		}

		// TODO: generalize with Axis parameter
		for (int i = 0; i < searchOffsets.length; i ++) {
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
					checkEdgeBlock(neighbor, searchPos, targetPos, side, blockView);
				}
			}
		}

		for (int i = 0; i < searchOffsets.length; i ++) {
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
					checkEdgeBlock(neighbor, searchPos, targetPos, side, blockView);
				}
			}
		}
	}

	public void close() {
		if (!lightData.isClosed()) {
			lightData.close();
		}
	}

	public boolean isClosed() {
		return lightData == null || lightData.isClosed();
	}
}
