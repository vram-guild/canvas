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

package grondag.canvas.terrain.occlusion;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class SortableVisibleRegionList extends VisibleRegionList {
	private int sortPositionVersion;
	private int lastSortPositionVersion;
	private long lastCameraBlockPos = Long.MAX_VALUE;
	private Vec3 lastSortPos = new Vec3(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);

	@Override
	public void clear() {
		super.clear();
		lastSortPos = new Vec3(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
		lastCameraBlockPos = Long.MAX_VALUE;
	}

	/**
	 * Incremented when player moves enough to trigger translucency resort.
	 */
	public int sortPositionVersion() {
		return sortPositionVersion;
	}

	public Vec3 lastSortPos() {
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
	public void scheduleResort(Vec3 cameraPos) {
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
			final Minecraft mc = Minecraft.getInstance();
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
