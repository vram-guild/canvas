package grondag.canvas.light.color;

import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import io.vram.frex.api.light.ItemLight;

import grondag.canvas.CanvasMod;
import grondag.canvas.light.color.LightSectionData.Elem;

public class LightPropagation {
	final static BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();
	private static Int2ShortOpenHashMap lights = new Int2ShortOpenHashMap();
	private static LongArrayFIFOQueue incQueue = new LongArrayFIFOQueue();
	private static LongArrayFIFOQueue decQueue = new LongArrayFIFOQueue();
	private static boolean dirty = false;
	private static Block debugBlock;
	private static int debugCountThread = 0;

	static {
		lights.defaultReturnValue((short) 0);
	}

	public static synchronized void onStartBuildTerrain() {
		debugCountThread++;
	}

	public static synchronized void onBuildTerrain(BlockPos pos, final int lightEmission, Block block, boolean occluding) {
		if (!LightDebug.debugData.withinExtents(pos)) {
			return;
		}

		debugBlock = block;

		short light = 0;

		if (lightEmission > 0) {
			light = lights.get(block.hashCode());

			if (light == 0) {
				// PERF: modify ItemLight API or make new API that doesn't need ItemStack
				// TODO: ItemLight API isn't suitable for this anyway since we rely on blockstates not blocks/items
				final ItemStack stack = new ItemStack(block, 1);
				final ItemLight itemLight = ItemLight.get(stack);

				if (itemLight != null) {
					final int r = (int) (lightEmission * itemLight.red());
					final int g = (int) (lightEmission * itemLight.green());
					final int b = (int) (lightEmission * itemLight.blue());
					light = LightSectionData.encodeRgba(r, g, b, 0);
				} else {
					light = LightSectionData.encodeRgba(lightEmission, lightEmission, lightEmission, 0);
				}

				lights.put(block.hashCode(), lightSource(light));
			}
		}

		evaluateForQueue(pos, light, occluding);
	}

	public static synchronized void onFinishedBuildTerrain() {
		debugCountThread --;

		if (dirty && debugCountThread == 0) {
			dirty = false;
			// TODO: there are multiple worker threads. the queue can't handle that yet
			// TODO: still not working
			processQueues();
			CanvasMod.LOG.info("Uploading texture");
			RenderSystem.recordRenderCall(() -> LightDebug.debugData.upload());
		}
	}

	private static void evaluateForQueue(BlockPos pos, short light, boolean occluding) {
		final int index = LightDebug.debugData.indexify(pos);
		final short getLight = LightDebug.debugData.get(index);

		lessThan(getLight, light);
		// greaterThan(getLight, light);

		// short incLight = 0;
		// short incLightMask = 0;
		//
		// short decLight = 0;
		// short decLightMask = 0;
		//
		// final Elem[] elems = Elem.values();
		//
		// for (int i = 0; i < 3; i++) {
		// 	if (inc.get(i)) {
		// 		incLight |= elems[i].of(light);
		// 		incLightMask |= elems[i].mask;
		// 	} else if (dec.get(i)) {
		// 		decLight |= elems[i].of(light);
		// 		decLightMask |= elems[i].mask;
		// 	}
		// }
		//
		// final short putLight = (short) (((getLight & ~incLightMask) & ~decLightMask) | incLight | decLight);

		if (less.any()) {
			// int[] xyz = new int[3];
			// LightDebug.debugData.reverseIndexify(index, xyz);
			// CanvasMod.LOG.info("test reverse index : " + pos + "/" + xyz[0] + "," + xyz[1] + "," + xyz[2]);
			LightDebug.debugData.put(index, light);
			enqueue(incQueue, index, light);
			CanvasMod.LOG.info("Add light at " + pos + " light is (get,put) "
					+ Elem.text(getLight) + "," + Elem.text(light) + " block: " + debugBlock);
		} else if (light == 0 && (isLightSource(getLight) || isOccluding(getLight) || occluding)) {
			LightDebug.debugData.put(index, occluding((short) 0, occluding));
			enqueue(decQueue, index, getLight);
			CanvasMod.LOG.info("Remove light at " + pos + " light is (get,put) "
					+ Elem.text(getLight) + "," + Elem.text(light) + " block: " + debugBlock);
		}
	}

