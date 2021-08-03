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

	long packedOriginBlockPos();

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
		public long packedOriginBlockPos() {
			return 0;
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
