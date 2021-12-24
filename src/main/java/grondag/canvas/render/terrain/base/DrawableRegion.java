/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.render.terrain.base;

import grondag.canvas.buffer.render.UploadableVertexStorage;

/**
 * Token for the region-specific resources (vertex buffers, storage buffers)
 * needed to draw a region in a specific draw pass (solid, translucent, shadow.)
 * Could represent multiple render states to be drawn within the same pass.
 */
public interface DrawableRegion {
	/**
	 * RenderRegions MUST be call this exactly once when the resources
	 * are no longer needed.  Events that would trigger this include:
	 * <ul><li>Holding region is closed due to world change/reload</li>
	 * <li>Holding region goes out of render distance</li>
	 * <li>This instance is replaced by a different DrawableRegion
	 * when a region is rebuilt. (UploadableRegion does not handle this!)</li></ul>
	 */
	void releaseFromRegion();

	void retainFromDrawList();

	void releaseFromDrawList();

	int quadVertexCount();

	UploadableVertexStorage storage();

	DrawableRegion EMPTY_DRAWABLE = new DrawableRegion() {
		@Override
		public void releaseFromRegion() {
			// NOOP
		}

		@Override
		public void retainFromDrawList() {
			// NOOP
		}

		@Override
		public void releaseFromDrawList() {
			// NOOP
		}

		@Override
		public int quadVertexCount() {
			return 0;
		}

		@Override
		public UploadableVertexStorage storage() {
			return null;
		}
	};
}
