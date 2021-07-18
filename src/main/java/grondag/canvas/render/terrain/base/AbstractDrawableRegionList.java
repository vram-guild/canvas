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

package grondag.canvas.render.terrain.base;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public abstract class AbstractDrawableRegionList implements DrawableRegionList {
	protected final ObjectArrayList<DrawableRegion> regions;
	private boolean isClosed = false;

	protected AbstractDrawableRegionList(final ObjectArrayList<DrawableRegion> regions) {
		this.regions = regions;
	}

	@Override
	public final void close() {
		if (!isClosed) {
			for (DrawableRegion region : regions) {
				region.releaseFromDrawList();
			}

			isClosed = true;
			closeInner();
		}
	}

	protected abstract void closeInner();

	@Override
	public boolean isClosed() {
		return isClosed;
	}
}
