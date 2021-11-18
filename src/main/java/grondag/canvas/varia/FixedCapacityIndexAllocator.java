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

package grondag.canvas.varia;

import java.util.BitSet;

public class FixedCapacityIndexAllocator {
	private final int indexCapacity;
	private final BitSet bits;
	private int inUseCount = 0;
	private int nextIndex = 0;

	public FixedCapacityIndexAllocator(int indexCapacity) {
		this.indexCapacity = indexCapacity;
		bits = new BitSet(indexCapacity);
	}

	public synchronized int claimIndex() {
		if (inUseCount < indexCapacity) {
			++inUseCount;

			int result = bits.nextClearBit(nextIndex);

			if (result >= indexCapacity) {
				nextIndex = 0;
			}

			result = bits.nextClearBit(nextIndex);

			assert result < indexCapacity;
			assert result >= 0;

			bits.set(result);
			nextIndex = result + 1;

			return result;
		}

		assert false : "Unable to claim index - may have reached capacity";
		return 0;
	}

	public synchronized void releaseIndex(final int index) {
		assert bits.get(index);
		bits.clear(index);
		--inUseCount;
	}

	public synchronized void clear() {
		bits.clear();
		inUseCount = 0;
		nextIndex = 0;
	}
}
