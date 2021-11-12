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

package grondag.canvas.varia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

/**
 * Random utilities that have not yet found a more appropriate home.
 * Many of these are obsolete and should be removed.
 */
@Deprecated
// UGLY: was copied from Fermion Varia and should depend on the appopriate VRAM library when it becomes available.
public class CircleHacks {
	/**
	 * See {@link #getDistanceSortedCircularOffset(int)}. If you simply want to
	 * iterate all, can simply use this directly.
	 */
	public static final Vec3i[] DISTANCE_SORTED_CIRCULAR_OFFSETS;

	public static final int DISTANCE_SORTED_CIRCULAR_OFFSETS_MAX_RADIUS = 64;

	public static final int DISTANCE_SORTED_CIRCULAR_OFFSETS_COUNT;

	static {
		// need to use a hash bc fill2dCircleInPlaneXZ does not guarantee uniqueness.
		final HashSet<Vec3i> offsets = new HashSet<>();

		for (final long packed : fill2dCircleInPlaneXZ(DISTANCE_SORTED_CIRCULAR_OFFSETS_MAX_RADIUS).toLongArray()) {
			final int x = BlockPos.getX(packed);
			final int z = BlockPos.getZ(packed);
			offsets.add(new Vec3i(x, Math.sqrt(x * x + z * z), z));
		}

		final ArrayList<Vec3i> offsetList = new ArrayList<>(offsets);

		offsetList.sort(new Comparator<Vec3i>() {
			@Override
			public int compare(Vec3i o1, Vec3i o2) {
				return Integer.compare(o1.getY(), o2.getY());
			}
		});

		DISTANCE_SORTED_CIRCULAR_OFFSETS_COUNT = offsetList.size();
		DISTANCE_SORTED_CIRCULAR_OFFSETS = offsetList.toArray(new Vec3i[DISTANCE_SORTED_CIRCULAR_OFFSETS_COUNT]);
	}

	/**
	 * Returns values in a sequence of horizontal offsets from X=0, Z=0.<br>
	 * Y value is the euclidian distance from the origin.<br>
	 * Values are sorted by distance from 0,0,0. Value at index 0 is the origin.<br>
	 * Distance is up to 64 blocks from origin. Values outside that range throw
	 * exceptions.<br>
	 */
	public static Vec3i getDistanceSortedCircularOffset(int index) {
		return DISTANCE_SORTED_CIRCULAR_OFFSETS[index];
	}

	/**
	 * Returns the last (exclusive) offset index of
	 * {@value #DISTANCE_SORTED_CIRCULAR_OFFSETS} (also the index for
	 * {@link #getDistanceSortedCircularOffset(int)} that is at the given radius
	 * from the origin.
	 */
	public static int getLastDistanceSortedOffsetIndex(int radius) {
		if (radius < 0) {
			radius = 0;
		}

		int result = 0;

		while (++result < DISTANCE_SORTED_CIRCULAR_OFFSETS.length) {
			if (DISTANCE_SORTED_CIRCULAR_OFFSETS[result].getY() > radius) {
				return result;
			}
		}

		return result;
	}

	/**
	 * Returns a list of packed block position x & z OFFSETS within the given
	 * radius. Origin will be the start position.
	 */
	private static LongArrayList fill2dCircleInPlaneXZ(int radius) {
		final LongArrayList result = new LongArrayList((int) (2 * radius * 3.2));

		// uses midpoint circle algorithm
		if (radius > 0) {
			int x = radius;
			int z = 0;
			int err = 0;

			result.add(BlockPos.asLong(0, 0, 0));

			while (x >= z) {
				if (z > 0) {
					result.add(BlockPos.asLong(z, 0, z));
					result.add(BlockPos.asLong(-z, 0, z));
					result.add(BlockPos.asLong(z, 0, -z));
					result.add(BlockPos.asLong(-z, 0, -z));
				}

				for (int i = x; i > z; i--) {
					result.add(BlockPos.asLong(i, 0, z));
					result.add(BlockPos.asLong(z, 0, i));
					result.add(BlockPos.asLong(-i, 0, z));
					result.add(BlockPos.asLong(-z, 0, i));
					result.add(BlockPos.asLong(i, 0, -z));
					result.add(BlockPos.asLong(z, 0, -i));
					result.add(BlockPos.asLong(-i, 0, -z));
					result.add(BlockPos.asLong(-z, 0, -i));
				}

				if (err <= 0) {
					z += 1;
					err += 2 * z + 1;
				}

				if (err > 0) {
					x -= 1;
					err -= 2 * x + 1;
				}
			}
		}

		return result;
	}
}
