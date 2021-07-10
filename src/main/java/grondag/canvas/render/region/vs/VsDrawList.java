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

package grondag.canvas.render.region.vs;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.render.region.DrawableRegion;
import grondag.canvas.render.region.RegionDrawList;
import grondag.canvas.render.region.base.AbstractDrawList;

public class VsDrawList extends AbstractDrawList {
	private VsDrawList(final ObjectArrayList<DrawableRegion> regions) {
		super(regions);
	}

	public static RegionDrawList build(final ObjectArrayList<DrawableRegion> regions) {
		if (regions.isEmpty()) {
			return RegionDrawList.EMPTY;
		}

		return new VsDrawList(regions);
	}

	@Override
	public void draw() {
		// WIP
	}

	@Override
	protected void closeInner() {
		// WIP
	}
}
