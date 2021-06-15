/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.terrain.occlusion;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.terrain.region.RenderRegionIndexer;

public class ShadowPotentiallyVisibleRegionSet<T extends ShadowPotentiallyVisibleRegion> implements PotentiallyVisibleRegionSet<T> {
	private int version = 1;

	private final T[] regions;

	int xBase;
	int zBase;

	protected ShadowPotentiallyVisibleRegionSet(T[] regions) {
		this.regions = regions;
		assert regions.length == RenderRegionIndexer.PADDED_REGION_INDEX_COUNT;
	}

	private interface AxisIterator {
		boolean next();

		void reset();
	}

	private int x, y, z;

	private final AxisIterator XPOS = new AxisIterator() {
		@Override
		public boolean next() {
			if (++x < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER) {
				return true;
			} else {
				reset();
				return false;
			}
		}

		@Override
		public void reset() {
			x = 0;
		}
	};

	private final AxisIterator XNEG = new AxisIterator() {
		@Override
		public boolean next() {
			if (--x >= 0) {
				return true;
			} else {
				reset();
				return false;
			}
		}

		@Override
		public void reset() {
			x = RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER - 1;
		}
	};

	private final AxisIterator YPOS = new AxisIterator() {
		@Override
		public boolean next() {
			if (++y < RenderRegionIndexer.MAX_Y_REGIONS) {
				return true;
			} else {
				reset();
				return false;
			}
		}

		@Override
		public void reset() {
			y = 0;
		}
	};

	private final AxisIterator YNEG = new AxisIterator() {
		@Override
		public boolean next() {
			if (--y >= 0) {
				return true;
			} else {
				reset();
				return false;
			}
		}

		@Override
		public void reset() {
			y = RenderRegionIndexer.MAX_Y_REGIONS - 1;
		}
	};

	private final AxisIterator ZPOS = new AxisIterator() {
		@Override
		public boolean next() {
			if (++z < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER) {
				return true;
			} else {
				reset();
				return false;
			}
		}

		@Override
		public void reset() {
			z = 0;
		}
	};

	private final AxisIterator ZNEG = new AxisIterator() {
		@Override
		public boolean next() {
			if (--z >= 0) {
				return true;
			} else {
				reset();
				return false;
			}
		}

		@Override
		public void reset() {
			z = RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER - 1;
		}
	};

	private AxisIterator primary = XPOS;
	private AxisIterator secondary = YPOS;
	private AxisIterator tertiary = ZPOS;

	/**
	 * Points toward the light from any point in the scene.
	 * (Assumes an orthogonal light/shadow projection.)
	 * Used to control the order of iteration and can be
	 * changed after regions are added.
	 *
	 * <p>Calls {@link #returnToStart()} because a new light
	 * vector invalidates the previous sort order.
	 *
	 * @param x x-axis component of light vector
	 * @param y y-axis component of light vector
	 * @param z z-axis component of light vector
	 */
	public void setLightVectorAndRestart(float x, float y, float z) {
		final float ax = Math.abs(x);
		final float ay = Math.abs(y);
		final float az = Math.abs(z);

		if (ax > ay) {
			if (ax > az) {
				// X primary
				if (ay > az) {
					// ORDER XYZ
					primary = x > 0 ? XNEG : XPOS;
					secondary = y > 0 ? YNEG : YPOS;
					tertiary = z > 0 ? ZNEG : ZPOS;
				} else {
					// ORDER XZY
					primary = x > 0 ? XNEG : XPOS;
					secondary = z > 0 ? ZNEG : ZPOS;
					tertiary = y > 0 ? YNEG : YPOS;
				}
			} else {
				// Z primary, because X > Y and Z >= X
				// ORDER ZXY
				primary = z > 0 ? ZNEG : ZPOS;
				secondary = x > 0 ? XNEG : XPOS;
				tertiary = y > 0 ? YNEG : YPOS;
			}
		} else {
			// X cannot be primary
			if (ay > az) {
				// Y primary, XZ order undetermined
				if (ax > az) {
					// ORDER YXZ
					primary = y > 0 ? YNEG : YPOS;
					secondary = x > 0 ? XNEG : XPOS;
					tertiary = z > 0 ? ZNEG : ZPOS;
				} else {
					// ORDER YZX
					primary = y > 0 ? YNEG : YPOS;
					secondary = z > 0 ? ZNEG : ZPOS;
					tertiary = x > 0 ? XNEG : XPOS;
				}
			} else {
				// Z primary, because Y >= X and Z >= Y
				// ORDER ZYX
				primary = z > 0 ? ZNEG : ZPOS;
				secondary = y > 0 ? YNEG : YPOS;
				tertiary = x > 0 ? XNEG : XPOS;
			}
		}

		returnToStart();
	}

