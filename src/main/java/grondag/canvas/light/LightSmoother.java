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

package grondag.canvas.light;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import grondag.canvas.terrain.region.input.InputRegion;

// TODO: look at VoxelShapes.method_1080 as a way to not propagate thru slabs
// Also BlockState.hasSidedTransparency seems promising

public class LightSmoother {
	public static final int OPAQUE = -1;
	private static final int BLUR_RADIUS = 2;
	private static final int MARGIN = BLUR_RADIUS + 2;
	private static final int POS_DIAMETER = 16 + MARGIN * 2;
	private static final int POS_COUNT = POS_DIAMETER * POS_DIAMETER * POS_DIAMETER;
	private static final int Y_INC = POS_DIAMETER;
	private static final int Z_INC = POS_DIAMETER * POS_DIAMETER;
	private static final ThreadLocal<Helper> helpers = ThreadLocal.withInitial(Helper::new);
	private static final int INNER_DIST = 28966; // fractional part of 0xFFFF
	private static final int OUTER_DIST = (0xFFFF - INNER_DIST) / 2;
	private static final int INNER_PLUS = INNER_DIST + OUTER_DIST;

	public static void computeSmoothedBrightness(InputRegion region) {
		final Helper help = helpers.get();
		final BlockPos.MutableBlockPos smoothPos = help.smoothPos;
		final int[] sky = help.a;
		final int[] block = help.b;

		final int minX = region.originX() - MARGIN;
		final int minY = region.originY() - MARGIN;
		final int minZ = region.originZ() - MARGIN;

		for (int x = 0; x < POS_DIAMETER; x++) {
			for (int y = 0; y < POS_DIAMETER; y++) {
				for (int z = 0; z < POS_DIAMETER; z++) {
					final int bx = x + minX;
					final int by = y + minY;
					final int bz = z + minZ;
					smoothPos.set(bx, by, bz);

					final BlockState state = region.getBlockState(bx, by, bz);
					// don't use cache here because we are populating the cache
					final int packedLight = region.directBrightness(smoothPos);

					final boolean opaque = state.isSolidRender(region, smoothPos);

					final int i = index(x, y, z);

					if (opaque) {
						block[i] = OPAQUE;
						sky[i] = OPAQUE;
					} else if (packedLight == 0) {
						block[i] = 0;
						sky[i] = 0;
					} else {
						block[i] = (packedLight & 0xFF);
						sky[i] = ((packedLight >>> 16) & 0xFF);
					}
				}
			}
		}

		final int[] work = help.c;
		smooth(BLUR_RADIUS + 1, block, work);
		smooth(BLUR_RADIUS, work, block);
		//        smooth(1, block, work);
		//        float[] swap = block;
		//        block = work;
		//        work = swap;

		smooth(BLUR_RADIUS + 1, sky, work);
		smooth(BLUR_RADIUS, work, sky);
		//        smooth(1, sky, work);
		//        swap = sky;
		//        sky = work;
		//        work = swap;

		final int limit = 16 + MARGIN + 1;

		for (int x = MARGIN - 1; x < limit; x++) {
			for (int y = MARGIN - 1; y < limit; y++) {
				for (int z = MARGIN - 1; z < limit; z++) {
					final int i = index(x, y, z);
					final int b = Mth.clamp(((block[i]) * 104 + 51) / 100, 0, 240);
					final int k = Mth.clamp(((sky[i]) * 104 + 51) / 100, 0, 240);
					region.setLightCache(x + minX, y + minY, z + minZ, ((b + 2) & 0b11111100) | (((k + 2) & 0b11111100) << 16));
				}
			}
		}
	}

	private static int index(int x, int y, int z) {
		return x + y * Y_INC + z * Z_INC;
	}

