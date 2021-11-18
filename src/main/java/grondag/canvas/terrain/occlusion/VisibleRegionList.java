/*
 * Copyright © Original Authors
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

import java.util.Arrays;

import grondag.canvas.terrain.region.RenderRegion;

public class VisibleRegionList {
	protected RenderRegion[] visibleRegions = new RenderRegion[4096];
	protected volatile int visibleRegionCount = 0;

	public void clear() {
		visibleRegionCount = 0;
		Arrays.fill(visibleRegions, null);
	}

	public final void add(RenderRegion builtRegion) {
		int index = visibleRegionCount++;
		RenderRegion[] visibleRegions = this.visibleRegions;

		if (index >= visibleRegions.length) {
			RenderRegion[] newRegions = new RenderRegion[visibleRegions.length * 2];
			System.arraycopy(visibleRegions, 0, newRegions, 0, visibleRegions.length);
			this.visibleRegions = newRegions;
			visibleRegions = newRegions;
		}

		visibleRegions[index] = builtRegion;
	}

	public final void copyFrom(VisibleRegionList source) {
		final int count = source.visibleRegionCount;
		visibleRegionCount = count;

		if (count > visibleRegions.length) {
			visibleRegions = new RenderRegion[source.visibleRegions.length];
		}

		System.arraycopy(source.visibleRegions, 0, visibleRegions, 0, count);
	}

	public final int size() {
		return visibleRegionCount;
	}

	public final RenderRegion get(int index) {
		return visibleRegions[index];
	}

	public final int getActiveCount() {
		return visibleRegionCount;
	}
}