	/**
	 * Captures the x and z origin coordinates of the camera region.
	 * This is used to compute a relative position for iteration.
	 * The y coordinate is not used because the sky light is always
	 * outside of the world and y isn't useful as a relative origin.
	 *
	 * <p>Also calls {@link #clear()} because changing the origin
	 * invalidates the addressing of any regions already added.
	 *
	 * @param x x-axis block position coordinate of the camera region/chunk origin
	 * @param z z-axis block position coordinate of the camera region/chunk origin
	 */
	public void setCameraChunkOriginAndClear(int x, int z) {
		xBase = RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS - (x >> 4);
		zBase = RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS - (z >> 4);
		clear();
	}

	@Override
	public int version() {
		return version;
	}

	@Override
	public void clear() {
		Arrays.fill(regions, null);
		++version;
		returnToStart();
	}

	/**
	 * Computes index given normalized x, y, z region coordinates.
	 *
	 * <p>These are chunk-type coordinates, not block coordinates. (>> 4).
	 *
	 * @param rx chunk coordinate relative to xBase (0 to MAX_CHUNK_DIAMETER - 1)
	 * @param ry chunk coordinate relative to Y_BLOCKPOS_OFFSET
	 * @param rz chunk coordinate relative to zBase (0 to MAX_CHUNK_DIAMETER - 1)
	 * @return index to region array, will be within {@link RenderRegionIndexer#REGION_INDEX_COUNT}
	 */
	private static int index(int rx, int ry, int rz) {
		assert rx >= 0;
		assert rx < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER;
		assert rz >= 0;
		assert rz < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER;
		assert ry >= 0;
		assert ry < RenderRegionIndexer.MAX_Y_REGIONS;

		return rx | (rz << RenderRegionIndexer.CHUNK_DIAMETER_BITS) | (ry << (RenderRegionIndexer.CHUNK_DIAMETER_BITS * 2));
	}

	@Override
	public void add(T region) {
		BlockPos origin = region.origin();
		int rx = (origin.getX() >> 4) + xBase;
		int rz = (origin.getZ() >> 4) + zBase;
		int ry = (origin.getY() + RenderRegionIndexer.Y_BLOCKPOS_OFFSET) >> 4;

		//System.out.println(String.format("Adding origin %s with region pos %d  %d  %d  with index %d", region.origin().toShortString(), rx, ry, rz, index(rx, ry, rz)));
		regions[index(rx, ry, rz)] = region;
	}

	boolean complete = false;

	@Override
	public void returnToStart() {
		primary.reset();
		secondary.reset();
		tertiary.reset();
		complete = false;
	}

	@Override
	public @Nullable T next() {
		T result = null;

		if (!complete) {
			while (result == null) {
				System.out.println(x + ", " + y + ", " + z);
				int i = index(x, y, z);
				result = regions[i];

				if (!tertiary.next()) {
					if (!secondary.next()) {
						if (!primary.next()) {
							complete = true;
							break;
						}
					}
				}
			}
		}

		return result;
	}
}
