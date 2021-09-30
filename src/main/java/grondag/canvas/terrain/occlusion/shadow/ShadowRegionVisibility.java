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

import grondag.canvas.terrain.occlusion.base.AbstractRegionVisibility;
import grondag.canvas.terrain.region.RenderRegion;

public class ShadowRegionVisibility extends AbstractRegionVisibility<ShadowVisibility, ShadowRegionVisibility> {
	public ShadowRegionVisibility(ShadowVisibility visibility, RenderRegion region) {
		super(visibility, region);
	}

	@Override
	public void addIfValid() {
		if (region.origin.isPotentiallyVisibleFromSkylight() && !region.isClosed() && region.renderChunk.areCornersLoaded()) {
			addVisitedIfNotPresent();
		}
	}
}
