package grondag.canvas.light.color;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

import grondag.canvas.CanvasMod;
import grondag.canvas.light.color.LightSectionData.Elem;
import grondag.canvas.light.color.LightSectionData.Encoding;

// TODO: edge chunks
// TODO: occlusion shapes
// TODO: re-propagate light sources upon removal
public class LightChunkTask {
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

		void lessThan(short left, short right) {
			r = Elem.R.of(left) < Elem.R.of(right);
			g = Elem.G.of(left) < Elem.G.of(right);
			b = Elem.B.of(left) < Elem.B.of(right);
		}

		void lessThanMinusOne(short left, short right) {
			r = Elem.R.of(left) < Elem.R.of(right) - 1;
			g = Elem.G.of(left) < Elem.G.of(right) - 1;
			b = Elem.B.of(left) < Elem.B.of(right) - 1;
		}

		void and(BVec other, BVec another) {
			r = other.r && another.r;
			g = other.g && another.g;
			b = other.b && another.b;
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
	}

	private static class Queues {
		static void enqueue(LongArrayFIFOQueue queue, long index, long light) {
			queue.enqueue(((long) Side.nullId << 48L) | (index << 16L) | light & 0xffffL);
		}

		static void enqueue(LongArrayFIFOQueue queue, long index, long light, Side target) {
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

	private final BVec less = new BVec();
	private final BlockPos.MutableBlockPos sourcePos = new BlockPos.MutableBlockPos();
	private final BlockPos.MutableBlockPos nodePos = new BlockPos.MutableBlockPos();
	private static final LongArrayFIFOQueue incQueue = new LongArrayFIFOQueue();
	private static final LongArrayFIFOQueue decQueue = new LongArrayFIFOQueue();

	public void checkBlock(BlockPos pos, BlockState blockState) {
		if (!LightDebug.debugData.withinExtents(pos)) {
			return;
		}

		short light = 0;

		if (blockState.getLightEmission() > 0) {
			light = LightRegistry.get(blockState);
		}

		final int index = LightDebug.debugData.indexify(pos);
		final short getLight = LightDebug.debugData.get(index);
		final boolean occluding = blockState.canOcclude();

		less.lessThan(getLight, light);

		if (less.any()) {
			LightDebug.debugData.put(index, light);
			Queues.enqueue(incQueue, index, light);
			CanvasMod.LOG.info("Add light at " + pos + " light is (get,put) " + Elem.text(getLight) + "," + Elem.text(light) + " block: " + blockState);
		} else if (light == 0 && (Encoding.isLightSource(getLight) || (Encoding.isOccluding(getLight) != occluding))) {
			LightDebug.debugData.put(index, Encoding.encodeLight(0, false, occluding));
			Queues.enqueue(decQueue, index, getLight);
			CanvasMod.LOG.info("Remove light at " + pos + " light is (get,put) " + Elem.text(getLight) + "," + Elem.text(light) + " block: " + blockState);
		}
	}

	private boolean occludeSide(BlockState state, Side dir, BlockAndTintGetter view, BlockPos pos) {
		if (!state.canOcclude()) {
			return false;
		}

		return Shapes.faceShapeOccludes(Shapes.empty(), state.getFaceOcclusionShape(view, pos, dir.vanilla));
	}

	public void propagateLight(BlockAndTintGetter blockView) {
		if (incQueue.isEmpty() && decQueue.isEmpty()) {
			CanvasMod.LOG.info("Nothing to process!");
			return;
		}

		CanvasMod.LOG.info("Processing queues.. inc,dec " + incQueue.size() + "," + decQueue.size());

		final BVec removeFlag = new BVec();
		final BVec removeMask = new BVec();

		int debugMaxDec = 0;
		int debugMaxInc = 0;

		while(!decQueue.isEmpty()) {
			debugMaxDec++;

			final long entry = decQueue.dequeueLong();
			final int index = Queues.index(entry);
			final short sourcePrevLight = Queues.light(entry);
			final int from = Queues.from(entry);

			// only remove elements that are less than 1 (zero)
			final short sourceCurrentLight = LightDebug.debugData.get(index);
			removeFlag.lessThan(sourceCurrentLight, (short) 0x1110);

			LightDebug.debugData.reverseIndexify(index, sourcePos);

			final BlockState sourceState = blockView.getBlockState(sourcePos);

			for (var side:Side.values()) {
				if (side.id == from) {
					continue;
				}

				// check self occlusion for decrease
				if (!Encoding.isOccluding(sourceCurrentLight) && occludeSide(sourceState, side, blockView, sourcePos)) {
					continue;
				}

				nodePos.setWithOffset(sourcePos, side.x, side.y, side.z);

				// TODO: remove
				if (!LightDebug.debugData.withinExtents(nodePos)) {
					continue;
				}

				final int nodeIndex = LightDebug.debugData.indexify(nodePos);
				short nodeLight = LightDebug.debugData.get(nodeIndex);

				if (Encoding.pure(nodeLight) == 0) {
					continue;
				}

				final BlockState nodeState = blockView.getBlockState(nodePos);

				// check neighbor occlusion for decrease
				if (occludeSide(nodeState, side.opposite, blockView, nodePos)) {
					continue;
				}

				less.lessThan(nodeLight, sourcePrevLight);

				// only propagate removal according to removeFlag
				removeMask.and(less, removeFlag);

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
					LightDebug.debugData.put(nodeIndex, resultLight);

					Queues.enqueue(decQueue, nodeIndex, nodeLight, side);

					nodeLight = resultLight;
				}

				if (!less.all()) {
					// increases queued in decrease may propagate to all directions as if a light source
					Queues.enqueue(incQueue, nodeIndex, nodeLight);
				}
			}
		}

		while (!incQueue.isEmpty()) {
			debugMaxInc++;

			final long entry = incQueue.dequeueLong();
			final int index = Queues.index(entry);
			final short recordedLight = Queues.light(entry);
			final int from = Queues.from(entry);

			final short sourceLight = LightDebug.debugData.get(index);

			if (Encoding.pure(sourceLight) != Encoding.pure(recordedLight)) {
				continue;
			}

			LightDebug.debugData.reverseIndexify(index, sourcePos);

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

				// CanvasMod.LOG.info("increase at " + nodeX + "," + nodeY + "," + nodeZ);

				// TODO: remove
				if (!LightDebug.debugData.withinExtents(nodePos)) {
					continue;
				}

				final int nodeIndex = LightDebug.debugData.indexify(nodePos);
				final short nodeLight = LightDebug.debugData.get(nodeIndex);
				final BlockState nodeState = blockView.getBlockState(nodePos);

				// check neighbor occlusion for increase
				if (occludeSide(nodeState, side.opposite, blockView, nodePos)) {
					continue;
				}

				// CanvasMod.LOG.info("current/neighbor index " + index + "/" + nodeIndex);

				less.lessThanMinusOne(nodeLight, sourceLight);

				if (less.any()) {
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

					LightDebug.debugData.put(nodeIndex, resultLight);

					// CanvasMod.LOG.info("updating neighbor to: " + nodeX + "," + nodeY + "," + nodeZ + "," + Elem.text(resultLight));

					Queues.enqueue(incQueue, nodeIndex, resultLight, side);
				}
			}
		}

		CanvasMod.LOG.info("Processed queues! Count: inc,dec " + debugMaxInc + "," + debugMaxDec);
		CanvasMod.LOG.info("Marking texture as dirty");
		LightDebug.debugData.markAsDirty();
	}
}
