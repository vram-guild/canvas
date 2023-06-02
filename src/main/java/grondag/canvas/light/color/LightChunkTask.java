package grondag.canvas.light.color;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import grondag.canvas.CanvasMod;
import grondag.canvas.light.color.LightSectionData.Elem;
import grondag.canvas.light.color.LightSectionData.Encoding;

public class LightChunkTask {
	private static class BVec {
		boolean r, g, b;

		public BVec() {
			this.r = false;
			this.g = false;
			this.b = false;
		}

		public boolean get(int i) {
			return switch (i) {
				case 0 -> r;
				case 1 -> g;
				case 2 -> b;
				default -> false;
			};
		}

		public boolean any() {
			return r || g || b;
		}

		public boolean all() {
			return r && g && b;
		}

		private void lessThan(short left, short right) {
			r = Elem.R.of(left) < Elem.R.of(right);
			g = Elem.G.of(left) < Elem.G.of(right);
			b = Elem.B.of(left) < Elem.B.of(right);
		}

		private void lessThanMinusOne(short left, short right) {
			r = Elem.R.of(left) < Elem.R.of(right) - 1;
			g = Elem.G.of(left) < Elem.G.of(right) - 1;
			b = Elem.B.of(left) < Elem.B.of(right) - 1;
		}
	}

	private final BVec less = new BVec();
	private static final LongArrayFIFOQueue incQueue = new LongArrayFIFOQueue();
	private static final LongArrayFIFOQueue decQueue = new LongArrayFIFOQueue();

	private static void enqueue(LongArrayFIFOQueue queue, long index, long light) {
		queue.enqueue((index << 16L) | light & 0xffffL);
	}

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
			enqueue(incQueue, index, light);
			CanvasMod.LOG.info("Add light at " + pos + " light is (get,put) " + Elem.text(getLight) + "," + Elem.text(light) + " block: " + blockState);
		} else if (light == 0 && (Encoding.isLightSource(getLight) || (Encoding.isOccluding(getLight) != occluding))) {
			LightDebug.debugData.put(index, Encoding.encodeLight(0, false, occluding));
			enqueue(decQueue, index, getLight);
			CanvasMod.LOG.info("Remove light at " + pos + " light is (get,put) " + Elem.text(getLight) + "," + Elem.text(light) + " block: " + blockState);
		}
	}

	public void propagateLight() {
		if (incQueue.isEmpty() && decQueue.isEmpty()) {
			CanvasMod.LOG.info("Nothing to process!");
			return;
		}

		CanvasMod.LOG.info("Processing queues.. inc,dec " + incQueue.size() + "," + decQueue.size());

		int[] pos = new int[3];
		int debugMaxDec = 0;
		int debugMaxInc = 0;

		while(!decQueue.isEmpty()) {
			debugMaxDec++;

			final long entry = decQueue.dequeueLong();
			final int index = (int) (entry >> 16L);
			final short sourcePrevLight = (short) entry;
			LightDebug.debugData.reverseIndexify(index, pos);

			for (var d: Direction.values()) {
				final int nodeX = pos[0] + d.getStepX();
				final int nodeY = pos[1] + d.getStepY();
				final int nodeZ = pos[2] + d.getStepZ();

				if (!LightDebug.debugData.withinExtents(nodeX, nodeY, nodeZ)) {
					continue;
				}

				final int nodeIndex = LightDebug.debugData.indexify(nodeX, nodeY, nodeZ);
				short nodeLight = LightDebug.debugData.get(nodeIndex);

				if (Encoding.pure(nodeLight) == 0) {
					continue;
				}

				// this is problematic, might result in residual light unless we know the light source color
				// if (isLightSource(nodeLight)) {
				// 	continue;
				// }

				less.lessThan(nodeLight, sourcePrevLight);

				if (less.any()) {
					int mask = 0;

					if (less.r) {
						mask |= Elem.R.mask;
					}

					if (less.g) {
						mask |= Elem.G.mask;
					}

					if (less.b) {
						mask |= Elem.B.mask;
					}

					final short resultLight = (short) (nodeLight & ~(mask));
					LightDebug.debugData.put(nodeIndex, resultLight);

					enqueue(decQueue, nodeIndex, nodeLight);

					nodeLight = resultLight;
				}

				if (!less.all()) {
					// TODO: queue but only if it's a neighboring chunk?? nah that'd be wrong
					enqueue(incQueue, nodeIndex, nodeLight);
				}
			}
		}

		while (!incQueue.isEmpty()) {
			debugMaxInc++;

			final long entry = incQueue.dequeueLong();
			final int index = (int) (entry >> 16L);
			final short recordedLight = (short) entry;
			final short sourceLight = LightDebug.debugData.get(index);

			if (Encoding.pure(sourceLight) != Encoding.pure(recordedLight)) {
				continue;
			}

			LightDebug.debugData.reverseIndexify(index, pos);

			for (var d: Direction.values()) {
				final int nodeX = pos[0] + d.getStepX();
				final int nodeY = pos[1] + d.getStepY();
				final int nodeZ = pos[2] + d.getStepZ();

				// CanvasMod.LOG.info("increase at " + nodeX + "," + nodeY + "," + nodeZ);

				if (!LightDebug.debugData.withinExtents(nodeX, nodeY, nodeZ)) {
					continue;
				}

				final int nodeIndex = LightDebug.debugData.indexify(nodeX, nodeY, nodeZ);
				final short nodeLight = LightDebug.debugData.get(nodeIndex);

				if (Encoding.isOccluding(nodeLight)) {
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

					enqueue(incQueue, nodeIndex, resultLight);
				}
			}
		}

		CanvasMod.LOG.info("Processed queues! Count: inc,dec " + debugMaxInc + "," + debugMaxDec);
		CanvasMod.LOG.info("Uploading texture");
		RenderSystem.recordRenderCall(() -> LightDebug.debugData.upload());
	}
}
