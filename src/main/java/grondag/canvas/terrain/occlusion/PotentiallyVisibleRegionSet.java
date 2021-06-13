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

import org.jetbrains.annotations.Nullable;

import grondag.canvas.terrain.region.BuiltRenderRegion;

/**
 * Tracks which regions are potentially visible from a perspective that is
 * implementation-dependent and sorts them from near to far relative to that perspective.
 * Supports iteration in sorted order.
 */
public interface PotentiallyVisibleRegionSet {
	/**
	 * Increments every time {@link #clear()} is called.
	 * Use for synchronization of dependent state.
	 * Also useful to know if a region has already been added
	 * to the set.
	 */
	int version();

	/**
	 * Empties the set and increments version.
	 */
	void clear();

	/**
	 * Adds region to set in sorted position according to implementation.
	 * Requires but does NOT check that region is not already in the set.
	 */
	void add(BuiltRenderRegion region);

	/**
	 * Restarts the iteration from the beginning.
	 */
	void returnToStart();

	/**
	 * Returns next region in sorted iteration and advances for next call.
	 * Returns null if at end or if set is empty.
	 */
	@Nullable BuiltRenderRegion next();
}
