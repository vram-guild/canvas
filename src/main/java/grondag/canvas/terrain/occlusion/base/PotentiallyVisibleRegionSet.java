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

package grondag.canvas.terrain.occlusion.base;

import org.jetbrains.annotations.Nullable;

/**
 * Tracks which regions are potentially visible from a perspective that is
 * implementation-dependent and sorts them from near to far relative to that perspective.
 * Supports iteration in sorted order.
 */
public interface PotentiallyVisibleRegionSet<S extends PotentiallyVisibleRegionSet<S, U>, U extends AbstractRegionVisibility<?, U>> {
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
	void add(U region);

	/**
	 * Restarts the iteration from the beginning.
	 */
	void returnToStart();

	/**
	 * Returns next region in sorted iteration and advances for next call.
	 * Returns null if at end or if set is empty.
	 */
	@Nullable U next();
}
