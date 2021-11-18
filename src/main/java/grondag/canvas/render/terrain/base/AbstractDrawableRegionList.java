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

package grondag.canvas.render.terrain.base;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.material.state.RenderState;

public abstract class AbstractDrawableRegionList implements DrawableRegionList {
	protected final ObjectArrayList<DrawableRegion> regions;
	private boolean isClosed = false;
	public final RenderState renderState;

	protected AbstractDrawableRegionList(final ObjectArrayList<DrawableRegion> regions, RenderState renderState) {
		this.regions = regions;
		this.renderState = renderState;
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
