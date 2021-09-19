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

package grondag.canvas.terrain.occlusion.shadow;

import grondag.fermion.varia.Useful;
import net.minecraft.core.Vec3i;

public class RegionBoundingSphere {
	/** Max visible range, in chunks. (Not block pos.) */
	private int regionRenderDistance;
	private int indexLimit;
	private int[] yDist = new int[Useful.DISTANCE_SORTED_CIRCULAR_OFFSETS_COUNT];

	public void update(int regionRenderDistance) {
		if (regionRenderDistance != this.regionRenderDistance) {
			this.regionRenderDistance = regionRenderDistance;

			final int maxSqDist = regionRenderDistance * regionRenderDistance;

			final int indexLimit = Useful.getLastDistanceSortedOffsetIndex(regionRenderDistance);
			this.indexLimit = indexLimit;

			for (int i = 0; i < indexLimit; ++i) {
				final Vec3i offset = Useful.getDistanceSortedCircularOffset(i);
				int x = offset.getX();
				int z = offset.getZ();

				int ysq = maxSqDist - x * x - z * z;

				if (ysq <= 0) {
					// Implies outside render distance.
					// Negative value lets us filter these out in iteration
					yDist[i] = -1;
				} else {
					int y = (int) Math.floor(Math.sqrt(ysq));

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
