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

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class SortableVisibleRegionList extends VisibleRegionList {
	private int sortPositionVersion;
	private int lastSortPositionVersion;
	private long lastCameraBlockPos = Long.MAX_VALUE;
	private Vec3d lastSortPos = new Vec3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);

	@Override
	public void clear() {
		super.clear();
		lastSortPos = new Vec3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
		lastCameraBlockPos = Long.MAX_VALUE;
	}

	/**
	 * Incremented when player moves enough to trigger translucency resort.
	 */
	public int sortPositionVersion() {
		return sortPositionVersion;
	}

	public Vec3d lastSortPos() {
		return lastSortPos;
	}

	/**
	 * Checks build regions for translucent resort need and schedules
	 * up to 16 of them per pass. Nearer regions are checked first and
	 * the sort position version isn't updated until all regions are handled.
	 *
	 * <p>Regions that are non-translucent, already scheduled or already current
	 * won't count against the limit.  Resorts are fast and happen off thread -
	 * checking incrementally avoids overloading the GPU with buffer uploads.
	 */
	public void scheduleResort(Vec3d cameraPos) {
		final double x = cameraPos.x;
		final double y = cameraPos.y;
		final double z = cameraPos.z;

		final long cameraBlockPos = BlockPos.asLong((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
		boolean movedEnoughToInvalidateSort = false;

		if (cameraBlockPos != lastCameraBlockPos) {
			lastCameraBlockPos = cameraBlockPos;
			movedEnoughToInvalidateSort = true;
		} else {
			// can move 1.0 or more diagonally within same block pos
			final double sdx = x - lastSortPos.x;
			final double sdy = y - lastSortPos.y;
			final double sdz = z - lastSortPos.z;
			movedEnoughToInvalidateSort = sdx * sdx + sdy * sdy + sdz * sdz >= 1.0D;
		}

		if (movedEnoughToInvalidateSort) {
			++sortPositionVersion;
			lastSortPos = cameraPos;
		}

		final int positionVersion = sortPositionVersion;

		if (positionVersion != lastSortPositionVersion) {
			final MinecraftClient mc = MinecraftClient.getInstance();
			mc.getProfiler().push("translucent_sort");
			final int limit = visibleRegionCount;
			int count = 0;
			int i;

			// PERF: probably a way to sort more distant regions less frequently
			for (i = 0; i < limit; i++) {
				if (visibleRegions[i].scheduleSort(positionVersion)) {
					if (++count > 16) {
						break;
					}
				}
			}

			if (i == limit) {
				lastSortPositionVersion = positionVersion;
			}

			mc.getProfiler().pop();
		}
	}
}
