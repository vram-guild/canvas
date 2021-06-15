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

import it.unimi.dsi.fastutil.longs.LongArrays;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import grondag.canvas.apiimpl.util.GeometryHelper;
import grondag.canvas.terrain.region.RenderRegionIndexer;

public class ShadowPotentiallyVisibleRegionSet<T extends ShadowPotentiallyVisibleRegion> implements PotentiallyVisibleRegionSet<T> {
	private int version = 1;

	private final T[] regions;

	/**
	 * Holds offsets from the sweeping plane for each indexed position within the
	 * sweep cross-section. Used to ensure that we iterate positions that are
	 * closest to the light. Because the skylight/shadow use an orthogonal projection,
	 * we consider every region on the sweeping place to be equally close. So we
	 * can iterate easily by translating these offsets through the region volume.
	 *
	 * <p>The address space is smaller when we are sweeping along the X or Z axis,
	 * but to avoid waste we use a single offset array large enough for Y axis, too.
	 */
	private final int[] sortOrder = new int[RenderRegionIndexer.LOADED_REGION_INDEX_COUNT];

	private int sortIndex = 0;

	int xBase;
	int zBase;

	protected ShadowPotentiallyVisibleRegionSet(T[] regions) {
		this.regions = regions;
		assert regions.length == RenderRegionIndexer.PADDED_REGION_INDEX_COUNT;
	}

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
		// signs are flipped because vector is towards light and we are sweeping away from it
		x = -x;
		y = -y;
		z = -z;

		Direction sweepDirection = Direction.byId(GeometryHelper.closestFaceFromNormal(x, y, z));

		switch (sweepDirection) {
			case EAST:
				assert false;
				break;
			case NORTH:
				assert false;
				break;
			case SOUTH:
				assert false;
				break;
			case WEST:
				assert false;
				break;
			case UP:
				setupSweepUp(x, y, z);
				break;
			case DOWN:
				setupSweepDown(x, y, z);
				break;
			default:
				assert false : "Bad shadow light vector";
				setupSweepDown(x, y, z);
				break;
		}

		returnToStart();
	}

	final int[][] depth = new int[RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER][RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER];

	void setupSweepDown(float x, float y, float z) {
		final int[][] depth = this.depth;

		int minDepth = Integer.MAX_VALUE;

		for (int rx = 0; rx < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER; ++rx) {
			int brx = (rx - RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS) << 4;

			final int[] subDepth = depth[rx];

			for (int rz = 0; rz < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER; ++rz) {
				int brz = (rz - RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS) << 4;
				int d = (int) Math.floor((brx * x + brz * z) / -y) >> 4;

				if (d < minDepth) {
					minDepth = d;
				}

				subDepth[rz] = d;
			}
		}

		int n = 0;
		int depthOffset = RenderRegionIndexer.MAX_Y_REGIONS - 1 - minDepth;

		while (n < RenderRegionIndexer.LOADED_REGION_INDEX_COUNT) {
			for (int rx = 0; rx < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER; ++rx) {
				final int[] subDepth = depth[rx];

				for (int rz = 0; rz < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER; ++rz) {
					int ry = subDepth[rz] + depthOffset;

					if (ry < RenderRegionIndexer.MAX_Y_REGIONS && ry >= 0) {
						sortOrder[n++] = index(rx, ry, rz);
					}
				}
			}

			--depthOffset;
		}
	}

	void setupSweepUp(float x, float y, float z) {
		final int[][] depth = this.depth;

		int maxDepth = Integer.MIN_VALUE;

		for (int rx = 0; rx < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER; ++rx) {
			int brx = (rx - RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS) << 4;

			final int[] subDepth = depth[rx];

			for (int rz = 0; rz < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER; ++rz) {
				int brz = (rz - RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS) << 4;
				int d = Math.round((brx * x + brz * z) / -y) >> 4;

				if (d > maxDepth) {
					maxDepth = d;
				}

				subDepth[rz] = d;
			}
		}

		int n = 0;
		int depthOffset = RenderRegionIndexer.MAX_Y_REGIONS + 1 + maxDepth;

		while (n < RenderRegionIndexer.LOADED_REGION_INDEX_COUNT) {
			for (int rx = 0; rx < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER; ++rx) {
				final int[] subDepth = depth[rx];

				for (int rz = 0; rz < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER; ++rz) {
					int ry = subDepth[rz] - depthOffset;

					if (ry < RenderRegionIndexer.MAX_Y_REGIONS && ry >= 0) {
						sortOrder[n++] = index(rx, ry, rz);
					}
				}
			}

			--depthOffset;
		}
	}

	final long[] wideSorter = new long[RenderRegionIndexer.LOADED_REGION_INDEX_COUNT];

	/** Slow but almost certainly correct.  Here for testing support. */
	public void setLightVectorAndRestartSlowly(float x, float y, float z) {
		// signs are flipped because vector is towards light and we are sweeping away from it
		x = -x;
		y = -y;
		z = -z;

		final long[] wideSorter = new long[RenderRegionIndexer.LOADED_REGION_INDEX_COUNT];

		int n = 0;

		for (int rx = 0; rx < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER; ++rx) {
			int brx = (rx - RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS) << 4;

			for (int rz = 0; rz < RenderRegionIndexer.MAX_LOADED_CHUNK_DIAMETER; ++rz) {
				int brz = (rz - RenderRegionIndexer.MAX_LOADED_CHUNK_RADIUS) << 4;

				for (int ry = 0; ry < RenderRegionIndexer.MAX_Y_REGIONS; ++ry) {
					int bry = (ry << 4) - RenderRegionIndexer.Y_BLOCKPOS_OFFSET;
					long dist = Math.round((brx * x + bry * y + brz * z + 10000f));

					//assert dist > 0;

					int index = index(rx, ry, rz);

					long val = (dist << 20) | index;

					//assert val >= 0;

					wideSorter[n++] = val;

					//assert val >> 20 == dist;
					//assert (val & 0x000FFFFF) == index;
				}
			}
		}

		//assert n == RenderRegionIndexer.LOADED_REGION_INDEX_COUNT;

		LongArrays.radixSort(wideSorter);

		for (int i = 0; i < RenderRegionIndexer.LOADED_REGION_INDEX_COUNT; ++i) {
			sortOrder[i] = (int) (wideSorter[i] & 0x000FFFFFL);
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

	@Override
	public void returnToStart() {
		sortIndex = 0;
	}

	@Override
	public @Nullable T next() {
		while (sortIndex < RenderRegionIndexer.LOADED_REGION_INDEX_COUNT) {
			final T region = regions[sortOrder[sortIndex++]];

			if (region != null) {
				return region;
			}
		}

		return null;
	}
}
