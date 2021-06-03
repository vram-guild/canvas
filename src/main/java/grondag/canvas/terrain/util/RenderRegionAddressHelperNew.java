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

package grondag.canvas.terrain.util;

public abstract class RenderRegionAddressHelperNew {
	/** How many block/fluid states we capture along edges of region to support neighbor queries on edges. Vanilla is 2. */
	private static final int RENDER_REGION_PADDING = 2;
	/** Number of block/fluid states in each axis of a render region including padding on each face. */
	private static final int RENDER_REGION_DIAMETER = 16 + RENDER_REGION_PADDING * 2;
	private static final int RENDER_REGION_DIAMETER_SQ = RENDER_REGION_DIAMETER * RENDER_REGION_DIAMETER;

	/** How many block/fluid states catured for a render region, including padding. */
	public static final int RENDER_REGION_STATE_COUNT = RENDER_REGION_DIAMETER * RENDER_REGION_DIAMETER * RENDER_REGION_DIAMETER;

	/** Index of state from coordinates relative to region origin. */
	public static final int renderRegionIndex(int x, int y, int z) {
		return (x + RENDER_REGION_PADDING) + (y + RENDER_REGION_PADDING) * RENDER_REGION_DIAMETER + (z + RENDER_REGION_PADDING) * RENDER_REGION_DIAMETER_SQ;
	}
}
