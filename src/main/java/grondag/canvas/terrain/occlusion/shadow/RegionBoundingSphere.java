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

package grondag.canvas.terrain.occlusion.shadow;

import net.minecraft.core.Vec3i;

import grondag.canvas.varia.CircleHacks;

public class RegionBoundingSphere {
	/** Max visible range, in chunks. (Not block pos.) */
	private int regionRenderDistance;
	private int indexLimit;
	private int[] yDist = new int[CircleHacks.DISTANCE_SORTED_CIRCULAR_OFFSETS_COUNT];

	public void update(int regionRenderDistance) {
		if (regionRenderDistance != this.regionRenderDistance) {
			this.regionRenderDistance = regionRenderDistance;

			final int maxSqDist = regionRenderDistance * regionRenderDistance;

			final int indexLimit = CircleHacks.getLastDistanceSortedOffsetIndex(regionRenderDistance);
			this.indexLimit = indexLimit;

			for (int i = 0; i < indexLimit; ++i) {
				final Vec3i offset = CircleHacks.getDistanceSortedCircularOffset(i);
				final int x = offset.getX();
				final int z = offset.getZ();

				final int ysq = maxSqDist - x * x - z * z;

				if (ysq <= 0) {
					// Implies outside render distance.
					// Negative value lets us filter these out in iteration
					yDist[i] = -1;
				} else {
					final int y = (int) Math.floor(Math.sqrt(ysq));

					assert x * x + y * y + z * z <= maxSqDist;
					assert x * x + (y + 1) * (y + 1) + z * z > maxSqDist;

					yDist[i] = y;
				}
			}
		}
	}

	/**
	 * Negative value implies outside render distance.
	 */
	public int getY(int index) {
		assert index < indexLimit;
		return yDist[index];
	}
}