	private static void enqueue(LongArrayFIFOQueue queue, long index, long light) {
		dirty = true;
		queue.enqueue((index << 16L) | light & 0xffffL);
	}

	private static void processQueues() {
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

				if (pure(nodeLight) == 0) {
					continue;
				}

				// this is problematic, might result in residual light unless we know the light source color
				// if (isLightSource(nodeLight)) {
				// 	continue;
				// }

				lessThan(nodeLight, sourcePrevLight);

				if (less.any()) {
					int mask = 0;

					if (less.r) {
						mask |= Elem.R.mask;
						// LightDebug.debugData.put(nodeIndex, Elem.R, 0);
					}

					if (less.g) {
						mask |= Elem.G.mask;
						// LightDebug.debugData.put(nodeIndex, Elem.G, 0);
					}

					if (less.b) {
						mask |= Elem.B.mask;
						// LightDebug.debugData.put(nodeIndex, Elem.B, 0);
					}

					final short nodeAfterLight = (short) (nodeLight & ~(mask));
					LightDebug.debugData.put(nodeIndex, nodeAfterLight);

					// final short nodeAfterLight = LightDebug.debugData.get(nodeIndex);

					enqueue(decQueue, nodeIndex, nodeLight);

					nodeLight = nodeAfterLight;
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

			if (sourceLight != recordedLight) {
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

				if (isOccluding(nodeLight)) {
					continue;
				}

				// CanvasMod.LOG.info("current/neighbor index " + index + "/" + nodeIndex);

				lessThanMinusOne(nodeLight, sourceLight);

				if (less.any()) {
					// TODO: optimize
					if (less.r) {
						LightDebug.debugData.put(nodeIndex, Elem.R, (Elem.R.of(sourceLight) - 1));
					}

					if (less.g) {
						LightDebug.debugData.put(nodeIndex, Elem.G, (Elem.G.of(sourceLight) - 1));
					}

					if (less.b) {
						LightDebug.debugData.put(nodeIndex, Elem.B, (Elem.B.of(sourceLight) - 1));
					}

					// TODO: optimize
					final short nodeAfterLight = LightDebug.debugData.get(nodeIndex);

					// CanvasMod.LOG.info("updating neighbor to: " + nodeX + "," + nodeY + "," + nodeZ + "," + Elem.text(nodeAfterLight));

					enqueue(incQueue, nodeIndex, nodeAfterLight);
				}
			}
		}

		dirty = false;

		CanvasMod.LOG.info("Processed queues! Count: inc,dec " + debugMaxInc + "," + debugMaxDec);
	}

	private static boolean isLightSource(short light) {
		return (light & 0b1) != 0;
	}

	private static boolean isOccluding(short light) {
		return (light & 0b10) != 0;
	}

	private static short lightSource(short light) {
		return (short) (light | 0b1);
	}

	private static short occluding(short light, boolean occluding) {
		return occluding ? (short) (light | 0b10) : light;
	}

	private static short pure(short light) {
		return (short) (light & 0xfff0);
	}

	private static class BVec {
		boolean r, g, b;

		public BVec() {
			this.r = false;
			this.g = false;
			this.b = false;
			// this.a = false;
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
	}

	private static BVec less = new BVec();
	private static BVec greater = new BVec();

	private static void lessThan(short left, short right) {
		less.r = Elem.R.of(left) < Elem.R.of(right);
		less.g = Elem.G.of(left) < Elem.G.of(right);
		less.b = Elem.B.of(left) < Elem.B.of(right);
	}

	private static void lessThanMinusOne(short left, short right) {
		less.r = Elem.R.of(left) < Elem.R.of(right) - 1;
		less.g = Elem.G.of(left) < Elem.G.of(right) - 1;
		less.b = Elem.B.of(left) < Elem.B.of(right) - 1;
	}

	private static void equalsMinusOne(short left, short right) {
		less.r = Elem.R.of(left) == Elem.R.of(right) - 1;
		less.g = Elem.G.of(left) == Elem.G.of(right) - 1;
		less.b = Elem.B.of(left) == Elem.B.of(right) - 1;
	}

	private static void greaterThanOrEqual(short left, short right) {
		greater.r = Elem.R.of(left) >= Elem.R.of(right);
		greater.g = Elem.G.of(left) >= Elem.G.of(right);
		greater.b = Elem.B.of(left) >= Elem.B.of(right);
	}
}