	private static void smooth(int margin, int[] src, int[] dest) {
		final int xBase = MARGIN - margin;
		final int xLimit = POS_DIAMETER - MARGIN + margin;

		final int yBase = xBase * Y_INC;
		final int yLimit = xLimit * Y_INC;
		final int zBase = xBase * Z_INC;
		final int zLimit = xLimit * Z_INC;

		// X PASS
		for (int x = xBase; x < xLimit; x++) {
			for (int y = yBase; y < yLimit; y += Y_INC) {
				for (int z = zBase; z < zLimit; z += Z_INC) {
					final int i = x + y + z;

					final int c = src[i];

					if (c == OPAQUE) {
						dest[i] = OPAQUE;
						continue;
					}

					//                    int a = src[index(x + 1, y, z)];
					//                    int b = src[index(x - 1, y, z)];
					final int a = src[i + 1];
					final int b = src[i - 1];

					if (a == OPAQUE) {
						if (b == OPAQUE) {
							dest[i] = c;
						} else {
							dest[i] = (b * OUTER_DIST + c * INNER_PLUS + 0x7FFF) >> 16;
						}
					} else if (b == OPAQUE) {
						dest[i] = (a * OUTER_DIST + c * INNER_PLUS + 0x7FFF) >> 16;
					} else {
						dest[i] = (a * OUTER_DIST + b * OUTER_DIST + c * INNER_DIST + 0x7FFF) >> 16;
					}
				}
			}
		}

		// Y PASS
		for (int x = xBase; x < xLimit; x++) {
			for (int y = yBase; y < yLimit; y += Y_INC) {
				for (int z = zBase; z < zLimit; z += Z_INC) {
					final int i = x + y + z;

					// Note arrays are swapped here
					final int c = dest[i];

					if (c == OPAQUE) {
						src[i] = OPAQUE;
						continue;
					}

					//                    int a = dest[index(x, y - 1, z)];
					//                    int b = dest[index(x, y + 1, z)];
					final int a = dest[i + Y_INC];
					final int b = dest[i - Y_INC];

					if (a == OPAQUE) {
						if (b == OPAQUE) {
							src[i] = c;
						} else {
							src[i] = (b * OUTER_DIST + c * INNER_PLUS + 0x7FFF) >> 16;
						}
					} else if (b == OPAQUE) {
						src[i] = (a * OUTER_DIST + c * INNER_PLUS + 0x7FFF) >> 16;
					} else {
						src[i] = (a * OUTER_DIST + b * OUTER_DIST + c * INNER_DIST + 0x7FFF) >> 16;
					}
				}
			}
		}

		// Z PASS
		for (int x = xBase; x < xLimit; x++) {
			for (int y = yBase; y < yLimit; y += Y_INC) {
				for (int z = zBase; z < zLimit; z += Z_INC) {
					final int i = x + y + z;

					// Arrays are swapped back to original roles here
					final int c = src[i];

					if (c == OPAQUE) {
						dest[i] = OPAQUE;
						continue;
					}

					//                    int a = src[index(x, y, z - 1)];
					//                    int b = src[index(x, y, z + 1)];
					final int a = src[i + Z_INC];
					final int b = src[i - Z_INC];

					if (a == OPAQUE) {
						if (b == OPAQUE) {
							dest[i] = c;
						} else {
							dest[i] = (b * OUTER_DIST + c * INNER_PLUS + 0x7FFF) >> 16;
						}
					} else if (b == OPAQUE) {
						dest[i] = (a * OUTER_DIST + c * INNER_PLUS + 0x7FFF) >> 16;
					} else {
						dest[i] = (a * OUTER_DIST + b * OUTER_DIST + c * INNER_DIST + 0x7FFF) >> 16;
					}
				}
			}
		}
	}

	private static class Helper {
		private final BlockPos.MutableBlockPos smoothPos = new BlockPos.MutableBlockPos();
		private final int[] a = new int[POS_COUNT];
		private final int[] b = new int[POS_COUNT];
		private final int[] c = new int[POS_COUNT];
	}
}
